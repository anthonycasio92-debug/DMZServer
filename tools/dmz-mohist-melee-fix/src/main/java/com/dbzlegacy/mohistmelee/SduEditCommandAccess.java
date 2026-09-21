package com.dbzlegacy.mohistmelee;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.CommandNode;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.function.Predicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.simple.SimpleChannel;
import net.shurui.dev.sdu.network.DmzNet;
import net.shurui.dev.sdu.network.OpenHubPacket;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SDU 3.0.11 {@code /sdu edit} on Mohist:
 * <ol>
 *   <li>{@code /sdu} requires Forge permission level 2.</li>
 *   <li>The stock executor only opens the hub if {@code getEntity() instanceof ServerPlayer}.</li>
 *   <li>{@code OpenHubPacket.encode} writes 0 bytes. Live 2.12.24 logged
 *       {@code opened SDU hub for JLDK1310} and the client still showed no screen —
 *       empty custom payloads are dropped. {@link com.dbzlegacy.mohistmelee.mixin.OpenHubPacketEncodeMixin}
 *       writes one byte; we also {@code sendTo} the connection and retry after chat closes.</li>
 *   <li>No vanilla chest menu — use SDU hub screen + {@code /sduedit &lt;editor&gt;} chat links.</li>
 * </ol>
 */
public final class SduEditCommandAccess {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final String REQUIRE_MARKER = "dbzlegacy$sduStaffRequires";
    private static final String COMMAND_MARKER = "dbzlegacy$sduEditOpen";
    private static final int[] HUB_DELAY_TICKS = {10, 25};
    private static final String[][] HUB_LINKS = {
            {"race", "§e[Race]"},
            {"form", "§d[Form]"},
            {"saga", "§b[Saga]"},
            {"sidequest", "§a[Sidequest]"},
            {"wish", "§5[Wish]"},
            {"shrine", "§6[Shrine]"},
            {"options", "§7[Options]"}
    };

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
                        .executes(ctx -> openEditor(ctx.getSource(), "hub"))
                        .then(Commands.m_82129_("which", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    for (String name : new String[]{
                                            "hub", "race", "form", "saga", "sidequest", "wish", "shrine", "options"
                                    }) {
                                        builder.suggest(name);
                                    }
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> openEditor(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "which")
                                )))
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
                    return openEditor(ctx.getSource(), "hub");
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

    static int openEditor(CommandSourceStack source, String which) {
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
        String target = which == null || which.isBlank() ? "hub" : which.trim().toLowerCase(Locale.ROOT);
        MinecraftServer server = player.m_20194_();
        if ("hub".equals(target)) {
            sendClickableIndex(player);
            if (server != null) {
                for (int delay : HUB_DELAY_TICKS) {
                    int tick = server.m_129921_() + delay;
                    server.m_6937_(new TickTask(tick, () -> sendHub(player)));
                }
            } else {
                sendHub(player);
            }
            player.m_5661_(Component.m_237113_("§eOpening SDU editor… (or use chat links / /sduedit <editor>)"), false);
            return 1;
        }
        try {
            openNamedEditor(player, target);
            player.m_5661_(Component.m_237113_("§eOpening SDU " + target + " editor…"), false);
            LOGGER.info("[{}] opened SDU {} editor for {}", DmzMohistMeleeFix.MOD_ID, target, player.m_6302_());
            return 1;
        } catch (Throwable t) {
            source.m_81352_(Component.m_237113_("Unknown editor '" + target + "'. Use hub, race, form, saga, sidequest, wish, shrine, options."));
            LOGGER.warn("[{}] SDU editor {} failed for {}: {}", DmzMohistMeleeFix.MOD_ID, target, player.m_6302_(), t.toString());
            return 0;
        }
    }

    private static void sendHub(ServerPlayer player) {
        if (player == null || player.m_9236_() == null) {
            return;
        }
        try {
            DmzNet.openHub(player);
        } catch (Throwable t) {
            LOGGER.warn("[{}] DmzNet.openHub failed for {}: {}", DmzMohistMeleeFix.MOD_ID, player.m_6302_(), t.toString());
        }
        try {
            sendHubOnConnection(player);
        } catch (Throwable t) {
            LOGGER.warn("[{}] SDU hub sendTo failed for {}: {}", DmzMohistMeleeFix.MOD_ID, player.m_6302_(), t.toString());
        }
        LOGGER.info("[{}] opened SDU hub for {}", DmzMohistMeleeFix.MOD_ID, player.m_6302_());
    }

    private static void sendClickableIndex(ServerPlayer player) {
        MutableComponent line = Component.m_237113_("§6SDU editors: ");
        for (int i = 0; i < HUB_LINKS.length; i++) {
            if (i > 0) {
                line.m_7220_(Component.m_237113_(" "));
            }
            String which = HUB_LINKS[i][0];
            line.m_7220_(Component.m_237113_(HUB_LINKS[i][1]).m_130938_(style -> style
                    .m_131142_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/sduedit " + which))
                    .m_131144_(new HoverEvent(HoverEvent.Action.f_130831_, Component.m_237113_("Open SDU " + which)))));
        }
        player.m_5661_(line, false);
    }

    private static void sendHubOnConnection(ServerPlayer player) throws Exception {
        Field channelField = DmzNet.class.getDeclaredField("channel");
        channelField.setAccessible(true);
        Object raw = channelField.get(null);
        if (!(raw instanceof SimpleChannel channel)) {
            return;
        }
        Connection connection = playerConnection(player);
        if (connection == null) {
            return;
        }
        channel.sendTo(new OpenHubPacket(), connection, NetworkDirection.PLAY_TO_CLIENT);
    }

    private static Connection playerConnection(ServerPlayer player) {
        try {
            Object listener = firstField(player, "connection", "f_8906_");
            if (listener == null) {
                return null;
            }
            if (listener instanceof Connection connection) {
                return connection;
            }
            Object raw = firstField(listener, "connection", "f_9742_");
            return raw instanceof Connection connection ? connection : null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object firstField(Object owner, String... names) {
        Class<?> type = owner.getClass();
        while (type != null && type != Object.class) {
            for (String name : names) {
                try {
                    Field field = type.getDeclaredField(name);
                    field.setAccessible(true);
                    return field.get(owner);
                } catch (ReflectiveOperationException ignored) {
                }
            }
            type = type.getSuperclass();
        }
        return null;
    }

    private static void openNamedEditor(ServerPlayer player, String which) throws Exception {
        String methodName = switch (which) {
            case "race" -> "openRaceEditor";
            case "form" -> "openFormEditor";
            case "saga" -> "openSagaEditor";
            case "sidequest" -> "openSideQuestEditor";
            case "wish" -> "openWishEditor";
            case "shrine" -> "openShrineConfig";
            case "options" -> "openOptions";
            default -> throw new IllegalArgumentException(which);
        };
        Method method = DmzNet.class.getMethod(methodName, ServerPlayer.class);
        method.invoke(null, player);
    }
}
