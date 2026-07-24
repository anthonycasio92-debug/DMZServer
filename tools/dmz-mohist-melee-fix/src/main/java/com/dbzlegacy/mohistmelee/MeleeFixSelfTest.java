package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.monster.Zombie;
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
 * Boot probe. Always runs on Mohist; on Forge only with {@code -Ddmz.melee.fix.selftest=true}.
 * Verifies: strikeUnlocked, ENTITY_REACH repair, mixin redirect damage.
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
        LOGGER.info(
                "[{}] Self-test enabled (mohist={} forced={})",
                DmzMohistMeleeFix.MOD_ID,
                mohist,
                forced
        );
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
        ServerLevel level = server.m_129783_();
        Zombie zombie = null;
        try {
            FakePlayer fake = FakePlayerFactory.get(level, new GameProfile(PROBE_UUID, "MeleeFixProbe"));
            fake.m_6034_(8.5D, 130.0D, 8.5D);

            // Simulate the stuck-lock bug: strikeLocked makes Status.isStunned() true.
            StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake).ifPresent(data -> {
                data.getStatus().setStrikeLocked(true);
                data.getStatus().setKnockedDown(false);
                data.getStatus().setStunEffect(false);
            });
            boolean lockedBefore = StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake)
                    .map(d -> d.getStatus().isStunned())
                    .orElse(false);

            // Simulate corrupted Forge ENTITY_REACH (NaN) — DMZ range checks then always fail.
            double reachBeforeBreak = ReachAttributeFix.readEntityReach(fake);
            Attribute entityReach = ForgeMod.ENTITY_REACH.get();
            AttributeInstance reachInst = entityReach == null ? null : fake.m_21051_(entityReach);
            if (reachInst != null) {
                reachInst.m_22100_(Double.NaN);
            }
            double brokenRange = PlayerAttackHelper.getEffectiveAttackRange((Player) fake, 2.0D);
            ReachAttributeFix.repair(fake, "selftest-reach");
            double reachAfter = ReachAttributeFix.readEntityReach(fake);
            double fixedRange = PlayerAttackHelper.getEffectiveAttackRange((Player) fake, 2.0D);
            boolean reachOk = Double.isFinite(reachAfter)
                    && reachAfter >= 0.25D
                    && Double.isFinite(fixedRange)
                    && fixedRange > 0.05D;

            zombie = EntityType.f_20501_.m_20615_(level);
            if (zombie == null) {
                LOGGER.error("[{}] SELFTEST FAIL could not create zombie", DmzMohistMeleeFix.MOD_ID);
                return;
            }
            zombie.m_6034_(9.5D, 130.0D, 8.5D);
            level.m_7967_(zombie);

            float before = zombie.m_21223_();
            DamageBridge.forceClearCombatLocks(fake, "selftest");
            DamageBridge.repairAttacker(fake, "selftest");
            boolean lockedAfter = StatsProvider.get(StatsCapability.INSTANCE, (Entity) fake)
                    .map(d -> d.getStatus().isStunned())
                    .orElse(true);

            boolean mixinHit = invokeMixinRedirect(fake, zombie);
            float after = zombie.m_21223_();
            float delta = before - after;

            boolean pass = delta > 0.05F && !lockedAfter && reachOk;
            LOGGER.info(
                    "[{}] SELFTEST {} lockedBefore={} lockedAfter={} mixinRedirect={} delta={} hp {}->{} reachBefore={} brokenRange={} reachAfter={} fixedRange={}",
                    DmzMohistMeleeFix.MOD_ID,
                    pass ? "PASS" : "FAIL",
                    lockedBefore,
                    lockedAfter,
                    mixinHit,
                    delta,
                    before,
                    after,
                    reachBeforeBreak,
                    brokenRange,
                    reachAfter,
                    fixedRange
            );
        } catch (Throwable t) {
            LOGGER.error("[{}] SELFTEST FAIL {}", DmzMohistMeleeFix.MOD_ID, t.toString(), t);
        } finally {
            if (zombie != null) {
                zombie.m_146870_();
            }
        }
    }

    private static boolean invokeMixinRedirect(ServerPlayer player, Entity target) {
        try {
            Class<?> cls = Class.forName("com.dragonminez.common.network.C2S.CombatAttackRequestC2S");
            for (Method method : cls.getDeclaredMethods()) {
                if (!method.getName().contains("useHurtInsteadOfAttack")) {
                    continue;
                }
                if (method.getParameterTypes().length != 2) {
                    continue;
                }
                method.setAccessible(true);
                method.invoke(null, player, target);
                return true;
            }
            if (target instanceof net.minecraft.world.entity.LivingEntity living) {
                DamageBridge.applyPlayerDamage(
                        player,
                        living,
                        player.m_269291_().m_269075_(player),
                        1.0F
                );
                return true;
            }
            return false;
        } catch (Throwable t) {
            LOGGER.warn("[{}] mixin redirect invoke failed: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
            return false;
        }
    }
}
