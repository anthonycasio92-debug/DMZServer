package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyChatMenu;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DifficultyCommands {
    private DifficultyCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new DifficultyCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.m_82127_("difficulty")
                .executes(ctx -> openGui(ctx.getSource(), "main"))
                .then(Commands.m_82127_("gui")
                        .executes(ctx -> openGui(ctx.getSource(), "main"))
                        .then(Commands.m_82129_("page", StringArgumentType.word())
                                .executes(ctx -> openGui(ctx.getSource(), StringArgumentType.getString(ctx, "page")))))
                .then(Commands.m_82127_("chat")
                        .executes(ctx -> openChat(ctx.getSource(), "main"))
                        .then(Commands.m_82129_("page", StringArgumentType.word())
                                .executes(ctx -> openChat(ctx.getSource(), StringArgumentType.getString(ctx, "page")))))
                .then(Commands.m_82127_("show")
                        .executes(ctx -> show(ctx.getSource())))
                .then(Commands.m_82127_("up")
                        .executes(ctx -> act(ctx.getSource(), DifficultyActions.ACT_UP, 100))
                        .then(Commands.m_82129_("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> act(ctx.getSource(), DifficultyActions.ACT_UP, LongArgumentType.getLong(ctx, "amount")))))
                .then(Commands.m_82127_("down")
                        .executes(ctx -> act(ctx.getSource(), DifficultyActions.ACT_DOWN, 100))
                        .then(Commands.m_82129_("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> act(ctx.getSource(), DifficultyActions.ACT_DOWN, LongArgumentType.getLong(ctx, "amount")))))
                .then(Commands.m_82127_("set")
                        .then(Commands.m_82129_("amount", LongArgumentType.longArg(0))
                                .executes(ctx -> act(ctx.getSource(), DifficultyActions.ACT_SET, LongArgumentType.getLong(ctx, "amount"))))
                        .then(Commands.m_82129_("key", StringArgumentType.word())
                                .requires(DifficultyCommands::isAdmin)
                                .then(Commands.m_82129_("value", StringArgumentType.greedyString())
                                        .executes(ctx -> adminSet(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "key"),
                                                StringArgumentType.getString(ctx, "value"))))))
                .then(Commands.m_82127_("buy")
                        .then(Commands.m_82129_("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> act(ctx.getSource(), DifficultyActions.ACT_BUY, LongArgumentType.getLong(ctx, "amount")))))
                .then(Commands.m_82127_("team")
                        .executes(ctx -> act(ctx.getSource(), DifficultyActions.ACT_TEAM, 0))
                        .then(Commands.m_82129_("mode", StringArgumentType.word())
                                .executes(ctx -> setTeam(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
                .then(Commands.m_82127_("settings")
                        .requires(DifficultyCommands::isAdmin)
                        .executes(ctx -> openChat(ctx.getSource(), "settings"))
                        .then(Commands.m_82129_("key", StringArgumentType.word())
                                .then(Commands.m_82129_("value", StringArgumentType.greedyString())
                                        .executes(ctx -> adminSet(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "key"),
                                                StringArgumentType.getString(ctx, "value"))))))
                .then(Commands.m_82127_("reload")
                        .requires(DifficultyCommands::isAdmin)
                        .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.m_82127_("admin")
                        .requires(DifficultyCommands::isAdmin)
                        .executes(ctx -> adminHelp(ctx.getSource()))
                        .then(Commands.m_82127_("set")
                                .then(Commands.m_82129_("key", StringArgumentType.word())
                                        .then(Commands.m_82129_("value", StringArgumentType.greedyString())
                                                .executes(ctx -> adminSet(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "key"),
                                                        StringArgumentType.getString(ctx, "value")))))));

        event.getDispatcher().register(root);
        AdaptiveDifficultyMod.LOGGER.info("[{}] registered /difficulty", AdaptiveDifficultyMod.MOD_ID);
    }

    private static boolean isAdmin(CommandSourceStack src) {
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

    private static int openGui(CommandSourceStack source, String page) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            source.m_81352_(Component.m_237113_("Players only."));
            return 0;
        }
        DifficultyMenu.open(player, page);
        return 1;
    }

    private static int openChat(CommandSourceStack source, String page) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            source.m_81352_(Component.m_237113_("Players only."));
            return 0;
        }
        DifficultyChatMenu.open(player, page);
        return 1;
    }

    private static int act(CommandSourceStack source, String action, long amount) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return 0;
        }
        DifficultyActions.Result result = DifficultyActions.handle(player, action, amount, "main");
        result.tell(player);
        return result.ok() ? 1 : 0;
    }

    private static int show(CommandSourceStack source) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            source.m_81352_(Component.m_237113_("Players only."));
            return 0;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        String color = snap.stateColorCode();
        source.m_288197_(() -> Component.m_237113_(
                "§6=== Adaptive Difficulty §" + color + snap.active + "§6 ===\n"
                        + "§eActive: §f" + snap.active + "\n"
                        + "§eCalculated: §f" + snap.calculated + " §7(level " + snap.dmzLevel
                        + ", prestige " + snap.prestige + ")\n"
                        + "§ePurchased: §f" + snap.purchased + "\n"
                        + "§ePersonal Max: §f" + snap.personalMax + "\n"
                        + "§eTeam: §f" + TeamScaling.teamName(player)
                        + " §7(" + TeamScaling.teammates(player).size() + " online via "
                        + TeamScaling.teamSourceLabel() + ")\n"
                        + "§eTeam Threshold Bonus: §f" + snap.teamThresholdBonus + "\n"
                        + "§eTeam Contribution: §f" + snap.teamContribution + "\n"
                        + "§eAvailable Max: §f" + snap.availableMax + "\n"
                        + "§eTeam Mode: §f" + snap.teamMode + "\n"
                        + "§7Currency: §f" + CurrencyBridge.currencyLabel()
                        + " §8| Balance: §f" + CurrencyBridge.balanceText(player) + "\n"
                        + "§7Next +100 cost: §f" + CurrencyBridge.formatCost(
                        DifficultyCalculator.purchaseCost(snap.purchased, 100))
        ), false);
        return 1;
    }

    private static int setTeam(CommandSourceStack source, String mode) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return 0;
        }
        DifficultyActions.Result result = DifficultyActions.setTeam(player, TeamMode.fromString(mode), "main");
        result.tell(player);
        return result.ok() ? 1 : 0;
    }

    private static int reload(CommandSourceStack source) {
        DifficultyConfig.reload();
        DifficultyCache.invalidateAll();
        source.m_288197_(() -> Component.m_237113_("§aAdaptive difficulty config reloaded."), true);
        return 1;
    }

    private static int adminHelp(CommandSourceStack source) {
        source.m_288197_(() -> Component.m_237113_(
                "§6/difficulty admin set <key> <value>\n"
                        + "§7Keys: prestigeMultiplier, levelMultiplier, teamBonusPercent, contributionPercent,\n"
                        + "§7baseCost, costScaling, rewardScaling, health/damage/defense percents,\n"
                        + "§7purchaseCurrency (lightmans|training_points|free),\n"
                        + "§7enableElites, eliteChancePercent, enableMutations, mutationChancePercent,\n"
                        + "§7enableAdaptiveAi, enableBossScaling, bossStatMultiplier, bossHealthThreshold"
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
