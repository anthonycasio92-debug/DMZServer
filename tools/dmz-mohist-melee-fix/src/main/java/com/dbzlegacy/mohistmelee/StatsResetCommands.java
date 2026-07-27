package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.server.commands.DMZPermissions;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Vanilla DMZ only wires keepPercentage/keepSkills under
 * {@code /dmzstats reset <targets> ...}, so {@code /dmzstats reset 100 true} does not do what
 * players expect (and bare {@code /dmzstats reset} full-wipes with keepSkills=false).
 * <p>
 * Adds self-targeted siblings:
 * <ul>
 *   <li>{@code /dmzstats reset <keepPercentage>}</li>
 *   <li>{@code /dmzstats reset <keepPercentage> <keepSkills>}</li>
 * </ul>
 */
public final class StatsResetCommands {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);

    private static final SuggestionProvider<CommandSourceStack> PERCENTAGE_SUGGESTIONS =
            (ctx, builder) -> {
                for (String s : new String[] {"0", "25", "50", "75", "100"}) {
                    if (s.startsWith(builder.getRemainingLowerCase())) {
                        builder.suggest(s);
                    }
                }
                return builder.buildFuture();
            };

    private StatsResetCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new StatsResetCommands());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRegisterCommands(RegisterCommandsEvent event) {
        try {
            CommandNode<CommandSourceStack> root = event.getDispatcher().getRoot();
            CommandNode<CommandSourceStack> dmzstats = root.getChild("dmzstats");
            if (!(dmzstats instanceof LiteralCommandNode)) {
                return;
            }
            CommandNode<CommandSourceStack> reset = dmzstats.getChild("reset");
            if (!(reset instanceof LiteralCommandNode)) {
                return;
            }
            // Already patched (reload / duplicate event)
            if (reset.getChild("keepPercentage") != null) {
                return;
            }

            RequiredArgumentBuilder<CommandSourceStack, String> keepPercentage =
                    Commands.m_82129_("keepPercentage", StringArgumentType.word())
                            .suggests(PERCENTAGE_SUGGESTIONS)
                            .executes(ctx -> resetSelf(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "keepPercentage"),
                                    false
                            ));

            keepPercentage.then(
                    Commands.m_82129_("keepSkills", BoolArgumentType.bool())
                            .executes(ctx -> resetSelf(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "keepPercentage"),
                                    BoolArgumentType.getBool(ctx, "keepSkills")
                            ))
            );

            // LiteralCommandNode.addChild is available on CommandNode
            reset.addChild(keepPercentage.build());
            LOGGER.info(
                    "[{}] patched /dmzstats reset <keepPercentage> [keepSkills] for self targets",
                    DmzMohistMeleeFix.MOD_ID
            );
        } catch (Throwable t) {
            LOGGER.warn(
                    "[{}] failed to patch dmzstats reset self-args: {}",
                    DmzMohistMeleeFix.MOD_ID,
                    t.toString()
            );
        }
    }

    private static int resetSelf(CommandSourceStack source, String keepPercentageStr, boolean keepSkills) {
        ServerPlayer self;
        try {
            self = source.m_81375_();
        } catch (Exception e) {
            source.m_81352_(Component.m_237113_("This reset form requires a player."));
            return 0;
        }
        if (!DMZPermissions.check(source, DMZPermissions.STATS_RESET_SELF, DMZPermissions.STATS_RESET_OTHERS)) {
            source.m_81352_(Component.m_237113_("No permission to reset stats."));
            return 0;
        }

        Integer keepPercentage = null;
        if (keepPercentageStr != null && !keepPercentageStr.isEmpty()) {
            try {
                keepPercentage = Integer.parseInt(keepPercentageStr);
                if (keepPercentage <= -1 || keepPercentage >= 101) {
                    source.m_81352_(Component.m_237110_(
                            "command.dragonminez.stats.invalid_number",
                            keepPercentageStr
                    ));
                    return 0;
                }
            } catch (NumberFormatException e) {
                source.m_81352_(Component.m_237110_(
                        "command.dragonminez.stats.invalid_number",
                        keepPercentageStr
                ));
                return 0;
            }
        }

        Integer finalKeepPercentage = keepPercentage;
        StatsData data = StatsProvider.get(StatsCapability.INSTANCE, (Entity) self).orElse(null);
        if (data != null) {
            data.resetPlayerProgress(self, finalKeepPercentage, keepSkills, false);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(self), self);
        }

        source.m_288197_(
                () -> Component.m_237110_(
                        "command.dragonminez.stats.reset.success",
                        self.m_7755_().getString()
                ),
                true
        );
        return 1;
    }
}
