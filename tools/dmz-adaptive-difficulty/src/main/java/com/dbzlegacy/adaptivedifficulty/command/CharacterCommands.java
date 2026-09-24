package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesMenu;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
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
        event.getDispatcher().register(staffCharacterRoot("character"));
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /character", AdaptiveDifficultyMod.MOD_ID);
    }

    private static LiteralArgumentBuilder<CommandSourceStack> staffCharacterRoot(String name) {
        return Commands.m_82127_(name)
                .requires(StaffAccess::isStaffSource)
                .executes(ctx -> open(ctx.getSource(), "main"))
                .then(Commands.m_82127_("help").executes(ctx -> characterHelp(ctx.getSource())));
    }

    private static int characterHelp(CommandSourceStack source) {
        LmCommandFeedback.tellLines(source, LmCommandHelp.characterStaff());
        return 1;
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
