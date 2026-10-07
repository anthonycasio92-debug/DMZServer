package com.dbzlegacy.adaptivedifficulty.event;

import com.dragonminez.common.dragonball.DragonRadarDefinition;
import com.dragonminez.common.init.block.custom.DragonBallBlock;
import com.dragonminez.common.init.item.DragonRadarItem;
import com.dragonminez.server.events.DragonBallsHandler;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * One left-click with the radar for that ball set takes the ball into the
 * inventory. The Earth radar takes Earth balls. The Namek radar takes Namek
 * balls. A different radar leaves the ball where it is.
 */
public final class DragonBallRadarPickup {
    private DragonBallRadarPickup() {}

    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        if (event == null || event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) {
            return;
        }
        LevelSide side = LevelSide.of(event);
        if (side == null) {
            return;
        }
        try {
            ItemStack held = event.getItemStack();
            if (held == null || held.m_41619_() || !(held.m_41720_() instanceof DragonRadarItem radar)) {
                return;
            }
            BlockPos pos = event.getPos();
            if (pos == null) {
                return;
            }
            BlockState state = side.level.m_8055_(pos);
            if (!(state.m_60734_() instanceof DragonBallBlock ball)) {
                return;
            }
            DragonRadarDefinition definition = radar.getDefinition();
            if (definition == null || !definition.supportsBallSet(ball.getBallSetId())) {
                return;
            }
            if (!deposit(side.level, pos, side.player, ball)) {
                return;
            }
            event.setCanceled(true);
            event.setUseBlock(Event.Result.DENY);
            event.setUseItem(Event.Result.DENY);
        } catch (Throwable ignored) {
        }
    }

    private static boolean deposit(ServerLevel level, BlockPos pos, Player player, DragonBallBlock ball) {
        if (!level.m_8055_(pos).m_60713_(ball)) {
            return false;
        }
        ItemStack stack = new ItemStack(ball);
        if (stack.m_41619_()) {
            return false;
        }
        level.m_46597_(pos, Blocks.f_50016_.m_49966_());
        try {
            DragonBallsHandler.unregisterConsumedDragonBalls(level, List.of(pos), ball.getBallSetId());
        } catch (Throwable ignored) {
        }
        if (!player.m_150109_().m_36054_(stack)) {
            player.m_36176_(stack, false);
        }
        level.m_5594_(null, pos, SoundEvents.f_12019_, SoundSource.PLAYERS, 0.4f, 1.0f);
        return true;
    }

    private static final class LevelSide {
        final ServerLevel level;
        final Player player;

        private LevelSide(ServerLevel level, Player player) {
            this.level = level;
            this.player = player;
        }

        static LevelSide of(PlayerInteractEvent.LeftClickBlock event) {
            if (event.getLevel() == null || event.getLevel().m_5776_()) {
                return null;
            }
            if (!(event.getLevel() instanceof ServerLevel level)) {
                return null;
            }
            Player player = event.getEntity();
            if (player == null || player.m_5833_()) {
                return null;
            }
            return new LevelSide(level, player);
        }
    }
}
