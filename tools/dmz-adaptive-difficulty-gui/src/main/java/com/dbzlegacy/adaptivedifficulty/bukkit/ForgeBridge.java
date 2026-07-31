package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/** Reflects into the Forge mod for live values / actions (Mohist shared JVM). */
public final class ForgeBridge {
    private static final Set<UUID> ADMIN = ConcurrentHashMap.newKeySet();
    private static final long PLACEHOLDER_TTL_MS = 200L;
    private static final Map<UUID, CachedPlaceholders> PLACEHOLDER_CACHE = new ConcurrentHashMap<>();

    private static volatile boolean resolved;
    private static Class<?> serverPlayerCls;
    private static Class<?> cacheCls;
    private static Class<?> tierCls;
    private static Class<?> teamCls;
    private static Class<?> currencyCls;
    private static Class<?> calcCls;
    private static Class<?> dataCls;
    private static Class<?> actionsCls;
    private static Method getHandle;
    private static Method cacheGet;
    private static Method cacheRefresh;
    private static Method cacheData;
    private static Method tierOf;
    private static Method tierValues;
    private static Method tierThreshold;
    private static Method teamName;
    private static Method teamSource;
    private static Method teammates;
    private static Method balanceText;
    private static Method currencyLabel;
    private static Method purchaseCost;
    private static Method raiseCostIronCoins;
    private static Method formatCost;
    private static Method rewardMult;
    private static Method killTp;
    private static Method dataTitles;
    private static Method snapshotState;
    private static Method snapshotStateColor;
    private static Method actionsHandle;
    private static Method resultMessage;
    private static Method resultOk;
    private static Method chatMenuOpen;

    private ForgeBridge() {}

    public static Object nmsPlayer(Player player) {
        if (player == null) {
            return null;
        }
        try {
            ensureResolved();
            if (getHandle == null) {
                getHandle = player.getClass().getMethod("getHandle");
            }
            return getHandle.invoke(player);
        } catch (Throwable t) {
            return null;
        }
    }

    public static Map<String, String> placeholders(Player player) {
        if (player == null) {
            return Map.of();
        }
        long now = System.currentTimeMillis();
        CachedPlaceholders cached = PLACEHOLDER_CACHE.get(player.getUniqueId());
        if (cached != null && now - cached.atMs <= PLACEHOLDER_TTL_MS) {
            return cached.map;
        }
        Map<String, String> built = buildPlaceholders(player);
        PLACEHOLDER_CACHE.put(player.getUniqueId(), new CachedPlaceholders(built, now));
        return built;
    }

    public static String placeholder(Player player, String id) {
        if (id == null) {
            return "";
        }
        return placeholders(player).getOrDefault(id.toLowerCase(Locale.ROOT), "");
    }

