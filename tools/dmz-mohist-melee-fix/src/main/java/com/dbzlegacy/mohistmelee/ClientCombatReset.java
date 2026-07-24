package com.dbzlegacy.mohistmelee;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-only: flush DMZ MinecraftMixin upswing / attackCooldown state that can
 * stick when the local player is briefly null. Death "fixes" melee partly by
 * recreating LocalPlayer — we cancel upswing on login/clone instead.
 */
public final class ClientCombatReset {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static int resets;

    private ClientCombatReset() {}

    public static void registerIfClient() {
        if (!isClientDist()) {
            return;
        }
        MinecraftForge.EVENT_BUS.register(new ClientCombatReset());
        LOGGER.info("[{}] client combat reset hooks registered", DmzMohistMeleeFix.MOD_ID);
    }

    private static boolean isClientDist() {
        try {
            Class<?> env = Class.forName("net.minecraftforge.fml.loading.FMLEnvironment");
            Object dist = env.getField("dist").get(null);
            return dist != null && "CLIENT".equals(String.valueOf(dist));
        } catch (Throwable t) {
            return false;
        }
    }

    @SubscribeEvent
    public void onEvent(Event event) {
        String name = event.getClass().getName();
        if (!name.startsWith("net.minecraftforge.client.event.ClientPlayerNetworkEvent$")) {
            return;
        }
        if (!(name.endsWith("$LoggingIn") || name.endsWith("$Clone"))) {
            return;
        }
        cancelUpswingReflect("client-" + event.getClass().getSimpleName());
    }

    public static void cancelUpswingReflect(String reason) {
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.Minecraft");
            Object mc = mcClass.getMethod("m_91087_").invoke(null); // getInstance
            if (mc instanceof com.dragonminez.common.combat.util.Minecraft_DMZ) {
                ((com.dragonminez.common.combat.util.Minecraft_DMZ) mc).cancelUpswing();
                int n = ++resets;
                if (n <= 20) {
                    LOGGER.info("[{}] cancelled client upswing reason={}", DmzMohistMeleeFix.MOD_ID, reason);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
