package com.dbzlegacy.adaptivedifficulty.service;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import net.minecraft.server.level.ServerPlayer;

/** V3 server-side actions for GUI / command buttons. */
public final class DifficultyActions {
    public static final String ACT_UP = "up";
    public static final String ACT_DOWN = "down";
    public static final String ACT_BUY = "buy";
    public static final String ACT_ACTIVATE = "activate";
    public static final String ACT_PURCHASE_TIER = "purchase_tier";
    public static final String ACT_TEAM = "team";
    public static final String ACT_SET = "set";
    public static final String ACT_SET_MAX = "set_max";
    public static final String ACT_RESET = "reset";
    /** Character wipe: same temporary clear as death (unlocks / prestige / coins kept). */
    public static final String ACT_CHARACTER_RESET = "character_reset";
    public static final String ACT_REFRESH = "refresh";
    public static final String ACT_PAGE = "page";
    public static final String ACT_EQUIP_TITLE = "equip_title";
    public static final String ACT_CLEAR_TITLE = "clear_title";

    private DifficultyActions() {}

    public static void openGui(ServerPlayer player, String page) {
        String target = page == null || page.isBlank() ? "main" : page;
        if ("titles".equalsIgnoreCase(target) || "title".equalsIgnoreCase(target)) {
            TitleSystem.syncTierTitles(player, true);
        }
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
        if (ACT_PAGE.equals(act)) {
            openGui(player, page);
            return Result.ok("");
        }
        if (ACT_EQUIP_TITLE.equals(act) || "equip".equals(act)) {
            return equipTitle(player, arg, page == null || page.isBlank() ? "titles" : page);
        }
        if (ACT_CLEAR_TITLE.equals(act) || "unequip_title".equals(act)) {
            return clearTitle(player, page == null || page.isBlank() ? "titles" : page);
        }
        long amount = 0L;
        if (arg != null && !arg.isBlank()) {
            try {
                amount = Long.parseLong(arg.trim());
            } catch (NumberFormatException ignored) {
                if (ACT_UP.equals(act) || ACT_DOWN.equals(act) || ACT_SET.equals(act)
                        || ACT_ACTIVATE.equals(act) || ACT_PURCHASE_TIER.equals(act) || ACT_BUY.equals(act)) {
                    return Result.fail("Invalid amount: " + arg);
                }
            }
        }
        return switch (act) {
            case ACT_UP, "upgrade" -> upgrade(player, Math.max(1L, amount <= 0 ? 100L : amount), page);
            case ACT_DOWN -> lower(player, Math.max(1L, amount <= 0 ? 100L : amount), page);
            case ACT_ACTIVATE, ACT_PURCHASE_TIER, ACT_BUY -> activateTier(player, (int) amount, page);
            case ACT_TEAM -> cycleTeam(player, page);
            case ACT_SET -> setActive(player, Math.max(0L, amount), page);
            case ACT_SET_MAX -> setActive(player, Long.MAX_VALUE, page);
            case ACT_RESET, "zero", "clear" -> resetActive(player, page);
            case ACT_CHARACTER_RESET, "char_reset", "characterreset" -> characterReset(player, page);
            case ACT_REFRESH -> {
                openGui(player, page);
                yield Result.ok("");
            }
            default -> Result.fail("Unknown action.");
        };
    }

    private static Result equipTitle(ServerPlayer player, String titleId, String page) {
        TitleSystem.syncTierTitles(player, false);
        DifficultyTitle title = DifficultyTitle.byId(titleId);
        if (title == null) {
            openGui(player, page);
            return Result.fail("Unknown title.");
        }
        if (!TitleSystem.has(player, title)) {
            openGui(player, page);
            return Result.fail("Title locked: " + title.display);
        }
        if (title.id.equals(TitleSystem.activeId(player))) {
            TitleSystem.clear(player);
            openGui(player, page);
            return Result.ok("Title unequipped.");
        }
        if (!TitleSystem.equip(player, title.id)) {
            openGui(player, page);
            return Result.fail("Could not equip " + title.display + ".");
        }
        openGui(player, page);
        return Result.ok("Equipped title: " + title.display);
    }

    private static Result clearTitle(ServerPlayer player, String page) {
        TitleSystem.clear(player);
        openGui(player, page);
        return Result.ok("Title unequipped.");
    }

