package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyChatMenu;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty;
import com.dbzlegacy.adaptivedifficulty.world.VanillaDifficultyGuard;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Players: {@code /difficulty} opens the GUI.
 * GUI buttons use {@code /difficulty do ...} (not for normal player use).
 * Staff: {@code /difficulty admin} toggles config commands.
 * Ops can still set vanilla world difficulty via {@code /difficulty hard|normal|easy|peaceful}
 * (this mod replaces the vanilla {@code /difficulty} command name).
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
                // Player: reset active difficulty to 0 (free; purchased max kept)
                .then(Commands.m_82127_("reset")
                        .executes(ctx -> guiDo(ctx.getSource(), DifficultyActions.ACT_RESET, "0")))
                // Vanilla world difficulty (replaces overwritten /difficulty <level>)
                .then(vanillaDifficultyLiteral("peaceful"))
                .then(vanillaDifficultyLiteral("easy"))
                .then(vanillaDifficultyLiteral("normal"))
                .then(vanillaDifficultyLiteral("hard"))
                .then(Commands.m_82127_("admin")
                        .executes(ctx -> toggleAdmin(ctx.getSource()))
                        .then(Commands.m_82127_("help")
                                .executes(ctx -> adminHelpOrDeny(ctx.getSource())))
                        .then(Commands.m_82127_("reload")
                                .executes(ctx -> reloadOrDeny(ctx.getSource())))
                        .then(Commands.m_82127_("settings")
                                .executes(ctx -> openAdminSettingsOrDeny(ctx.getSource())))
                        .then(Commands.m_82127_("gamedifficulty")
                                .then(Commands.m_82129_("level", StringArgumentType.word())
                                        .executes(ctx -> setVanillaDifficultyOrDeny(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "level")))))
                        .then(Commands.m_82127_("area")
                                .executes(ctx -> showAreaDifficulty(ctx.getSource())))
                        .then(Commands.m_82127_("resetpurchased")
                                .executes(ctx -> adminResetPurchasedOrDeny(ctx.getSource())))
                        .then(Commands.m_82127_("set")
                                .then(Commands.m_82129_("key", StringArgumentType.word())
                                        .then(Commands.m_82129_("value", StringArgumentType.greedyString())
                                                .executes(ctx -> adminSetOrDeny(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "key"),
                                                        StringArgumentType.getString(ctx, "value")))))));

        event.getDispatcher().register(root);
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] registered /difficulty (GUI + vanilla hard/normal/easy/peaceful for ops)",
                AdaptiveDifficultyMod.MOD_ID
        );
    }

    private static LiteralArgumentBuilder<CommandSourceStack> vanillaDifficultyLiteral(String level) {
        return Commands.m_82127_(level)
                .requires(src -> src.m_6761_(2))
                .executes(ctx -> setVanillaDifficulty(ctx.getSource(), level));
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
        if (!isStaff(source)) {
            source.m_288197_(() -> Component.m_237113_(
                    "§cNo permission for /difficulty admin (need op or difficulty.admin)."
            ), false);
            return 0;
        }
        boolean enabled = AdminCommandAccess.toggle(player);
        if (enabled) {
            source.m_288197_(() -> Component.m_237113_(
                    "§aAdmin commands ENABLED.\n"
                            + "§7/difficulty admin help|reload|settings\n"
                            + "§7/difficulty admin gamedifficulty <peaceful|easy|normal|hard>\n"
                            + "§7/difficulty admin set <key> <value>\n"
                            + "§8Run §f/difficulty admin §8again to disable."
            ), false);
        } else {
            source.m_288197_(() -> Component.m_237113_("§cAdmin commands DISABLED."), false);
        }
        return 1;
    }

    private static int denyAdmin(CommandSourceStack source) {
        if (!isStaff(source)) {
            source.m_288197_(() -> Component.m_237113_(
                    "§cNo permission for /difficulty admin (need op or difficulty.admin)."
            ), false);
            return 0;
        }
        ServerPlayer player = source.m_230896_();
        if (player != null && !AdminCommandAccess.isEnabled(player)) {
            source.m_288197_(() -> Component.m_237113_(
                    "§cEnable admin mode first: §f/difficulty admin"
            ), false);
            return 0;
        }
        return 1;
    }

    private static int adminHelpOrDeny(CommandSourceStack source) {
        return denyAdmin(source) == 0 ? 0 : adminHelp(source);
    }

    private static int reloadOrDeny(CommandSourceStack source) {
        return denyAdmin(source) == 0 ? 0 : reload(source);
    }

    private static int openAdminSettingsOrDeny(CommandSourceStack source) {
        return denyAdmin(source) == 0 ? 0 : openAdminSettings(source);
    }

    private static int setVanillaDifficultyOrDeny(CommandSourceStack source, String level) {
        return denyAdmin(source) == 0 ? 0 : setVanillaDifficulty(source, level);
    }

    private static int adminSetOrDeny(CommandSourceStack source, String key, String value) {
        return denyAdmin(source) == 0 ? 0 : adminSet(source, key, value);
    }

    private static int setVanillaDifficulty(CommandSourceStack source, String level) {
        Difficulty difficulty = VanillaDifficultyGuard.parse(level);
        if (difficulty == null) {
            source.m_81352_(Component.m_237113_("Unknown vanilla difficulty: " + level
                    + " (use peaceful|easy|normal|hard)"));
            return 0;
        }
        if (!VanillaDifficultyGuard.set(source.m_81377_(), difficulty)) {
            source.m_81352_(Component.m_237113_("Failed to set vanilla difficulty."));
            return 0;
        }
        if (difficulty == Difficulty.PEACEFUL) {
            source.m_288197_(() -> Component.m_237113_(
                    "§cVanilla difficulty set to PEACEFUL.\n"
                            + "§7Hostile mobs will not spawn — adaptive scaling will not run.\n"
                            + "§eUse §f/difficulty hard §eto restore."
            ), true);
        } else {
            source.m_288197_(() -> Component.m_237113_(
                    "§aVanilla difficulty set to §f" + difficulty.m_19036_()
            ), true);
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

    /** Scaling Health-style area difficulty readout (`sh_difficulty get`). */
    private static int showAreaDifficulty(CommandSourceStack source) {
        if (denyAdmin(source) == 0) {
            return 0;
        }
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            source.m_81352_(Component.m_237113_("Players only."));
            return 0;
        }
        ServerLevel level = player.m_284548_(); // serverLevel / getLevel
        if (level == null) {
            level = player.m_9236_() instanceof ServerLevel sl ? sl : null;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        long area = level == null ? 0L : AreaDifficulty.at(level, player.m_20183_());
        DifficultyConfig cfg = DifficultyConfig.get();
        ServerLevel finalLevel = level;
        source.m_288197_(() -> Component.m_237113_(
                "§6Area Difficulty §8(Scaling Health-style)\n"
                        + "§ePlayer active: §f" + snap.active + " §7/ max §f" + snap.availableMax + "\n"
                        + "§eArea at you: §f" + area + "\n"
                        + "§eMode: §f" + cfg.areaDifficultyMode
                        + " §8| §eradius §f" + cfg.mobScaleRadius
                        + " §8| §egroupBonus% §f" + cfg.areaGroupBonusPercent
                        + (finalLevel == null ? "\n§cNo server level" : "")
        ), false);
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
                        + "§e/difficulty §7— open player GUI (CMI / chest / chat)\n"
                        + "§e/difficulty reset §7— set your active difficulty to 0 (free)\n"
                        + "§e/difficulty hard|normal|easy|peaceful §7— vanilla world difficulty (ops)\n"
                        + "§e/difficulty admin §7— toggle admin command access\n"
                        + "§e/difficulty admin reload|settings|area|gamedifficulty|resetpurchased\n"
                        + "§e/difficulty admin set <key> <value>\n"
                        + "§8areaDifficultyMode=weighted|average|max (Scaling Health-style)\n"
                        + "§8tierAwakened|tierEnhanced|…|tierImpossible — ability unlock thresholds\n"
                        + "§8movement|dmzExtraHealth|dmzExtraDamage|dmzExtraDefense|dmzExtraKiDamage"
        ), false);
        return 1;
    }

    private static int adminResetPurchasedOrDeny(CommandSourceStack source) {
        if (denyAdmin(source) == 0) {
            return 0;
        }
        ServerPlayer player = source.m_230896_();
        if (player == null) {
            source.m_81352_(Component.m_237113_("Players only."));
            return 0;
        }
        var data = DifficultyCache.data(player);
        data.setPurchasedDifficulty(0L);
        data.setActiveDifficulty(0L);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        AreaDifficulty.clearCache();
        source.m_288197_(() -> Component.m_237113_(
                "§aReset purchased + active difficulty to 0 for yourself."
        ), true);
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
                case "movement", "movementpercentper100difficulty" ->
                        cfg.movementPercentPer100Difficulty = Double.parseDouble(value);
                case "dmzextrahealth", "dmzextrahealthpercent" -> cfg.dmzExtraHealthPercent = Double.parseDouble(value);
                case "dmzextradamage", "dmzextradamagepercent" -> cfg.dmzExtraDamagePercent = Double.parseDouble(value);
                case "dmzextradefense", "dmzextradefensepercent" -> cfg.dmzExtraDefensePercent = Double.parseDouble(value);
                case "dmzextrakidamage", "dmzextrakidamagepercent" ->
                        cfg.dmzExtraKiDamagePercent = Double.parseDouble(value);
                case "tierawakened" -> cfg.tierAwakened = Long.parseLong(value);
                case "tierenhanced" -> cfg.tierEnhanced = Long.parseLong(value);
                case "tierelite" -> cfg.tierElite = Long.parseLong(value);
                case "tieradvanced" -> cfg.tierAdvanced = Long.parseLong(value);
                case "tiermaster" -> cfg.tierMaster = Long.parseLong(value);
                case "tierlegendary" -> cfg.tierLegendary = Long.parseLong(value);
                case "tiergod" -> cfg.tierGod = Long.parseLong(value);
                case "tierdivine" -> cfg.tierDivine = Long.parseLong(value);
                case "tierimpossible" -> cfg.tierImpossible = Long.parseLong(value);
                case "hardcap", "hardcapdifficulty" -> cfg.hardCapDifficulty = Long.parseLong(value);
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
                case "guibackend" -> cfg.guiBackend = value.trim().toLowerCase();
                case "vanilladifficulty" -> cfg.vanillaDifficulty = value.trim().toLowerCase();
                case "restorevanilladifficultyfrompeaceful" ->
                        cfg.restoreVanillaDifficultyFromPeaceful = Boolean.valueOf(value);
                case "areadifficultymode" -> cfg.areaDifficultyMode = value.trim().toLowerCase();
                case "areagroupbonuspercent" -> cfg.areaGroupBonusPercent = Double.parseDouble(value);
                case "areadifficultyvariancepercent" ->
                        cfg.areaDifficultyVariancePercent = Double.parseDouble(value);
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
