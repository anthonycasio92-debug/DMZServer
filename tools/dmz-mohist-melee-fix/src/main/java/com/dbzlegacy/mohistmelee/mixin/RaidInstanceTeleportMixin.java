package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatUnlock;
import net.minecraft.server.level.ServerPlayer;
import net.shurui.dev.shuruis_raid_bosses.region.Region;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Belt-and-suspenders for Shurui raid arena/home teleports when {@code healOnStart} is false
 * (no {@code fullHeal}) or return paths that call {@code returnToStart} directly.
 * <p>
 * Handler args must match the target exactly — {@code Object} for {@code Region} fails mixin apply
 * (seen in production logs with shuruis_raid_bosses 2.7.2).
 */
@Mixin(targets = "net.shurui.dev.shuruis_raid_bosses.raid.RaidInstance", remap = false)
public abstract class RaidInstanceTeleportMixin {

    @Inject(method = "teleport", at = @At("RETURN"), remap = false)
    private void dbzlegacy$afterRaidTeleport(ServerPlayer player, Region region, CallbackInfo ci) {
        CombatUnlock.unlockAfterTeleport(player, "shurui-teleport");
    }

    @Inject(method = "teleportToArena", at = @At("RETURN"), remap = false)
    private void dbzlegacy$afterTeleportToArena(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        CombatUnlock.unlockAfterTeleport(player, "shurui-teleport-arena");
    }

    @Inject(method = "returnToStart", at = @At("RETURN"), remap = false)
    private void dbzlegacy$afterRaidReturn(ServerPlayer player, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ()) {
            CombatUnlock.unlockAfterTeleport(player, "shurui-return");
        }
    }
}
