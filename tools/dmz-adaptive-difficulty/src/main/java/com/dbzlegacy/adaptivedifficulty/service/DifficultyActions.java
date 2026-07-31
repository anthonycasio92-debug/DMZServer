package com.dbzlegacy.adaptivedifficulty.service;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import net.minecraft.server.level.ServerPlayer;

/**
 * V3 player actions only:
 * buy/activate tier, lower, reset, team, character reset, page/refresh.
 * No +difficulty upgrades, titles, or set-max.
 */
public final class DifficultyActions {
    public static final String ACT_DOWN = "down";
    public static final String ACT_BUY = "buy";
    public static final String ACT_ACTIVATE = "activate";
    public static final String ACT_PURCHASE_TIER = "purchase_tier";
    public static final String ACT_TEAM = "team";
    public static final String ACT_RESET = "reset";
    public static final String ACT_CHARACTER_RESET = "character_reset";
    public static final String ACT_REFRESH = "refresh";
    public static final String ACT_PAGE = "page";

    private DifficultyActions() {}

    public static void openGui(ServerPlayer player, String page) {
        String target = page == null || page.isBlank() ? "main" : page;
        UnlockSystem.syncUnlocks(player, DifficultyCache.data(player));
        DifficultyMenu.open(player, target);
    }

    public static Result handle(ServerPlayer player, String action, long amount, String page) {
        return handleArg(player, action, String.valueOf(amount), page);
    }

    public static Result handleArg(ServerPlayer player, String action, String arg, String page) {
        if (player == null || action == null) {
            return Result.fail("Invalid action.");
        }
        String act = action.toLowerCase();
        if (ACT_PAGE.equals(act) || ACT_REFRESH.equals(act)) {
            openGui(player, page);
            return Result.ok("");
        }
        // Removed legacy actions — refuse clearly.
        if ("up".equals(act) || "upgrade".equals(act) || "set_max".equals(act)
                || "equip_title".equals(act) || "clear_title".equals(act) || "equip".equals(act)
                || "unequip_title".equals(act)) {
            openGui(player, page == null || page.isBlank() ? "buy" : page);
            return Result.fail("That feature was removed. Purchase a tier to raise difficulty.");
        }
        // Character-wipe hook must still work while the system is offline.
        if (ACT_CHARACTER_RESET.equals(act) || "char_reset".equals(act) || "characterreset".equals(act)) {
            return characterReset(player, page);
        }
        // Master admin switch — block gameplay actions while the system is off.
        if (!DifficultyConfig.isEnabled()) {
            openGui(player, page == null || page.isBlank() ? "main" : page);
            return Result.fail("Adaptive Difficulty is disabled by an admin.");
        }

        long amount = 0L;
        if (arg != null && !arg.isBlank()) {
            try {
                amount = Long.parseLong(arg.trim());
            } catch (NumberFormatException ignored) {
                if (ACT_DOWN.equals(act) || ACT_ACTIVATE.equals(act)
                        || ACT_PURCHASE_TIER.equals(act) || ACT_BUY.equals(act) || "set".equals(act)) {
                    return Result.fail("Invalid amount: " + arg);
                }
            }
        }
        return switch (act) {
            case ACT_DOWN -> lower(player, Math.max(1L, amount <= 0 ? 100L : amount), page);
            case "set" -> setLowerOnly(player, Math.max(0L, amount), page);
            case ACT_ACTIVATE, ACT_PURCHASE_TIER, ACT_BUY -> activateTier(player, (int) amount, page);
            case ACT_TEAM -> cycleTeam(player, page);
            case ACT_RESET, "zero", "clear" -> resetActive(player, page);
            default -> Result.fail("Unknown action.");
        };
    }

