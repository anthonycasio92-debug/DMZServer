package com.dbzlegacy.mohistmelee;

import com.dbzlegacy.mohistmelee.RateLog;
import com.dragonminez.common.init.MainAttributes;
import com.dragonminez.common.init.MainEffects;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.ResourceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Stats;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.techniques.Techniques;
import com.dragonminez.server.events.players.StatsEvents;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.registries.RegistryObject;

public final class CombatRepair {
    private static final Map<UUID, int[]> SNAPSHOTS = new ConcurrentHashMap<UUID, int[]>();
    /** Suppress restore/read-fallback while an intentional DMZ stat reset is in progress. */
    private static final Map<UUID, Integer> SUPPRESS_TICKS = new ConcurrentHashMap<UUID, Integer>();
    private static final RegistryObject<Attribute>[] PRIMARIES = CombatRepair.primaries();
    private static final String[] PRIMARY_KEYS = new String[]{"STR", "SKP", "RES", "VIT", "PWR", "ENE"};

    private CombatRepair() {
    }

    private static RegistryObject<Attribute>[] primaries() {
        return new RegistryObject[]{MainAttributes.STRENGTH, MainAttributes.STRIKE_POWER, MainAttributes.RESISTANCE, MainAttributes.VITALITY, MainAttributes.KI_POWER, MainAttributes.ENERGY};
    }

    public static void repairAll(ServerPlayer player, String reason) {
        if (player == null || player.m_9236_().f_46443_) {
            return;
        }
        if (!CombatRepair.isSuppressed((Player)player)) {
            CombatRepair.snapshotPrimaries((Player)player);
            CombatRepair.restorePrimaries((Player)player, reason);
        }
        CombatRepair.sanitizeReach(player, reason);
        CombatRepair.clearCombatLock(player, reason, false);
        CombatRepair.sync(player);
    }

