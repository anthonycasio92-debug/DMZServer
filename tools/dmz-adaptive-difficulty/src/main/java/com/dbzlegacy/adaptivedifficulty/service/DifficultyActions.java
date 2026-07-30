package com.dbzlegacy.adaptivedifficulty.service;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.currency.CurrencyBridge;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.network.DifficultyNet;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Shared server-side actions used by commands and GUI packets. */
public final class DifficultyActions {
    public static final String ACT_UP = "up";
    public static final String ACT_DOWN = "down";
    public static final String ACT_BUY = "buy";
    public static final String ACT_TEAM = "team";
    public static final String ACT_SET = "set";
    public static final String ACT_SET_MAX = "set_max";
    public static final String ACT_REFRESH = "refresh";

    private DifficultyActions() {}

    public static void openGui(ServerPlayer player, String page) {
        DifficultyNet.openScreen(player, page == null || page.isBlank() ? "main" : page);
    }

    public static Result handle(ServerPlayer player, String action, long amount, String page) {
        if (player == null || action == null) {
            return Result.fail("Invalid action.");
        }
        return switch (action.toLowerCase()) {
            case ACT_UP -> adjust(player, Math.max(1L, amount <= 0 ? 100L : amount), page);
            case ACT_DOWN -> adjust(player, -Math.max(1L, amount <= 0 ? 100L : amount), page);
            case ACT_BUY -> buy(player, Math.max(1L, amount <= 0 ? 100L : amount), page);
            case ACT_TEAM -> cycleTeam(player, page);
            case ACT_SET -> setActive(player, Math.max(0L, amount), page);
            case ACT_SET_MAX -> setActive(player, Long.MAX_VALUE, page);
            case ACT_REFRESH -> {
                openGui(player, page);
                yield Result.ok("");
            }
            default -> Result.fail("Unknown action: " + action);
        };
    }

    private static Result adjust(ServerPlayer player, long delta, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        DifficultySnapshot before = DifficultyCache.refresh(player);
        long next = Math.max(0L, before.active + delta);
        next = Math.min(next, before.availableMax);
        data.setActiveDifficulty(next);
        DifficultyCache.save(player);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Active difficulty set to " + snap.active + " (max " + snap.availableMax + ")");
    }

    private static Result setActive(ServerPlayer player, long amount, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        DifficultySnapshot bounds = DifficultyCache.refresh(player);
        long next = amount == Long.MAX_VALUE ? bounds.availableMax : Math.max(0L, Math.min(amount, bounds.availableMax));
        data.setActiveDifficulty(next);
        DifficultyCache.save(player);
        DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Active difficulty set to " + next);
    }

    private static Result buy(ServerPlayer player, long amount, String page) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        long cost = DifficultyCalculator.purchaseCost(data.getPurchasedDifficulty(), amount);
        if (!CurrencyBridge.canAfford(player, cost)) {
            openGui(player, page);
            return Result.fail("Need " + CurrencyBridge.formatCost(cost) + " (" + CurrencyBridge.currencyLabel()
                    + "). Balance: " + CurrencyBridge.balanceText(player));
        }
        if (!CurrencyBridge.charge(player, cost)) {
            openGui(player, page);
            return Result.fail("Payment failed. Balance: " + CurrencyBridge.balanceText(player));
        }
        data.setPurchasedDifficulty(data.getPurchasedDifficulty() + amount);
        DifficultyCache.save(player);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        openGui(player, page);
        return Result.ok("Purchased +" + amount + " for " + CurrencyBridge.formatCost(cost)
                + ". Purchased total: " + snap.purchased + " | Available max: " + snap.availableMax);
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
