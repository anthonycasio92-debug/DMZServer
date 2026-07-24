package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.CombatRepair;
import com.dbzlegacy.mohistmelee.RateLog;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * resetPlayerProgress sets primaries to 0 (or a %). Without this, snapshot restore
 * treats that like a Mohist dim-wipe and puts old STR/etc back within seconds.
 */
@Mixin(value={StatsData.class}, remap=false)
public abstract class StatsDataResetMixin {

    @Inject(method={"resetPlayerProgress"}, at={@At(value="HEAD")}, remap=false)
    private void dmzmmf$suppressBeforeReset(
            ServerPlayer player,
            Integer keepPercent,
            boolean keepSkills,
            boolean keepTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        CombatRepair.dropSnapshot(player.m_20148_());
        CombatRepair.suppressRestore((net.minecraft.world.entity.player.Player)player, 100);
        RateLog.info("reset", 40, "stat reset: cleared primary snapshot player={} keepPercent={}",
                player.m_36316_().getName(), keepPercent);
    }

    @Inject(method={"resetPlayerProgress"}, at={@At(value="RETURN")}, remap=false)
    private void dmzmmf$adoptAfterReset(
            ServerPlayer player,
            Integer keepPercent,
            boolean keepSkills,
            boolean keepTail,
            CallbackInfo ci
    ) {
        if (player == null) {
            return;
        }
        CombatRepair.adoptCurrent((net.minecraft.world.entity.player.Player)player);
        CombatRepair.suppressRestore((net.minecraft.world.entity.player.Player)player, 100);
    }
}
