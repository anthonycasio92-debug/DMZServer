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

/** Clean clickable chat fallback when inventory GUIs are unavailable. */
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
        send(player, Component.m_237113_("§8──────── §fDifficulty §8────────"));
        send(player, Component.m_237113_("§f" + snap.active + " §8/ §7" + snap.availableMax
                + "  §8·  §" + color + snap.state()
                + "  §8·  §7" + DifficultyTier.of(snap.active).display));
        send(player, Component.m_237113_("§8Calc §f" + snap.calculated
                + "  §8Bought §f" + snap.purchased
                + "  §8Wallet §f" + CurrencyBridge.balanceText(player)));
        send(player, Component.m_237113_(""));

        long room = Math.max(0L, snap.availableMax - snap.active);
        long upAmt = Math.min(100L, room);
        long upCost = upAmt <= 0 ? 0L : DifficultyCalculator.raiseCostIronCoins(snap.active, upAmt);
        long maxCost = room <= 0 ? 0L : DifficultyCalculator.raiseCostIronCoins(snap.active, room);

        MutableComponent controls = Component.m_237113_("")
                .m_7220_(btn("§c−100", "/difficulty do down 100", "Lower (free)"))
                .m_7220_(Component.m_237113_(" §8· "))
                .m_7220_(btn("§fReset", "/difficulty do reset 0", "Active → 0"))
                .m_7220_(Component.m_237113_(" §8· "))
                .m_7220_(btn("§a+100", "/difficulty do up 100",
                        upAmt <= 0 ? "At max" : "Raise — " + CurrencyBridge.formatCost(upCost)))
                .m_7220_(Component.m_237113_(" §8· "))
                .m_7220_(btn("§6Max", "/difficulty do set_max 0",
                        room <= 0 ? "At max" : "Max — " + CurrencyBridge.formatCost(maxCost)))
                .m_7220_(Component.m_237113_(" §8· "))
                .m_7220_(btn("§bTeam", "/difficulty do team 0", "Mode: " + snap.teamMode));
        send(player, controls);

        long buy100 = DifficultyCalculator.purchaseCost(snap.purchased, 100);
        long buy1k = DifficultyCalculator.purchaseCost(snap.purchased, 1_000);
        long buy10k = DifficultyCalculator.purchaseCost(snap.purchased, 10_000);
        MutableComponent buy = Component.m_237113_("§7Buy max  ")
                .m_7220_(btn("§e+100", "/difficulty do buy 100", CurrencyBridge.formatCost(buy100)))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§e+1k", "/difficulty do buy 1000", CurrencyBridge.formatCost(buy1k)))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§e+10k", "/difficulty do buy 10000", CurrencyBridge.formatCost(buy10k)));
        send(player, buy);

        MutableComponent pages = Component.m_237113_("§7More  ")
                .m_7220_(btn("§fRewards", "/difficulty do page rewards", "TP multiplier"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fTiers", "/difficulty do page tiers", "Enemy tiers"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fDetails", "/difficulty do page stats", "Full breakdown"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§7↻", "/difficulty", "Refresh"));
        send(player, pages);
        send(player, Component.m_237113_("§8────────────────────────"));
    }

    private static void rewards(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        double mult = DifficultyCalculator.rewardMultiplier(snap.active);
        send(player, Component.m_237113_("§8──────── §fRewards §8────────"));
        send(player, Component.m_237113_("§7Active §f" + snap.active + "  §7TP ×§a" + String.format("%.2f", mult)));
        send(player, Component.m_237113_("§81 + Difficulty / RewardScaling"));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void tiers(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        DifficultyTier current = DifficultyTier.of(snap.active);
        send(player, Component.m_237113_("§8──────── §fTiers §8────────"));
        send(player, Component.m_237113_("§7Current §f" + current.display));
        for (DifficultyTier tier : DifficultyTier.values()) {
            if (tier == DifficultyTier.NONE) {
                continue;
            }
            boolean on = snap.active >= tier.threshold();
            send(player, Component.m_237113_((on ? "§a● " : "§8○ ") + "§f" + tier.display + " §8" + tier.threshold()));
        }
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void stats(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerDifficultyData data = DifficultyCache.data(player);
        List<String> titles = data.getTitles();
        send(player, Component.m_237113_("§8──────── §fDetails §8────────"));
        send(player, Component.m_237113_("§7State §" + snap.stateColorCode() + snap.state()
                + "  §7Team §f" + snap.teamMode
                + "  §7" + TeamScaling.teamName(player)));
        send(player, Component.m_237113_("§7Bonus §f" + snap.teamThresholdBonus
                + "  §7Contrib §f" + snap.teamContribution
                + "  §7Online §f" + TeamScaling.teammates(player).size()));
        send(player, Component.m_237113_("§7Titles §f" + (titles.isEmpty() ? "none" : String.join("§8, §f", titles))));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void settings(ServerPlayer player) {
        DifficultyConfig cfg = DifficultyConfig.get();
        send(player, Component.m_237113_("§8──────── §fAdmin §8────────"));
        send(player, Component.m_237113_("§7ironBase §f" + cfg.baseCostIronCoins
                + "  §7scale §f" + cfg.costScalePerDifficulty
                + "  §7coin §f" + cfg.costCoinItem));
        send(player, Component.m_237113_("§8/difficulty admin set <key> <value>"));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
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
