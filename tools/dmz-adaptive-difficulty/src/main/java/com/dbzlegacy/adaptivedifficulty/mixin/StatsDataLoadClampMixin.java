package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dbzlegacy.adaptivedifficulty.progression.StatsDataLoadContext;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Marks DMZ stats NBT load — no ki clamp here (clamp on login via {@code ProgressionSystem}).
 * Running {@code DmzResourcePoolClamp} during load caused Mohist join failures
 * (Invalid player data).
 */
@Mixin(value = StatsData.class, remap = false)
public abstract class StatsDataLoadClampMixin {
    /** Stops the cancelled head call from re-entering the guard. */
    private static final ThreadLocal<Boolean> REENTRY = ThreadLocal.withInitial(() -> Boolean.FALSE);

    /**
     * A RETURN inject does not run when {@code load} throws, which left the
     * load flag stuck on that thread. Cancel the outer call and run the real
     * load inside try/finally.
     */
    @Inject(method = "load", at = @At("HEAD"), cancellable = true, remap = false)
    private void lm$loadGuarded(CompoundTag tag, CallbackInfo ci) throws ClassNotFoundException {
        if (REENTRY.get()) {
            return;
        }
        ci.cancel();
        REENTRY.set(Boolean.TRUE);
        try {
            StatsDataLoadContext.enter();
            try {
                ((StatsData) (Object) this).load(tag);
            } finally {
                StatsDataLoadContext.exit();
            }
        } finally {
            REENTRY.set(Boolean.FALSE);
        }
    }
}
