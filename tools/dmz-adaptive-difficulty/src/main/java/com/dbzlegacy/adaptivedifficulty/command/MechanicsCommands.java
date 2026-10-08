package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsMenu;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import java.util.Locale;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** {@code /lm} — Legacy Mechanics hub (player menus + staff admin). */
public final class MechanicsCommands {
    private MechanicsCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new MechanicsCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(build());
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /lm", AdaptiveDifficultyMod.MOD_ID);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.m_82127_("lm")
                .requires(CommandAccess::isPlayerSlashUser);
        return root.executes(ctx -> open(ctx.getSource(), "main"))
                .then(Commands.m_82127_("gui").executes(ctx -> open(ctx.getSource(), "main")))
                .then(Commands.m_82127_("help").executes(ctx -> playerHelp(ctx.getSource())))
                .then(Commands.m_82127_("open")
                        .then(LmCommandSuggestions.word("system", LmCommandSuggestions.LM_OPEN_SYSTEMS)
                                .executes(ctx -> openSystem(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "system")))))
                .then(Commands.m_82127_("page")
                        .then(Commands.m_82129_("page", StringArgumentType.word())
                                .executes(ctx -> open(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "page")))))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> open(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page")))))
                        .then(Commands.m_82127_("open")
                                .then(LmCommandSuggestions.word("system", LmCommandSuggestions.LM_OPEN_SYSTEMS)
                                        .executes(ctx -> openSystem(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "system")))))
                        .then(CommandAccess.staffLiteral("syslog")
                                .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.SYSLOG_MODES)
                                        .executes(ctx -> {
                                            ServerPlayer p = playerOrNull(ctx.getSource());
                                            if (p == null) {
                                                return 0;
                                            }
                                            String msg = MechanicsGuiApi.handleDo(
                                                    p, "syslog",
                                                    StringArgumentType.getString(ctx, "mode"),
                                                    "logs");
                                            p.m_213846_(Component.m_237113_(msg));
                                            return open(ctx.getSource(), "logs");
                                        }))))
                .then(fusionResetLiteral())
                .then(CommandAccess.staffLiteral("admin")
                        .executes(ctx -> adminHelp(ctx.getSource()))
                        .then(Commands.m_82127_("help").executes(ctx -> adminHelp(ctx.getSource())))
                        .then(Commands.m_82127_("reload").executes(ctx -> adminReload(ctx.getSource())))
                        .then(migrateCnpcLiteral())
                        .then(adminClearRoot())
                        .then(characterCooldownAdmin())
                        .then(Commands.m_82127_("syslog")
                                .executes(ctx -> adminSyslog(ctx.getSource(), "status"))
                                .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.SYSLOG_MODES)
                                        .executes(ctx -> adminSyslog(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mode")))))
                        .then(Commands.m_82127_("open")
                                .executes(ctx -> adminOpen(ctx.getSource(), "hub"))
                                .then(LmCommandSuggestions.word("system", LmCommandSuggestions.LM_OPEN_SYSTEMS)
                                        .executes(ctx -> adminOpen(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "system")))))
                        .then(adminInspectRoot())
                        .then(Commands.m_82127_("testgui")
                                .executes(ctx -> adminTestGui(ctx.getSource()))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> migrateCnpcLiteral() {
        return Commands.m_82127_("migrate-cnpc")
                .executes(ctx -> adminMigrateCnpc(ctx.getSource(), false))
                .then(Commands.m_82127_("force")
                        .executes(ctx -> adminMigrateCnpc(ctx.getSource(), true)));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminClearRoot() {
        return Commands.m_82127_("clear")
                .then(LmCommandSuggestions.word("player", LmCommandSuggestions.CLEAR_TARGETS)
                        .executes(ctx -> adminClear(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"),
                                "all"))
                        .then(LmCommandSuggestions.word("scope", LmCommandSuggestions.LM_CLEAR_SCOPES)
                                .executes(ctx -> adminClear(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "scope")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> characterCooldownClearTree() {
        return Commands.m_82127_("clear")
                .executes(ctx -> {
                    LmCommandFeedback.tell(ctx.getSource(),
                            LmCommandMessages.tryCommand(
                                    "/lm admin character cooldown clear <player> [race|class|reskin|all]"));
                    return 0;
                })
                .then(LmCommandSuggestions.playerWord("player")
                        .executes(ctx -> adminCharacterCooldownClear(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"),
                                "all"))
                        .then(LmCommandSuggestions.word(
                                        "kind", LmCommandSuggestions.LM_CHARACTER_COOLDOWN_KINDS)
                                .executes(ctx -> adminCharacterCooldownClear(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "kind")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> fusionResetLiteral() {
        return CommandAccess.staffLiteral("fusionreset")
                .executes(ctx -> {
                    LmCommandFeedback.tell(ctx.getSource(),
                            LmCommandMessages.tryCommand("/lm fusionreset <player>"));
                    return 0;
                })
                .then(LmCommandSuggestions.playerWord("player")
                        .executes(ctx -> fusionReset(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> characterCooldownAdmin() {
        return Commands.m_82127_("character")
                .then(Commands.m_82127_("cooldown")
                        .then(characterCooldownClearTree()));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adminInspectRoot() {
        return Commands.m_82127_("inspect")
                .executes(ctx -> adminInspect(ctx.getSource(), null, "hub"))
                .then(Commands.m_82127_("clear")
                        .executes(ctx -> adminInspect(ctx.getSource(), "clear", "hub")))
                .then(LmCommandSuggestions.playerWord("player")
                        .executes(ctx -> adminInspect(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"),
                                "hub"))
                        .then(LmCommandSuggestions.word(
                                        "system", LmCommandSuggestions.LM_INSPECT_SYSTEMS)
                                .executes(ctx -> adminInspect(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "system")))));
    }

    private static int open(CommandSourceStack source, String page) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            return 0;
        }
        MechanicsMenu.open(player, page);
        return 1;
    }

    private static int openSystem(CommandSourceStack source, String system) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            return 0;
        }
        return openSystemMenu(player, system) ? 1 : 0;
    }

    private static int adminOpen(CommandSourceStack source, String system) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        return openSystem(source, system);
    }

    /** Opens the standard LM inventory/chat menu for a system (shared with staff CNPC test GUI). */
    public static boolean openSystemMenu(ServerPlayer player, String system) {
        if (player == null) {
            return false;
        }
        String s = system == null ? "" : system.toLowerCase();
        // Prefer Bukkit companion when present; Forge opens chat/menu fallbacks.
        return switch (s) {
            case "difficulty", "diff" -> {
                com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu.open(player, "main");
                yield true;
            }
            case "rival" -> {
                com.dbzlegacy.adaptivedifficulty.gui.RivalMenu.open(player, "main");
                yield true;
            }
            case "spar", "sparring" -> {
                com.dbzlegacy.adaptivedifficulty.gui.SparMenu.open(player, "main");
                yield true;
            }
            case "progression", "prog" -> {
                if (!StaffAccess.isStaff(player)) {
                    player.m_213846_(Component.m_237113_(LmCommandMessages.STAFF_ONLY));
                    yield false;
                }
                com.dbzlegacy.adaptivedifficulty.gui.ProgressionMenu.open(player, "main");
                yield true;
            }
            case "prestige" -> {
                com.dbzlegacy.adaptivedifficulty.gui.PrestigeMenu.open(player, "main");
                yield true;
            }
            case "character", "characterservices", "charservices", "char" -> {
                com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesMenu.open(player, "main");
                yield true;
            }
            case "android_remove", "androidremove", "removeandroid" -> {
                com.dbzlegacy.adaptivedifficulty.gui.ProgressionMenu.open(player, "android_remove");
                yield true;
            }
            case "skills", "skill" -> {
                if (!StaffAccess.isStaff(player)) {
                    player.m_213846_(Component.m_237113_(LmCommandMessages.STAFF_ONLY));
                    yield false;
                }
                com.dbzlegacy.adaptivedifficulty.gui.SkillsMenu.open(player, "core");
                yield true;
            }
            case "skillcheck" -> {
                if (!StaffAccess.hasSkillCheck(player)) {
                    player.m_213846_(Component.m_237113_(LmCommandMessages.SKILLCHECK_DONATOR));
                    yield false;
                }
                com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService.open(player, "core");
                yield true;
            }
            case "hub", "main", "help", "lm", "legacymechanics" -> {
                MechanicsMenu.open(player, "main");
                yield true;
            }
            default -> {
                player.m_213846_(Component.m_237113_(LmCommandMessages.unknownOpenTarget(
                        s,
                        "difficulty, rival, spar, prestige, skillcheck, character, progression, skills")));
                yield false;
            }
        };
    }

    private static int adminTestGui(CommandSourceStack source) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            source.m_81352_(Component.m_237113_(LmCommandMessages.NEED_IN_GAME
                    + " §8Then: §f/lm admin testgui"));
            return 0;
        }
        return com.dbzlegacy.adaptivedifficulty.gui.CnpcStaffTestGui.open(player) ? 1 : 0;
    }

    private static int playerHelp(CommandSourceStack source) {
        LmCommandFeedback.tellLines(source, LmCommandHelp.lmPlayer());
        return 1;
    }

    private static int adminHelp(CommandSourceStack source) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        LmCommandFeedback.tellLines(source, LmCommandHelp.lmAdmin());
        return 1;
    }

    private static int adminInspect(CommandSourceStack source, String playerName, String system) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        ServerPlayer admin = playerOrNull(source);
        if (admin == null) {
            source.m_81352_(Component.m_237113_(LmCommandMessages.PLAYERS_ONLY_INSPECT));
            return 0;
        }
        if (playerName == null || playerName.isBlank()
                || "clear".equalsIgnoreCase(playerName)
                || "self".equalsIgnoreCase(playerName)
                || "me".equalsIgnoreCase(playerName)) {
            return com.dbzlegacy.adaptivedifficulty.gui.ForgeInspectGui.clear(admin) ? 1 : 0;
        }
        ServerPlayer subject = admin.m_20194_() == null
                ? null
                : admin.m_20194_().m_6846_().m_11255_(playerName.trim());
        if (subject == null && admin.m_20194_() != null) {
            String want = playerName.trim().toLowerCase();
            for (ServerPlayer p : admin.m_20194_().m_6846_().m_11314_()) {
                if (p.m_7755_().getString().toLowerCase().equals(want)) {
                    subject = p;
                    break;
                }
            }
        }
        if (subject == null) {
            admin.m_213846_(Component.m_237113_(LmCommandMessages.playerOffline(playerName)));
            return 0;
        }
        String sys = system == null || system.isBlank() ? "hub" : system;
        return com.dbzlegacy.adaptivedifficulty.gui.ForgeInspectGui.open(admin, subject, sys) ? 1 : 0;
    }

    private static int adminMigrateCnpc(CommandSourceStack source, boolean force) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        String msg = com.dbzlegacy.adaptivedifficulty.data.CnpcDataMigrator.forceMigrateWorld(
                source.m_81377_(), force);
        LmCommandFeedback.tellLines(source, msg);
        return 1;
    }

    private static int adminClear(CommandSourceStack source, String player, String scope) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        String msg = com.dbzlegacy.adaptivedifficulty.data.PlayerDataClear.clear(
                source.m_81377_(), player, scope);
        LmCommandFeedback.tellLines(source, msg);
        return msg != null && msg.startsWith("§c") ? 0 : 1;
    }

    private static int fusionReset(CommandSourceStack source, String player) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        ServerPlayer target = onlinePlayer(source.m_81377_(), player);
        if (target == null) {
            LmCommandFeedback.tell(source, LmCommandMessages.playerOffline(player));
            return 0;
        }
        String msg = FusionCooldownReset.reset(target);
        LmCommandFeedback.tell(source, msg);
        if (source.m_230896_() != target && msg != null && !msg.startsWith("§c")) {
            target.m_213846_(Component.m_237113_(
                    "§eStaff cleared your fusion cooldown."));
        }
        return msg != null && msg.startsWith("§c") ? 0 : 1;
    }

    private static ServerPlayer onlinePlayer(MinecraftServer server, String raw) {
        if (server == null || raw == null || raw.isBlank()) {
            return null;
        }
        String name = raw.trim();
        ServerPlayer exact = server.m_6846_().m_11255_(name);
        if (exact != null) {
            return exact;
        }
        String want = name.toLowerCase(Locale.ROOT);
        for (ServerPlayer candidate : server.m_6846_().m_11314_()) {
            String online = candidate.m_6302_();
            if (online != null && online.toLowerCase(Locale.ROOT).equals(want)) {
                return candidate;
            }
        }
        return null;
    }

    private static int adminCharacterCooldownClear(CommandSourceStack source, String player, String kind) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        String msg = com.dbzlegacy.adaptivedifficulty.data.PlayerDataClear.clearCharacterCooldowns(
                source.m_81377_(), player, kind);
        LmCommandFeedback.tellLines(source, msg);
        return msg != null && msg.startsWith("§c") ? 0 : 1;
    }

    private static int adminReload(CommandSourceStack source) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        boolean ok = DifficultyConfig.reload();
        LmCommandFeedback.tell(source,
                ok ? LmCommandMessages.reloaded("Legacy Mechanics config")
                        : "§cConfig reload failed—check the server log. Your live settings are unchanged.");
        return ok ? 1 : 0;
    }

    private static int adminSyslog(CommandSourceStack source, String mode) {
        if (StaffAccess.denyUnlessStaff(source) == 0) {
            return 0;
        }
        String m = mode == null ? "status" : mode.toLowerCase();
        String msg = switch (m) {
            case "on", "true", "enable" -> {
                SystemTelemetry.setEnabled(true);
                yield "§aEvent log ON";
            }
            case "off", "false", "disable" -> {
                SystemTelemetry.setEnabled(false);
                yield "§eEvent log OFF";
            }
            case "flush" -> {
                SystemTelemetry.flushAndClose();
                yield "§aLogs flushed.";
            }
            default -> "§7" + SystemTelemetry.statusLine();
        };
        LmCommandFeedback.tellLines(source, msg);
        return 1;
    }

    private static ServerPlayer playerOrNull(CommandSourceStack source) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            source.m_81352_(Component.m_237113_(LmCommandMessages.PLAYERS_ONLY));
        }
        return player;
    }
}
