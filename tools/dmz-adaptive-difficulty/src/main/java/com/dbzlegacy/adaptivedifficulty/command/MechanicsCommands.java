package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsMenu;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
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
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.m_82127_("legacymechanics")
                .executes(ctx -> open(ctx.getSource(), "main"))
                .then(Commands.m_82127_("gui").executes(ctx -> open(ctx.getSource(), "main")))
                .then(Commands.m_82127_("help").executes(ctx -> open(ctx.getSource(), "help")))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> open(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page"))))));
        event.getDispatcher().register(root);
        event.getDispatcher().register(Commands.m_82127_("lm")
                .executes(ctx -> open(ctx.getSource(), "main"))
                .then(Commands.m_82127_("gui").executes(ctx -> open(ctx.getSource(), "main")))
                .then(Commands.m_82127_("help").executes(ctx -> open(ctx.getSource(), "help")))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> open(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page")))))));
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /legacymechanics /lm", AdaptiveDifficultyMod.MOD_ID);
    }

    private static int open(CommandSourceStack source, String page) {
        ServerPlayer player = null;
        try {
            player = source.m_81375_();
        } catch (Exception e) {
            return 0;
        }
        MechanicsMenu.open(player, page);
        return 1;
    }
}
