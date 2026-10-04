package com.dbzlegacy.mohistmelee;

import java.lang.reflect.Method;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Keeps Dragon Block Noea experimental grab off server-wide (even if an op runs
 * {@code /noea experimental grab on}).
 */
@Mod.EventBusSubscriber(modid = DmzMohistMeleeFix.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class NoeaGrabDisable {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final String GRAB_SERVICE = "com.butterjaffa.noeabosses.GrabService";
    private static volatile boolean noeaGrabPresent;
    private static volatile boolean logged;
    private static int tickCounter;

    static {
        try {
            Class.forName(GRAB_SERVICE, false, NoeaGrabDisable.class.getClassLoader());
            noeaGrabPresent = true;
        } catch (ClassNotFoundException e) {
            noeaGrabPresent = false;
        }
    }

    private NoeaGrabDisable() {}

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        if (!noeaGrabPresent) {
            return;
        }
        forceGrabOff(event.getServer());
        if (!logged) {
            logged = true;
            LOGGER.info("[{}] Dragon Block Noea experimental grab hard-disabled (mixins + server flag).", DmzMohistMeleeFix.MOD_ID);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (!noeaGrabPresent || event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server == null) {
            return;
        }
        if (++tickCounter % 200 != 0) {
            return;
        }
        forceGrabOff(server);
    }

    private static void forceGrabOff(MinecraftServer server) {
        try {
            Class<?> grabService = Class.forName(GRAB_SERVICE);
            Method setEnabled = grabService.getMethod("setEnabled", MinecraftServer.class, boolean.class);
            setEnabled.invoke(null, server, false);
        } catch (ReflectiveOperationException e) {
            LOGGER.warn("[{}] Failed to force Noea grab off: {}", DmzMohistMeleeFix.MOD_ID, e.toString());
        }
    }
}
