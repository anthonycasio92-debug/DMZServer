package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.end.EndDimensionStrength;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/** Difficulty — Home overview / Tiers / Summons; sub-pages for teams, titles, settings. */
public final class UltraDifficultyScreens {
    private UltraDifficultyScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        DifficultyActions.prepareGui(player);
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        switch (p) {
            case "tiers", "buy", "tier", "purchase", "unlock", "adjust", "change", "set", "lower" ->
                    openTiers(player);
            case "summons", "summon" -> openSummons(player);
            case "end_dragon", "dragon", "summon_dragon", "summon_confirm" -> openSummonConfirm(player);
            case "team", "teams" -> openTeam(player);
            case "titles", "title" -> openTitles(player);
            case "settings", "coins" -> openSettings(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("difficulty")
                .title("Difficulty")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "tiers|Tiers", "summons|Summons");
    }

    /** Overview only — titles / rival teams / coin settings. Tabs hold Tiers & Summons. */
    private static void openHome(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean personal = data != null && data.isPersonalEnabled();
        String tierName = snap == null || snap.activeTier <= 0 ? "None" : snap.activeTierName;
        UltraScreenSpec spec = base("main", "home")
                .labelValue("Personal difficulty", personal ? "On" : "Off")
                .labelValue("Active tier", tierName)
                .labelValue("Title", blank(TitleSystem.activeDisplay(player)))
                .labelValue("Ancient Coins", AncientCoinEconomy.inventoryBreakdown(player))
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(
                spec, "titles|Titles", "team|Rival teams", "settings|Coin settings");
        if (StaffAccess.isStaff(player)) {
            spec.buttonLeft("open:admin", UltraStyle.danger("Staff Admin"), "danger").rowGap();
        }
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openTiers(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerDifficultyData data = DifficultyCache.data(player);
        int active = data == null ? 0 : data.getActiveTier();
        int max = snap == null ? 0 : Math.max(0, snap.highestUnlockedTier);
        UltraScreenSpec spec = base("tiers", "tiers")
                .labelValue("Active tier", active <= 0 ? "None" : (snap == null ? "T" + active : snap.activeTierName))
                .labelValue("Unlocked up to", "T" + max)
                .labelValue("Ancient Coins", AncientCoinEconomy.inventoryBreakdown(player))
                .notice("Click a tier again within 10 seconds to confirm.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 90);
        for (int t = 1; t <= 7; t += 2) {
            UnlockTier left = UnlockTier.byId(t);
            UnlockTier right = UnlockTier.byId(t + 1);
            if (left != null) {
                boolean on = active == t;
                String label = on
                        ? UltraStyle.confirm("T" + t + " " + left.display + " · active")
                        : "T" + t + " " + left.display;
                spec.buttonLeft("do:activate:" + t, label, on ? "confirm" : "normal");
            }
            if (right != null) {
                boolean on = active == t + 1;
                String label = on
                        ? UltraStyle.confirm("T" + (t + 1) + " " + right.display + " · active")
                        : "T" + (t + 1) + " " + right.display;
                spec.buttonRight("do:activate:" + (t + 1), label, on ? "confirm" : "normal");
            }
            spec.rowGap();
        }
        spec.buttonLeft("do:lower_tier:0", UltraStyle.danger("Clear active tier"), "danger")
                .buttonRight("do:toggle_personal", UltraStyle.confirm("Toggle personal"), "normal")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    /** Summons tab — boss list. "Summon End Dragon" opens the confirm sub-page. */
    private static void openSummons(ServerPlayer player) {
        boolean ready = EndDimensionStrength.canOpenSummonMenu(player);
        boolean free = StaffAccess.isStaff(player);
        String cost = free
                ? "free for staff"
                : AncientCoinEconomy.formatExactCost(EndDimensionStrength.summonCopperCost());
        UltraScreenSpec spec = base("summons", "summons")
                .line(UltraStyle.LABEL + "Select a boss, then confirm. More bosses can be added to this list.");
        for (String line : EndDimensionStrength.summonMenuLines(player)) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        spec.labelValue("End Dragon", ready ? "Ready · " + cost : "Requirements not met")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.buttonLeft(
                        "page:end_dragon",
                        UltraStyle.confirm("Summon End Dragon"),
                        "confirm")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    /** End Dragon confirm sub-page — opened from the Summons list. */
    private static void openSummonConfirm(ServerPlayer player) {
        boolean ready = EndDimensionStrength.canOpenSummonMenu(player);
        boolean free = StaffAccess.isStaff(player);
        String cost = free
                ? "free for staff"
                : AncientCoinEconomy.formatExactCost(EndDimensionStrength.summonCopperCost());
        UltraScreenSpec spec = base("end_dragon", "summons")
                .line(UltraStyle.LABEL + "End Dragon")
                .labelValue("Cost", cost)
                .labelValue("Status", ready ? "Ready" : "Requirements not met")
                .line(UltraStyle.LABEL + "Travel to The End, then confirm the summon.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        if (ready) {
            spec.button(
                            "do:summon_end_dragon",
                            UltraStyle.confirm("Confirm summon"),
                            "confirm")
                    .rowGap();
        } else {
            spec.button(
                            "do:summon_end_dragon",
                            UltraStyle.dim("Confirm summon · locked"),
                            "dim")
                    .rowGap();
        }
        spec.button("page:summons", UltraStyle.dim("Back to summons"), "dim").rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openTeam(ServerPlayer player) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean personal = data != null && data.isPersonalEnabled();
        TeamMode mode = data == null ? TeamMode.PERSONAL_ONLY : data.getTeamMode();
        UltraScreenSpec spec = base("team", "home");
        for (String line : DifficultyTeamGuiApi.linesForPage(player, "team")) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        if (!personal) {
            spec.notice("Enable personal difficulty before changing team modes.")
                    .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
            spec.buttonLeft("do:toggle_personal", UltraStyle.confirm("Personal difficulty"), "confirm")
                    .rowGap();
            spec.footerBackHome();
            spec.send(player);
            return;
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.buttonLeft(
                        "do:mode:personal",
                        mode == TeamMode.PERSONAL_ONLY
                                ? UltraStyle.confirm("Personal · on")
                                : "Personal",
                        mode == TeamMode.PERSONAL_ONLY ? "confirm" : "normal")
                .buttonRight(
                        "do:mode:threshold",
                        mode == TeamMode.THRESHOLD_BONUS_ONLY
                                ? UltraStyle.confirm("Threshold · on")
                                : "Threshold",
                        mode == TeamMode.THRESHOLD_BONUS_ONLY ? "confirm" : "normal")
                .rowGap();
        spec.buttonLeft(
                        "do:mode:full",
                        mode == TeamMode.FULL_TEAM_SCALING
                                ? UltraStyle.confirm("Full team · on")
                                : "Full team",
                        mode == TeamMode.FULL_TEAM_SCALING ? "confirm" : "normal")
                .buttonRight("do:toggle_personal", UltraStyle.confirm("Toggle personal"), "normal")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openTitles(ServerPlayer player) {
        String active = TitleSystem.activeId(player);
        UltraScreenSpec spec = base("titles", "home")
                .labelValue("Active title", blank(TitleSystem.activeDisplay(player)))
                .labelValue("Title score", String.valueOf(TitleSystem.computeTitleScore(player)))
                .line(UltraStyle.LABEL + "Equip a title you have unlocked.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        int placed = 0;
        for (DifficultyTitle t : DifficultyTitle.values()) {
            if (placed >= 8 || t == null) {
                continue;
            }
            boolean earned = TitleSystem.has(player, t);
            boolean on = t.id != null && t.id.equals(active);
            String label = on
                    ? UltraStyle.confirm(t.display + " · equipped")
                    : (earned ? t.display : UltraStyle.dim(t.display + " · locked"));
            String style = on ? "confirm" : (earned ? "normal" : "dim");
            String action = "do:equip_title:" + t.id;
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, style);
            } else {
                spec.buttonRight(action, label, style);
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        spec.buttonLeft("do:clear_title", UltraStyle.danger("Unequip title"), "danger")
                .buttonRight("do:toggle_title_sense", "Title Sense", "normal")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openSettings(ServerPlayer player) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean coinChat = data != null && data.isCoinDropChat();
        UltraScreenSpec spec = base("settings", "home")
                .line(UltraStyle.LABEL + "Coin drop messages in chat.")
                .labelValue("Coin messages", coinChat ? "On" : "Off")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        spec.button(
                        "do:toggle_coin_chat",
                        coinChat
                                ? UltraStyle.confirm("Coin messages · ON")
                                : UltraStyle.dim("Coin messages · OFF"),
                        coinChat ? "confirm" : "dim")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static String blank(String s) {
        if (s == null || s.isBlank() || "None".equalsIgnoreCase(s.trim())) {
            return "—";
        }
        return s;
    }
}