    private static Result activateTier(ServerPlayer player, int tierId, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        UnlockSystem.syncUnlocks(player, data);
        UnlockTier tier = UnlockTier.byId(tierId);
        if (tier == null) {
            openGui(player, page == null || page.isBlank() ? "tiers" : page);
            return Result.fail("Unknown tier. Use 1–7.");
        }
        if (!data.hasUnlockedTier(tier.id)) {
            openGui(player, "tiers");
            return Result.fail("Tier " + tier.id + " locked. Need DMZ " + tier.requiredDmzLevel()
                    + " or Prestige " + tier.id + ".");
        }
        if (data.getActiveTier() == tier.id) {
            openGui(player, "main");
            return Result.ok("Tier " + tier.id + " already active.");
        }
        long cost = AncientCoinEconomy.activationCost(tier);
        if (!AncientCoinEconomy.canAfford(player, cost)) {
            openGui(player, "tiers");
            return Result.fail("Need " + AncientCoinEconomy.format(cost) + " Ancient Coins (have "
                    + AncientCoinEconomy.balanceText(player) + ").");
        }
        if (!AncientCoinEconomy.charge(player, cost)) {
            openGui(player, "tiers");
            return Result.fail("Payment failed.");
        }
        data.setActiveTier(tier.id);
        // Keep level if still under new ceiling; otherwise start at 0.
        if (data.getActiveDifficultyLevel() > tier.maxDifficulty()) {
            data.setActiveDifficultyLevel(0L);
        }
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        TitleSystem.syncTierTitles(player, true);
        DifficultySnapshot snap = DifficultyCache.get(player);
        openGui(player, page == null || page.isBlank() ? "main" : page);
        return Result.ok("Activated " + tier.display + " (T" + tier.id + "). Active "
                + snap.active + " / " + snap.availableMax
                + " · CR " + snap.combatRating);
    }

    private static Result upgrade(ServerPlayer player, long amount, String page) {
        DifficultySnapshot before = DifficultyCache.get(player);
        if (before.activeTier <= 0) {
            openGui(player, "tiers");
            return Result.fail("No tier active. Purchase a tier first.");
        }
        long room = Math.max(0L, before.availableMax - before.active);
        if (room <= 0L) {
            openGui(player, page);
            return Result.fail("Already at maximum for this tier/team (" + before.availableMax + ").");
        }
        long raiseBy = Math.min(amount, room);
        long cost = DifficultyCalculator.upgradeCost(before.active, raiseBy);
        if (!AncientCoinEconomy.canAfford(player, cost)) {
            openGui(player, page);
            return Result.fail("Need " + AncientCoinEconomy.format(cost) + " Ancient Coins.");
        }
        if (!AncientCoinEconomy.charge(player, cost)) {
            openGui(player, page);
            return Result.fail("Payment failed.");
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.setActiveDifficultyLevel(before.active + raiseBy);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        TitleSystem.syncTierTitles(player, true);
        DifficultySnapshot snap = DifficultyCache.get(player);
        openGui(player, page);
        return Result.ok("Difficulty +" + raiseBy + " (−" + AncientCoinEconomy.format(cost)
                + "). Now " + snap.active + " / " + snap.availableMax
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

    private static Result setActive(ServerPlayer player, long target, String page) {
        DifficultySnapshot bounds = DifficultyCache.get(player);
        if (bounds.activeTier <= 0) {
            openGui(player, "tiers");
            return Result.fail("No tier active. Purchase a tier first.");
        }
        if (target == Long.MAX_VALUE) {
            target = bounds.availableMax;
        }
        target = Math.max(0L, Math.min(target, bounds.availableMax));
        if (target <= bounds.active) {
            PlayerDifficultyData data = DifficultyCache.data(player);
            data.setActiveDifficultyLevel(target);
            DifficultyCache.save(player);
            DifficultyCache.refresh(player);
            openGui(player, page);
            return Result.ok("Active difficulty set to " + target + " (free).");
        }
        return upgrade(player, target - bounds.active, page);
    }

    private static Result resetActive(ServerPlayer player, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.resetTemporary();
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Active difficulty cleared. Unlocks and Ancient Coins kept.");
    }

    /**
     * Character reset hook for DMZ / CNPC scripts.
     * Clears temporary difficulty state; keeps prestige, unlock tiers, Ancient Coins.
     */
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
