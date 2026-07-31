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
    private static final long[] STEPS = {1L, 5L, 25L, 100L, 1000L, 10000L, 100000L};

    private DifficultyChatMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page)) {
            main(player);
        } else if ("adjust".equalsIgnoreCase(page) || "change".equalsIgnoreCase(page) || "set".equalsIgnoreCase(page)) {
            adjust(player);
        } else if ("buy".equalsIgnoreCase(page) || "purchase".equalsIgnoreCase(page) || "unlock".equalsIgnoreCase(page)) {
            buy(player);
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
        send(player, Component.m_237113_("§8Theoretical §f" + snap.calculated
                + "  §8Bought §f" + snap.purchased
                + "  §8Inv §f" + CurrencyBridge.balanceText(player)));
        send(player, Component.m_237113_(""));

        MutableComponent hub = Component.m_237113_("§7")
                .m_7220_(btn("§aAdjust", "/difficulty do page adjust", "Raise / lower active"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§eBuy Max", "/difficulty do page buy", "Unlock more max"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§bTeam", "/difficulty do team 0 main", "Mode: " + snap.teamMode));
        send(player, hub);

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

    private static void adjust(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        long room = Math.max(0L, snap.availableMax - snap.active);
        long maxCost = room <= 0 ? 0L : DifficultyCalculator.raiseCostIronCoins(snap.active, room);

        send(player, Component.m_237113_("§8──────── §fAdjust Difficulty §8────────"));
        send(player, Component.m_237113_("§f" + snap.active + " §8/ §7" + snap.availableMax
                + "  §8Inv §f" + CurrencyBridge.balanceText(player)));

        MutableComponent down = Component.m_237113_("§7Lower  ");
        for (int i = STEPS.length - 1; i >= 0; i--) {
            long step = STEPS[i];
            if (i < STEPS.length - 1) {
                down.m_7220_(Component.m_237113_(" "));
            }
            down.m_7220_(btn("§c−" + step, "/difficulty do down " + step + " adjust", "Lower (free)"));
        }
        down.m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fReset", "/difficulty do reset 0 adjust", "Active → 0"));
        send(player, down);

        MutableComponent up = Component.m_237113_("§7Raise  ");
        for (int i = 0; i < STEPS.length; i++) {
            long step = STEPS[i];
            if (i > 0) {
                up.m_7220_(Component.m_237113_(" "));
            }
            long amt = Math.min(step, room);
            String tip = amt <= 0
                    ? "At max"
                    : "Raise — " + CurrencyBridge.formatCost(
                            DifficultyCalculator.raiseCostIronCoins(snap.active, amt))
                    + " (inventory)";
            up.m_7220_(btn("§a+" + step, "/difficulty do up " + step + " adjust", tip));
        }
        up.m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§6Max", "/difficulty do set_max 0 adjust",
                        room <= 0 ? "At max" : "Max — " + CurrencyBridge.formatCost(maxCost)));
        send(player, up);

        MutableComponent nav = Component.m_237113_("")
                .m_7220_(btn("§7« Back", "/difficulty do page main", "Return"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§eBuy Max", "/difficulty do page buy", "Unlock more max"));
        send(player, nav);
    }

    private static void buy(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        send(player, Component.m_237113_("§8──────── §fBuy Max Difficulty §8────────"));
        send(player, Component.m_237113_("§7Purchased §f" + snap.purchased
                + "  §7Available §f" + snap.availableMax
                + "  §8Inv §f" + CurrencyBridge.balanceText(player)));

        MutableComponent row = Component.m_237113_("§7Buy  ");
        for (int i = 0; i < STEPS.length; i++) {
            long step = STEPS[i];
            if (i > 0) {
                row.m_7220_(Component.m_237113_(" "));
            }
            long cost = DifficultyCalculator.purchaseCost(snap.purchased, step);
            row.m_7220_(btn("§e+" + step, "/difficulty do buy " + step + " buy",
                    CurrencyBridge.formatCost(cost) + " (inventory)"));
        }
        send(player, row);

        MutableComponent nav = Component.m_237113_("")
                .m_7220_(btn("§7« Back", "/difficulty do page main", "Return"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§aAdjust", "/difficulty do page adjust", "Raise / lower"));
        send(player, nav);
    }

    private static void rewards(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        DifficultyConfig cfg = DifficultyConfig.get();
        double killTp = DifficultyCalculator.killTrainingPoints(snap.active);
        send(player, Component.m_237113_("§8──────── §fRewards §8────────"));
        send(player, Component.m_237113_("§7Active §f" + snap.active
                + "  §7Kill TP §a~" + String.format("%,.0f", killTp)));
        send(player, Component.m_237113_("§8Kill TP = mob max HP × "
                + String.format("%.0f", cfg.killTpPerHealth)
                + " · Train TP: no multiplier"));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void tiers(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        DifficultyConfig cfg = DifficultyConfig.get();
        DifficultyTier current = DifficultyTier.of(snap.active);
        send(player, Component.m_237113_("§8──────── §fTiers §8────────"));
        send(player, Component.m_237113_("§7Current §f" + current.display
                + "  §8·  §7Zenith §f" + cfg.tierZenith
                + " §8(L" + cfg.referenceMaxLevel + "/P" + cfg.referenceMaxPrestige + ")"));
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
        send(player, Component.m_237113_("§7Inventory §f" + CurrencyBridge.balanceText(player)));
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
