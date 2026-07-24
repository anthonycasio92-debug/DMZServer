package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.RepairEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={ServerPlayer.class})
public abstract class ServerPlayerTeleportUnlockMixin {
    @Inject(method={"teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDFF)V"}, at={@At(value="RETURN")})
    private void dmzmmf$afterTeleport(ServerLevel level, double x, double y, double z, float yaw, float pitch, CallbackInfo ci) {
        RepairEvents.onTeleport((ServerPlayer)(Object)this, "server-teleport");
    }
}

