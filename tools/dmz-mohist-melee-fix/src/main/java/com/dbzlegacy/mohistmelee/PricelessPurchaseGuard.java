package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.stats.StatsData;

/**
 * Marks the current thread as inside a player skill PURCHASE/UPGRADE packet so level-grant
 * mixins can refuse priceless tiers without affecting admin {@code /dmzskill set} or quests.
 */
public final class PricelessPurchaseGuard {
    private static final ThreadLocal<StatsData> ACTIVE_DATA = new ThreadLocal<>();

    private PricelessPurchaseGuard() {}

    public static void enter(StatsData data) {
        ACTIVE_DATA.set(data);
    }

    public static void exit() {
        ACTIVE_DATA.remove();
    }

    public static boolean isActive() {
        return ACTIVE_DATA.get() != null;
    }

    public static StatsData currentData() {
        return ACTIVE_DATA.get();
    }
}
