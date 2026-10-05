package com.dbzlegacy.adaptivedifficulty.progression.tp;

import com.dbzlegacy.adaptivedifficulty.util.LmChat;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dbzlegacy.adaptivedifficulty.util.ScreenNotify;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Temporary death penalty: TP gain is cut in half for 10 minutes.
 * <p>
 * The timer is stored on the player, so a relog or restart keeps the remaining
 * time. A later death refreshes the window back to 10 minutes. The cut is
 * applied once, on the training points DragonMineZ is about to grant, so a
 * global TP boost is halved too instead of stacking into a second formula.
 */
public final class DeathTpPenalty {
    public static final double MULTIPLIER = 0.5d;
    public static final long DURATION_MS = 10L * 60L * 1000L;
    /** Copied onto the respawned player. Forge does not always keep persistent data across death. */
    public static final String KEY = "lm.death_tp_penalty_until";
    /** Leftover half-point so a 1 TP hit still becomes half a point over two hits. */
    public static final String FRAC_KEY = "lm.death_tp_penalty_frac";
    private static final String BAR_KEY = "lm.death.tp.bar";
    private static final long BAR_COOLDOWN_MS = 8_000L;
    /** Depth of an {@code addTrainingPoints} call that already applied the cut. */
    private static final ThreadLocal<Integer> GRANT_DEPTH = ThreadLocal.withInitial(() -> 0);

    private DeathTpPenalty() {}

    public static void enterGrant() {
        GRANT_DEPTH.set(1);
    }

    public static void exitGrant() {
        GRANT_DEPTH.set(0);
    }

    public static boolean inGrant() {
        return GRANT_DEPTH.get() > 0;
    }

    /** 0.5 while the penalty is active, otherwise 1. */
    public static double multiplier(ServerPlayer player) {
        if (!active(player)) {
            return 1.0d;
        }
        return MULTIPLIER;
    }

    /**
     * Whole training points after the death cut, for callers that never go through
     * {@code addTrainingPoints}. Does not touch the leftover fraction.
     */
    public static int applyToGain(ServerPlayer player, int gain) {
        if (player == null || gain <= 0 || !active(player)) {
            return gain;
        }
        int whole = (int) Math.floor(gain * MULTIPLIER);
        return Math.max(0, whole);
    }

    /**
     * Same cut for a raw {@code addTrainingPoints} amount that never reaches
     * {@code calculateTPGain} (Mohist leaves the resources player null).
     */
    public static float applyToAmount(ServerPlayer player, float amount) {
        if (player == null || !(amount > 0f) || !active(player)) {
            return amount;
        }
        double acc = amount * MULTIPLIER + readFrac(player);
        int whole = (int) Math.floor(acc + 1.0e-6);
        if (whole < 0) {
            whole = 0;
        }
        writeFrac(player, Math.max(0.0d, acc - whole));
        return whole;
    }

    public static boolean active(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return PersistentDataAccess.getLong(player, KEY, 0L) > System.currentTimeMillis();
    }

    public static void onDeath(ServerPlayer player) {
        if (player == null) {
            return;
        }
        boolean refreshed = active(player);
        long until = System.currentTimeMillis() + DURATION_MS;
        PersistentDataAccess.putLong(player, KEY, until);
        String body = refreshed
                ? "§cDeath penalty refreshed. §7TP gain stays at §chalf §7for §f10 minutes§7."
                : "§cDeath penalty. §7TP gain is cut to §chalf §7for §f10 minutes§7.";
        msg(player, LmChat.note("TP", body));
        ScreenNotify.actionBar(player, "TP gain halved", "10m left", "", 0L);
    }

    public static void onLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!active(player)) {
            clearExpired(player, false);
            return;
        }
        msg(player, LmChat.note(
                "TP",
                "§cDeath penalty §7is still on. TP gain is §chalf §7for §f"
                        + remainingLabel(player) + "§7."));
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null || tick % 20 != 0) {
            return;
        }
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null) {
                continue;
            }
            long until = PersistentDataAccess.getLong(player, KEY, 0L);
            if (until <= 0L) {
                continue;
            }
            if (until <= System.currentTimeMillis()) {
                clearExpired(player, true);
                continue;
            }
            ScreenNotify.actionBar(
                    player,
                    "TP gain halved",
                    remainingLabel(player) + " left",
                    BAR_KEY,
                    BAR_COOLDOWN_MS);
        }
    }

    public static String remainingLabel(ServerPlayer player) {
        long rem = Math.max(0L, PersistentDataAccess.getLong(player, KEY, 0L) - System.currentTimeMillis());
        long totalSec = rem / 1000L;
        long minutes = totalSec / 60L;
        long sec = totalSec % 60L;
        if (minutes <= 0L) {
            return sec + "s";
        }
        return minutes + "m " + sec + "s";
    }

    private static void clearExpired(ServerPlayer player, boolean announce) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag) || !tag.m_128441_(KEY)) {
            return;
        }
        tag.m_128473_(KEY);
        tag.m_128473_(FRAC_KEY);
        if (announce) {
            msg(player, LmChat.ok("TP", "Death penalty ended. TP gain is back to normal."));
        }
    }

    private static double readFrac(ServerPlayer player) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag) || !tag.m_128441_(FRAC_KEY)) {
            return 0.0d;
        }
        try {
            double v = tag.m_128459_(FRAC_KEY);
            if (!Double.isFinite(v) || v <= 0.0d) {
                return 0.0d;
            }
            return Math.min(v, 1.0d);
        } catch (Throwable ignored) {
            return 0.0d;
        }
    }

    private static void writeFrac(ServerPlayer player, double frac) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag)) {
            return;
        }
        if (!Double.isFinite(frac) || frac < 0.001d) {
            tag.m_128473_(FRAC_KEY);
            return;
        }
        tag.m_128347_(FRAC_KEY, Math.min(frac, 0.999d));
    }

    private static void msg(ServerPlayer player, String text) {
        try {
            com.dbzlegacy.adaptivedifficulty.util.DmzRewards.msg(player, text);
        } catch (Throwable ignored) {
        }
    }
}
