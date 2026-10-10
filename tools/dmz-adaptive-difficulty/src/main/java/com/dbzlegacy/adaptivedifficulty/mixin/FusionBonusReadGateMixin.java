package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.FusionBonusOwner;
import com.dragonminez.common.stats.character.BonusStats;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fusion stores its stat bump as a split bonus named {@code FusionBonus}.
 * Unfuse does not always delete that key. While {@link com.dragonminez.common.stats.character.Status#isFused()}
 * is false, reads pretend the key is absent. A name that contains both
 * "fusion" and "zenkai" is the same kind of leftover. Racial {@code Zenkai_}
 * bonuses are left alone. The unfuse mixin still deletes the key when it can.
 */
@Mixin(value = BonusStats.class, remap = false)
public abstract class FusionBonusReadGateMixin {
    private static final String FUSION_BONUS = "FusionBonus";

    @Shadow(remap = false)
    private Map<String, List<BonusStats.StatBonus>> bonuses;

    @Inject(method = "hasBonus", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$hideFusionHasBonus(String stat, String name, CallbackInfoReturnable<Boolean> cir) {
        if (lm$suppress(name)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "getBonuses", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$hideFusionGetBonuses(String stat, CallbackInfoReturnable<List<BonusStats.StatBonus>> cir) {
        if (lm$keepFusion()) {
            return;
        }
        List<BonusStats.StatBonus> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) {
            return;
        }
        List<BonusStats.StatBonus> kept = null;
        for (int i = 0; i < list.size(); i++) {
            BonusStats.StatBonus bonus = list.get(i);
            if (bonus != null && lm$fusionKey(bonus.name)) {
                if (kept == null) {
                    kept = new ArrayList<>(list.subList(0, i));
                }
                continue;
            }
            if (kept != null) {
                kept.add(bonus);
            }
        }
        if (kept != null) {
            cir.setReturnValue(kept);
        }
    }

    @Inject(method = "calculateBonus", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$hideFusionCalculateBonus(String stat, int base, boolean applyMultipliers, CallbackInfoReturnable<Double> cir) {
        if (lm$keepFusion() || stat == null || bonuses == null) {
            return;
        }
        List<BonusStats.StatBonus> list = bonuses.get(stat.toUpperCase(Locale.ROOT));
        if (list == null || list.isEmpty() || !lm$listHasFusion(list)) {
            return;
        }
        cir.setReturnValue(lm$withoutFusion(list, base, applyMultipliers));
    }

    private boolean lm$suppress(String name) {
        return lm$fusionKey(name) && !lm$keepFusion();
    }

    private boolean lm$keepFusion() {
        return FusionBonusOwner.rawRead() || FusionBonusOwner.isFused((BonusStats) (Object) this);
    }

    private static boolean lm$listHasFusion(List<BonusStats.StatBonus> list) {
        for (int i = 0; i < list.size(); i++) {
            BonusStats.StatBonus bonus = list.get(i);
            if (bonus != null && lm$fusionKey(bonus.name)) {
                return true;
            }
        }
        return false;
    }

    /** Same + / - / * fold as {@code BonusStats.calculateBonus}, skipping fusion keys. */
    private static double lm$withoutFusion(List<BonusStats.StatBonus> list, int base, boolean applyMultipliers) {
        double additive = 0d;
        double multiplier = 1d;
        for (int i = 0; i < list.size(); i++) {
            BonusStats.StatBonus bonus = list.get(i);
            if (bonus == null || bonus.applyMultipliers != applyMultipliers || lm$fusionKey(bonus.name)) {
                continue;
            }
            String operation = bonus.operation;
            if ("+".equals(operation)) {
                additive += bonus.value;
            } else if ("-".equals(operation)) {
                additive -= bonus.value;
            } else if ("*".equals(operation)) {
                multiplier *= bonus.value;
            }
        }
        return base * multiplier - base + additive;
    }

    private static boolean lm$fusionKey(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        if (FUSION_BONUS.equalsIgnoreCase(name)) {
            return true;
        }
        String lower = name.toLowerCase(Locale.ROOT);
        return lower.contains("fusion") && lower.contains("zenkai");
    }
}
