package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.init.MainAttributes;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Lightweight boot probe: intentional reset must not be undone by primary snapshot restore.
 * No world entity spawns.
 */
public final class ResetSelfTest {
    private static final UUID PROBE_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private ResetSelfTest() {}

    public static void registerIfEnabled() {
        boolean mohist = isMohist();
        boolean forced = Boolean.getBoolean("dmz.melee.fix.selftest");
        if (!mohist && !forced) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new ResetSelfTest());
        RateLog.logger().info("[{}] Reset self-test enabled (mohist={} forced={})", DmzMohistMeleeFix.MOD_ID, mohist, forced);
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
            Attribute strAttr = MainAttributes.STRENGTH.get();
            if (strAttr == null) {
                RateLog.logger().warn("[{}] RESET SELFTEST SKIP (no STR attr)", DmzMohistMeleeFix.MOD_ID);
                return;
            }
            AttributeInstance strInst = fake.m_21051_(strAttr);
            if (strInst == null) {
                strInst = CombatRepair.injectAttribute((Player) fake, strAttr, 250.0D);
            }
            if (strInst == null) {
                RateLog.logger().error("[{}] RESET SELFTEST FAIL could not inject STR", DmzMohistMeleeFix.MOD_ID);
                return;
            }

            strInst.m_22100_(250.0D);
            CombatRepair.snapshotPrimaries((Player) fake);

            // Simulate DMZ full reset path: clear snapshot + suppress, write 0, adopt zeros.
            CombatRepair.beginIntentionalReset((Player) fake, 5);
            strInst.m_22100_(0.0D);
            CombatRepair.recordPrimaryWrite((Player) fake, strAttr, 0);
            CombatRepair.endIntentionalReset((Player) fake, null, 5);

            boolean restoredWhile = CombatRepair.ensurePrimaries((Player) fake, "selftest-reset-should-not-restore");
            for (int i = 0; i < 8; i++) {
                CombatRepair.tickSuppress((Player) fake);
            }
            boolean restoredAfter = CombatRepair.ensurePrimaries((Player) fake, "selftest-reset-after-suppress");
            int saved = CombatRepair.savedFor((Player) fake, strAttr);
            double live = strInst.m_22115_();

            boolean pass = !restoredWhile && !restoredAfter && saved <= 0 && live <= 0.0D;
            RateLog.logger().info(
                    "[{}] RESET SELFTEST {} restoredWhile={} restoredAfter={} saved={} live={}",
                    DmzMohistMeleeFix.MOD_ID,
                    pass ? "PASS" : "FAIL",
                    restoredWhile,
                    restoredAfter,
                    saved,
                    live
            );
        } catch (Throwable t) {
            RateLog.logger().error("[{}] RESET SELFTEST FAIL {}", DmzMohistMeleeFix.MOD_ID, t.toString());
        }
    }
}
