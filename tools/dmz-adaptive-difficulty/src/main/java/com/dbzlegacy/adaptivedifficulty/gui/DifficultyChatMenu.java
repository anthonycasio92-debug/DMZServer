package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/** V3 chat fallback: Buy Tier / Lower / Team / Details. */
public final class DifficultyChatMenu {
    private static final long[] STEPS = {1L, 5L, 25L, 100L, 1000L, 10000L, 100000L};

    private DifficultyChatMenu() {}

    public static void open(ServerPlayer player, String page) {
        if ("settings".equalsIgnoreCase(page)) {
            settings(player);
            return;
        }
        // While the master switch is off / player not whitelisted, show the blocked hub.
        if ((!DifficultyConfig.isEnabled() || !SystemGate.allows(player))
                && (page == null || page.isBlank() || !"settings".equalsIgnoreCase(page))) {
            main(player);
            return;
        }
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page)) {
            main(player);
        } else if ("adjust".equalsIgnoreCase(page) || "change".equalsIgnoreCase(page) || "set".equalsIgnoreCase(page)) {
            adjust(player);
        } else if ("buy".equalsIgnoreCase(page) || "purchase".equalsIgnoreCase(page) || "unlock".equalsIgnoreCase(page)
                || "tiers".equalsIgnoreCase(page) || "enemies".equalsIgnoreCase(page)
                || "titles".equalsIgnoreCase(page) || "title".equalsIgnoreCase(page)
                || "rewards".equalsIgnoreCase(page)) {
            // Legacy page names redirect to Buy Tier.
            buy(player);
        } else if ("stats".equalsIgnoreCase(page) || "statistics".equalsIgnoreCase(page) || "details".equalsIgnoreCase(page)) {
            stats(player);
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        String color = snap.stateColorCode();
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8──────── §fDifficulty V3 §8────────"));
        if (!DifficultyConfig.isEnabled()) {
            send(player, Component.m_237113_("§c§lSYSTEM DISABLED §8· §7An admin turned Adaptive Difficulty off."));
            send(player, Component.m_237113_("§8No scaling, kill coins, or tier purchases until re-enabled."));
            send(player, Component.m_237113_("§8────────────────────────"));
            return;
        }
        if (!SystemGate.allows(player)) {
            send(player, Component.m_237113_("§e§lWHITELIST ONLY §8· §7Adaptive Difficulty is in testing mode."));
            send(player, Component.m_237113_("§8Ask an admin to add you: §f/difficulty admin whitelist add <you>"));
            send(player, Component.m_237113_("§8────────────────────────"));
            return;
        }
        send(player, Component.m_237113_("§7Tier §f" + snap.activeTierName
                + "  §8·  §f" + snap.active + " §8/ §7" + snap.availableMax
                + "  §8·  §" + color + snap.state()));
        send(player, Component.m_237113_("§7Combat Rating §f" + snap.combatRating
                + "  §8·  §7Ancient §f" + AncientCoinEconomy.format(snap.ancientCopper)));
        send(player, Component.m_237113_("§7DMZ §f" + snap.dmzLevel
                + "  §8·  §7Prestige §f" + snap.prestige
                + "  §8·  §7Unlocked §fT" + snap.highestUnlockedTier
                + "  §8·  §7Team §f" + snap.teamMode.displayName()));
        send(player, Component.m_237113_("§8Kill rewards: Ancient Coins drop at the mob."));
        send(player, Component.m_237113_(""));

        MutableComponent hub = Component.m_237113_("§7")
                .m_7220_(btn("§eBuy Tier", "/difficulty do page buy", "Purchase Unlock Tier 1–7"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fLower", "/difficulty do page adjust", "Lower / clear (free)"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§bTeam", "/difficulty do team 0 main", "Mode: " + snap.teamMode))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fDetails", "/difficulty do page stats", "Breakdown"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§7↻", "/difficulty", "Refresh"));
        send(player, hub);
        send(player, Component.m_237113_("§8────────────────────────"));
    }

    private static void adjust(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        send(player, Component.m_237113_("§8──────── §fLower Difficulty §8────────"));
        send(player, Component.m_237113_("§f" + snap.active + " §8/ §7" + snap.availableMax
                + "  §8Inv §f" + AncientCoinEconomy.balanceText(player)));
        send(player, Component.m_237113_("§8Raise difficulty by purchasing a tier — not per-level upgrades."));

        MutableComponent down = Component.m_237113_("§7Lower  ");
        for (int i = STEPS.length - 1; i >= 0; i--) {
            long step = STEPS[i];
            if (i < STEPS.length - 1) {
                down.m_7220_(Component.m_237113_(" "));
            }
            down.m_7220_(btn("§c−" + step, "/difficulty do down " + step + " adjust", "Lower (free)"));
        }
        down.m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fReset", "/difficulty do reset 0 adjust", "Clear active tier"));
        send(player, down);

        MutableComponent nav = Component.m_237113_("")
                .m_7220_(btn("§7« Back", "/difficulty do page main", "Return"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§eBuy Tier", "/difficulty do page buy", "Purchase a tier"));
        send(player, nav);
    }

    private static void buy(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        send(player, Component.m_237113_("§8──────── §fBuy Difficulty Tier §8────────"));
        send(player, Component.m_237113_("§7Active §f" + snap.activeTierName
                + "  §7Unlocked §fT" + snap.highestUnlockedTier
                + "  §7Inv §f" + AncientCoinEconomy.balanceText(player)));
        send(player, Component.m_237113_("§8Purchase sets full tier difficulty. Exact coins. No change."));

        MutableComponent row = Component.m_237113_("§7");
        for (UnlockTier tier : UnlockTier.values()) {
            boolean unlocked = snap.highestUnlockedTier >= tier.id;
            boolean active = snap.activeTier == tier.id;
            String label = (active ? "§a● T" : unlocked ? "§eBuy T" : "§8T") + tier.id;
            String tip = unlocked
                    ? tier.display + " · max " + tier.maxDifficulty()
                    + " · cost " + AncientCoinEconomy.formatExactCost(tier.activationCost())
                    : "Locked · need DMZ " + tier.requiredDmzLevel() + " or Prestige " + tier.id;
            row.m_7220_(btn(label, "/difficulty do activate " + tier.id + " buy", tip));
            row.m_7220_(Component.m_237113_(" "));
        }
        send(player, row);

        MutableComponent nav = Component.m_237113_("")
                .m_7220_(btn("§7« Back", "/difficulty do page main", "Return"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fLower", "/difficulty do page adjust", "Lower / clear"));
        send(player, nav);
    }

    private static void stats(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        send(player, Component.m_237113_("§8──────── §fDetails §8────────"));
        send(player, Component.m_237113_("§7State §" + snap.stateColorCode() + snap.state()
                + "  §7Team §f" + snap.teamMode
                + "  §7" + TeamScaling.teamName(player)));
        send(player, Component.m_237113_("§7Bonus §f" + snap.teamThresholdBonus
                + "  §7Contrib §f" + snap.teamContribution
                + "  §7Online §f" + TeamScaling.teammates(player).size()));
        send(player, Component.m_237113_("§7Combat Rating §f" + snap.combatRating
                + "  §7Ancient §f" + AncientCoinEconomy.balanceText(player)));
        send(player, Component.m_237113_("§8Gates: T2 evo · T3 AI · T4 elite · T5 mutation · T6 boss · T7 full"));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void settings(ServerPlayer player) {
        DifficultyConfig cfg = DifficultyConfig.get();
        send(player, Component.m_237113_("§8──────── §fAdmin §8────────"));
        send(player, Component.m_237113_(
                (cfg.enabled ? "§aSystem ENABLED" : "§cSystem DISABLED")
                        + " §8· "
                        + (cfg.whitelistEnabled
                        ? "§eWhitelist ON §7(" + DifficultyConfig.whitelistEntries().size() + ")"
                        : "§7Whitelist OFF")));
        send(player, Component.m_237113_("§8/difficulty admin off|on · whitelist on|off|add|remove|list"));
        send(player, Component.m_237113_("§7CR weights DMZ §f" + cfg.combatRatingDmzWeight
                + "  §7Prestige §f" + cfg.combatRatingPrestigeWeight
                + "  §7Active §f" + cfg.combatRatingDifficultyWeight));
        send(player, Component.m_237113_("§7Coin drops §f" + cfg.enableAncientCoinDrops
                + "  §7Death reset §f" + cfg.deathResetsActiveDifficulty));
        send(player, Component.m_237113_("§8/difficulty admin set enabled|whitelistEnabled true|false"));
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
