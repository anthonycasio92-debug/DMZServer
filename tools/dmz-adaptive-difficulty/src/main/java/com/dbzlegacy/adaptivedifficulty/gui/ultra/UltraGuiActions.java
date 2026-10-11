package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcMenuFeedback;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.server.level.ServerPlayer;

/** Dispatches KaiPath GuiActionPacket presses to existing server menu APIs. */
public final class UltraGuiActions {
    private UltraGuiActions() {}

    public static void dispatch(ServerPlayer player, String screenId, String actionId, String argsJson) {
        if (player == null || actionId == null || actionId.isBlank()) {
            return;
        }
        String screen = screenId == null ? "hub" : screenId.trim().toLowerCase();
        String action = actionId.trim();
        JsonObject args = parse(argsJson);

        if (action.startsWith("tab:")) {
            String tab = action.substring(4);
            UltraGui.open(player, screen, tabPage(screen, tab));
            return;
        }
        if ("nav:hub".equals(action)) {
            UltraGui.openHub(player, "main");
            return;
        }
        if ("nav:home".equals(action)) {
            UltraGui.open(player, screen, homePage(screen));
            return;
        }
        if ("nav:refresh".equals(action)) {
            String page = args.has("page") ? args.get("page").getAsString() : homePage(screen);
            UltraGui.open(player, screen, page);
            return;
        }
        if (action.startsWith("open:")) {
            UltraGui.open(player, action.substring(5), "main");
            return;
        }

        DoParts parts = parseDo(action);
        // Prefer arg encoded in the action id (do:action:arg…); fall back to argsJson.
        if (!parts.arg.isBlank()) {
            args.addProperty("arg", parts.arg);
        } else if (!args.has("arg")) {
            args.addProperty("arg", "");
        }

        String msg = runDo(player, screen, parts.action, args);
        if (msg != null && !msg.isBlank()) {
            DmzRewards.msg(player, CnpcMenuFeedback.noticeBody(msg));
        }
        // saga_confirm opens its page inside characterDo.
        if ("saga_confirm".equals(parts.action)) {
            return;
        }
        String reopen = reopenPage(screen, parts, args);
        UltraGui.open(player, screen, reopen);
    }

    private static String runDo(ServerPlayer player, String screen, String act, JsonObject args) {
        String arg = args.has("arg") ? args.get("arg").getAsString() : "";
        String page = args.has("page") ? args.get("page").getAsString() : homePage(screen);
        return switch (screen) {
            case "difficulty" -> DifficultyActions.handleArgNoReopen(player, act, arg, page).message();
            case "rival" -> RivalGuiApi.handleDo(player, act, arg, page);
            case "spar", "sparring" -> SparGuiApi.handleDo(player, act, arg, page);
            case "prestige" -> ProgressionGuiApi.handlePrestigeDo(player, act, arg, page);
            case "character", "saga" -> characterDo(player, act, arg, page);
            case "progression" -> ProgressionGuiApi.handleDo(player, act, arg, page);
            case "config" -> UltraConfigScreens.handleAction(player, act, arg, page);
            case "hub", "admin" -> MechanicsGuiApi.handleDo(player, act, arg, page);
            case "team" -> DifficultyTeamGuiApi.handleDo(player, act, arg, page);
            default -> "That menu action is not available.";
        };
    }

    private static String characterDo(ServerPlayer player, String action, String arg, String page) {
        if ("android_remove".equals(action) || "android".equals(action)) {
            return ProgressionGuiApi.handleDo(player, action, arg, page);
        }
        if ("saga_confirm".equals(action)) {
            String id = arg == null ? "" : arg.trim();
            if (id.isBlank()) {
                return UltraStyle.dangerLine("Choose a saga first.");
            }
            UltraCharacterScreens.open(player, "saga_confirm:" + id);
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
        return CharacterServicesGuiApi.handleDo(player, action, arg, page);
    }

    /**
     * Parses {@code do:action}, {@code do:action:arg}, or {@code do:action:arg:more}.
     * Everything after the first colon following the action name is the arg
     * (so {@code do:boost:1.25:30} → action {@code boost}, arg {@code 1.25:30}).
     */
    static DoParts parseDo(String action) {
        String raw = stripDo(action);
        if (raw.isBlank()) {
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

    private static String reopenPage(String screen, DoParts parts, JsonObject args) {
        if ("config".equals(screen) && "open_module".equals(parts.action) && !parts.arg.isBlank()) {
            return "module:" + parts.arg;
        }
        if (("character".equals(screen) || "saga".equals(screen))
                && ("saga_reset".equals(parts.action) || "saga".equals(parts.action))) {
            return "reset";
        }
        if (args.has("page")) {
            return args.get("page").getAsString();
        }
        // Keep player on the tab they acted from when possible.
        return switch (screen) {
            case "difficulty" -> switch (parts.action) {
                case "activate", "lower_tier", "purchase_tier", "buy", "toggle_personal" -> "tiers";
                case "summon_end_dragon", "end_dragon", "summon_dragon" -> "summons";
                default -> homePage(screen);
            };
            case "prestige" -> switch (parts.action) {
                case "confirm", "turnin", "turn_in", "redeem" -> "turnin";
                case "skill", "buy_skill", "skillup" -> "shop";
                default -> homePage(screen);
            };
            case "progression" -> switch (parts.action) {
                case "boost" -> "boosts";
                case "toggle_staff_free_coins", "staff_free_coins" -> "coins";
                default -> homePage(screen);
            };
            case "spar", "sparring" -> switch (parts.action) {
                case "dojo_accept", "dojo_war_accept", "dojo_war_cancel", "dojo_war_revoke",
                        "dojo_cancel", "dojo_challenge" -> "dojo_war";
                case "mentor", "mentor_accept", "mentor_decline", "mentor_tpmsg", "mentor_tp_msg" -> "dojo";
                default -> homePage(screen);
            };
            case "rival" -> switch (parts.action) {
                case "challenge", "challenge_accept", "challenge_decline", "challenge_cancel",
                        "challenge_send", "spectate", "spectate_stop" -> "challenge";
                case "tpmsg", "tp_msg", "instinct", "declare", "accept", "decline", "silent",
                        "remove", "accept_replace" -> "actions";
                default -> homePage(screen);
            };
            case "character" -> switch (parts.action) {
                case "android_remove", "android" -> "android_remove";
                default -> homePage(screen);
            };
            default -> homePage(screen);
        };
    }

    private static String homePage(String screen) {
        return switch (screen) {
            case "rival" -> "home";
            case "config" -> "list";
            default -> "main";
        };
    }

    private static String tabPage(String screen, String tab) {
        return switch (screen) {
            case "character" -> switch (tab) {
                case "saga", "reset" -> "reset";
                case "android", "android_remove" -> "android_remove";
                default -> "main";
            };
            case "difficulty" -> switch (tab) {
                case "tiers" -> "tiers";
                case "summons" -> "summons";
                default -> "main";
            };
            case "rival" -> switch (tab) {
                case "actions" -> "actions";
                case "challenge" -> "challenge";
                default -> "home";
            };
            case "spar", "sparring" -> switch (tab) {
                case "dojo" -> "dojo";
                case "wars", "dojo_war" -> "dojo_war";
                default -> "main";
            };
            case "prestige" -> switch (tab) {
                case "prestige", "turnin", "points" -> "turnin";
                case "shop" -> "shop";
                default -> "main";
            };
            case "progression" -> switch (tab) {
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
            return JsonParser.parseString(argsJson).getAsJsonObject();
        } catch (Throwable ignored) {
            return new JsonObject();
        }
    }

    record DoParts(String action, String arg) {}
}
