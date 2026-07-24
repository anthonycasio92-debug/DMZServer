package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.DamageBridge;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsProvider;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * DMZ {@code handle()} drops CombatAttackRequest when {@code Status.isStunned()} is true.
 * That flag is also true for {@code strikeLocked}/{@code knockedDown}. Cancelled strike/PvP
 * hits on Mohist can leave {@code strikeLocked=true}, permanently eating all M1 packets
 * until death/login clears it.
 *
 * Replace handle: clear stale strike locks, only block on real STUN mob effect.
 */
@Mixin(value = CombatAttackRequestC2S.class, remap = false)
public abstract class CombatAttackRequestHandleMixin {

    @Inject(method = "handle", at = @At("HEAD"), cancellable = true, remap = false)
    private void dbzlegacy$unlockedHandle(Supplier<NetworkEvent.Context> ctx, CallbackInfo ci) {
        ci.cancel();
        NetworkEvent.Context context = ctx.get();
        CombatAttackRequestC2S self = (CombatAttackRequestC2S) (Object) this;
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }
            clearStaleStrikeLock(player);
            DamageBridge.repairAttacker(player, "melee-packet");

            // Only real STUN potion blocks M1 — not stale strikeLocked/knockedDown.
            if (hasRealStun(player)) {
                return;
            }
            StatsProvider.get(StatsCapability.INSTANCE, (Entity) player).ifPresent(stats ->
                    CombatAttackRequestC2S.processAttackRequest(player, self));
        });
        context.setPacketHandled(true);
    }

    private static boolean hasRealStun(ServerPlayer player) {
        try {
            MobEffect stun = MainEffects.STUN.get();
            return stun != null && player.m_21023_(stun);
        } catch (Throwable t) {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    private static void clearStaleStrikeLock(ServerPlayer player) {
        StatsProvider.get(StatsCapability.INSTANCE, (Entity) player).ifPresent(stats -> {
            if (!stats.getStatus().isStrikeLocked() && !stats.getStatus().isKnockedDown()) {
                return;
            }
            boolean inActiveStrike = false;
            try {
                Class<?> cls = Class.forName("com.dragonminez.server.events.players.combat.StrikeAttackHandler");
                Field active = cls.getDeclaredField("ACTIVE");
                active.setAccessible(true);
                Object map = active.get(null);
                if (map instanceof Map<?, ?> m) {
                    inActiveStrike = m.containsKey(player.m_20148_());
                }
            } catch (Throwable ignored) {
            }
            if (!inActiveStrike) {
                stats.getStatus().setStrikeLocked(false);
                stats.getStatus().setKnockedDown(false);
                stats.getStatus().setStunEffect(false);
            }
        });
    }
}
