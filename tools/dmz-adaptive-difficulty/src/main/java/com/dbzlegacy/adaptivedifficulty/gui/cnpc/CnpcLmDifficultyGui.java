package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.LmOverhaulScaledCombat;
import com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

public final class CnpcLmDifficultyGui {
    private static final int H = 320;

    private CnpcLmDifficultyGui() {}

    public static void open(ServerPlayer player, String page) {
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        if (p.startsWith("title_detail:")) {
            String titleId = raw.substring("title_detail:".length()).trim();
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_DIFFICULTY, CnpcGuiSupport.W, 320, (pl, gui) ->
                    paintTitleDetail(pl, gui, titleId));
            return;
        }
        if (("stats".equals(p) || "statistics".equals(p) || "details".equals(p)) && !StaffAccess.isStaff(player)) {
            p = "main";
        }
        String pageFinal = p;
        int height = switch (pageFinal) {
            case "stats", "statistics", "details" -> 340;
            case "titles", "title" -> 360;
            default -> H;
        };
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_DIFFICULTY, CnpcGuiSupport.W, height, (pl, gui) -> {
            switch (pageFinal) {
                case "tiers", "buy", "tier", "purchase", "unlock", "adjust", "change", "set", "lower" ->
                        paintTiers(pl, gui);
                case "titles", "title" -> paintTitles(pl, gui);
                case "team", "teams" -> paintTeam(pl, gui);
                case "stats", "statistics", "details" -> paintStats(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static ServerPlayer who(ServerPlayer viewer) {
        return CnpcGuiSupport.target(viewer);
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        DifficultySnapshot snap = DifficultyCache.refresh(subject);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§aDifficulty",
                "§7Tougher mobs, tiers, and optional titles");

        List<String> lines = new ArrayList<>();
        if (!DifficultyConfig.isEnabled()) {
            lines.add("§cDifficulty is turned off on this server.");
        } else if (!SystemGate.allows(subject)) {
            lines.add("§eThis account cannot use personal difficulty.");
        } else {
            PlayerDifficultyData data = DifficultyCache.data(subject);
            if (!data.isPersonalEnabled()) {
                lines.add("§cYour personal difficulty is off.");
            } else {
                lines.add("§7Active tier §f" + snap.activeTierName + " §8· §7Unlocked up to §fT"
                        + snap.highestUnlockedTier);
            }
            lines.add("§6Ancient Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));
            String title = TitleSystem.activeDisplay(subject);
            if (title != null && !"None".equals(title)) {
                lines.add("§7Equipped title §e" + title);
            }
            lines.add("§8Scaled enemies can hurt other players nearby.");
        }
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 3);
        CnpcGuiSupport.button(gui, 20, "§eUnlock tiers", CnpcGuiSupport.COL_L, row, () -> open(player, "tiers"));
        CnpcGuiSupport.button(gui, 21, "§dTitles", CnpcGuiSupport.COL_R, row, () -> open(player, "titles"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§bTeam scaling", CnpcGuiSupport.COL_L, row, () -> open(player, "team"));
        CnpcGuiSupport.button(gui, 23, "§7Toggle personal", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "toggle_personal", "0", "main").message(),
                () -> open(player, "main")));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§5Summon end dragon", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "summon_end_dragon", "0", "main").message(),
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 25, "§6Rival system", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmGui.open(player, "rival", "main"));
        row += 24;
        PlayerDifficultyData d = DifficultyCache.data(subject);
        boolean coinChat = d != null && d.isCoinDropChat();
        CnpcGuiSupport.button(gui, 26, coinChat ? CnpcGuiStyle.toggleOn("Coin drop chat")
                : CnpcGuiStyle.toggleOff("Coin drop chat"), CnpcGuiSupport.COL_L, row,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> DifficultyActions.handleArg(subject, "toggle_coin_chat", "0", "main").message(),
                        () -> open(player, "main")));
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.button(gui, 27, "§8Staff details", CnpcGuiSupport.COL_R, row,
                    () -> open(player, "stats"));
        }
        row += 24;
        navFooter(player, gui, row, null, subject, infoY);
    }

    private static void paintStats(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        DifficultySnapshot snap = DifficultyCache.refresh(subject);
        PlayerCombatProfile profile = PlayerCombatProfile.of(subject);
        Map<String, String> ph = new HashMap<>();
        LmOverhaulScaledCombat.putPlaceholders(ph, subject);

        long cr = snap.combatRating > 0 ? snap.combatRating : snap.calculated;
        List<String> lines = new ArrayList<>();
        lines.add("§7Tier §f" + snap.activeTierName + CnpcGuiStyle.SEP + "§7State §" + snap.stateColorCode()
                + snap.state());
        lines.add("§7Combat CR §f" + cr + CnpcGuiStyle.SEP + "§7Unlocked §fT" + snap.highestUnlockedTier);
        lines.add("§7Level §f" + snap.dmzLevel + CnpcGuiStyle.SEP + "§7Prestige §f" + snap.prestige);
        lines.add("§7Title §e" + blankNone(TitleSystem.activeDisplay(subject)));
        lines.add("§7Overhaul §f" + ph.getOrDefault("overhaul_scale", "x1") + CnpcGuiStyle.SEP + "§7Melee §f"
                + ph.getOrDefault("melee_scaled", "?") + CnpcGuiStyle.SEP + "§7Strike §f"
                + ph.getOrDefault("strike_scaled", "?"));
        lines.add("§7Ki §f" + ph.getOrDefault("ki_scaled", "?")
                + "  §7Defense §f" + ph.getOrDefault("defense_scaled", "?"));
        String fightingClass = profile.fightingClass == null ? "" : profile.fightingClass;
        lines.add("§7Class §f" + blankNone(fightingClass)
                + "  §7Style §f" + (profile.style == null ? "HYBRID" : profile.style.name()));
        lines.add("§7Top stats §f" + blankNone(profile.topStatsLabel()));
        lines.add("§6Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));

        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§8Staff · Details",
                "§7Combat rating and scaled stats (read-only)");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 3);
        row += 8;
        navFooter(player, gui, row, "main", subject, infoY);
    }

    private static String blankNone(String s) {
        if (s == null || s.isBlank() || "None".equalsIgnoreCase(s.trim())) {
            return "—";
        }
        return s;
    }

    private static void paintTiers(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        UnlockSystem.syncUnlocks(subject, DifficultyCache.data(subject));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§a", "Difficulty", "Tiers"),
                "§7Costs show before you confirm · lower tiers are free");

        DifficultySnapshot snap = DifficultyCache.refresh(subject);
        PlayerDifficultyData data = DifficultyCache.data(subject);
        int max = Math.max(0, snap.highestUnlockedTier);
        int active = data.getActiveTier();
        List<String> lines = new ArrayList<>();
        if (active <= 0) {
            lines.add("§7Active tier §fNone");
        } else {
            lines.add("§7Active tier §fT" + active + CnpcGuiStyle.SEP + snap.activeTierName);
        }
        lines.add("§7Unlocked up to §fT" + max);
        lines.add("§6Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));
        if (!PaidFeatureAccess.bypassAncientCoinCost(subject)) {
            lines.add("§8Activation cost scales with your level");
        }
        UnlockTier nextLocked = UnlockTier.byId(max + 1);
        if (nextLocked != null) {
            lines.add("§7Next tier §fT" + nextLocked.id + CnpcGuiStyle.SEP
                    + CnpcDifficultyTierUi.humanRequirement(nextLocked));
            lines.add("§7Cost to activate §f"
                    + CnpcDifficultyTierUi.formatActivationCost(subject, nextLocked));
        }
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX);

        int placed = 0;
        for (int t = 1; t <= 7; t++) {
            UnlockTier ut = UnlockTier.byId(t);
            if (ut == null) {
                continue;
            }
            boolean unlocked = data.hasUnlockedTier(t) || t <= max;
            boolean eligible = CnpcDifficultyTierUi.isEligible(subject, ut);
            String label = CnpcDifficultyTierUi.tierActionLabel(subject, t, active, unlocked, eligible);
            int col = (placed % 2 == 0) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
            if (placed > 0 && placed % 2 == 0) {
                row += CnpcGuiSupport.ROW_STEP;
            }
            int tier = t;
            if (CnpcDifficultyTierUi.tierButtonEnabled(t, active, unlocked, eligible)) {
                CnpcGuiSupport.buttonSmallFull(gui, 30 + t, label, col, row, 195,
                        () -> CnpcGuiSupport.act(
                                player,
                                () -> DifficultyActions.handleArg(subject, "activate", String.valueOf(tier), "tiers")
                                        .message(),
                                () -> open(player, "tiers")));
            } else if (active == t) {
                gui.addLabel(30 + t, CnpcGuiSupport.safeChat(label), col, row + 4, 195, 14);
            } else if (!unlocked) {
                gui.addLabel(30 + t, "§8T" + t + " · " + CnpcDifficultyTierUi.humanRequirement(ut), col, row + 2,
                        195, 12);
            } else {
                gui.addLabel(30 + t, CnpcGuiSupport.safeChat(label), col, row + 4, 195, 14);
            }
            placed++;
        }
        row += CnpcGuiSupport.ROW_STEP + 12;
        CnpcGuiSupport.button(gui, 50, "§cClear active tier", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "lower_tier", "0", "tiers").message(),
                () -> open(player, "tiers")));
        row += 28;
        navFooter(player, gui, row, "main", subject, infoY);
    }

    private static void paintTitles(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§a", "Difficulty", "Titles"),
                "§7Click for details · double-click to equip");
        List<String> header = new ArrayList<>();
        header.add("§7Wearing §e" + blankNone(TitleSystem.activeDisplay(subject)));
        header.add("§7Title score §6" + TitleSystem.computeTitleScore(subject));
        int listY = CnpcGuiSupport.paintInfoBlock(gui, infoY, header, CnpcGuiStyle.INFO_LIST_HEADER_MAX);

        List<String> cards = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String active = TitleSystem.activeId(subject);
        for (DifficultyTitle t : DifficultyTitle.values()) {
            boolean earned = TitleSystem.has(subject, t);
            String label = (earned ? "§a" : "§8") + t.display
                    + (t.id.equals(active) ? " §e(equipped)" : "");
            labels.add(label);
            cards.add(t.id + "\t" + label);
        }
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, listY, 2);
        IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, listY,
                CnpcGuiSupport.W - CnpcGuiSupport.M * 2, scrollH, labels.toArray(String[]::new));
        CnpcGuiSupport.wireScrollOpenDetail(scroll, cards, 0, id -> open(player, "title_detail:" + id));
        CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0, id -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "equip_title", id, "titles").message(),
                () -> open(player, "titles")));

        int row = CnpcGuiSupport.navRowAfterScroll(listY, scrollH);
        PlayerDifficultyData data = DifficultyCache.data(subject);
        boolean sense = data.titleProgress().titleSenseChat();
        CnpcGuiSupport.buttonSmall(gui, 20, sense ? CnpcGuiStyle.toggleOn("Title sense")
                : CnpcGuiStyle.toggleOff("Title sense"), CnpcGuiSupport.COL_L, row, 95,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> DifficultyActions.handleArg(subject, "toggle_title_sense", "0", "titles").message(),
                        () -> open(player, "titles")));
        CnpcGuiSupport.buttonSmall(gui, 21, "§cClear title", CnpcGuiSupport.COL_R, row, 95, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "clear_title", "0", "titles").message(),
                () -> open(player, "titles")));
        row += 28;
        navFooter(player, gui, row, "main", subject, infoY);
    }

    private static void paintTitleDetail(ServerPlayer player, ICustomGui gui, String titleIdRaw) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        DifficultyTitle title = DifficultyTitle.byId(titleIdRaw);
        if (title == null) {
            int infoY = CnpcGuiSupport.paintHeader(player, gui, "§dTitle", "§7Unknown entry");
            int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of("§7That title could not be found."), 2);
            navFooter(player, gui, row + 8, "titles", subject, infoY);
            return;
        }
        boolean earned = TitleSystem.has(subject, title);
        boolean equipped = title.id.equals(TitleSystem.activeId(subject));
        int mastery = DifficultyCache.data(subject).titleProgress().masteryLevel(title.id);

        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§d" + title.masteryDisplay(mastery),
                earned ? "§7Unlocked · tap Equip to wear" : "§7Locked · see how to earn it");

        List<String> lines = new ArrayList<>();
        lines.add(title.rarity.coloredLabel() + " §8· §7" + kindLabel(title.kind));
        lines.add(earned ? "§aYou have this title." : "§8You do not have this yet.");
        if (equipped) {
            lines.add("§eCurrently equipped.");
        }
        lines.add("§7How to earn §f" + humanRequirement(title));
        String perk = title.perkTip(mastery).replace(" §8| ", " · ");
        lines.add("§7Bonus when worn §f" + perk);
        lines.add("§7Score value §6+" + title.scorePoints + " §8(title score)");

        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 2);
        if (earned) {
            CnpcGuiSupport.button(gui, 20, equipped ? "§aAlready equipped" : "§aEquip this title",
                    CnpcGuiSupport.COL_L, row, () -> {
                        if (!equipped) {
                            CnpcGuiSupport.act(
                                    player,
                                    () -> DifficultyActions.handleArg(subject, "equip_title", title.id, "titles")
                                            .message(),
                                    () -> open(player, "title_detail:" + title.id));
                        } else {
                            open(player, "title_detail:" + title.id);
                        }
                    });
        } else {
            gui.addLabel(20, "§8Equip unlocks after you earn the title.", CnpcGuiSupport.COL_L, row + 4,
                    CnpcGuiSupport.BTN_W, 14);
        }
        row += 28;
        navFooter(player, gui, row, "titles", subject, infoY);
    }

    private static String kindLabel(DifficultyTitle.Kind kind) {
        return switch (kind) {
            case TIER -> "Tier milestone";
            case COMBAT -> "Combat challenge";
            case CHALLENGE -> "Special challenge";
        };
    }

    private static String humanRequirement(DifficultyTitle title) {
        String raw = title.requirementTip();
        return raw.replace("DMZ ", "Level ")
                .replace(" or Prestige ", " or prestige ")
                .replace(" §8(keeps after lowering tier)", " (stays unlocked if you lower tier)");
    }

    private static void paintTeam(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§a", "Difficulty", "Teams"),
                "§7How rival teams affect scaling");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, DifficultyTeamGuiApi.linesForPage(subject, "team"),
                CnpcGuiStyle.INFO_INLINE_MAX);
        CnpcGuiSupport.button(gui, 40, "§7Personal only", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "personal_only", "team"),
                () -> open(player, "team")));
        CnpcGuiSupport.button(gui, 41, "§eThreshold bonus", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "threshold_bonus_only", "team"),
                () -> open(player, "team")));
        row += 24;
        CnpcGuiSupport.button(gui, 42, "§aFull team scaling", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "full_team_scaling", "team"),
                () -> open(player, "team")));
        row += 24;
        navFooter(player, gui, row, "main", subject, infoY);
    }

    /** {@code parentPage} null on difficulty main; otherwise Back reopens that page. */
    private static void navFooter(
            ServerPlayer player,
            ICustomGui gui,
            int row,
            String parentPage,
            ServerPlayer previewSubject,
            int previewAnchorY) {
        if (parentPage == null) {
            CnpcGuiSupport.navSystemRoot(player, gui, row);
        } else {
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, parentPage), "§7« Back");
        }
        if (StaffAccess.isStaff(player) && parentPage == null) {
            row += 24;
            CnpcGuiSupport.buttonSmall(gui, 98, "§8Staff: /difficulty admin", CnpcGuiSupport.COL_L, row, 195, () -> {
                CnpcGuiSupport.pushMenuMessage(player,
                        "§7Full difficulty admin settings: §f/difficulty admin §7(chat command).");
                open(player, "main");
            });
        }
        if (previewSubject != null && previewAnchorY >= 0) {
            CnpcGuiSupport.paintPlayerPreviewSlot(previewSubject, gui, previewAnchorY);
        }
    }
}
