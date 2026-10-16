package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.character.RacialStrikeWipe;
import com.dragonminez.common.stats.techniques.TechniqueData;
import com.dragonminez.common.stats.techniques.Techniques;
import java.util.Map;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A strike the current race does not grant reads as absent. The race-change
 * wipe deletes the stored copy. This gate covers a read that happens before
 * that delete, or a leftover the delete did not reach.
 */
@Mixin(value = Techniques.class, remap = false)
public abstract class StrikeTechniqueReadGateMixin {
    @Inject(method = "getUnlockedTechniques", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$hideForeignStrikes(CallbackInfoReturnable<Map<String, TechniqueData>> cir) {
        Map<String, TechniqueData> unlocked = cir.getReturnValue();
        Map<String, TechniqueData> filtered =
                RacialStrikeWipe.filterUnlocked((Techniques) (Object) this, unlocked);
        if (filtered != unlocked) {
            cir.setReturnValue(filtered);
        }
    }

    @Inject(method = "getSelectedTechnique", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$hideForeignSelected(CallbackInfoReturnable<TechniqueData> cir) {
        TechniqueData selected = cir.getReturnValue();
        if (RacialStrikeWipe.hideSelected((Techniques) (Object) this, selected)) {
            cir.setReturnValue(null);
        }
    }
}
