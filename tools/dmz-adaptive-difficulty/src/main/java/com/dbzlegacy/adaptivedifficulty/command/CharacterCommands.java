package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesMenu;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** {@code /character} — paid race / class / reskin services. */
public final class CharacterCommands {
    private CharacterCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new CharacterCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.m_82127_("character")
                .executes(ctx -> open(ctx.getSource(), "main"));
        event.getDispatcher().register(root);
        event.getDispatcher().register(Commands.m_82127_("characterservices").executes(ctx -> open(ctx.getSource(), "main")));
        event.getDispatcher().register(Commands.m_82127_("charservices").executes(ctx -> open(ctx.getSource(), "main")));
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /character", AdaptiveDifficultyMod.MOD_ID);
    }

    private static int open(CommandSourceStack source, String page) {
        ServerPlayer player;
        try {
            player = source.m_81375_();
        } catch (Exception e) {
            return 0;
        }
        CharacterServicesMenu.open(player, page);
        return 1;
    }
}
