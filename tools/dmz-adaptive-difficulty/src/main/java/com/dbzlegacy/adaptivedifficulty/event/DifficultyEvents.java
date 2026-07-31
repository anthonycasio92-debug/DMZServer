package com.dbzlegacy.adaptivedifficulty.event;

import com.dbzlegacy.adaptivedifficulty.ai.AdaptiveAiSystem;
import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultyCalculator;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.evolution.CombatGravity;
import com.dbzlegacy.adaptivedifficulty.evolution.EnemyEvolution;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.reward.RewardSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dbzlegacy.adaptivedifficulty.world.VanillaDifficultyGuard;
import com.dragonminez.common.events.DMZEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DifficultyEvents {

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DifficultyConfig.load();
        DifficultyCache.invalidateAll();
        AreaDifficulty.clearCache();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        VanillaDifficultyGuard.restoreIfPeaceful(event.getServer());
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
            CombatGravity.clearPlayer(player);
            AreaDifficulty.clearCache();
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
        CombatGravity.clearPlayer(old);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        LivingEntity entity = event.getEntity();
        if (entity != null && !event.isCanceled() && !event.isSpawnCancelled()) {
            MobScaling.scaleIfNeeded(entity);
        }
    }

    /**
     * Concept §17 — only touch mobs that need work.
     * Unscaled hostiles get sparse spawn-retry; unmarked vanilla mobs exit immediately.
     */
    @SubscribeEvent
    public void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob) || entity.m_9236_().f_46443_ || entity instanceof Player) {
            return;
        }

        int age = entity.f_19797_;
        CompoundTag tag = PersistentDataAccess.get(entity);
        boolean scaled = tag.m_128471_(MobScaling.TAG_SCALED);

        // Sparse deferred scale if FinalizeSpawn missed (Mohist).
        if (!scaled && age > 2 && age < 40 && (age == 5 || age == 15 || age == 30)) {
            MobScaling.scaleIfNeeded(entity);
            tag = PersistentDataAccess.get(entity);
            scaled = tag.m_128471_(MobScaling.TAG_SCALED);
        }
        if (!scaled) {
            return;
        }

        boolean elite = tag.m_128471_(EliteSystem.TAG_ELITE);
        boolean boss = tag.m_128471_(BossScaling.TAG_BOSS);
        boolean mutated = tag.m_128441_(MutationSystem.TAG_MUTATION)
                && !tag.m_128461_(MutationSystem.TAG_MUTATION).isEmpty();
        long difficulty = tag.m_128441_(MobScaling.TAG_DIFFICULTY) ? tag.m_128454_(MobScaling.TAG_DIFFICULTY) : 0L;

        // Plain scaled-zero hostiles need no AI/evolution/phases.
        if (difficulty <= 0 && !elite && !boss && !mutated) {
            return;
        }

        if (mutated && age % 20 == 0) {
            MutationSystem.tick(entity, tag);
        }
        if (boss && age % 10 == 0) {
            BossScaling.tickPhases(entity, tag);
        }

        // Stagger AI / evolution across entity ids to smooth TPS.
        int stagger = Math.floorMod(entity.m_19879_(), 20);
        if (age % 20 == stagger) {
            AdaptiveAiSystem.tick(mob, difficulty, elite);
        }
        if (EnemyEvolution.isEvolvable(mob) && age % 20 == ((stagger + 10) % 20)) {
            EnemyEvolution.tick(mob, difficulty, elite);
        }
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }
        // Keep DMZ gravity-chamber zones glued to the player while endermen/wardens agro.
        CombatGravity.tickPlayer(player);

        if (player.f_19797_ % 100 != 0) {
            return;
        }
        DifficultySnapshot before = DifficultyCache.get(player);
        int level = DmzProgression.dmzLevel(player);
        int prestige = DmzProgression.prestige(player);
        if (before.dmzLevel != level || before.prestige != prestige) {
            DifficultyCache.refresh(player);
            AreaDifficulty.clearCache();
        }
    }

    /**
     * Primary damage scaling path (Forge event — reliable on Mohist).
     * Melee uses scaled ATTACK_DAMAGE; this multiplies projectiles / custom hits.
     * <p>
     * Hostile→hostile damage is cancelled: high offense multipliers were turning
     * splash/ki/pack mistakes into mob civil wars.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onHurt(LivingHurtEvent event) {
        LivingEntity victim = event.getEntity();
        var source = event.getSource();
        Entity causing = source == null ? null : source.m_7639_(); // getEntity
        // Block hostile→hostile (and hostile booms on other hostiles). Allow self-damage
        // so creeper fuse can finish killing the exploding creeper on some Mohist paths.
        if (HostileMobs.isHostile(victim) && !(causing instanceof Player) && causing != victim) {
            boolean hostileAttacker = causing instanceof LivingEntity atk && HostileMobs.isHostile(atk);
            boolean hostileBoom = source != null && source.m_269533_(DamageTypeTags.f_268415_) // IS_EXPLOSION
                    && !(causing instanceof Player);
            if (hostileAttacker || hostileBoom) {
                event.setCanceled(true);
                event.setAmount(0.0f);
                if (hostileAttacker && causing instanceof Mob am && am.m_5448_() == victim) {
                    am.m_6710_(null);
                }
                if (victim instanceof Mob vm && causing instanceof LivingEntity
                        && vm.m_5448_() == causing) {
                    vm.m_6710_(null);
                }
                return;
            }
        }

        float amount = event.getAmount();
        if (amount > 0.0f) {
            float scaled = MobScaling.scaleOutgoingHurt(amount, event.getSource());
            if (scaled != amount) {
                event.setAmount(scaled);
            }
        }
        AdaptiveAiSystem.onHurt(event);
        if (victim != null && event.getAmount() > 0.0f) {
            EnemyEvolution.onHurt(victim);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead instanceof Creeper creeper) {
            EnemyEvolution.onCreeperDeath(creeper, event.getSource());
        }
        if (!(event.getSource().m_7639_() instanceof ServerPlayer killer)) {
            return;
        }
        if (dead != null) {
            RewardSystem.onKill(killer, dead);
        }
    }

    /**
     * Replace vanilla projectiles from evolved skeletons / blazes / ghasts with DMZ ki.
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().m_5776_()) { // isClientSide
            return;
        }
        Entity entity = event.getEntity();
        LivingEntity target = null;
        Mob shooter = null;

        if (entity instanceof AbstractArrow arrow && arrow.m_19749_() instanceof AbstractSkeleton skel) {
            shooter = skel;
            target = skel.m_5448_();
        } else if (entity instanceof SmallFireball ball && ball.m_19749_() instanceof Blaze blaze) {
            shooter = blaze;
            target = blaze.m_5448_();
        } else if (entity instanceof LargeFireball ball && ball.m_19749_() instanceof Ghast ghast) {
            shooter = ghast;
            target = ghast.m_5448_();
        } else {
            return;
        }

        if (shooter == null || target == null || !EnemyEvolution.isEvolvable(shooter)) {
            return;
        }
        if (!PersistentDataAccess.get(shooter).m_128471_(MobScaling.TAG_SCALED)) {
            return;
        }
        if (EnemyEvolution.tryReplaceProjectile(shooter, target)) {
            event.setCanceled(true);
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
        DifficultySnapshot snap = DifficultyCache.get(player);
        double mult = DifficultyCalculator.rewardMultiplier(snap.active);
        if (mult > 1.0) {
            int gained = event.getTpGain();
            int scaled = (int) Math.max(0, Math.round(gained * mult));
            if (scaled != gained) {
                event.setTpGain(scaled);
            }
        }
    }

    @SubscribeEvent
    public void onKiCharge(DMZEvent.KiChargeEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) {
            return;
        }
        AdaptiveAiSystem.onPlayerKiCharge(player);
    }
}
