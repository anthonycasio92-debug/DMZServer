package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side clickable difficulty GUI (concept §5).
 * Works without a client Screen jar — buttons are clickable chat components.
 */
public final class DifficultyMenu {
    private DifficultyMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page)) {
            main(player);
        } else if ("rewards".equalsIgnoreCase(page)) {
            rewards(player);
        } else if ("tiers".equalsIgnoreCase(page) || "enemies".equalsIgnoreCase(page)) {
            tiers(player);
        } else if ("stats".equalsIgnoreCase(page) || "statistics".equalsIgnoreCase(page)) {
            stats(player);
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        String color = snap.stateColorCode();
        long buy100 = DifficultyCalculator.purchaseCost(snap.purchased, 100);
        long buy1k = DifficultyCalculator.purchaseCost(snap.purchased, 1_000);
        long buy10k = DifficultyCalculator.purchaseCost(snap.purchased, 10_000);

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
        send(player, Component.m_237113_("§eTeam Mode: §f" + snap.teamMode
                + " §8| §eCurrency: §f" + CurrencyBridge.currencyLabel()));
        send(player, Component.m_237113_("§eEnemy Tier: §f" + DifficultyTier.of(snap.active).display));
        send(player, Component.m_237113_(""));

        MutableComponent controls = Component.m_237113_("§7Controls: ")
                .m_7220_(btn("§a▲ +100", "/difficulty up 100", "Raise active difficulty"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§c▼ -100", "/difficulty down 100", "Lower active difficulty (free)"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§bTeam", "/difficulty team", "Cycle team scaling mode"));
        send(player, controls);

        MutableComponent buy = Component.m_237113_("§7Buy: ")
                .m_7220_(btn("§6+100 §8(" + buy100 + ")", "/difficulty buy 100", "Purchase +100 difficulty"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§6+1,000 §8(" + buy1k + ")", "/difficulty buy 1000", "Purchase +1,000 difficulty"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§6+10,000 §8(" + buy10k + ")", "/difficulty buy 10000", "Purchase +10,000 difficulty"));
        send(player, buy);

        MutableComponent pages = Component.m_237113_("§7Pages: ")
                .m_7220_(btn("§fRewards", "/difficulty gui rewards", "Reward multipliers"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§fEnemy Tiers", "/difficulty gui tiers", "Tier unlock list"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§fStatistics", "/difficulty gui stats", "Detailed snapshot"));
        send(player, pages);
        send(player, Component.m_237113_("§8§m                                        "));
    }

    private static void rewards(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        double mult = DifficultyCalculator.rewardMultiplier(snap.active);
        send(player, Component.m_237113_("§6§lRewards §8(Active " + snap.active + ")"));
        send(player, Component.m_237113_("§eTP Multiplier: §f×" + String.format("%.2f", mult)));
        send(player, Component.m_237113_("§7Formula: 1 + Difficulty / RewardScaling"));
        send(player, Component.m_237113_("§7Elite kills grant bonus TP. Bosses scale harder."));
        send(player, btn("§a« Back", "/difficulty gui", "Return"));
    }

    private static void tiers(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        DifficultyTier current = DifficultyTier.of(snap.active);
        send(player, Component.m_237113_("§6§lEnemy Tiers §8(current §f" + current.display + "§8)"));
        for (DifficultyTier tier : DifficultyTier.values()) {
            if (tier == DifficultyTier.NONE) {
                continue;
            }
            String mark = snap.active >= tier.threshold ? "§a✓" : "§8·";
            send(player, Component.m_237113_(mark + " §e" + tier.threshold + " §f" + tier.display
                    + aiUnlockHint(tier)));
        }
        send(player, btn("§a« Back", "/difficulty gui", "Return"));
    }

    private static String aiUnlockHint(DifficultyTier tier) {
        return switch (tier) {
            case AWAKENED -> " §7· base scaling";
            case ENHANCED -> " §7· mutations / focus weakest";
            case ELITE -> " §7· elite spawns / retreat AI";
            case ADVANCED -> " §7· dodge / anti-flight";
            case MASTER -> " §7· coordinated attacks";
            case LEGENDARY, GOD, DIVINE, IMPOSSIBLE -> " §7· max AI pressure";
            default -> "";
        };
    }

    private static void stats(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        send(player, Component.m_237113_("§6§lStatistics"));
        send(player, Component.m_237113_("§7DMZ Level §f" + snap.dmzLevel
                + " §8| §7Prestige §f" + snap.prestige));
        send(player, Component.m_237113_("§7Calculated §f" + snap.calculated
                + " §8| §7Purchased §f" + snap.purchased));
        send(player, Component.m_237113_("§7Personal Max §f" + snap.personalMax
                + " §8| §7Available §f" + snap.availableMax));
        send(player, Component.m_237113_("§7Active §f" + snap.active
                + " §8| §7State color §" + snap.stateColorCode() + "●"));
        send(player, Component.m_237113_("§7Lightman's loaded: §f" + CurrencyBridge.lightmansAvailable()
                + " §8| mode §f" + CurrencyBridge.activeMode()));
        send(player, btn("§a« Back", "/difficulty gui", "Return"));
    }

    private static MutableComponent btn(String label, String command, String hover) {
        return Component.m_237113_(label).m_130948_(
                Style.f_131099_
                        .m_131142_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .m_131144_(new HoverEvent(HoverEvent.Action.f_130831_, Component.m_237113_(hover)))
        );
    }

    private static void send(ServerPlayer player, Component component) {
        player.m_213846_(component); // sendSystemMessage
    }
}
