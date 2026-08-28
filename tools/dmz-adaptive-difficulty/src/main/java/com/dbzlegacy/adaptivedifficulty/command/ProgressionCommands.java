package com.dbzlegacy.adaptivedifficulty.command;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.PrestigeMenu;
import com.dbzlegacy.adaptivedifficulty.gui.ProgressionMenu;
import com.dbzlegacy.adaptivedifficulty.gui.SkillsMenu;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionSystem;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/** {@code /progression} / {@code /prog} — staff natural progression controls. */
public final class ProgressionCommands {
    private ProgressionCommands() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.register(new ProgressionCommands());
    }

    @SubscribeEvent
    public void onRegister(RegisterCommandsEvent event) {
        LiteralArgumentBuilder<CommandSourceStack> root = build("progression");
        event.getDispatcher().register(root);
        event.getDispatcher().register(build("prog"));

        // Prestige NPC purchase GUI — staff only
        event.getDispatcher().register(Commands.m_82127_("prestige")
                .requires(ProgressionCommands::staff)
                .executes(ctx -> prestigeGui(ctx.getSource(), "main"))
                .then(Commands.m_82127_("gui").executes(ctx -> prestigeGui(ctx.getSource(), "main")))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("confirm")
                                .executes(ctx -> prestigeConfirm(ctx.getSource(), "main"))
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> prestigeConfirm(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page")))))
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> prestigeGui(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page")))))));

        // SkillUnlock admin browser — staff only (not aliased to skillcheck)
        event.getDispatcher().register(Commands.m_82127_("skills")
                .requires(ProgressionCommands::staff)
                .executes(ctx -> skillsPage(ctx.getSource(), "core"))
                .then(Commands.m_82127_("gui").executes(ctx -> skillsPage(ctx.getSource(), "core")))
                .then(Commands.m_82127_("check").executes(ctx -> skillsPage(ctx.getSource(), "core")))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> skillsPage(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page")))))));

        // Donator Skill Check
        event.getDispatcher().register(Commands.m_82127_("skillcheck")
                .requires(ProgressionCommands::skillCheck)
                .executes(ctx -> skillCheckPage(ctx.getSource(), "core"))
                .then(Commands.m_82127_("gui").executes(ctx -> skillCheckPage(ctx.getSource(), "core")))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> skillCheckPage(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page"))))))
                .then(Commands.m_82129_("page", StringArgumentType.word())
                        .executes(ctx -> skillCheckPage(
                                ctx.getSource(),
                                StringArgumentType.getString(ctx, "page")))));

        // End Dimension Strength triggers 50/51
        event.getDispatcher().register(Commands.m_82127_("enddragon")
                .requires(src -> src.m_6761_(2))
                .executes(ctx -> endSpawn(ctx.getSource()))
                .then(Commands.m_82127_("spawn").executes(ctx -> endSpawn(ctx.getSource())))
                .then(Commands.m_82127_("clear").executes(ctx -> endClear(ctx.getSource())))
                .then(Commands.m_82127_("cleanup").executes(ctx -> endClear(ctx.getSource()))));
        event.getDispatcher().register(Commands.m_82127_("cleardragons")
                .requires(src -> src.m_6761_(2))
                .executes(ctx -> endClear(ctx.getSource())));

        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] registered /progression /prog /prestige /skills /skillcheck /enddragon",
                AdaptiveDifficultyMod.MOD_ID
        );
    }

    private static int skillsPage(CommandSourceStack source, String page) {
        ServerPlayer p = playerOrNull(source);
        if (p == null) {
            return 0;
        }
        if (!StaffAccess.isStaff(p)) {
            reply(source, p, "§cStaff only.");
            return 0;
        }
        SkillsMenu.open(p, page);
        return 1;
    }

    private static int skillCheckPage(CommandSourceStack source, String page) {
        ServerPlayer p = playerOrNull(source);
        if (p == null) {
            return 0;
        }
        if (!SkillCheckService.canUse(p)) {
            reply(source, p, "§cNo permission: legacymechanics.skillcheck");
            return 0;
        }
        SkillCheckService.open(p, page);
        return 1;
    }

    private static int prestigeGui(CommandSourceStack source, String page) {
        ServerPlayer p = playerOrNull(source);
        if (p == null) {
            return 0;
        }
        if (!StaffAccess.isStaff(p)) {
            reply(source, p, "§cStaff only.");
            return 0;
        }
        PrestigeMenu.open(p, page);
        return 1;
    }

    private static int prestigeConfirm(CommandSourceStack source, String page) {
        ServerPlayer p = playerOrNull(source);
        if (p == null) {
            return 0;
        }
        if (!StaffAccess.isStaff(p)) {
            reply(source, p, "§cStaff only.");
            return 0;
        }
        com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem.confirmOrPrompt(p);
        PrestigeMenu.open(p, page == null || page.isBlank() ? "main" : page);
        return 1;
    }

    private static int endSpawn(CommandSourceStack source) {
        ServerPlayer p = playerOrNull(source);
        if (p == null) {
            return 0;
        }
        return com.dbzlegacy.adaptivedifficulty.progression.end.EndDimensionStrength.cmdSpawnDragon(p);
    }

    private static int endClear(CommandSourceStack source) {
        return com.dbzlegacy.adaptivedifficulty.progression.end.EndDimensionStrength.cmdCleanupDragons(
                playerOrNull(source));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> build(String name) {
        return Commands.m_82127_(name)
                .requires(ProgressionCommands::staff)
                .executes(ctx -> gui(ctx.getSource(), "main"))
                .then(Commands.m_82127_("gui").executes(ctx -> gui(ctx.getSource(), "main")))
                .then(Commands.m_82127_("help").executes(ctx -> gui(ctx.getSource(), "help")))
                .then(Commands.m_82127_("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.m_82127_("do")
                        .then(Commands.m_82127_("page")
                                .then(Commands.m_82129_("page", StringArgumentType.word())
                                        .executes(ctx -> gui(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "page")))))
                        .then(Commands.m_82127_("flag")
                                .then(Commands.m_82129_("flag", StringArgumentType.word())
                                        .then(Commands.m_82129_("page", StringArgumentType.word())
                                                .executes(ctx -> flagDo(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "flag"),
                                                        StringArgumentType.getString(ctx, "page")))))))
                .then(Commands.m_82127_("boost")
                        .requires(ProgressionCommands::staff)
                        .executes(ctx -> boostStatus(ctx.getSource()))
                        .then(Commands.m_82127_("status").executes(ctx -> boostStatus(ctx.getSource())))
                        .then(Commands.m_82127_("help").executes(ctx -> boostStatus(ctx.getSource())))
                        .then(Commands.m_82127_("start")
                                .requires(ProgressionCommands::staff)
                                .then(Commands.m_82129_("encoded", IntegerArgumentType.integer(1))
                                        .executes(ctx -> boostEncoded(
                                                ctx.getSource(),
                                                IntegerArgumentType.getInteger(ctx, "encoded"),
                                                null))
                                        .then(Commands.m_82129_("purchaser", StringArgumentType.greedyString())
                                                .executes(ctx -> boostEncoded(
                                                        ctx.getSource(),
                                                        IntegerArgumentType.getInteger(ctx, "encoded"),
                                                        StringArgumentType.getString(ctx, "purchaser")))))
                                .then(Commands.m_82129_("multiplier", DoubleArgumentType.doubleArg(1.25))
                                        .then(Commands.m_82129_("minutes", IntegerArgumentType.integer(1))
                                                .executes(ctx -> boostMinutes(
                                                        ctx.getSource(),
                                                        DoubleArgumentType.getDouble(ctx, "multiplier"),
                                                        IntegerArgumentType.getInteger(ctx, "minutes"),
                                                        null)))))
                        .then(Commands.m_82127_("end")
                                .requires(ProgressionCommands::staff)
                                .executes(ctx -> boostEnd(ctx.getSource()))))
                .then(Commands.m_82127_("meditation")
                        .executes(ctx -> meditationStatus(ctx.getSource()))
                        .then(Commands.m_82127_("status").executes(ctx -> meditationStatus(ctx.getSource())))
                        .then(Commands.m_82127_("help").executes(ctx -> meditationStatus(ctx.getSource())))
                        .then(Commands.m_82127_("next")
                                .requires(ProgressionCommands::staff)
                                .executes(ctx -> meditationNext(ctx.getSource()))))
                .then(Commands.m_82127_("android")
                        .requires(ProgressionCommands::staff)
                        .executes(ctx -> androidSelf(ctx.getSource()))
                        .then(Commands.m_82129_("player", StringArgumentType.word())
                                .executes(ctx -> androidPlayer(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("admin")
                        .requires(ProgressionCommands::staff)
                        .executes(ctx -> gui(ctx.getSource(), "admin"))
                        .then(Commands.m_82129_("flag", StringArgumentType.word())
                                .then(Commands.m_82129_("value", StringArgumentType.word())
                                        .executes(ctx -> adminToggle(
                                                ctx.getSource(),
                                                StringArgumentType.getString(ctx, "flag"),
                                                StringArgumentType.getString(ctx, "value"))))));
    }

    private static boolean staff(CommandSourceStack src) {
        try {
            if (src.m_6761_(2)) {
                return true;
            }
            ServerPlayer p = src.m_81375_();
            return StaffAccess.isStaff(p);
        } catch (Exception e) {
            return src.m_6761_(2);
        }
    }

    private static boolean skillCheck(CommandSourceStack src) {
        try {
            if (src.m_6761_(2)) {
                return true;
            }
            ServerPlayer p = src.m_81375_();
            return SkillCheckService.canUse(p);
        } catch (Exception e) {
            return src.m_6761_(2);
        }
    }

    private static int gui(CommandSourceStack source, String page) {
        ServerPlayer player = playerOrNull(source);
        if (player == null || !DifficultyConfig.get().enableProgression) {
            return 0;
        }
        if (!StaffAccess.isStaff(player)) {
            reply(source, player, "§cStaff only.");
            return 0;
        }
        ProgressionMenu.open(player, page);
        return 1;
    }

    private static int flagDo(CommandSourceStack source, String flag, String page) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            return 0;
        }
        if (!staff(source)) {
            reply(source, player, "§cStaff only.");
            return 0;
        }
        String msg = com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi.handleDo(
                player, "flag", flag, page);
        if (msg != null && !msg.isBlank()) {
            reply(source, player, msg);
        }
        ProgressionMenu.open(player, page == null || page.isBlank() ? "admin" : page);
        return 1;
    }

    private static int status(CommandSourceStack source) {
        String summary = ProgressionSystem.statusSummary();
        source.m_288197_(() -> Component.m_237113_("§7" + summary.replace("\n", "\n§7")), true);
        return 1;
    }

    private static int boostEncoded(CommandSourceStack source, int encoded, String purchaser) {
        ServerPlayer actor = playerOrNull(source);
        String msg = ProgressionSystem.boostStartEncoded(actor, encoded, purchaser);
        reply(source, actor, msg);
        return 1;
    }

    private static int boostMinutes(CommandSourceStack source, double mult, int minutes, String purchaser) {
        ServerPlayer actor = playerOrNull(source);
        String msg = ProgressionSystem.boostStart(actor, mult, minutes, purchaser);
        reply(source, actor, msg);
        return 1;
    }

    private static int boostEnd(CommandSourceStack source) {
        String msg = ProgressionSystem.boostEnd();
        reply(source, playerOrNull(source), msg);
        return 1;
    }

    private static int boostStatus(CommandSourceStack source) {
        ServerPlayer actor = playerOrNull(source);
        String msg = com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi.boost(actor, "status");
        for (String line : msg.split("\n")) {
            reply(source, actor, line);
        }
        return 1;
    }

    private static int meditationStatus(CommandSourceStack source) {
        ServerPlayer actor = playerOrNull(source);
        boolean staff = staff(source);
        String msg = com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression
                .explainTrials(staff);
        for (String line : msg.split("\n")) {
            reply(source, actor, line);
        }
        return 1;
    }

    private static int meditationNext(CommandSourceStack source) {
        ServerPlayer actor = playerOrNull(source);
        String msg = ProgressionSystem.meditationNext(actor);
        reply(source, actor, msg);
        // Also print explanation so staff see how trials work after a rotate.
        boolean staff = staff(source);
        for (String line : com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression
                .explainTrials(staff).split("\n")) {
            reply(source, actor, line);
        }
        return 1;
    }

    private static int androidSelf(CommandSourceStack source) {
        ServerPlayer player = playerOrNull(source);
        if (player == null) {
            source.m_288197_(() -> Component.m_237113_("§cPlayer required."), false);
            return 0;
        }
        reply(source, player, ProgressionSystem.androidConvert(player));
        return 1;
    }

    private static int androidPlayer(CommandSourceStack source, String name) {
        ServerPlayer target = resolve(source, name);
        if (target == null) {
            source.m_288197_(() -> Component.m_237113_("§cPlayer not found: " + name), false);
            return 0;
        }
        reply(source, playerOrNull(source), ProgressionSystem.androidConvert(target));
        return 1;
    }

    private static int adminToggle(CommandSourceStack source, String flag, String value) {
        boolean on = "on".equalsIgnoreCase(value) || "true".equalsIgnoreCase(value) || "1".equals(value);
        boolean off = "off".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value) || "0".equals(value);
        if (!on && !off) {
            source.m_288197_(() -> Component.m_237113_("§cUse on|off."), false);
            return 0;
        }
        if (!ProgressionSystem.setFlag(flag, on)) {
            source.m_288197_(() -> Component.m_237113_("§cUnknown flag: " + flag), false);
            return 0;
        }
        String msg = "§aProgression §f" + flag + " §7→ §f" + (on ? "ON" : "OFF");
        reply(source, playerOrNull(source), msg);
        ServerPlayer p = playerOrNull(source);
        if (p != null) {
            ProgressionMenu.open(p, "admin");
        }
        return 1;
    }

    private static void reply(CommandSourceStack source, ServerPlayer player, String msg) {
        if (player != null) {
            DmzRewards.msg(player, msg);
        } else {
            source.m_288197_(() -> Component.m_237113_(msg), true);
        }
    }

    private static ServerPlayer playerOrNull(CommandSourceStack source) {
        try {
            return source.m_81375_();
        } catch (Exception e) {
            return null;
        }
    }

    private static ServerPlayer resolve(CommandSourceStack source, String name) {
        try {
            MinecraftLookup:
            {
                var server = source.m_81377_();
                if (server == null) {
                    break MinecraftLookup;
                }
                ServerPlayer p = server.m_6846_().m_11255_(name);
                if (p != null) {
                    return p;
                }
                for (ServerPlayer online : server.m_6846_().m_11314_()) {
                    if (online.m_7755_().getString().equalsIgnoreCase(name)) {
                        return online;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }
}
