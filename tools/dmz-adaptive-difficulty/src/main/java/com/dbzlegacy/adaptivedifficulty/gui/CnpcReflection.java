package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Reflection helpers for CustomNPCs GUI API (server-side, no compile-time CNPC dep). */
final class CnpcReflection {
    private CnpcReflection() {}

    static boolean available() {
        try {
            Class<?> api = Class.forName("noppes.npcs.api.NpcAPI");
            Object ok = api.getMethod("IsAvailable").invoke(null);
            return ok instanceof Boolean b && b;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static Object wrapPlayer(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            Class<?> api = Class.forName("noppes.npcs.api.NpcAPI");
            Object instance = api.getMethod("Instance").invoke(null);
            Object entity = api.getMethod("getIEntity", Entity.class).invoke(instance, player);
            Class<?> iPlayer = Class.forName("noppes.npcs.api.entity.IPlayer");
            if (iPlayer.isInstance(entity)) {
                return entity;
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] CNPC wrapPlayer failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
        return null;
    }

    static Object createGui(int id, int width, int height, boolean pause, Object iPlayer) throws ReflectiveOperationException {
        Class<?> api = Class.forName("noppes.npcs.api.NpcAPI");
        Object instance = api.getMethod("Instance").invoke(null);
        Class<?> iPlayerClass = Class.forName("noppes.npcs.api.entity.IPlayer");
        return api.getMethod("createCustomGui", int.class, int.class, int.class, boolean.class, iPlayerClass)
                .invoke(instance, id, width, height, pause, iPlayer);
    }

    static void showCustomGui(Object iPlayer, Object gui) throws ReflectiveOperationException {
        Class<?> iGui = Class.forName("noppes.npcs.api.gui.ICustomGui");
        iPlayer.getClass().getMethod("showCustomGui", iGui).invoke(iPlayer, gui);
    }

    static void closeGui(Object gui) {
        if (gui == null) {
            return;
        }
        try {
            gui.getClass().getMethod("close").invoke(gui);
        } catch (Throwable ignored) {
        }
    }

    static void addLabel(Object gui, int id, String text, int x, int y, int w, int h) throws ReflectiveOperationException {
        gui.getClass()
                .getMethod("addLabel", int.class, String.class, int.class, int.class, int.class, int.class)
                .invoke(gui, id, text, x, y, w, h);
    }

    static void addButton(Object gui, int id, String label, int x, int y, int w, int h, Runnable onPress)
            throws ReflectiveOperationException {
        Object button = gui.getClass()
                .getMethod("addButton", int.class, String.class, int.class, int.class, int.class, int.class)
                .invoke(gui, id, label, x, y, w, h);
        Class<?> clickType = Class.forName("noppes.npcs.api.function.gui.GuiComponentClicked");
        Object handler = Proxy.newProxyInstance(
                clickType.getClassLoader(),
                new Class<?>[] {clickType},
                (proxy, method, args) -> {
                    if ("onClick".equals(method.getName()) && args != null && args.length >= 1) {
                        closeGui(args[0]);
                        onPress.run();
                    }
                    return null;
                });
        button.getClass().getMethod("setOnPress", clickType).invoke(button, handler);
    }

    static void setClosesOnEsc(Object gui, boolean value) throws ReflectiveOperationException {
        gui.getClass().getMethod("setClosesOnEsc", boolean.class).invoke(gui, value);
    }
}
