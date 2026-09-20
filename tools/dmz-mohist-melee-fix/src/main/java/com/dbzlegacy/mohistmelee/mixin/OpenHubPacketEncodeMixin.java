package com.dbzlegacy.mohistmelee.mixin;

import net.minecraft.network.FriendlyByteBuf;
import net.shurui.dev.sdu.network.OpenHubPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * SDU 3.0.11 {@code OpenHubPacket.encode} writes nothing. Live 2.12.24 called
 * {@code DmzNet.openHub} (log: opened SDU hub for JLDK1310) and the client still
 * showed no screen. Mohist / packetfixer / connectivity drop or ignore 0-byte
 * custom payloads. Write one byte; client {@code decode} constructs an empty
 * packet and does not read the buffer.
 */
@Mixin(value = OpenHubPacket.class, remap = false)
public abstract class OpenHubPacketEncodeMixin {

    @Inject(method = "encode", at = @At("HEAD"), remap = false)
    private void dbzlegacy$nonEmptyHubPayload(FriendlyByteBuf buf, CallbackInfo ci) {
        if (buf != null) {
            buf.writeByte(1);
        }
    }
}
