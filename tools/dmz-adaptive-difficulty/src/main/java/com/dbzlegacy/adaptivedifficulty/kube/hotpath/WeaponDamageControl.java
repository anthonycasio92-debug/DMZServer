package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Same lookup as {@code getWeaponDamageMultiplier} in dmz_weapon_bonus_scale.js.
 * Exact item ids win over {@code family:<name>}. {@code 1.0} leaves damage unchanged.
 * {@code 0} zeroes the damage figure used for scaling; family stat profiles still apply.
 */
public final class WeaponDamageControl {
    public static final double DEFAULT = 1.0;

    private static final Map<String, Double> OVERRIDES;

    static {
        Map<String, Double> overrides = new LinkedHashMap<>();
        // Add overrides here. Exact ids beat family keys.
        // overrides.put("minecraft:netherite_sword", 1.5);
        // overrides.put("family:dagger", 0.7);
        OVERRIDES = Collections.unmodifiableMap(overrides);
    }

    private WeaponDamageControl() {}

    public static double multiplier(String itemId, String family) {
        if (itemId == null || itemId.isEmpty()) {
            return DEFAULT;
        }
        Double exact = OVERRIDES.get(itemId);
        if (exact != null && Double.isFinite(exact)) {
            return exact;
        }
        if (family != null && !family.isEmpty()) {
            Double familyMultiplier = OVERRIDES.get("family:" + family);
            if (familyMultiplier != null && Double.isFinite(familyMultiplier)) {
                return familyMultiplier;
            }
        }
        return DEFAULT;
    }
}
