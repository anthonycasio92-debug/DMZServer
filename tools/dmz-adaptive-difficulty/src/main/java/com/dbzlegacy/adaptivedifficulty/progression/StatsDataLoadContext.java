package com.dbzlegacy.adaptivedifficulty.progression;

/** True while DMZ {@code StatsData#load} is on the stack — avoid pool clamps / player scans. */
public final class StatsDataLoadContext {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private StatsDataLoadContext() {}

    public static boolean inLoad() {
        return DEPTH.get() > 0;
    }

    public static void enter() {
        DEPTH.set(DEPTH.get() + 1);
    }

    public static void exit() {
        int d = DEPTH.get();
        DEPTH.set(d <= 1 ? 0 : d - 1);
    }
}
