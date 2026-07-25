package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatUnlock;
import com.dbzlegacy.mohistmelee.PersistentDataAccess;
import com.dbzlegacy.mohistmelee.PrimaryStatRepair;
import com.dbzlegacy.mohistmelee.ReachAttributeFix;
import com.dbzlegacy.mohistmelee.RespawnLikeRecovery;
import com.dbzlegacy.mohistmelee.ServerMeleeFallback;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import com.dragonminez.server.events.players.combat.CombatEvent;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Unlock stale strikeLocked, then run DMZ processAttackRequest.
 * If that packet produced no LivingHurt (empty/stale client entity IDs), rescue a target
 * server-side. Still uses vanilla {@code ServerPlayer.attack}.
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
            if (player.m_21205_().m_41619_() && isPrimaryWiped(player)) {
                RespawnLikeRecovery.apply(player, "melee-empty-hand");
            }
            if (CombatUnlock.hasRealStunPotion(player)) {
                return;
            }

            long hitTimeBefore = PersistentDataAccess.get(player)
                    .m_128454_(CombatEvent.DMZ_LAST_HIT_TARGET_TIME_TAG);
            int packetIds = self.getEntityIds() == null ? 0 : self.getEntityIds().length;

            CombatAttackRequestC2S.processAttackRequest(player, self);

            // processAttackRequest itself server.execute()'s the real work — queue rescue
            // after that runnable so hit-time / LivingHurt from the packet path is visible.
            MinecraftServer server = player.m_20194_();
            if (server != null) {
                server.execute(() -> ServerMeleeFallback.maybeRescue(player, self, hitTimeBefore, packetIds));
            } else {
                ServerMeleeFallback.maybeRescue(player, self, hitTimeBefore, packetIds);
            }
        });
        context.setPacketHandled(true);
    }

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
