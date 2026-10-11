package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.progression.end.EndDimensionStrength;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/** Difficulty system — Home / Tiers / Summons. */
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
            case "summons", "summon", "end_dragon", "dragon", "summon_dragon" -> openSummons(player);
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
                .setRow(120);
        spec.buttonLeft("tab:tiers", UltraStyle.confirm("Tiers"), "normal")
                .buttonRight("tab:summons", "Summons", "normal")
                .rowGap();
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
                .setRow(120);
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

    private static void openSummons(ServerPlayer player) {
        boolean ready = EndDimensionStrength.canOpenSummonMenu(player);
        UltraScreenSpec spec = base("summons", "summons")
                .line(UltraStyle.LABEL + "Select a boss, then confirm.")
                .labelValue("End Dragon", ready ? "Ready" : "Requirements not met")
                .setRow(100);
        spec.button("do:summon_end_dragon", UltraStyle.confirm("Summon End Dragon"), "confirm")
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