    public static boolean clearCombatLock(ServerPlayer player, String reason, boolean doSync) {
        if (player == null) {
            return false;
        }
        boolean legitStrike = CombatRepair.isInActiveStrike(player);
        if (!legitStrike) {
            CombatRepair.abortStrikeMaps(player);
        }
        boolean[] changed = new boolean[]{false};
        StatsProvider.get(StatsCapability.INSTANCE, (Entity)player).ifPresent(raw -> {
            StatsData data = (StatsData) raw;
            boolean wasLocked;
            Status status = data.getStatus();
            boolean bl = wasLocked = status.isStrikeLocked() || status.isKnockedDown() || status.isStunEffect();
            if (!legitStrike) {
                status.setStrikeLocked(false);
                status.setKnockedDown(false);
                status.setStunEffect(false);
            }
            try {
                Techniques tech = data.getTechniques();
                if (tech.isTechniqueCharging() || tech.isTechniqueChargeActive()) {
                    tech.clearTechniqueCharge();
                }
                status.setChargingKi(false);
                status.setActionCharging(false);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            try {
                data.getCooldowns().removeCooldown("KnockdownDuration");
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            changed[0] = wasLocked;
        });
        CombatRepair.removeStunPotion(player);
        if (changed[0]) {
            RateLog.info("clear", 80, "cleared stale strike lock player={} reason={}", player.m_36316_().getName(), reason);
        }
        if (doSync) {
            CombatRepair.sync(player);
        }
        return changed[0];
    }

    public static boolean hasRealStunPotion(ServerPlayer player) {
        try {
            MobEffect stun = (MobEffect)MainEffects.STUN.get();
            return stun != null && player.m_21023_(stun);
        }
        catch (Throwable t) {
            return false;
        }
    }

    private static void removeStunPotion(ServerPlayer player) {
        try {
            MobEffect stun = (MobEffect)MainEffects.STUN.get();
            if (stun != null) {
                player.m_21195_(stun);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    public static boolean sanitizeReach(ServerPlayer player, String reason) {
        if (player == null) {
            return false;
        }
        boolean changed = false;
        try {
            Attribute entityReach = (Attribute)ForgeMod.ENTITY_REACH.get();
            Attribute blockReach = (Attribute)ForgeMod.BLOCK_REACH.get();
            if (entityReach != null) {
                changed |= CombatRepair.repairReach((Player)player, entityReach, "forge:entity_reach", 3.0, reason);
            }
            if (blockReach != null) {
                changed |= CombatRepair.repairReach((Player)player, blockReach, "forge:block_reach", 4.5, reason);
            }
            changed |= CombatRepair.repairFiniteBaseOnly((Player)player, Attributes.f_22281_, "minecraft:generic.attack_damage", 1.0, reason);
        }
        catch (Throwable t) {
            RateLog.warn("reach", 5, "reach repair failed: {}", t.toString());
        }
        return changed;
    }

    private static boolean repairReach(Player player, Attribute attr, String name, double fallbackDefault, String reason) {
        double base;
        AttributeInstance inst = player.m_21051_(attr);
        if (inst == null) {
            RateLog.warn("reach-missing", 20, "missing {} player={} reason={}", name, CombatRepair.playerName(player), reason);
            return false;
        }
        boolean changed = false;
        double dflt = attr.m_22082_();
        if (!Double.isFinite(dflt) || dflt <= 0.0) {
            dflt = fallbackDefault;
        }
        if (!Double.isFinite(base = inst.m_22115_()) || base < 0.25) {
            inst.m_22100_(dflt);
            changed = true;
        }
        changed |= CombatRepair.stripNonFiniteModifiers(inst);
        double value = inst.m_22135_();
        if (!Double.isFinite(value)) {
            inst.m_22100_(dflt);
            changed = true;
        }
        if (changed) {
            RateLog.info("reach-fix", 40, "repaired {} player={} base={}->{} value={} reason={}", name, CombatRepair.playerName(player), base, inst.m_22115_(), inst.m_22135_(), reason);
        }
        return changed;
    }

    private static boolean repairFiniteBaseOnly(Player player, Attribute attr, String name, double fallbackDefault, String reason) {
        if (attr == null) {
            return false;
        }
        AttributeInstance inst = player.m_21051_(attr);
        if (inst == null) {
            return false;
        }
        double base = inst.m_22115_();
        if (Double.isFinite(base)) {
            return CombatRepair.stripNonFiniteModifiers(inst);
        }
        double dflt = attr.m_22082_();
        if (!Double.isFinite(dflt) || dflt < 0.0) {
            dflt = fallbackDefault;
        }
        inst.m_22100_(dflt);
        CombatRepair.stripNonFiniteModifiers(inst);
        RateLog.info("reach-fix", 40, "repaired {} player={} base={}->{} value={} reason={}", name, CombatRepair.playerName(player), base, inst.m_22115_(), inst.m_22135_(), reason);
        return true;
    }

    private static boolean stripNonFiniteModifiers(AttributeInstance inst) {
        ArrayList<UUID> bad = new ArrayList<UUID>();
        for (AttributeModifier mod : inst.m_22122_()) {
            if (Double.isFinite(mod.m_22218_())) continue;
            bad.add(mod.m_22209_());
        }
        for (UUID id : bad) {
            inst.m_22120_(id);
        }
        return !bad.isEmpty();
    }

    public static double sanitizeEffectiveRange(Player player, double weaponRange, double computed) {
        double floor = Math.max(2.0, weaponRange > 0.0 ? weaponRange : 2.0);
        if (Double.isFinite(computed) && computed >= floor) {
            return computed;
        }
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            CombatRepair.sanitizeReach(sp, "sanitize-range");
        }
        try {
            AttributeInstance inst;
            Attribute entityReach = (Attribute)ForgeMod.ENTITY_REACH.get();
            AttributeInstance attributeInstance = inst = entityReach == null ? null : player.m_21051_(entityReach);
            if (inst != null) {
                double recomputed;
                double reachValue;
                double reachDefault = entityReach.m_22082_();
                if (!Double.isFinite(reachDefault) || reachDefault <= 0.0) {
                    reachDefault = 3.0;
                }
                if (!Double.isFinite(reachValue = inst.m_22135_()) || reachValue < reachDefault) {
                    reachValue = reachDefault;
                }
                if (Double.isFinite(recomputed = weaponRange + (reachValue - reachDefault)) && recomputed >= floor) {
                    return recomputed;
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        RateLog.info("range", 30, "sanitized range player={} weapon={} computed={} -> {}", CombatRepair.playerName(player), weaponRange, computed, floor);
        return floor;
    }

    public static void snapshotPrimaries(Player player) {
        if (player == null || CombatRepair.isSuppressed(player)) {
            return;
        }
        try {
            int[] prev = SNAPSHOTS.getOrDefault(player.m_20148_(), new int[PRIMARIES.length]);
            int[] next = Arrays.copyOf(prev, PRIMARIES.length);
            boolean any = false;
            for (int i = 0; i < PRIMARIES.length; ++i) {
                double base;
                AttributeInstance inst;
                Attribute attr = (Attribute)PRIMARIES[i].get();
                if (attr == null || (inst = player.m_21051_(attr)) == null || !Double.isFinite(base = inst.m_22115_()) || !(base > 0.0)) continue;
                next[i] = (int)Math.round(base);
                any = true;
            }
            if (any) {
                SNAPSHOTS.put(player.m_20148_(), next);
            }
        }
        catch (Throwable t) {
            RateLog.warn("snap", 5, "primary snapshot failed: {}", t.toString());
        }
    }

    /**
     * Record an intentional DMZ primary write (including 0). Mohist dim-wipes do not go through
     * {@code Stats.setAttributeBaseValue}, so those keep the previous snapshot for recovery.
     */
    public static void recordPrimaryWrite(Player player, Attribute attribute, int value) {
        if (player == null || attribute == null) {
            return;
        }
        try {
            int idx = -1;
            for (int i = 0; i < PRIMARIES.length; ++i) {
                Attribute primary = (Attribute)PRIMARIES[i].get();
                if (primary == attribute || (primary != null && primary.equals(attribute))) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                return;
            }
            int[] prev = SNAPSHOTS.getOrDefault(player.m_20148_(), new int[PRIMARIES.length]);
            int[] next = Arrays.copyOf(prev, PRIMARIES.length);
            next[idx] = Math.max(0, value);
            SNAPSHOTS.put(player.m_20148_(), next);
        }
        catch (Throwable t) {
            RateLog.warn("record", 5, "primary record failed: {}", t.toString());
        }
    }

    /** Overwrite snapshot from live bases (0 included) after {@code resetPlayerProgress}. */
    public static void adoptCurrent(Player player) {
        if (player == null) {
            return;
        }
        try {
            int[] next = new int[PRIMARIES.length];
            for (int i = 0; i < PRIMARIES.length; ++i) {
                Attribute attr = (Attribute)PRIMARIES[i].get();
                if (attr == null) {
                    next[i] = 0;
                    continue;
                }
                AttributeInstance inst = player.m_21051_(attr);
                if (inst == null) {
                    next[i] = 0;
                    continue;
                }
                double base = inst.m_22115_();
                next[i] = Double.isFinite(base) ? Math.max(0, (int)Math.round(base)) : 0;
            }
            SNAPSHOTS.put(player.m_20148_(), next);
            RateLog.info("adopt", 40, "adopted primary snapshot player={} STR={} SKP={} RES={} VIT={} PWR={} ENE={}",
                    CombatRepair.playerName(player), next[0], next[1], next[2], next[3], next[4], next[5]);
        }
        catch (Throwable t) {
            RateLog.warn("adopt", 5, "primary adopt failed: {}", t.toString());
        }
    }

    public static void suppressRestore(Player player, int ticks) {
        if (player == null) {
            return;
        }
        SUPPRESS_TICKS.put(player.m_20148_(), Math.max(1, ticks));
    }

    public static void tickSuppress(Player player) {
        if (player == null) {
            return;
        }
        UUID id = player.m_20148_();
        Integer left = SUPPRESS_TICKS.get(id);
        if (left == null) {
            return;
        }
        if (left <= 1) {
            SUPPRESS_TICKS.remove(id);
        } else {
            SUPPRESS_TICKS.put(id, left - 1);
        }
    }

    public static boolean isSuppressed(Player player) {
        if (player == null) {
            return false;
        }
        Integer left = SUPPRESS_TICKS.get(player.m_20148_());
        return left != null && left > 0;
    }

    public static boolean restorePrimaries(Player player, String reason) {
        if (player == null || CombatRepair.isSuppressed(player)) {
            return false;
        }
        boolean changed = false;
        try {
            int[] saved = SNAPSHOTS.get(player.m_20148_());
            if (saved == null) {
                return false;
            }
            for (int i = 0; i < PRIMARIES.length; ++i) {
                Attribute attr = (Attribute)PRIMARIES[i].get();
                if (attr == null || saved[i] <= 0) continue;
                AttributeInstance inst = player.m_21051_(attr);
                if (inst == null) {
                    inst = CombatRepair.injectAttribute(player, attr, saved[i]);
                    if (inst == null) continue;
                    changed = true;
                    RateLog.info("prim", 40, "restored primary {} player={} base=inject->{} reason={}", PRIMARY_KEYS[i], CombatRepair.playerName(player), saved[i], reason);
                    continue;
                }
                double base = inst.m_22115_();
                if (Double.isFinite(base) && base > 0.0) continue;
                inst.m_22100_((double)saved[i]);
                changed = true;
                RateLog.info("prim", 40, "restored primary {} player={} base={}->{} reason={}", PRIMARY_KEYS[i], CombatRepair.playerName(player), base, saved[i], reason);
            }
        }
        catch (Throwable t) {
            RateLog.warn("prim", 5, "primary restore failed: {}", t.toString());
        }
        return changed;
    }

    public static int savedFor(Player player, Attribute attr) {
        if (player == null || attr == null || CombatRepair.isSuppressed(player)) {
            return 0;
        }
        int[] saved = SNAPSHOTS.get(player.m_20148_());
        if (saved == null) {
            return 0;
        }
        for (int i = 0; i < PRIMARIES.length; ++i) {
            try {
                if (PRIMARIES[i].get() != attr) continue;
                return saved[i];
            }
            catch (Throwable throwable) {
                // empty catch block
            }
        }
        return 0;
    }

    public static boolean restoreOnly(Player player, String reason) {
        return CombatRepair.restorePrimaries(player, reason);
    }

    /** Snapshot then restore — skipped while an intentional reset suppress window is active. */
    public static boolean ensurePrimaries(Player player, String reason) {
        if (player == null || CombatRepair.isSuppressed(player)) {
            return false;
        }
        CombatRepair.snapshotPrimaries(player);
        return CombatRepair.restorePrimaries(player, reason);
    }

    /**
     * Clear snapshot + suppress. Callers that need suppress after a clear must re-apply
     * {@link #suppressRestore} (same pattern as the verified 2.12.0 PrimaryStatRepair.clear).
     */
    public static void dropSnapshot(UUID id) {
        if (id != null) {
            SNAPSHOTS.remove(id);
            SUPPRESS_TICKS.remove(id);
        }
    }

    /** Force snapshot slots to 0 after a full (non-percentage) progress reset. */
    public static void forceZeroSnapshot(Player player) {
        if (player == null) {
            return;
        }
        SNAPSHOTS.put(player.m_20148_(), new int[PRIMARIES.length]);
        RateLog.info("adopt", 40, "forced zero primary snapshot player={}", CombatRepair.playerName(player));
    }

    /** Begin an intentional DMZ reset: cancel delayed repairs, clear snapshot, suppress restore. */
    public static void beginIntentionalReset(Player player, int suppressTicks) {
        if (player == null) {
            return;
        }
        if (player instanceof ServerPlayer sp) {
            RepairEvents.cancelFollowups(sp);
        }
        // Match verified 2.12.0: suppress, clear (clears suppress), suppress again.
        CombatRepair.suppressRestore(player, suppressTicks);
        CombatRepair.dropSnapshot(player.m_20148_());
        CombatRepair.suppressRestore(player, suppressTicks);
    }

    /** Finish an intentional DMZ reset: adopt live bases (or zeros), keep suppress briefly. */
    public static void endIntentionalReset(Player player, Integer keepPercentage, int suppressTicks) {
        if (player == null) {
            return;
        }
        if (keepPercentage == null) {
            CombatRepair.forceZeroSnapshot(player);
        } else {
            CombatRepair.adoptCurrent(player);
        }
        CombatRepair.suppressRestore(player, suppressTicks);
    }

    public static void respawnLikeRecovery(ServerPlayer player, String reason) {
        if (player == null || player.m_9236_().f_46443_) {
            return;
        }
        if (CombatRepair.isSuppressed((Player)player)) {
            CombatRepair.sanitizeReach(player, reason + "-respawn-like-suppressed");
            return;
        }
        try {
            CombatRepair.snapshotPrimaries((Player)player);
            CombatRepair.ensureReachInstances((Player)player);
            StatsProvider.get(StatsCapability.INSTANCE, (Entity)player).ifPresent(data -> {
                Stats stats = data.getStats();
                stats.setPlayer((Player)player);
                try {
                    data.getResources().setPlayer((Player)player);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                int str = CombatRepair.preferSaved((Player)player, (RegistryObject<Attribute>)MainAttributes.STRENGTH, stats.getStrength());
                int skp = CombatRepair.preferSaved((Player)player, (RegistryObject<Attribute>)MainAttributes.STRIKE_POWER, stats.getStrikePower());
                int res = CombatRepair.preferSaved((Player)player, (RegistryObject<Attribute>)MainAttributes.RESISTANCE, stats.getResistance());
                int vit = CombatRepair.preferSaved((Player)player, (RegistryObject<Attribute>)MainAttributes.VITALITY, stats.getVitality());
                int pwr = CombatRepair.preferSaved((Player)player, (RegistryObject<Attribute>)MainAttributes.KI_POWER, stats.getKiPower());
                int ene = CombatRepair.preferSaved((Player)player, (RegistryObject<Attribute>)MainAttributes.ENERGY, stats.getEnergy());
                CombatRepair.ensurePrimaryInstances((Player)player, str, skp, res, vit, pwr, ene);
                stats.setStrength(str);
                stats.setStrikePower(skp);
                stats.setResistance(res);
                stats.setVitality(vit);
                stats.setKiPower(pwr);
                stats.setEnergy(ene);
                data.getStatus().setStrikeLocked(false);
                data.getStatus().setStunEffect(false);
                data.getStatus().setKnockedDown(false);
                try {
                    data.getCooldowns().removeCooldown("KnockdownDuration");
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
                try {
                    StatsEvents.applyHealthBonus((ServerPlayer)player);
                }
                catch (Throwable t) {
                    RateLog.warn("healthbonus", 5, "applyHealthBonus failed: {}", t.toString());
                }
            });
            try {
                player.m_6210_();
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            CombatRepair.sanitizeReach(player, reason + "-respawn-like");
            CombatRepair.snapshotPrimaries((Player)player);
            CombatRepair.restorePrimaries((Player)player, reason + "-respawn-like");
            try {
                NetworkHandler.sendToTrackingEntityAndSelf((Object)new StatsSyncS2C(player), (Entity)player);
                NetworkHandler.sendToTrackingEntityAndSelf((Object)new ResourceSyncS2C(player), (Entity)player);
            }
            catch (Throwable throwable) {
                // empty catch block
            }
            RateLog.info("respawn", 40, "respawn-like recovery player={} reason={}", player.m_36316_().getName(), reason);
        }
        catch (Throwable t) {
            RateLog.warn("respawn", 8, "respawn-like recovery failed: {}", t.toString());
        }
    }

    public static boolean isPrimaryWiped(ServerPlayer player) {
        try {
            Attribute attr = (Attribute)MainAttributes.STRENGTH.get();
            if (attr == null) {
                return false;
            }
            AttributeInstance inst = player.m_21051_(attr);
            if (inst == null) {
                return true;
            }
            double base = inst.m_22115_();
            return !Double.isFinite(base) || base <= 0.0;
        }
        catch (Throwable t) {
            return false;
        }
    }

    private static int preferSaved(Player player, RegistryObject<Attribute> attr, int current) {
        if (current > 0) {
            return current;
        }
        try {
            return Math.max(current, CombatRepair.savedFor(player, (Attribute)attr.get()));
        }
        catch (Throwable t) {
            return current;
        }
    }

    private static void ensurePrimaryInstances(Player player, int str, int skp, int res, int vit, int pwr, int ene) {
        int[] vals = new int[]{str, skp, res, vit, pwr, ene};
        for (int i = 0; i < PRIMARIES.length; ++i) {
            Attribute attr = (Attribute)PRIMARIES[i].get();
            if (attr == null || player.m_21051_(attr) != null) continue;
            double v = vals[i] > 0 ? (double)vals[i] : attr.m_22082_();
            CombatRepair.injectAttribute(player, attr, v);
        }
        try {
            Attribute melee = (Attribute)MainAttributes.MELEE_DAMAGE.get();
            if (melee != null && player.m_21051_(melee) == null) {
                CombatRepair.injectAttribute(player, melee, 1.0);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static void ensureReachInstances(Player player) {
        try {
            Attribute blockReach;
            Attribute entityReach = (Attribute)ForgeMod.ENTITY_REACH.get();
            if (entityReach != null && player.m_21051_(entityReach) == null) {
                double d = entityReach.m_22082_();
                CombatRepair.injectAttribute(player, entityReach, Double.isFinite(d) && d > 0.0 ? d : 3.0);
            }
            if ((blockReach = (Attribute)ForgeMod.BLOCK_REACH.get()) != null && player.m_21051_(blockReach) == null) {
                double d = blockReach.m_22082_();
                CombatRepair.injectAttribute(player, blockReach, Double.isFinite(d) && d > 0.0 ? d : 4.5);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    static AttributeInstance injectAttribute(Player player, Attribute attr, double value) {
        try {
            AttributeMap map = player.m_21204_();
            Field field = null;
            Class<?> c = map.getClass();
            while (c != null && field == null) {
                try {
                    field = c.getDeclaredField("f_22139_");
                }
                catch (NoSuchFieldException e) {
                    c = c.getSuperclass();
                }
            }
            if (field == null) {
                return null;
            }
            field.setAccessible(true);
            Object raw = field.get(map);
            if (!(raw instanceof Map)) {
                return null;
            }
            Map instances = (Map)raw;
            AttributeInstance inst = new AttributeInstance(attr, a -> {});
            inst.m_22100_(value);
            instances.put(attr, inst);
            return inst;
        }
        catch (Throwable t) {
            return null;
        }
    }

    private static void abortStrikeMaps(ServerPlayer player) {
        try {
            Class<?> c = Class.forName("com.dragonminez.server.events.players.combat.StrikeAttackHandler");
            UUID id = player.m_20148_();
            for (String name : new String[]{"ACTIVE", "PENDING", "STRIKE_ANCHOR_PART"}) {
                try {
                    Field f = c.getDeclaredField(name);
                    f.setAccessible(true);
                    Object raw = f.get(null);
                    if (!(raw instanceof Map)) continue;
                    Map m = (Map)raw;
                    m.remove(id);
                }
                catch (Throwable throwable) {
                    // empty catch block
                }
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static boolean isInActiveStrike(ServerPlayer player) {
        try {
            Map m;
            Class<?> c = Class.forName("com.dragonminez.server.events.players.combat.StrikeAttackHandler");
            Field f = c.getDeclaredField("ACTIVE");
            f.setAccessible(true);
            Object raw = f.get(null);
            return raw instanceof Map && (m = (Map)raw).containsKey(player.m_20148_());
        }
        catch (Throwable t) {
            return false;
        }
    }

    public static void sync(ServerPlayer player) {
        try {
            NetworkHandler.sendToTrackingEntityAndSelf((Object)new StatsSyncS2C(player), (Entity)player);
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    private static String playerName(Player player) {
        if (player instanceof ServerPlayer) {
            ServerPlayer sp = (ServerPlayer)player;
            return sp.m_36316_().getName();
        }
        return String.valueOf(player.m_20148_());
    }
}

