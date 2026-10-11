package com.dbzlegacy.adaptivedifficulty.kube;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.CapsuleBan;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.HealCap;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.HeartstopStrip;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.PothalaClean;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.RaceLockClear;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.SilkTouchStrip;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.SilkyBan;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.SpawnerVanillaize;
import com.dbzlegacy.adaptivedifficulty.kube.hotpath.WeaponBonusScale;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

/**
 * Native replacements for the KubeJS player-tick and item-spawn hot path.
 * KubeJS skips those listeners when {@link #ENABLED} is visible.
 */
public final class LmHotPath {
    /** KubeJS checks this field and leaves the tick / item-spawn listeners idle. */
    public static final boolean ENABLED = true;
    public static final LmHotPath INSTANCE = new LmHotPath();

    private LmHotPath() {}

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        int age = player.f_19797_;
        if (age < 0) {
            return;
        }
        if (age % WeaponBonusScale.UPDATE_INTERVAL == 0) {
            WeaponBonusScale.update(player);
        }
        if (age % HealCap.TICK_INTERVAL == 0) {
            HealCap.clamp(player);
        }
        if (age % PothalaClean.CHECK_INTERVAL == 0) {
            PothalaClean.clean(player);
        }
        if (age > 0 && age % RaceLockClear.SYNC_INTERVAL_TICKS == 0) {
            RaceLockClear.clear(player);
        }
        if (SpawnerVanillaize.staggered(player)) {
            SpawnerVanillaize.scan(player);
        }
        if (age % CapsuleBan.PLAYER_PURGE_INTERVAL == 0) {
            CapsuleBan.purge(player);
        }
        if (age % HeartstopStrip.INTERVAL == 0) {
            HeartstopStrip.strip(player);
        }
        if (age % SilkTouchStrip.PLAYER_SCAN_INTERVAL == 0) {
            SilkTouchStrip.purge(player);
        }
        if (age % SilkyBan.CLOTH_SCAN_INTERVAL == 0) {
            SilkyBan.purge(player, age % SilkyBan.TOOL_SCAN_INTERVAL == 0);
        }
    }

    @SubscribeEvent
    public void onItemSpawn(EntityJoinLevelEvent event) {
        if (event.getLevel().m_5776_() || !(event.getEntity() instanceof ItemEntity entity)) {
            return;
        }
        ItemStack stack = entity.m_32055_();
        if (stack == null || stack.m_41619_()) {
            return;
        }
        SpawnerVanillaize.onItemSpawn(stack);
        if (CapsuleBan.onItemSpawn(event, entity, stack)) {
            return;
        }
        SilkTouchStrip.onItemSpawn(entity, stack);
        SilkyBan.onItemSpawn(entity, entity.m_32055_());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        WeaponBonusScale.clearPlayer(player.m_20148_());
        WeaponBonusScale.update(player);
        HealCap.clamp(player);
        HealCap.announce(player);
        PothalaClean.clean(player);
        RaceLockClear.clear(player);
        SpawnerVanillaize.scan(player);
        CapsuleBan.purge(player);
        HeartstopStrip.strip(player);
        SilkTouchStrip.purge(player);
        SilkyBan.purge(player, true);
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() != null) {
            WeaponBonusScale.clearPlayer(event.getEntity().m_20148_());
        }
    }

    public static void logReady() {
        AdaptiveDifficultyMod.LOGGER.info(
                "[{}] native hot path owns weapon bonus, heal cap, pothala, race-lock clear, "
                        + "spawner vanillaize, capsule ban, heartstop, silk touch, and silky",
                AdaptiveDifficultyMod.MOD_ID);
    }
}
