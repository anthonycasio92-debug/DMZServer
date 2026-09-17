package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
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
        // Chat pages open without DifficultyMenu sometimes — still re-sample DMZ level.
        int sampled = DmzProgression.sampleLevelOnGuiOpen(player);
        if (DmzProgression.hasReliableUnlockGateSample(player)
                && !DmzProgression.isTransformed(player)) {
            DifficultyCache.data(player).noteDmzLevel(sampled);
        }
        DifficultyCache.refresh(player);
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
        } else if ("tiers".equalsIgnoreCase(page) || "tier".equalsIgnoreCase(page)
                || "buy".equalsIgnoreCase(page) || "purchase".equalsIgnoreCase(page)
                || "unlock".equalsIgnoreCase(page) || "enemies".equalsIgnoreCase(page)
                || "rewards".equalsIgnoreCase(page) || "adjust".equalsIgnoreCase(page)
                || "change".equalsIgnoreCase(page) || "set".equalsIgnoreCase(page)
                || "lower".equalsIgnoreCase(page)) {
            tiers(player);
        } else if ("titles".equalsIgnoreCase(page) || "title".equalsIgnoreCase(page)) {
            titles(player);
        } else if ("team".equalsIgnoreCase(page) || "teams".equalsIgnoreCase(page)) {
            teams(player);
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
        send(player, Component.m_237113_("§8── §fLegacy Mechanics · Difficulty §8──"));
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
        send(player, Component.m_237113_("§cWarning: §7Scaled mobs can attack other players as well"));
        send(player, Component.m_237113_(""));
        MutableComponent hub = Component.m_237113_("§7")
                .m_7220_(btn("§e[Tiers]", "/difficulty do page tiers",
                        "Buy higher · lower unlocked · reset to None"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§d[Titles]", "/difficulty do page titles", "Equip difficulty titles"));
        send(player, hub);
        MutableComponent toggles = Component.m_237113_("§7")
                .m_7220_(btn(personalOn ? "§a[Difficulty ON]" : "§c[Difficulty OFF]",
                        "/difficulty do toggle_personal 0 main",
                        personalOn
                                ? "Turn OFF — no scaling, kill coins, AI pressure, or tier buys"
                                : "Turn ON — restore scaling. Scaled mobs can attack other players as well"))
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
        send(player, btn("§7« Hub", "/lmdo lm open hub", "Main menu"));
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void tiers(ServerPlayer player) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        UnlockSystem.syncUnlocks(player, data);
        int level = DmzProgression.tierScalingDmzLevel(player);
        int active = data.getActiveTier();
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8──────── §eDifficulty Tiers §8────────"));
        if (!data.isPersonalEnabled()) {
            send(player, Component.m_237113_("§cPersonal difficulty is OFF — turn it ON on the main menu to change tiers."));
            send(player, btn("§7« Back", "/difficulty do page main", "Return"));
            return;
        }
        int prestige = DmzProgression.prestige(player);
        send(player, Component.m_237113_("§7Current §f" + (active <= 0 ? "None" : ("T" + active))
                + "  §8·  §7DMZ §f" + level
                + "  §8·  §7Prestige §f" + prestige));
        send(player, Component.m_237113_("§8Buy higher (coins) · lower unlocked (free) · reset to None."));
        send(player, Component.m_237113_("§8Unlock with §fDMZ level §8OR §fPrestige §8(either one). Costs scale with level."));
        send(player, Component.m_237113_("§f" + AncientCoinEconomy.inventoryBreakdown(player)));
        send(player, Component.m_237113_(""));
        send(player, btn("§c[Reset to None]", "/difficulty do lower_tier 0 tiers", "Clear active tier"));
        for (UnlockTier tier : UnlockTier.values()) {
            long cost = AncientCoinEconomy.activationCost(tier, player);
            boolean freeCost = PaidFeatureAccess.bypassAncientCoinCost(player);
            boolean unlocked = data.hasUnlockedTier(tier.id);
            boolean activeHere = active == tier.id;
            boolean canLower = unlocked && tier.id < active;
            MutableComponent line = Component.m_237113_(
                    (activeHere ? "§a● " : unlocked ? "§e" : "§8")
                            + "T" + tier.id + " " + tier.display);
            if (activeHere) {
                line = line.m_7220_(Component.m_237113_(" §aCURRENT"));
            } else if (canLower) {
                line = line.m_7220_(Component.m_237113_(" "))
                        .m_7220_(btn("§f[LOWER]", "/difficulty do activate " + tier.id + " tiers",
                                "Lower to Tier " + tier.id + " (free)"));
            } else if (unlocked) {
                String costLabel = freeCost ? "§afree (staff)" : ("§6" + AncientCoinEconomy.formatExactCost(cost));
                line = line.m_7220_(Component.m_237113_(" " + costLabel + " "))
                        .m_7220_(btn("§a[BUY]", "/difficulty do activate " + tier.id + " tiers",
                                freeCost
                                        ? "Activate Tier " + tier.id + " (staff — no coin charge)"
                                        : "Pay Ancient Coins for Tier " + tier.id + " (pay-up OK, change returned)"));
            } else {
                line = line.m_7220_(Component.m_237113_(
                        " §cLOCKED §8(" + tier.requirementTip() + ")"));
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
        send(player, Component.m_237113_("§7Unlock with DMZ level or Prestige — keeps after lowering tier."));
        send(player, Component.m_237113_("§7Swap freely. Rarity adds landing relief · AD damage · TP."));
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

    private static void teams(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8──────── §bRival Teams §8────────"));
        for (String line : com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi.linesForPage(player, "team")) {
            send(player, Component.m_237113_(line));
        }
        send(player, Component.m_237113_(""));
        send(player, btn("§7Personal", "/difficulty do team personal team", "Solo ceiling only"));
        send(player, btn("§aThreshold", "/difficulty do team threshold team", "Extra max + harder spawns"));
        send(player, btn("§2Full", "/difficulty do team full team", "Threshold + nearby spare + best spawn boost"));
        send(player, btn("§6Open Rival", "/lmdo lm open rival", "Manage mutual rivals"));
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
                + "  §8·  §7Style §f" + profile.style.name()));
        send(player, Component.m_237113_("§7Top stats §f" + profile.topStatsLabel()));
        send(player, Component.m_237113_("§7Unlocked §fT" + snap.highestUnlockedTier
                + "  §8·  §7Title §e" + TitleSystem.activeDisplay(player)));
        send(player, Component.m_237113_("§6Ancient Coins §f" + AncientCoinEconomy.inventoryBreakdown(player)));
        send(player, Component.m_237113_(
                "§8Counters: class · top stat · kits cadence"));
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
        send(player, Component.m_237113_(
                "§7Staff Ancient Coin pricing §8→ §f/lm §7→ Progression → Ancient Coins"));
        send(player, Component.m_237113_("§7Tier cost anchor §f" + (long) cfg.tierCostLevelAnchor
                + " §8· T7 target §f" + cfg.tierCostT7TargetCopper
                + " copper §8(stock 150000 → 100× Netherite)"));
        send(player, Component.m_237113_("§8/difficulty admin set tierCostLevelAnchor|tierCostT7TargetCopper <n>"));
        send(player, Component.m_237113_("§7Coin drops §f" + cfg.enableAncientCoinDrops
                + "  §7Death reset §f" + cfg.deathResetsActiveDifficulty));
        send(player, Component.m_237113_("§7Drop chance §f" + cfg.ancientCoinDropChance
                + "  §7Dual upgrade §f" + cfg.ancientCoinUpgradeChance
                + " §8(stock 0.05 / 0.005)"));
        send(player, Component.m_237113_("§8/difficulty admin set ancientCoinDropChance|ancientCoinUpgradeChance <0-1>"));
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
