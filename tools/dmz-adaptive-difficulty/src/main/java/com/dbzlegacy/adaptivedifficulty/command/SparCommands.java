package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSystem;
import com.dbzlegacy.adaptivedifficulty.sparring.SparStore;
import com.dbzlegacy.adaptivedifficulty.sparring.SparringSystem;
import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
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
                .then(Commands.m_82127_("dojo")
                        .executes(ctx -> dojoInfo(ctx.getSource()))
                        .then(Commands.m_82127_("top")
                                .executes(ctx -> dojoTop(ctx.getSource(), "rp"))
                                .then(Commands.m_82129_("category", StringArgumentType.word())
                                        .executes(ctx -> dojoTop(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "category")))))
                        .then(Commands.m_82127_("accept").executes(ctx -> dojoAccept(ctx.getSource())))
                        .then(Commands.m_82127_("decline").executes(ctx -> dojoDecline(ctx.getSource())))
                        .then(Commands.m_82127_("hof").executes(ctx -> dojoHof(ctx.getSource())))
                        .then(Commands.m_82127_("members").executes(ctx -> dojoMembers(ctx.getSource())))
                        .then(Commands.m_82127_("name")
                                .then(Commands.m_82129_("display", StringArgumentType.greedyString())
                                        .executes(ctx -> dojoName(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "display")))))
                        .then(Commands.m_82127_("banner")
                                .then(Commands.m_82129_("material", StringArgumentType.word())
                                        .executes(ctx -> dojoBanner(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "material")))))
                        .then(Commands.m_82127_("challenge")
                                .then(Commands.m_82129_("player", StringArgumentType.word())
                                        .executes(ctx -> dojoChallenge(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"))))))
                .then(Commands.m_82127_("do")
                        // Full GUI surface via SparGuiApi.handleDo — greedy rest for uuid: args.
                        .then(Commands.m_82129_("action", StringArgumentType.word())
                                .executes(ctx -> guiDo(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "action"),
                                        "",
                                        "main"))
                                .then(Commands.m_82129_("rest", StringArgumentType.greedyString())
                                        .executes(ctx -> guiDoRest(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "action"),
                                                StringArgumentType.getString(ctx, "rest"))))))
                .then(Commands.m_82127_("mentor")
                        .executes(ctx -> mentorStatus(ctx.getSource()))
                        .then(Commands.m_82127_("accept").executes(ctx -> mentorAccept(ctx.getSource())))
                        .then(Commands.m_82127_("decline").executes(ctx -> mentorDecline(ctx.getSource())))
                        .then(Commands.m_82127_("deny").executes(ctx -> mentorDecline(ctx.getSource())))
                        .then(Commands.m_82127_("leave").executes(ctx -> mentorRemove(ctx.getSource())))
                        .then(Commands.m_82127_("remove").executes(ctx -> mentorRemove(ctx.getSource())))
                        .then(Commands.m_82127_("clear").executes(ctx -> mentorRemove(ctx.getSource())))
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> mentorInvite(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("apprentice")
                        .executes(ctx -> mentorStatus(ctx.getSource()))
                        .then(Commands.m_82127_("remove")
                                .executes(ctx -> apprenticeRemove(ctx.getSource(), null))
                                .then(Commands.m_82129_("player", StringArgumentType.word())
                                        .executes(ctx -> apprenticeRemove(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player")))))
                        .then(Commands.m_82127_("release")
                                .executes(ctx -> apprenticeRemove(ctx.getSource(), null))
                                .then(Commands.m_82129_("player", StringArgumentType.word())
                                        .executes(ctx -> apprenticeRemove(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player")))))
                        .then(Commands.m_82127_("clear")
                                .executes(ctx -> apprenticeRemove(ctx.getSource(), null))
                                .then(Commands.m_82129_("player", StringArgumentType.word())
                                        .executes(ctx -> apprenticeRemove(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player")))))
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> apprenticeInvite(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("admin")
                        .requires(SparCommands::staff)
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
                        .requires(SparCommands::staff)
                        .executes(ctx -> save(ctx.getSource())));

        event.getDispatcher().register(root);
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /spar", AdaptiveDifficultyMod.MOD_ID);
    }

    /** Op level 2 or configured admin permission (Mohist Bukkit node). */
    private static boolean staff(CommandSourceStack src) {
        return StaffAccess.isStaffSource(src);
    }

    private static int gui(CommandSourceStack source, String page) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        String p = page == null || page.isBlank() ? "main" : page;
        if (p.startsWith("top_")) {
            p = "top_" + p.substring(4);
        }
        com.dbzlegacy.adaptivedifficulty.gui.SparMenu.open(player, p);
        return 1;
    }

    /** Parse {@code /spar do <action> [arg…] [returnPage]} for CMI/GUI clicks. */
    private static int guiDoRest(CommandSourceStack source, String action, String rest) {
        String a = action == null ? "" : action.trim();
        String r = rest == null ? "" : rest.trim();
        if ("page".equalsIgnoreCase(a) || "refresh".equalsIgnoreCase(a)) {
            return guiDo(source, a, r, r.isBlank() ? "main" : r);
        }
        int sp = r.lastIndexOf(' ');
        if (sp <= 0) {
            return guiDo(source, a, r, "main");
        }
        return guiDo(source, a, r.substring(0, sp).trim(), r.substring(sp + 1).trim());
    }

    private static int guiDo(CommandSourceStack source, String action, String arg, String page) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            return 0;
        }
        String act = action == null ? "" : action;
        String a = arg == null ? "" : arg;
        String reopen = page == null || page.isBlank() ? "main" : page;
        if ("page".equalsIgnoreCase(act) || "refresh".equalsIgnoreCase(act)) {
            reopen = a.isBlank() ? "main" : a;
        } else if (!"admin".equalsIgnoreCase(act) && !enabled(player)) {
            return 0;
        }
        String msg = SparGuiApi.handleDo(player, act, a, reopen);
        if (msg != null && !msg.isBlank()) {
            DmzRewards.msg(player, msg);
        }
        com.dbzlegacy.adaptivedifficulty.gui.SparMenu.open(player, reopen);
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

    private static int dojoTop(CommandSourceStack source, String category) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        for (String line : SparringSystem.dojoTopLines(category, 10)) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int dojoInfo(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        for (String line : SparringSystem.dojoInfoLines(player)) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int dojoChallenge(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        ServerPlayer target = RivalSystem.findOnline(source.m_81377_(), name);
        DmzRewards.msg(player, SparringSystem.dojoChallenge(player, target));
        return 1;
    }

    private static int dojoAccept(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.dojoAcceptWar(player));
        return 1;
    }

    private static int dojoDecline(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.dojoDeclineWar(player));
        return 1;
    }

    private static int dojoHof(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        for (String line : SparringSystem.dojoHallOfFameLines()) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int dojoMembers(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        for (String line : SparringSystem.dojoMemberLines(player)) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int dojoName(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.dojoSetName(player, name));
        return 1;
    }

    private static int dojoBanner(CommandSourceStack source, String material) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.dojoSetBanner(player, material));
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

    private static int apprenticeRemove(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, SparringSystem.removeApprentice(player, name));
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
                        + "§e/spar admin save §7— save sparring data to disk\n"
                        + "§e/spar admin status §7— enabled + path\n"
                        + "§7Clear mentor cooldown\n"
                        + "§8/spar admin mentor resetcd [player]"
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
