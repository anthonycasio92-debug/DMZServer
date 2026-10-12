package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
import net.minecraft.server.level.ServerPlayer;

/** Tier button labels and cost copy for Ultra difficulty menus. */
public final class UltraDifficultyTierUi {
    private UltraDifficultyTierUi() {}

    public static String formatActivationCost(ServerPlayer player, UnlockTier tier) {
        if (tier == null || PaidFeatureAccess.bypassAncientCoinCost(player)) {
            return UltraStyle.LABEL + "free";
        }
        long cost = AncientCoinEconomy.activationCost(tier, player);
        if (cost <= 0L) {
            return UltraStyle.DIM + "—";
        }
        return UltraStyle.VALUE + AncientCoinEconomy.formatExactCost(cost);
    }

    public static String humanRequirement(UnlockTier tier) {
        if (tier == null) {
            return "—";
        }
        return tier.requirementTip()
                .replace("DMZ ", "Level ")
                .replace(" or Prestige ", " or prestige ");
    }

    public static String tierActionLabel(
            ServerPlayer player, int t, int activeTier, boolean unlocked, boolean eligible) {
        if (t <= 0) {
            return UltraStyle.DIM + "—";
        }
        if (activeTier == t) {
            return UltraStyle.confirm("Tier " + t + " · active");
        }
        if (!unlocked) {
            if (eligible) {
                return UltraStyle.confirm(
                        "Unlock T" + t + " · " + UltraStyle.plain(formatActivationCost(player, UnlockTier.byId(t))));
            }
            return UltraStyle.dim("Tier " + t + " · locked");
        }
        if (t < activeTier) {
            return UltraStyle.LABEL + "Use T" + t + " " + UltraStyle.DIM + "(free)";
        }
        return UltraStyle.LABEL
                + "Switch T"
                + t
                + " · "
                + UltraStyle.plain(formatActivationCost(player, UnlockTier.byId(t)));
    }

    public static boolean tierButtonEnabled(int t, int activeTier, boolean unlocked, boolean eligible) {
        if (t <= 0 || t > 7) {
            return false;
        }
        if (activeTier == t) {
            return false;
        }
        if (!unlocked) {
            return eligible;
        }
        return true;
    }

    public static boolean isEligible(ServerPlayer player, UnlockTier tier) {
        if (player == null || tier == null) {
            return false;
        }
        return UnlockSystem.isEligible(player, tier);
    }
}