    private static Map<String, String> buildPlaceholders(Player player) {
        Map<String, String> out = new HashMap<>();
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return out;
        }
        try {
            ensureResolved();
            // Refresh so GUI costs/balance match post-level / coin changes (chat menu does the same).
            Object snap = cacheRefresh.invoke(null, nms);
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

            Object tier = tierOf.invoke(null, active);
            out.put("tier", String.valueOf(field(tier, "display")));

            out.put("team_name", String.valueOf(teamName.invoke(null, nms)));
            out.put("team_source", String.valueOf(teamSource.invoke(null)));
            Object mates = teammates.invoke(null, nms);
            out.put("team_size", mates instanceof List<?> list ? String.valueOf(list.size()) : "0");

            out.put("balance", String.valueOf(balanceText.invoke(null, nms)));
            out.put("currency", String.valueOf(currencyLabel.invoke(null)));

            long room = Math.max(0L, available - active);
            long[] steps = {1L, 5L, 25L, 100L, 1000L, 10000L, 100000L};
            for (long step : steps) {
                long buyCost = (Long) purchaseCost.invoke(null, purchased, step);
                out.put("cost_buy_" + step, String.valueOf(formatCost.invoke(null, buyCost)));
                // legacy aliases used by older menu builds
                out.put("cost_" + step, out.get("cost_buy_" + step));

                long upAmt = Math.min(step, room);
                if (upAmt <= 0) {
                    out.put("cost_up_" + step, "at max");
                } else {
                    long upCost = (Long) raiseCostIronCoins.invoke(null, active, upAmt);
                    out.put("cost_up_" + step, String.valueOf(formatCost.invoke(null, upCost)));
                }
            }
            // keep old buy aliases for any leftover references
            out.put("cost_1000", out.get("cost_buy_1000"));
            out.put("cost_10000", out.get("cost_buy_10000"));
            out.put("cost_100000", out.get("cost_buy_100000"));

            long maxCost = room <= 0 ? 0L : (Long) raiseCostIronCoins.invoke(null, active, room);
            out.put("cost_max", room <= 0 ? "at max" : String.valueOf(formatCost.invoke(null, maxCost)));
            out.put("raise_room", String.valueOf(room));

            double mult = (Double) rewardMult.invoke(null, active);
            out.put("reward_mult", String.format(Locale.US, "%.2f", mult));
            if (killTp != null) {
                double kill = (Double) killTp.invoke(null, active);
                out.put("kill_tp", String.format(Locale.US, "%,.0f", kill));
            } else {
                out.put("kill_tp", "?");
            }

            Object state = snapshotState.invoke(snap);
            Object stateColor = snapshotStateColor.invoke(snap);
            out.put("state", state == null ? "?" : String.valueOf(state));
            out.put("state_color", stateColor == null ? "f" : String.valueOf(stateColor));

            Object data = cacheData.invoke(null, nms);
            if (data != null) {
                Object titlesObj = dataTitles.invoke(data);
                if (titlesObj instanceof List<?> titles && !titles.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (Object t : titles) {
                        if (sb.length() > 0) {
                            sb.append(", ");
                        }
                        sb.append(t);
                    }
                    out.put("titles", sb.toString());
                } else {
                    out.put("titles", "none");
                }
            } else {
                out.put("titles", "none");
            }

            for (Object t : (Object[]) tierValues.invoke(null)) {
                String name = String.valueOf(t);
                if ("NONE".equals(name)) {
                    continue;
                }
                Object thr = tierThreshold.invoke(t);
                out.put("tier_" + name.toLowerCase(Locale.ROOT), String.valueOf(thr));
            }
        } catch (Throwable ignored) {
            // Forge mod not loaded
        }
        return out;
    }

    /** Runs a GUI action through the Forge mod and returns the result message. */
    public static String handleAction(Player player, String action, String arg) {
        return handleActionResult(player, action, arg, null).message;
    }

    /**
     * @param returnPage GUI page to reopen after the action (e.g. {@code adjust}, {@code buy}).
     */
    public static String handleAction(Player player, String action, String arg, String returnPage) {
        return handleActionResult(player, action, arg, returnPage).message;
    }

    /**
     * @param returnPage GUI page to reopen after the action (e.g. {@code adjust}, {@code buy}).
     */
    public static ActionResult handleActionResult(Player player, String action, String arg, String returnPage) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return ActionResult.fail("Could not reach adaptive difficulty mod.");
        }
        try {
            ensureResolved();
            String act = action == null ? "" : action.toLowerCase(Locale.ROOT);
            String page = resolveReturnPage(act, arg, returnPage);
            long amount = 0L;
            if ("page".equals(act) || "team".equals(act) || "refresh".equals(act)
                    || "set_max".equals(act) || "reset".equals(act) || "zero".equals(act)
                    || "clear".equals(act)) {
                if ("page".equals(act)) {
                    page = arg == null || arg.isBlank() ? "main" : arg;
                }
            } else if (arg != null && !arg.isBlank()) {
                try {
                    amount = Long.parseLong(arg);
                } catch (NumberFormatException ignored) {
                    return ActionResult.fail("Invalid amount: " + arg);
                }
            }
            Object result = actionsHandle.invoke(null, nms, act, amount, page);
            PLACEHOLDER_CACHE.remove(player.getUniqueId());
            if (result == null) {
                return ActionResult.ok("");
            }
            Object msg = resultMessage.invoke(result);
            boolean ok = resultOk == null || Boolean.TRUE.equals(resultOk.invoke(result));
            String text = msg == null ? "" : String.valueOf(msg);
            return new ActionResult(ok, text);
        } catch (Throwable t) {
            return ActionResult.fail("Action failed: " + t.getClass().getSimpleName());
        }
    }

    /** Forge {@code guiBackend} config value (lowercased), or {@code cmi} default. */
    public static String guiBackend() {
        try {
            ensureResolved();
            Object cfg = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig")
                    .getMethod("get").invoke(null);
            Object raw = cfg.getClass().getField("guiBackend").get(cfg);
            if (raw == null) {
                return "cmi";
            }
            String v = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
            return v.isEmpty() ? "cmi" : v;
        } catch (Throwable t) {
            return "cmi";
        }
    }

    public static String adminPermission() {
        try {
            Object cfg = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig")
                    .getMethod("get").invoke(null);
            Object raw = cfg.getClass().getField("adminPermission").get(cfg);
            if (raw == null) {
                return "difficulty.admin";
            }
            String v = String.valueOf(raw).trim();
            return v.isEmpty() ? "difficulty.admin" : v;
        } catch (Throwable t) {
            return "difficulty.admin";
        }
    }

    /** Opens Forge clickable chat menu (settings / chat backend). */
    public static boolean openChatMenu(Player player, String page) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return false;
        }
        try {
            ensureResolved();
            if (chatMenuOpen == null) {
                return false;
            }
            chatMenuOpen.invoke(null, nms, page == null || page.isBlank() ? "main" : page);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    public record ActionResult(boolean ok, String message) {
        public static ActionResult ok(String message) {
            return new ActionResult(true, message == null ? "" : message);
        }

        public static ActionResult fail(String message) {
            return new ActionResult(false, message == null ? "" : message);
        }
    }

    /** Default page to reopen for an action when none is supplied. */
    public static String resolveReturnPage(String action, String arg, String explicitPage) {
        if (explicitPage != null && !explicitPage.isBlank()) {
            return explicitPage.toLowerCase(Locale.ROOT);
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT);
        return switch (act) {
            case "page" -> arg == null || arg.isBlank() ? "main" : arg.toLowerCase(Locale.ROOT);
            case "up", "down", "reset", "zero", "clear", "set_max", "set" -> "adjust";
            case "buy" -> "buy";
            default -> "main";
        };
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
        if (player.isOp() || player.hasPermission(adminPermission())) {
            return true;
        }
        return player.hasPermission("*");
    }

    public static boolean toggleAdmin(Player player) {
        // Prefer Forge AdminCommandAccess as source of truth.
        try {
            Object nms = nmsPlayer(player);
            Class<?> access = Class.forName("com.dbzlegacy.adaptivedifficulty.command.AdminCommandAccess");
            Method toggle = access.getMethod("toggle", Class.forName("net.minecraft.server.level.ServerPlayer"));
            boolean enabled = Boolean.TRUE.equals(toggle.invoke(null, nms));
            UUID id = player.getUniqueId();
            if (enabled) {
                ADMIN.add(id);
            } else {
                ADMIN.remove(id);
            }
            return enabled;
        } catch (Throwable ignored) {
        }
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
        if (player == null) {
            return false;
        }
        try {
            Object nms = nmsPlayer(player);
            Class<?> access = Class.forName("com.dbzlegacy.adaptivedifficulty.command.AdminCommandAccess");
            Method isEnabled = access.getMethod("isEnabled", Class.forName("net.minecraft.server.level.ServerPlayer"));
            boolean enabled = Boolean.TRUE.equals(isEnabled.invoke(null, nms));
            if (enabled) {
                ADMIN.add(player.getUniqueId());
            } else {
                ADMIN.remove(player.getUniqueId());
            }
            return enabled;
        } catch (Throwable ignored) {
        }
        return ADMIN.contains(player.getUniqueId());
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
            PLACEHOLDER_CACHE.clear();
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
            PLACEHOLDER_CACHE.clear();
        } catch (Throwable ignored) {
        }
    }

    /** Staff: wipe purchased + active difficulty for this player. */
    public static String resetPurchased(Player player) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return "§cCould not reach adaptive difficulty mod.";
        }
        try {
            ensureResolved();
            Object data = cacheData.invoke(null, nms);
            data.getClass().getMethod("setPurchasedDifficulty", long.class).invoke(data, 0L);
            data.getClass().getMethod("setActiveDifficulty", long.class).invoke(data, 0L);
            cacheCls.getMethod("save", serverPlayerCls).invoke(null, nms);
            cacheRefresh.invoke(null, nms);
            Class.forName("com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty")
                    .getMethod("clearCache").invoke(null);
            PLACEHOLDER_CACHE.remove(player.getUniqueId());
            return "§aReset purchased + active difficulty to 0.";
        } catch (Throwable t) {
            return "§cReset failed: " + t.getMessage();
        }
    }

    public static String areaDifficultyText(Player player) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return "§cCould not reach adaptive difficulty mod.";
        }
        try {
            ensureResolved();
            Object snap = cacheRefresh.invoke(null, nms);
            long active = ((Number) snap.getClass().getField("active").get(snap)).longValue();
            long available = ((Number) snap.getClass().getField("availableMax").get(snap)).longValue();
            Object level = serverPlayerCls.getMethod("m_284548_").invoke(nms);
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

    private static void ensureResolved() throws Exception {
        if (resolved) {
            return;
        }
        synchronized (ForgeBridge.class) {
            if (resolved) {
                return;
            }
            serverPlayerCls = Class.forName("net.minecraft.server.level.ServerPlayer");
            cacheCls = Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache");
            tierCls = Class.forName("com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier");
            teamCls = Class.forName("com.dbzlegacy.adaptivedifficulty.team.TeamScaling");
            currencyCls = Class.forName("com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge");
            calcCls = Class.forName("com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator");
            dataCls = Class.forName("com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData");
            actionsCls = Class.forName("com.dbzlegacy.adaptivedifficulty.service.DifficultyActions");

            cacheGet = cacheCls.getMethod("get", serverPlayerCls);
            cacheRefresh = cacheCls.getMethod("refresh", serverPlayerCls);
            cacheData = cacheCls.getMethod("data", serverPlayerCls);
            tierOf = tierCls.getMethod("of", long.class);
            tierValues = tierCls.getMethod("values");
            tierThreshold = tierCls.getMethod("threshold");
            teamName = teamCls.getMethod("teamName", serverPlayerCls);
            teamSource = teamCls.getMethod("teamSourceLabel");
            teammates = teamCls.getMethod("teammates", serverPlayerCls);
            balanceText = currencyCls.getMethod("balanceText", serverPlayerCls);
            currencyLabel = currencyCls.getMethod("currencyLabel");
            purchaseCost = calcCls.getMethod("purchaseCost", long.class, long.class);
            raiseCostIronCoins = calcCls.getMethod("raiseCostIronCoins", long.class, long.class);
            formatCost = currencyCls.getMethod("formatCost", long.class);
            rewardMult = calcCls.getMethod("rewardMultiplier", long.class);
            try {
                killTp = calcCls.getMethod("killTrainingPoints", long.class);
            } catch (NoSuchMethodException missing) {
                killTp = null;
            }
            dataTitles = dataCls.getMethod("getTitles");
            Class<?> snapCls = Class.forName("com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot");
            snapshotState = snapCls.getMethod("state");
            snapshotStateColor = snapCls.getMethod("stateColorCode");
            actionsHandle = actionsCls.getMethod("handle", serverPlayerCls, String.class, long.class, String.class);
            Class<?> resultCls = Class.forName("com.dbzlegacy.adaptivedifficulty.service.DifficultyActions$Result");
            resultMessage = resultCls.getMethod("message");
            resultOk = resultCls.getMethod("ok");
            chatMenuOpen = Class.forName("com.dbzlegacy.adaptivedifficulty.gui.DifficultyChatMenu")
                    .getMethod("open", serverPlayerCls, String.class);
            resolved = true;
        }
    }

    private static void tryEnableForgeAdmin(Player player) {
        try {
            Object nms = nmsPlayer(player);
            Class<?> access = Class.forName("com.dbzlegacy.adaptivedifficulty.command.AdminCommandAccess");
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

    private record CachedPlaceholders(Map<String, String> map, long atMs) {}
}
