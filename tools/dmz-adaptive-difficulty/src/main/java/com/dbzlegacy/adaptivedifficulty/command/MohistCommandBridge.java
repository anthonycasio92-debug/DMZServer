package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Mohist routes player chat through Bukkit. Without LegacyMechanicsGUI, only top-level
 * Forge literals work — subcommands like {@code /lm admin} show as unknown. Registers
 * Bukkit commands that forward the full line to Forge brigadier.
 */
public final class MohistCommandBridge {
    private static volatile boolean registered;

    private MohistCommandBridge() {}

    public static void tryRegister(MinecraftServer server) {
        if (registered || server == null) {
            return;
        }
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object pluginManager = bukkit.getMethod("getPluginManager").invoke(null);
            if (pluginManager.getClass().getMethod("getPlugin", String.class)
                    .invoke(pluginManager, "LegacyMechanicsGUI") != null) {
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] Mohist command bridge skipped (LegacyMechanicsGUI loaded)",
                        AdaptiveDifficultyMod.MOD_ID);
                registered = true;
                return;
            }
            Object hostPlugin = pluginManager.getClass().getMethod("getPlugin", String.class)
                    .invoke(pluginManager, AdaptiveDifficultyMod.MOD_ID);
            if (hostPlugin == null) {
                hostPlugin = pluginManager.getClass().getMethod("getPlugin", String.class)
                        .invoke(pluginManager, "LegacyMechanics");
            }
            if (hostPlugin == null) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Mohist command bridge: no Bukkit plugin host for {}",
                        AdaptiveDifficultyMod.MOD_ID,
                        AdaptiveDifficultyMod.MOD_ID);
                return;
            }
            Object commandMap = bukkit.getMethod("getCommandMap").invoke(null);
            Class<?> commandExecutor = Class.forName("org.bukkit.command.CommandExecutor");
            Object executor = Proxy.newProxyInstance(
                    commandExecutor.getClassLoader(),
                    new Class<?>[] {commandExecutor},
                    (proxy, method, args) -> {
                        if (!"onCommand".equals(method.getName()) || args == null || args.length < 4) {
                            return false;
                        }
                        Object sender = args[0];
                        String label = String.valueOf(args[2]);
                        String[] cmdArgs = (String[]) args[3];
                        dispatch(server, sender, label, cmdArgs);
                        return true;
                    });

            Class<?> pluginCommandClass = Class.forName("org.bukkit.command.PluginCommand");
            Constructor<?> ctor = pluginCommandClass.getDeclaredConstructor(String.class,
                    Class.forName("org.bukkit.plugin.Plugin"));
            ctor.setAccessible(true);

            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "lm",
                    List.of("legacymechanics"));
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "difficulty",
                    List.of("diff"));
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "rival", List.of());
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "spar",
                    List.of("sparring"));
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "skillcheck",
                    List.of());
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "progression",
                    List.of("prog"));
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "prestige",
                    List.of());
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "skills",
                    List.of());
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "character",
                    List.of("characterservices", "charservices"));
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "enddragon",
                    List.of());
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "cleardragons",
                    List.of());
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "spawndragon",
                    List.of());
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "killdragons",
                    List.of());
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "androidify",
                    List.of("androidification"));
            registerOne(server, commandMap, pluginCommandClass, ctor, hostPlugin, executor, "padmin",
                    List.of("prestigeadmin"));

            registered = true;
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Mohist Bukkit→Forge command bridge registered (Forge-only mode)",
                    AdaptiveDifficultyMod.MOD_ID);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Mohist command bridge failed (non-Mohist server?): {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
        }
    }

    private static void registerOne(
            MinecraftServer server,
            Object commandMap,
            Class<?> pluginCommandClass,
            Constructor<?> ctor,
            Object hostPlugin,
            Object executor,
            String name,
            List<String> aliases) throws ReflectiveOperationException {
        Object cmd = ctor.newInstance(name, hostPlugin);
        cmd.getClass().getMethod("setExecutor", Class.forName("org.bukkit.command.CommandExecutor"))
                .invoke(cmd, executor);
        if (!aliases.isEmpty()) {
            cmd.getClass().getMethod("setAliases", List.class).invoke(cmd, aliases);
        }
        cmd.getClass().getMethod("setDescription", String.class)
                .invoke(cmd, "LegacyMechanics → Forge (" + name + ")");
        Method register = commandMap.getClass().getMethod("register", String.class,
                Class.forName("org.bukkit.command.Command"));
        register.invoke(commandMap, name, cmd);
    }

    static void dispatch(MinecraftServer server, Object sender, String label, String[] args) {
        CommandSourceStack source = commandSource(server, sender);
        if (source == null) {
            return;
        }
        StringBuilder line = new StringBuilder(label);
        if (args != null) {
            for (String arg : args) {
                if (arg != null && !arg.isEmpty()) {
                    line.append(' ').append(arg);
                }
            }
        }
        try {
            server.m_129892_().m_230957_(source, line.toString());
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] Command dispatch failed for /{}: {}",
                    AdaptiveDifficultyMod.MOD_ID, line, t.toString());
        }
    }

    private static CommandSourceStack commandSource(MinecraftServer server, Object sender) {
        if (sender == null) {
            return null;
        }
        String senderName = sender.getClass().getName();
        if (senderName.contains("ConsoleCommandSender")
                || senderName.contains("RemoteConsoleCommandSender")) {
            return server.m_129893_();
        }
        try {
            Object handle = sender.getClass().getMethod("getHandle").invoke(sender);
            if (handle instanceof ServerPlayer sp) {
                CommandSourceStack stack = commandSourceStack(sp);
                if (stack != null) {
                    return stack;
                }
            } else if (handle != null) {
                CommandSourceStack stack = commandSourceStack(handle);
                if (stack != null) {
                    return stack;
                }
            }
        } catch (Throwable ignored) {
        }
        return server.m_129893_();
    }

    private static CommandSourceStack commandSourceStack(Object nmsPlayer) {
        if (nmsPlayer == null) {
            return null;
        }
        if (nmsPlayer instanceof ServerPlayer sp) {
            for (Method m : ServerPlayer.class.getMethods()) {
                if (m.getParameterCount() == 0
                        && CommandSourceStack.class.isAssignableFrom(m.getReturnType())) {
                    try {
                        return (CommandSourceStack) m.invoke(sp);
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        for (Method m : nmsPlayer.getClass().getMethods()) {
            if (m.getParameterCount() == 0
                    && m.getReturnType().getName().endsWith("CommandSourceStack")) {
                try {
                    return (CommandSourceStack) m.invoke(nmsPlayer);
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }
}
