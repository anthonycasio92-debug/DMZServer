package com.dbzlegacy.mohistmelee;

import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import com.dragonminez.server.events.players.combat.CombatEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * DMZ sends {@code MeleeAnimationS2C} <b>before</b> iterating packet entity IDs.
 * After a world change the client often sends an empty list or stale IDs that do not
 * resolve on the server — animation plays, {@code ServerPlayer.attack} never runs.
 * <p>
 * If the packet produced no LivingHurt (no {@code dmz_last_hit_target_time} bump),
 * scan server-side for a valid target in front of the player and attack it.
 * Still uses vanilla {@code attack()} — no damage redirects.
 */
public final class ServerMeleeFallback {
    private static final Logger LOGGER = LogManager.getLogger(DmzMohistMeleeFix.MOD_ID);
    private static final AtomicInteger LOGS = new AtomicInteger();
    private static final int MAX_FALLBACK_HITS = 4;

    private ServerMeleeFallback() {}

    /**
     * Must be queued on the server <em>after</em> {@code processAttackRequest}'s runnable
     * so we can see whether a hit already landed.
     */
    public static void maybeRescue(
            ServerPlayer player,
            CombatAttackRequestC2S request,
            long hitTimeBefore,
            int packetIdCount
    ) {
        if (player == null || player.m_9236_().f_46443_ || player.m_213877_()) {
            return;
        }
        long hitTimeAfter = PersistentDataAccess.get(player).m_128454_(CombatEvent.DMZ_LAST_HIT_TARGET_TIME_TAG);
        if (hitTimeAfter > hitTimeBefore) {
            return; // packet path already applied LivingHurt
        }

        AttackHand hand = PlayerAttackHelper.getCurrentAttack((Player) player, request.getComboCount());
        if (hand == null || hand.attributes() == null) {
            return;
        }

        double weaponRange = hand.attributes().attackRange();
        double maxRange = PlayerAttackHelper.getEffectiveAttackRange((Player) player, weaponRange);
        if (!Double.isFinite(maxRange) || maxRange < 0.75D) {
            maxRange = Math.max(2.0D, weaponRange);
        }

        Level level = player.m_9236_();
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        // Prefer resolving packet IDs again (in case of race); then AABB scan.
        List<LivingEntity> candidates = new ArrayList<>();
        int[] ids = request.getEntityIds();
        int nullIds = 0;
        if (ids != null) {
            for (int id : ids) {
                Entity entity = TargetHelper.getEntityOrPart((Level) serverLevel, id);
                if (entity == null) {
                    nullIds++;
                    continue;
                }
                entity = TargetHelper.resolveHittable(entity);
                if (entity instanceof LivingEntity living && living.m_6084_() && living != player) {
                    candidates.add(living);
                }
            }
        }

        if (candidates.isEmpty()) {
            double inflate = maxRange + 2.5D;
            AABB box = player.m_20191_().m_82400_(inflate);
            List<LivingEntity> nearby = serverLevel.m_45976_(LivingEntity.class, box);
            for (LivingEntity living : nearby) {
                if (living == null || living == player || !living.m_6084_()) {
                    continue;
                }
                candidates.add(living);
            }
        }

        if (candidates.isEmpty()) {
            int n = LOGS.incrementAndGet();
            if (n <= 40) {
                LOGGER.info(
                        "[{}] melee fallback: no candidates player={} packetIds={} nullIds={} range={}",
                        DmzMohistMeleeFix.MOD_ID,
                        player.m_36316_().getName(),
                        packetIdCount,
                        nullIds,
                        maxRange
                );
            }
            return;
        }

        Vec3 look = player.m_20154_().m_82541_();
        Vec3 eye = player.m_146892_();
        double rangeSq = maxRange * maxRange + 16.0D;
        final double useRange = maxRange;
        candidates.sort(Comparator.comparingDouble((LivingEntity e) -> {
            Vec3 to = e.m_20191_().m_82399_().m_82546_(eye);
            double dist = player.m_20280_(e);
            double align = to.m_82556_() < 1.0E-6D ? 0.0D : to.m_82541_().m_82526_(look);
            return dist - align * 4.0D; // prefer closer + more in-front
        }));

        int hits = 0;
        for (LivingEntity target : candidates) {
            if (hits >= MAX_FALLBACK_HITS) {
                break;
            }
            if (player.m_20280_(target) > rangeSq) {
                continue;
            }
            if (!TargetHelper.canAttack((Player) player, (Entity) target, useRange + 4.0D)) {
                continue;
            }
            // Require roughly in front so we don't slap random mobs behind the player.
            Vec3 to = target.m_20191_().m_82399_().m_82546_(eye);
            if (to.m_82556_() < 1.0E-6D) {
                continue;
            }
            double align = to.m_82541_().m_82526_(look);
            if (align < 0.15D) {
                continue;
            }

            PersistentDataAccess.get(player).m_128379_("dmz_first_hit", hits == 0);
            TargetHelper.Relation relation = TargetHelper.getRelation((Player) player, (Entity) target);
            TargetHelper.onSuccessfulAttack((Player) player, (Entity) target, relation);
            player.m_5706_(target);
            hits++;
        }
        PersistentDataAccess.get(player).m_128473_("dmz_first_hit");

        if (hits > 0) {
            int n = LOGS.incrementAndGet();
            if (n <= 60) {
                LOGGER.info(
                        "[{}] melee fallback HIT player={} hits={} packetIds={} nullIds={} range={}",
                        DmzMohistMeleeFix.MOD_ID,
                        player.m_36316_().getName(),
                        hits,
                        packetIdCount,
                        nullIds,
                        maxRange
                );
            }
        } else {
            int n = LOGS.incrementAndGet();
            if (n <= 40) {
                LOGGER.info(
                        "[{}] melee fallback: candidates but none valid player={} candidates={} packetIds={} nullIds={} range={}",
                        DmzMohistMeleeFix.MOD_ID,
                        player.m_36316_().getName(),
                        candidates.size(),
                        packetIdCount,
                        nullIds,
                        maxRange
                );
            }
        }
    }
}
