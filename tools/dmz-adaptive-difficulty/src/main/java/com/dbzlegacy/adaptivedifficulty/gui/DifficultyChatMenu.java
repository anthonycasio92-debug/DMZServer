package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import java.util.List;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * Clickable chat panel. Buttons call {@code /difficulty do ...} (not listed as player commands).
 */
public final class DifficultyChatMenu {
    private DifficultyChatMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page)) {
            main(player);
        } else if ("rewards".equalsIgnoreCase(page)) {
            rewards(player);
        } else if ("tiers".equalsIgnoreCase(page) || "enemies".equalsIgnoreCase(page)) {
            tiers(player);
        } else if ("stats".equalsIgnoreCase(page) || "statistics".equalsIgnoreCase(page)) {
            stats(player);
        } else if ("settings".equalsIgnoreCase(page)) {
            settings(player);
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        String color = snap.stateColorCode();
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8§m                                        "));
        send(player, Component.m_237113_("§6§l   Adaptive Difficulty §" + color + "§l" + snap.active));
        send(player, Component.m_237113_("§8§m                                        "));
        send(player, Component.m_237113_("§eActive: §f" + snap.active + " §7/ max §f" + snap.availableMax));
        send(player, Component.m_237113_("§eCalculated: §f" + snap.calculated
                + " §7(Lv " + snap.dmzLevel + " · Prestige " + snap.prestige + ")"));
        send(player, Component.m_237113_("§ePurchased: §f" + snap.purchased
                + " §8| §ePersonal Max: §f" + snap.personalMax));
        send(player, Component.m_237113_("§eTeam Bonus: §f" + snap.teamThresholdBonus
                + " §8| §eContribution: §f" + snap.teamContribution));
        send(player, Component.m_237113_("§eTeam: §f" + TeamScaling.teamName(player)
                + " §8(" + TeamScaling.teammates(player).size() + " online via " + TeamScaling.teamSourceLabel() + ")"));
        send(player, Component.m_237113_("§eTeam Mode: §f" + snap.teamMode
                + " §8| §eBalance: §f" + CurrencyBridge.balanceText(player)));
        send(player, Component.m_237113_("§eEnemy Tier: §f" + DifficultyTier.of(snap.active).display
                + " §8| §eState: §" + color + snap.state()));
        send(player, Component.m_237113_(""));

        MutableComponent controls = Component.m_237113_("§7Controls: ")
                .m_7220_(btn("§a▲ +100", "/difficulty do up 100", "Raise active difficulty"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§c▼ -100", "/difficulty do down 100", "Lower active difficulty (free)"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§eMax", "/difficulty do set_max 0", "Set active to available max"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§cReset", "/difficulty do reset 0", "Set active difficulty to 0 (free)"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§bTeam", "/difficulty do team 0", "Cycle team scaling mode"));
        send(player, controls);

        long buy100 = DifficultyCalculator.purchaseCost(snap.purchased, 100);
        long buy1k = DifficultyCalculator.purchaseCost(snap.purchased, 1_000);
        long buy10k = DifficultyCalculator.purchaseCost(snap.purchased, 10_000);
        MutableComponent buy = Component.m_237113_("§7Buy: ")
                .m_7220_(btn("§6+100 §8(" + CurrencyBridge.formatCost(buy100) + ")", "/difficulty do buy 100", "Purchase +100"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§6+1k §8(" + CurrencyBridge.formatCost(buy1k) + ")", "/difficulty do buy 1000", "Purchase +1,000"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§6+10k §8(" + CurrencyBridge.formatCost(buy10k) + ")", "/difficulty do buy 10000", "Purchase +10,000"));
        send(player, buy);

        MutableComponent pages = Component.m_237113_("§7Pages: ")
                .m_7220_(btn("§fRewards", "/difficulty do page rewards", "Reward multipliers"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§fTiers", "/difficulty do page tiers", "Enemy tiers"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§fStats", "/difficulty do page stats", "Statistics"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§aRefresh", "/difficulty", "Re-open menu"));
        send(player, pages);
        send(player, Component.m_237113_("§8§m                                        "));
    }

    private static void rewards(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        double mult = DifficultyCalculator.rewardMultiplier(snap.active);
        send(player, Component.m_237113_("§6§lRewards §8(Active " + snap.active + ")"));
        send(player, Component.m_237113_("§eTP Multiplier: §f×" + String.format("%.2f", mult)));
        send(player, Component.m_237113_("§7Formula: 1 + Difficulty / RewardScaling"));
        send(player, btn("§a« Back", "/difficulty do page main", "Return"));
    }

    private static void tiers(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        DifficultyTier current = DifficultyTier.of(snap.active);
        send(player, Component.m_237113_("§6§lEnemy Tiers §8(current §f" + current.display + "§8)"));
        for (DifficultyTier tier : DifficultyTier.values()) {
            if (tier == DifficultyTier.NONE) {
                continue;
            }
            String mark = snap.active >= tier.threshold() ? "§a✓" : "§8·";
            send(player, Component.m_237113_(mark + " §e" + tier.threshold() + " §f" + tier.display));
        }
        send(player, btn("§a« Back", "/difficulty do page main", "Return"));
    }

    private static void stats(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerDifficultyData data = DifficultyCache.data(player);
        List<String> titles = data.getTitles();
        send(player, Component.m_237113_("§6§lStatistics §8(§" + snap.stateColorCode() + snap.state() + "§8)"));
        send(player, Component.m_237113_("§7Balance §f" + CurrencyBridge.balanceText(player)
                + " §8| §7Teams §f" + TeamScaling.teamSourceLabel()));
        send(player, Component.m_237113_("§7Active §f" + snap.active
                + " §8| §7Available §f" + snap.availableMax));
        send(player, Component.m_237113_("§7Purchased §f" + snap.purchased
                + " §8| §7Calculated §f" + snap.calculated));
        send(player, Component.m_237113_("§7Titles §f" + (titles.isEmpty() ? "none" : String.join("§8, §f", titles))));
        send(player, btn("§a« Back", "/difficulty do page main", "Return"));
    }

    private static void settings(ServerPlayer player) {
        DifficultyConfig cfg = DifficultyConfig.get();
        send(player, Component.m_237113_("§6§lAdmin Settings"));
        send(player, Component.m_237113_("§7currency §f" + cfg.purchaseCurrency
                + " §8| §7baseCost §f" + cfg.baseCost));
        send(player, Component.m_237113_("§7maxHealthMultiplier §f" + cfg.maxHealthMultiplier
                + " §8| §7maxScaledHealth §f" + cfg.maxScaledHealth));
        send(player, Component.m_237113_("§7bossHealthThreshold §f" + cfg.bossHealthThreshold));
        send(player, Component.m_237113_("§8Use §f/difficulty admin set <key> <value>"));
        send(player, btn("§a« Back", "/difficulty do page main", "Return"));
    }

    private static MutableComponent btn(String label, String command, String hover) {
        return Component.m_237113_(label).m_130948_(
                Style.f_131099_
                        .m_131142_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .m_131144_(new HoverEvent(HoverEvent.Action.f_130831_, Component.m_237113_(hover)))
        );
    }

    private static void send(ServerPlayer player, Component component) {
        player.m_213846_(component);
    }
}
