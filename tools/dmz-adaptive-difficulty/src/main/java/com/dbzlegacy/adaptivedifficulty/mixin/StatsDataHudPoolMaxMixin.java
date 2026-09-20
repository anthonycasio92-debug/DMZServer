package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.stats.StatsData;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Client HUD {@code getMaxEnergy}/{@code getMaxStamina} uses Forge secondary default (20) because
 * Mohist Potentialist / Overhaul modifiers never sync. The server was adding those extras
 * (screenshot: HUD Ki 159/145 vs overlay 159/190, STM 188/101). Force the vanilla default so
 * the live cap matches the bar.
 *
 * <p>Does not write current pools — {@code setCurrentEnergy(≤1)} zeros Limit Release.
 */
@Mixin(value = StatsData.class, remap = false, priority = 2100)
public abstract class StatsDataHudPoolMaxMixin {

    @Redirect(
            method = "getMaxEnergy",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;getSecondaryAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;D)D"
            ),
            remap = false
    )
    private double lm$hudEnergyAttr(StatsData self, Attribute attr, double def) {
        return def;
    }

    @Redirect(
            method = "getMaxStamina",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/dragonminez/common/stats/StatsData;getSecondaryAttributeValue(Lnet/minecraft/world/entity/ai/attributes/Attribute;D)D"
            ),
            remap = false
    )
    private double lm$hudStaminaAttr(StatsData self, Attribute attr, double def) {
        return def;
    }
}
