package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatUnlock;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Shurui {@code DmzHooks.fullHeal} clears knockedDown/stunEffect and effects, but leaves
 * {@code strikeLocked} set — which still gates all M1 packets via {@code Status.isStunned()}.
 * Run after heal so the final stats sync includes the unlock.
 */
@Mixin(targets = "net.shurui.dev.shuruis_raid_bosses.dmz.DmzHooks", remap = false)
public abstract class DmzHooksFullHealMixin {

    @Inject(method = "fullHeal", at = @At("RETURN"), remap = false)
    private static void dbzlegacy$afterRaidFullHeal(Player player, CallbackInfo ci) {
        if (player instanceof ServerPlayer sp) {
            CombatUnlock.unlockAfterTeleport(sp, "shurui-fullHeal");
        }
    }
}
