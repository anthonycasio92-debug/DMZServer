package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatUnlock;
import com.dbzlegacy.mohistmelee.PrimaryStatRepair;
import com.dbzlegacy.mohistmelee.ReachAttributeFix;
import com.dbzlegacy.mohistmelee.RespawnLikeRecovery;
import com.dragonminez.common.init.MainAttributes;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
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
            PrimaryStatRepair.ensure(player, "melee-packet");
            // If STR still looks wiped when swinging empty-handed, run full respawn-like recovery.
            if (player.m_21205_().m_41619_() && isPrimaryWiped(player)) {
                RespawnLikeRecovery.apply(player, "melee-empty-hand");
            }
            if (CombatUnlock.hasRealStunPotion(player)) {
                return;
            }
            CombatAttackRequestC2S.processAttackRequest(player, self);
        });
        context.setPacketHandled(true);
    }

    /** True when live strength attribute base is missing/zero (ignores read-side fallback). */
    private static boolean isPrimaryWiped(ServerPlayer player) {
        try {
            Attribute str = MainAttributes.STRENGTH.get();
            if (str == null) {
                return false;
            }
            AttributeInstance inst = player.m_21051_(str);
            if (inst == null) {
                return true;
            }
            double base = inst.m_22115_();
            return !Double.isFinite(base) || base <= 0.0D;
        } catch (Throwable t) {
            return false;
        }
    }
}
