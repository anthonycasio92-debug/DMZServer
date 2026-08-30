package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Soft-dependency facade for Fabled / Bukkit / LuckPerms bridges.
 * Never hard-crashes when plugins are missing (same style as CmiGuiBridge).
 */
public final class FabledBridge {
    public static final String PLUGIN_NAME = "Fabled";
    public static final String FABLED_CLASS = "studio.magemonkey.fabled.Fabled";

    private static final AtomicBoolean LOGGED_MISSING = new AtomicBoolean();
    private static volatile Boolean availableCache;
    private static volatile long availableCacheAtMs;

    private FabledBridge() {}

    public static boolean configEnabled() {
        DifficultyConfig cfg = DifficultyConfig.get();
        return cfg != null && cfg.enableFabledBridge;
    }

    public static boolean available() {
        if (!configEnabled()) {
            return false;
        }
        long now = System.currentTimeMillis();
        Boolean cached = availableCache;
        if (cached != null && now - availableCacheAtMs < 3000L) {
            return cached;
        }
        boolean ok = false;
        try {
            Object plugin = getPlugin(PLUGIN_NAME);
            if (plugin != null) {
                Object enabled = plugin.getClass().getMethod("isEnabled").invoke(plugin);
                ok = enabled instanceof Boolean b && b;
            }
        } catch (Throwable ignored) {
            ok = false;
        }
        availableCache = ok;
        availableCacheAtMs = now;
        if (!ok && LOGGED_MISSING.compareAndSet(false, true)) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Fabled bridge idle — plugin not present/enabled",
                    AdaptiveDifficultyMod.MOD_ID);
        }
        return ok;
    }

    public static void onLogin(ServerPlayer player) {
        if (!configEnabled() || player == null) {
            return;
        }
        try {
            if (!available()) {
                return;
            }
            // Immediate catch-up; subsequent ticks refine.
            EnergyManaSync.sync(player);
            StatScreenSync.sync(player);
            TpSpMirror.sync(player);
            RaceSkillSync.ensureRaceSkills();
            RaceClassSync.sync(player);
            PrestigeSkillSync.sync(player);
            PrestigeFactionSync.sync(player);
            ClassPermissionSync.sync(player);
            SystemTelemetry.log(
                    "fabled",
                    "login_sync",
                    player,
                    null,
                    SystemTelemetry.fields("ok", true));
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Fabled login sync skip: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            EnergyManaSync.clear(player.m_20148_());
            TpSpMirror.clear(player.m_20148_());
            ClassPermissionSync.clearTemp(player.m_20148_());
        } catch (Throwable ignored) {
        }
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (!configEnabled() || server == null || !available()) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null || !player.m_6084_()) {
                continue;
            }
            try {
                if (cfg.enableStatScreenSync && tick % 5 == 0) {
                    StatScreenSync.sync(player);
                }
                if (cfg.enableTpSpMirror && tick % 5 == 0) {
                    TpSpMirror.sync(player);
                }
                if (cfg.enableAttrMultiBonus && tick % 20 == 0) {
                    AttrMultiBonus.sync(player);
                }
                if (cfg.enablePrestigeSkillSync && tick % 20 == 0) {
                    PrestigeSkillSync.sync(player);
                }
                if (cfg.enablePrestigeFactionSync && tick % 100 == 0) {
                    PrestigeFactionSync.sync(player);
                }
                if (cfg.enableValueCleaner && tick % 20 == 0) {
                    ValueCleaner.sync(player);
                }
                // Race sync more often so first-login profession is not delayed.
                if (cfg.enableRaceClassSync && tick % 40 == 0) {
                    if (tick % 200 == 0) {
                        RaceSkillSync.ensureRaceSkills();
                    }
                    RaceClassSync.sync(player);
                }
                if (cfg.enableClassPermissionSync && tick % 20 == 0) {
                    ClassPermissionSync.sync(player);
                }
                // Energy last: Fabled updatePlayerStat (from race/class ticks) resets maxMana.
                if (cfg.enableEnergyManaSync) {
                    EnergyManaSync.sync(player, tick % 10 == 0);
                }
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] Fabled pulse skip for {}: {}",
                        AdaptiveDifficultyMod.MOD_ID,
                        player.m_6302_(),
                        t.toString());
            }
        }
    }

    public static String statusSummary() {
        DifficultyConfig c = DifficultyConfig.get();
        return "master=" + onOff(c.enableFabledBridge)
                + " avail=" + onOff(available())
                + " energy=" + onOff(c.enableEnergyManaSync)
                + " stats=" + onOff(c.enableStatScreenSync)
                + " tpsp=" + onOff(c.enableTpSpMirror)
                + " attr=" + onOff(c.enableAttrMultiBonus)
                + " prestige=" + onOff(c.enablePrestigeSkillSync)
                + " faction=" + onOff(c.enablePrestigeFactionSync)
                + " clean=" + onOff(c.enableValueCleaner)
                + " race=" + onOff(c.enableRaceClassSync)
                + " classPerm=" + onOff(c.enableClassPermissionSync);
    }

    static Object getPlugin(String name) throws Exception {
        Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
        Object pm = bukkit.getMethod("getPluginManager").invoke(null);
        return pm.getClass().getMethod("getPlugin", String.class).invoke(pm, name);
    }

    static Object bukkitPlayer(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            Method getBukkit = player.getClass().getMethod("getBukkitEntity");
            Object bp = getBukkit.invoke(player);
            if (bp != null) {
                return bp;
            }
        } catch (Throwable ignored) {
        }
        UUID id = player.m_20148_();
        try {
            Object plugin = getPlugin(PLUGIN_NAME);
            if (plugin != null) {
                ClassLoader pcl = plugin.getClass().getClassLoader();
                Class<?> bukkit = Class.forName("org.bukkit.Bukkit", true, pcl);
                Object byId = bukkit.getMethod("getPlayer", UUID.class).invoke(null, id);
                if (byId != null) {
                    return byId;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object byId = bukkit.getMethod("getPlayer", UUID.class).invoke(null, id);
            if (byId != null) {
                return byId;
            }
            String name = player.m_6302_();
            if (name != null && !name.isBlank()) {
                Object byName = bukkit.getMethod("getPlayerExact", String.class).invoke(null, name);
                if (byName != null) {
                    return byName;
                }
                return bukkit.getMethod("getPlayer", String.class).invoke(null, name);
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    /** Resolve {@code Fabled.getData(OfflinePlayer)} via the plugin classloader. */
    static Object fabledData(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        try {
            Object plugin = getPlugin(PLUGIN_NAME);
            if (plugin == null) {
                return null;
            }
            Object enabled = plugin.getClass().getMethod("isEnabled").invoke(plugin);
            if (!(enabled instanceof Boolean b) || !b) {
                return null;
            }
            Object bukkitPlayer = bukkitPlayer(player);
            if (bukkitPlayer == null) {
                return null;
            }
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> fabledClass = Class.forName(FABLED_CLASS, true, loader);
            Method getData = findUnaryStatic(fabledClass, "getData");
            if (getData == null) {
                return null;
            }
            Object data = getData.invoke(null, bukkitPlayer);
            if (data == null) {
                return null;
            }
            // Do not gate on isInit(): race/energy sync must run before Fabled marks ready,
            // otherwise players sit at ki 0/0 with no class until a manual profess.
            return data;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Class<?> fabledClass() {
        try {
            Object plugin = getPlugin(PLUGIN_NAME);
            if (plugin == null) {
                return null;
            }
            return Class.forName(FABLED_CLASS, true, plugin.getClass().getClassLoader());
        } catch (Throwable ignored) {
            return null;
        }
    }

    static Method findUnaryStatic(Class<?> type, String name) {
        if (type == null || name == null) {
            return null;
        }
        for (Method m : type.getMethods()) {
            if (name.equals(m.getName()) && m.getParameterCount() == 1) {
                return m;
            }
        }
        return null;
    }

    static Method findNoArg(Class<?> type, String name) {
        if (type == null || name == null) {
            return null;
        }
        try {
            return type.getMethod(name);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }

    static double invokeDouble(Object target, String method) {
        if (target == null) {
            return 0.0;
        }
        try {
            Object v = target.getClass().getMethod(method).invoke(target);
            if (v instanceof Number n) {
                return n.doubleValue();
            }
        } catch (Throwable ignored) {
        }
        return 0.0;
    }

    static int invokeInt(Object target, String method) {
        if (target == null) {
            return 0;
        }
        try {
            Object v = target.getClass().getMethod(method).invoke(target);
            if (v instanceof Number n) {
                return n.intValue();
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    static void invokeVoid(Object target, String method, Object... args) {
        if (target == null || method == null) {
            return;
        }
        try {
            Class<?>[] types = new Class<?>[args.length];
            for (int i = 0; i < args.length; i++) {
                Object a = args[i];
                if (a == null) {
                    types[i] = Object.class;
                } else if (a instanceof Integer) {
                    types[i] = int.class;
                } else if (a instanceof Double) {
                    types[i] = double.class;
                } else if (a instanceof Float) {
                    types[i] = float.class;
                } else if (a instanceof Boolean) {
                    types[i] = boolean.class;
                } else {
                    types[i] = a.getClass();
                }
            }
            // Prefer exact match; fall back to scanning.
            try {
                target.getClass().getMethod(method, types).invoke(target, args);
                return;
            } catch (NoSuchMethodException ignored) {
            }
            for (Method m : target.getClass().getMethods()) {
                if (!method.equals(m.getName()) || m.getParameterCount() != args.length) {
                    continue;
                }
                m.invoke(target, args);
                return;
            }
        } catch (Throwable ignored) {
        }
    }

    static void setManaAndMax(Object fabledData, double mana, double maxMana) {
        if (fabledData == null) {
            return;
        }
        setDoubleField(fabledData, "maxMana", maxMana);
        setDoubleField(fabledData, "mana", mana);
        try {
            fabledData.getClass().getMethod("setMana", double.class).invoke(fabledData, mana);
        } catch (Throwable ignored) {
        }
        // Verify maxMana stuck; some loaders block Field.setDouble — retry via set(Object).
        double readMax = invokeDouble(fabledData, "getMaxMana");
        if (maxMana > 0 && readMax + 0.01 < maxMana) {
            setDoubleField(fabledData, "maxMana", maxMana);
            try {
                fabledData.getClass().getMethod("setMana", double.class).invoke(fabledData, mana);
            } catch (Throwable ignored) {
            }
        }
        try {
            fabledData.getClass().getMethod("updateScoreboard").invoke(fabledData);
        } catch (Throwable ignored) {
        }
    }

    static void setDoubleField(Object target, String fieldName, double value) {
        if (target == null || fieldName == null) {
            return;
        }
        Class<?> search = target.getClass();
        while (search != null) {
            try {
                Field f = search.getDeclaredField(fieldName);
                f.setAccessible(true);
                Class<?> type = f.getType();
                if (type == double.class) {
                    f.setDouble(target, value);
                } else if (type == float.class) {
                    f.setFloat(target, (float) value);
                } else if (type == Double.class || type == Float.class || type == Number.class) {
                    f.set(target, value);
                } else {
                    f.set(target, value);
                }
                return;
            } catch (NoSuchFieldException ignored) {
                search = search.getSuperclass();
            } catch (Throwable ignored) {
                return;
            }
        }
    }

    /**
     * Run after the current Bukkit tick so Fabled's {@code updatePlayerStat} cannot leave ki at 0/0.
     */
    static void runOnBukkit(ServerPlayer player, Runnable task) {
        if (player == null || task == null) {
            return;
        }
        try {
            Object plugin = getPlugin(PLUGIN_NAME);
            if (plugin == null) {
                plugin = getPlugin("LegacyMechanicsGUI");
            }
            if (plugin == null) {
                return;
            }
            ClassLoader loader = plugin.getClass().getClassLoader();
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit", true, loader);
            Object scheduler = bukkit.getMethod("getScheduler").invoke(null);
            Class<?> pluginCl = Class.forName("org.bukkit.plugin.Plugin", true, loader);
            scheduler.getClass()
                    .getMethod("runTask", pluginCl, Runnable.class)
                    .invoke(scheduler, plugin, (Runnable) () -> {
                        try {
                            task.run();
                        } catch (Throwable ignored) {
                        }
                    });
        } catch (Throwable ignored) {
        }
    }

    static void logSync(ServerPlayer player, String event, Object... kv) {
        SystemTelemetry.log("fabled", event, player, null, SystemTelemetry.fields(kv));
    }

    private static String onOff(boolean v) {
        return v ? "ON" : "OFF";
    }
}
