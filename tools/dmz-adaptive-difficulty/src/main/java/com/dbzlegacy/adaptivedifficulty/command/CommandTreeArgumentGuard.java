package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.synchronization.ArgumentTypeInfos;

/**
 * Staff login builds {@code ClientboundCommandsPacket} from the live command tree.
 * Every {@link ArgumentType} in that tree must be known to {@link ArgumentTypeInfos}.
 * A custom type that is missing from the map throws during player placement and kicks
 * staff with "Invalid player data". LM is server-only, so prefer vanilla argument
 * types ({@code brigadier:string}, doubles, …) over registering a custom network type
 * clients do not have.
 */
public final class CommandTreeArgumentGuard {
    private CommandTreeArgumentGuard() {}

    /** Walk the dispatcher and log every argument type {@link ArgumentTypeInfos} rejects. */
    public static void audit(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (dispatcher == null) {
            return;
        }
        List<String> bad = new ArrayList<>();
        Map<CommandNode<?>, Boolean> seen = new IdentityHashMap<>();
        walk(dispatcher.getRoot(), "", seen, bad);
        if (bad.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] command tree argument types are registered for client sync",
                    AdaptiveDifficultyMod.MOD_ID);
            return;
        }
        for (String line : bad) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] unregistered command argument type (staff login will fail): {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    line);
        }
    }

    private static void walk(
            CommandNode<?> node,
            String path,
            Map<CommandNode<?>, Boolean> seen,
            List<String> bad) {
        if (node == null || seen.put(node, Boolean.TRUE) != null) {
            return;
        }
        String here = path;
        if (node instanceof ArgumentCommandNode<?, ?> arg) {
            String name = arg.getName();
            here = path.isEmpty() ? "<" + name + ">" : path + " <" + name + ">";
            ArgumentType<?> type = arg.getType();
            Class<?> clazz = type == null ? null : type.getClass();
            if (clazz == null || !ArgumentTypeInfos.m_235391_(clazz)) {
                bad.add(here + " -> " + (clazz == null ? "null" : clazz.getName()));
            }
        } else if (node.getName() != null && !node.getName().isEmpty()) {
            here = path.isEmpty() ? node.getName() : path + " " + node.getName();
        }
        for (CommandNode<?> child : node.getChildren()) {
            walk(child, here, seen, bad);
        }
    }
}
