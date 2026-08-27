package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code Races.js} — DMZ race → Fabled main class (direct API, no commands).
 */
public final class RaceClassSync {
    private static final String NEXT_KEY = "dmzRaceNextCheck";
    private static final long CHECK_INTERVAL_MS = 5000L;

    private RaceClassSync() {}

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableRaceClassSync) {
            return;
        }
        long now = System.currentTimeMillis();
        long next = ProgressionData.tempGetLong(player, NEXT_KEY, 0L);
        if (now < next) {
            return;
        }
        ProgressionData.tempPut(player, NEXT_KEY, now + CHECK_INTERVAL_MS);

        String dmzRace = DmzProgression.race(player);
        if (dmzRace == null || dmzRace.isBlank()) {
            // DmzProgression.race lowercases; prefer character race string for class name match.
            try {
                var ch = DmzProgression.character(player);
                if (ch != null) {
                    String r = ch.getRace();
                    if (r == null || r.isBlank()) {
                        r = ch.getRaceName();
                    }
                    dmzRace = r == null ? "" : r.trim();
                }
            } catch (Throwable ignored) {
                dmzRace = "";
            }
        } else {
            // Restore original casing when possible for Fabled class lookup.
            try {
                var ch = DmzProgression.character(player);
                if (ch != null) {
                    String r = ch.getRace();
                    if (r == null || r.isBlank()) {
                        r = ch.getRaceName();
                    }
                    if (r != null && !r.isBlank()) {
                        dmzRace = r.trim();
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        if (dmzRace == null || dmzRace.isBlank()) {
            return;
        }

        Object fabledData = FabledBridge.fabledData(player);
        if (fabledData == null) {
            return;
        }
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return;
        }

        Object targetClass = resolveRegisteredClass(fabledClass, dmzRace);
        if (targetClass == null) {
            return;
        }

        String targetClassName = "";
        try {
            Object n = targetClass.getClass().getMethod("getName").invoke(targetClass);
            targetClassName = n == null ? "" : String.valueOf(n);
        } catch (Throwable ignored) {
        }

        String currentClassName = "";
        try {
            Object main = fabledData.getClass().getMethod("getMainClass").invoke(fabledData);
            if (main != null) {
                Object classData = main.getClass().getMethod("getData").invoke(main);
                if (classData != null) {
                    Object n = classData.getClass().getMethod("getName").invoke(classData);
                    currentClassName = n == null ? "" : String.valueOf(n);
                }
            }
        } catch (Throwable ignored) {
        }

        if (!currentClassName.isBlank() && equalsIgnoreCase(currentClassName, targetClassName)) {
            return;
        }

        Object bukkitPlayer = FabledBridge.bukkitPlayer(player);
        Object changed;
        try {
            Method setClass = null;
            for (Method m : fabledData.getClass().getMethods()) {
                if ("setClass".equals(m.getName()) && m.getParameterCount() == 3) {
                    setClass = m;
                    break;
                }
            }
            if (setClass == null) {
                return;
            }
            changed = setClass.invoke(fabledData, null, targetClass, true);
        } catch (Throwable t) {
            return;
        }
        if (changed == null) {
            return;
        }

        if (bukkitPlayer != null) {
            FabledBridge.invokeVoid(fabledData, "updatePlayerStat", bukkitPlayer);
            FabledBridge.invokeVoid(fabledData, "updateHealth", bukkitPlayer);
            FabledBridge.invokeVoid(fabledData, "updateWalkSpeed", bukkitPlayer);
            try {
                fabledData.getClass().getMethod("startPassives", bukkitPlayer.getClass()).invoke(fabledData, bukkitPlayer);
            } catch (Throwable t) {
                FabledBridge.invokeVoid(fabledData, "startPassives", bukkitPlayer);
            }
        }
        try {
            fabledData.getClass().getMethod("updateScoreboard").invoke(fabledData);
        } catch (Throwable ignored) {
        }

        FabledBridge.logSync(player, "race_class", "race", dmzRace, "class", targetClassName);
    }

    private static Object resolveRegisteredClass(Class<?> fabledClass, String dmzRace) {
        Method getClassMethod = FabledBridge.findUnaryStatic(fabledClass, "getClass");
        if (getClassMethod != null) {
            Object found = tryInvoke(getClassMethod, dmzRace);
            if (found == null) {
                found = tryInvoke(getClassMethod, formatRaceName(dmzRace));
            }
            if (found == null) {
                found = tryInvoke(getClassMethod, dmzRace.toLowerCase(Locale.ROOT));
            }
            if (found != null) {
                return found;
            }
        }
        try {
            Method getClasses = FabledBridge.findNoArg(fabledClass, "getClasses");
            if (getClasses == null) {
                return null;
            }
            Object registered = getClasses.invoke(null);
            if (!(registered instanceof Map<?, ?> map)) {
                return null;
            }
            Collection<?> values = map.values();
            Iterator<?> it = values.iterator();
            while (it.hasNext()) {
                Object registeredClass = it.next();
                if (registeredClass == null) {
                    continue;
                }
                Object name = registeredClass.getClass().getMethod("getName").invoke(registeredClass);
                if (name != null && equalsIgnoreCase(String.valueOf(name), dmzRace)) {
                    return registeredClass;
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static Object tryInvoke(Method m, String arg) {
        try {
            return m.invoke(null, arg);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static String formatRaceName(String race) {
        String r = race == null ? "" : race.trim().toLowerCase(Locale.ROOT);
        if (r.isEmpty()) {
            return "";
        }
        return Character.toUpperCase(r.charAt(0)) + r.substring(1);
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
