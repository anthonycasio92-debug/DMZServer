package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

/**
 * Reflects into the Forge mod for live values / actions (Mohist shared JVM).
 * Surface: hub status, Buy Tier (UnlockTier 1–7), Lower/Reset, Team (WIP), character_reset.
 */
public final class ForgeBridge {
    private static final long PLACEHOLDER_TTL_MS = 200L;
    private static final Map<UUID, CachedPlaceholders> PLACEHOLDER_CACHE = new ConcurrentHashMap<>();

    private static volatile boolean resolved;
    private static volatile String resolveError;
    private static Class<?> serverPlayerCls;
    private static Class<?> cacheCls;
    private static Class<?> actionsCls;
    private static Method getHandle;
    private static Method cacheGet;
    private static Method cacheRefresh;
    private static Method cacheData;
    private static Method economyBalanceText;
    private static Method economyFormat;
    private static Method economyFormatExactCost;
    private static Method economyActivationCostPlayer;
    private static Method economyCountOf;
    private static Method economyBalance;
    private static Class<?> coinKindCls;
    private static Method unlockTierValues;
    private static Method unlockTierById;
    private static Method unlockTierActivationCost;
    private static Method unlockTierActivationCostForLevel;
    private static Method unlockTierMaxDifficulty;
    private static Method titleActiveDisplay;
    private static Method titleActiveId;
    private static Method titleHas;
    private static Method titleValues;
    private static Method titleRequirementTip;
    private static Method snapshotState;
    private static Method snapshotStateColor;
    private static Method actionsHandle;
    private static Method actionsHandleArg;
    private static Method actionsHandleArgNoReopen;
    private static Method resultMessage;
    private static Method resultOk;
    private static Method chatMenuOpen;
    private static volatile Field RESULT_MESSAGE_FIELD;

    private ForgeBridge() {}

