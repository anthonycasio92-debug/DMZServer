package com.dbzlegacy.adaptivedifficulty.tick;

import com.dbzlegacy.adaptivedifficulty.ai.AdaptiveAiSystem;
import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.evolution.CombatGravity;
import com.dbzlegacy.adaptivedifficulty.evolution.EnemyEvolution;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.tier.DifficultyTier;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockAbilityCaps;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Behavior tick over claimed + combat-indexed hostiles.
 * <p>
 * Unlock-tier AI / evolution kits run on every difficulty-claimed mob near a
 * participating player — not only after a combat-index mark — so dash/leap/ki
 * show up as soon as the mob is scaled to that player's tier.
 */
public final class BehaviorScheduler {
    /** Server ticks between combat-index / claim processing. */
    public static final int PULSE_INTERVAL = 20;
    /** Max hostiles processed in one pulse. */
    public static final int BUDGET_PER_PULSE = 64;

    private BehaviorScheduler() {}

    public static void pulse(MinecraftServer server, int gameTick) {
        if (server == null) {
            return;
        }
        DifficultyConfig cfg = DifficultyConfig.get();
        if (!cfg.enabled) {
            return;
        }
        List<ServerPlayer> online = server.m_6846_().m_11314_();
        if (online == null || online.isEmpty()) {
            CombatIndex.clear();
            return;
        }

        // Gravity map apply — only players with active contributions.
        if (gameTick % 10 == 0) {
            CombatGravity.tickActive(server);
        }

        // Rescale hostiles already near players to transform / limit-release stats.
        NearbyMobScaler.pulse(server, gameTick);

        if (gameTick % PULSE_INTERVAL != 0) {
            return;
        }

        boolean ai = cfg.enableAdaptiveAi;
        boolean evo = cfg.enableEnemyEvolution;
        if (!ai && !evo && !cfg.enableMutations && !cfg.enableBossScaling) {
            return;
        }

        long resolvedGameTime = gameTick;
        for (var level : server.m_129785_()) {
            if (level != null) {
                resolvedGameTime = level.m_46467_();
                break;
            }
        }
        final long gameTime = resolvedGameTime;

        final int[] budgetRef = {BUDGET_PER_PULSE};
        Set<UUID> done = new HashSet<>(64);

        // Primary path: difficulty-claimed mobs (guarantees kit ticks per unlock tier).
        ScaledMobTracker.forEachClaimed(server, (owner, mob) -> {
            if (budgetRef[0] <= 0) {
                return;
            }
            if (!SystemGate.participates(owner) || DifficultyCache.get(owner).activeTier <= 0) {
                return;
            }
            // Outer pulse is already every PULSE_INTERVAL — do not filter by entityId
            // (that previously skipped ~95% of claimed mobs).
            if (done.contains(mob.m_20148_())) {
                return;
            }
            if (process(mob, owner, ai, evo, cfg)) {
                done.add(mob.m_20148_());
                CombatIndex.mark(mob, gameTime);
                budgetRef[0]--;
            }
        });
        int budget = budgetRef[0];

        // Secondary: combat-index peers not already claimed/processed (hurt chase, etc.).
        List<Mob> engaged = CombatIndex.snapshot(server, gameTime);
        for (Mob mob : engaged) {
            if (budget <= 0) {
                break;
            }
            if (done.contains(mob.m_20148_())) {
                continue;
            }
            if (process(mob, null, ai, evo, cfg)) {
                done.add(mob.m_20148_());
                budget--;
            }
        }
    }

    private static boolean process(
            Mob mob, ServerPlayer claimOwner, boolean ai, boolean evo, DifficultyConfig cfg
    ) {
        if (DimensionGates.isDisabled(mob)) {
            CombatIndex.unmark(mob.m_20148_());
            return false;
        }
        float hp = mob.m_21223_();
        if (!(hp > 0.0f) || Float.isNaN(hp) || Float.isInfinite(hp)) {
            MobScaling.terminateIfZeroHealth(mob);
            CombatIndex.unmark(mob.m_20148_());
            return true;
        }

        // Saga/quest + spawner mobs are owned by other systems — never AI-kit them.
        if (MobScaling.isExemptFromConversion(mob)) {
            CombatIndex.unmark(mob.m_20148_());
            return false;
        }

        CompoundTag tag = PersistentDataAccess.get(mob);
        if (!tag.m_128471_(MobScaling.TAG_SCALED)) {
            MobScaling.scaleIfNeeded(mob);
            return true;
        }

        ServerPlayer owner = claimOwner;
        if (owner == null) {
            UUID ownerId = ScaledMobTracker.findClaimOwnerId(mob.m_20148_());
            if (ownerId != null && mob.m_9236_().m_7654_() != null) {
                owner = mob.m_9236_().m_7654_().m_6846_().m_11259_(ownerId);
            }
        }

        // Gate: must be fighting / claimed by a participating player with an active tier.
        LivingEntity target = mob.m_5448_();
        if (owner != null) {
            if (!SystemGate.participates(owner) || DifficultyCache.get(owner).activeTier <= 0) {
                return false;
            }
            // Keep unlock tier stamped from the claiming player so kits match Buy Tier.
            int active = DifficultyCache.get(owner).activeTier;
            tag.m_128405_("dmz_ad_unlock_tier", active);
        } else if (target instanceof ServerPlayer sp) {
            if (!SystemGate.participates(sp) || DifficultyCache.get(sp).activeTier <= 0) {
                return false;
            }
            tag.m_128405_("dmz_ad_unlock_tier", DifficultyCache.get(sp).activeTier);
        } else if (DifficultyConfig.isWhitelistEnabled()) {
            // Whitelist on + no claim/target: do not invent kits for random hostiles.
            return false;
        }

        boolean elite = tag.m_128471_(EliteSystem.TAG_ELITE);
        boolean boss = tag.m_128471_(BossScaling.TAG_BOSS);
        boolean mutated = cfg.enableMutations
                && tag.m_128441_(MutationSystem.TAG_MUTATION)
                && !tag.m_128461_(MutationSystem.TAG_MUTATION).isEmpty();
        long difficulty = tag.m_128441_(MobScaling.TAG_DIFFICULTY)
                ? tag.m_128454_(MobScaling.TAG_DIFFICULTY) : 0L;

        int unlockTier = tag.m_128451_("dmz_ad_unlock_tier");
        if (unlockTier <= 0 && difficulty <= 0 && !elite && !boss && !mutated) {
            return false;
        }

        // Stamp the resolved kit name so nameplates / debug see the live unlock band.
        DifficultyTier kit = UnlockAbilityCaps.resolve(difficulty, unlockTier);
        if (kit != DifficultyTier.NONE && PersistentDataAccess.isWritable(tag)) {
            tag.m_128359_("dmz_ad_ability_tier", kit.display);
        }

        if (mutated && unlockTier >= cfg.mutationMinUnlockTier) {
            MutationSystem.tick(mob, tag);
        }
        if (boss && cfg.enableBossScaling && unlockTier >= cfg.bossMechanicsMinUnlockTier) {
            BossScaling.tickPhases(mob, tag);
        }
        if (ai && unlockTier >= cfg.adaptiveAiMinUnlockTier) {
            AdaptiveAiSystem.tick(mob, Math.max(1L, difficulty), elite);
        }
        if (evo && unlockTier >= cfg.enemyEvolutionMinUnlockTier && EnemyEvolution.isEvolvable(mob)) {
            EnemyEvolution.tick(mob, Math.max(1L, difficulty), elite);
        }
        return true;
    }
}
