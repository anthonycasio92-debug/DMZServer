package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.RepairEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value={ServerPlayer.class})
public abstract class ServerPlayerChangeDimensionMixin {
    @Inject(method={"changeDimension(Lnet/minecraft/server/level/ServerLevel;)Lnet/minecraft/world/entity/Entity;"}, at={@At(value="RETURN")}, require=0)
    private void dmzmmf$afterChangeDimension(ServerLevel level, CallbackInfoReturnable<Entity> cir) {
        RepairEvents.onTeleport((ServerPlayer)(Object)this, "change-dimension");
    }
}

