package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatUnlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shurui raids (and other mods) relocate players with {@code ServerPlayer.m_8999_(ServerLevel,...)}.
 * Same-dimension arena teleports do not fire {@code PlayerChangedDimensionEvent}, so join/dim
 * unlock never runs. Force-clear stale strike locks + repair attributes after any such teleport.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerTeleportUnlockMixin {

    @Inject(
            method = "m_8999_(Lnet/minecraft/server/level/ServerLevel;DDDFF)V",
            at = @At("RETURN"),
            remap = false
    )
    private void dbzlegacy$afterServerTeleport(
            ServerLevel level,
            double x,
            double y,
            double z,
            float yaw,
            float pitch,
            CallbackInfo ci
    ) {
        CombatUnlock.unlockAfterTeleport((ServerPlayer) (Object) this, "server-teleport");
    }
}