    public static Object nmsPlayer(Player player) {
        if (player == null) {
            return null;
        }
        try {
            if (getHandle == null) {
                getHandle = player.getClass().getMethod("getHandle");
            }
            Object handle = getHandle.invoke(player);
            ensureResolved(handle == null ? null : handle.getClass().getClassLoader());
            return handle;
        } catch (Throwable t) {
            resolveError = t.getClass().getSimpleName() + ": " + t.getMessage();
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
        String key = id.toLowerCase(Locale.ROOT);
        return placeholders(player).getOrDefault(key, "");
    }

    private static Map<String, String> buildPlaceholders(Player player) {
        Map<String, String> out = new HashMap<>();
        // Fail-closed defaults — GUI must not look healthy when Forge is unreachable.
        out.put("bridge_ok", "false");
        out.put("system_enabled", "false");
        out.put("player_allowed", "false");
        out.put("personal_enabled", "false");
        Object nms = nmsPlayer(player);
        if (nms == null || !forgeAvailable()) {
            return out;
        }
        try {
            ensureResolved();
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
            long combatRating = longField(snap, "combatRating");
            long ancientCopper = longField(snap, "ancientCopper");
            int level = intField(snap, "dmzLevel");
            int prestige = intField(snap, "prestige");
            int activeTier = intField(snap, "activeTier");
            int highestUnlocked = intField(snap, "highestUnlockedTier");
            Object teamMode = field(snap, "teamMode");
            Object activeTierName = field(snap, "activeTierName");

            // Internal numbers kept for PAPI compatibility; GUIs no longer show active/available points.
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
            out.put("dmz_level", String.valueOf(level));
            out.put("prestige", String.valueOf(prestige));
            out.put("team_mode", "WIP");
            out.put("combat_rating", String.valueOf(combatRating > 0 ? combatRating : calculated));
            out.put("active_tier", String.valueOf(activeTier));
            out.put("highest_unlocked", String.valueOf(highestUnlocked));
            out.put("active_tier_name", activeTierName == null ? "None" : String.valueOf(activeTierName));
            out.put("tier", activeTierName == null ? "None" : String.valueOf(activeTierName));

            out.put("team_name", "WIP");
            out.put("team_source", "personal");
            out.put("team_size", "0");

            long liveBalance = ancientCopper > 0 ? ancientCopper : purchased;
            if (economyBalance != null) {
                try {
                    Object bal = economyBalance.invoke(null, nms);
                    if (bal instanceof Number n) {
                        liveBalance = n.longValue();
                    }
                } catch (Throwable ignored) {
                }
            }
            out.put("ancient_coins", String.valueOf(liveBalance));

            String balance = "?";
            if (economyBalanceText != null) {
                balance = String.valueOf(economyBalanceText.invoke(null, nms));
            } else if (economyFormat != null) {
                balance = String.valueOf(economyFormat.invoke(null, liveBalance));
            }
            out.put("balance", balance);
            out.put("currency", "Ancient Coins");

            putCoinCounts(out, nms);

            boolean systemOn = systemEnabled();
            out.put("system_enabled", systemOn ? "true" : "false");
            out.put("system_status", systemOn ? "ENABLED" : "DISABLED");
            boolean wlOn = whitelistEnabled();
            boolean allowed = playerAllowed(player);
            out.put("whitelist_enabled", wlOn ? "true" : "false");
            out.put("whitelist_status", wlOn ? "ON" : "OFF");
            out.put("player_allowed", allowed ? "true" : "false");

            boolean personalOn = true;
            boolean coinChatOn = false;
            if (cacheData != null) {
                try {
                    Object data = cacheData.invoke(null, nms);
                    if (data != null) {
                        Object pe = data.getClass().getMethod("isPersonalEnabled").invoke(data);
                        if (pe instanceof Boolean b) {
                            personalOn = b;
                        }
                        Object cc = data.getClass().getMethod("isCoinDropChat").invoke(data);
                        if (cc instanceof Boolean b) {
                            coinChatOn = b;
                        }
                    }
                } catch (Throwable ignored) {
                }
            }
            out.put("personal_enabled", personalOn ? "true" : "false");
            out.put("personal_status", personalOn ? "ON" : "OFF");
            out.put("coin_drop_chat", coinChatOn ? "true" : "false");
            putCounterPlaceholders(out, nms);

            // UnlockTier 1–7 — level-scaled activation costs + live unlock flags.
            Object playerData = null;
            if (cacheData != null) {
                try {
                    playerData = cacheData.invoke(null, nms);
                } catch (Throwable ignored) {
                }
            }
            if (unlockTierValues != null && economyFormatExactCost != null) {
                try {
                    for (Object ut : (Object[]) unlockTierValues.invoke(null)) {
                        int id = ((Number) field(ut, "id")).intValue();
                        Object display = field(ut, "display");
                        out.put("tier_" + id + "_name", "T" + id + " " + (display == null ? "" : display));
                        long cost = resolveTierCost(ut, nms, level);
                        // Never show resolve failures as "free" (formatExactCost(0)).
                        String costText = cost <= 0L
                                ? "?"
                                : String.valueOf(economyFormatExactCost.invoke(null, cost));
                        out.put("unlock_tier_" + id + "_cost", costText);
                        out.put("tier_" + id + "_cost", costText);
                        out.put("tier_" + id + "_cost_raw", String.valueOf(Math.max(0L, cost)));
                        // Fail closed — never invent unlocks from highest_unlocked.
                        boolean unlocked = false;
                        if (playerData != null) {
                            try {
                                Object u = playerData.getClass()
                                        .getMethod("hasUnlockedTier", int.class)
                                        .invoke(playerData, id);
                                unlocked = u instanceof Boolean b && b;
                            } catch (Throwable ignored) {
                                unlocked = false;
                            }
                        }
                        out.put("tier_" + id + "_unlocked", unlocked ? "true" : "false");
                        Object defaultMax = field(ut, "defaultMaxDifficulty");
                        out.put("unlock_tier_" + id + "_max", String.valueOf(defaultMax));
                        if (unlockTierById != null && unlockTierMaxDifficulty != null) {
                            Object live = unlockTierById.invoke(null, id);
                            if (live != null) {
                                try {
                                    long max = ((Number) unlockTierMaxDifficulty.invoke(live)).longValue();
                                    out.put("unlock_tier_" + id + "_max", String.valueOf(max));
                                } catch (Throwable ignored) {
                                }
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            putTitlePlaceholders(out, nms);

            if (snapshotState != null) {
                Object state = snapshotState.invoke(snap);
                out.put("state", state == null ? "?" : String.valueOf(state));
            }
            if (snapshotStateColor != null) {
                Object stateColor = snapshotStateColor.invoke(snap);
                out.put("state_color", stateColor == null ? "f" : String.valueOf(stateColor));
            }
            // Personal OFF always wins for status display (GUI / PAPI).
            if (!personalOn) {
                out.put("state", "Off");
                out.put("state_color", "c");
            }
            out.put("bridge_ok", "true");
        } catch (Throwable ignored) {
            // Forge mod not loaded / partial API — keep fail-closed defaults.
            out.put("bridge_ok", "false");
            out.put("system_enabled", "false");
            out.put("player_allowed", "false");
        }
        return out;
    }

    private static long resolveTierCost(Object unlockTier, Object nmsPlayer, int dmzLevel) {
        try {
            if (economyActivationCostPlayer != null) {
                Object cost = economyActivationCostPlayer.invoke(null, unlockTier, nmsPlayer);
                if (cost instanceof Number n) {
                    return n.longValue();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (unlockTierActivationCostForLevel != null) {
                Object cost = unlockTierActivationCostForLevel.invoke(unlockTier, dmzLevel);
                if (cost instanceof Number n) {
                    return n.longValue();
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            if (unlockTierActivationCost != null) {
                Object cost = unlockTierActivationCost.invoke(unlockTier);
                if (cost instanceof Number n) {
                    return n.longValue();
                }
            }
        } catch (Throwable ignored) {
        }
        return 0L;
    }

    /** Class / top-stat counter identity for staff GUI + PAPI. */
    private static void putCounterPlaceholders(Map<String, String> out, Object nms) {
        out.putIfAbsent("fighting_class", "");
        out.putIfAbsent("race", "");
        out.putIfAbsent("fighting_style", "HYBRID");
        out.putIfAbsent("top_stats", "—");
        out.putIfAbsent("weak_stat", "NONE");
        if (nms == null) {
            return;
        }
        try {
            ensureResolved(nms.getClass().getClassLoader());
            Class<?> profileCls = loadClass(
                    "com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile",
                    nms.getClass().getClassLoader());
            Object profile = profileCls.getMethod("of", serverPlayerCls).invoke(null, nms);
            if (profile == null) {
                return;
            }
            Object cls = profileCls.getField("fightingClass").get(profile);
            Object race = profileCls.getField("race").get(profile);
            Object style = profileCls.getField("style").get(profile);
            Object weak = profileCls.getField("weakest").get(profile);
            Object topLabel = profileCls.getMethod("topStatsLabel").invoke(profile);
            out.put("fighting_class", cls == null ? "" : String.valueOf(cls));
            out.put("race", race == null ? "" : String.valueOf(race));
            out.put("fighting_style", style == null ? "HYBRID" : String.valueOf(style));
            out.put("weak_stat", weak == null ? "NONE" : String.valueOf(weak));
            out.put("top_stats", topLabel == null ? "—" : String.valueOf(topLabel));
        } catch (Throwable ignored) {
        }
    }

    private static void putCoinCounts(Map<String, String> out, Object nms) {
        // Ladder: Copper → Iron → Gold → Emerald → Diamond → Netherite
        // (Lapis / Ender Pearl ancients are unused by this mod.)
        String[] keys = {
                "coins_copper", "coins_iron", "coins_gold",
                "coins_emerald", "coins_diamond", "coins_netherite"
        };
        String[] kindNames = {
                "COPPER", "IRON", "GOLD", "EMERALD", "DIAMOND", "NETHERITE"
        };
        for (String key : keys) {
            out.putIfAbsent(key, "0");
        }
        if (economyCountOf == null || coinKindCls == null || nms == null) {
            return;
        }
        try {
            Object[] kinds = (Object[]) coinKindCls.getMethod("values").invoke(null);
            for (Object kind : kinds) {
                String name = String.valueOf(kind);
                Object countObj = economyCountOf.invoke(null, nms, kind);
                long count = countObj instanceof Number n ? n.longValue() : 0L;
                for (int i = 0; i < kindNames.length; i++) {
                    if (kindNames[i].equalsIgnoreCase(name)) {
                        out.put(keys[i], String.valueOf(count));
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static void putTitlePlaceholders(Map<String, String> out, Object nms) {
        out.put("active_title", "None");
        out.put("active_title_id", "");
        if (nms == null) {
            return;
        }
        try {
            if (titleActiveDisplay != null) {
                Object display = titleActiveDisplay.invoke(null, nms);
                out.put("active_title", display == null || String.valueOf(display).isBlank()
                        ? "None" : String.valueOf(display));
            }
            if (titleActiveId != null) {
                Object id = titleActiveId.invoke(null, nms);
                out.put("active_title_id", id == null ? "" : String.valueOf(id));
            }
            if (titleValues != null) {
                for (Object title : (Object[]) titleValues.invoke(null)) {
                    String id = String.valueOf(field(title, "id"));
                    String display = String.valueOf(field(title, "display"));
                    out.put("title_" + id + "_name", display);
                    boolean earned = false;
                    if (titleHas != null) {
                        earned = Boolean.TRUE.equals(titleHas.invoke(null, nms, title));
                    }
                    out.put("title_" + id + "_earned", earned ? "true" : "false");
                    String req = "";
                    if (titleRequirementTip != null) {
                        Object tip = titleRequirementTip.invoke(title);
                        req = tip == null ? "" : String.valueOf(tip);
                    }
                    out.put("title_" + id + "_req", req);
                }
            }
        } catch (Throwable ignored) {
        }
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
     * V3 actions: activate/purchase_tier/buy, down, team, reset, character_reset, page, refresh;
     * optional set (free lower only). Unknown actions fail.
     */
    public static ActionResult handleActionResult(Player player, String action, String arg, String returnPage) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            String detail = resolveError == null || resolveError.isBlank()
                    ? "is AdaptiveDifficulty-*.jar loaded in mods/?"
                    : resolveError;
            return ActionResult.fail("Could not reach AdaptiveDifficulty mod (" + detail + ").");
        }
        try {
            ensureResolved(nms.getClass().getClassLoader());
            String act = action == null ? "" : action.toLowerCase(Locale.ROOT);
            if (!isSupportedAction(act)) {
                return ActionResult.fail("Unknown action.");
            }
            String page = resolveReturnPage(act, arg, returnPage);
            if ("page".equals(act)) {
                page = arg == null || arg.isBlank() ? "main" : arg;
            }
            Object result;
            // Prefer no-reopen: Bukkit already owns inventory reopen after /difficulty do.
            // Avoids Forge DifficultyMenu dumping chat when reflection player resolve flickers.
            if (actionsHandleArgNoReopen != null) {
                result = actionsHandleArgNoReopen.invoke(null, nms, act, arg == null ? "" : arg, page);
            } else if (actionsHandleArg != null) {
                result = actionsHandleArg.invoke(null, nms, act, arg == null ? "" : arg, page);
            } else {
                long amount = 0L;
                if (!("page".equals(act) || "team".equals(act) || "refresh".equals(act)
                        || "reset".equals(act) || "zero".equals(act) || "clear".equals(act)
                        || "character_reset".equals(act) || "char_reset".equals(act)
                        || "characterreset".equals(act))) {
                    if (arg != null && !arg.isBlank()) {
                        try {
                            amount = Long.parseLong(arg);
                        } catch (NumberFormatException ignored) {
                            return ActionResult.fail("Invalid amount: " + arg);
                        }
                    }
                }
                result = actionsHandle.invoke(null, nms, act, amount, page);
            }
            PLACEHOLDER_CACHE.remove(player.getUniqueId());
            if (result == null) {
                return ActionResult.ok("");
            }
            Object msg = readResultMessage(result);
            boolean ok = resultOk == null || Boolean.TRUE.equals(resultOk.invoke(result));
            String text = msg == null ? "" : String.valueOf(msg);
            return new ActionResult(ok, text);
        } catch (Throwable t) {
            Throwable root = t.getCause() == null ? t : t.getCause();
            return ActionResult.fail("Action failed: " + root.getClass().getSimpleName()
                    + (root.getMessage() == null ? "" : " — " + root.getMessage()));
        }
    }

    private static boolean isSupportedAction(String act) {
        return switch (act) {
            case "activate", "purchase_tier", "buy", "lower_tier",
                 "down", "team", "reset", "zero", "clear",
                 "character_reset", "char_reset", "characterreset",
                 "equip_title", "clear_title", "equip", "unequip_title",
                 "toggle_personal", "personal", "toggle_difficulty", "difficulty_toggle",
                 "toggle_coin_chat", "coin_chat", "toggle_chat", "chat_drops",
                 "page", "refresh", "set" -> true;
            default -> false;
        };
    }

    /**
     * Canonical Forge {@code guiBackend}: {@code cmi|chest|chat|auto}.
     * Aliases ({@code bukkit}, {@code cmilib}, legacy deluxe, …) are normalized here
     * so Bukkit open paths match Forge {@code GuiBackend.fromConfig()}.
     */
    public static String guiBackend() {
        try {
            ensureResolved();
            Object cfg = loadClass("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig", preferredLoader())
                    .getMethod("get").invoke(null);
            Object raw = cfg.getClass().getField("guiBackend").get(cfg);
            if (raw == null) {
                return "cmi";
            }
            String v = String.valueOf(raw).trim().toLowerCase(Locale.ROOT);
            if (v.isEmpty()) {
                return "cmi";
            }
            return switch (v) {
                case "cmi", "cmilib", "cmigui", "deluxemenus", "deluxe", "dm" -> "cmi";
                case "chest", "bukkit", "inventory", "gui" -> "chest";
                case "chat" -> "chat";
                case "auto" -> "auto";
                default -> "auto";
            };
        } catch (Throwable t) {
            return "cmi";
        }
    }

    /** Forge mod version string, or null if unreachable. */
    public static String modVersion() {
        try {
            ensureResolved();
            Object v = loadClass(
                    "com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod", preferredLoader())
                    .getField("VERSION").get(null);
            return v == null ? null : String.valueOf(v);
        } catch (Throwable t) {
            return null;
        }
    }

    private static ClassLoader preferredLoader() {
        try {
            if (serverPlayerCls != null) {
                return serverPlayerCls.getClassLoader();
            }
        } catch (Throwable ignored) {
        }
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        return ctx != null ? ctx : ForgeBridge.class.getClassLoader();
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

    /** Master Adaptive Difficulty switch ({@code DifficultyConfig.enabled}). */
    public static boolean systemEnabled() {
        try {
            Object on = loadClass("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig", preferredLoader())
                    .getMethod("isEnabled").invoke(null);
            return on instanceof Boolean b && b;
        } catch (Throwable t) {
            try {
                Object cfg = loadClass("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig", preferredLoader())
                        .getMethod("get").invoke(null);
                Object raw = cfg.getClass().getField("enabled").get(cfg);
                return raw instanceof Boolean b && b;
            } catch (Throwable ignored) {
                // Fail closed — UI must not claim the system is on when Forge is unreachable.
                return false;
            }
        }
    }

    public static boolean whitelistEnabled() {
        try {
            Object on = loadClass("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig", preferredLoader())
                    .getMethod("isWhitelistEnabled").invoke(null);
            return on instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Whether this Bukkit player may use AD right now (master + whitelist). */
    public static boolean playerAllowed(Player player) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            // Fail closed when whitelist is on or Forge player resolve failed.
            return systemEnabled() && !whitelistEnabled();
        }
        try {
            ClassLoader cl = nms.getClass().getClassLoader();
            Class<?> sp = loadClass("net.minecraft.server.level.ServerPlayer", cl);
            Object allowed = loadClass("com.dbzlegacy.adaptivedifficulty.util.SystemGate", cl)
                    .getMethod("allows", sp)
                    .invoke(null, nms);
            return allowed instanceof Boolean b && b;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Opens Forge clickable chat menu (settings / chat backend). */
    public static boolean openChatMenu(Player player, String page) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return false;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        // Staff-only pages must not open for players who only have dmzdiff.gui.
        if (("settings".equalsIgnoreCase(target) || "stats".equalsIgnoreCase(target)
                || "statistics".equalsIgnoreCase(target) || "details".equalsIgnoreCase(target))
                && !isStaff(player)) {
            target = "main";
        }
        try {
            ensureResolved();
            if (chatMenuOpen == null) {
                return false;
            }
            chatMenuOpen.invoke(null, nms, target);
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
            case "down", "reset", "zero", "clear", "set", "lower_tier" -> "lower";
            case "buy", "activate", "purchase_tier" -> "buy";
            case "equip_title", "clear_title", "equip", "unequip_title" -> "titles";
            case "toggle_personal", "personal", "toggle_difficulty", "difficulty_toggle",
                 "toggle_coin_chat", "coin_chat", "toggle_chat", "chat_drops" -> "main";
            default -> "main";
        };
    }

    public static boolean forgeAvailable() {
        try {
            loadClass("com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod", null);
            return true;
        } catch (Throwable t) {
            resolveError = t.getClass().getSimpleName() + ": " + t.getMessage();
            return false;
        }
    }

    /** Last reflection/classloader failure detail (for logs / player messages). */
    public static String lastError() {
        return resolveError == null ? "" : resolveError;
    }

    public static boolean isStaff(Player player) {
        if (player == null) {
            return false;
        }
        // Align with Forge StaffAccess: op or configured adminPermission only.
        // Do not treat permission("*") as staff — that widens settings/admin set to anyone
        // whose permission plugin grants a wildcard node by default.
        return player.isOp() || player.hasPermission(adminPermission());
    }

    public static String adminSet(String key, String value) {
        try {
            String k = key == null ? "" : key.toLowerCase(Locale.ROOT);
            // Prefer dedicated setters so caches clear correctly.
            if ("enabled".equals(k) || "system".equals(k) || "systemenabled".equals(k)) {
                boolean on = Boolean.parseBoolean(value)
                        || "on".equalsIgnoreCase(value)
                        || "true".equalsIgnoreCase(value);
                return setSystemEnabled(on);
            }
            if ("whitelistenabled".equals(k) || "whitelist".equals(k)) {
                if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)
                        || "on".equalsIgnoreCase(value) || "off".equalsIgnoreCase(value))) {
                    return "Use true/false, or: /difficulty admin whitelist add <player>";
                }
                boolean on = "true".equalsIgnoreCase(value) || "on".equalsIgnoreCase(value);
                return setWhitelistEnabled(on);
            }
            Class<?> cfgCls = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig");
            Object cfg = cfgCls.getMethod("get").invoke(null);
            Field field = findConfigField(cfgCls, key);
            if (field == null) {
                return "Unknown key: " + key;
            }
            // Block mutating collections / migration flags via raw reflection.
            if (isLegacyUnusedCounterField(field.getName())) {
                return key + " is unused as of 3.3.35 (class + top-stat counters only)";
            }
            if (!isSafeAdminField(field.getName())) {
                return "Key not settable here: " + key + " (use a dedicated admin command)";
            }
            Object parsed = coerce(field.getType(), value);
            // Pre-clamp obvious foot-guns before assign.
            parsed = clampAdminValue(field.getName(), parsed);
            field.setAccessible(true);
            field.set(cfg, parsed);
            // Re-run config normalize so maxScaledMobs / intervals / costs stay valid.
            try {
                cfgCls.getMethod("sanitizeLive").invoke(null);
            } catch (NoSuchMethodException ignored) {
            }
            cfgCls.getMethod("save").invoke(null);
            Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache")
                    .getMethod("invalidateAll").invoke(null);
            clearAreaCache();
            PLACEHOLDER_CACHE.clear();
            Object live = field.get(cfg);
            return "Set " + key + " = " + live;
        } catch (Throwable t) {
            return "Failed: " + t.getMessage();
        }
    }

    public static String setSystemEnabled(boolean on) {
        try {
            Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig")
                    .getMethod("setEnabled", boolean.class)
                    .invoke(null, on);
            Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache")
                    .getMethod("invalidateAll").invoke(null);
            if (!on) {
                try {
                    Class.forName("com.dbzlegacy.adaptivedifficulty.tick.NearbyMobScaler")
                            .getMethod("shutdownAllScaling")
                            .invoke(null);
                } catch (Throwable ignored) {
                }
            }
            clearAreaCache();
            PLACEHOLDER_CACHE.clear();
            return on
                    ? "§aAdaptive Difficulty ENABLED.\n§7Scaling, rewards, AI, and tier purchases are active again."
                    : "§cAdaptive Difficulty DISABLED.\n§7No scaling, kill coins, AI, or tier purchases until re-enabled.\n§eRe-enable: §f/difficulty admin on";
        } catch (Throwable t) {
            return "§cFailed to toggle system: " + t.getMessage();
        }
    }

    public static String setWhitelistEnabled(boolean on) {
        try {
            Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig")
                    .getMethod("setWhitelistEnabled", boolean.class)
                    .invoke(null, on);
            Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache")
                    .getMethod("invalidateAll").invoke(null);
            clearAreaCache();
            PLACEHOLDER_CACHE.clear();
            int n = whitelistEntries().size();
            return on
                    ? "§eWhitelist ENABLED §7(" + n + " entries).\n§7Only listed players use Adaptive Difficulty.\n§eAdd: §f/difficulty admin whitelist add <player>"
                    : "§aWhitelist DISABLED.\n§7All players may use Adaptive Difficulty again (if system is on).";
        } catch (Throwable t) {
            return "§cFailed to toggle whitelist: " + t.getMessage();
        }
    }

    public static String systemStatusText() {
        boolean on = systemEnabled();
        boolean wl = whitelistEnabled();
        int n = whitelistEntries().size();
        return (on ? "§aSystem ENABLED" : "§cSystem DISABLED")
                + " §8· "
                + (wl ? "§eWhitelist ON §7(" + n + " entries)" : "§7Whitelist OFF")
                + "\n§8/difficulty admin whitelist on|off|add|remove|list";
    }

    public static String whitelistStatusText() {
        boolean wl = whitelistEnabled();
        int n = whitelistEntries().size();
        return (wl ? "§eWhitelist ON" : "§7Whitelist OFF")
                + " §8· §f" + n + " §7entries\n"
                + "§8/difficulty admin whitelist add|remove|list|on|off|toggle|clear";
    }

    @SuppressWarnings("unchecked")
    public static List<String> whitelistEntries() {
        try {
            Object list = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig")
                    .getMethod("whitelistEntries").invoke(null);
            if (list instanceof List<?> raw) {
                List<String> out = new ArrayList<>();
                for (Object o : raw) {
                    if (o != null) {
                        out.add(String.valueOf(o));
                    }
                }
                return out;
            }
        } catch (Throwable ignored) {
        }
        return List.of();
    }

    public static String whitelistListText() {
        List<String> entries = whitelistEntries();
        if (entries.isEmpty()) {
            return "§7Whitelist is empty. §8Add with §f/difficulty admin whitelist add <player>";
        }
        return "§6Whitelist §7(" + entries.size() + ")\n§f" + String.join("§8, §f", entries);
    }

    public static String whitelistAdd(String raw) {
        if (raw == null || raw.isBlank()) {
            return "§cUsage: /difficulty admin whitelist add <player>";
        }
        try {
            Class<?> cfg = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig");
            Player online = org.bukkit.Bukkit.getPlayerExact(raw.trim());
            boolean added = false;
            if (online != null) {
                added = Boolean.TRUE.equals(cfg.getMethod("addWhitelistEntry", String.class)
                        .invoke(null, online.getName())) || added;
                added = Boolean.TRUE.equals(cfg.getMethod("addWhitelistEntry", String.class)
                        .invoke(null, online.getUniqueId().toString())) || added;
            } else {
                added = Boolean.TRUE.equals(cfg.getMethod("addWhitelistEntry", String.class)
                        .invoke(null, raw.trim()));
            }
            clearAreaCache();
            PLACEHOLDER_CACHE.clear();
            if (!added) {
                return "§eAlready on whitelist: §f" + raw.trim();
            }
            String label = online != null ? online.getName() : raw.trim();
            return "§aAdded §f" + label + " §ato whitelist"
                    + (whitelistEnabled() ? "." : ".\n§eWhitelist is OFF — §f/difficulty admin whitelist on");
        } catch (Throwable t) {
            return "§cWhitelist add failed: " + t.getMessage();
        }
    }

    public static String whitelistRemove(String raw) {
        if (raw == null || raw.isBlank()) {
            return "§cUsage: /difficulty admin whitelist remove <player|uuid>";
        }
        try {
            Class<?> cfg = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig");
            Player online = org.bukkit.Bukkit.getPlayerExact(raw.trim());
            boolean removed = Boolean.TRUE.equals(cfg.getMethod("removeWhitelistEntry", String.class)
                    .invoke(null, raw.trim()));
            if (online != null) {
                removed = Boolean.TRUE.equals(cfg.getMethod("removeWhitelistEntry", String.class)
                        .invoke(null, online.getName())) || removed;
                removed = Boolean.TRUE.equals(cfg.getMethod("removeWhitelistEntry", String.class)
                        .invoke(null, online.getUniqueId().toString())) || removed;
            }
            clearAreaCache();
            PLACEHOLDER_CACHE.clear();
            if (!removed) {
                return "§cNot on whitelist: §f" + raw.trim();
            }
            return "§aRemoved §f" + raw.trim() + " §afrom whitelist.";
        } catch (Throwable t) {
            return "§cWhitelist remove failed: " + t.getMessage();
        }
    }

    public static String whitelistClear() {
        try {
            Class<?> cfgCls = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig");
            Object cfg = cfgCls.getMethod("get").invoke(null);
            Field listField = cfgCls.getField("whitelist");
            @SuppressWarnings("unchecked")
            List<String> list = (List<String>) listField.get(cfg);
            int n = list == null ? 0 : list.size();
            if (list != null) {
                list.clear();
            }
            cfgCls.getMethod("save").invoke(null);
            clearAreaCache();
            PLACEHOLDER_CACHE.clear();
            return "§aCleared whitelist (§f" + n + "§a entries).";
        } catch (Throwable t) {
            return "§cWhitelist clear failed: " + t.getMessage();
        }
    }

    private static void clearAreaCache() {
        try {
            Class.forName("com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty")
                    .getMethod("clearCache").invoke(null);
        } catch (Throwable ignored) {
        }
    }

    /** @return true when Forge reload succeeded (false if mod missing / load failed). */
    public static boolean reloadConfig() {
        try {
            Object ok = Class.forName("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig")
                    .getMethod("reload").invoke(null);
            Class.forName("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache")
                    .getMethod("invalidateAll").invoke(null);
            PLACEHOLDER_CACHE.clear();
            if (ok instanceof Boolean b) {
                return b;
            }
            return true;
        } catch (Throwable t) {
            resolveError = t.getClass().getSimpleName() + ": " + t.getMessage();
            return false;
        }
    }

    /**
     * Refresh unlock-tier grants and difficulty titles from current DMZ / prestige
     * (same sync chat menu runs when opening Buy / Titles).
     */
    public static void syncPlayerProgress(Player player) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return;
        }
        try {
            ensureResolved(nms.getClass().getClassLoader());
            if (cacheData == null) {
                return;
            }
            Object data = cacheData.invoke(null, nms);
            ClassLoader cl = nms.getClass().getClassLoader();
            Class.forName("com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem", true, cl)
                    .getMethod("syncUnlocks",
                            Class.forName("net.minecraft.server.level.ServerPlayer", true, cl),
                            Class.forName("com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData", true, cl))
                    .invoke(null, nms, data);
            Class.forName("com.dbzlegacy.adaptivedifficulty.title.TitleSystem", true, cl)
                    .getMethod("syncTierTitles",
                            Class.forName("net.minecraft.server.level.ServerPlayer", true, cl),
                            boolean.class)
                    .invoke(null, nms, false);
            PLACEHOLDER_CACHE.remove(player.getUniqueId());
        } catch (Throwable ignored) {
        }
    }

    /** Staff: clear V3 temporary difficulty (active tier/level). Unlocks & coins kept. */
    public static String resetPurchased(Player player) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return "§cCould not reach adaptive difficulty mod"
                    + (resolveError == null || resolveError.isBlank() ? "." : " (" + resolveError + ").");
        }
        try {
            ensureResolved(nms.getClass().getClassLoader());
            Object data = cacheData.invoke(null, nms);
            try {
                data.getClass().getMethod("resetTemporary").invoke(data);
            } catch (NoSuchMethodException legacy) {
                data.getClass().getMethod("setActiveDifficultyLevel", long.class).invoke(data, 0L);
                data.getClass().getMethod("setActiveTier", int.class).invoke(data, 0);
            }
            cacheCls.getMethod("save", serverPlayerCls).invoke(null, nms);
            cacheRefresh.invoke(null, nms);
            try {
                loadClass("com.dbzlegacy.adaptivedifficulty.tick.ScaledMobTracker",
                        nms.getClass().getClassLoader())
                        .getMethod("releaseAndRevertPlayer", serverPlayerCls)
                        .invoke(null, nms);
                loadClass("com.dbzlegacy.adaptivedifficulty.tick.NearbyMobScaler",
                        nms.getClass().getClassLoader())
                        .getMethod("processEvictions")
                        .invoke(null);
            } catch (Throwable ignored) {
            }
            loadClass("com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty", nms.getClass().getClassLoader())
                    .getMethod("clearCache").invoke(null);
            PLACEHOLDER_CACHE.remove(player.getUniqueId());
            return "§aCleared active tier/level. Unlocks & Ancient Coins kept.";
        } catch (Throwable t) {
            return "§cReset failed: " + t.getMessage();
        }
    }

    public static String areaDifficultyText(Player player) {
        Object nms = nmsPlayer(player);
        if (nms == null) {
            return "§cCould not reach adaptive difficulty mod"
                    + (resolveError == null || resolveError.isBlank() ? "." : " (" + resolveError + ").");
        }
        try {
            ClassLoader cl = nms.getClass().getClassLoader();
            ensureResolved(cl);
            Object snap = cacheRefresh.invoke(null, nms);
            long active = ((Number) snap.getClass().getField("active").get(snap)).longValue();
            long available = ((Number) snap.getClass().getField("availableMax").get(snap)).longValue();
            Object level = serverPlayerCls.getMethod("m_284548_").invoke(nms);
            Object pos = nms.getClass().getMethod("m_20183_").invoke(nms);
            long area = 0L;
            if (level != null && pos != null) {
                area = ((Number) loadClass("com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty", cl)
                        .getMethod("at", loadClass("net.minecraft.server.level.ServerLevel", cl),
                                loadClass("net.minecraft.core.BlockPos", cl))
                        .invoke(null, level, pos)).longValue();
            }
            Object cfg = loadClass("com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig", cl)
                    .getMethod("get").invoke(null);
            String mode = String.valueOf(cfg.getClass().getField("areaDifficultyMode").get(cfg));
            double radius = ((Number) cfg.getClass().getField("mobScaleRadius").get(cfg)).doubleValue();
            double group = ((Number) cfg.getClass().getField("areaGroupBonusPercent").get(cfg)).doubleValue();
            return "§6Area Difficulty\n"
                    + "§eActive tier CR proxy: §f" + active + " §7/ max §f" + available + "\n"
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
        ensureResolved(null);
    }

    private static void ensureResolved(ClassLoader preferred) throws Exception {
        if (resolved) {
            return;
        }
        synchronized (ForgeBridge.class) {
            if (resolved) {
                return;
            }
            try {
                serverPlayerCls = loadClass("net.minecraft.server.level.ServerPlayer", preferred);
                cacheCls = loadClass("com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache", preferred);
                actionsCls = loadClass("com.dbzlegacy.adaptivedifficulty.service.DifficultyActions", preferred);

                cacheGet = cacheCls.getMethod("get", serverPlayerCls);
                cacheRefresh = cacheCls.getMethod("refresh", serverPlayerCls);
                cacheData = cacheCls.getMethod("data", serverPlayerCls);

                // Optional: Ancient Coin economy + UnlockTier costs.
                try {
                    Class<?> economyCls = loadClass(
                            "com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy", preferred);
                    economyBalanceText = economyCls.getMethod("balanceText", serverPlayerCls);
                    try {
                        economyFormat = economyCls.getMethod("format", long.class);
                    } catch (NoSuchMethodException ignored) {
                        economyFormat = null;
                    }
                    economyFormatExactCost = economyCls.getMethod("formatExactCost", long.class);
                    try {
                        economyBalance = economyCls.getMethod("balance", serverPlayerCls);
                    } catch (NoSuchMethodException ignored) {
                        economyBalance = null;
                    }
                    try {
                        coinKindCls = loadClass(
                                "com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy$CoinKind",
                                preferred);
                        economyCountOf = economyCls.getMethod("countOf", serverPlayerCls, coinKindCls);
                    } catch (Throwable ignored) {
                        coinKindCls = null;
                        economyCountOf = null;
                    }
                    try {
                        Class<?> unlockTierCls = loadClass(
                                "com.dbzlegacy.adaptivedifficulty.tier.UnlockTier", preferred);
                        economyActivationCostPlayer = economyCls.getMethod(
                                "activationCost", unlockTierCls, serverPlayerCls);
                    } catch (Throwable ignored) {
                        economyActivationCostPlayer = null;
                    }
                } catch (Throwable missing) {
                    economyBalanceText = null;
                    economyFormat = null;
                    economyFormatExactCost = null;
                    economyBalance = null;
                    economyCountOf = null;
                    economyActivationCostPlayer = null;
                    coinKindCls = null;
                }
                try {
                    Class<?> unlockTierCls = loadClass(
                            "com.dbzlegacy.adaptivedifficulty.tier.UnlockTier", preferred);
                    unlockTierValues = unlockTierCls.getMethod("values");
                    unlockTierById = unlockTierCls.getMethod("byId", int.class);
                    unlockTierActivationCost = unlockTierCls.getMethod("activationCost");
                    unlockTierMaxDifficulty = unlockTierCls.getMethod("maxDifficulty");
                    try {
                        unlockTierActivationCostForLevel =
                                unlockTierCls.getMethod("activationCostForLevel", int.class);
                    } catch (NoSuchMethodException ignored) {
                        unlockTierActivationCostForLevel = null;
                    }
                } catch (Throwable missing) {
                    unlockTierValues = null;
                    unlockTierById = null;
                    unlockTierActivationCost = null;
                    unlockTierActivationCostForLevel = null;
                    unlockTierMaxDifficulty = null;
                }
                try {
                    Class<?> titleSys = loadClass(
                            "com.dbzlegacy.adaptivedifficulty.title.TitleSystem", preferred);
                    Class<?> titleCls = loadClass(
                            "com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle", preferred);
                    titleActiveDisplay = titleSys.getMethod("activeDisplay", serverPlayerCls);
                    titleActiveId = titleSys.getMethod("activeId", serverPlayerCls);
                    titleHas = titleSys.getMethod("has", serverPlayerCls, titleCls);
                    titleValues = titleCls.getMethod("values");
                    titleRequirementTip = titleCls.getMethod("requirementTip");
                } catch (Throwable missing) {
                    titleActiveDisplay = null;
                    titleActiveId = null;
                    titleHas = null;
                    titleValues = null;
                    titleRequirementTip = null;
                }

                try {
                    Class<?> snapCls = loadClass(
                            "com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot", preferred);
                    snapshotState = snapCls.getMethod("state");
                    snapshotStateColor = snapCls.getMethod("stateColorCode");
                } catch (Throwable missing) {
                    snapshotState = null;
                    snapshotStateColor = null;
                }

                actionsHandle = actionsCls.getMethod(
                        "handle", serverPlayerCls, String.class, long.class, String.class);
                try {
                    actionsHandleArg = actionsCls.getMethod(
                            "handleArg", serverPlayerCls, String.class, String.class, String.class);
                } catch (NoSuchMethodException missing) {
                    actionsHandleArg = null;
                }
                try {
                    actionsHandleArgNoReopen = actionsCls.getMethod(
                            "handleArgNoReopen", serverPlayerCls, String.class, String.class, String.class);
                } catch (NoSuchMethodException missing) {
                    actionsHandleArgNoReopen = null;
                }
                Class<?> resultCls = loadClass(
                        "com.dbzlegacy.adaptivedifficulty.service.DifficultyActions$Result", preferred);
                try {
                    resultMessage = resultCls.getMethod("message");
                    RESULT_MESSAGE_FIELD = null;
                } catch (NoSuchMethodException missing) {
                    resultMessage = null;
                    RESULT_MESSAGE_FIELD = resultCls.getField("message");
                }
                resultOk = resultCls.getMethod("ok");
                try {
                    chatMenuOpen = loadClass(
                            "com.dbzlegacy.adaptivedifficulty.gui.DifficultyChatMenu", preferred)
                            .getMethod("open", serverPlayerCls, String.class);
                } catch (Throwable missing) {
                    chatMenuOpen = null;
                }
                resolveError = null;
                resolved = true;
            } catch (Throwable t) {
                resolveError = t.getClass().getSimpleName() + ": " + t.getMessage();
                throw t instanceof Exception e ? e : new Exception(t);
            }
        }
    }

    private static Object readResultMessage(Object result) throws Exception {
        if (resultMessage != null) {
            return resultMessage.invoke(result);
        }
        if (RESULT_MESSAGE_FIELD != null) {
            return RESULT_MESSAGE_FIELD.get(result);
        }
        return "";
    }

    /** Load a class trying Mohist/Forge-friendly classloaders first. */
    private static Class<?> loadClass(String name, ClassLoader preferred) throws ClassNotFoundException {
        ClassNotFoundException last = null;
        List<ClassLoader> loaders = new ArrayList<>();
        if (preferred != null) {
            loaders.add(preferred);
        }
        ClassLoader ctx = Thread.currentThread().getContextClassLoader();
        if (ctx != null) {
            loaders.add(ctx);
        }
        loaders.add(ForgeBridge.class.getClassLoader());
        try {
            Object server = Class.forName("org.bukkit.Bukkit").getMethod("getServer").invoke(null);
            if (server != null) {
                loaders.add(server.getClass().getClassLoader());
            }
        } catch (Throwable ignored) {
        }
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                return Class.forName(name, true, loader);
            } catch (ClassNotFoundException e) {
                last = e;
            }
        }
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            if (last != null) {
                throw last;
            }
            throw e;
        }
    }

    private static Field findConfigField(Class<?> cfgCls, String key) {
        String k = key == null ? "" : key.toLowerCase(Locale.ROOT);
        Map<String, String> aliases = Map.ofEntries(
                Map.entry("guibackend", "guiBackend"),
                Map.entry("vanilladifficulty", "vanillaDifficulty"),
                Map.entry("enablemobscaling", "enableMobScaling"),
                Map.entry("adminpermission", "adminPermission"),
                Map.entry("enabled", "enabled"),
                Map.entry("system", "enabled"),
                Map.entry("systemenabled", "enabled"),
                Map.entry("whitelistenabled", "whitelistEnabled"),
                Map.entry("rewardscaling", "rewardScaling"),
                Map.entry("prestigemultiplier", "prestigeMultiplier"),
                Map.entry("levelmultiplier", "levelMultiplier"),
                Map.entry("teambonus", "teamBonusPercent"),
                Map.entry("teambonuspercent", "teamBonusPercent"),
                Map.entry("maxhealthmultiplier", "maxHealthMultiplier"),
                Map.entry("maxscaledhealth", "maxScaledHealth"),
                Map.entry("mobscaleradius", "mobScaleRadius"),
                Map.entry("mobhealthscale", "mobHealthScale"),
                Map.entry("ancientcoinupgradechance", "ancientCoinUpgradeChance"),
                Map.entry("nearbyscaleintervalticks", "nearbyScaleIntervalTicks"),
                Map.entry("nearbyscaleinterval", "nearbyScaleIntervalTicks"),
                Map.entry("maxscaledmobsperplayer", "maxScaledMobsPerPlayer"),
                Map.entry("nearbyscalebudgetperplayer", "maxScaledMobsPerPlayer"),
                Map.entry("tiercostleveldivisor", "tierCostLevelDivisor"),
                Map.entry("enableclasscounters", "enableClassCounters"),
                Map.entry("classcounters", "enableClassCounters"),
                Map.entry("enablestrongstatcounters", "enableStrongStatCounters"),
                Map.entry("strongstatcounters", "enableStrongStatCounters"),
                Map.entry("topstatcounters", "enableStrongStatCounters"),
                Map.entry("strongstatcountermult", "strongStatCounterMult"),
                Map.entry("strongcounter", "strongStatCounterMult"),
                Map.entry("topstatmult", "strongStatCounterMult"),
                Map.entry("classcounterdamagemult", "classCounterDamageMult"),
                Map.entry("classdmg", "classCounterDamageMult"),
                Map.entry("classcounterhealthmult", "classCounterHealthMult"),
                Map.entry("classhp", "classCounterHealthMult"),
                Map.entry("classcounterarmormult", "classCounterArmorMult"),
                Map.entry("classarmor", "classCounterArmorMult"),
                Map.entry("maxcounteroverlaymult", "maxCounterOverlayMult"),
                Map.entry("countercap", "maxCounterOverlayMult"),
                Map.entry("overlaycap", "maxCounterOverlayMult"),
                Map.entry("transformscaleweight", "transformScaleWeight"),
                Map.entry("transformweight", "transformScaleWeight"),
                Map.entry("formscaleweight", "transformScaleWeight"),
                Map.entry("transformscaleexponent", "transformScaleExponent"),
                Map.entry("transformexponent", "transformScaleExponent"),
                Map.entry("formscaleexponent", "transformScaleExponent")
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

    /** Legacy counter knobs kept in JSON for migrate/compat; no combat effect. */
    private static boolean isLegacyUnusedCounterField(String fieldName) {
        if (fieldName == null) {
            return false;
        }
        return switch (fieldName) {
            case "weakStatCounterMult", "weakDefensePierceMult",
                 "specializationDamageTax", "raceCounterMult" -> true;
            default -> false;
        };
    }

    /**
     * Allowlist of scalar config fields settable via Bukkit {@code admin set}.
     * Deny-by-default: collections, migration flags, and {@code adminPermission}
     * (privilege escalation if set to a common/default node) are never writable here.
     */
    private static boolean isSafeAdminField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        return switch (fieldName) {
            case "enabled", "whitelistEnabled",
                 "prestigeMultiplier", "levelMultiplier", "teamBonusPercent",
                 "contributionPercent", "rewardScaling",
                 "combatCurveExponent", "combatCurvePivot",
                 "healthCurveExponent", "healthCurvePivot",
                 "healthPercentPerDifficulty", "damagePercentPerDifficulty",
                 "defensePercentPerDifficulty", "movementPercentPer100Difficulty",
                 "dmzExtraHealthPercent", "dmzExtraDamagePercent",
                 "dmzExtraDefensePercent", "dmzExtraKiDamagePercent",
                 "tierAwakened", "tierEnhanced", "tierElite", "tierAdvanced",
                 "tierMaster", "tierLegendary", "tierGod", "tierDivine",
                 "tierImpossible", "tierTranscendent", "tierEternal", "tierMythic",
                 "tierOmega", "tierAbsolute", "tierApex", "tierZenith",
                 "referenceMaxLevel", "referenceMaxPrestige", "hardCapDifficulty",
                 "mobScaleRadius", "enableMobScaling", "scaleHostileOnly",
                 "applyDmzExtrasToAllHostiles", "enableRewardScaling",
                 "enableElites", "eliteChancePercent", "eliteStatMultiplier",
                 "enableMutations", "mutationChancePercent",
                 "enableAdaptiveAi", "enableEnemyEvolution", "enableBossScaling",
                 "bossStatMultiplier", "bossHealthThreshold",
                 "maxHealthMultiplier", "maxScaledHealth", "maxMoveMultiplier",
                 "maxArmorBonus", "maxDamageMultiplier",
                 "guiBackend", "vanillaDifficulty", "areaDifficultyMode",
                 "areaGroupBonusPercent", "areaDifficultyVariancePercent",
                 "combatRatingDmzWeight", "combatRatingPrestigeWeight",
                 "combatRatingTransformWeight", "combatRatingDifficultyWeight",
                 "unlockTier1Level", "unlockTier2Level", "unlockTier3Level",
                 "unlockTier4Level", "unlockTier5Level", "unlockTier6Level",
                 "unlockTier7Level",
                 "unlockTier1Max", "unlockTier2Max", "unlockTier3Max",
                 "unlockTier4Max", "unlockTier5Max", "unlockTier6Max",
                 "unlockTier7Max",
                 "unlockTier1Cost", "unlockTier2Cost", "unlockTier3Cost",
                 "unlockTier4Cost", "unlockTier5Cost", "unlockTier6Cost",
                 "unlockTier7Cost",
                 "unlockTier1EnemyMult", "unlockTier2EnemyMult", "unlockTier3EnemyMult",
                 "unlockTier4EnemyMult", "unlockTier5EnemyMult", "unlockTier6EnemyMult",
                 "unlockTier7EnemyMult",
                 "enableClassCounters", "enableStrongStatCounters",
                 "strongStatCounterMult", "classCounterDamageMult",
                 "classCounterHealthMult", "classCounterArmorMult",
                 "maxCounterOverlayMult", "transformScaleWeight", "transformScaleExponent",
                 "defenseToArmorFactor",
                 "nearbyScaleIntervalTicks", "maxScaledMobsPerPlayer",
                 "nearbyScaleBudgetPerPlayer", "tierCostLevelDivisor",
                 "enableAncientCoinDrops", "ancientCoinDropMult",
                 "ancientCoinRatingDivisor", "ancientCoinUpgradeChance",
                 "deathResetsActiveDifficulty", "mobHealthScale",
                 "tankDamageDefenseRatio", "tankDamageHealthRatio",
                 "maxFormBoost", "maxLiveCombatChannel",
                 "eliteMinUnlockTier", "mutationMinUnlockTier",
                 "adaptiveAiMinUnlockTier", "enemyEvolutionMinUnlockTier",
                 "bossMechanicsMinUnlockTier" -> true;
            // Legacy unused: weakStatCounterMult, weakDefensePierceMult,
            // specializationDamageTax, raceCounterMult — not live-settable.
            default -> false;
        };
    }

    private static Object clampAdminValue(String fieldName, Object parsed) {
        if (parsed == null || fieldName == null) {
            return parsed;
        }
        try {
            return switch (fieldName) {
                case "maxScaledMobsPerPlayer", "nearbyScaleBudgetPerPlayer" -> {
                    int n = ((Number) parsed).intValue();
                    yield Math.max(1, Math.min(5, n));
                }
                case "nearbyScaleIntervalTicks" -> Math.max(10, ((Number) parsed).intValue());
                case "mobScaleRadius" -> {
                    double r = ((Number) parsed).doubleValue();
                    yield Math.max(8.0, Math.min(128.0, r));
                }
                case "mobHealthScale" -> {
                    double h = ((Number) parsed).doubleValue();
                    // Match DifficultyConfig.sanitizeLive stock default.
                    yield h <= 0.0 || h > 4.0 ? 1.15 : h;
                }
                case "tankDamageDefenseRatio" -> {
                    double r = ((Number) parsed).doubleValue();
                    yield r < 0.0 || r > 10.0 || Double.isNaN(r) ? 0.45 : r;
                }
                case "tankDamageHealthRatio" -> {
                    double r = ((Number) parsed).doubleValue();
                    yield r < 0.0 || r > 1.0 || Double.isNaN(r) ? 0.22 : r;
                }
                case "maxFormBoost" -> {
                    double m = ((Number) parsed).doubleValue();
                    // Matches DifficultyConfig.sanitizeLive bounds.
                    if (!(m > 1.0) || m > 500.0 || Double.isNaN(m)) {
                        yield 100.0;
                    }
                    yield m;
                }
                case "maxLiveCombatChannel" -> {
                    double m = ((Number) parsed).doubleValue();
                    if (!(m > 1.0) || m > 1.0e12 || Double.isNaN(m)) {
                        yield 50_000_000.0;
                    }
                    yield m;
                }
                case "ancientCoinUpgradeChance" -> {
                    double c = ((Number) parsed).doubleValue();
                    yield Math.max(0.0, Math.min(0.25, c));
                }
                case "ancientCoinDropMult" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(0.0, Math.min(10.0, m));
                }
                case "ancientCoinRatingDivisor" -> {
                    double d = ((Number) parsed).doubleValue();
                    yield Math.max(1.0, Math.min(1_000_000.0, d));
                }
                case "prestigeMultiplier", "levelMultiplier", "rewardScaling" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(0.0, Math.min(50.0, m));
                }
                case "teamBonusPercent", "contributionPercent",
                     "areaGroupBonusPercent", "areaDifficultyVariancePercent",
                     "movementPercentPer100Difficulty" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(0.0, Math.min(5.0, m));
                }
                case "eliteChancePercent", "mutationChancePercent" -> {
                    double c = ((Number) parsed).doubleValue();
                    // Live admin set: keep rarities from becoming a free farm.
                    yield Math.max(0.0, Math.min(25.0, c));
                }
                case "eliteStatMultiplier", "bossStatMultiplier" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(1.0, Math.min(5.0, m));
                }
                case "maxDamageMultiplier", "maxHealthMultiplier", "maxMoveMultiplier" -> {
                    double m = ((Number) parsed).doubleValue();
                    // 0 = uncapped (documented); otherwise keep a sane ceiling.
                    if (m <= 0.0) {
                        yield 0.0;
                    }
                    yield Math.max(1.0, Math.min(20.0, m));
                }
                case "maxScaledHealth", "maxArmorBonus", "bossHealthThreshold" -> {
                    double m = ((Number) parsed).doubleValue();
                    if (m <= 0.0) {
                        yield 0.0; // uncapped / disabled
                    }
                    yield Math.max(1.0, Math.min(100_000.0, m));
                }
                case "dmzExtraKiDamagePercent", "dmzExtraDamagePercent",
                     "dmzExtraHealthPercent", "dmzExtraDefensePercent",
                     "damagePercentPerDifficulty", "healthPercentPerDifficulty",
                     "defensePercentPerDifficulty" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(0.0, Math.min(2.0, m));
                }
                case "unlockTier1EnemyMult", "unlockTier2EnemyMult", "unlockTier3EnemyMult",
                     "unlockTier4EnemyMult", "unlockTier5EnemyMult", "unlockTier6EnemyMult",
                     "unlockTier7EnemyMult" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(0.05, Math.min(4.0, m));
                }
                case "strongStatCounterMult", "classCounterDamageMult",
                     "classCounterHealthMult", "classCounterArmorMult" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(1.0, Math.min(3.0, m));
                }
                case "maxCounterOverlayMult" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(1.0, Math.min(4.0, m));
                }
                case "transformScaleWeight" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(0.0, Math.min(1.0, m));
                }
                case "transformScaleExponent" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(0.20, Math.min(1.0, m));
                }
                case "defenseToArmorFactor" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(0.1, Math.min(50.0, m));
                }
                case "tierCostLevelDivisor" -> {
                    double m = ((Number) parsed).doubleValue();
                    yield Math.max(1.0, Math.min(10_000.0, m));
                }
                case "eliteMinUnlockTier", "mutationMinUnlockTier",
                     "adaptiveAiMinUnlockTier", "enemyEvolutionMinUnlockTier",
                     "bossMechanicsMinUnlockTier" -> {
                    int n = ((Number) parsed).intValue();
                    yield Math.max(0, Math.min(7, n));
                }
                case "unlockTier1Cost", "unlockTier2Cost", "unlockTier3Cost",
                     "unlockTier4Cost", "unlockTier5Cost", "unlockTier6Cost",
                     "unlockTier7Cost" -> Math.max(1L, ((Number) parsed).longValue());
                case "guiBackend" -> {
                    String s = String.valueOf(parsed).trim().toLowerCase(Locale.ROOT);
                    yield switch (s) {
                        case "cmi", "cmilib", "cmigui", "deluxemenus", "deluxe", "dm" -> "cmi";
                        case "chest", "bukkit", "inventory", "gui" -> "chest";
                        case "chat" -> "chat";
                        case "auto" -> "auto";
                        default -> "auto";
                    };
                }
                case "vanillaDifficulty" -> {
                    String s = String.valueOf(parsed).trim().toLowerCase(Locale.ROOT);
                    yield switch (s) {
                        case "peaceful", "easy", "normal", "hard" -> s;
                        default -> "hard";
                    };
                }
                default -> parsed;
            };
        } catch (Throwable t) {
            return parsed;
        }
    }

    private static Object coerce(Class<?> type, String value) {
        if (type == String.class) {
            return value;
        }
        if (type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value)
                    || "on".equalsIgnoreCase(value)
                    || "yes".equalsIgnoreCase(value);
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
        // Refuse List/Map/etc. mutation through this path.
        throw new IllegalArgumentException("Unsupported config type: " + type.getSimpleName());
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
