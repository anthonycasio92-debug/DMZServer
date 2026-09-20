package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.MohistStaffAccess;
import net.minecraft.commands.CommandSourceStack;
import net.shurui.dev.sdu.command.DmzNpcCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@code /sdu} {@code requires} is {@code CommandSourceStack.hasPermission(2)}
 * ({@code DmzNpcCommand.lambda$register$0}). Mixin is a second gate next to
 * {@link com.dbzlegacy.mohistmelee.SduEditCommandAccess} wrapping the node.
 */
@Mixin(value = DmzNpcCommand.class, remap = false)
public abstract class SduCommandRequiresMixin {

    @Inject(method = "lambda$register$0", at = @At("RETURN"), cancellable = true, remap = false)
    private static void dbzlegacy$mohistStaffCanRunSdu(
            CommandSourceStack source, CallbackInfoReturnable<Boolean> cir
    ) {
        if (cir.getReturnValue() != null && cir.getReturnValue()) {
            return;
        }
        if (MohistStaffAccess.canEditSdu(source)) {
            cir.setReturnValue(true);
        }
    }
}
