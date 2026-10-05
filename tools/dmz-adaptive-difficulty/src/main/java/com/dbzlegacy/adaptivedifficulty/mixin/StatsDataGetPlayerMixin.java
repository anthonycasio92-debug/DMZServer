package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.StatsDataLoadContext;
import com.dragonminez.common.stats.StatsData;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mohist often leaves {@link StatsData}'s final {@code player} field null. {@code getMaxEnergy()}
 * then skips the {@code MAX_ENERGY} attribute and returns the 1× pool while the client HUD
 * (which has a player) shows Overhaul/form-scaled max — regen +1 then clamps back.
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataGetPlayerMixin {
    private static final Map<StatsData, UUID> OWNER_UUID =
            Collections.synchronizedMap(new WeakHashMap<>());
    /** Skip the online-player scan after a miss. Regen and HUD call getPlayer() every tick. */
    private static final Map<StatsData, Long> MISS_UNTIL =
            Collections.synchronizedMap(new WeakHashMap<>());
    private static final long NEGATIVE_TTL_NANOS = 500_000_000L;

    @Inject(method = "getPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$resolveOwner(CallbackInfoReturnable<Player> cir) {
        if (StatsDataLoadContext.inLoad()) {
            return;
        }
        Player existing = cir.getReturnValue();
        StatsData self = (StatsData) (Object) this;
        if (existing instanceof ServerPlayer sp && sp.m_6084_()) {
            OWNER_UUID.put(self, sp.m_20148_());
            MISS_UNTIL.remove(self);
            return;
        }
        if (existing != null && existing.m_6084_()) {
            MISS_UNTIL.remove(self);
            return;
        }
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        UUID cached = OWNER_UUID.get(self);
        if (cached != null) {
            ServerPlayer sp = server.m_6846_().m_11259_(cached);
            if (sp != null && sp.m_6084_()) {
                MISS_UNTIL.remove(self);
                cir.setReturnValue(sp);
                return;
            }
        }
        long now = System.nanoTime();
        Long missUntil = MISS_UNTIL.get(self);
        if (missUntil != null && now < missUntil) {
            return;
        }
        for (ServerPlayer sp : server.m_6846_().m_11314_()) {
            if (sp == null || !sp.m_6084_()) {
                continue;
            }
            try {
                if (DmzProgression.stats(sp) == self) {
                    OWNER_UUID.put(self, sp.m_20148_());
                    MISS_UNTIL.remove(self);
                    cir.setReturnValue(sp);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
        MISS_UNTIL.put(self, now + NEGATIVE_TTL_NANOS);
    }
}
