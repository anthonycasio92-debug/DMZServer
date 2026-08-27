package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.rival.RivalChallengeManager;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSystem;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** {@code /rival} — Rival System 4.7.10 command surface. */
public final class RivalCommands {
    private RivalCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new RivalCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.m_82127_("rival")
                .executes(ctx -> help(ctx.getSource()))
                .then(Commands.m_82127_("help").executes(ctx -> help(ctx.getSource())))
                .then(Commands.m_82127_("list").executes(ctx -> list(ctx.getSource())))
                .then(Commands.m_82127_("stats").executes(ctx -> stats(ctx.getSource())))
                .then(Commands.m_82127_("top").executes(ctx -> top(ctx.getSource())))
                .then(Commands.m_82127_("declare")
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> declare(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("accept")
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> accept(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("decline")
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> decline(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("remove")
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> remove(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("tpmsg")
                        .executes(ctx -> tpmsg(ctx.getSource(), null))
                        .then(Commands.m_82127_("on").executes(ctx -> tpmsg(ctx.getSource(), true)))
                        .then(Commands.m_82127_("off").executes(ctx -> tpmsg(ctx.getSource(), false))))
                .then(Commands.m_82127_("challenge")
                        .then(Commands.m_82127_("send")
                                .then(Commands.m_82129_("player", StringArgumentType.word())
                                        .executes(ctx -> challengeSend(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "player"),
                                                1))
                                        .then(Commands.m_82129_("minutes", IntegerArgumentType.integer(1, 10))
                                                .executes(ctx -> challengeSend(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        IntegerArgumentType.getInteger(ctx, "minutes"))))))
                        .then(Commands.m_82127_("accept").executes(ctx -> challengeAccept(ctx.getSource())))
                        .then(Commands.m_82127_("decline").executes(ctx -> challengeDecline(ctx.getSource())))
                        .then(Commands.m_82127_("cancel").executes(ctx -> challengeCancel(ctx.getSource()))))
                .then(Commands.m_82127_("refresh")
                        .requires(src -> src.m_6761_(2))
                        .executes(ctx -> refresh(ctx.getSource())))
                .then(Commands.m_82127_("save")
                        .requires(src -> src.m_6761_(2))
                        .executes(ctx -> save(ctx.getSource())))
                .then(Commands.m_82129_("player", StringArgumentType.word())
                        .executes(ctx -> silent(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "player"))));

        event.getDispatcher().register(root);
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /rival", AdaptiveDifficultyMod.MOD_ID);
    }

    private static int help(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            return 0;
        }
        DmzRewards.msg(player, "§6§l/rival §8— Rival System");
        DmzRewards.msg(player, "§e/rival <player> §7silent rival");
        DmzRewards.msg(player, "§e/rival declare|accept|decline|remove <player>");
        DmzRewards.msg(player, "§e/rival list|stats|top|tpmsg [on|off]");
        DmzRewards.msg(player, "§e/rival challenge send <player> [minutes]");
        DmzRewards.msg(player, "§e/rival challenge accept|decline|cancel");
        return 1;
    }

    private static int silent(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        ServerPlayer target = RivalSystem.findOnline(source.m_81377_(), name);
        if (target == null) {
            DmzRewards.msg(player, "§cPlayer not online: " + name);
            return 0;
        }
        DmzRewards.msg(player, RivalSystem.silentRival(player, target));
        return 1;
    }

    private static int declare(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        ServerPlayer target = RivalSystem.findOnline(source.m_81377_(), name);
        if (target == null) {
            DmzRewards.msg(player, "§cPlayer not online: " + name);
            return 0;
        }
        DmzRewards.msg(player, RivalSystem.declare(player, target));
        return 1;
    }

    private static int accept(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, RivalSystem.accept(player, name));
        return 1;
    }

    private static int decline(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, RivalSystem.decline(player, name));
        return 1;
    }

    private static int remove(CommandSourceStack source, String name) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, RivalSystem.remove(player, name));
        return 1;
    }

    private static int list(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        for (String line : RivalSystem.listLines(player)) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int stats(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        for (String line : RivalSystem.statsLines(player)) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int top(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        for (String line : RivalSystem.topLines(10)) {
            DmzRewards.msg(player, line);
        }
        return 1;
    }

    private static int tpmsg(CommandSourceStack source, Boolean on) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, RivalSystem.setTpMsg(player, on));
        return 1;
    }

    private static int challengeSend(CommandSourceStack source, String name, int minutes) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        if (!DifficultyConfig.get().rivalChallenges) {
            DmzRewards.msg(player, "§cRival challenges are disabled.");
            return 0;
        }
        ServerPlayer target = RivalSystem.findOnline(source.m_81377_(), name);
        if (target == null) {
            DmzRewards.msg(player, "§cPlayer not online: " + name);
            return 0;
        }
        DmzRewards.msg(player, RivalChallengeManager.get().sendChallenge(player, target, minutes));
        return 1;
    }

    private static int challengeAccept(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, RivalChallengeManager.get().acceptChallenge(player));
        return 1;
    }

    private static int challengeDecline(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, RivalChallengeManager.get().declineChallenge(player));
        return 1;
    }

    private static int challengeCancel(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !enabled(player)) {
            return 0;
        }
        DmzRewards.msg(player, RivalChallengeManager.get().cancelChallenge(player));
        return 1;
    }

    private static int refresh(CommandSourceStack source) {
        RivalStore.get().load();
        source.m_288197_(() -> Component.m_237113_("§aRival store reloaded."), true);
        return 1;
    }

    private static int save(CommandSourceStack source) {
        RivalStore.get().markDirty();
        RivalStore.get().save();
        source.m_288197_(() -> Component.m_237113_("§aRival store saved."), true);
        return 1;
    }

    private static boolean enabled(ServerPlayer player) {
        if (!DifficultyConfig.get().enableRivalSystem) {
            DmzRewards.msg(player, "§cRival system is disabled.");
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
