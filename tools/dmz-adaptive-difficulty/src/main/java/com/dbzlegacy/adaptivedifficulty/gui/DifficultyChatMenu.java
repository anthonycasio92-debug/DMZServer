package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.team.TeamScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
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
        } else if ("titles".equalsIgnoreCase(page) || "title".equalsIgnoreCase(page)) {
            titles(player);
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
        send(player, Component.m_237113_("§8──────── §fDifficulty V3 §8────────"));
        send(player, Component.m_237113_("§7Tier §f" + snap.activeTierName
                + "  §8·  §f" + snap.active + " §8/ §7" + snap.availableMax
                + "  §8·  §" + color + snap.state()));
        send(player, Component.m_237113_("§7Combat Rating §f" + snap.combatRating
                + "  §8·  §7Ancient §f" + AncientCoinEconomy.format(snap.ancientCopper)
                + "  §8·  §7Title §f" + TitleSystem.activeDisplay(player)));
        send(player, Component.m_237113_("§7DMZ §f" + snap.dmzLevel
                + "  §8·  §7Prestige §f" + snap.prestige
                + "  §8·  §7Unlocked §fT" + snap.highestUnlockedTier
                + "  §8·  §7Team §f" + snap.teamMode.displayName()));
        send(player, Component.m_237113_(""));

        MutableComponent hub = Component.m_237113_("§7")
                .m_7220_(btn("§eBuy Tier", "/difficulty do page buy", "Purchase a difficulty tier"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fLower", "/difficulty do page adjust", "Lower / clear (free)"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§bTeam", "/difficulty do team 0 main", "Mode: " + snap.teamMode));
        send(player, hub);

        MutableComponent pages = Component.m_237113_("§7More  ")
                .m_7220_(btn("§fRewards", "/difficulty do page rewards", "Kill rewards info"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fTiers", "/difficulty do page tiers", "Enemy tiers"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§eTitles", "/difficulty do page titles", "Unlock & equip titles"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fDetails", "/difficulty do page stats", "Full breakdown"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§7↻", "/difficulty", "Refresh"));
        send(player, pages);
        send(player, Component.m_237113_("§8────────────────────────"));
    }

    private static void adjust(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        send(player, Component.m_237113_("§8──────── §fLower Difficulty §8────────"));
        send(player, Component.m_237113_("§f" + snap.active + " §8/ §7" + snap.availableMax
                + "  §8Inv §f" + CurrencyBridge.balanceText(player)));
        send(player, Component.m_237113_("§8Raising is done by purchasing tiers — no +difficulty upgrades."));

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
        send(player, Component.m_237113_("§8Click a tier to purchase. Sets full tier difficulty. No change given."));

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

    private static void rewards(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        send(player, Component.m_237113_("§8──────── §fRewards §8────────"));
        double mult = DifficultyCalculator.rewardMultiplier(snap.active);
        send(player, Component.m_237113_("§7Active §f" + snap.active
                + "  §7Tier §f" + DifficultyTier.of(snap.active).display
                + "  §7Reward × §a" + String.format("%.2f", mult)));
        send(player, Component.m_237113_("§8Primary currency reward: Ancient Coins (tiered quality)."));
        send(player, Component.m_237113_("§8XP / drop odds scale with active difficulty. TP / Potential untouched."));
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

    private static void titles(ServerPlayer player) {
        TitleSystem.syncTierTitles(player, true);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        String equipped = TitleSystem.activeDisplay(player);
        List<String> owned = TitleSystem.unlockedDisplays(player);
        send(player, Component.m_237113_("§8──────── §fTitles §8────────"));
        send(player, Component.m_237113_("§7Equipped §e" + equipped
                + "  §8·  §7Owned §f" + owned.size()
                + "  §8·  §7Active §f" + snap.active));
        MutableComponent tierRow = Component.m_237113_("§7Tiers  ");
        int inRow = 0;
        for (DifficultyTitle title : DifficultyTitle.values()) {
            if (title.kind != DifficultyTitle.Kind.TIER) {
                continue;
            }
            if (inRow > 0) {
                tierRow.m_7220_(Component.m_237113_(" "));
            }
            boolean unlocked = TitleSystem.has(player, title);
            boolean on = title.id.equals(TitleSystem.activeId(player));
            String label = on ? "§e[" + title.display + "]"
                    : unlocked ? "§a" + title.display : "§8" + title.display;
            String tip = !unlocked
                    ? "Locked · reach " + title.unlockTier.threshold()
                    : on ? "Click to unequip" : "Equip " + title.display;
            if (unlocked) {
                tierRow.m_7220_(btn(label, "/difficulty do equip_title " + title.id + " titles", tip));
            } else {
                tierRow.m_7220_(Component.m_237113_(label));
            }
            inRow++;
            if (inRow >= 8) {
                send(player, tierRow);
                tierRow = Component.m_237113_("§7      ");
                inRow = 0;
            }
        }
        if (inRow > 0) {
            send(player, tierRow);
        }

        MutableComponent combat = Component.m_237113_("§7Combat  ");
        boolean firstCombat = true;
        for (DifficultyTitle title : DifficultyTitle.values()) {
            if (title.kind != DifficultyTitle.Kind.COMBAT) {
                continue;
            }
            if (!firstCombat) {
                combat.m_7220_(Component.m_237113_(" "));
            }
            firstCombat = false;
            boolean unlocked = TitleSystem.has(player, title);
            boolean on = title.id.equals(TitleSystem.activeId(player));
            String label = on ? "§e[" + title.display + "]"
                    : unlocked ? "§a" + title.display : "§8" + title.display;
            String tip = combatTip(title, unlocked, on);
            if (unlocked) {
                combat.m_7220_(btn(label, "/difficulty do equip_title " + title.id + " titles", tip));
            } else {
                combat.m_7220_(Component.m_237113_(label));
            }
        }
        send(player, combat);

        MutableComponent nav = Component.m_237113_("")
                .m_7220_(btn("§7« Back", "/difficulty do page main", "Return"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§fUnequip", "/difficulty do clear_title 0 titles", "Clear equipped title"));
        send(player, nav);
    }

    private static String combatTip(DifficultyTitle title, boolean unlocked, boolean equipped) {
        if (equipped) {
            return "Click to unequip";
        }
        if (unlocked) {
            return "Equip " + title.display;
        }
        return switch (title) {
            case BOSS_SLAYER -> "Kill a boss at Master+";
            case LEGENDARY_HUNTER -> "Kill an elite at Legendary+";
            case GOD_CHALLENGER -> "Get a kill at God+";
            default -> "Locked";
        };
    }

    private static void stats(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        List<String> titles = TitleSystem.unlockedDisplays(player);
        send(player, Component.m_237113_("§8──────── §fDetails §8────────"));
        send(player, Component.m_237113_("§7State §" + snap.stateColorCode() + snap.state()
                + "  §7Team §f" + snap.teamMode
                + "  §7" + TeamScaling.teamName(player)));
        send(player, Component.m_237113_("§7Bonus §f" + snap.teamThresholdBonus
                + "  §7Contrib §f" + snap.teamContribution
                + "  §7Online §f" + TeamScaling.teammates(player).size()));
        send(player, Component.m_237113_("§7Inventory §f" + CurrencyBridge.balanceText(player)));
        send(player, Component.m_237113_("§7Title §e" + TitleSystem.activeDisplay(player)
                + "  §7Owned §f" + (titles.isEmpty() ? "none" : String.join("§8, §f", titles))));
        MutableComponent nav = Component.m_237113_("")
                .m_7220_(btn("§7« Back", "/difficulty do page main", "Return"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§eTitles", "/difficulty do page titles", "Unlock & equip"));
        send(player, nav);
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
