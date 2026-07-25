package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatUnlock;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Portal / {@code /execute in} / world-travel uses {@code ServerPlayer.changeDimension}.
 * That is the spawn-world → other-world path players hit even outside raids.
 */
@Mixin(ServerPlayer.class)
public abstract class ServerPlayerChangeDimensionMixin {

    @Inject(
            method = "m_5489_(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;",
            at = @At("RETURN"),
            remap = false
    )
    private void dbzlegacy$afterChangeDimension(
            ServerLevel destination,
            CallbackInfoReturnable<Entity> cir
    ) {
        CombatUnlock.unlockAfterTeleport((ServerPlayer) (Object) this, "change-dimension");
    }
}
