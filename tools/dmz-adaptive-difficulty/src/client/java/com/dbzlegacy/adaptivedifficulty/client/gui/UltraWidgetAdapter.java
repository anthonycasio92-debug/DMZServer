package com.dbzlegacy.adaptivedifficulty.client.gui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.util.List;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * The only place that names DMZUltra UI classes. Screens call these methods
 * and never mention {@code com.dmzultra}. Missing classes fall back to plain widgets.
 */
@OnlyIn(Dist.CLIENT)
public final class UltraWidgetAdapter {
    private static final String THEME = "com.dmzultra.client.ui.UltraUi$Theme";
    private static final String BUTTON = "com.dmzultra.client.ui.UltraButton";
    private static final String LIST = "com.dmzultra.client.ui.UltraList";
    private static Boolean available;

    private UltraWidgetAdapter() {}

    public static boolean ultraAvailable() {
        if (available == null) {
            available = classExists(THEME) && classExists(BUTTON);
        }
        return available;
    }

    public static Object forgeTheme() {
        try {
            Class<?> theme = Class.forName(THEME);
            Field forge = theme.getField("FORGE");
            return forge.get(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static int backgroundColor(Object theme) {
        if (theme == null) {
            return 0xF0101018;
        }
        try {
            Object palette = firstField(theme, "palette", "colors");
            Object source = palette != null ? palette : theme;
            Integer color = colorField(source, "background", "backgroundColor", "bg", "panel");
            if (color != null) {
                return color;
            }
        } catch (Throwable ignored) {
            // plain fill
        }
        return 0xF0101018;
    }

    public static GuiEventListener button(
            int x, int y, int w, int h, String label, Runnable onPress, Object theme) {
        GuiEventListener ultra = ultraWidget(BUTTON, x, y, w, h, label, onPress, theme, null);
        if (ultra != null) {
            return ultra;
        }
        Runnable press = onPress == null ? () -> { } : onPress;
        return Button.m_253074_(Component.m_237113_(label), ignored -> press.run())
                .m_252987_(x, y, w, h)
                .m_253136_();
    }

    public static GuiEventListener list(
            int x, int y, int w, int h, List<String> rows, Object theme) {
        return ultraWidget(LIST, x, y, w, h, null, null, theme, rows);
    }

    private static GuiEventListener ultraWidget(
            String className,
            int x,
            int y,
            int w,
            int h,
            String label,
            Runnable onPress,
            Object theme,
            List<String> rows) {
        if (!classExists(className)) {
            return null;
        }
        try {
            Class<?> type = Class.forName(className);
            Component text = label == null ? null : Component.m_237113_(label);
            for (Constructor<?> ctor : type.getConstructors()) {
                Object made = tryConstruct(ctor, x, y, w, h, label, text, onPress, theme, rows);
                if (made instanceof GuiEventListener listener) {
                    return listener;
                }
            }
        } catch (Throwable ignored) {
            return null;
        }
        return null;
    }

    private static Object tryConstruct(
            Constructor<?> ctor,
            int x,
            int y,
            int w,
            int h,
            String label,
            Component text,
            Runnable onPress,
            Object theme,
            List<String> rows) {
        Class<?>[] params = ctor.getParameterTypes();
        Object[] args = new Object[params.length];
        int[] ints = {x, y, w, h};
        int intsUsed = 0;
        for (int i = 0; i < params.length; i++) {
            Class<?> param = params[i];
            if (param == int.class || param == Integer.class) {
                if (intsUsed >= ints.length) {
                    return null;
                }
                args[i] = ints[intsUsed++];
            } else if (param == String.class) {
                args[i] = label == null ? "" : label;
            } else if (text != null && param.isInstance(text)) {
                args[i] = text;
            } else if (rows != null && param.isInstance(rows)) {
                args[i] = rows;
            } else if (onPress != null && param.isInstance(onPress)) {
                args[i] = onPress;
            } else if (theme != null && param.isInstance(theme)) {
                args[i] = theme;
            } else if (onPress != null && param.isInterface()) {
                args[i] = pressProxy(param, onPress);
            } else {
                return null;
            }
        }
        try {
            return ctor.newInstance(args);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object pressProxy(Class<?> param, Runnable onPress) {
        InvocationHandler handler = (proxy, method, methodArgs) -> {
            if (method.getDeclaringClass() == Object.class) {
                if ("equals".equals(method.getName())) {
                    return proxy == (methodArgs == null ? null : methodArgs[0]);
                }
                if ("hashCode".equals(method.getName())) {
                    return System.identityHashCode(proxy);
                }
                return method.getName();
            }
            onPress.run();
            Class<?> ret = method.getReturnType();
            if (ret == boolean.class) {
                return false;
            }
            if (ret == int.class) {
                return 0;
            }
            return null;
        };
        return Proxy.newProxyInstance(param.getClassLoader(), new Class<?>[] {param}, handler);
    }

    private static boolean classExists(String name) {
        try {
            Class.forName(name, false, UltraWidgetAdapter.class.getClassLoader());
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static Object firstField(Object target, String... names) {
        for (String name : names) {
            try {
                Field field = target.getClass().getField(name);
                Object value = field.get(target);
                if (value != null) {
                    return value;
                }
            } catch (Throwable ignored) {
                // try the next name
            }
        }
        return null;
    }

    private static Integer colorField(Object target, String... names) {
        for (String name : names) {
            try {
                Field field = target.getClass().getField(name);
                Object value = field.get(target);
                if (value instanceof Integer color) {
                    return color;
                }
                if (value instanceof Number number) {
                    return number.intValue();
                }
            } catch (Throwable ignored) {
                // try the next name
            }
        }
        return null;
    }
}
