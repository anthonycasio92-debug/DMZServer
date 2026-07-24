package com.dbzlegacy.mohistmelee;

import com.dbzlegacy.mohistmelee.CombatRepair;
import com.dbzlegacy.mohistmelee.RateLog;
import com.dragonminez.common.combat.logic.player.PlayerAttackHelper;
import com.dragonminez.common.combat.logic.player.TargetHelper;
import com.dragonminez.common.combat.player.AttackHand;
import com.dragonminez.common.network.C2S.CombatAttackRequestC2S;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class MeleeRescue {
    private static final int MAX_FALLBACK_HITS = 4;

    private MeleeRescue() {
    }

    private static CompoundTag pdata(Entity e) {
        try {
            return (CompoundTag) Entity.class.getMethod("getPersistentData").invoke(e);
        } catch (Throwable t) {
            // Forge IForgeEntity#getPersistentData
            try {
                return (CompoundTag) e.getClass().getMethod("getPersistentData").invoke(e);
            } catch (Throwable t2) {
                throw new IllegalStateException("getPersistentData unavailable", t2);
            }
        }
    }

    public static void maybeRescue(ServerPlayer player, CombatAttackRequestC2S packet, long hitTimeBefore, int packetIdCount) {
        Level level;
        AttackHand hand;
        if (player == null || player.m_9236_().f_46443_ || player.m_213877_()) {
            return;
        }
        long hitTimeNow = MeleeRescue.pdata((Entity)player).m_128454_("dmz_last_hit_target_time");
        if (hitTimeNow > hitTimeBefore) {
            return;
        }
        CombatRepair.clearCombatLock(player, "melee-fallback", true);
        CombatRepair.sanitizeReach(player, "melee-fallback");
        CombatRepair.snapshotPrimaries((Player)player);
        CombatRepair.restorePrimaries((Player)player, "melee-fallback");
        if (player.m_21205_().m_41619_() && CombatRepair.isPrimaryWiped(player)) {
            CombatRepair.respawnLikeRecovery(player, "melee-fallback-empty-hand");
        }
        if ((hand = PlayerAttackHelper.getCurrentAttack((Player)player, (int)packet.getComboCount())) == null || hand.attributes() == null) {
            RateLog.info("rescue-nohand", 40, "melee fallback: no AttackHand player={} combo={} packetIds={}", player.m_36316_().getName(), packet.getComboCount(), packetIdCount);
            return;
        }
        double weaponRange = hand.attributes().attackRange();
        double range = PlayerAttackHelper.getEffectiveAttackRange((Player)player, (double)weaponRange);
        if (!Double.isFinite(range) || range < 0.75) {
            range = Math.max(2.0, weaponRange);
        }
        if (!((level = player.m_9236_()) instanceof ServerLevel)) {
            return;
        }
        ServerLevel serverLevel = (ServerLevel)level;
        List<LivingEntity> packetCandidates = MeleeRescue.resolvePacketCandidates(player, serverLevel, packet);
        int nullIds = MeleeRescue.countNullPacketIds(serverLevel, packet);
        int hits = MeleeRescue.tryHitCandidates(player, packetCandidates, range, hitTimeBefore);
        int nearbyCount = -1;
        if (hits == 0) {
            List<LivingEntity> nearby = MeleeRescue.scanNearby(player, serverLevel, range);
            nearbyCount = nearby.size();
            LinkedHashSet<Integer> alreadyTried = new LinkedHashSet<Integer>();
            for (LivingEntity e : packetCandidates) {
                alreadyTried.add(e.m_19879_());
            }
            ArrayList<LivingEntity> fresh = new ArrayList<LivingEntity>();
            for (LivingEntity e : nearby) {
                if (alreadyTried.contains(e.m_19879_())) continue;
                fresh.add(e);
            }
            hits = MeleeRescue.tryHitCandidates(player, fresh.isEmpty() ? nearby : fresh, range, hitTimeBefore);
        }
        if (hits > 0) {
            RateLog.info("rescue-hit", 60, "melee fallback HIT player={} hits={} packetIds={} nullIds={} nearby={} range={}", player.m_36316_().getName(), hits, packetIdCount, nullIds, nearbyCount, range);
        } else {
            RateLog.info("rescue-miss", 40, "melee fallback: no valid target player={} packetCand={} nearby={} packetIds={} nullIds={} range={}", player.m_36316_().getName(), packetCandidates.size(), nearbyCount, packetIdCount, nullIds, range);
        }
    }

    private static List<LivingEntity> resolvePacketCandidates(ServerPlayer player, ServerLevel level, CombatAttackRequestC2S packet) {
        ArrayList<LivingEntity> out = new ArrayList<LivingEntity>();
        int[] ids = packet.getEntityIds();
        if (ids == null) {
            return out;
        }
        for (int id : ids) {
            LivingEntity living;
            Entity entity = TargetHelper.getEntityOrPart((Level)level, id);
            if (entity == null || !((entity = TargetHelper.resolveHittable(entity)) instanceof LivingEntity) || !(living = (LivingEntity)entity).m_6084_() || living == player) continue;
            out.add(living);
        }
        return out;
    }

    private static int countNullPacketIds(ServerLevel level, CombatAttackRequestC2S packet) {
        int[] ids = packet.getEntityIds();
        if (ids == null) {
            return 0;
        }
        int n = 0;
        for (int id : ids) {
            if (TargetHelper.getEntityOrPart((Level)level, id) != null) continue;
            ++n;
        }
        return n;
    }

    private static List<LivingEntity> scanNearby(ServerPlayer player, ServerLevel level, double range) {
        AABB box = player.m_20191_().m_82400_(range + 2.5);
        ArrayList<LivingEntity> out = new ArrayList<LivingEntity>();
        for (LivingEntity e : level.m_45976_(LivingEntity.class, box)) {
            if (e == null || e == player || !e.m_6084_()) continue;
            out.add(e);
        }
        return out;
    }

    private static int tryHitCandidates(ServerPlayer player, List<LivingEntity> candidates, double range, long hitTimeBefore) {
        if (candidates == null || candidates.isEmpty()) {
            return 0;
        }
        Vec3 look = player.m_20154_().m_82541_();
        Vec3 eye = player.m_146892_();
        double rangeSq = range * range + 16.0;
        candidates.sort(Comparator.comparingDouble(e -> {
            Vec3 toTarget = e.m_20191_().m_82399_().m_82546_(eye);
            double dist = player.m_20280_((Entity)e);
            double align = toTarget.m_82556_() < 1.0E-6 ? 0.0 : toTarget.m_82541_().m_82526_(look);
            return dist - align * 4.0;
        }));
        int hits = 0;
        for (LivingEntity target : candidates) {
            double dot;
            Vec3 toTarget;
            if (hits >= 4) break;
            if (player.m_20280_((Entity)target) > rangeSq || !TargetHelper.canAttack((Player)player, (Entity)target, range + 4.0) || (toTarget = target.m_20191_().m_82399_().m_82546_(eye)).m_82556_() < 1.0E-6 || (dot = toTarget.m_82541_().m_82526_(look)) < 0.0) continue;
            MeleeRescue.pdata((Entity)player).m_128379_("dmz_first_hit", hits == 0);
            TargetHelper.Relation relation = TargetHelper.getRelation((Player)player, (Entity)target);
            TargetHelper.onSuccessfulAttack((Player)player, (Entity)target, relation);
            float healthBefore = target.m_21223_();
            player.m_5706_((Entity)target);
            long hitTimeNow = MeleeRescue.pdata((Entity)player).m_128454_("dmz_last_hit_target_time");
            boolean landed = target.m_21223_() < healthBefore || target.f_19802_ > 0 || hitTimeNow > hitTimeBefore;
            if (!landed) continue;
            ++hits;
        }
        MeleeRescue.pdata((Entity)player).m_128473_("dmz_first_hit");
        return hits;
    }
}

