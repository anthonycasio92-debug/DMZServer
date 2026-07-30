package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.entity.Player;

/** Reflects into the Forge mod for live values / actions (Mohist shared JVM). */
public final class ForgeBridge {
    private static final Set<UUID> ADMIN = new HashSet<>();

    private ForgeBridge() {}

    public static Object nmsPlayer(Player player) {
        try {
            return player.getClass().getMethod("getHandle").invoke(player);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Map<String, String> placeholders(Player player) {
        Map<String, String> out = new HashMap<>();
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return out;
        }
        try {
            Class<?> sp = Class.forName("net.minecraft.server.level.ServerPlayer");
            Class<?> cache = Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache");
            Object snap = cache.getMethod("refresh", sp).invoke(null, nms);
            if (snap == null) {
                return out;
            }

            long active = longField(snap, "active");
            long available = longField(snap, "availableMax");
            long calculated = longField(snap, "calculated");
            long purchased = longField(snap, "purchased");
            long personalMax = longField(snap, "personalMax");
            long teamBonus = longField(snap, "teamThresholdBonus");
            long teamContrib = longField(snap, "teamContribution");
            int level = intField(snap, "dmzLevel");
            int prestige = intField(snap, "prestige");
            Object teamMode = field(snap, "teamMode");

            out.put("active", String.valueOf(active));
            out.put("available", String.valueOf(available));
            out.put("available_max", String.valueOf(available));
            out.put("calculated", String.valueOf(calculated));
            out.put("purchased", String.valueOf(purchased));
            out.put("personal_max", String.valueOf(personalMax));
            out.put("team_bonus", String.valueOf(teamBonus));
            out.put("team_contrib", String.valueOf(teamContrib));
            out.put("team_contribution", String.valueOf(teamContrib));
            out.put("level", String.valueOf(level));
            out.put("prestige", String.valueOf(prestige));
            out.put("team_mode", teamMode == null ? "?" : String.valueOf(teamMode));

            Class<?> tierCls = Class.forName("com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier");
            Object tier = tierCls.getMethod("of", long.class).invoke(null, active);
            out.put("tier", String.valueOf(field(tier, "display")));

            Class<?> team = Class.forName("com.dbzlegacy.adaptivedifficulty.team.TeamScaling");
            out.put("team_name", String.valueOf(team.getMethod("teamName", sp).invoke(null, nms)));
            out.put("team_source", String.valueOf(team.getMethod("teamSourceLabel").invoke(null)));
            Object mates = team.getMethod("teammates", sp).invoke(null, nms);
            out.put("team_size", mates instanceof List<?> list ? String.valueOf(list.size()) : "0");

            Class<?> currency = Class.forName("com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge");
            out.put("balance", String.valueOf(currency.getMethod("balanceText", sp).invoke(null, nms)));
            out.put("currency", String.valueOf(currency.getMethod("currencyLabel").invoke(null)));

            Class<?> calc = Class.forName("com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator");
            long c100 = (Long) calc.getMethod("purchaseCost", long.class, long.class).invoke(null, purchased, 100L);
            long c1k = (Long) calc.getMethod("purchaseCost", long.class, long.class).invoke(null, purchased, 1000L);
            long c10k = (Long) calc.getMethod("purchaseCost", long.class, long.class).invoke(null, purchased, 10000L);
            Method format = currency.getMethod("formatCost", long.class);
            out.put("cost_100", String.valueOf(format.invoke(null, c100)));
            out.put("cost_1000", String.valueOf(format.invoke(null, c1k)));
            out.put("cost_10000", String.valueOf(format.invoke(null, c10k)));
            double mult = (Double) calc.getMethod("rewardMultiplier", long.class).invoke(null, active);
            out.put("reward_mult", String.format(Locale.US, "%.2f", mult));
        } catch (Throwable ignored) {
            // Forge mod not loaded
        }
        return out;
    }

    public static String placeholder(Player player, String id) {
        if (id == null) {
            return "";
        }
        return placeholders(player).getOrDefault(id.toLowerCase(Locale.ROOT), "");
    }

    /** Runs a GUI action through the Forge mod and returns the result message. */
    public static String handleAction(Player player, String action, String arg) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return "§cCould not reach adaptive difficulty mod.";
        }
        try {
            Class<?> sp = Class.forName("net.minecraft.server.level.ServerPlayer");
            Class<?> actions = Class.forName("com.dbzlegacy.adaptivedifficulty.service.DifficultyActions");
            String act = action == null ? "" : action.toLowerCase(Locale.ROOT);
            String page = "main";
            long amount = 0L;
            if ("page".equals(act)) {
                page = arg == null || arg.isBlank() ? "main" : arg;
            } else if (arg != null && !arg.isBlank()) {
                try {
                    amount = Long.parseLong(arg);
                } catch (NumberFormatException ignored) {
                    amount = 0L;
                }
            }
            Object result = actions.getMethod("handle", sp, String.class, long.class, String.class)
                    .invoke(null, nms, act, amount, page);
            if (result == null) {
                return "";
            }
            Method message = result.getClass().getMethod("message");
            Object msg = message.invoke(result);
            return msg == null ? "" : String.valueOf(msg);
        } catch (Throwable t) {
            return "§cAction failed: " + t.getClass().getSimpleName();
        }
    }

    public static boolean forgeAvailable() {
        try {
            Class.forName("com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod");
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean isStaff(Player player) {
        if (player == null) {
            return false;
        }
        if (player.isOp() || player.hasPermission("difficulty.admin")) {
            return true;
        }
        return player.hasPermission("*");
    }

    public static boolean toggleAdmin(Player player) {
        UUID id = player.getUniqueId();
        if (ADMIN.contains(id)) {
            ADMIN.remove(id);
            tryDisableForgeAdmin(player);
            return false;
        }
        ADMIN.add(id);
        tryEnableForgeAdmin(player);
        return true;
    }

    public static boolean hasAdmin(Player player) {
        return player != null && ADMIN.contains(player.getUniqueId());
    }

    public static void clearAdmin(Player player) {
        if (player != null) {
            ADMIN.remove(player.getUniqueId());
            tryDisableForgeAdmin(player);
        }
    }

    public static String adminSet(String key, String value) {
        try {
            Class<?> cfgCls = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig");
            Object cfg = cfgCls.getMethod("get").invoke(null);
            // Prefer the Forge command path for full key coverage when possible is hard;
            // set common fields reflectively + save.
            Field field = findConfigField(cfgCls, key);
            if (field == null) {
                return "Unknown key: " + key;
            }
            Object parsed = coerce(field.getType(), value);
            field.setAccessible(true);
            field.set(cfg, parsed);
            cfgCls.getMethod("save").invoke(null);
            Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache")
                    .getMethod("invalidateAll").invoke(null);
            return "Set " + key + " = " + value;
        } catch (Throwable t) {
            return "Failed: " + t.getMessage();
        }
    }

    public static void reloadConfig() {
        try {
            Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig")
                    .getMethod("reload").invoke(null);
            Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache")
                    .getMethod("invalidateAll").invoke(null);
        } catch (Throwable ignored) {
        }
    }

    public static String areaDifficultyText(Player player) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return "§cCould not reach adaptive difficulty mod.";
        }
        try {
            Class<?> sp = Class.forName("net.minecraft.server.level.ServerPlayer");
            Object snap = Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache")
                    .getMethod("refresh", sp).invoke(null, nms);
            long active = ((Number) snap.getClass().getField("active").get(snap)).longValue();
            long available = ((Number) snap.getClass().getField("availableMax").get(snap)).longValue();
            Object level = sp.getMethod("m_284548_").invoke(nms);
            Object pos = nms.getClass().getMethod("m_20183_").invoke(nms);
            long area = 0L;
            if (level != null && pos != null) {
                area = ((Number) Class.forName("com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty")
                        .getMethod("at", Class.forName("net.minecraft.server.level.ServerLevel"),
                                Class.forName("net.minecraft.core.BlockPos"))
                        .invoke(null, level, pos)).longValue();
            }
            Object cfg = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig")
                    .getMethod("get").invoke(null);
            String mode = String.valueOf(cfg.getClass().getField("areaDifficultyMode").get(cfg));
            double radius = ((Number) cfg.getClass().getField("mobScaleRadius").get(cfg)).doubleValue();
            double group = ((Number) cfg.getClass().getField("areaGroupBonusPercent").get(cfg)).doubleValue();
            return "§6Area Difficulty §8(Scaling Health-style)\n"
                    + "§ePlayer active: §f" + active + " §7/ max §f" + available + "\n"
                    + "§eArea at you: §f" + area + "\n"
                    + "§eMode: §f" + mode + " §8| §eradius §f" + radius
                    + " §8| §egroupBonus% §f" + group;
        } catch (Throwable t) {
            return "§cArea difficulty failed: " + t.getMessage();
        }
    }

    public static boolean setVanillaDifficulty(String level) {
        try {
            Class<?> difficulty = Class.forName("net.minecraft.world.Difficulty");
            Object parsed = Class.forName("com.dbzlegacy.adaptivedifficulty.world.VanillaDifficultyGuard")
                    .getMethod("parse", String.class)
                    .invoke(null, level);
            if (parsed == null) {
                return false;
            }
            Object server = Class.forName("org.bukkit.Bukkit")
                    .getMethod("getServer").invoke(null);
            // Mohist CraftServer -> getServer() NMS
            Object nmsServer = server.getClass().getMethod("getServer").invoke(server);
            return Boolean.TRUE.equals(
                    Class.forName("com.dbzlegacy.adaptivedifficulty.world.VanillaDifficultyGuard")
                            .getMethod("set", Class.forName("net.minecraft.server.MinecraftServer"), difficulty)
                            .invoke(null, nmsServer, parsed)
            );
        } catch (Throwable t) {
            return false;
        }
    }

    private static void tryEnableForgeAdmin(Player player) {
        try {
            Object nms = nmsPlayer(player);
            Class<?> access = Class.forName("com.dbzlegacy.adaptivedifficulty.command.AdminCommandAccess");
            // enable by toggling until enabled
            Method isEnabled = access.getMethod("isEnabled", Class.forName("net.minecraft.server.level.ServerPlayer"));
            Method toggle = access.getMethod("toggle", Class.forName("net.minecraft.server.level.ServerPlayer"));
            if (!Boolean.TRUE.equals(isEnabled.invoke(null, nms))) {
                toggle.invoke(null, nms);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void tryDisableForgeAdmin(Player player) {
        try {
            Object nms = nmsPlayer(player);
            Class<?> access = Class.forName("com.dbzlegacy.adaptivedifficulty.command.AdminCommandAccess");
            Method disable = access.getMethod("disable", Class.forName("net.minecraft.server.level.ServerPlayer"));
            disable.invoke(null, nms);
        } catch (Throwable ignored) {
        }
    }

    private static Field findConfigField(Class<?> cfgCls, String key) {
        String k = key == null ? "" : key.toLowerCase(Locale.ROOT);
        Map<String, String> aliases = Map.ofEntries(
                Map.entry("guibackend", "guiBackend"),
                Map.entry("vanilladifficulty", "vanillaDifficulty"),
                Map.entry("enablemobscaling", "enableMobScaling"),
                Map.entry("adminpermission", "adminPermission"),
                Map.entry("basecost", "baseCost"),
                Map.entry("costscaling", "costScaling"),
                Map.entry("rewardscaling", "rewardScaling"),
                Map.entry("prestigemultiplier", "prestigeMultiplier"),
                Map.entry("levelmultiplier", "levelMultiplier"),
                Map.entry("teambonus", "teamBonusPercent"),
                Map.entry("teambonuspercent", "teamBonusPercent"),
                Map.entry("maxhealthmultiplier", "maxHealthMultiplier"),
                Map.entry("maxscaledhealth", "maxScaledHealth")
        );
        String fieldName = aliases.getOrDefault(k, key);
        try {
            return cfgCls.getField(fieldName);
        } catch (NoSuchFieldException e) {
            for (Field f : cfgCls.getFields()) {
                if (f.getName().equalsIgnoreCase(fieldName) || f.getName().equalsIgnoreCase(k)) {
                    return f;
                }
            }
            return null;
        }
    }

    private static Object coerce(Class<?> type, String value) {
        if (type == String.class) {
            return value;
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        }
        if (type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        }
        if (type == long.class || type == Long.class) {
            return Long.parseLong(value);
        }
        if (type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        }
        if (type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        }
        return value;
    }

    private static Object field(Object obj, String name) throws Exception {
        Field f = obj.getClass().getField(name);
        return f.get(obj);
    }

    private static long longField(Object obj, String name) throws Exception {
        return ((Number) field(obj, name)).longValue();
    }

    private static int intField(Object obj, String name) throws Exception {
        return ((Number) field(obj, name)).intValue();
    }
}
