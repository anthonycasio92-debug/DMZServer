package com.dbzlegacy.mohistmelee;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.shurui.dev.sdu.network.DmzNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SDU 3.0.11 {@code /sdu edit} is two silent no-ops on Mohist:
 * <ol>
 *   <li>{@code /sdu} requires Forge permission level 2.</li>
 *   <li>The edit executor only opens the hub if {@code getEntity() instanceof ServerPlayer}.
 *       Mohist sources often have a player sender and a null entity, so the command
 *       returns 0 and sends no GUI.</li>
 * </ol>
 * Chat closing on the client can also clear a same-tick {@code OpenHubPacket}, so the
 * hub is opened two ticks later.
 */
public final class SduEditCommandAccess {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final String REQUIRE_MARKER = "dbzlegacy$sduStaffRequires";
    private static final String COMMAND_MARKER = "dbzlegacy$sduEditOpen";

    private SduEditCommandAccess() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new SduEditCommandAccess());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandNode<CommandSourceStack> sdu = event.getDispatcher().getRoot().getChild("sdu");
        if (sdu != null) {
            wrapRequirement(sdu);
            wrapEditCommand(sdu);
        }
        event.getDispatcher().register(
                Commands.m_82127_("sduedit")
                        .requires(MohistStaffAccess::canEditSdu)
                        .executes(ctx -> openEditor(ctx.getSource()))
        );
        LOGGER.info("[{}] registered /sduedit fallback for SDU 3.0.11 hub", DmzMohistMeleeFix.MOD_ID);
    }

    private static void wrapRequirement(CommandNode<CommandSourceStack> sdu) {
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Predicate<CommandSourceStack> original = (Predicate<CommandSourceStack>) field.get(sdu);
            if (original != null && REQUIRE_MARKER.equals(original.toString())) {
                return;
            }
            Predicate<CommandSourceStack> wrapped = new Predicate<>() {
                @Override
                public boolean test(CommandSourceStack source) {
                    try {
                        if (original != null && original.test(source)) {
                            return true;
                        }
                    } catch (Throwable ignored) {
                    }
                    return MohistStaffAccess.canEditSdu(source);
                }

                @Override
                public String toString() {
                    return REQUIRE_MARKER;
                }
            };
            field.set(sdu, wrapped);
            LOGGER.info("[{}] wrapped /sdu command requirement for Mohist staff", DmzMohistMeleeFix.MOD_ID);
        } catch (Throwable t) {
            LOGGER.warn("[{}] failed to wrap /sdu command requirement: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
        }
    }

    private static void wrapEditCommand(CommandNode<CommandSourceStack> sdu) {
        CommandNode<CommandSourceStack> edit = sdu.getChild("edit");
        if (edit == null) {
            LOGGER.warn("[{}] /sdu has no edit child", DmzMohistMeleeFix.MOD_ID);
            return;
        }
        try {
            Field field = CommandNode.class.getDeclaredField("command");
            field.setAccessible(true);
            Object current = field.get(edit);
            if (current instanceof Command<?> cmd && COMMAND_MARKER.equals(cmd.toString())) {
                return;
            }
            Command<CommandSourceStack> replacement = new Command<>() {
                @Override
                public int run(com.mojang.brigadier.context.CommandContext<CommandSourceStack> ctx) {
                    return openEditor(ctx.getSource());
                }

                @Override
                public String toString() {
                    return COMMAND_MARKER;
                }
            };
            field.set(edit, replacement);
            LOGGER.info("[{}] replaced /sdu edit executor (Mohist player resolve + delayed hub)", DmzMohistMeleeFix.MOD_ID);
        } catch (Throwable t) {
            LOGGER.warn("[{}] failed to replace /sdu edit executor: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
        }
    }

    static int openEditor(CommandSourceStack source) {
        ServerPlayer player = MohistStaffAccess.resolvePlayer(source);
        if (player == null) {
            source.m_81352_(Component.m_237113_("Run /sdu edit as a player in-game."));
            LOGGER.warn("[{}] /sdu edit had no resolvable player", DmzMohistMeleeFix.MOD_ID);
            return 0;
        }
        if (!MohistStaffAccess.canEditSdu(player)) {
            source.m_81352_(Component.m_237113_("No permission to open the SDU editor."));
            return 0;
        }
        MinecraftServer server = player.m_20194_();
        Runnable open = () -> {
            try {
                DmzNet.openHub(player);
                LOGGER.info("[{}] opened SDU hub for {}", DmzMohistMeleeFix.MOD_ID, player.m_6302_());
            } catch (Throwable t) {
                LOGGER.warn(
                        "[{}] DmzNet.openHub failed for {}: {}",
                        DmzMohistMeleeFix.MOD_ID,
                        player.m_6302_(),
                        t.toString()
                );
            }
        };
        if (server != null) {
            server.m_6937_(new TickTask(server.m_129921_() + 2, open));
        } else {
            open.run();
        }
        player.m_5661_(Component.m_237113_("§eOpening SDU editor…"), false);
        return 1;
    }
}
