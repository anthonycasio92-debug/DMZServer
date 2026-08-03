package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/**
 * Chat fallback when CMI/chest GUI is unavailable.
 * Tier-centric: no difficulty points, no V3 branding.
 */
public final class DifficultyChatMenu {
    private DifficultyChatMenu() {}

    public static void open(ServerPlayer player, String page) {
        if ("settings".equalsIgnoreCase(page)) {
            if (StaffAccess.isStaff(player)) {
                settings(player);
            } else {
                main(player);
            }
            return;
        }
        if ((!DifficultyConfig.isEnabled() || !SystemGate.allows(player))
                && (page == null || page.isBlank() || !"settings".equalsIgnoreCase(page))) {
            main(player);
            return;
        }
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page)) {
            main(player);
        } else if ("adjust".equalsIgnoreCase(page) || "change".equalsIgnoreCase(page)
                || "set".equalsIgnoreCase(page) || "lower".equalsIgnoreCase(page)) {
            lower(player);
        } else if ("buy".equalsIgnoreCase(page) || "purchase".equalsIgnoreCase(page)
                || "unlock".equalsIgnoreCase(page) || "tiers".equalsIgnoreCase(page)
                || "enemies".equalsIgnoreCase(page) || "rewards".equalsIgnoreCase(page)) {
            buy(player);
        } else if ("titles".equalsIgnoreCase(page) || "title".equalsIgnoreCase(page)) {
            titles(player);
        } else if ("team".equalsIgnoreCase(page) || "teams".equalsIgnoreCase(page)) {
            teamsWip(player);
        } else if ("stats".equalsIgnoreCase(page) || "statistics".equalsIgnoreCase(page)
                || "details".equalsIgnoreCase(page)) {
            if (isStaff(player)) {
                stats(player);
            } else {
                main(player);
            }
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fAdaptive Difficulty §8──"));
        if (!DifficultyConfig.isEnabled()) {
            send(player, Component.m_237113_("§c§lSYSTEM DISABLED"));
            if (isStaff(player)) {
                send(player, Component.m_237113_("§8Re-enable: §f/difficulty admin on"));
            } else {
                send(player, Component.m_237113_("§7Please try again later."));
            }
            send(player, Component.m_237113_("§8────────────────"));
            return;
        }
        if (!SystemGate.allows(player)) {
            send(player, Component.m_237113_("§e§lNOT AVAILABLE"));
            if (isStaff(player)) {
                send(player, Component.m_237113_("§8Whitelist: §f/difficulty admin whitelist add <you>"));
            } else {
                send(player, Component.m_237113_("§7Ask an admin if you need access."));
            }
            send(player, Component.m_237113_("§8────────────────"));
            return;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean personalOn = data.isPersonalEnabled();
        boolean coinChatOn = data.isCoinDropChat();
        if (!personalOn) {
            send(player, Component.m_237113_("§c§lDIFFICULTY OFF"));
            send(player, Component.m_237113_("§7No scaling, kill coins, AI pressure, or tier buys"));
            send(player, Component.m_237113_("§8Saved tier §f" + snap.activeTierName
                    + "  §8·  §7Unlocked §fT" + snap.highestUnlockedTier));
        } else {
            send(player, Component.m_237113_("§7Tier §f" + snap.activeTierName
                    + "  §8·  §" + snap.stateColorCode() + snap.state()
                    + "  §8·  §7Unlocked §fT" + snap.highestUnlockedTier));
        }
        send(player, Component.m_237113_("§6Coins §f" + AncientCoinEconomy.inventoryBreakdown(player)));
        if (!TitleSystem.activeDisplay(player).equals("None")) {
            send(player, Component.m_237113_("§7Title §e" + TitleSystem.activeDisplay(player)));
        }
        send(player, Component.m_237113_(""));
        MutableComponent hub = Component.m_237113_("§7")
                .m_7220_(btn("§a[Buy Tier]", "/difficulty do page buy", "Purchase a higher Unlock Tier"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§f[Lower]", "/difficulty do page lower", "Select a lower unlocked tier"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§d[Titles]", "/difficulty do page titles", "Equip difficulty titles"));
        send(player, hub);
        MutableComponent toggles = Component.m_237113_("§7")
                .m_7220_(btn(personalOn ? "§a[Difficulty ON]" : "§c[Difficulty OFF]",
                        "/difficulty do toggle_personal 0 main",
                        personalOn
                                ? "Turn OFF — no scaling, kill coins, AI pressure, or tier buys"
                                : "Turn ON — restore scaling, kill coins, and tier buys"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn(coinChatOn ? "§a[Coin Chat ON]" : "§8[Coin Chat OFF]",
                        "/difficulty do toggle_coin_chat 0 main",
                        coinChatOn
                                ? "Mute Ancient Coin drop chat messages"
                                : "Show Ancient Coin drop chat messages"));
        send(player, toggles);
        if (isStaff(player)) {
            send(player, btn("§8[Details]", "/difficulty do page stats", "Staff breakdown"));
        }
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void buy(ServerPlayer player) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        UnlockSystem.syncUnlocks(player, data);
        int level = DmzProgression.dmzLevel(player);
        int active = data.getActiveTier();
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8──────── §aBuy Higher Tier §8────────"));
        if (!data.isPersonalEnabled()) {
            send(player, Component.m_237113_("§cPersonal difficulty is OFF — turn it ON on the main menu to buy."));
            send(player, btn("§7« Back", "/difficulty do page main", "Return"));
            return;
        }
        send(player, Component.m_237113_("§7Current §f" + (active <= 0 ? "None" : ("T" + active))
                + "  §8·  §7DMZ §f" + level
                + "  §8·  §7Costs scale with your level"));
        send(player, Component.m_237113_("§8Ancient Coins — pay-up OK (lower coins OK), no change."));
        send(player, Component.m_237113_("§f" + AncientCoinEconomy.inventoryBreakdown(player)));
        send(player, Component.m_237113_(""));
        for (UnlockTier tier : UnlockTier.values()) {
            long cost = AncientCoinEconomy.activationCost(tier, player);
            boolean unlocked = data.hasUnlockedTier(tier.id);
            boolean activeHere = active == tier.id;
            MutableComponent line = Component.m_237113_(
                    (activeHere ? "§a● " : unlocked ? "§e" : "§8")
                            + "T" + tier.id + " " + tier.display
                            + " §6" + AncientCoinEconomy.formatExactCost(cost));
            if (activeHere) {
                line = line.m_7220_(Component.m_237113_(" §aCURRENT"));
            } else if (unlocked) {
                line = line.m_7220_(Component.m_237113_(" "))
                        .m_7220_(btn("§a[BUY]", "/difficulty do activate " + tier.id + " buy",
                                "Pay Ancient Coins for Tier " + tier.id + " (pay-up OK, no change)"));
            } else {
                line = line.m_7220_(Component.m_237113_(
                        " §cLOCKED §8(DMZ " + tier.requiredDmzLevel() + " or Prestige " + tier.id + ")"));
            }
            send(player, line);
        }
        send(player, Component.m_237113_(""));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void lower(ServerPlayer player) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        int active = data.getActiveTier();
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8──────── §fLower Difficulty Tier §8────────"));
        if (!data.isPersonalEnabled()) {
            send(player, Component.m_237113_("§cPersonal difficulty is OFF — turn it ON on the main menu to change tiers."));
            send(player, btn("§7« Back", "/difficulty do page main", "Return"));
            return;
        }
        send(player, Component.m_237113_("§7Current §f" + (active <= 0 ? "None" : ("T" + active + " "
                + (UnlockTier.byId(active) == null ? "" : UnlockTier.byId(active).display)))));
        send(player, Component.m_237113_("§8Select a lower unlocked tier, or reset to None."));
        send(player, Component.m_237113_(""));
        send(player, btn("§c[Reset to None]", "/difficulty do lower_tier 0 lower", "Clear active tier"));
        for (UnlockTier tier : UnlockTier.values()) {
            boolean owned = data.hasUnlockedTier(tier.id);
            boolean isCurrent = active == tier.id;
            boolean canLower = owned && tier.id < active;
            MutableComponent line = Component.m_237113_(
                    (isCurrent ? "§a● " : canLower ? "§e" : "§8")
                            + "T" + tier.id + " " + tier.display);
            if (isCurrent) {
                line = line.m_7220_(Component.m_237113_(" §aCURRENT"));
            } else if (canLower) {
                line = line.m_7220_(Component.m_237113_(" "))
                        .m_7220_(btn("§f[SELECT]", "/difficulty do lower_tier " + tier.id + " lower",
                                "Lower active difficulty to Tier " + tier.id));
            } else if (!owned) {
                line = line.m_7220_(Component.m_237113_(" §cNOT OWNED"));
            } else {
                line = line.m_7220_(Component.m_237113_(" §8HIGHER — use Buy"));
            }
            send(player, line);
        }
        send(player, Component.m_237113_(""));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void titles(ServerPlayer player) {
        TitleSystem.syncTierTitles(player, true);
        PlayerDifficultyData data = DifficultyCache.data(player);
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8──────── §dDifficulty Titles §8────────"));
        send(player, Component.m_237113_("§7Titles need higher CR milestones / kill feats."));
        send(player, Component.m_237113_("§7Equipped §e" + TitleSystem.activeDisplay(player)));
        send(player, btn("§c[Clear Title]", "/difficulty do clear_title 0 titles", "Unequip title"));
        send(player, Component.m_237113_(""));
        for (DifficultyTitle title : DifficultyTitle.values()) {
            boolean earned = TitleSystem.has(player, title);
            MutableComponent line = Component.m_237113_(
                    (earned ? "§6" : "§8") + title.display + " §7" + title.requirementTip());
            if (earned) {
                boolean equipped = title.id.equals(data.getActiveTitle());
                if (equipped) {
                    line = line.m_7220_(Component.m_237113_(" §aEQUIPPED"));
                } else {
                    line = line.m_7220_(Component.m_237113_(" "))
                            .m_7220_(btn("§a[EQUIP]", "/difficulty do equip_title " + title.id + " titles",
                                    "Equip " + title.display));
                }
            }
            send(player, line);
        }
        send(player, Component.m_237113_(""));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void teamsWip(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8──────── §7Teams (WIP) §8────────"));
        send(player, Component.m_237113_("§7Team difficulty is not available yet."));
        send(player, Component.m_237113_("§eDifficulty is personal / individual only for now."));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void stats(ServerPlayer player) {
        if (!isStaff(player)) {
            main(player);
            return;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerCombatProfile profile = PlayerCombatProfile.of(player);
        send(player, Component.m_237113_("§8── §fDetails §8(staff) ──"));
        send(player, Component.m_237113_("§7Tier §f" + snap.activeTierName
                + "  §8·  §7State §" + snap.stateColorCode() + snap.state()));
        send(player, Component.m_237113_("§7Combat Rating §f" + snap.combatRating
                + "  §8·  §7DMZ §f" + snap.dmzLevel
                + "  §8·  §7Prestige §f" + snap.prestige));
        send(player, Component.m_237113_("§7Class §f"
                + (profile.fightingClass.isBlank() ? "?" : profile.fightingClass)
                + "  §8·  §7Race §f" + (profile.race.isBlank() ? "?" : profile.race)
                + "  §8·  §7Style §f" + profile.style.name()));
        send(player, Component.m_237113_("§7Top stats §f" + profile.topStatsLabel()
                + "  §8·  §7Weak §f" + profile.weakest.name()));
        send(player, Component.m_237113_("§7Unlocked §fT" + snap.highestUnlockedTier
                + "  §8·  §7Title §e" + TitleSystem.activeDisplay(player)));
        send(player, Component.m_237113_("§6Ancient Coins §f" + AncientCoinEconomy.inventoryBreakdown(player)));
        send(player, Component.m_237113_(
                "§8Counters: class · race · top-3 stats · kits cadence"));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static void settings(ServerPlayer player) {
        if (!StaffAccess.isStaff(player)) {
            main(player);
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        send(player, Component.m_237113_("§8──────── §fAdmin §8────────"));
        send(player, Component.m_237113_(
                (cfg.enabled ? "§aSystem ENABLED" : "§cSystem DISABLED")
                        + " §8· "
                        + (cfg.whitelistEnabled
                        ? "§eWhitelist ON §7(" + DifficultyConfig.whitelistEntries().size() + ")"
                        : "§7Whitelist OFF")));
        send(player, Component.m_237113_("§8/difficulty admin off|on · whitelist on|off|add|remove|list"));
        send(player, Component.m_237113_("§7Tier cost level divisor §f" + cfg.tierCostLevelDivisor
                + " §8(cost × (1 + levelsAboveUnlock / divisor))"));
        send(player, Component.m_237113_("§7Coin drops §f" + cfg.enableAncientCoinDrops
                + "  §7Death reset §f" + cfg.deathResetsActiveDifficulty));
        send(player, Component.m_237113_("§8/difficulty admin set tierCostLevelDivisor <n>"));
        send(player, btn("§7« Back", "/difficulty do page main", "Return"));
    }

    private static MutableComponent btn(String label, String command, String hover) {
        return Component.m_237113_(label).m_130948_(
                Style.f_131099_
                        .m_131142_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                        .m_131144_(new HoverEvent(HoverEvent.Action.f_130831_, Component.m_237113_(hover)))
        );
    }

    private static boolean isStaff(ServerPlayer player) {
        return StaffAccess.isStaff(player);
    }

    private static void send(ServerPlayer player, Component component) {
        player.m_213846_(component);
    }
}
