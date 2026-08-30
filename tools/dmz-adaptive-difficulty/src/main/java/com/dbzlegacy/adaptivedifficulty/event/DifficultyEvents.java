package com.dbzlegacy.adaptivedifficulty.event;

import com.dbzlegacy.adaptivedifficulty.ai.AdaptiveAiSystem;
import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.evolution.CombatGravity;
import com.dbzlegacy.adaptivedifficulty.evolution.EnemyEvolution;
import com.dbzlegacy.adaptivedifficulty.progression.PlayerStatChecker;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionSystem;
import com.dbzlegacy.adaptivedifficulty.progression.end.EndProgression;
import com.dbzlegacy.adaptivedifficulty.reward.RewardSystem;
import com.dbzlegacy.adaptivedifficulty.rival.RivalProgression;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.AreaDifficulty;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.scaling.SlimeSplitGuard;
import com.dbzlegacy.adaptivedifficulty.sparring.SparStore;
import com.dbzlegacy.adaptivedifficulty.sparring.SparringSystem;
import com.dbzlegacy.adaptivedifficulty.tick.BehaviorScheduler;
import com.dbzlegacy.adaptivedifficulty.tick.CombatIndex;
import com.dbzlegacy.adaptivedifficulty.tick.NearbyMobScaler;
import com.dbzlegacy.adaptivedifficulty.telemetry.BalanceTelemetry;
import com.dbzlegacy.adaptivedifficulty.tick.ScaledMobTracker;
import com.dbzlegacy.adaptivedifficulty.title.TitleEffects;
import com.dbzlegacy.adaptivedifficulty.title.TitleSense;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dbzlegacy.adaptivedifficulty.util.NearbyPlayers;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import com.dbzlegacy.adaptivedifficulty.world.VanillaDifficultyGuard;
import com.dragonminez.common.events.DMZEvent;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
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
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public final class DifficultyEvents {
    /**
     * Players whose personal toggle was forced OFF for this server boot.
     * Cleared on {@link ServerStartingEvent}; first login that boot resets personal OFF.
     * Mid-session reconnects keep the in-session toggle.
     */
    private static final java.util.Set<UUID> PERSONAL_OFF_THIS_BOOT =
            java.util.concurrent.ConcurrentHashMap.newKeySet();
    /** Last polled DMZ form×stack multiplier — catches custom races without FormChangeEvent. */
    private static final Map<UUID, Double> LAST_FORM_MULT = new ConcurrentHashMap<>();
    /** Last polled live offense peak — catches custom forms that never bump form multipliers. */
    private static final Map<UUID, Double> LAST_LIVE_OFFENSE = new ConcurrentHashMap<>();
    /** Last polled race id — clear form baselines when players swap custom races. */
    private static final Map<UUID, String> LAST_RACE = new ConcurrentHashMap<>();
    /** Last polled form key — catches future races that swap forms without mult spikes. */
    private static final Map<UUID, String> LAST_FORM_KEY = new ConcurrentHashMap<>();

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        DifficultyConfig.load();
        DifficultyCache.invalidateAll();
        AreaDifficulty.clearCache();
        CombatIndex.clear();
        PERSONAL_OFF_THIS_BOOT.clear();
        LAST_FORM_MULT.clear();
        LAST_LIVE_OFFENSE.clear();
        LAST_RACE.clear();
        LAST_FORM_KEY.clear();
        RivalStore.get().load();
        SparStore.get().load();
        RivalProgression.get().load();
    }

    @SubscribeEvent
    public void onServerStarted(ServerStartedEvent event) {
        VanillaDifficultyGuard.restoreIfPeaceful(event.getServer());
        try {
            com.dbzlegacy.adaptivedifficulty.data.CnpcDataMigrator.migrateWorldIfNeeded(event.getServer());
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] CNPC world migration hook failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    @SubscribeEvent
    public void onServerStopping(ServerStoppingEvent event) {
        BalanceTelemetry.flushAndClose();
        com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry.flushAndClose();
        try {
            RivalStore.get().save();
            SparStore.get().save();
            RivalProgression.get().save();
        } catch (Throwable ignored) {
        }
        RivalStore.get().save();
        SparStore.get().save();
    }

    @SubscribeEvent
    public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            var data = DifficultyCache.data(player);
            // Every server boot: personal difficulty starts OFF for everyone.
            // First login that boot forces it; reconnects later in the same uptime keep the toggle.
            if (PERSONAL_OFF_THIS_BOOT.add(player.m_20148_()) && data.isPersonalEnabled()) {
                data.setPersonalEnabled(false);
                ScaledMobTracker.releaseAndRevertPlayer(player);
                NearbyMobScaler.processEvictions();
            }
            // Convert any leftover NBT Ancient Coin wallet into real Lightman's items (once/session).
            AncientCoinEconomy.migrateWalletToItems(player);
            try {
                com.dbzlegacy.adaptivedifficulty.data.CnpcDataMigrator.migratePlayerIfNeeded(player);
            } catch (Throwable ignored) {
            }
            DifficultyCache.refresh(player);
            // Persist any unlock-list repairs from refresh so the next disconnect keeps the tier.
            DifficultyCache.save(player);
            TitleSystem.syncTierTitles(player, false);
            RivalSystem.onLogin(player);
            SparringSystem.onLogin(player);
            ProgressionSystem.onLogin(player);
        }
    }

    /**
     * Forge persists players via SaveToFile — often before {@link PlayerEvent.PlayerLoggedOutEvent}.
     * Always flush AD data here so purchased tiers survive disconnect.
     */
    @SubscribeEvent
    public void onSaveToFile(PlayerEvent.SaveToFile event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            DifficultyCache.save(player);
        }
    }

    @SubscribeEvent
    public void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Flush player AD data only — do NOT clear active tier on disconnect.
            DifficultyCache.save(player);
            DifficultyCache.remove(player.m_20148_());
            CombatGravity.clearPlayer(player);
            // Queue claimed mobs for base-stat revert, then apply immediately.
            // (Mob scaling reverts; the player's purchased unlock tier is kept.)
            ScaledMobTracker.releaseAndRevertPlayer(player);
            NearbyMobScaler.processEvictions();
            PlayerCombatProfile.clear(player.m_20148_());
            PlayerCombatProfile.clearFormBaseline(player.m_20148_());
            DmzProgression.clearBaseFormLevel(player.m_20148_());
            LAST_FORM_MULT.remove(player.m_20148_());
            LAST_LIVE_OFFENSE.remove(player.m_20148_());
            LAST_RACE.remove(player.m_20148_());
            LAST_FORM_KEY.remove(player.m_20148_());
            AncientCoinEconomy.clearMigrateFlag(player.m_20148_());
            AreaDifficulty.clearCache();
            RivalSystem.onLogout(player);
            SparringSystem.onLogout(player);
            ProgressionSystem.onLogout(player);
        }
    }

    @SubscribeEvent
    public void onClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer neu) || !(event.getOriginal() instanceof ServerPlayer old)) {
            return;
        }
        DifficultyCache.save(old);
        var data = DifficultyCache.data(old);
        var neuTag = PersistentDataAccess.get(neu);
        if (PersistentDataAccess.isWritable(neuTag)) {
            data.writeToPlayerNbt(neuTag);
        }
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
        if (entity == null || event.isCanceled() || event.isSpawnCancelled()
                || DimensionGates.isDisabled(entity)) {
            return;
        }
        // Cage / minecart / SDD Advanced Spawner — stamp so nearby rescale can't convert later.
        if (event.getSpawnType() == net.minecraft.world.entity.MobSpawnType.SPAWNER
                || MobScaling.hasSddSpawnerMark(entity)) {
            MobScaling.markFromSpawner(entity);
            return;
        }
        // Slime / magma split children — never AD-convert the cubs.
        if (SlimeSplitGuard.tryMarkSplitChild(entity)) {
            return;
        }
        // Saga/quest kills already carry dmz_quest_* / dmz_saga_id — scaleIfNeeded no-ops.
        MobScaling.scaleIfNeeded(entity);
    }

    /**
     * SDD Advanced Spawner stamps {@code sdd_spawner}/{@code sdd_boss} after finalizeSpawn
     * (and finalize can fail on Mohist). Catch them on world join and undo any AD paint.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public void onLivingJoin(EntityJoinLevelEvent event) {
        if (SystemGate.isDisabled() || event.getLevel().m_5776_()) {
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity living) || DimensionGates.isDisabled(living)) {
            return;
        }
        // Split cubs join via addFreshEntity (often without FinalizeSpawn on Mohist).
        // Run before any other AD work so magma/slime children never get claimed.
        if (SlimeSplitGuard.tryMarkSplitChild(living)) {
            MobScaling.ensureExempt(living);
            return;
        }
        if (MobScaling.hasSddSpawnerMark(living)) {
            MobScaling.markFromSpawner(living);
        }
        if (MobScaling.isExemptFromConversion(living)) {
            MobScaling.ensureExempt(living);
            return;
        }
        // Finish deferred leave-area / admin-off reverts once the chunk loads again.
        if (living instanceof Mob mob && ScaledMobTracker.isPendingRevert(mob.m_20148_())) {
            MobScaling.revertToBases(mob);
            CombatIndex.unmark(mob.m_20148_());
            ScaledMobTracker.clearPendingRevert(mob.m_20148_());
            return;
        }
        // Orphan scaled paint with no living claim owner → strip on join.
        if (living instanceof Mob mob
                && PersistentDataAccess.get(mob).m_128471_(MobScaling.TAG_SCALED)
                && ScaledMobTracker.findClaimOwnerId(mob.m_20148_()) == null
                && !ScaledMobTracker.isPendingRevert(mob.m_20148_())) {
            MobScaling.revertToBases(mob);
            CombatIndex.unmark(mob.m_20148_());
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
        if (neu instanceof ServerPlayer player && SystemGate.participates(player)) {
            CombatIndex.mark(mob);
            MobScaling.retargetToPlayer(mob, player);
            return;
        }
        if (neu != null && HostileMobs.bothHostile(mob, neu)) {
            ServerPlayer player = NearbyPlayers.nearestParticipating(
                    mob, DifficultyConfig.get().mobScaleRadius);
            if (player != null) {
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
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        // Even when master is OFF, drain claims + revert scaled hostiles.
        BehaviorScheduler.pulse(server, server.m_129921_()); // getTickCount
        RivalSystem.pulse(server, server.m_129921_());
        SparringSystem.pulse(server, server.m_129921_());
        ProgressionSystem.pulse(server, server.m_129921_());
    }

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || SystemGate.isDisabled() || !SystemGate.allows(player)) {
            return;
        }
        // Level / prestige / transform-power / form-mult poll.
        // Custom races often skip FormChangeEvent — catch form mult spikes here.
        if (player.f_19797_ % 20 != 0) {
            return;
        }
        // Survivor challenge: accumulate playtime while a tier is active.
        if (DifficultyCache.data(player).getActiveTier() > 0) {
            DifficultyCache.data(player).titleProgress().addPlaySeconds(1L);
            if (player.f_19797_ % 1200 == 0) {
                TitleSystem.syncChallengeTitles(player, true);
                DifficultyCache.save(player);
            }
        }
        if (player.f_19797_ % 40 == 0) {
            TitleSense.pulse(player);
        }
        DifficultySnapshot before = DifficultyCache.get(player);
        int level = DmzProgression.dmzLevelForProgression(player);
        int prestige = DmzProgression.prestige(player);
        double transform = DmzProgression.transformationPower(player);
        double formMult = PlayerCombatProfile.liveFormMultiplier(player);
        double liveOffense = liveOffensePeak(player);
        String race = DmzProgression.race(player);
        String formKey = DmzProgression.activeFormKey(player);
        UUID id = player.m_20148_();
        String prevRace = LAST_RACE.put(id, race == null ? "" : race);
        if (prevRace != null && race != null && !prevRace.equals(race)) {
            // New / swapped race — drop baselines so future custom races start clean.
            PlayerCombatProfile.clearFormBaseline(id);
            DmzProgression.clearBaseFormLevel(id);
            PlayerCombatProfile.clear(id);
            LAST_FORM_KEY.remove(id);
            LAST_FORM_MULT.remove(id);
            LAST_LIVE_OFFENSE.remove(id);
        }
        String prevFormKey = LAST_FORM_KEY.put(id, formKey == null ? "" : formKey);
        Double prevForm = LAST_FORM_MULT.put(id, formMult);
        Double prevOffense = LAST_LIVE_OFFENSE.put(id, liveOffense);
        boolean formKeyChanged = prevFormKey != null && formKey != null && !prevFormKey.equals(formKey);
        boolean formChanged = prevForm != null && Math.abs(prevForm - formMult) > 0.08;
        // Lower threshold — custom forms sometimes step up in smaller mastery chunks.
        boolean offenseChanged = prevOffense != null && prevOffense > 1.0 && liveOffense > 1.0
                && Math.abs(liveOffense - prevOffense) / prevOffense > 0.08;
        boolean progressChanged = before.dmzLevel != level
                || before.prestige != prestige
                || Math.abs(before.transformationPower - transform) > 0.5;
        if (formChanged || formKeyChanged || offenseChanged
                || Math.abs(before.transformationPower - transform) > 0.5
                || (prevRace != null && race != null && !prevRace.equals(race))) {
            com.dbzlegacy.adaptivedifficulty.service.DifficultyActions.refreshCombatPaint(player);
        } else if (progressChanged) {
            DifficultyCache.refresh(player);
        }
    }

    private static double liveOffensePeak(ServerPlayer player) {
        try {
            var data = DmzProgression.stats(player);
            if (data == null) {
                return 1.0;
            }
            // Peak live offense across STR/SKP/PWR (+ mild ENE pool).
            double m = Math.max(1.0, data.getMeleeDamage());
            double s = Math.max(1.0, data.getStrikeDamage());
            double k = Math.max(1.0, data.getKiDamage());
            double e = Math.max(1.0, data.getMaxEnergy() * 0.08);
            return Math.max(m, Math.max(s, Math.max(k, e)));
        } catch (Throwable t) {
            return 1.0;
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
        // Immediate claimed-mob repaint — don't wait for the nearby pulse.
        com.dbzlegacy.adaptivedifficulty.service.DifficultyActions.refreshCombatPaint(player);
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
        com.dbzlegacy.adaptivedifficulty.service.DifficultyActions.refreshCombatPaint(player);
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

        // Dimensions listed in disabledDimensions skip AD work entirely.
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

        // Rider↔mount: skeleton/spider jockeys + skeleton horses — ki spawns on the rider
        // and otherwise kills their own vehicle (mount may not be HostileMobs-tagged).
        if (causing != null && causing != victim && HostileMobs.isMountPair(causing, victim)) {
            event.setCanceled(true);
            event.setAmount(0.0f);
            return;
        }

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
        // Equipped title / Title Score damage perks (small, capped).
        if (amount > 0.0f && causerPlayer && victimHostile
                && causing instanceof ServerPlayer attacker) {
            float boosted = TitleEffects.applyOutgoingDamageBonus(attacker, victim, event.getAmount());
            if (boosted != event.getAmount()) {
                event.setAmount(boosted);
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

        // Rival / Sparring PvP scoring + natural-progression combat/End/dummy.
        if (victim instanceof ServerPlayer pvpVictim
                && causing instanceof ServerPlayer pvpAttacker) {
            RivalSystem.onPlayerHurt(pvpVictim, pvpAttacker, source);
            SparringSystem.onPlayerHurt(pvpVictim, pvpAttacker, source);
        }
        ProgressionSystem.onHurt(event);
    }

    @SubscribeEvent
    public void onBlockBreak(BlockEvent.BreakEvent event) {
        if (!(event.getPlayer() instanceof ServerPlayer player) || player.m_9236_().f_46443_) {
            return;
        }
        ProgressionSystem.onBlockBreak(player, event.getPos(), event.getState());
    }

    @SubscribeEvent
    public void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (event.getLevel() == null || event.getLevel().m_5776_()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ProgressionSystem.onBlockPlace(player, event.getPos(), event.getPlacedBlock());
    }

    @SubscribeEvent
    public void onTravelToDimension(net.minecraftforge.event.entity.EntityTravelToDimensionEvent event) {
        EndProgression.onTravelToDimension(event);
    }

    @SubscribeEvent
    public void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        EndProgression.onRightClickBlock(event);
    }

    /** Sneak + right-click another player → DMZ stat dump (PlayerStatChecker.js).
     * Also Skill Check / Rival / Spar / Hub / Difficulty / Prestige CNPC interact. */
    @SubscribeEvent
    public void onPlayerEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (event.getLevel() == null || event.getLevel().m_5776_()) {
            return;
        }
        if (event.getHand() != net.minecraft.world.InteractionHand.MAIN_HAND) {
            return;
        }
        if (event.getEntity() instanceof net.minecraft.server.level.ServerPlayer player
                && event.getTarget() != null) {
            if (com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService.tryOpenFromNpc(
                    player, event.getTarget())) {
                event.setCanceled(true);
                return;
            }
            // Tags or strict names — cancel (LegacyMechanics replaces CNPC scripts).
            if (com.dbzlegacy.adaptivedifficulty.gui.CnpcGuiOpener.tryOpenFromNpc(
                    player, event.getTarget())) {
                event.setCanceled(true);
                return;
            }
        }
        PlayerStatChecker.onEntityInteract(event);
    }

    /**
     * Rival + Sparring death / KO / mob-kill hooks.
     * Independent of AD master gate — these systems have their own config flags.
     */
    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRivalSparDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (dead == null || dead.m_9236_().f_46443_) {
            return;
        }
        Entity killerEnt = event.getSource() == null ? null : event.getSource().m_7639_();
        if (dead instanceof ServerPlayer victim) {
            LivingEntity killerLiving = killerEnt instanceof LivingEntity le ? le : null;
            RivalSystem.onDeath(victim, killerLiving);
            SparringSystem.onDeath(victim);
        }
        if (killerEnt instanceof ServerPlayer killer
                && dead instanceof LivingEntity
                && !(dead instanceof Player)) {
            RivalSystem.onMobKillNear(killer, (LivingEntity) dead);
            ProgressionSystem.onDeath(event);
        }
    }

    /**
     * Post-mitigation safety net.
     * <ul>
     *   <li>Hostiles at ≤0 HP — force terminate</li>
     *   <li>AD mob → player hard-cancelled by DMZ DEF ({@code flatMit ≥ dmg×2.5}) —
     *       restore tier-scaled landing damage so SSJB/T7 cannot knock without hurting</li>
     * </ul>
     */
    @SubscribeEvent(priority = EventPriority.LOWEST, receiveCanceled = true)
    public void onDamageDone(LivingDamageEvent event) {
        if (SystemGate.isDisabled()) {
            return;
        }
        LivingEntity victim = event.getEntity();
        if (victim != null && HostileMobs.isHostile(victim) && victim.m_21223_() <= 0.0f) {
            MobScaling.terminateIfZeroHealth(victim);
        }
        // Personal OFF / whitelist-blocked: never inject landing damage.
        // (allows() alone let stored active tiers bite players with personal OFF.)
        if (!(victim instanceof ServerPlayer player) || !SystemGate.participates(player)) {
            return;
        }
        if (DimensionGates.isDisabled(player)) {
            return;
        }
        var source = event.getSource();
        Entity causing = source == null ? null : source.m_7639_();
        if (!(causing instanceof Mob mob) || !HostileMobs.isHostile(mob)) {
            return;
        }
        // Claimed/profile-scaled only — spawn-init TAG_SCALED shells are not difficulty.
        if (!MobScaling.isAdPainted(mob)) {
            return;
        }
        float preAmount = event.getAmount();
        boolean cancelled = event.isCanceled() || preAmount <= 0.0f;
        PlayerCombatProfile profile = PlayerCombatProfile.of(player);
        if (!profile.active()) {
            return;
        }
        double land = profile.targetLandingDamage(DifficultyConfig.get());
        if (cancelled) {
            // DMZ applyFullNegation zeroed the hit — put the tier bite back.
            event.setCanceled(false);
            event.setAmount((float) Math.max(1.0, land));
        } else if (preAmount < land) {
            // 1.0.24: always fill up to the landing floor when short.
            // Old 0.45× gate left T4 tanks below T3 (Got2takeitez 0.17 vs 0.24).
            event.setAmount((float) Math.max(preAmount, land));
        }
        // Soft-cap crushing hits — monotonic buy ladder.
        // 2.3.57 (hits-2026-08-29..30): ease T1–T3 — T2 gods were pinned at 43% bag.
        // Ladder: T1 0.34 · T2 0.36 · T3 0.44 · T4 0.50 · T5 0.52 · T6 0.58 · T7 0.62.
        double bag = Math.max(20.0, profile.liveMaxHealth);
        double maxFrac = switch (profile.activeTier) {
            case 7 -> 0.62;
            case 6 -> 0.58;
            case 5 -> 0.52;
            case 4 -> 0.50;
            case 3 -> 0.44;
            case 2 -> 0.36;
            default -> 0.34; // T1
        };
        float softCap = (float) (bag * maxFrac);
        if (event.getAmount() > softCap) {
            event.setAmount(softCap);
        }
        // Whitelist telemetry — log pre/post so cancelled zeros stay visible.
        if (BalanceTelemetry.shouldLog(player)) {
            BalanceTelemetry.logIncomingHit(
                    player, mob, profile, preAmount, cancelled, event.getAmount());
        }
    }

    @SubscribeEvent
    public void onDeath(LivingDeathEvent event) {
        if (SystemGate.isDisabled()) {
            return;
        }
        LivingEntity dead = event.getEntity();
        // Combat gravity must drop the moment the chamber source dies — do not wait
        // for contribution TTL (and never leave a sticky GravityDeviceManager zone).
        if (dead instanceof EnderMan || dead instanceof Warden) {
            CombatGravity.removeSource(dead.m_20148_());
        }
        // Register before vanilla remove() spawns smaller slime/magma cubs.
        SlimeSplitGuard.onParentDeath(dead);
        if (dead instanceof Creeper creeper) {
            EnemyEvolution.onCreeperDeath(creeper, event.getSource());
        }
        // V3 death penalty: clear temporary active tier/level; unlocks & coins stay.
        // Uses allows() (not participates) so toggling personal OFF cannot skip the penalty.
        if (dead instanceof ServerPlayer victim) {
            // Title no-death streaks always break on death (even if personal OFF).
            TitleSystem.noteDeath(victim);
            if (SystemGate.allows(victim)
                    && DifficultyConfig.get().deathResetsActiveDifficulty) {
                var data = DifficultyCache.data(victim);
                if (data.getActiveTier() > 0 || data.getActiveDifficultyLevel() > 0L) {
                    data.resetTemporary();
                    DifficultyCache.save(victim);
                    DifficultyCache.refresh(victim);
                    ScaledMobTracker.releaseAndRevertPlayer(victim);
                    NearbyMobScaler.processEvictions();
                    victim.m_213846_(net.minecraft.network.chat.Component.m_237113_(
                            "§cDifficulty deactivated on death. §7Unlocks & Ancient Coins kept."));
                }
            }
        }
        if (!(event.getSource().m_7639_() instanceof ServerPlayer killer) || !SystemGate.participates(killer)) {
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
        ProgressionSystem.onJoin(event);
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
                || !SystemGate.participates(player)) {
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
        if (player == null || !player.m_6084_() || player.m_9236_() != mob.m_9236_()
                || !SystemGate.participates(player)) {
            player = NearbyPlayers.nearestParticipating(
                    mob, DifficultyConfig.get().mobScaleRadius);
        }
        if (player != null) {
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
