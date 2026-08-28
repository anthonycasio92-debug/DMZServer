package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSystem;
import com.dbzlegacy.adaptivedifficulty.sparring.SparStore;
import com.dbzlegacy.adaptivedifficulty.sparring.SparringSystem;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** {@code /spar} — Sparring Tp System 3.2.11 command surface. */
public final class SparCommands {
    private SparCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new SparCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.m_82127_("spar")
                .executes(ctx -> gui(ctx.getSource(), "main"))
                .then(Commands.m_82127_("gui").executes(ctx -> gui(ctx.getSource(), "main")))
                .then(Commands.m_82127_("help").executes(ctx -> gui(ctx.getSource(), "help")))
                .then(Commands.m_82127_("stats")
                        .executes(ctx -> stats(ctx.getSource(), null))
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> stats(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("end").executes(ctx -> end(ctx.getSource())))
                .then(Commands.m_82127_("top")
                        .executes(ctx -> top(ctx.getSource(), "tp"))
                        .then(Commands.m_82129_("category", StringArgumentType.word())
                                .executes(ctx -> top(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "category")))))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> {
                                            String page = StringArgumentType.getString(ctx, "page");
                                            if (page.startsWith("top_")) {
                                                return gui(ctx.getSource(), "top " + page.substring(4));
                                            }
                                            return gui(ctx.getSource(), page);
                                        })))
                        .then(Commands.m_82127_("end")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> {
                                            end(ctx.getSource());
                                            return gui(ctx.getSource(),
                                                    StringArgumentType.getString(ctx, "page"));
                                        })))
                        .then(Commands.m_82127_("mentor")
                                .then(Commands.m_82127_("accept")
                                        .then(Commands.m_82129_("page", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    mentorAccept(ctx.getSource());
                                                    return gui(ctx.getSource(),
                                                            StringArgumentType.getString(ctx, "page"));
                                                })))
                                .then(Commands.m_82127_("decline")
                                        .then(Commands.m_82129_("page", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    mentorDecline(ctx.getSource());
                                                    return gui(ctx.getSource(),
                                                            StringArgumentType.getString(ctx, "page"));
                                                })))
                                .then(Commands.m_82127_("remove")
                                        .then(Commands.m_82129_("page", StringArgumentType.word())
                                                .executes(ctx -> {
                                                    mentorRemove(ctx.getSource());
                                                    return gui(ctx.getSource(),
                                                            StringArgumentType.getString(ctx, "page"));
                                                })))))
                .then(Commands.m_82127_("mentor")
                        .executes(ctx -> mentorStatus(ctx.getSource()))
                        .then(Commands.m_82127_("accept").executes(ctx -> mentorAccept(ctx.getSource())))
                        .then(Commands.m_82127_("decline").executes(ctx -> mentorDecline(ctx.getSource())))
                        .then(Commands.m_82127_("deny").executes(ctx -> mentorDecline(ctx.getSource())))
                        .then(Commands.m_82127_("remove").executes(ctx -> mentorRemove(ctx.getSource())))
                        .then(Commands.m_82127_("clear").executes(ctx -> mentorRemove(ctx.getSource())))
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> mentorInvite(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("apprentice")
                        .executes(ctx -> mentorStatus(ctx.getSource()))
                        .then(Commands.m_82127_("remove").executes(ctx -> apprenticeRemove(ctx.getSource())))
                        .then(Commands.m_82127_("clear").executes(ctx -> apprenticeRemove(ctx.getSource())))
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> apprenticeInvite(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("admin")
                        .requires(src -> src.m_6761_(2))
                        .executes(ctx -> sparAdminHelp(ctx.getSource()))
                        .then(Commands.m_82127_("help").executes(ctx -> sparAdminHelp(ctx.getSource())))
                        .then(Commands.m_82127_("save").executes(ctx -> save(ctx.getSource())))
                        .then(Commands.m_82127_("status").executes(ctx -> sparAdminStatus(ctx.getSource())))
                        .then(Commands.m_82127_("mentor")
                                .then(Commands.m_82127_("resetcd")
                                        .executes(ctx -> resetCd(ctx.getSource(), null))
                                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                                .executes(ctx -> resetCd(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player")))))))
                .then(Commands.m_82127_("save")
                        .requires(src -> src.m_6761_(2))
                        .executes(ctx -> save(ctx.getSource())));

        event.getDispatcher().register(root);
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /spar", AdaptiveDifficultyMod.MOD_ID);
    }

    private static int gui(CommandSourceStack source, String page) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        com.dbzlegacy.adaptivedifficulty.gui.SparMenu.open(player, page);
        return 1;
    }

    private static int top(CommandSourceStack source, String category) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        for (String line : SparringSystem.topLines(category, 10)) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int help(CommandSourceStack source) {
        return gui(source, "help");
    }

    private static int stats(CommandSourceStack source, String targetName) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        ServerPlayer target = player;
        if (targetName != null && !targetName.isBlank()) {
            target = RivalSystem.findOnline(source.m_81377_(), targetName);
            if (target == null) {
                DmzRewards.msg(player, "§cPlayer not online: " + targetName);
                return 0;
            }
        }
        for (String line : SparringSystem.statsLines(target)) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int end(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.endCommand(player));
        return 1;
    }

    private static int mentorStatus(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.bondStatus(player));
        return 1;
    }

    private static int mentorInvite(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        ServerPlayer target = RivalSystem.findOnline(source.m_81377_(), name);
        DmzRewards.msg(player, SparringSystem.mentorInvite(player, target));
        return 1;
    }

    private static int apprenticeInvite(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        ServerPlayer target = RivalSystem.findOnline(source.m_81377_(), name);
        DmzRewards.msg(player, SparringSystem.apprenticeInvite(player, target));
        return 1;
    }

    private static int mentorAccept(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.mentorAccept(player));
        return 1;
    }

    private static int mentorDecline(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.mentorDecline(player));
        return 1;
    }

    private static int mentorRemove(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.removeMentor(player));
        return 1;
    }

    private static int apprenticeRemove(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.removeApprentice(player));
        return 1;
    }

    private static int resetCd(CommandSourceStack source, String name) {
        ServerPlayer admin = playerOrNull(source);
        ServerPlayer target = name == null ? admin : RivalSystem.findOnline(source.m_81377_(), name);
        if (target == null) {
            source.m_81352_(Component.m_237113_("§cPlayer not online: " + name));
            return 0;
        }
        String msg = SparringSystem.resetMentorCd(admin == null ? target : admin, target);
        source.m_288197_(() -> Component.m_237113_(msg), true);
        return 1;
    }

    private static int save(CommandSourceStack source) {
        SparStore.get().markDirty();
        SparStore.get().save();
        source.m_288197_(() -> Component.m_237113_("§aSpar store saved."), true);
        return 1;
    }

    private static int sparAdminHelp(CommandSourceStack source) {
        source.m_288197_(() -> Component.m_237113_(
                "§6§l/spar admin\n"
                        + "§e/spar admin save §7— write sparring.json\n"
                        + "§e/spar admin status §7— enabled + path\n"
                        + "§e/spar admin mentor resetcd [player] §7— clear mentor cooldown"
        ), false);
        return 1;
    }

    private static int sparAdminStatus(CommandSourceStack source) {
        boolean on = DifficultyConfig.get().enableSparringSystem;
        source.m_288197_(() -> Component.m_237113_(
                "§6Spar admin status\n"
                        + "§7enabled §f" + (on ? "ON" : "OFF") + "\n"
                        + "§7bonds §f" + SparStore.get().bondsByPlayer.size() + "\n"
                        + "§8" + SparStore.path()
        ), false);
        return 1;
    }

    private static boolean enabled(ServerPlayer player) {
        if (!DifficultyConfig.get().enableSparringSystem) {
            DmzRewards.msg(player, "§cSparring system is disabled.");
            return false;
        }
        return true;
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
