package com.dbzlegacy.mohistmelee;

import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Field;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SDU registers {@code /sdu} with {@code CommandSourceStack.hasPermission(2)}.
 * That hides {@code /sdu edit} from Mohist staff even when they are Bukkit OP.
 * Wrap the node requirement after SDU registers so the command is visible and runnable.
 */
public final class SduEditCommandAccess {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final String MARKER = "dbzlegacy$sduStaffRequires";

    private SduEditCommandAccess() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new SduEditCommandAccess());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRegisterCommands(RegisterCommandsEvent event) {
        CommandNode<CommandSourceStack> sdu = event.getDispatcher().getRoot().getChild("sdu");
        if (sdu == null) {
            return;
        }
        try {
            Field field = CommandNode.class.getDeclaredField("requirement");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Predicate<CommandSourceStack> original = (Predicate<CommandSourceStack>) field.get(sdu);
            if (original != null && MARKER.equals(original.toString())) {
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
                    return MARKER;
                }
            };
            field.set(sdu, wrapped);
            LOGGER.info("[{}] wrapped /sdu command requirement for Mohist staff", DmzMohistMeleeFix.MOD_ID);
        } catch (Throwable t) {
            LOGGER.warn("[{}] failed to wrap /sdu command requirement: {}", DmzMohistMeleeFix.MOD_ID, t.toString());
        }
    }
}
