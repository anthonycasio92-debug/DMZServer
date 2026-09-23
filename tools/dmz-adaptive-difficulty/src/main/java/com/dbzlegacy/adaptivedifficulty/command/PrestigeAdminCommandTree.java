package com.dbzlegacy.adaptivedifficulty.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

/** Shared Brigadier tree for {@code /prestige admin …} and {@code /padmin …}. */
public final class PrestigeAdminCommandTree {
    private PrestigeAdminCommandTree() {}

    public static LiteralArgumentBuilder<CommandSourceStack> attach(
            LiteralArgumentBuilder<CommandSourceStack> root
    ) {
        return root
                .executes(ctx -> ProgressionCommands.prestigeAdminHelp(ctx.getSource()))
                .then(Commands.m_82127_("help").executes(ctx -> ProgressionCommands.prestigeAdminHelp(ctx.getSource())))
                .then(Commands.m_82127_("info")
                        .executes(ctx -> ProgressionCommands.prestigeAdminInfo(ctx.getSource(), null))
                        .then(LmCommandSuggestions.playerWord("player")
                                .executes(ctx -> ProgressionCommands.prestigeAdminInfo(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("sync")
                        .then(LmCommandSuggestions.playerWord("player")
                                .executes(ctx -> ProgressionCommands.prestigeAdminSync(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("skills")
                        .then(LmCommandSuggestions.playerWord("player")
                                .executes(ctx -> ProgressionCommands.prestigeAdminListSkills(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("invested")
                        .then(LmCommandSuggestions.playerWord("player")
                                .executes(ctx -> ProgressionCommands.prestigeAdminListSkills(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))))
                .then(Commands.m_82127_("skill")
                        .then(LmCommandSuggestions.playerWord("player")
                                .executes(ctx -> ProgressionCommands.prestigeAdminListSkills(
                                        ctx.getSource(),
                                        StringArgumentType.getString(ctx, "player")))
                                .then(Commands.m_82129_("skillId", StringArgumentType.word())
                                        .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_ADJUST_MODES)
                                                .then(Commands.m_82129_("levels", IntegerArgumentType.integer())
                                                        .executes(ctx -> ProgressionCommands.prestigeAdminSkill(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "skillId"),
                                                                StringArgumentType.getString(ctx, "mode"),
                                                                IntegerArgumentType.getInteger(ctx, "levels"))))))))
                .then(Commands.m_82127_("invest")
                        .then(LmCommandSuggestions.playerWord("player")
                                .then(Commands.m_82129_("skillId", StringArgumentType.word())
                                        .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_ADJUST_MODES)
                                                .then(Commands.m_82129_("levels", IntegerArgumentType.integer())
                                                        .executes(ctx -> ProgressionCommands.prestigeAdminSkill(
                                                                ctx.getSource(),
                                                                StringArgumentType.getString(ctx, "player"),
                                                                StringArgumentType.getString(ctx, "skillId"),
                                                                StringArgumentType.getString(ctx, "mode"),
                                                                IntegerArgumentType.getInteger(ctx, "levels"))))))))
                .then(Commands.m_82127_("tier")
                        .then(LmCommandSuggestions.playerWord("player")
                                .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_TIER_MODES)
                                        .then(LmCommandSuggestions.word("tier", LmCommandSuggestions.PRESTIGE_TIER_IDS)
                                                .executes(ctx -> ProgressionCommands.prestigeAdminTier(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "mode"),
                                                        StringArgumentType.getString(ctx, "tier")))))))
                .then(Commands.m_82127_("tiers")
                        .then(LmCommandSuggestions.playerWord("player")
                                .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_TIER_MODES)
                                        .then(LmCommandSuggestions.word("tier", LmCommandSuggestions.PRESTIGE_TIER_IDS)
                                                .executes(ctx -> ProgressionCommands.prestigeAdminTier(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "mode"),
                                                        StringArgumentType.getString(ctx, "tier")))))))
                .then(Commands.m_82127_("difficulty")
                        .then(LmCommandSuggestions.playerWord("player")
                                .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_TIER_MODES)
                                        .then(LmCommandSuggestions.word("tier", LmCommandSuggestions.PRESTIGE_TIER_IDS)
                                                .executes(ctx -> ProgressionCommands.prestigeAdminTier(
                                                        ctx.getSource(),
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "mode"),
                                                        StringArgumentType.getString(ctx, "tier")))))))
                .then(adjustField("held"))
                .then(adjustField("completed"))
                .then(adjustField("points"))
                .then(adjustField("breakthroughs"))
                .then(Commands.m_82127_("breakthrough")
                        .then(LmCommandSuggestions.playerWord("player")
                                .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_ADJUST_MODES)
                                        .then(Commands.m_82129_("amount", IntegerArgumentType.integer())
                                                .executes(ctx -> ProgressionCommands.prestigeAdminAdjust(
                                                        ctx.getSource(), "breakthroughs",
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "mode"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.m_82127_("cap")
                        .then(LmCommandSuggestions.playerWord("player")
                                .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_ADJUST_MODES)
                                        .then(Commands.m_82129_("amount", IntegerArgumentType.integer())
                                                .executes(ctx -> ProgressionCommands.prestigeAdminAdjust(
                                                        ctx.getSource(), "breakthroughs",
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "mode"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(Commands.m_82127_("fabled")
                        .then(LmCommandSuggestions.playerWord("player")
                                .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_FABLED_MODES)
                                        .then(Commands.m_82129_("amount", IntegerArgumentType.integer())
                                                .executes(ctx -> ProgressionCommands.prestigeAdminAdjust(
                                                        ctx.getSource(), "fabled",
                                                        StringArgumentType.getString(ctx, "player"),
                                                        StringArgumentType.getString(ctx, "mode"),
                                                        IntegerArgumentType.getInteger(ctx, "amount")))))))
                .then(pointsShorthand("addpoints"))
                .then(pointsShorthand("givepoints"))
                .then(pointsShorthand("grantpoints"))
                .then(pointsShorthand("setpoints"))
                .then(pointsShorthand("removepoints"))
                .then(pointsShorthand("takepoints"));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> adjustField(String field) {
        return Commands.m_82127_(field)
                .then(LmCommandSuggestions.word("selfMode", LmCommandSuggestions.PRESTIGE_ADJUST_MODES)
                        .then(Commands.m_82129_("amount", IntegerArgumentType.integer())
                                .executes(ctx -> ProgressionCommands.prestigeAdminViaApi(
                                        ctx.getSource(),
                                        field + " "
                                                + StringArgumentType.getString(ctx, "selfMode") + " "
                                                + IntegerArgumentType.getInteger(ctx, "amount")))))
                .then(LmCommandSuggestions.playerWord("player")
                        .then(LmCommandSuggestions.word("mode", LmCommandSuggestions.PRESTIGE_ADJUST_MODES)
                                .then(Commands.m_82129_("amount", IntegerArgumentType.integer())
                                        .executes(ctx -> ProgressionCommands.prestigeAdminAdjust(
                                                ctx.getSource(), field,
                                                StringArgumentType.getString(ctx, "player"),
                                                StringArgumentType.getString(ctx, "mode"),
                                                IntegerArgumentType.getInteger(ctx, "amount")))))
                        .then(Commands.m_82129_("amount", IntegerArgumentType.integer())
                                .executes(ctx -> ProgressionCommands.prestigeAdminViaApi(
                                        ctx.getSource(),
                                        field + " "
                                                + StringArgumentType.getString(ctx, "player") + " "
                                                + IntegerArgumentType.getInteger(ctx, "amount")))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> pointsShorthand(String name) {
        return Commands.m_82127_(name)
                .then(LmCommandSuggestions.playerWord("player")
                        .then(Commands.m_82129_("amount", IntegerArgumentType.integer())
                                .executes(ctx -> ProgressionCommands.prestigeAdminViaApi(
                                        ctx.getSource(),
                                        name + " "
                                                + StringArgumentType.getString(ctx, "player") + " "
                                                + IntegerArgumentType.getInteger(ctx, "amount")))));
    }
}
