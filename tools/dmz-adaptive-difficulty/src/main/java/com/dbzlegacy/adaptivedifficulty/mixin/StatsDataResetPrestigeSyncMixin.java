package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.noea.AbsorptionClearLog;
import com.dbzlegacy.adaptivedifficulty.noea.MajinAbsorptionStore;
import com.dbzlegacy.adaptivedifficulty.progression.PrestigeResourceRecovery;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge;
import com.dragonminez.common.hair.CustomHair;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * {@link StatsData#resetPlayerProgress} clears dmzrevamp Overhaul prestige count on stats data.
 * Re-apply Legacy Mechanics held prestige after every intentional wipe.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataResetPrestigeSyncMixin {
    @Inject(method = "resetPlayerProgress", at = @At("RETURN"), remap = false)
    private void lm$syncOverhaulPrestigeAfterReset(
            ServerPlayer player,
            Integer keepPercent,
            boolean keepSkills,
            boolean keepTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        try {
            System.out.println("[LM] wipeAbsorption firing for " + player.m_7755_().getString());
        } catch (Throwable ignored) {
        }
        try {
            MajinAbsorptionStore.clear(player);
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                t.printStackTrace();
            }
        }
        try {
            PrestigeResourceRecovery.afterDmzStatsReset(player);
        } catch (Throwable ignored) {
        }
        try {
            DmzRevampPrestigeBridge.scheduleSyncAfterStatsReset(player);
        } catch (Throwable ignored) {
        }
    }

    /**
     * Character creation applies the race at the start of this method.
     * A different race drops the stored absorption bonus first.
     * {@code require = 0} so a signature change skips the hook.
     */
    @Inject(method = "initializeWithRaceAndClass", at = @At("HEAD"), remap = false, require = 0)
    private void lm$wipeAbsorptionBeforeRaceInit(
            String race,
            String characterClass,
            String gender,
            int hairId,
            CustomHair customHair,
            int bodyType,
            int eyesType,
            int noseType,
            int mouthType,
            int tattooType,
            float boobScale,
            String activeHeadBone,
            String hairColor,
            String bodyColor,
            String bodyColor2,
            String bodyColor3,
            String eye1Color,
            String eye2Color,
            String auraColor,
            CallbackInfo ci
    ) {
        try {
            MajinAbsorptionStore.clearIfRaceChanges((StatsData) (Object) this, race);
        } catch (Throwable t) {
            try {
                AbsorptionClearLog.failure(t);
            } catch (Throwable ignored) {
                t.printStackTrace();
            }
        }
    }
}
