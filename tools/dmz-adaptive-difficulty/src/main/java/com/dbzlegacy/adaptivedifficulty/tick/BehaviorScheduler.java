package com.dbzlegacy.adaptivedifficulty.tick;

import com.dbzlegacy.adaptivedifficulty.ai.AdaptiveAiSystem;
import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.evolution.CombatGravity;
import com.dbzlegacy.adaptivedifficulty.evolution.EnemyEvolution;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.util.DimensionGates;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Concept §17 — behavior tick over the combat index only.
 * <p>
 * Never calls {@code getEntitiesOfClass}. Engaged mobs are registered from
 * target/hurt events via {@link CombatIndex}.
 */
public final class BehaviorScheduler {
    /** Server ticks between combat-index processing. */
    public static final int PULSE_INTERVAL = 20;
    /** Max hostiles processed in one pulse. */
    public static final int BUDGET_PER_PULSE = 48;

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

        if (gameTick % PULSE_INTERVAL != 0) {
            return;
        }

        boolean ai = cfg.enableAdaptiveAi;
        boolean evo = cfg.enableEnemyEvolution;
        if (!ai && !evo && !cfg.enableMutations && !cfg.enableBossScaling) {
            return;
        }

        // Prefer overworld game-time so TTL matches CombatIndex.mark(level.getGameTime()).
        long gameTime = gameTick;
        for (var level : server.m_129785_()) {
            if (level != null) {
                gameTime = level.m_46467_();
                break;
            }
        }
        List<Mob> engaged = CombatIndex.snapshot(server, gameTime);
        int budget = BUDGET_PER_PULSE;
        for (Mob mob : engaged) {
            if (budget <= 0) {
                return;
            }
            // Stagger within the engaged set.
            if (Math.floorMod(gameTick + mob.m_19879_(), 40) != 0) {
                continue;
            }
            if (process(mob, ai, evo, cfg)) {
                budget--;
            }
        }
    }

    private static boolean process(Mob mob, boolean ai, boolean evo, DifficultyConfig cfg) {
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

        CompoundTag tag = PersistentDataAccess.get(mob);
        if (!tag.m_128471_(MobScaling.TAG_SCALED)) {
            MobScaling.scaleIfNeeded(mob);
            return true;
        }

        boolean elite = tag.m_128471_(EliteSystem.TAG_ELITE);
        boolean boss = tag.m_128471_(BossScaling.TAG_BOSS);
        boolean mutated = cfg.enableMutations
                && tag.m_128441_(MutationSystem.TAG_MUTATION)
                && !tag.m_128461_(MutationSystem.TAG_MUTATION).isEmpty();
        long difficulty = tag.m_128441_(MobScaling.TAG_DIFFICULTY)
                ? tag.m_128454_(MobScaling.TAG_DIFFICULTY) : 0L;

        if (difficulty <= 0 && !elite && !boss && !mutated) {
            return false;
        }

        int unlockTier = tag.m_128451_("dmz_ad_unlock_tier");
        if (mutated && unlockTier >= cfg.mutationMinUnlockTier) {
            MutationSystem.tick(mob, tag);
        }
        if (boss && cfg.enableBossScaling && unlockTier >= cfg.bossMechanicsMinUnlockTier) {
            BossScaling.tickPhases(mob, tag);
        }
        if (ai && unlockTier >= cfg.adaptiveAiMinUnlockTier) {
            AdaptiveAiSystem.tick(mob, difficulty, elite);
        }
        if (evo && unlockTier >= cfg.enemyEvolutionMinUnlockTier && EnemyEvolution.isEvolvable(mob)) {
            EnemyEvolution.tick(mob, difficulty, elite);
        }
        return true;
    }
}
