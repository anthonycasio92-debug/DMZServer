package com.dbzlegacy.mohistmelee;

import com.mojang.authlib.GameProfile;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Optional boot probe ({@code -Ddmz.melee.fix.selftest=true}).
 */
public final class MeleeFixSelfTest {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final UUID PROBE_UUID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    private MeleeFixSelfTest() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean("dmz.melee.fix.selftest")) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new MeleeFixSelfTest());
        LOGGER.info("[{}] Self-test enabled (-Ddmz.melee.fix.selftest=true)", DmzMohistMeleeFix.MOD_ID);
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

            zombie = EntityType.f_20501_.m_20615_(level);
            if (zombie == null) {
                LOGGER.error("[{}] SELFTEST FAIL could not create zombie", DmzMohistMeleeFix.MOD_ID);
                return;
            }
            zombie.m_6034_(9.5D, 130.0D, 8.5D);
            level.m_7967_(zombie);

            float before = zombie.m_21223_();
            boolean mixinHit = invokeMixinRedirect(fake, zombie);
            float after = zombie.m_21223_();
            float delta = before - after;

            boolean pass = delta > 0.05F;
            LOGGER.info(
                    "[{}] SELFTEST {} mixinRedirect={} delta={} hp {}->{}",
                    DmzMohistMeleeFix.MOD_ID,
                    pass ? "PASS" : "FAIL",
                    mixinHit,
                    delta,
                    before,
                    after
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
            LOGGER.warn("[{}] mixin redirect method not found on CombatAttackRequestC2S", DmzMohistMeleeFix.MOD_ID);
            return false;
        } catch (Throwable t) {
            LOGGER.warn("[{}] mixin redirect invoke failed: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
            return false;
        }
    }
}
