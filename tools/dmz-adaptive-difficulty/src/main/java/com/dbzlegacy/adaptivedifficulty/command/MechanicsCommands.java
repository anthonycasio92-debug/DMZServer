package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsMenu;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** {@code /legacymechanics} / {@code /lm} hub. */
public final class MechanicsCommands {
    private MechanicsCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new MechanicsCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = build("legacymechanics");
        event.getDispatcher().register(root);
        event.getDispatcher().register(build("lm"));
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /legacymechanics /lm", AdaptiveDifficultyMod.MOD_ID);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.m_82127_(name)
                .executes(ctx -> open(ctx.getSource(), "main"))
                .then(Commands.m_82127_("gui").executes(ctx -> open(ctx.getSource(), "main")))
                .then(Commands.m_82127_("help").executes(ctx -> open(ctx.getSource(), "main")))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> open(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page")))))
                        .then(Commands.m_82127_("open")
                                .then(Commands.m_82129_("system", StringArgumentType.word())
                                        .executes(ctx -> openSystem(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "system")))))
                        .then(Commands.m_82127_("syslog")
                                .then(Commands.m_82129_("mode", StringArgumentType.word())
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
                .then(Commands.m_82127_("admin")
                        .requires(src -> {
                            try {
                                ServerPlayer p = src.m_81375_();
                                return StaffAccess.isStaff(p) || src.m_6761_(2);
                            } catch (Exception e) {
                                return src.m_6761_(2);
                            }
                        })
                        .executes(ctx -> adminHelp(ctx.getSource()))
                        .then(Commands.m_82127_("help").executes(ctx -> adminHelp(ctx.getSource())))
                        .then(Commands.m_82127_("reload").executes(ctx -> adminReload(ctx.getSource())))
                        .then(Commands.m_82127_("migrate-cnpc")
                                .executes(ctx -> adminMigrateCnpc(ctx.getSource(), false))
                                .then(Commands.m_82127_("force")
                                        .executes(ctx -> adminMigrateCnpc(ctx.getSource(), true))))
                        .then(Commands.m_82127_("syslog")
                                .executes(ctx -> adminSyslog(ctx.getSource(), "status"))
                                .then(Commands.m_82129_("mode", StringArgumentType.word())
                                        .executes(ctx -> adminSyslog(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "mode")))))
                        .then(Commands.m_82127_("open")
                                .then(Commands.m_82129_("system", StringArgumentType.word())
                                        .executes(ctx -> openSystem(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "system")))))
                        .then(Commands.m_82127_("inspect")
                                .executes(ctx -> adminInspect(ctx.getSource(), null, "hub"))
                                .then(Commands.m_82129_("player", StringArgumentType.string())
                                        .executes(ctx -> adminInspect(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                "hub"))
                                        .then(Commands.m_82129_("system", StringArgumentType.word())
                                                .executes(ctx -> adminInspect(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "system")))))));
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
        String s = system == null ? "" : system.toLowerCase();
        // Prefer Bukkit companion when present; Forge opens chat/menu fallbacks.
        return switch (s) {
            case "difficulty", "diff" -> {
                com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu.open(player, "main");
                yield 1;
            }
            case "rival" -> {
                com.dbzlegacy.adaptivedifficulty.gui.RivalMenu.open(player, "main");
                yield 1;
            }
            case "spar", "sparring" -> {
                com.dbzlegacy.adaptivedifficulty.gui.SparMenu.open(player, "main");
                yield 1;
            }
            case "progression", "prog" -> {
                if (!StaffAccess.isStaff(player)) {
                    player.m_213846_(Component.m_237113_("§cStaff only."));
                    yield 0;
                }
                com.dbzlegacy.adaptivedifficulty.gui.ProgressionMenu.open(player, "main");
                yield 1;
            }
            case "prestige" -> {
                if (!StaffAccess.isStaff(player)) {
                    player.m_213846_(Component.m_237113_("§cStaff only."));
                    yield 0;
                }
                com.dbzlegacy.adaptivedifficulty.gui.PrestigeMenu.open(player, "main");
                yield 1;
            }
            case "skills", "skill" -> {
                if (!StaffAccess.isStaff(player)) {
                    player.m_213846_(Component.m_237113_("§cStaff only."));
                    yield 0;
                }
                com.dbzlegacy.adaptivedifficulty.gui.SkillsMenu.open(player, "core");
                yield 1;
            }
            case "skillcheck" -> {
                com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService.open(player, "core");
                yield 1;
            }
            default -> {
                player.m_213846_(Component.m_237113_(
                        "§cUnknown: " + s + " §8(difficulty|rival|spar|progression|prestige|skills)"));
                yield 0;
            }
        };
    }

    private static int adminHelp(CommandSourceStack source) {
        source.m_288197_(() -> Component.m_237113_(
                "§6§l/lm admin\n"
                        + "§e/lm admin help §7— this list\n"
                        + "§e/lm admin reload §7— reload config\n"
                        + "§e/lm admin migrate-cnpc §7— import CNPC Rival/Spar into LM (once)\n"
                        + "§e/lm admin migrate-cnpc force §7— wipe LM stores + re-import\n"
                        + "§e/lm admin syslog on|off|status|flush\n"
                        + "§e/lm admin open <difficulty|rival|spar|progression|prestige|skills|hub>\n"
                        + "§e/lm admin inspect <player> [hub|difficulty|rival|spar|skillcheck|…]\n"
                        + "§e/lm admin inspect clear §7— stop inspecting\n"
                        + "§8Also: /difficulty admin gui|inspect <player>"
        ), false);
        return 1;
    }

    private static int adminInspect(CommandSourceStack source, String playerName, String system) {
        ServerPlayer admin = playerOrNull(source);
        if (admin == null) {
            source.m_81352_(Component.m_237113_("Players only (open inspect from in-game)."));
            return 0;
        }
        if (!StaffAccess.isStaff(admin)) {
            admin.m_213846_(Component.m_237113_("§cStaff only."));
            return 0;
        }
        if (playerName == null || playerName.isBlank()
                || "clear".equalsIgnoreCase(playerName)
                || "self".equalsIgnoreCase(playerName)
                || "me".equalsIgnoreCase(playerName)) {
            if (com.dbzlegacy.adaptivedifficulty.gui.CmiGuiBridge.clearLmInspect(admin.m_20148_())) {
                return 1;
            }
            com.dbzlegacy.adaptivedifficulty.gui.MechanicsMenu.open(admin, "main");
            admin.m_213846_(Component.m_237113_("§7Inspect clear requested — reopen §f/lm §7if needed."));
            return 1;
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
            admin.m_213846_(Component.m_237113_("§cPlayer not online: §f" + playerName));
            return 0;
        }
        String sys = system == null || system.isBlank() ? "hub" : system;
        if (com.dbzlegacy.adaptivedifficulty.gui.CmiGuiBridge.openLmInspect(admin, subject, sys, "main")) {
            return 1;
        }
        admin.m_213846_(Component.m_237113_(
                "§cCould not open inspect GUI. Is LegacyMechanicsGUI loaded?"));
        return 0;
    }

    private static int adminMigrateCnpc(CommandSourceStack source, boolean force) {
        String msg = com.dbzlegacy.adaptivedifficulty.data.CnpcDataMigrator.forceMigrateWorld(
                source.m_81377_(), force);
        source.m_288197_(() -> Component.m_237113_(msg), true);
        return 1;
    }

    private static int adminReload(CommandSourceStack source) {
        boolean ok = DifficultyConfig.reload();
        source.m_288197_(() -> Component.m_237113_(
                ok ? "§aLegacyMechanics config reloaded." : "§cConfig reload failed."), true);
        return ok ? 1 : 0;
    }

    private static int adminSyslog(CommandSourceStack source, String mode) {
        String m = mode == null ? "status" : mode.toLowerCase();
        switch (m) {
            case "on", "true", "enable" -> {
                SystemTelemetry.setEnabled(true);
                source.m_288197_(() -> Component.m_237113_("§aSystem telemetry ON"), true);
            }
            case "off", "false", "disable" -> {
                SystemTelemetry.setEnabled(false);
                source.m_288197_(() -> Component.m_237113_("§eSystem telemetry OFF"), true);
            }
            case "flush" -> {
                SystemTelemetry.flushAndClose();
                source.m_288197_(() -> Component.m_237113_("§aSyslog flushed."), true);
            }
            default -> source.m_288197_(() -> Component.m_237113_("§7" + SystemTelemetry.statusLine()), false);
        }
        return 1;
    }

    private static ServerPlayer playerOrNull(CommandSourceStack source) {
        try {
            return source.m_81375_();
        } catch (Exception e) {
            source.m_81352_(Component.m_237113_("Players only."));
            return null;
        }
    }
}
