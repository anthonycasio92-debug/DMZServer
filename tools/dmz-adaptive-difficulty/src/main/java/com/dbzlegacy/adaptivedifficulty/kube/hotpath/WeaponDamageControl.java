package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

/**
 * Stat-scaling multiplier from the Combat config. Exact item ids beat {@code family:<name>}.
 * {@code 1.0} leaves damage unchanged. {@code 0} zeroes the scaling figure; stat profiles still apply.
 */
public final class WeaponDamageControl {
    public static final double DEFAULT = 1.0;

    private WeaponDamageControl() {}

    public static double multiplier(String itemId, String family) {
        if (itemId == null || itemId.isEmpty()) {
            return DEFAULT;
        }
        return HotPathConfig.get().weaponDamage.multiplier(itemId, family);
    }
}
