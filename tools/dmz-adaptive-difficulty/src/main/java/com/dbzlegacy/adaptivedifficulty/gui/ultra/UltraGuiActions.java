package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.GuiNotice;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;

/** Dispatches KaiPath GuiActionPacket presses to existing server menu APIs. */
public final class UltraGuiActions {
    private UltraGuiActions() {}

    public static void dispatch(ServerPlayer player, String screenId, String actionId, String argsJson) {
        if (player == null) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra action dropped: null player screenId={} actionId={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    screenId,
                    actionId);
            return;
        }
        if (actionId == null || actionId.isBlank()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra action missing actionId screenId={} args={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    screenId,
                    argsJson);
            return;
        }

        JsonObject args = parse(argsJson);
        if (args == null) {
            args = new JsonObject();
        }
        Qualified q = qualify(screenId, actionId, args);
        if (q == null) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra qualify returned null screenId={} actionId={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    screenId,
                    actionId);
            return;
        }
        String screen = q.screen() == null ? "hub" : q.screen();
        String action = q.action() == null ? "" : q.action();

        AdaptiveDifficultyMod.LOGGER.debug(
                "[{}] Ultra action player={} rawActionId={} screen={} resolvedAction={} args={}",
                AdaptiveDifficultyMod.MOD_ID,
                safeName(player),
                actionId,
                screen,
                action,
                args);

        try {
            dispatchResolved(player, screen, action, args, actionId);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] UltraGuiActions.handle failed rawActionId={} screen={} resolvedAction={} args={} player={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    actionId,
                    screen,
                    action,
                    args,
                    safeName(player),
                    t);
            DmzRewards.msg(
                    player,
                    UltraStyle.dangerLine(
                            "Something went wrong. Try again, or ask staff if it keeps happening."));
        }
    }

    private static void dispatchResolved(
            ServerPlayer player,
            String screen,
            String action,
            JsonObject args,
            String rawActionId) {
        if (player == null) {
            return;
        }
        if (action == null || action.isBlank()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra blank resolved action screen={} rawActionId={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    screen,
                    rawActionId);
            return;
        }
        if (args == null) {
            args = new JsonObject();
        }
        String safeScreen = screen == null || screen.isBlank() ? "hub" : screen;

        // KaiPath sometimes sends bare system names (e.g. "character") from the hub
        // instead of open:character / @hub:open:character.
        if (!hasVerbPrefix(action) && isHubScreen(safeScreen)) {
            String openTarget = bareHubOpenTarget(action);
            if (openTarget != null) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] Ultra bare hub action rawActionId={} → open:{}",
                        AdaptiveDifficultyMod.MOD_ID,
                        rawActionId,
                        openTarget);
                UltraGui.open(player, openTarget, "main");
                return;
            }
        }

        if (action.startsWith("tab:")) {
            String tab = action.length() > 4 ? action.substring(4) : "";
            if (tab.isBlank()) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Ultra empty tab: screen={} rawActionId={}",
                        AdaptiveDifficultyMod.MOD_ID,
                        safeScreen,
                        rawActionId);
                return;
            }
            // Same screenId, updated page — never CNPC, never a different system.
            String page = tabPage(safeScreen, tab);
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Ultra tab: screen={} tab={} → page={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeScreen,
                    tab,
                    page);
            UltraGui.open(player, safeScreen, page);
            return;
        }
        if ("nav:hub".equals(action)) {
            UltraGui.openHub(player, "main");
            return;
        }
        if ("nav:home".equals(action)) {
            UltraGui.open(player, safeScreen, homePage(safeScreen));
            return;
        }
        if ("nav:refresh".equals(action)) {
            String page = stringArg(args, "page");
            if (page.isBlank()) {
                page = homePage(safeScreen);
            }
            UltraGui.open(player, safeScreen, page);
            return;
        }
        if (action.startsWith("open:")) {
            String target = action.length() > 5 ? action.substring(5) : "";
            if (target.isBlank()) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Ultra empty open: screen={} rawActionId={}",
                        AdaptiveDifficultyMod.MOD_ID,
                        safeScreen,
                        rawActionId);
                return;
            }
            // Always Ultra GuiOpenPacket for the target system (no CNPC submenu fallback).
            UltraGui.open(player, target, "main");
            return;
        }

        DoParts parts = parseDo(action);
        if (parts == null || parts.action == null || parts.action.isBlank()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra empty do-action screen={} rawActionId={} resolvedAction={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeScreen,
                    rawActionId,
                    action);
            DmzRewards.msg(
                    player,
                    UltraStyle.dangerLine("That menu action is not available."));
            return;
        }
        // Prefer arg encoded in the action id (do:action:arg…); fall back to argsJson.
        if (parts.arg != null && !parts.arg.isBlank()) {
            args.addProperty("arg", parts.arg);
        } else if (!args.has("arg") || args.get("arg") == null || args.get("arg").isJsonNull()) {
            args.addProperty("arg", "");
        }

        String msg = runDo(player, safeScreen, parts.action, args, rawActionId);
        if (msg != null && !msg.isBlank()) {
            String notice = GuiNotice.noticeBody(msg);
            if (notice != null && !notice.isBlank()) {
                DmzRewards.msg(player, notice);
            }
        }
        // saga_confirm opens its page inside characterDo.
        if ("saga_confirm".equals(parts.action)) {
            return;
        }
        String reopen = reopenPage(safeScreen, parts, args);
        UltraGui.open(
                player,
                safeScreen,
                reopen == null || reopen.isBlank() ? homePage(safeScreen) : reopen);
    }

    /** True when action already uses a protocol verb. */
    static boolean hasVerbPrefix(String action) {
        if (action == null || action.isBlank()) {
            return false;
        }
        return action.startsWith("do:")
                || action.startsWith("tab:")
                || action.startsWith("nav:")
                || action.startsWith("open:");
    }

    static boolean isHubScreen(String screen) {
        if (screen == null || screen.isBlank()) {
            return true;
        }
        String s = screen.toLowerCase();
        return "hub".equals(s) || "lm".equals(s) || "legacymechanics".equals(s) || "main".equals(s);
    }

    /**
     * Map a bare hub tile id to an {@code open:} target, or null if not a known system.
     * Handles KaiPath clients that send simplified action IDs without verb prefixes.
     */
    static String bareHubOpenTarget(String action) {
        if (action == null || action.isBlank() || action.indexOf(':') >= 0) {
            return null;
        }
        return switch (action.trim().toLowerCase()) {
            case "character", "charservices", "char" -> "character";
            case "difficulty", "diff" -> "difficulty";
            case "progression", "prog" -> "progression";
            case "prestige" -> "prestige";
            case "spar", "sparring" -> "spar";
            case "rival" -> "rival";
            case "config", "cfgedit", "configeditor" -> "config";
            case "admin" -> "admin";
            case "hub", "lm", "legacymechanics", "main" -> "hub";
            default -> null;
        };
    }

    private static String runDo(
            ServerPlayer player, String screen, String act, JsonObject args, String rawActionId) {
        if (player == null || act == null || act.isBlank()) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra empty act screen={} rawActionId={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    screen,
                    rawActionId);
            return UltraStyle.dangerLine("That menu action is not available.");
        }
        if (args == null) {
            args = new JsonObject();
        }
        String arg = stringArg(args, "arg");
        String page = stringArg(args, "page");
        if (page.isBlank()) {
            page = homePage(screen);
        }
        String safeScreen = screen == null ? "" : screen;
        String result;
        try {
            result = switch (safeScreen) {
                case "difficulty" -> {
                    DifficultyActions.Result r =
                            DifficultyActions.handleArgNoReopen(player, act, arg, page);
                    if (r == null) {
                        yield "";
                    }
                    yield r.message() == null ? "" : r.message();
                }
                case "rival" -> nullToEmpty(RivalGuiApi.handleDo(player, act, arg, page));
                case "spar", "sparring" -> nullToEmpty(SparGuiApi.handleDo(player, act, arg, page));
                case "prestige" ->
                        nullToEmpty(ProgressionGuiApi.handlePrestigeDo(player, act, arg, page));
                case "character", "saga" -> characterDo(player, act, arg, page);
                case "progression" -> nullToEmpty(ProgressionGuiApi.handleDo(player, act, arg, page));
                case "config" -> nullToEmpty(UltraConfigScreens.handleAction(player, act, arg, page));
                case "hub", "admin" -> nullToEmpty(MechanicsGuiApi.handleDo(player, act, arg, page));
                case "team" -> nullToEmpty(DifficultyTeamGuiApi.handleDo(player, act, arg, page));
                default -> null;
            };
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] Ultra runDo threw screen={} action={} arg={} page={} rawActionId={} player={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeScreen,
                    act,
                    arg,
                    page,
                    rawActionId,
                    safeName(player),
                    t);
            return UltraStyle.dangerLine(
                    "Something went wrong. Try again, or ask staff if it keeps happening.");
        }
        if (result == null) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra unrecognized screen=\"{}\" rawActionId=\"{}\" action=\"{}\" arg=\"{}\"",
                    AdaptiveDifficultyMod.MOD_ID,
                    screen,
                    rawActionId,
                    act,
                    arg);
            return UltraStyle.dangerLine("That menu action is not available.");
        }
        if (isGenericFailure(result)) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra handler generic failure screen=\"{}\" rawActionId=\"{}\" action=\"{}\" arg=\"{}\" msg={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    screen,
                    rawActionId,
                    act,
                    arg,
                    UltraStyle.plain(result));
        }
        return result;
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static boolean isGenericFailure(String msg) {
        if (msg == null) {
            return false;
        }
        String plain = UltraStyle.plain(msg).toLowerCase();
        return plain.contains("something went wrong");
    }

    private static String characterDo(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return UltraStyle.dangerLine("This only works in-game.");
        }
        if (action == null || action.isBlank()) {
            return UltraStyle.dangerLine("That menu action is not available.");
        }
        if (UltraStyle.PAGE_ANDROID_REMOVE.equals(action) || "android".equals(action)) {
            String msg = ProgressionGuiApi.handleDo(player, action, arg, page);
            return msg == null ? "" : msg;
        }
        if ("saga_confirm".equals(action)) {
            String id = arg == null ? "" : arg.trim();
            if (id.isBlank()) {
                return UltraStyle.dangerLine("Choose a saga first.");
            }
            UltraGui.open(player, "character", "saga_confirm:" + id);
            return "";
        }
        if ("saga_reset".equals(action) || "saga".equals(action) || "reset_saga".equals(action)) {
            String id = arg == null ? "" : arg.trim();
            if (id.isBlank()) {
                return UltraStyle.dangerLine("Choose a saga first.");
            }
            String result = SagaResetService.reset(player, id);
            return result == null ? "" : result;
        }
        String msg = CharacterServicesGuiApi.handleDo(player, action, arg, page);
        return msg == null ? "" : msg;
    }

    /**
     * Parses {@code do:action}, {@code do:action:arg}, or {@code do:action:arg:more}.
     * Everything after the first colon following the action name is the arg
     * (so {@code do:boost:1.25:30} → action {@code boost}, arg {@code 1.25:30}).
     */
    static DoParts parseDo(String action) {
        String raw = stripDo(action);
        if (raw == null || raw.isBlank()) {
            return new DoParts("", "");
        }
        int colon = raw.indexOf(':');
        if (colon < 0) {
            return new DoParts(raw, "");
        }
        return new DoParts(raw.substring(0, colon), raw.substring(colon + 1));
    }

    static String stripDo(String action) {
        if (action != null && action.startsWith("do:")) {
            return action.substring(3);
        }
        return action == null ? "" : action;
    }

    /**
     * Resolve logical screen + action from packet fields.
     *
     * <p>Priority (hub wire shim aware):
     * <ol>
     *   <li>{@code @logical:action} prefix on actionId
     *   <li>{@code args.system} / {@code args.screen} (logical metadata from button args)
     *   <li>packet {@code screenId} (may be {@code hub} when KaiPath shim is active)
     * </ol>
     * Bare hub tile names ({@code character} → {@code open:character}) still work when the
     * resolved screen is hub.
     */
    static Qualified qualify(String screenId, String actionId, JsonObject args) {
        String action = actionId == null ? "" : actionId.trim();
        String screen = "";
        if (action.startsWith("@")) {
            int colon = action.indexOf(':');
            if (colon > 1) {
                screen = action.substring(1, colon).trim().toLowerCase();
                action = action.substring(colon + 1).trim();
            } else {
                action = action.substring(1).trim();
            }
        }
        // Prefer logical metadata over wire screenId (hub shim tunnels submenus as hub).
        if (screen.isBlank() && args != null) {
            screen = stringArg(args, UltraScreenIds.META_SYSTEM);
        }
        if (screen.isBlank() && args != null) {
            screen = stringArg(args, "screen");
        }
        if (screen.isBlank() && screenId != null && !screenId.isBlank()) {
            // Wire hub with no metadata → real hub. Otherwise treat packet id as logical.
            screen = screenId.trim();
        }
        if (screen.isBlank()) {
            screen = "hub";
        }
        screen = UltraScreenIds.normalizeLogical(screen);
        return new Qualified(screen, action);
    }

    private static String reopenPage(String screen, DoParts parts, JsonObject args) {
        if (parts == null) {
            return homePage(screen);
        }
        if ("config".equals(screen) && "open_module".equals(parts.action) && parts.arg != null && !parts.arg.isBlank()) {
            return "module:" + parts.arg;
        }
        if (("character".equals(screen) || "saga".equals(screen))
                && ("saga_reset".equals(parts.action) || "saga".equals(parts.action))) {
            return "reset";
        }
        String page = stringArg(args, "page");
        if (!page.isBlank()) {
            return page;
        }
        // Keep player on the tab they acted from when possible.
        return switch (screen == null ? "" : screen) {
            case "difficulty" -> switch (parts.action == null ? "" : parts.action) {
                case "activate", "lower_tier", "purchase_tier", "buy", "toggle_personal" -> "tiers";
                case "summon_end_dragon", "end_dragon", "summon_dragon" -> "summons";
                default -> homePage(screen);
            };
            case "prestige" -> switch (parts.action == null ? "" : parts.action) {
                case "confirm", "turnin", "turn_in", "redeem" -> "turnin";
                case "skill", "buy_skill", "skillup" -> "shop";
                default -> homePage(screen);
            };
            case "progression" -> switch (parts.action == null ? "" : parts.action) {
                case "boost" -> "boosts";
                case "toggle_staff_free_coins", "staff_free_coins" -> "coins";
                default -> homePage(screen);
            };
            case "spar", "sparring" -> switch (parts.action == null ? "" : parts.action) {
                case "dojo_accept", "dojo_war_accept", "dojo_war_cancel", "dojo_war_revoke",
                        "dojo_cancel", "dojo_challenge" -> "dojo_war";
                case "mentor", "mentor_accept", "mentor_decline", "mentor_tpmsg", "mentor_tp_msg" -> "dojo";
                default -> homePage(screen);
            };
            case "rival" -> switch (parts.action == null ? "" : parts.action) {
                case "challenge", "challenge_accept", "challenge_decline", "challenge_cancel",
                        "challenge_send", "spectate", "spectate_stop" -> "challenge";
                case "tpmsg", "tp_msg", "instinct", "declare", "accept", "decline", "silent",
                        "remove", "accept_replace" -> "actions";
                default -> homePage(screen);
            };
            case "character" -> switch (parts.action == null ? "" : parts.action) {
                case "android_remove", "android" -> UltraStyle.PAGE_ANDROID_REMOVE;
                default -> homePage(screen);
            };
            case "admin" -> "admin";
            default -> homePage(screen);
        };
    }

    private static String homePage(String screen) {
        return switch (screen == null ? "" : screen) {
            case "rival" -> "home";
            case "config" -> "list";
            case "admin" -> "admin";
            default -> "main";
        };
    }

    private static String tabPage(String screen, String tab) {
        String t = tab == null ? "" : tab.toLowerCase();
        return switch (screen == null ? "" : screen) {
            case "character" -> switch (t) {
                case "saga", "reset" -> "reset";
                case "android", "android_remove" -> UltraStyle.PAGE_ANDROID_REMOVE;
                default -> "main";
            };
            case "difficulty" -> switch (t) {
                case "tiers" -> "tiers";
                case "summons" -> "summons";
                default -> "main";
            };
            case "rival" -> switch (t) {
                case "actions" -> "actions";
                case "challenge" -> "challenge";
                default -> "home";
            };
            case "spar", "sparring" -> switch (t) {
                case "dojo" -> "dojo";
                case "wars", "dojo_war" -> "dojo_war";
                default -> "main";
            };
            case "prestige" -> switch (t) {
                case "prestige", "turnin", "points" -> "turnin";
                case "shop" -> "shop";
                default -> "main";
            };
            case "progression" -> switch (t) {
                case "boosts" -> "boosts";
                case "coins" -> "coins";
                default -> "main";
            };
            default -> homePage(screen);
        };
    }

    private static JsonObject parse(String argsJson) {
        try {
            if (argsJson == null || argsJson.isBlank()) {
                return new JsonObject();
            }
            JsonElement el = JsonParser.parseString(argsJson);
            if (el != null && el.isJsonObject()) {
                return el.getAsJsonObject();
            }
            return new JsonObject();
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra bad argsJson: {}", AdaptiveDifficultyMod.MOD_ID, argsJson);
            return new JsonObject();
        }
    }

    private static String stringArg(JsonObject args, String key) {
        if (args == null || key == null || !args.has(key)) {
            return "";
        }
        JsonElement el = args.get(key);
        if (el == null || el.isJsonNull()) {
            return "";
        }
        try {
            if (el.isJsonPrimitive()) {
                String s = el.getAsString();
                return s == null ? "" : s.trim();
            }
            return el.toString().trim();
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String safeName(ServerPlayer player) {
        if (player == null) {
            return "?";
        }
        try {
            return player.m_7755_().getString();
        } catch (Throwable ignored) {
            return "?";
        }
    }

    record DoParts(String action, String arg) {}

    record Qualified(String screen, String action) {}
}
