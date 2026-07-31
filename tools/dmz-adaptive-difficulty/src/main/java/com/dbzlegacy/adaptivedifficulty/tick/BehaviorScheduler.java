package com.dbzlegacy.adaptivedifficulty.tick;

import com.dbzlegacy.adaptivedifficulty.ai.AdaptiveAiSystem;
import com.dbzlegacy.adaptivedifficulty.boss.BossScaling;
import com.dbzlegacy.adaptivedifficulty.elite.EliteSystem;
import com.dbzlegacy.adaptivedifficulty.evolution.CombatGravity;
import com.dbzlegacy.adaptivedifficulty.evolution.EnemyEvolution;
import com.dbzlegacy.adaptivedifficulty.mutation.MutationSystem;
import com.dbzlegacy.adaptivedifficulty.scaling.HostileMobs;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/**
 * Concept §17 — player-centric behavior tick.
 * <p>
 * Replaces per-entity {@code LivingTickEvent} for AI / evolution / mutation / boss
 * phases. Only hostiles near online players are considered, with a per-pulse budget
 * and per-mob stagger so busy servers stay near 20 TPS.
 */
public final class BehaviorScheduler {
    /** Server ticks between player-neighborhood scans. */
    public static final int PULSE_INTERVAL = 10;
    /** How often a given mob may run heavy behavior while near players. */
    public static final int MOB_PERIOD = 40;
    /** Max hostiles processed in one pulse across the whole server. */
    public static final int BUDGET_PER_PULSE = 96;
    /** Scan radius around each player. */
    public static final double SCAN_RADIUS = 48.0;

    private BehaviorScheduler() {}

    public static void pulse(MinecraftServer server, int gameTick) {
        if (server == null) {
            return;
        }
        List<ServerPlayer> online = server.m_6846_().m_11314_();
        if (online == null || online.isEmpty()) {
            return;
        }

        // Gravity apply is cheap (map only) — batch for all online players.
        for (ServerPlayer player : online) {
            CombatGravity.tickPlayer(player);
        }

        if (gameTick % PULSE_INTERVAL != 0) {
            return;
        }

        int budget = BUDGET_PER_PULSE;
        Set<Integer> seen = new HashSet<>(128);

        for (ServerPlayer player : online) {
            if (budget <= 0) {
                return;
            }
            if (player == null || !player.m_6084_()) {
                continue;
            }
            if (!(player.m_9236_() instanceof ServerLevel level)) {
                continue;
            }
            AABB box = player.m_20191_().m_82400_(SCAN_RADIUS);
            List<Mob> mobs = level.m_45976_(Mob.class, box);
            for (Mob mob : mobs) {
                if (budget <= 0) {
                    return;
                }
                if (mob == null || !mob.m_6084_() || !HostileMobs.isHostile(mob)) {
                    continue;
                }
                int id = mob.m_19879_();
                if (!seen.add(id)) {
                    continue;
                }
                // Stagger: each mob ~ once per MOB_PERIOD while in range.
                if (Math.floorMod(gameTick + id, MOB_PERIOD) != 0) {
                    continue;
                }
                if (process(mob)) {
                    budget--;
                }
            }
        }
    }

    /**
     * @return true if meaningful work ran (counts against budget)
     */
    private static boolean process(Mob mob) {
        float hp = mob.m_21223_();
        if (!(hp > 0.0f) || Float.isNaN(hp) || Float.isInfinite(hp)) {
            MobScaling.terminateIfZeroHealth(mob);
            return true;
        }

        CompoundTag tag = PersistentDataAccess.get(mob);
        if (!tag.m_128471_(MobScaling.TAG_SCALED)) {
            // Mohist sometimes skips FinalizeSpawn — catch up here.
            MobScaling.scaleIfNeeded(mob);
            return true;
        }

        boolean elite = tag.m_128471_(EliteSystem.TAG_ELITE);
        boolean boss = tag.m_128471_(BossScaling.TAG_BOSS);
        boolean mutated = tag.m_128441_(MutationSystem.TAG_MUTATION)
                && !tag.m_128461_(MutationSystem.TAG_MUTATION).isEmpty();
        long difficulty = tag.m_128441_(MobScaling.TAG_DIFFICULTY)
                ? tag.m_128454_(MobScaling.TAG_DIFFICULTY) : 0L;

        if (difficulty <= 0 && !elite && !boss && !mutated) {
            return false;
        }

        if (mutated) {
            MutationSystem.tick(mob, tag);
        }
        if (boss) {
            BossScaling.tickPhases(mob, tag);
        }
        AdaptiveAiSystem.tick(mob, difficulty, elite);
        if (EnemyEvolution.isEvolvable(mob)) {
            EnemyEvolution.tick(mob, difficulty, elite);
        }
        return true;
    }
}
