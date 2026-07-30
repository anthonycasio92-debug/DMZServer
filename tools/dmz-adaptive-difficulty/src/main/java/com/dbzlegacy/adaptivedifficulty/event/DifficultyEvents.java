package com.dbzlegacy.adaptivedifficulty.event;

import com.dbzlegacy.adaptivedifficulty.ai.AdaptiveAiSystem;
import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.events.DMZEvent;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DifficultyEvents {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DifficultyConfig.load();
        DifficultyCache.invalidateAll();
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DifficultyCache.data(player);
            DifficultyCache.refresh(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DifficultyCache.save(player);
            DifficultyCache.remove(player.m_20148_());
            DifficultyCache.invalidateAll();
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer neu) || !(event.getOriginal() instanceof ServerPlayer old)) {
            return;
        }
        DifficultyCache.save(old);
        var data = DifficultyCache.data(old);
        data.writeToPlayerNbt(PersistentDataAccess.get(neu));
        DifficultyCache.putData(neu, data);
        DifficultyCache.remove(old.m_20148_());
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        LivingEntity entity = event.getEntity();
        if (entity != null) {
            MobScaling.scaleIfNeeded(entity);
        }
    }

    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity == null || entity.m_9236_().f_46443_ || entity instanceof Player) {
            return;
        }
        MutationSystem.tick(entity);
        AdaptiveAiSystem.tick(entity);
        BossScaling.tickPhases(entity);
    }

    @SubscribeEvent
    public void onHurt(LivingHurtEvent event) {
        AdaptiveAiSystem.onHurt(event);
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().m_7639_() instanceof ServerPlayer killer)) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (dead == null || dead instanceof Player) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enableRewardScaling) {
            return;
        }
        float bonus = 0.0f;
        if (EliteSystem.isElite(dead)) {
            bonus += 50.0f * (float) cfg.eliteRewardBonus;
        }
        if (PersistentDataAccess.get(dead).m_128471_(BossScaling.TAG_BOSS)) {
            bonus += 200.0f;
        }
        if (MutationSystem.get(dead) != null) {
            bonus += 25.0f;
        }
        if (bonus <= 0.0f) {
            return;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(killer);
        bonus *= (float) DifficultyCalculator.rewardMultiplier(snap.active);
        StatsData stats = DmzProgression.stats(killer);
        if (stats == null) {
            return;
        }
        Resources resources = stats.getResources();
        if (resources == null) {
            return;
        }
        resources.addTrainingPoints(bonus);
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(killer), killer);
        } catch (Throwable ignored) {
        }
    }

    @SubscribeEvent
    public void onTpGain(DMZEvent.TPGainEvent event) {
        if (!DifficultyConfig.get().enableRewardScaling) {
            return;
        }
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        double mult = DifficultyCalculator.rewardMultiplier(snap.active);
        if (mult > 1.0) {
            int gained = event.getTpGain();
            int scaled = (int) Math.max(0, Math.round(gained * mult));
            if (scaled != gained) {
                event.setTpGain(scaled);
            }
        }
    }
}
