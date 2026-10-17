package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesMenu;
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

/**
 * {@code /character} — paid race / class / reskin services with cosmetic unlocks.
 * Player-facing with parity for the Character Services screen; the {@code do} backend
 * mirrors {@code /difficulty do} for chat buttons and typed actions.
 */
public final class CharacterCommands {
    private CharacterCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new CharacterCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        event.getDispatcher().register(root("character"));
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /character", AdaptiveDifficultyMod.MOD_ID);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> root(String name) {
        return Commands.m_82127_(name)
                .requires(CommandAccess::isPlayerSlashUser)
                .executes(ctx -> open(ctx.getSource(), "main"))
                .then(Commands.m_82127_("help").executes(ctx -> characterHelp(ctx.getSource())))
                .then(Commands.m_82127_("race")
                        .then(Commands.m_82129_("race", StringArgumentType.word())
                                .then(Commands.m_82129_("keep%", IntegerArgumentType.integer(0, 100))
                                        .suggests(LmCommandSuggestions.CHARACTER_KEEP_PCT)
                                        .executes(ctx -> raceConfirm(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "race"),
                                                IntegerArgumentType.getInteger(ctx, "keep%"))))))
                .then(Commands.m_82127_("class")
                        .then(Commands.m_82129_("class", StringArgumentType.word())
                                .executes(ctx -> characterDo(
                                        ctx.getSource(),
                                        "class_confirm",
                                        StringArgumentType.getString(ctx, "class")))))
                .then(Commands.m_82127_("reskin")
                        .executes(ctx -> characterDo(ctx.getSource(), "reskin_confirm", "")))
                .then(Commands.m_82127_("do")
                        .then(LmCommandSuggestions.word("action", LmCommandSuggestions.CHARACTER_ACTIONS)
                                .executes(ctx -> characterDoRest(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "action"),
                                        ""))
                                .then(Commands.m_82129_("arg", StringArgumentType.greedyString())
                                        .executes(ctx -> characterDoRest(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "action"),
                                                StringArgumentType.getString(ctx, "arg"))))));
    }

    private static int characterHelp(CommandSourceStack source) {
        LmCommandFeedback.tellLines(source, LmCommandHelp.characterPlayer());
        return 1;
    }

    private static int open(CommandSourceStack source, String page) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            return 0;
        }
        CharacterServicesMenu.open(player, page);
        return 1;
    }

    private static int raceConfirm(CommandSourceStack source, String race, int keepPct) {
        return characterDo(source, "race_confirm", race + ":" + keepPct);
    }

    /** {@code /character do <action> [arg…]} — chat-button parity backend (no screen reopen). */
    private static int characterDoRest(CommandSourceStack source, String action, String rest) {
        return characterDo(source, action, rest == null ? "" : rest.trim());
    }

    private static int characterDo(CommandSourceStack source, String action, String arg) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            source.m_288197_(() -> Component.m_237113_(LmCommandMessages.NEED_IN_GAME), false);
            return 0;
        }
        String msg = CharacterServicesGuiApi.handleDo(player, action, arg, "main");
        if (msg != null && !msg.isBlank()) {
            LmCommandFeedback.tell(source, msg);
        }
        return msg != null && msg.startsWith("§c") ? 0 : 1;
    }

    private static ServerPlayer playerOrNull(CommandSourceStack source) {
        try {
            return source.m_81375_();
        } catch (Exception e) {
            return null;
        }
    }
}
