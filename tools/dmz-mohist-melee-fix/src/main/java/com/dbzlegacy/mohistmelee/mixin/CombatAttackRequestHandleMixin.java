package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatRepair;
import com.dbzlegacy.mohistmelee.MeleeRescue;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import java.util.function.Supplier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CombatAttackRequestC2S.class}, remap=false)
public abstract class CombatAttackRequestHandleMixin {
    @Inject(method={"handle"}, at={@At(value="HEAD")}, cancellable=true, remap=false)
    private void dmzmmf$unlockHandle(Supplier<NetworkEvent.Context> supplier, CallbackInfo ci) {
        ci.cancel();
        NetworkEvent.Context context = supplier.get();
        CombatAttackRequestC2S packet = (CombatAttackRequestC2S)(Object)this;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            CombatRepair.clearCombatLock(player, "melee-packet", true);
            CombatRepair.sanitizeReach(player, "melee-packet");
            CombatRepair.snapshotPrimaries((Player)player);
            CombatRepair.restorePrimaries((Player)player, "melee-packet");
            if (player.m_21205_().m_41619_() && CombatRepair.isPrimaryWiped(player)) {
                CombatRepair.respawnLikeRecovery(player, "melee-empty-hand");
            }
            if (CombatRepair.hasRealStunPotion(player)) {
                return;
            }
            long hitTimeBefore = 0L;
            try {
                hitTimeBefore = ((net.minecraft.nbt.CompoundTag) player.getClass().getMethod("getPersistentData").invoke(player)).m_128454_("dmz_last_hit_target_time");
            } catch (Throwable ignored) {
            }
            final long hitBefore = hitTimeBefore;
            int packetIdCount = packet.getEntityIds() == null ? 0 : packet.getEntityIds().length;
            CombatAttackRequestC2S.processAttackRequest((ServerPlayer)player, (CombatAttackRequestC2S)packet);
            MinecraftServer server = player.m_20194_();
            if (server != null) {
                server.execute(() -> MeleeRescue.maybeRescue(player, packet, hitBefore, packetIdCount));
            } else {
                MeleeRescue.maybeRescue(player, packet, hitBefore, packetIdCount);
            }
        });
        context.setPacketHandled(true);
    }
}

