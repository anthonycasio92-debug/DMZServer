package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
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
                .executes(ctx -> show(ctx.getSource()))
                .then(Commands.m_82127_("up")
                        .executes(ctx -> adjust(ctx.getSource(), 100))
                        .then(Commands.m_82129_("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> adjust(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))))
                .then(Commands.m_82127_("down")
                        .executes(ctx -> adjust(ctx.getSource(), -100))
                        .then(Commands.m_82129_("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> adjust(ctx.getSource(), -LongArgumentType.getLong(ctx, "amount")))))
                .then(Commands.m_82127_("set")
                        .then(Commands.m_82129_("amount", LongArgumentType.longArg(0))
                                .executes(ctx -> setActive(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))))
                .then(Commands.m_82127_("buy")
                        .then(Commands.m_82129_("amount", LongArgumentType.longArg(1))
                                .executes(ctx -> buy(ctx.getSource(), LongArgumentType.getLong(ctx, "amount")))))
                .then(Commands.m_82127_("team")
                        .executes(ctx -> cycleTeam(ctx.getSource()))
                        .then(Commands.m_82129_("mode", StringArgumentType.word())
                                .executes(ctx -> setTeam(ctx.getSource(), StringArgumentType.getString(ctx, "mode")))))
                .then(Commands.m_82127_("reload")
                        .requires(src -> src.m_6761_(2))
                        .executes(ctx -> reload(ctx.getSource())))
                .then(Commands.m_82127_("admin")
                        .requires(src -> src.m_6761_(2))
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
                        + "§eTeam Threshold Bonus: §f" + snap.teamThresholdBonus + "\n"
                        + "§eTeam Contribution: §f" + snap.teamContribution + "\n"
                        + "§eAvailable Max: §f" + snap.availableMax + "\n"
                        + "§eTeam Mode: §f" + snap.teamMode + "\n"
                        + "§7Currency: §f" + CurrencyBridge.currencyLabel()
                        + " §8| next +100 cost ~ §f"
                        + DifficultyCalculator.purchaseCost(snap.purchased, 100) + "\n"
                        + "§7/difficulty up|down [amount]  /difficulty buy <amount>\n"
                        + "§7/difficulty team [personal|threshold|full]"
        ), false);
        return 1;
    }

    private static int adjust(CommandSourceStack source, long delta) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return 0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        DifficultySnapshot before = DifficultyCache.refresh(player);
        long next = Math.max(0L, before.active + delta);
        next = Math.min(next, before.availableMax);
        data.setActiveDifficulty(next);
        DifficultyCache.save(player);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        source.m_288197_(() -> Component.m_237113_("§aActive difficulty set to §f" + snap.active
                + " §7(max " + snap.availableMax + ")"), false);
        return 1;
    }

    private static int setActive(CommandSourceStack source, long amount) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return 0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        DifficultySnapshot bounds = DifficultyCache.refresh(player);
        long next = Math.max(0L, Math.min(amount, bounds.availableMax));
        data.setActiveDifficulty(next);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        source.m_288197_(() -> Component.m_237113_("§aActive difficulty set to §f" + next), false);
        return 1;
    }

    private static int buy(CommandSourceStack source, long amount) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return 0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        long cost = DifficultyCalculator.purchaseCost(data.getPurchasedDifficulty(), amount);
        if (!CurrencyBridge.canAfford(player, cost)) {
            source.m_81352_(Component.m_237113_("§cNeed " + cost + " " + CurrencyBridge.currencyLabel() + "."));
            return 0;
        }
        if (!CurrencyBridge.charge(player, cost)) {
            source.m_81352_(Component.m_237113_("§cPayment failed."));
            return 0;
        }
        data.setPurchasedDifficulty(data.getPurchasedDifficulty() + amount);
        DifficultyCache.save(player);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        source.m_288197_(() -> Component.m_237113_(
                "§aPurchased §f+" + amount + " §adifficulty for §f" + cost + " §a"
                        + CurrencyBridge.currencyLabel() + ".\n§ePurchased total: §f" + snap.purchased
                        + " §7| Available max: §f" + snap.availableMax
        ), false);
        return 1;
    }

    private static int cycleTeam(CommandSourceStack source) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return 0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.cycleTeamMode();
        DifficultyCache.save(player);
        DifficultyCache.invalidateAll();
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        source.m_288197_(() -> Component.m_237113_("§aTeam mode: §f" + snap.teamMode), false);
        return 1;
    }

    private static int setTeam(CommandSourceStack source, String mode) {
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            return 0;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.setTeamMode(TeamMode.fromString(mode));
        DifficultyCache.save(player);
        DifficultyCache.invalidateAll();
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        source.m_288197_(() -> Component.m_237113_("§aTeam mode: §f" + snap.teamMode), false);
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
                "§6/difficulty admin set <key> <value>\n"
                        + "§7Keys: prestigeMultiplier, levelMultiplier, teamBonusPercent, contributionPercent,\n"
                        + "§7baseCost, costScaling, rewardScaling, healthPercentPerDifficulty,\n"
                        + "§7damagePercentPerDifficulty, defensePercentPerDifficulty, mobScaleRadius,\n"
                        + "§7purchaseCurrency (training_points|free|lightmans), enableMobScaling, enableRewardScaling"
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
