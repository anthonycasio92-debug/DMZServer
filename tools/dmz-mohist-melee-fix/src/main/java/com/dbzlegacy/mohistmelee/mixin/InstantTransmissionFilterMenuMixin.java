package com.dbzlegacy.mohistmelee.mixin;

import com.dragonminez.common.network.C2S.RequestITTargetsC2S;
import com.dragonminez.common.network.ITTargetEntry;
import com.dragonminez.common.network.S2C.OpenITMenuS2C;
import java.util.ArrayList;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * IT menu only lists masters — strips party/external player targets.
 */
@Mixin(value = RequestITTargetsC2S.class, remap = false)
public abstract class InstantTransmissionFilterMenuMixin {

    @Redirect(
            method = "lambda$handle$0",
            at = @At(
                    value = "NEW",
                    target = "com/dragonminez/common/network/S2C/OpenITMenuS2C"
            ),
            remap = false
    )
    private static OpenITMenuS2C dbzlegacy$mastersOnly(List<ITTargetEntry> entries) {
        List<ITTargetEntry> masters = new ArrayList<>();
        if (entries != null) {
            for (ITTargetEntry entry : entries) {
                if (entry != null && entry.getType() == ITTargetEntry.Type.MASTER) {
                    masters.add(entry);
                }
            }
        }
        return new OpenITMenuS2C(masters);
    }
}
