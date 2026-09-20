package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
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

    @Inject(method = "getPlayer", at = @At("RETURN"), cancellable = true, remap = false)
    private void lm$resolveOwner(CallbackInfoReturnable<Player> cir) {
        Player existing = cir.getReturnValue();
        if (existing instanceof ServerPlayer sp && sp.m_6084_()) {
            OWNER_UUID.put((StatsData) (Object) this, sp.m_20148_());
            return;
        }
        if (existing != null && existing.m_6084_()) {
            return;
        }
        StatsData self = (StatsData) (Object) this;
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        UUID cached = OWNER_UUID.get(self);
        if (cached != null) {
            ServerPlayer sp = server.m_6846_().m_11259_(cached);
            if (sp != null && sp.m_6084_()) {
                cir.setReturnValue(sp);
                return;
            }
        }
        for (ServerPlayer sp : server.m_6846_().m_11314_()) {
            if (sp == null || !sp.m_6084_()) {
                continue;
            }
            try {
                if (DmzProgression.stats(sp) == self) {
                    OWNER_UUID.put(self, sp.m_20148_());
                    cir.setReturnValue(sp);
                    return;
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
