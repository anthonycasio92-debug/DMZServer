package com.dbzlegacy.adaptivedifficulty.service;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player actions: buy/lower tiers, titles, reset.
 * Difficulty points are not player-facing — only Unlock Tiers.
 */
public final class DifficultyActions {
    public static final String ACT_BUY = "buy";
    public static final String ACT_ACTIVATE = "activate";
    public static final String ACT_PURCHASE_TIER = "purchase_tier";
    public static final String ACT_LOWER_TIER = "lower_tier";
    public static final String ACT_TEAM = "team";
    public static final String ACT_RESET = "reset";
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
        if (ACT_PAGE.equals(act) || ACT_REFRESH.equals(act)) {
            openGui(player, page);
            return Result.ok("");
        }
        if (ACT_EQUIP_TITLE.equals(act) || "equip".equals(act)) {
            return equipTitle(player, arg, page == null || page.isBlank() ? "titles" : page);
        }
        if (ACT_CLEAR_TITLE.equals(act) || "unequip_title".equals(act)) {
            TitleSystem.clear(player);
            openGui(player, page == null || page.isBlank() ? "titles" : page);
            return Result.ok("Title unequipped.");
        }
        if ("up".equals(act) || "upgrade".equals(act) || "set_max".equals(act)
                || "down".equals(act) || "set".equals(act)) {
            openGui(player, page == null || page.isBlank() ? "buy" : page);
            return Result.fail("Difficulty points were removed — buy or lower Unlock Tiers instead.");
        }
        if (ACT_CHARACTER_RESET.equals(act) || "char_reset".equals(act) || "characterreset".equals(act)) {
            return characterReset(player, page);
        }
        if (!DifficultyConfig.isEnabled()) {
            openGui(player, page == null || page.isBlank() ? "main" : page);
            return Result.fail("Adaptive Difficulty is disabled by an admin.");
        }
        if (!SystemGate.allows(player)) {
            openGui(player, page == null || page.isBlank() ? "main" : page);
            return Result.fail("Adaptive Difficulty is whitelist-only right now. Ask an admin to add you.");
        }
        if (ACT_TEAM.equals(act)) {
            openGui(player, page == null || page.isBlank() ? "main" : page);
            return Result.fail("Team scaling is a work in progress — difficulty is personal only for now.");
        }

        long amount = 0L;
        if (arg != null && !arg.isBlank()) {
            try {
                amount = Long.parseLong(arg.trim());
            } catch (NumberFormatException ignored) {
                if (ACT_ACTIVATE.equals(act) || ACT_PURCHASE_TIER.equals(act)
                        || ACT_BUY.equals(act) || ACT_LOWER_TIER.equals(act)) {
                    return Result.fail("Invalid tier: " + arg);
                }
            }
        }
        return switch (act) {
            case ACT_ACTIVATE, ACT_PURCHASE_TIER, ACT_BUY -> setTier(player, (int) amount, page);
            case ACT_LOWER_TIER -> lowerTier(player, (int) amount, page);
            case ACT_RESET, "zero", "clear" -> resetActive(player, page);
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
            return Result.fail("Title locked: " + title.display + " §8(" + title.requirementTip() + ")");
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

    /**
     * Raise to a higher tier (paid, level-scaled) or switch to a lower unlocked tier (free).
     */
    private static Result setTier(ServerPlayer player, int tierId, String page) {
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
        int current = data.getActiveTier();
        if (current == tier.id) {
            openGui(player, returnPage);
            return Result.ok("Already on " + tier.display + " (T" + tier.id + ").");
        }

        // Lowering / lateral via buy menu is free.
        if (tier.id < current) {
            applyTier(data, player, tier);
            openGui(player, returnPage);
            return Result.ok("Lowered to " + tier.display + " (T" + tier.id + ") — free.");
        }

        long cost = AncientCoinEconomy.activationCost(tier, player);
        String costText = AncientCoinEconomy.formatExactCost(cost);
        if (!AncientCoinEconomy.canAfford(player, cost)) {
            openGui(player, returnPage);
            return Result.fail(AncientCoinEconomy.missingText(player, cost));
        }
        if (!AncientCoinEconomy.charge(player, cost)) {
            openGui(player, returnPage);
            return Result.fail(AncientCoinEconomy.missingText(player, cost));
        }
        applyTier(data, player, tier);
        TitleSystem.syncTierTitles(player, true);
        DifficultySnapshot snap = DifficultyCache.get(player);
        openGui(player, returnPage);
        return Result.ok("Purchased " + tier.display + " (T" + tier.id + ") for " + costText
                + ". CR " + snap.combatRating);
    }

    private static Result lowerTier(ServerPlayer player, int tierId, String page) {
        String returnPage = page == null || page.isBlank() ? "lower" : page;
        if (tierId <= 0) {
            return resetActive(player, returnPage);
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        UnlockTier tier = UnlockTier.byId(tierId);
        if (tier == null) {
            openGui(player, returnPage);
            return Result.fail("Unknown tier. Use 1–7.");
        }
        if (tier.id > data.getActiveTier()) {
            openGui(player, "buy");
            return Result.fail("Buy a higher tier to raise difficulty.");
        }
        if (!data.hasUnlockedTier(tier.id)) {
            openGui(player, returnPage);
            return Result.fail("Tier " + tier.id + " is not unlocked.");
        }
        if (data.getActiveTier() == tier.id) {
            openGui(player, returnPage);
            return Result.ok("Already on " + tier.display + ".");
        }
        applyTier(data, player, tier);
        openGui(player, returnPage);
        return Result.ok("Lowered to " + tier.display + " (T" + tier.id + ") — free.");
    }

    private static void applyTier(PlayerDifficultyData data, ServerPlayer player, UnlockTier tier) {
        data.setActiveTier(tier.id);
        // Internal CR scale only — not shown as player-facing "points".
        data.setActiveDifficultyLevel(tier.maxDifficulty());
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
    }

    private static Result resetActive(ServerPlayer player, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.resetTemporary();
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Difficulty cleared. Unlocks and Ancient Coins kept.");
    }

    private static Result characterReset(ServerPlayer player, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.resetTemporary();
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        if (page != null && !page.isBlank()) {
            openGui(player, page);
        }
        return Result.ok("Character reset: active tier cleared. Prestige, unlocks, and Ancient Coins kept.");
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
