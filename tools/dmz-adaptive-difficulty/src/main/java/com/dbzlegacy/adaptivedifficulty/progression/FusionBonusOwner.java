package com.dbzlegacy.adaptivedifficulty.progression;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import com.dragonminez.common.stats.character.Status;
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Links a {@link BonusStats} map back to the {@link StatsData} that owns it so a
 * read can see {@link Status#isFused()} without BonusStats holding a player.
 * The map is weak in both directions: the key does not pin the bonus object, and
 * the value does not pin the stats object.
 */
public final class FusionBonusOwner {
    private static final Map<BonusStats, WeakReference<StatsData>> OWNERS =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<Boolean> RAW_READ = new ThreadLocal<>();

    private FusionBonusOwner() {}

    /**
     * Record the owner of a bonus map that the caller already has.
     * Never calls back into {@code StatsData} bonus reads: the read mixin is on
     * that getter, so a lookup here recurses until the stack overflows.
     */
    public static void remember(StatsData data, BonusStats bonuses) {
        if (data == null || bonuses == null) {
            return;
        }
        WeakReference<StatsData> existing = OWNERS.get(bonuses);
        if (existing != null && existing.get() == data) {
            return;
        }
        OWNERS.put(bonuses, new WeakReference<>(data));
    }

    /**
     * True when this map belongs to a player who is fused right now.
     * An unknown owner stays fused so a live fusion is not stripped if the
     * link has not been recorded yet.
     */
    public static boolean isFused(BonusStats bonuses) {
        if (bonuses == null) {
            return true;
        }
        WeakReference<StatsData> ref = OWNERS.get(bonuses);
        StatsData data = ref == null ? null : ref.get();
        if (data == null) {
            return true;
        }
        try {
            Status status = data.getStatus();
            return status != null && status.isFused();
        } catch (Throwable ignored) {
            return true;
        }
    }

    /** Unfuse cleanup has to see the stored key so it can delete it. */
    public static boolean rawRead() {
        return Boolean.TRUE.equals(RAW_READ.get());
    }

    public static void runRaw(Runnable action) {
        boolean previous = Boolean.TRUE.equals(RAW_READ.get());
        RAW_READ.set(true);
        try {
            action.run();
        } finally {
            if (previous) {
                RAW_READ.set(true);
            } else {
                RAW_READ.remove();
            }
        }
    }
}
