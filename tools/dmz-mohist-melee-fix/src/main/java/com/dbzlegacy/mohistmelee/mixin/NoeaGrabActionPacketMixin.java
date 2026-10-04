package com.dbzlegacy.mohistmelee.mixin;

import java.util.function.Supplier;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Drop all Noea experimental grab client packets on the server. */
@Mixin(targets = "com.butterjaffa.noeabosses.GrabActionC2SPacket", remap = false)
public abstract class NoeaGrabActionPacketMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void dbzlegacy$blockExperimentalGrab(Supplier<NetworkEvent.Context> ctx, CallbackInfo ci) {
        ci.cancel();
    }
}
