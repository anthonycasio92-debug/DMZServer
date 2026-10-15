package com.dmzlegacy.kaipath.ultra.consumers.legacymechanics;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Stock KaiPath 1.3.17 {@code LmDynamicScreen.renderPreview} always draws
 * {@code Minecraft.player}. LegacyMechanics already sends {@code playerModel.subject}
 * ({@code uuid:<id>}) for an inspect subject. This hook is injected at the start of that
 * method: when the token names someone else, draw that player and skip the local-player path.
 */
public final class InspectPreview {
    private InspectPreview() {}

    /**
     * @return {@code true} when this call owned the preview (subject drawn, or suppressed so the
     *     viewer is not substituted). {@code false} leaves stock behavior — the local player.
     */
    public static boolean tryRender(LmDynamicScreen screen, GuiGraphics graphics) {
        if (screen == null || graphics == null) {
            return false;
        }
        Object preview = field(screen, "preview");
        if (preview == null) {
            return false;
        }
        String token;
        try {
            token = InspectPreviewSubjects.tokenFromLayout(layoutJson(screen));
        } catch (Throwable t) {
            return false;
        }
        UUID subjectId = subjectId(token);
        if (subjectId == null) {
            return false;
        }
        Object mc = field(screen, "f_96541_");
        Object local = mc == null ? null : field(mc, "f_91074_");
        UUID self = entityUuid(local);
        if (self != null && self.equals(subjectId)) {
            return false;
        }
        try {
            Object entity = local == null ? null : resolve(mc, local, subjectId);
            if (entity != null) {
                draw(screen, graphics, preview, entity);
            }
        } catch (Throwable ignored) {
            // A failed subject draw must not fall through to the staff member's model.
        }
        return true;
    }

    private static String layoutJson(Object screen) throws Exception {
        Object packet = field(screen, "packet");
        if (packet == null) {
            return "";
        }
        Method layout = packet.getClass().getMethod("layoutJson");
        Object json = layout.invoke(packet);
        return json instanceof String s ? s : "";
    }

