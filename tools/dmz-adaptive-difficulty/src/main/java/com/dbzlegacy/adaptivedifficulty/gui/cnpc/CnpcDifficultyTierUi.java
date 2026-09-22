package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
import net.minecraft.server.level.ServerPlayer;

/** Tier button labels and cost copy for CNPC difficulty menus. */
public final class CnpcDifficultyTierUi {
    private CnpcDifficultyTierUi() {}

    public static String formatActivationCost(ServerPlayer player, UnlockTier tier) {
        if (tier == null || PaidFeatureAccess.bypassAncientCoinCost(player)) {
            return "§7free";
        }
        long cost = AncientCoinEconomy.activationCost(tier, player);
        if (cost <= 0L) {
            return "?";
        }
        return "§6" + AncientCoinEconomy.formatExactCost(cost);
    }

    public static String humanRequirement(UnlockTier tier) {
        if (tier == null) {
            return "—";
        }
        return tier.requirementTip()
                .replace("DMZ ", "Level ")
                .replace(" or Prestige ", " or prestige ");
    }

    /**
     * Button / label for tier {@code t} on the tiers page.
     * {@code unlocked} = owned in player data; {@code activeTier} = current activation (0 = none).
     */
    public static String tierActionLabel(
            ServerPlayer player, int t, int activeTier, boolean unlocked, boolean eligible) {
        if (t <= 0) {
            return "§8—";
        }
        if (activeTier == t) {
            return "§aT" + t + " active";
        }
        if (!unlocked) {
            if (eligible) {
                return "§aT" + t + " unlock · " + formatActivationCost(player, UnlockTier.byId(t));
            }
            return "§8T" + t + " locked";
        }
        if (t < activeTier) {
            return "§fT" + t + " lower §8(free)";
        }
        return "§eT" + t + " · " + formatActivationCost(player, UnlockTier.byId(t));
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
