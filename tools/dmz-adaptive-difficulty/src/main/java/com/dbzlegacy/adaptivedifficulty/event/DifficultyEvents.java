package com.dbzlegacy.adaptivedifficulty.event;

import com.dbzlegacy.adaptivedifficulty.ai.AdaptiveAiSystem;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.evolution.CombatGravity;
import com.dbzlegacy.adaptivedifficulty.evolution.EnemyEvolution;
import com.dbzlegacy.adaptivedifficulty.reward.RewardSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tick.BehaviorScheduler;
import com.dbzlegacy.adaptivedifficulty.tick.CombatIndex;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dbzlegacy.adaptivedifficulty.util.NearbyPlayers;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import com.dbzlegacy.adaptivedifficulty.world.VanillaDifficultyGuard;
import com.dragonminez.common.events.DMZEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.AbstractSkeleton;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.LargeFireball;
import net.minecraft.world.entity.projectile.SmallFireball;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingChangeTargetEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
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
        CombatIndex.clear();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        VanillaDifficultyGuard.restoreIfPeaceful(event.getServer());
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DifficultyCache.data(player);
            // Convert any leftover NBT Ancient Coin wallet into real Lightman's items.
            com.dbzlegacy.adaptivedifficulty.currency.            AncientCoinEconomy.migrateWalletToItems(player);
            DifficultyCache.refresh(player);
            TitleSystem.syncTierTitles(player, false);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DifficultyCache.save(player);
            DifficultyCache.remove(player.m_20148_());
            CombatGravity.clearPlayer(player);
            com.dbzlegacy.adaptivedifficulty.tick.ScaledMobTracker.clearPlayer(player.m_20148_());
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
        if (SystemGate.isDisabled()) {
            return;
        }
        if (entity != null && !event.isCanceled() && !event.isSpawnCancelled()
                && !DimensionGates.isDisabled(entity)) {
            MobScaling.scaleIfNeeded(entity);
        }
    }

    /**
     * Player agro → scale to that player. Hostile→hostile agro → block / redirect to a player
     * so packs do not civil-war (vanilla revenge + splash/ki otherwise re-locks every tick).
     */
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onChangeTarget(LivingChangeTargetEvent event) {
        if (SystemGate.isDisabled()) {
            return;
        }
        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Mob mob) || DimensionGates.isDisabled(mob)) {
            return;
        }
        LivingEntity neu = event.getNewTarget();
        if (neu instanceof ServerPlayer player && SystemGate.allows(player)) {
            CombatIndex.mark(mob);
            MobScaling.retargetToPlayer(mob, player);
            return;
        }
        if (neu != null && HostileMobs.bothHostile(mob, neu)) {
            ServerPlayer player = NearbyPlayers.nearest(mob, DifficultyConfig.get().mobScaleRadius);
            if (player != null && SystemGate.allows(player)) {
                event.setNewTarget(player);
                CombatIndex.mark(mob);
                MobScaling.retargetToPlayer(mob, player);
            } else {
                event.setNewTarget(null);
                HostileMobs.clearCivilWarAggro(mob);
            }
        }
    }

    /**
     * Cheap early cancel: stop hostile→hostile swings before revenge memory arms.
     * (Heavier combat-index work stays on {@link #onHurt} for player fights only.)
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onAttack(LivingAttackEvent event) {
        if (SystemGate.isDisabled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim == null || victim.m_9236_().f_46443_ || DimensionGates.isDisabled(victim)) {
            return;
        }
        Entity causing = event.getSource() == null ? null : event.getSource().m_7639_();
        if (causing == null || causing == victim || causing instanceof Player || victim instanceof Player) {
            return;
        }
        if (causing instanceof LivingEntity attacker && HostileMobs.bothHostile(victim, attacker)) {
            event.setCanceled(true);
        }
    }

    /**
     * Concept §17 — one server-pulse scheduler instead of LivingTick on every entity.
     */
    @SubscribeEvent
    public void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SystemGate.isDisabled()) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        BehaviorScheduler.pulse(server, server.m_129921_()); // getTickCount
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || SystemGate.isDisabled() || !SystemGate.allows(player)) {
            return;
        }
        // Level / prestige / transform-power poll — transform & limit-release change CR + mob scale.
        if (player.f_19797_ % 40 != 0) {
            return;
        }
        DifficultySnapshot before = DifficultyCache.get(player);
        int level = DmzProgression.dmzLevel(player);
        int prestige = DmzProgression.prestige(player);
        double transform = DmzProgression.transformationPower(player);
        if (before.dmzLevel != level
                || before.prestige != prestige
                || Math.abs(before.transformationPower - transform) > 0.5) {
            DifficultyCache.refresh(player);
        }
    }

    @SubscribeEvent
    public void onFormChange(DMZEvent.FormChangeEvent event) {
        if (SystemGate.isDisabled()) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        if (player == null || !SystemGate.allows(player)) {
            return;
        }
        DifficultyCache.refresh(player);
    }

    @SubscribeEvent
    public void onStackFormChange(DMZEvent.StackFormChangeEvent event) {
        if (SystemGate.isDisabled()) {
            return;
        }
        ServerPlayer player = event.getPlayer();
        if (player == null || !SystemGate.allows(player)) {
            return;
        }
        DifficultyCache.refresh(player);
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
        if (victim == null || victim.m_9236_().f_46443_ || SystemGate.isDisabled()) {
            return;
        }
        var source = event.getSource();
        Entity causing = source == null ? null : source.m_7639_(); // getEntity

        // The End (etc.) is owned by other systems — do not stack AD work there.
        if (DimensionGates.isDisabled(victim)) {
            return;
        }

        // Fast reject: ignore damage that cannot involve our systems (no player, no hostile).
        boolean victimPlayer = victim instanceof Player;
        boolean causerPlayer = causing instanceof Player;
        boolean victimHostile = !victimPlayer && HostileMobs.isHostile(victim);
        boolean causerHostile = !causerPlayer && causing instanceof LivingEntity le && HostileMobs.isHostile(le);
        if (!victimPlayer && !causerPlayer && !victimHostile && !causerHostile) {
            return;
        }

        // Whitelist testing: skip player-facing AD unless an allowed tester is in the fight.
        // Hostile↔hostile civil-war cancel below still applies to scaled mobs.
        boolean whitelistBlocksPlayers = DifficultyConfig.isWhitelistEnabled()
                && !SystemGate.allowsAny(victim, causing);

        // Block hostile→hostile (and hostile booms on other hostiles). Allow self-damage
        // so creeper fuse can finish killing the exploding creeper on some Mohist paths.
        if (victimHostile && !causerPlayer && causing != victim) {
            boolean hostileBoom = source != null && source.m_269533_(DamageTypeTags.f_268415_) // IS_EXPLOSION
                    && !causerPlayer;
            if (causerHostile || hostileBoom) {
                event.setCanceled(true);
                event.setAmount(0.0f);
                // Wipe revenge + redirect onto a nearby player (null target lets vanilla re-aggro).
                redirectCivilWar(causing instanceof LivingEntity le ? le : null, victim);
                return;
            }
        }

        if (whitelistBlocksPlayers) {
            return;
        }

        // Combat-index + cached retarget (LivingAttackEvent removed — every swing crushed TPS).
        if (causing instanceof ServerPlayer player && victimHostile && victim instanceof Mob vm) {
            CombatIndex.mark(vm);
            MobScaling.retargetToPlayer(vm, player);
        } else if (victim instanceof ServerPlayer player && causerHostile && causing instanceof Mob atk) {
            CombatIndex.mark(atk);
            MobScaling.retargetToPlayer(atk, player);
        }

        float amount = event.getAmount();
        if (amount > 0.0f && causerHostile) {
            float scaled = MobScaling.scaleOutgoingHurt(amount, event.getSource());
            if (scaled != amount) {
                event.setAmount(scaled);
            }
        }
        if (victimHostile) {
            AdaptiveAiSystem.onHurt(event);
        }
        // Counter-teleport only for kits that use it — never NBT-scan every hurt victim.
        if (victimHostile && event.getAmount() > 0.0f
                && (victim instanceof EnderMan
                || victim instanceof ZombifiedPiglin
                || victim instanceof Warden)) {
            EnemyEvolution.onHurt(victim);
        }
        if (victimHostile && victim.m_21223_() <= 0.0f) {
            MobScaling.terminateIfZeroHealth(victim);
        }
    }

    /**
     * Post-mitigation safety net — only for hostiles at ≤0 HP.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onDamageDone(net.minecraftforge.event.entity.living.LivingDamageEvent event) {
        if (SystemGate.isDisabled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim != null && HostileMobs.isHostile(victim) && victim.m_21223_() <= 0.0f) {
            MobScaling.terminateIfZeroHealth(victim);
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (SystemGate.isDisabled()) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (dead instanceof Creeper creeper) {
            EnemyEvolution.onCreeperDeath(creeper, event.getSource());
        }
        // V3 death penalty: clear temporary active tier/level; unlocks & coins stay.
        if (dead instanceof ServerPlayer victim
                && SystemGate.allows(victim)
                && DifficultyConfig.get().deathResetsActiveDifficulty) {
            var data = DifficultyCache.data(victim);
            if (data.getActiveTier() > 0 || data.getActiveDifficultyLevel() > 0L) {
                data.resetTemporary();
                DifficultyCache.save(victim);
                DifficultyCache.refresh(victim);
                victim.m_213846_(net.minecraft.network.chat.Component.m_237113_(
                        "§cDifficulty deactivated on death. §7Unlocks & Ancient Coins kept."));
            }
        }
        if (!(event.getSource().m_7639_() instanceof ServerPlayer killer) || !SystemGate.allows(killer)) {
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
        if (SystemGate.isDisabled() || event.getLevel().m_5776_()) { // isClientSide
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
    public void onKiCharge(DMZEvent.KiChargeEvent event) {
        if (SystemGate.isDisabled() || !(event.getPlayer() instanceof ServerPlayer player)
                || !SystemGate.allows(player)) {
            return;
        }
        AdaptiveAiSystem.onPlayerKiCharge(player);
    }

    /**
     * After a hostile↔hostile hit/boom is cancelled, clear civil-war agro and point both
     * sides at the nearest allowed player when one is in range.
     */
    private static void redirectCivilWar(LivingEntity causing, LivingEntity victim) {
        ServerPlayer player = null;
        if (victim instanceof Mob vm) {
            player = focusPlayerOrClear(vm, null);
        }
        if (causing instanceof Mob am) {
            focusPlayerOrClear(am, player);
        }
    }

    private static ServerPlayer focusPlayerOrClear(Mob mob, ServerPlayer hint) {
        HostileMobs.clearCivilWarAggro(mob);
        ServerPlayer player = hint;
        if (player == null || !player.m_6084_() || player.m_9236_() != mob.m_9236_()) {
            player = NearbyPlayers.nearest(mob, DifficultyConfig.get().mobScaleRadius);
        }
        if (player != null && SystemGate.allows(player)) {
            LivingEntity current = mob.m_5448_();
            if (!(current instanceof Player) || !current.m_6084_()) {
                mob.m_6710_(player);
            }
            CombatIndex.mark(mob);
            MobScaling.retargetToPlayer(mob, player);
            return player;
        }
        return null;
    }
}
