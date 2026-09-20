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
import net.shurui.dev.sdu.network.DmzNet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * SDU 3.0.11 {@code /sdu edit} on Mohist:
 * <ol>
 *   <li>{@code /sdu} requires Forge permission level 2.</li>
 *   <li>The stock executor only opens the hub if {@code getEntity() instanceof ServerPlayer}.</li>
 *   <li>{@code OpenHubPacket.encode} writes 0 bytes. Live 2.12.25 logged
 *       {@code opened SDU hub} twice and the client still showed no screen.
 *       The hub is a vanilla chest; clicks call fat {@code DmzNet.open*} packets.</li>
 * </ol>
 */
public final class SduEditCommandAccess {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final String REQUIRE_MARKER = "dbzlegacy$sduStaffRequires";
    private static final String COMMAND_MARKER = "dbzlegacy$sduEditOpen";
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
            LOGGER.info("[{}] replaced /sdu edit executor (Mohist chest hub)", DmzMohistMeleeFix.MOD_ID);
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
        if ("hub".equals(target)) {
            sendHub(player);
            return 1;
        }
        try {
            try {
                player.m_6915_();
            } catch (Throwable ignored) {
            }
            MinecraftServer server = player.m_20194_();
            Runnable send = () -> {
                try {
                    openNamedEditor(player, target);
                    LOGGER.info("[{}] opened SDU {} editor for {}", DmzMohistMeleeFix.MOD_ID, target, player.m_6302_());
                } catch (Throwable t) {
                    LOGGER.warn("[{}] SDU editor {} failed for {}: {}", DmzMohistMeleeFix.MOD_ID, target, player.m_6302_(), t.toString());
                }
            };
            if (server != null) {
                int now = server.m_129921_();
                server.m_6937_(new TickTask(now + 5, send));
                server.m_6937_(new TickTask(now + 15, send));
            } else {
                send.run();
            }
            player.m_5661_(Component.m_237113_("§eOpening SDU " + target + " editor…"), false);
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
        boolean opened = false;
        try {
            opened = SduStaffHubMenu.open(player);
        } catch (Throwable t) {
            LOGGER.warn("[{}] SDU chest hub failed for {}: {}", DmzMohistMeleeFix.MOD_ID, player.m_6302_(), t.toString());
        }
        sendClickableIndex(player);
        if (opened) {
            player.m_5661_(Component.m_237113_("§eSDU editor menu opened. Click a slot or a chat button."), false);
            LOGGER.info("[{}] opened SDU chest hub for {}", DmzMohistMeleeFix.MOD_ID, player.m_6302_());
        } else {
            player.m_5661_(Component.m_237113_("§eSDU chest failed — use the chat buttons or /sduedit <editor>."), false);
            LOGGER.warn("[{}] SDU chest hub did not attach for {}", DmzMohistMeleeFix.MOD_ID, player.m_6302_());
        }
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

    static void openNamedEditor(ServerPlayer player, String which) throws Exception {
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
