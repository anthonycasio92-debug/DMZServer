package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatUnlock;
import com.dbzlegacy.mohistmelee.ReachAttributeFix;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ {@code handle()} drops all M1 when {@code Status.isStunned()} is true, and that
 * includes stale {@code strikeLocked}. Unlock the gate, then call the original
 * {@code processAttackRequest} which still uses vanilla {@code ServerPlayer.attack}
 * (no damage redirect — NPCs keep normal hurt/death).
 */
@Mixin(value = CombatAttackRequestC2S.class, remap = false)
public abstract class CombatAttackRequestHandleMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void dbzlegacy$unlockHandle(Supplier<NetworkEvent.Context> ctx, CallbackInfo ci) {
        ci.cancel();
        NetworkEvent.Context context = ctx.get();
        CombatAttackRequestC2S self = (CombatAttackRequestC2S) (Object) this;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            CombatUnlock.clearStaleStrikeLock(player, "melee-packet");
            ReachAttributeFix.repair(player, "melee-packet");
            if (CombatUnlock.hasRealStunPotion(player)) {
                return;
            }
            CombatAttackRequestC2S.processAttackRequest(player, self);
        });
        context.setPacketHandled(true);
    }
}
