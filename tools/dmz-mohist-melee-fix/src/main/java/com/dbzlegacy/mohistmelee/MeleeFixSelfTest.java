package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
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
 * Boot probe: break ENTITY_REACH to NaN, confirm repair + usable DMZ range.
 * Always on Mohist; elsewhere with {@code -Ddmz.melee.fix.selftest=true}.
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

            double before = ReachAttributeFix.readEntityReach(fake);
            Attribute attr = ForgeMod.ENTITY_REACH.get();
            AttributeInstance inst = attr == null ? null : fake.m_21051_(attr);
            if (inst == null) {
                LOGGER.error("[{}] SELFTEST FAIL no ENTITY_REACH instance", DmzMohistMeleeFix.MOD_ID);
                return;
            }

            inst.m_22100_(Double.NaN);
            // Triggers mixin sanitize + repair
            double sanitized = PlayerAttackHelper.getEffectiveAttackRange((Player) fake, 2.0D);
            double after = ReachAttributeFix.readEntityReach(fake);

            boolean pass = Double.isFinite(after)
                    && after >= 0.25D
                    && Double.isFinite(sanitized)
                    && sanitized > 0.05D;

            LOGGER.info(
                    "[{}] SELFTEST {} reachBefore={} reachAfter={} sanitizedRange={}",
                    DmzMohistMeleeFix.MOD_ID,
                    pass ? "PASS" : "FAIL",
                    before,
                    after,
                    sanitized
            );
        } catch (Throwable t) {
            LOGGER.error("[{}] SELFTEST FAIL {}", DmzMohistMeleeFix.MOD_ID, t.toString(), t);
        }
    }
}
