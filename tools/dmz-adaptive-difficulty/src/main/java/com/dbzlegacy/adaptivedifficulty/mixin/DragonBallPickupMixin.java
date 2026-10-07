package com.dbzlegacy.adaptivedifficulty.mixin;

import com.dragonminez.common.init.block.custom.DragonBallBlock;
import com.dragonminez.server.events.DragonBallsHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A placed dragon ball only reacts to a right-click when all seven of that
 * set are close enough to summon. Any other right-click, and a sneak
 * right-click even then, takes that one ball into the player's inventory.
 * {@code require = 0} skips the hook if the use method is renamed.
 */
@Mixin(value = DragonBallBlock.class, remap = false)
public abstract class DragonBallPickupMixin {

    @Inject(method = "m_6227_", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void lm$sneakPickupDragonBall(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (player == null || !player.m_6144_()) {
            return;
        }
        if (lm$depositDragonBall(state, level, pos, player)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "m_6227_", at = @At("RETURN"), cancellable = true, remap = false, require = 0)
    private void lm$pickupDragonBallWhenNotSummoning(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit,
            CallbackInfoReturnable<InteractionResult> cir
    ) {
        if (cir.getReturnValue() != InteractionResult.PASS) {
            return;
        }
        if (lm$depositDragonBall(state, level, pos, player)) {
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    /** Remove this ball and put its item in the inventory. Leftovers drop at the player. */
    private boolean lm$depositDragonBall(BlockState state, Level level, BlockPos pos, Player player) {
        if (state == null || level == null || level.f_46443_ || pos == null || player == null || player.m_5833_()) {
            return false;
        }
        if (!(level instanceof ServerLevel server)) {
            return false;
        }
        if (!(state.m_60734_() instanceof DragonBallBlock ball)) {
            return false;
        }
        if (!server.m_8055_(pos).m_60713_(ball)) {
            return false;
        }
        ItemStack stack = new ItemStack(ball);
        if (stack.m_41619_()) {
            return false;
        }
        server.m_46597_(pos, Blocks.f_50016_.m_49966_());
        try {
            DragonBallsHandler.unregisterConsumedDragonBalls(server, List.of(pos), ball.getBallSetId());
        } catch (Throwable ignored) {
        }
        if (!player.m_150109_().m_36054_(stack)) {
            player.m_36176_(stack, false);
        }
        server.m_5594_(null, pos, SoundEvents.f_12019_, SoundSource.PLAYERS, 0.4f, 1.0f);
        return true;
    }
}