    private static Result activateTier(ServerPlayer player, int tierId, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        UnlockSystem.syncUnlocks(player, data);
        UnlockTier tier = UnlockTier.byId(tierId);
        String returnPage = page == null || page.isBlank() ? "buy" : page;
        if (tier == null) {
            openGui(player, returnPage);
            return Result.fail("Unknown tier. Use 1–7.");
        }
        if (!data.hasUnlockedTier(tier.id)) {
            openGui(player, returnPage);
            return Result.fail("Tier " + tier.id + " locked. Need DMZ " + tier.requiredDmzLevel()
                    + " or Prestige " + tier.id + ".");
        }
        if (data.getActiveTier() == tier.id) {
            openGui(player, returnPage);
            return Result.ok("Tier " + tier.id + " already active.");
        }
        long cost = AncientCoinEconomy.activationCost(tier);
        String costText = AncientCoinEconomy.formatExactCost(cost);
        if (!AncientCoinEconomy.canAfford(player, cost)) {
            openGui(player, returnPage);
            return Result.fail("Need " + costText + " (have "
                    + AncientCoinEconomy.balanceText(player) + ").");
        }
        if (!AncientCoinEconomy.charge(player, cost)) {
            openGui(player, returnPage);
            return Result.fail("Payment failed — need " + costText + " in inventory.");
        }
        data.setActiveTier(tier.id);
        data.setActiveDifficultyLevel(tier.maxDifficulty());
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        DifficultySnapshot snap = DifficultyCache.get(player);
        openGui(player, returnPage);
        return Result.ok("Purchased " + tier.display + " (T" + tier.id + ") for " + costText
                + ". Difficulty " + snap.active + " / " + snap.availableMax
                + " · CR " + snap.combatRating);
    }

    private static Result lower(ServerPlayer player, long amount, String page) {
        DifficultySnapshot before = DifficultyCache.get(player);
        long next = Math.max(0L, before.active - amount);
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.setActiveDifficultyLevel(next);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Difficulty lowered to " + next + " (free).");
    }

    /** Free set only when lowering or equal; raising requires tier purchase. */
    private static Result setLowerOnly(ServerPlayer player, long target, String page) {
        DifficultySnapshot bounds = DifficultyCache.get(player);
        if (target > bounds.active) {
            openGui(player, "buy");
            return Result.fail("Buy a higher tier in the Tier menu to raise difficulty.");
        }
        target = Math.max(0L, Math.min(target, bounds.availableMax));
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.setActiveDifficultyLevel(target);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        openGui(player, page == null || page.isBlank() ? "main" : page);
        return Result.ok("Active difficulty set to " + target + " (free).");
    }

    private static Result resetActive(ServerPlayer player, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.resetTemporary();
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Active difficulty cleared. Unlocks and Ancient Coins kept.");
    }

    private static Result characterReset(ServerPlayer player, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.resetTemporary();
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        if (page != null && !page.isBlank()) {
            openGui(player, page);
        }
        return Result.ok("Character reset: active tier/level cleared. Prestige, unlocks, and Ancient Coins kept.");
    }

    private static Result cycleTeam(ServerPlayer player, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.cycleTeamMode();
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        DifficultySnapshot snap = DifficultyCache.get(player);
        openGui(player, page);
        return Result.ok("Team mode: " + snap.teamMode.displayName()
                + ". Active " + snap.active + " / " + snap.availableMax);
    }

    public static final class Result {
        public final boolean ok;
        public final String message;

        private Result(boolean ok, String message) {
            this.ok = ok;
            this.message = message == null ? "" : message;
        }

        public boolean ok() {
            return ok;
        }

        /** Accessor used by the Bukkit GUI ForgeBridge (reflection). */
        public String message() {
            return message == null ? "" : message;
        }

        public void tell(ServerPlayer player) {
            if (player == null || message == null || message.isBlank()) {
                return;
            }
            player.m_213846_(net.minecraft.network.chat.Component.m_237113_(
                    (ok ? "§a" : "§c") + message));
        }

        public static Result ok(String message) {
            return new Result(true, message);
        }

        public static Result fail(String message) {
            return new Result(false, message);
        }
    }
}
