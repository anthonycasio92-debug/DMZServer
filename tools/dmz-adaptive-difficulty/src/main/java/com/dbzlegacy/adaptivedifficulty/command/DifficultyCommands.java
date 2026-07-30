package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyChatMenu;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Players: {@code /difficulty} opens the chat GUI.
 * GUI buttons use {@code /difficulty do ...} (not for normal player use).
 * Staff: {@code /difficulty admin} toggles config commands.
 */
public final class DifficultyCommands {
    private DifficultyCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new DifficultyCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.m_82127_("difficulty")
                .executes(ctx -> openGui(ctx.getSource()))
                // Hidden GUI action handler used by clickable chat buttons
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82129_("action", StringArgumentType.word())
                                .executes(ctx -> guiDo(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "action"),
                                        null))
                                .then(Commands.m_82129_("arg", StringArgumentType.word())
                                        .executes(ctx -> guiDo(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "action"),
                                                StringArgumentType.getString(ctx, "arg"))))))
                .then(Commands.m_82127_("admin")
                        .requires(DifficultyCommands::canUseAdminToggle)
                        .executes(ctx -> toggleAdmin(ctx.getSource()))
                        .then(Commands.m_82127_("help")
                                .requires(DifficultyCommands::hasAdminMode)
                                .executes(ctx -> adminHelp(ctx.getSource())))
                        .then(Commands.m_82127_("reload")
                                .requires(DifficultyCommands::hasAdminMode)
                                .executes(ctx -> reload(ctx.getSource())))
                        .then(Commands.m_82127_("settings")
                                .requires(DifficultyCommands::hasAdminMode)
                                .executes(ctx -> openAdminSettings(ctx.getSource())))
                        .then(Commands.m_82127_("set")
                                .requires(DifficultyCommands::hasAdminMode)
                                .then(Commands.m_82129_("key", StringArgumentType.word())
                                        .then(Commands.m_82129_("value", StringArgumentType.greedyString())
                                                .executes(ctx -> adminSet(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "key"),
                                                        StringArgumentType.getString(ctx, "value")))))));

        event.getDispatcher().register(root);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] registered /difficulty (server-only chat GUI; admin toggle for staff)",
                AdaptiveDifficultyMod.MOD_ID
        );
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            AdminCommandAccess.disable(player);
        }
    }

    private static int guiDo(CommandSourceStack source, String action, String arg) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return 0;
        }
        String act = action == null ? "" : action.toLowerCase();
        if ("page".equals(act)) {
            String targetPage = arg == null || arg.isBlank() ? "main" : arg;
            DifficultyActions.Result result =
                    DifficultyActions.handle(player, DifficultyActions.ACT_PAGE, 0L, targetPage);
            result.tell(player);
            return 1;
        }
        long amount = 0L;
        if (arg != null && !arg.isBlank()) {
            try {
                amount = Long.parseLong(arg);
            } catch (NumberFormatException ignored) {
                amount = 0L;
            }
        }
        DifficultyActions.Result result = DifficultyActions.handle(player, act, amount, "main");
        result.tell(player);
        return result.ok() ? 1 : 0;
    }

    private static boolean canUseAdminToggle(CommandSourceStack src) {
        if (src.m_230896_() == null) {
            return src.m_6761_(2);
        }
        return isStaff(src);
    }

    private static boolean hasAdminMode(CommandSourceStack src) {
        ServerPlayer player = src.m_230896_();
        if (player == null) {
            return src.m_6761_(2);
        }
        return isStaff(src) && AdminCommandAccess.isEnabled(player);
    }

    private static boolean isStaff(CommandSourceStack src) {
        if (src.m_6761_(2)) {
            return true;
        }
        ServerPlayer player = src.m_230896_();
        if (player == null) {
            return false;
        }
        String node = DifficultyConfig.get().adminPermission;
        try {
            var method = player.getClass().getMethod("hasPermission", String.class);
            Object result = method.invoke(player, node);
            return result instanceof Boolean b && b;
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static int openGui(CommandSourceStack source) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            source.m_81352_(Component.m_237113_("Players only. Use /difficulty admin … from console."));
            return 0;
        }
        DifficultyMenu.open(player, "main");
        return 1;
    }

    private static int toggleAdmin(CommandSourceStack source) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return adminHelp(source);
        }
        boolean enabled = AdminCommandAccess.toggle(player);
        if (enabled) {
            source.m_288197_(() -> Component.m_237113_(
                    "§aAdmin commands ENABLED.\n"
                            + "§7/difficulty admin help|reload|settings\n"
                            + "§7/difficulty admin set <key> <value>\n"
                            + "§8Run §f/difficulty admin §8again to disable."
            ), false);
        } else {
            source.m_288197_(() -> Component.m_237113_("§cAdmin commands DISABLED."), false);
        }
        return 1;
    }

    private static int openAdminSettings(CommandSourceStack source) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return adminHelp(source);
        }
        DifficultyChatMenu.open(player, "settings");
        return 1;
    }

    private static int reload(CommandSourceStack source) {
        DifficultyConfig.reload();
        DifficultyCache.invalidateAll();
        source.m_288197_(() -> Component.m_237113_("§aAdaptive difficulty config reloaded."), true);
        return 1;
    }

    private static int adminHelp(CommandSourceStack source) {
        source.m_288197_(() -> Component.m_237113_(
                "§6Adaptive Difficulty — admin (server-side only mod)\n"
                        + "§e/difficulty §7— open player chat GUI\n"
                        + "§e/difficulty admin §7— toggle admin command access\n"
                        + "§e/difficulty admin reload|settings\n"
                        + "§e/difficulty admin set <key> <value>"
        ), false);
        return 1;
    }

    private static int adminSet(CommandSourceStack source, String key, String value) {
        DifficultyConfig cfg = DifficultyConfig.get();
        try {
            switch (key.toLowerCase()) {
                case "prestigemultiplier" -> cfg.prestigeMultiplier = Double.parseDouble(value);
                case "levelmultiplier" -> cfg.levelMultiplier = Double.parseDouble(value);
                case "teambonus", "teambonuspercent" -> cfg.teamBonusPercent = Double.parseDouble(value);
                case "contribution", "contributionpercent" -> cfg.contributionPercent = Double.parseDouble(value);
                case "basecost" -> cfg.baseCost = Long.parseLong(value);
                case "costscaling" -> cfg.costScaling = Long.parseLong(value);
                case "rewardscaling" -> cfg.rewardScaling = Double.parseDouble(value);
                case "health", "healthpercentperdifficulty" -> cfg.healthPercentPerDifficulty = Double.parseDouble(value);
                case "damage", "damagepercentperdifficulty" -> cfg.damagePercentPerDifficulty = Double.parseDouble(value);
                case "defense", "defensepercentperdifficulty" -> cfg.defensePercentPerDifficulty = Double.parseDouble(value);
                case "mobscaleradius" -> cfg.mobScaleRadius = Double.parseDouble(value);
                case "purchasecurrency" -> cfg.purchaseCurrency = value.trim();
                case "enablemobscaling" -> cfg.enableMobScaling = Boolean.parseBoolean(value);
                case "enablerewardscaling" -> cfg.enableRewardScaling = Boolean.parseBoolean(value);
                case "enableelites" -> cfg.enableElites = Boolean.parseBoolean(value);
                case "elitechance", "elitechancepercent" -> cfg.eliteChancePercent = Double.parseDouble(value);
                case "enablemutations" -> cfg.enableMutations = Boolean.parseBoolean(value);
                case "mutationchance", "mutationchancepercent" -> cfg.mutationChancePercent = Double.parseDouble(value);
                case "enableadaptiveai" -> cfg.enableAdaptiveAi = Boolean.parseBoolean(value);
                case "enableenemyevolution" -> cfg.enableEnemyEvolution = Boolean.parseBoolean(value);
                case "enablebossscaling" -> cfg.enableBossScaling = Boolean.parseBoolean(value);
                case "bossstatmultiplier" -> cfg.bossStatMultiplier = Double.parseDouble(value);
                case "bosshealththreshold" -> cfg.bossHealthThreshold = Double.parseDouble(value);
                case "maxhealthmultiplier" -> cfg.maxHealthMultiplier = Double.parseDouble(value);
                case "maxscaledhealth" -> cfg.maxScaledHealth = Double.parseDouble(value);
                case "maxmovemultiplier" -> cfg.maxMoveMultiplier = Double.parseDouble(value);
                case "maxarmorbonus" -> cfg.maxArmorBonus = Double.parseDouble(value);
                case "maxdamagemultiplier" -> cfg.maxDamageMultiplier = Double.parseDouble(value);
                case "adminpermission" -> cfg.adminPermission = value.trim();
                default -> {
                    source.m_81352_(Component.m_237113_("Unknown key: " + key));
                    return 0;
                }
            }
            DifficultyConfig.save();
            DifficultyCache.invalidateAll();
            source.m_288197_(() -> Component.m_237113_("§aSet §f" + key + " §a= §f" + value), true);
            return 1;
        } catch (Exception e) {
            source.m_81352_(Component.m_237113_("Invalid value: " + e.getMessage()));
            return 0;
        }
    }
}
