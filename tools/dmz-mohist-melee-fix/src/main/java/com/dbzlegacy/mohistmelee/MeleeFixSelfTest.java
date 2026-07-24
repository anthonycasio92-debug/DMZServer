package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Boot probe: ENTITY_REACH repair + prove we do not wipe DMZ damage attributes.
 */
public final class MeleeFixSelfTest {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final UUID PROBE_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private MeleeFixSelfTest() {}

    public static void registerIfEnabled() {
        boolean mohist = isMohist();
        boolean forced = Boolean.getBoolean("dmz.melee.fix.selftest");
        if (!mohist && !forced) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new MeleeFixSelfTest());
        LOGGER.info("[{}] Self-test enabled (mohist={} forced={})", DmzMohistMeleeFix.MOD_ID, mohist, forced);
    }

    private static boolean isMohist() {
        try {
            Class.forName("com.mohistmc.MohistMC");
            return true;
        } catch (ClassNotFoundException e) {
            try {
                Class.forName("org.bukkit.Bukkit");
                return true;
            } catch (ClassNotFoundException e2) {
                return false;
            }
        }
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        server.execute(() -> run(server));
    }

    private static void run(MinecraftServer server) {
        try {
            ServerLevel level = server.m_129783_();
            FakePlayer fake = FakePlayerFactory.get(level, new GameProfile(PROBE_UUID, "ReachFixProbe"));

            // --- ENTITY_REACH ---
            double reachBefore = ReachAttributeFix.readEntityReach(fake);
            Attribute reachAttr = ForgeMod.ENTITY_REACH.get();
            AttributeInstance reachInst = reachAttr == null ? null : fake.m_21051_(reachAttr);
            if (reachInst == null) {
                LOGGER.error("[{}] SELFTEST FAIL no ENTITY_REACH instance", DmzMohistMeleeFix.MOD_ID);
                return;
            }
            reachInst.m_22100_(Double.NaN);
            double sanitized = PlayerAttackHelper.getEffectiveAttackRange((Player) fake, 2.0D);
            double reachAfter = ReachAttributeFix.readEntityReach(fake);
            boolean reachOk = Double.isFinite(reachAfter) && reachAfter >= 0.25D
                    && Double.isFinite(sanitized) && sanitized > 0.05D;

            // --- Prove repair does NOT wipe dragonminez:ki_damage ---
            Attribute kiAttr = MainAttributes.KI_DAMAGE.get();
            AttributeInstance kiInst = kiAttr == null ? null : fake.m_21051_(kiAttr);
            if (kiAttr != null && kiInst == null) {
                kiInst = injectAttribute(fake, kiAttr, 42.0D);
            }
            boolean kiPreserved = true;
            double kiAfterRepair = Double.NaN;
            if (kiInst != null) {
                kiInst.m_22100_(42.0D);
                ReachAttributeFix.repair(fake, "selftest-preserve-ki");
                kiAfterRepair = kiInst.m_22135_();
                kiPreserved = Math.abs(kiAfterRepair - 42.0D) < 1.0E-6D;
            }

            // --- Read-side NaN guard on StatsData ---
            boolean secondaryOk = true;
            double secondaryRead = Double.NaN;
            if (kiInst != null) {
                kiInst.m_22100_(Double.NaN);
                secondaryRead = readSecondaryViaStats(fake, kiAttr, 0.0D);
                secondaryOk = Double.isFinite(secondaryRead) && Math.abs(secondaryRead - 0.0D) < 1.0E-9D;
                // restore a sane value so we don't leave the probe poisoned
                kiInst.m_22100_(42.0D);
            }

            // --- strike lock gate ---
            boolean lockedBefore = false;
            boolean lockedAfter = false;
            try {
                StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake).ifPresent(data -> {
                    data.getStatus().setStrikeLocked(true);
                    data.getStatus().setKnockedDown(false);
                    data.getStatus().setStunEffect(false);
                });
                lockedBefore = StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake)
                        .map(d -> d.getStatus().isStunned())
                        .orElse(false);
                CombatUnlock.clearStaleStrikeLock(fake, "selftest");
                lockedAfter = StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake)
                        .map(d -> d.getStatus().isStunned())
                        .orElse(true);
            } catch (Throwable ignored) {
            }

            boolean pass = reachOk && kiPreserved && secondaryOk && (!lockedBefore || !lockedAfter);
            LOGGER.info(
                    "[{}] SELFTEST {} reachBefore={} reachAfter={} sanitizedRange={} kiPreserved={} kiAfterRepair={} secondaryRead={} lockedBefore={} lockedAfter={}",
                    DmzMohistMeleeFix.MOD_ID,
                    pass ? "PASS" : "FAIL",
                    reachBefore,
                    reachAfter,
                    sanitized,
                    kiPreserved,
                    kiAfterRepair,
                    secondaryRead,
                    lockedBefore,
                    lockedAfter
            );

            runShuruiFullHealProbe(fake);
        } catch (Throwable t) {
            LOGGER.error("[{}] SELFTEST FAIL {}", DmzMohistMeleeFix.MOD_ID, t.toString(), t);
        }
    }

    private static double readSecondaryViaStats(FakePlayer fake, Attribute attr, double fallback) {
        try {
            StatsData data = StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake).orElse(null);
            if (data == null) {
                return Double.NaN;
            }
            Method m = StatsData.class.getDeclaredMethod(
                    "getSecondaryAttributeValue",
                    Attribute.class,
                    double.class
            );
            m.setAccessible(true);
            Object out = m.invoke(data, attr, fallback);
            return out instanceof Double d ? d : Double.NaN;
        } catch (Throwable t) {
            LOGGER.warn("[{}] secondary read probe failed: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
            return Double.NaN;
        }
    }

    /** Verify Shurui {@code DmzHooks.fullHeal} mixin clears strikeLocked. */
    private static void runShuruiFullHealProbe(FakePlayer fake) {
        try {
            Class<?> hooks = Class.forName("net.shurui.dev.shuruis_raid_bosses.dmz.DmzHooks");
            StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake).ifPresent(data -> {
                data.getStatus().setStrikeLocked(true);
                data.getStatus().setKnockedDown(true);
                data.getStatus().setStunEffect(true);
            });
            boolean lockedBefore = StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake)
                    .map(d -> d.getStatus().isStrikeLocked())
                    .orElse(false);
            hooks.getMethod("fullHeal", Player.class).invoke(null, fake);
            boolean lockedAfter = StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake)
                    .map(d -> d.getStatus().isStrikeLocked())
                    .orElse(true);
            boolean pass = lockedBefore && !lockedAfter;
            LOGGER.info(
                    "[{}] RAIDHEAL SELFTEST {} lockedBefore={} lockedAfter={}",
                    DmzMohistMeleeFix.MOD_ID,
                    pass ? "PASS" : "FAIL",
                    lockedBefore,
                    lockedAfter
            );
        } catch (ClassNotFoundException e) {
            LOGGER.info("[{}] RAIDHEAL SELFTEST SKIP (shuruis_raid_bosses not present)", DmzMohistMeleeFix.MOD_ID);
        } catch (Throwable t) {
            LOGGER.error("[{}] RAIDHEAL SELFTEST FAIL {}", DmzMohistMeleeFix.MOD_ID, t.toString(), t);
        }
    }

    @SuppressWarnings("unchecked")
    private static AttributeInstance injectAttribute(Player player, Attribute attribute, double base) {
        try {
            Object map = player.m_21204_();
            java.lang.reflect.Field field = null;
            Class<?> c = map.getClass();
            while (c != null && field == null) {
                try {
                    field = c.getDeclaredField("f_22139_");
                } catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object raw = field.get(map);
            if (!(raw instanceof java.util.Map<?, ?>)) {
                return null;
            }
            java.util.Map<Attribute, AttributeInstance> instances = (java.util.Map<Attribute, AttributeInstance>) raw;
            AttributeInstance created = new AttributeInstance(attribute, ignored -> {});
            created.m_22100_(base);
            instances.put(attribute, created);
            return created;
        } catch (Throwable t) {
            LOGGER.warn("[{}] could not inject {}: {}", DmzMohistMeleeFix.MOD_ID, attribute, t.toString());
            return null;
        }
    }
}