    private static UUID subjectId(String token) {
        if (token == null || token.isBlank() || "local".equalsIgnoreCase(token)) {
            return null;
        }
        String id = token;
        if (id.regionMatches(true, 0, "uuid:", 0, 5)) {
            id = id.substring(5).trim();
        }
        try {
            return UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static Object resolve(Object mc, Object local, UUID id) throws Exception {
        Object level = invoke(local, "m_9236_");
        if (level != null) {
            Object found = invoke(level, "m_46003_", new Class<?>[] {UUID.class}, new Object[] {id});
            if (found != null) {
                return found;
            }
        }
        Object profile = profile(mc, id);
        if (level == null || profile == null) {
            return null;
        }
        Class<?> remote = Class.forName("net.minecraft.client.player.RemotePlayer");
        for (Constructor<?> ctor : remote.getDeclaredConstructors()) {
            Class<?>[] params = ctor.getParameterTypes();
            if (params.length == 2 && params[1].isInstance(profile)) {
                ctor.setAccessible(true);
                return ctor.newInstance(level, profile);
            }
        }
        return null;
    }

    private static Object profile(Object mc, UUID id) throws Exception {
        Object conn = invoke(mc, "m_91403_");
        if (conn != null) {
            Object info = invoke(conn, "m_104949_", new Class<?>[] {UUID.class}, new Object[] {id});
            if (info != null) {
                Object profile = invoke(info, "m_105312_");
                if (profile != null) {
                    return profile;
                }
            }
        }
        Class<?> profiles = Class.forName("com.mojang.authlib.GameProfile");
        return profiles.getConstructor(UUID.class, String.class).newInstance(id, "player");
    }

    /**
     * Same pose / scissor / scale math as stock {@code renderPreview}, but the entity is the
     * inspect subject ({@code LivingEntity}), not {@code LocalPlayer}.
     */
    private static void draw(Object screen, Object graphics, Object preview, Object entity) throws Exception {
        float body = getFloat(entity, "f_20883_");
        float yRot = callFloat(entity, "m_146908_");
        float xRot = callFloat(entity, "m_146909_");
        float head = getFloat(entity, "f_20885_");
        float headO = getFloat(entity, "f_20886_");
        int x = intField(screen, "panelX") + callInt(preview, "x");
        int y = intField(screen, "panelY") + callInt(preview, "y");
        int width = callInt(preview, "width");
        int height = callInt(preview, "height");
        boolean framed = callBool(preview, "frame");
        boolean rotate = callBool(preview, "rotate");
        float previewScale = callFloat(preview, "scale");
        boolean scissored = false;
        try {
            if (framed) {
                invoke(
                        graphics,
                        "m_280637_",
                        new Class<?>[] {int.class, int.class, int.class, int.class, int.class},
                        new Object[] {x, y, width, height, intField(screen, "titleTint")});
            }
            invoke(
                    graphics,
                    "m_280588_",
                    new Class<?>[] {int.class, int.class, int.class, int.class},
                    new Object[] {x, y, x + width, y + height});
            scissored = true;
            float spin = rotate ? (System.nanoTime() / 1.0E9f) * 0.35f : 0f;
            float fit =
                    Math.min(
                            width / Math.max(0.1f, callFloat(entity, "m_20205_")),
                            height / Math.max(0.1f, callFloat(entity, "m_20206_")));
            int scale = Math.max(1, (int) (fit * 0.85f * previewScale));
            setFloat(entity, "f_20883_", 0f);
            callVoidFloat(entity, "m_146922_", 0f);
            callVoidFloat(entity, "m_146926_", 0f);
            setFloat(entity, "f_20885_", 0f);
            setFloat(entity, "f_20886_", 0f);
            Object bodyQuat = quat(spin);
            Object extraQuat = Class.forName("org.joml.Quaternionf").getConstructor().newInstance();
            Class<?> inv = Class.forName("net.minecraft.client.gui.screens.inventory.InventoryScreen");
            Method render = null;
            for (Method method : inv.getMethods()) {
                if ("m_280432_".equals(method.getName()) && method.getParameterCount() == 7) {
                    render = method;
                    break;
                }
            }
            if (render == null) {
                throw new NoSuchMethodException("InventoryScreen.m_280432_");
            }
            render.invoke(
                    null,
                    graphics,
                    x + width / 2,
                    y + height - 4,
                    scale,
                    bodyQuat,
                    extraQuat,
                    entity);
        } finally {
            setFloat(entity, "f_20883_", body);
            callVoidFloat(entity, "m_146922_", yRot);
            callVoidFloat(entity, "m_146926_", xRot);
            setFloat(entity, "f_20885_", head);
            setFloat(entity, "f_20886_", headO);
            if (scissored) {
                try {
                    invoke(graphics, "m_280618_", new Class<?>[] {}, new Object[] {});
                } catch (Throwable ignored) {
                    // scissor pop is best-effort after a failed draw
                }
            }
        }
    }

    private static Object quat(float spin) throws Exception {
        Class<?> type = Class.forName("org.joml.Quaternionf");
        Object quat = type.getConstructor().newInstance();
        Object turned = type.getMethod("rotationZ", float.class).invoke(quat, (float) Math.PI);
        type.getMethod("rotateY", float.class).invoke(turned, spin);
        return turned;
    }

    private static UUID entityUuid(Object entity) {
        if (entity == null) {
            return null;
        }
        Object id = invokeQuiet(entity, "m_20148_");
        if (!(id instanceof UUID)) {
            id = invokeQuiet(entity, "getUUID");
        }
        return id instanceof UUID uuid ? uuid : null;
    }

    private static Object field(Object target, String name) {
        if (target == null || name == null) {
            return null;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field found = type.getDeclaredField(name);
                found.setAccessible(true);
                return found.get(target);
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                return null;
            }
        }
        return null;
    }

    private static int intField(Object target, String name) {
        Object value = field(target, name);
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static void setFloat(Object target, String name, float value) {
        if (target == null) {
            return;
        }
        Class<?> type = target.getClass();
        while (type != null) {
            try {
                Field found = type.getDeclaredField(name);
                found.setAccessible(true);
                found.setFloat(target, value);
                return;
            } catch (NoSuchFieldException e) {
                type = type.getSuperclass();
            } catch (IllegalAccessException e) {
                return;
            }
        }
    }

    private static float getFloat(Object target, String name) {
        Object value = field(target, name);
        return value instanceof Number n ? n.floatValue() : 0f;
    }

    private static int callInt(Object target, String name) throws Exception {
        Object value = target.getClass().getMethod(name).invoke(target);
        return value instanceof Number n ? n.intValue() : 0;
    }

    private static float callFloat(Object target, String name) throws Exception {
        Method method = findMethod(target.getClass(), name, 0);
        Object value = method.invoke(target);
        return value instanceof Number n ? n.floatValue() : 0f;
    }

    private static boolean callBool(Object target, String name) throws Exception {
        Object value = target.getClass().getMethod(name).invoke(target);
        return value instanceof Boolean b && b;
    }

    private static void callVoidFloat(Object target, String name, float value) throws Exception {
        Method method = findMethod(target.getClass(), name, 1);
        method.invoke(target, value);
    }

    private static Method findMethod(Class<?> type, String name, int args) throws NoSuchMethodException {
        Class<?> cursor = type;
        while (cursor != null) {
            for (Method method : cursor.getDeclaredMethods()) {
                if (method.getName().equals(name) && method.getParameterCount() == args) {
                    method.setAccessible(true);
                    return method;
                }
            }
            cursor = cursor.getSuperclass();
        }
        throw new NoSuchMethodException(name);
    }

    private static Object invoke(Object target, String name, Class<?>[] types, Object[] args) throws Exception {
        Method method = target.getClass().getMethod(name, types);
        return method.invoke(target, args);
    }

    private static Object invoke(Object target, String name) throws Exception {
        return invoke(target, name, new Class<?>[] {}, new Object[] {});
    }

    private static Object invokeQuiet(Object target, String name) {
        try {
            return invoke(target, name);
        } catch (Throwable t) {
            return null;
        }
    }
}
