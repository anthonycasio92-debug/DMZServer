package com.dbzlegacy.adaptivedifficulty.service;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyMenu;
import com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Shared server-side actions used by chat GUI click handlers. */
public final class DifficultyActions {
    public static final String ACT_UP = "up";
    public static final String ACT_DOWN = "down";
    public static final String ACT_BUY = "buy";
    public static final String ACT_TEAM = "team";
    public static final String ACT_SET = "set";
    public static final String ACT_SET_MAX = "set_max";
    public static final String ACT_RESET = "reset";
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
        DifficultyMenu.open(player, target);
    }

    public static Result handle(ServerPlayer player, String action, long amount, String page) {
        return handleArg(player, action, String.valueOf(amount), page);
    }

    /**
     * String-arg entry point for GUI actions (supports title ids as well as numeric amounts).
     */
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
                if (ACT_UP.equals(act) || ACT_DOWN.equals(act) || ACT_BUY.equals(act) || ACT_SET.equals(act)) {
                    return Result.fail("Invalid amount: " + arg);
                }
            }
        }
        return switch (act) {
            case ACT_UP -> raise(player, Math.max(1L, amount <= 0 ? 100L : amount), page);
            case ACT_DOWN -> lower(player, Math.max(1L, amount <= 0 ? 100L : amount), page);
            case ACT_BUY -> buy(player, Math.max(1L, amount <= 0 ? 100L : amount), page);
            case ACT_TEAM -> cycleTeam(player, page);
            case ACT_SET -> setActivePaid(player, Math.max(0L, amount), page);
            case ACT_SET_MAX -> setActivePaid(player, Long.MAX_VALUE, page);
            case ACT_RESET, "zero", "clear" -> resetActive(player, page);
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
        // Clicking the already-equipped title unequips it.
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

    private static void afterActiveChanged(ServerPlayer player) {
        TitleSystem.syncTierTitles(player, true);
    }

    /** Lowering is always free. */
    private static Result lower(ServerPlayer player, long amount, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        DifficultySnapshot before = DifficultyCache.refresh(player);
        long next = Math.max(0L, before.active - amount);
        data.setActiveDifficulty(next);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        AreaDifficulty.clearCache();
        afterActiveChanged(player);
        openGui(player, page);
        return Result.ok("Active difficulty set to " + next + " (free).");
    }

    /** Raising always costs Lightman's iron coins (scaled). */
    private static Result raise(ServerPlayer player, long amount, String page) {
        DifficultySnapshot before = DifficultyCache.refresh(player);
        long room = Math.max(0L, before.availableMax - before.active);
        if (room <= 0) {
            openGui(player, page);
            return Result.fail("Already at your max (" + before.availableMax
                    + "). Theoretical (stats): " + before.calculated
                    + ". Buy more max, level up / prestige, or enable team scaling.");
        }
        long raiseBy = Math.min(amount, room);
        return chargeAndRaise(player, before.active, raiseBy, page);
    }

    private static Result setActivePaid(ServerPlayer player, long amount, String page) {
        DifficultySnapshot bounds = DifficultyCache.refresh(player);
        long target = amount == Long.MAX_VALUE
                ? bounds.availableMax
                : Math.max(0L, Math.min(amount, bounds.availableMax));
        if (target <= bounds.active) {
            // Lower or no-op — free
            PlayerDifficultyData data = DifficultyCache.data(player);
            data.setActiveDifficulty(target);
            DifficultyCache.save(player);
            DifficultyCache.refresh(player);
            AreaDifficulty.clearCache();
            afterActiveChanged(player);
            openGui(player, page);
            return Result.ok("Active difficulty set to " + target + (target < bounds.active ? " (free)." : "."));
        }
        long raiseBy = target - bounds.active;
        return chargeAndRaise(player, bounds.active, raiseBy, page);
    }

    private static Result chargeAndRaise(ServerPlayer player, long fromActive, long raiseBy, String page) {
        if (raiseBy <= 0) {
            openGui(player, page);
            return Result.ok("Already at that difficulty.");
        }
        if (!CurrencyBridge.lightmansAvailable()) {
            openGui(player, page);
            return Result.fail("Lightman's Currency is required to raise difficulty.");
        }
        long cost = DifficultyCalculator.raiseCostIronCoins(fromActive, raiseBy);
        if (!CurrencyBridge.canAfford(player, cost)) {
            openGui(player, page);
            return Result.fail("Need " + CurrencyBridge.formatCost(cost)
                    + " in inventory. Have: " + CurrencyBridge.balanceText(player));
        }
        if (!CurrencyBridge.charge(player, cost)) {
            openGui(player, page);
            return Result.fail("Payment failed. Inventory: " + CurrencyBridge.balanceText(player));
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        long next = fromActive + raiseBy;
        data.setActiveDifficulty(next);
        DifficultyCache.save(player);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        AreaDifficulty.clearCache();
        afterActiveChanged(player);
        openGui(player, page);
        return Result.ok("Raised +" + raiseBy + " for " + CurrencyBridge.formatCost(cost)
                + ". Active: " + snap.active + " / " + snap.availableMax);
    }

    /** Free — does not refund purchases or spent raise costs. */
    private static Result resetActive(ServerPlayer player, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.setActiveDifficulty(0L);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        AreaDifficulty.clearCache();
        afterActiveChanged(player);
        openGui(player, page);
        return Result.ok("Active difficulty reset to 0 (purchased max unchanged).");
    }

    /** Unlock more max difficulty (purchased). Always Lightman's iron coins. */
    private static Result buy(ServerPlayer player, long amount, String page) {
        if (!CurrencyBridge.lightmansAvailable()) {
            openGui(player, page);
            return Result.fail("Lightman's Currency is required to buy difficulty.");
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        long cost = DifficultyCalculator.purchaseCost(data.getPurchasedDifficulty(), amount);
        if (!CurrencyBridge.canAfford(player, cost)) {
            openGui(player, page);
            return Result.fail("Need " + CurrencyBridge.formatCost(cost)
                    + " in inventory. Have: " + CurrencyBridge.balanceText(player));
        }
        if (!CurrencyBridge.charge(player, cost)) {
            openGui(player, page);
            return Result.fail("Payment failed. Inventory: " + CurrencyBridge.balanceText(player));
        }
        data.setPurchasedDifficulty(data.getPurchasedDifficulty() + amount);
        DifficultyCache.save(player);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        AreaDifficulty.clearCache();
        openGui(player, page);
        return Result.ok("Purchased +" + amount + " max for " + CurrencyBridge.formatCost(cost)
                + ". Purchased: " + snap.purchased + " | Available max: " + snap.availableMax);
    }

    private static Result cycleTeam(ServerPlayer player, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.cycleTeamMode();
        DifficultyCache.save(player);
        DifficultyCache.invalidateAll();
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Team mode: " + snap.teamMode);
    }

    public static Result setTeam(ServerPlayer player, TeamMode mode, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        data.setTeamMode(mode);
        DifficultyCache.save(player);
        DifficultyCache.invalidateAll();
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Team mode: " + snap.teamMode);
    }

    public record Result(boolean ok, String message) {
        public static Result ok(String message) {
            return new Result(true, message == null ? "" : message);
        }

        public static Result fail(String message) {
            return new Result(false, message == null ? "Failed." : message);
        }

        public void tell(ServerPlayer player) {
            if (message == null || message.isBlank()) {
                return;
            }
            String color = ok ? "§a" : "§c";
            player.m_213846_(Component.m_237113_(color + message));
        }
    }
}
