package com.dbzlegacy.mohistmelee.mixin;

import com.dbzlegacy.mohistmelee.RateLog;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import java.util.Arrays;
import net.minecraft.network.FriendlyByteBuf;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={CombatAttackRequestC2S.class}, remap=false)
public abstract class CombatAttackRequestDecodeMixin {
    private static final int DMZ_MAX = 64;
    @Shadow
    @Final
    @Mutable
    private int[] entityIds;

    @ModifyConstant(method={"<init>(Lnet/minecraft/network/FriendlyByteBuf;)V"}, constant={@Constant(intValue=64)}, remap=false)
    private int dmzmmf$raiseDecodeCap(int original) {
        return 256;
    }

    @Inject(method={"<init>(Lnet/minecraft/network/FriendlyByteBuf;)V"}, at={@At(value="RETURN")}, remap=false)
    private void dmzmmf$truncateEntityIds(FriendlyByteBuf buf, CallbackInfo ci) {
        if (this.entityIds == null || this.entityIds.length <= 64) {
            return;
        }
        int was = this.entityIds.length;
        this.entityIds = Arrays.copyOf(this.entityIds, 64);
        RateLog.info("clamp", 40, "clamped CombatAttackRequest entity ids {} -> {} (packet would have been dropped)", was, 64);
    }
}

