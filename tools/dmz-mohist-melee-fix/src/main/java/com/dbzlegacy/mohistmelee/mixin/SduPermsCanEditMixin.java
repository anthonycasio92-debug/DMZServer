package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.MohistStaffAccess;
import net.minecraft.world.entity.player.Player;
import net.shurui.dev.sdu.util.SduPerms;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * SDU {@code SduPerms.canEdit} is only {@code player.hasPermissions(2)}.
 * Open/save/delete editor packets return silently when that is false.
 */
@Mixin(value = SduPerms.class, remap = false)
public abstract class SduPermsCanEditMixin {

    @Inject(method = "canEdit", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dbzlegacy$mohistStaffCanEdit(
            Player player, CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValue() != null && cir.getReturnValue()) {
            return;
        }
        if (MohistStaffAccess.canEditSdu(player)) {
            cir.setReturnValue(true);
        }
    }
}
