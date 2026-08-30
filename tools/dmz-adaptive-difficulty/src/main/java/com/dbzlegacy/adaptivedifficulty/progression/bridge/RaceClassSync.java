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
 * Port of {@code Races.js} — DMZ race → Fabled main class (direct API, no commands),
 * plus auto race-skill ensure/grant via {@link RaceSkillSync}.
 */
public final class RaceClassSync {
    private static final String NEXT_KEY = "dmzRaceNextCheck";
    private static final long CHECK_INTERVAL_MS = 2000L;

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

        String dmzRace = resolveRaceName(player);
        if (dmzRace == null || dmzRace.isBlank()) {
            return;
        }

        Object fabledData = FabledBridge.fabledData(player);
        if (fabledData == null) {
            // Not ready yet — retry sooner.
            ProgressionData.tempPut(player, NEXT_KEY, now + 500L);
            return;
        }
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return;
        }

        // Always keep race skills in sync (even when class already matches).
        RaceSkillSync.sync(player, dmzRace);

        Object targetClass = resolveRegisteredClass(fabledClass, dmzRace);
        if (targetClass == null) {
            FabledBridge.logSync(player, "race_class_miss", "race", dmzRace);
            return;
        }

        String targetClassName = "";
        try {
            Object n = targetClass.getClass().getMethod("getName").invoke(targetClass);
            targetClassName = n == null ? "" : String.valueOf(n);
        } catch (Throwable ignored) {
        }

        String currentClassName = "";
        Object currentClassData = null;
        try {
            Object main = fabledData.getClass().getMethod("getMainClass").invoke(fabledData);
            if (main != null) {
                currentClassData = main.getClass().getMethod("getData").invoke(main);
                if (currentClassData != null) {
                    Object n = currentClassData.getClass().getMethod("getName").invoke(currentClassData);
                    currentClassName = n == null ? "" : String.valueOf(n);
                }
            }
        } catch (Throwable ignored) {
        }

        if (!currentClassName.isBlank() && equalsIgnoreCase(currentClassName, targetClassName)) {
            return;
        }

        Object bukkitPlayer = FabledBridge.bukkitPlayer(player);
        Object changed = null;
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
            // Prefer (previous, next, force) when we know the previous FabledClass.
            changed = setClass.invoke(fabledData, currentClassData, targetClass, true);
            if (changed == null && currentClassData != null) {
                changed = setClass.invoke(fabledData, null, targetClass, true);
            }
        } catch (Throwable t) {
            FabledBridge.logSync(player, "race_class_err", "race", dmzRace, "err", String.valueOf(t));
            ProgressionData.tempPut(player, NEXT_KEY, now + 1000L);
            return;
        }
        if (changed == null) {
            FabledBridge.logSync(player, "race_class_reject", "race", dmzRace, "class", targetClassName);
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

        // setClass/updatePlayerStat reset maxMana from class mana (often 0) — restore DMZ ki.
        EnergyManaSync.sync(player, true);

        FabledBridge.logSync(player, "race_class", "race", dmzRace, "class", targetClassName);
    }

    private static String resolveRaceName(ServerPlayer player) {
        String dmzRace = DmzProgression.race(player);
        if (dmzRace == null || dmzRace.isBlank()) {
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
        return stripNamespace(dmzRace);
    }

    private static String stripNamespace(String raw) {
        if (raw == null) {
            return "";
        }
        String r = raw.trim();
        int colon = r.lastIndexOf(':');
        if (colon >= 0 && colon < r.length() - 1) {
            r = r.substring(colon + 1).trim();
        }
        return r;
    }

    private static Object resolveRegisteredClass(Class<?> fabledClass, String dmzRace) {
        Method getClassMethod = FabledBridge.findUnaryStatic(fabledClass, "getClass");
        String[] candidates = new String[] {
                dmzRace,
                formatRaceName(dmzRace),
                dmzRace.toLowerCase(Locale.ROOT),
                dmzRace.toUpperCase(Locale.ROOT)
        };
        if (getClassMethod != null) {
            for (String c : candidates) {
                if (c == null || c.isBlank()) {
                    continue;
                }
                Object found = tryInvoke(getClassMethod, c);
                if (found != null) {
                    return found;
                }
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
            // Also match map keys (sometimes keyed differently from getName).
            for (Map.Entry<?, ?> e : map.entrySet()) {
                if (e.getKey() != null && equalsIgnoreCase(String.valueOf(e.getKey()), dmzRace)) {
                    return e.getValue();
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
