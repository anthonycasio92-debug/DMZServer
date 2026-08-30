package com.dbzlegacy.adaptivedifficulty.progression.end;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.init.entities.ki.KiBlastEntity;
import com.dragonminez.common.init.entities.ki.KiLaserEntity;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.dimension.end.EndDragonFight;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Core port of {@code End Dimension Strength.js} 2.12.0:
 * scale End dragon from the strongest End player, settle dragon kill TP,
 * dragon spawn/cleanup commands (trigger 50/51), damage mitigation,
 * single-dragon enforcement, and End ki_laser/ki_blast world hygiene.
 * End mob HP/DEF scaling is off by default (v2.11.0).
 */
public final class EndDimensionStrength {
    private static final String TAG_BUFFED = "end_strength_v15";
    private static final String NBT_DEF = "end_strength_entity_def";
    private static final String NBT_MAX = "end_strength_real_max";
    private static final String NBT_HITS = "end_strength_hit_target";
    private static final String NBT_DMZ_HP = "end_strength_dmz_hp_src";

    private static final double SCAN_RADIUS = 96.0;
    private static final long SCAN_MS = 1500L;
    private static final long NATURAL_CHECK_MS = 10_000L;
    private static final long NATURAL_SPAWN_MS = 5L * 60L * 1000L;
    private static final long DRAGON_RESCALE_MS = 3000L;
    private static final double DRAGON_SCALE_SCORE_EPSILON = 0.01;
    private static final int TP_SETTLE_DELAY_TICKS = 6;

    /** Script v2.12.0: min(KI_CLEANUP_INTERVAL_MS, SINGLE_DRAGON_CHECK_MS). */
    private static final long HYGIENE_INTERVAL_MS = 1500L;
    private static final String KI_LASER_ID = "dragonminez:ki_laser";
    private static final String KI_BLAST_ID = "dragonminez:ki_blast";
    private static final AABB END_KI_SCAN_BOX = new AABB(-800.0, 0.0, -800.0, 800.0, 320.0, 800.0);

    /** Live End Dimension Strength.js 2.12.0 — dragon extra DMZ ki attacks. */
    private static final boolean DRAGON_EXTRA_ATTACKS = true;
    private static final long DRAGON_ATTACK_INTERVAL_MS = 3200L;
    private static final double DRAGON_ATTACK_RANGE = 96.0;
    private static final double DRAGON_KI_BEAM_CHANCE = 0.60;
    private static final double DRAGON_DMZ_KI_DAMAGE = 450.0;
    private static final double DRAGON_DMZ_KI_MELEE_FRAC = 0.10;
    private static final double DRAGON_DMZ_KI_DAMAGE_CAP = 8000.0;
    private static final float DRAGON_DMZ_KI_SPEED_BEAM = 1.75f;
    private static final float DRAGON_DMZ_KI_SPEED_BLAST = 2.35f;
    private static final float DRAGON_DMZ_KI_SIZE_BLAST = 1.35f;
    private static final int DRAGON_DMZ_KI_LIFE_BEAM = 28;
    private static final int DRAGON_DMZ_KI_LIFE_BLAST = 36;
    private static final int DRAGON_DMZ_KI_COLOR_MAIN = 0xC44CFF;
    private static final int DRAGON_DMZ_KI_COLOR_BORDER = 0x7A1FA2;
    private static final int DRAGON_DMZ_KI_COLOR_OUTLINE = 0xFFFFFF;

    private static final boolean DESTROY_CRYSTALS_ON_KILL = true;
    private static final boolean REMOVE_EGG_BLOCK = true;
    private static final int EGG_CLEAR_RADIUS = 8;
    private static final int EGG_CLEAR_Y_MIN = 50;
    private static final int EGG_CLEAR_Y_MAX = 120;

    private static final double DRAGON_BASE_HP = 12_000;
    private static final double DRAGON_HP_CAP = 28_000;
    private static final double DRAGON_HP_LOG_REF = 10_000_000;
    private static final double DRAGON_DEF_BASE = 50_000;
    private static final double DRAGON_DEF_FROM_PLAYER = 2.25;
    private static final double DRAGON_DEF_FROM_MELEE = 1.75;
    private static final double DRAGON_DEF_PER_LEVEL = 750;
    private static final double DRAGON_DEF_PER_BP = 0.15;
    private static final double DRAGON_DEF_CAP = 25_000_000;
    private static final int DRAGON_TARGET_HITS = 250;
    private static final double DRAGON_MIN_DMG_FRAC = 0.008;
    private static final double END_FLAT_ABSORB = 0.55;
    private static final double END_REDUCTION_CAP = 0.92;
    private static final double END_DEF_SCALE = 12.0;

    private static final double END_MOB_DEF_FROM_PLAYER = 1.10;
    private static final double END_MOB_DEF_FROM_MELEE = 0.85;
    private static final double END_MOB_DEF_PER_LEVEL = 120;
    private static final double END_MOB_DEF_SCALE_CAP = 8.0;
    private static final double END_MOB_MIN_DMG_FRAC = 0.03;
    private static final double END_MOB_FLAT_ABSORB = 0.35;
    private static final double END_MOB_REDUCTION_CAP = 0.80;
    private static final double END_MOB_DEF_SCALE = 20.0;
    private static final double ENDERMAN_ATTACK = 12.0;

    private static volatile long lastWorldScanAt;
    private static volatile long lastNaturalCheckAt;
    private static volatile long lastNaturalSpawnAt;
    /** 0 = never set; first natural check arms the timer without spawning. */
    private static volatile boolean naturalTimerArmed;
    private static volatile long lastDragonRescaleAt;
    private static volatile long lastHygieneAt;
    private static volatile long lastDragonAttackAt;
    /** Retry crystal/egg podium clear for a few seconds after dragon kill. */
    private static volatile long crystalClearUntil;
    /** Last power score we sized the living dragon to (script TEMP_DRAGON_SCALE_SCORE). */
    private static volatile double lastDragonScaleScore = -1.0;

    private static final Map<UUID, PendingTp> PENDING_TP = new ConcurrentHashMap<>();

    private EndDimensionStrength() {}

    public static boolean isTheEnd(Level level) {
        if (level == null) {
            return false;
        }
        try {
            ResourceLocation dim = level.m_46472_().m_135782_(); // dimension().location()
            if (dim == null) {
                return false;
            }
            String id = dim.toString().toLowerCase(Locale.ROOT);
            if ("minecraft:the_end".equals(id) || id.contains("the_end")) {
                return true;
            }
            return id.endsWith(":end") && !id.contains("endermi");
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void pulse(MinecraftServer server) {
        if (!DifficultyConfig.get().enableEndDimensionStrength || server == null) {
            return;
        }
        long now = System.currentTimeMillis();
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            if (player == null) {
                continue;
            }
            processKillTpSettle(player);
            if (!isTheEnd(player.m_9236_())) {
                continue;
            }
            maybeNaturalDragon(player, now);
        }

        ServerLevel end = server.m_129880_(Level.f_46430_); // END
        if (end != null) {
            runDragonWorldHygiene(end, now);
            tickDragonExtraAttacks(end, now);
        }

        if (now - lastWorldScanAt < SCAN_MS) {
            return;
        }
        lastWorldScanAt = now;

        // World-wide End dragon scan — dragons fly far; do not require player proximity.
        if (end != null) {
            enforceSingleDragon(end);
            PlayerPower strongest = strongestInEnd(end, null);
            for (EnderDragon dragon : findDragons(end)) {
                maybeRescaleDragon(dragon, end, strongest, now);
            }
            boolean mobScaling = DifficultyConfig.get().enableEndMobScaling;
            if (mobScaling) {
                for (ServerPlayer player : server.m_6846_().m_11314_()) {
                    if (player == null || !isTheEnd(player.m_9236_())) {
                        continue;
                    }
                    ServerLevel level = player.m_284548_();
                    if (level == null) {
                        continue;
                    }
                    AABB box = player.m_20191_().m_82400_(SCAN_RADIUS);
                    List<LivingEntity> nearby = level.m_45976_(LivingEntity.class, box);
                    PlayerPower localStrong = strongestInEnd(level, player);
                    for (LivingEntity ent : nearby) {
                        String kind = classify(ent);
                        if (kind == null || "dragon".equals(kind)) {
                            continue;
                        }
                        buffMob(ent, kind, nearbyPower(ent, level, localStrong));
                    }
                    break;
                }
            }
        }
    }

    public static void onHurt(LivingHurtEvent event) {
        if (!DifficultyConfig.get().enableEndDimensionStrength) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target == null || !isTheEnd(target.m_9236_())) {
            return;
        }
        String kind = classify(target);
        if (kind == null) {
            return;
        }
        boolean dragon = "dragon".equals(kind);
        boolean mobScaling = DifficultyConfig.get().enableEndMobScaling;
        if (!dragon && !mobScaling) {
            return;
        }
        // Attack-tick path: keep a single dragon before DEF mitigation.
        if (dragon && target.m_9236_() instanceof ServerLevel endLevel) {
            EnderDragon kept = enforceSingleDragon(endLevel);
            if (kept != null && kept != target) {
                return;
            }
        }
        float raw = event.getAmount();
        if (!(raw > 0.0f)) {
            return;
        }
        double def = readDef(target);
        if (!(def > 0.0) && event.getSource() != null
                && event.getSource().m_7639_() instanceof ServerPlayer attacker) {
            ServerLevel level = attacker.m_284548_();
            PlayerPower power = strongestInEnd(level, attacker);
            if (dragon && target instanceof EnderDragon enderDragon) {
                applyDragonStats(enderDragon, power, "onhit");
            } else if (mobScaling) {
                buffMob(target, kind, power);
            }
            def = readDef(target);
        }
        if (!(def > 0.0)) {
            return;
        }
        double minFrac = dragon ? DRAGON_MIN_DMG_FRAC : END_MOB_MIN_DMG_FRAC;
        double absorb = dragon ? END_FLAT_ABSORB : END_MOB_FLAT_ABSORB;
        double redCap = dragon ? END_REDUCTION_CAP : END_MOB_REDUCTION_CAP;
        double defScale = dragon ? END_DEF_SCALE : END_MOB_DEF_SCALE;
        float mitigated = (float) mitigate(raw, def, minFrac, absorb, redCap, defScale);
        // Hit-cap AFTER mitigation — do not re-floor with raw*minFrac (that undoes the cap).
        mitigated = capForHits(target, kind, mitigated);
        event.setAmount(mitigated);
    }

    public static void onKill(LivingDeathEvent event, ServerPlayer killer) {
        if (!DifficultyConfig.get().enableEndDimensionStrength || killer == null) {
            return;
        }
        LivingEntity dead = event.getEntity();
        if (dead == null) {
            return;
        }
        String kind = classify(dead);
        if (kind == null) {
            return;
        }
        boolean isDragon = "dragon".equals(kind);
        // Dragon always; other End mobs only when enableEndMobScaling (v2.11.0).
        if (isDragon || DifficultyConfig.get().enableEndMobScaling) {
            scheduleKillTp(killer, kind, dead.m_21233_());
        }
        if (!isDragon) {
            return;
        }
        lastNaturalSpawnAt = System.currentTimeMillis();
        crystalClearUntil = lastNaturalSpawnAt + 12_000L;
        MinecraftServer server = killer.m_20194_();
        if (server != null) {
            ServerLevel end = server.m_129880_(Level.f_46430_);
            if (end != null) {
                try {
                    purgeEndKiCommands(end);
                } catch (Throwable ignored) {
                }
                try {
                    cleanupEndKiProjectiles(end, false);
                } catch (Throwable ignored) {
                }
                try {
                    clearEndCrystals(end);
                } catch (Throwable ignored) {
                }
                try {
                    clearDragonEggBlocks(end);
                } catch (Throwable ignored) {
                }
            }
        }
        try {
            var item = ForgeRegistries.ITEMS.getValue(new ResourceLocation("minecraft", "dragon_egg"));
            ItemStack egg = item == null ? ItemStack.f_41583_ : new ItemStack(item);
            if (!egg.m_41619_()) {
                if (!killer.m_150109_().m_36054_(egg)) {
                    killer.m_36176_(egg, false);
                }
                killer.m_213846_(Component.m_237113_("§d[The End] §fDragon Egg claimed."));
            }
        } catch (Throwable ignored) {
        }
        SystemTelemetry.log("end_strength", "dragon_kill", killer, null, Map.of("kind", kind));
    }

    /** Trigger 50 — spawn / refresh End dragon (EndDragonFight-linked like the script). */
    public static int cmdSpawnDragon(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return 0;
        }
        ServerLevel end = server.m_129880_(Level.f_46430_); // END
        if (end == null) {
            msg(player, "§c[The End] End dimension unavailable.");
            return 0;
        }
        EnderDragon existing = enforceSingleDragon(end);
        if (existing != null) {
            PlayerPower power = strongestInEnd(end, player);
            applyDragonStats(existing, power, "cmd");
            lastDragonScaleScore = score(power);
            msg(player, "§e[The End] An Ender Dragon is already alive.");
            msg(player, "§8Scaled to §f" + power.name + " §8· HP §c"
                    + DmzRewards.formatWhole(existing.m_21233_())
                    + " §8· DEF §b" + DmzRewards.formatWhole(readDef(existing)));
            return 1;
        }
        msg(player, "§7[The End] Spawning Ender Dragon...");
        PlayerPower power = strongestInEnd(end, player);
        EnderDragon dragon = spawnFightLinkedDragon(end, player);
        if (dragon == null) {
            msg(player, "§c[The End] Failed to spawn — visit The End once, then retry /enddragon.");
            return 0;
        }
        applyDragonStats(dragon, power, "spawn");
        lastDragonScaleScore = score(power);
        lastNaturalSpawnAt = System.currentTimeMillis();
        naturalTimerArmed = true;
        msg(player, "§6[The End] §eSpawned Ender Dragon with §c"
                + DmzRewards.formatWhole(dragon.m_21233_())
                + " §eHP / §b" + DmzRewards.formatWhole(readDef(dragon))
                + " §eDEF §8(scaled to " + power.name + " / Lv" + power.level + ")");
        SystemTelemetry.log("end_strength", "dragon_spawn", player, null, Map.of("via", "command"));
        return 1;
    }

    /**
     * Spawn through {@link EndDragonFight} so perch / charge / crystal AI stays linked.
     * Raw {@code EntityType} summon orphans the dragon (script warning).
     */
    private static EnderDragon spawnFightLinkedDragon(ServerLevel end, ServerPlayer requester) {
        if (end == null) {
            return null;
        }
        // Clear any leftovers first.
        for (EnderDragon d : findDragons(end)) {
            try {
                d.m_146870_();
            } catch (Throwable ignored) {
            }
        }
        EndDragonFight fight = end.m_8586_(); // dragonFight
        if (fight == null) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] EndDragonFight is null — visit The End once so the dimension initializes",
                    AdaptiveDifficultyMod.MOD_ID);
            return spawnOrphanFallback(end, requester);
        }
        try {
            fight.m_287277_(); // skipArenaLoadedCheck
        } catch (Throwable ignored) {
        }
        // dragonKilled=false, previouslyKilled=true (script field names f_64068_/f_64069_).
        setFightBoolean(fight, "f_64068_", false);
        setFightBoolean(fight, "f_64069_", true);
        try {
            clearEndCrystals(end);
        } catch (Throwable ignored) {
        }
        try {
            fight.m_64101_(); // resetSpikeCrystals
        } catch (Throwable ignored) {
        }
        try {
            restoreTowerCrystals(end);
        } catch (Throwable ignored) {
        }
        EnderDragon dragon = invokeCreateNewDragon(fight);
        if (dragon == null) {
            dragon = spawnOrphanFallback(end, requester);
            if (dragon != null) {
                try {
                    dragon.m_287231_(fight); // setDragonFight
                } catch (Throwable ignored) {
                }
            }
        }
        return dragon;
    }

    private static EnderDragon spawnOrphanFallback(ServerLevel end, ServerPlayer requester) {
        try {
            EnderDragon dragon = net.minecraft.world.entity.EntityType.f_20565_.m_20615_(end);
            if (dragon == null) {
                return null;
            }
            double x = 0.5;
            double y = 128.0;
            double z = 0.5;
            if (requester != null && isTheEnd(requester.m_9236_())) {
                x = requester.m_20185_();
                y = requester.m_20186_() + 12.0;
                z = requester.m_20189_();
            }
            dragon.m_7678_(x, y, z, 0.0f, 0.0f);
            end.m_7967_(dragon);
            return dragon;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] orphan dragon spawn failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return null;
        }
    }

    private static EnderDragon invokeCreateNewDragon(EndDragonFight fight) {
        if (fight == null) {
            return null;
        }
        try {
            java.lang.reflect.Method m = EndDragonFight.class.getDeclaredMethod("m_64110_");
            m.setAccessible(true);
            Object raw = m.invoke(fight);
            return raw instanceof EnderDragon d ? d : null;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] createNewDragon reflect failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return null;
        }
    }

    private static void setFightBoolean(EndDragonFight fight, String field, boolean value) {
        if (fight == null || field == null) {
            return;
        }
        try {
            java.lang.reflect.Field f = EndDragonFight.class.getDeclaredField(field);
            f.setAccessible(true);
            f.setBoolean(fight, value);
        } catch (Throwable ignored) {
        }
    }

    /** Re-place tower end crystals so the dragon keeps heal/perch AI (script restoreTowerCrystals). */
    private static int restoreTowerCrystals(ServerLevel end) {
        if (end == null) {
            return 0;
        }
        int placed = 0;
        try {
            List<net.minecraft.world.level.levelgen.feature.SpikeFeature.EndSpike> spikes =
                    net.minecraft.world.level.levelgen.feature.SpikeFeature.m_66858_(end);
            if (spikes == null || spikes.isEmpty()) {
                return 0;
            }
            for (net.minecraft.world.level.levelgen.feature.SpikeFeature.EndSpike spike : spikes) {
                int cx = spike.m_66886_(); // getCenterX
                int cz = spike.m_66893_(); // getCenterZ
                int height = spike.m_66899_(); // getHeight
                int y = Math.max(70, height + 1);
                try {
                    EndCrystal crystal = EntityType.f_20564_.m_20615_(end); // END_CRYSTAL
                    if (crystal == null) {
                        continue;
                    }
                    crystal.m_7678_(cx + 0.5, y, cz + 0.5, 0.0f, 0.0f);
                    crystal.m_31056_(true); // setShowBottom
                    end.m_7967_(crystal);
                    placed++;
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] restoreTowerCrystals failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
        return placed;
    }

    /** Trigger 51 — clear End dragons. */
    public static int cmdCleanupDragons(ServerPlayer player) {
        MinecraftServer server = player == null ? null : player.m_20194_();
        if (server == null) {
            return 0;
        }
        ServerLevel end = server.m_129880_(Level.f_46430_);
        if (end == null) {
            return 0;
        }
        int removed = 0;
        java.util.ArrayList<Entity> dragons = new java.util.ArrayList<>();
        for (Entity e : end.m_8583_()) {
            if (e instanceof EnderDragon) {
                dragons.add(e);
            }
        }
        for (Entity e : dragons) {
            e.m_146870_();
            removed++;
        }
        if (player != null) {
            msg(player, "§7[The End] Cleared §f" + removed + "§7 dragon(s).");
        }
        SystemTelemetry.log("end_strength", "dragon_cleanup", player, null, Map.of("removed", removed));
        return removed;
    }

    private static void maybeNaturalDragon(ServerPlayer player, long now) {
        if (!DifficultyConfig.get().enableEndNaturalDragonSpawn) {
            return;
        }
        if (now - lastNaturalCheckAt < NATURAL_CHECK_MS) {
            return;
        }
        lastNaturalCheckAt = now;
        ServerLevel end = player.m_284548_();
        if (end == null || !isTheEnd(end)) {
            return;
        }
        EnderDragon kept = enforceSingleDragon(end);
        if (kept != null) {
            maybeRescaleDragon(kept, end, strongestInEnd(end, player), now);
            return;
        }
        // Script: first boot arms the timer and waits a full interval before spawning.
        if (!naturalTimerArmed || lastNaturalSpawnAt <= 0L) {
            lastNaturalSpawnAt = now;
            naturalTimerArmed = true;
            return;
        }
        if (now - lastNaturalSpawnAt < NATURAL_SPAWN_MS) {
            return;
        }
        int spawned = cmdSpawnDragon(player);
        if (spawned > 0) {
            lastNaturalSpawnAt = now;
            DmzRewards.msg(player, "§5[The End] §cAn Ender Dragon has appeared!");
        }
    }

    /**
     * Keep at most one living Ender Dragon. Prefer the healthiest.
     * Returns the kept dragon, or null if none.
     */
    static EnderDragon enforceSingleDragon(ServerLevel end) {
        if (end == null) {
            return null;
        }
        List<EnderDragon> dragons = findDragons(end);
        if (dragons.isEmpty()) {
            return null;
        }
        if (!DifficultyConfig.get().endEnforceSingleDragon) {
            return dragons.get(0);
        }
        if (dragons.size() == 1) {
            return dragons.get(0);
        }
        EnderDragon keep = dragons.get(0);
        double keepHp = dragonHealthScore(keep);
        for (int i = 1; i < dragons.size(); i++) {
            EnderDragon d = dragons.get(i);
            if (d == null) {
                continue;
            }
            double hp = dragonHealthScore(d);
            if (hp > keepHp) {
                keep = d;
                keepHp = hp;
            }
        }
        UUID keepId = keep.m_20148_();
        int removed = 0;
        for (EnderDragon extra : dragons) {
            if (extra == null || extra == keep) {
                continue;
            }
            if (keepId != null && keepId.equals(extra.m_20148_())) {
                continue;
            }
            try {
                extra.m_146870_(); // discard
                removed++;
            } catch (Throwable ignored) {
            }
        }
        if (removed > 0) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Enforced single dragon: removed {} duplicate(s), kept {}",
                    AdaptiveDifficultyMod.MOD_ID, removed, keepId);
        }
        return keep;
    }

    /**
     * Purge / cap End ki_laser + ki_blast.
     * No dragon: kill all. Dragon alive: hard-cap count.
     */
    static void cleanupEndKiProjectiles(ServerLevel end, boolean hasDragon) {
        if (end == null || !DifficultyConfig.get().endKiCleanupEnabled) {
            return;
        }
        if (!hasDragon && DifficultyConfig.get().endKiPurgeWhenNoDragon) {
            purgeEndKiCommands(end);
            return;
        }
        if (!hasDragon) {
            return;
        }
        List<Entity> ents = collectEndKiEntities(end);
        int n = ents.size();
        int cap = Math.max(1, DifficultyConfig.get().endKiMaxAliveWhileDragon);
        if (n <= cap) {
            return;
        }
        // Flooded End: discard collected entities in one pass (no console kill spam).
        if (n > cap * 2) {
            purgeEndKiCommands(end);
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ki flood ({} > {}); purged all End ki projectiles",
                    AdaptiveDifficultyMod.MOD_ID, n, cap * 2);
            return;
        }
        int over = n - cap;
        int killed = 0;
        for (int j = 0; j < ents.size() && killed < over; j++) {
            if (discardEntitySafe(ents.get(j))) {
                killed++;
            }
        }
        if (killed < over) {
            purgeEndKiCommands(end);
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ki cap exceeded ({}); purged all End ki projectiles",
                    AdaptiveDifficultyMod.MOD_ID, n);
        } else if (killed > 0) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Ki cap: discarded {} (had {})",
                    AdaptiveDifficultyMod.MOD_ID, killed, n);
        }
    }

    /** Throttled single-dragon enforce + ki cleanup (shared across End). */
    static void runDragonWorldHygiene(ServerLevel end, long now) {
        if (end == null) {
            return;
        }
        try {
            if (crystalClearUntil > 0L && now <= crystalClearUntil) {
                clearEndCrystals(end);
                clearDragonEggBlocks(end);
            } else if (crystalClearUntil > 0L && now > crystalClearUntil) {
                crystalClearUntil = 0L;
            }
            if (now - lastHygieneAt < HYGIENE_INTERVAL_MS) {
                return;
            }
            lastHygieneAt = now;
            EnderDragon kept = enforceSingleDragon(end);
            cleanupEndKiProjectiles(end, kept != null);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] runDragonWorldHygiene: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    /** Live 2.12.0 — periodic DMZ ki beam/blast from the single kept dragon. */
    static void tickDragonExtraAttacks(ServerLevel end, long now) {
        if (!DRAGON_EXTRA_ATTACKS || end == null || !DifficultyConfig.get().enableEndDimensionStrength) {
            return;
        }
        if (now - lastDragonAttackAt < DRAGON_ATTACK_INTERVAL_MS) {
            return;
        }
        lastDragonAttackAt = now;
        try {
            EnderDragon dragon = enforceSingleDragon(end);
            if (dragon == null || !dragon.m_6084_()) {
                return;
            }
            ServerPlayer target = nearestEndPlayer(end, dragon, DRAGON_ATTACK_RANGE);
            if (target == null) {
                return;
            }
            aimLivingAt(dragon, target);
            double roll = Math.random();
            if (roll < DRAGON_KI_BEAM_CHANCE) {
                if (!fireDragonKiBeam(end, dragon, target)) {
                    fireDragonKiBlast(end, dragon, target);
                }
            } else {
                fireDragonKiBlast(end, dragon, target);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] dragon extra attack: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    private static ServerPlayer nearestEndPlayer(ServerLevel end, Entity from, double range) {
        if (end == null || from == null) {
            return null;
        }
        ServerPlayer best = null;
        double bestD = range * range;
        try {
            for (ServerPlayer p : end.m_6907_()) {
                if (p == null || !p.m_6084_()) {
                    continue;
                }
                double d = p.m_20275_(from.m_20185_(), from.m_20186_(), from.m_20189_());
                if (d < bestD) {
                    bestD = d;
                    best = p;
                }
            }
        } catch (Throwable ignored) {
        }
        return best;
    }

    private static double calcDragonKiDamage(ServerPlayer target) {
        double base = DRAGON_DMZ_KI_DAMAGE;
        double melee = 0.0;
        try {
            melee = Math.max(0.0, readPower(target).melee);
        } catch (Throwable ignored) {
        }
        double scaled = Math.max(base, melee * DRAGON_DMZ_KI_MELEE_FRAC);
        return Math.min(DRAGON_DMZ_KI_DAMAGE_CAP, scaled);
    }

    private static void aimLivingAt(LivingEntity living, LivingEntity target) {
        if (living == null || target == null) {
            return;
        }
        try {
            Vec3 from = living.m_146892_();
            double tx = target.m_20185_();
            double ty = target.m_20186_() + target.m_20206_() * 0.45;
            double tz = target.m_20189_();
            double dx = tx - from.f_82479_;
            double dy = ty - from.f_82480_;
            double dz = tz - from.f_82481_;
            double horiz = Math.sqrt(dx * dx + dz * dz);
            float yaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
            float pitch = (float) (-Math.toDegrees(Math.atan2(dy, Math.max(1.0E-4, horiz))));
            if (pitch > 89f) {
                pitch = 89f;
            }
            if (pitch < -89f) {
                pitch = -89f;
            }
            living.m_146922_(yaw);
            living.m_146926_(pitch);
            living.f_20885_ = yaw; // yHeadRot
            living.f_20883_ = yaw; // yBodyRot
        } catch (Throwable ignored) {
        }
    }

    private static boolean fireDragonKiBeam(ServerLevel end, EnderDragon dragon, ServerPlayer target) {
        try {
            float dmg = (float) calcDragonKiDamage(target);
            KiLaserEntity beam = new KiLaserEntity(end, dragon);
            try {
                beam.setupKiBeamPlayer(dragon, dmg, DRAGON_DMZ_KI_SPEED_BEAM,
                        DRAGON_DMZ_KI_COLOR_MAIN, DRAGON_DMZ_KI_COLOR_BORDER, DRAGON_DMZ_KI_COLOR_OUTLINE);
            } catch (Throwable t1) {
                try {
                    beam.setupKiBeamPlayer(dragon, dmg, DRAGON_DMZ_KI_SPEED_BEAM,
                            DRAGON_DMZ_KI_COLOR_MAIN, DRAGON_DMZ_KI_COLOR_BORDER);
                } catch (Throwable t2) {
                    beam.setupKiLaser(dragon, dmg, DRAGON_DMZ_KI_SPEED_BEAM,
                            DRAGON_DMZ_KI_COLOR_MAIN, DRAGON_DMZ_KI_COLOR_BORDER,
                            DRAGON_DMZ_KI_COLOR_OUTLINE, 0);
                }
            }
            try {
                beam.setHomingTarget(target.m_19879_());
            } catch (Throwable ignored) {
            }
            return spawnAndFireKi(beam, end, DRAGON_DMZ_KI_LIFE_BEAM);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] dragon ki beam failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    private static boolean fireDragonKiBlast(ServerLevel end, EnderDragon dragon, ServerPlayer target) {
        try {
            float dmg = (float) calcDragonKiDamage(target);
            KiBlastEntity blast = new KiBlastEntity(end, dragon);
            try {
                blast.setupKiBlastPlayer(dragon, dmg, DRAGON_DMZ_KI_SPEED_BLAST,
                        DRAGON_DMZ_KI_COLOR_MAIN, DRAGON_DMZ_KI_COLOR_BORDER,
                        DRAGON_DMZ_KI_COLOR_OUTLINE, DRAGON_DMZ_KI_SIZE_BLAST);
            } catch (Throwable t1) {
                try {
                    blast.setupKiBlastPlayer(dragon, dmg, DRAGON_DMZ_KI_SPEED_BLAST,
                            DRAGON_DMZ_KI_COLOR_MAIN, DRAGON_DMZ_KI_COLOR_BORDER, DRAGON_DMZ_KI_SIZE_BLAST);
                } catch (Throwable t2) {
                    blast.setupKiLargeBlast(dragon, dmg, DRAGON_DMZ_KI_SPEED_BLAST,
                            DRAGON_DMZ_KI_COLOR_MAIN, DRAGON_DMZ_KI_COLOR_BORDER,
                            DRAGON_DMZ_KI_COLOR_OUTLINE, DRAGON_DMZ_KI_SIZE_BLAST, 0);
                }
            }
            try {
                blast.setHomingTarget(target.m_19879_());
            } catch (Throwable ignored) {
            }
            return spawnAndFireKi(blast, end, DRAGON_DMZ_KI_LIFE_BLAST);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] dragon ki blast failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    private static boolean spawnAndFireKi(Entity proj, ServerLevel end, int lifeTicks) {
        if (proj == null || end == null) {
            return false;
        }
        try {
            proj.getClass().getMethod("setCastTime", int.class).invoke(proj, 0);
        } catch (Throwable ignored) {
        }
        try {
            proj.getClass().getMethod("setBlockDestructionEnabled", boolean.class).invoke(proj, false);
        } catch (Throwable ignored) {
        }
        try {
            if (!end.m_7967_(proj)) {
                return false;
            }
        } catch (Throwable t) {
            return false;
        }
        int life = Math.max(10, lifeTicks);
        try {
            proj.getClass().getMethod("fireHability", int.class).invoke(proj, life);
        } catch (Throwable t1) {
            try {
                proj.getClass().getMethod("setFiring", boolean.class).invoke(proj, true);
                proj.getClass().getMethod("setMaxLife", int.class).invoke(proj, life);
            } catch (Throwable ignored) {
            }
        }
        return true;
    }

    /** Live DESTROY_CRYSTALS_ON_KILL — despawn all End crystals in The End. */
    static int clearEndCrystals(ServerLevel end) {
        if (!DESTROY_CRYSTALS_ON_KILL || end == null) {
            return 0;
        }
        int removed = 0;
        List<Entity> toRemove = new ArrayList<>();
        try {
            for (Entity e : end.m_8583_()) {
                if (e instanceof EndCrystal) {
                    toRemove.add(e);
                }
            }
        } catch (Throwable ignored) {
        }
        for (Entity e : toRemove) {
            try {
                e.m_146870_();
                removed++;
            } catch (Throwable ignored) {
            }
        }
        if (removed <= 0) {
            // Already empty — do not run console kill @e (spams "No entity was found").
            return 0;
        }
        return removed;
    }

    /** Live REMOVE_EGG_BLOCK — clear podium dragon_egg blocks near 0,0 (never end_portal). */
    static int clearDragonEggBlocks(ServerLevel end) {
        if (!REMOVE_EGG_BLOCK || end == null) {
            return 0;
        }
        int removed = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int x = -EGG_CLEAR_RADIUS; x <= EGG_CLEAR_RADIUS; x++) {
            for (int z = -EGG_CLEAR_RADIUS; z <= EGG_CLEAR_RADIUS; z++) {
                for (int y = EGG_CLEAR_Y_MIN; y <= EGG_CLEAR_Y_MAX; y++) {
                    pos.m_122178_(x, y, z); // set
                    try {
                        var state = end.m_8055_(pos);
                        if (!isDragonEggBlock(state)) {
                            continue;
                        }
                        end.m_46597_(pos, Blocks.f_50016_.m_49966_()); // AIR
                        removed++;
                    } catch (Throwable ignored) {
                    }
                }
            }
        }
        return removed;
    }

    /** Dragon egg only — never end_portal / end_gateway (older builds wrongly cleared portals). */
    private static boolean isDragonEggBlock(net.minecraft.world.level.block.state.BlockState state) {
        if (state == null) {
            return false;
        }
        try {
            var key = ForgeRegistries.BLOCKS.getKey(state.m_60734_());
            if (key == null) {
                return false;
            }
            String name = key.toString().toLowerCase(Locale.ROOT);
            // Exact id only — never "end_portal" (previous bug used END_PORTAL constant).
            return "minecraft:dragon_egg".equals(name) || name.endsWith(":dragon_egg");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static List<EnderDragon> findDragons(ServerLevel end) {
        List<EnderDragon> out = new ArrayList<>();
        if (end == null) {
            return out;
        }
        try {
            for (Entity e : end.m_8583_()) {
                if (e instanceof EnderDragon dragon && dragon.m_6084_()) {
                    out.add(dragon);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static double dragonHealthScore(EnderDragon dragon) {
        if (dragon == null) {
            return -1.0;
        }
        try {
            return Math.max(0.0, dragon.m_21223_());
        } catch (Throwable ignored) {
            return -1.0;
        }
    }

    /**
     * Remove all End ki_laser / ki_blast via entity discard (silent).
     * Avoids console {@code kill @e[…]} which prints "No entity was found" every hygiene pulse
     * when The End is empty.
     */
    private static void purgeEndKiCommands(ServerLevel end) {
        if (end == null) {
            return;
        }
        List<Entity> ents = collectEndKiEntities(end);
        if (ents.isEmpty()) {
            return;
        }
        int killed = 0;
        for (Entity e : ents) {
            if (discardEntitySafe(e)) {
                killed++;
            }
        }
        if (killed > 0) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Purged {} End ki projectiles", AdaptiveDifficultyMod.MOD_ID, killed);
        }
    }

    private static EntityType<?> resolveKiType(String path) {
        try {
            return ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("dragonminez", path));
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static List<Entity> collectEndKiEntities(ServerLevel end) {
        List<Entity> out = new ArrayList<>();
        if (end == null) {
            return out;
        }
        EntityType<?> laser = resolveKiType("ki_laser");
        EntityType<?> blast = resolveKiType("ki_blast");
        try {
            if (laser != null) {
                collectTyped(end, laser, out);
            }
            if (blast != null) {
                collectTyped(end, blast, out);
            }
            if (!out.isEmpty() || (laser != null && blast != null)) {
                return out;
            }
        } catch (Throwable ignored) {
        }
        // Fallback: scan all End entities by registry id / name.
        try {
            for (Entity e : end.m_8583_()) {
                if (isKiProjectileEntity(e)) {
                    out.add(e);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void collectTyped(ServerLevel end, EntityType<?> type, List<Entity> out) {
        if (end == null || type == null || out == null) {
            return;
        }
        try {
            List list = end.m_142425_((EntityTypeTest) type, END_KI_SCAN_BOX, e -> e != null);
            if (list != null) {
                for (Object o : list) {
                    if (o instanceof Entity entity) {
                        out.add(entity);
                    }
                }
                return;
            }
        } catch (Throwable ignored) {
        }
        try {
            for (Entity e : end.m_8583_()) {
                if (e != null && e.m_6095_() == type) {
                    out.add(e);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isKiProjectileEntity(Entity ent) {
        if (ent == null) {
            return false;
        }
        try {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(ent.m_6095_());
            if (id != null) {
                String key = id.toString().toLowerCase(Locale.ROOT);
                if (KI_LASER_ID.equals(key) || KI_BLAST_ID.equals(key)
                        || key.contains("ki_laser") || key.contains("ki_blast")
                        || key.contains("kilaser") || key.contains("kiblast")) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            String name = ent.m_6095_().toString().toLowerCase(Locale.ROOT);
            return name.contains("ki_laser") || name.contains("ki_blast")
                    || name.contains("kilaser") || name.contains("kiblast");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean discardEntitySafe(Entity ent) {
        if (ent == null) {
            return false;
        }
        try {
            ent.m_146870_(); // discard
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void maybeRescaleDragon(EnderDragon dragon, ServerLevel level, PlayerPower power, long now) {
        if (dragon == null || power == null) {
            return;
        }
        if (now - lastDragonRescaleAt < DRAGON_RESCALE_MS) {
            return;
        }
        double desired = score(power);
        // Script: only grow when a stronger End player arrives (epsilon gate).
        boolean stronger = desired > lastDragonScaleScore * (1.0 + DRAGON_SCALE_SCORE_EPSILON) + 1.0
                || lastDragonScaleScore < 0.0
                || !alreadyBuffed(dragon);
        if (!stronger) {
            return;
        }
        lastDragonRescaleAt = now;
        applyDragonStats(dragon, power, "rescale");
        lastDragonScaleScore = desired;
    }

    private static void applyDragonStats(EnderDragon dragon, PlayerPower power, String source) {
        if (dragon == null || power == null) {
            return;
        }
        double hp = mapDmzHp(power.maxHp, DRAGON_BASE_HP, DRAGON_HP_CAP);
        double def = calcDragonDef(power);
        boolean midFight = alreadyBuffed(dragon) && ("rescale".equals(source) || "onhit".equals(source) || "strongest".equals(source));
        double prevMax = dragon.m_21233_();
        double prevHp = dragon.m_21223_();
        if (midFight && prevMax > 20 && prevHp > 0 && hp <= prevMax + 500) {
            // DEF / name only — leave attributes alone mid-fight.
            storeDef(dragon, def);
            storeHits(dragon, DRAGON_TARGET_HITS);
            markBuffed(dragon);
            return;
        }
        setMaxHealth(dragon, hp, midFight ? Math.max(1.0, hp * (prevHp / Math.max(1.0, prevMax))) : hp);
        storeDef(dragon, def);
        storeHits(dragon, DRAGON_TARGET_HITS);
        storeDmzHp(dragon, power.maxHp);
        markBuffed(dragon);
        try {
            dragon.m_6593_(Component.m_237113_(
                    "§5Ender Dragon §8[" + DmzRewards.formatWhole(hp) + " HP / DEF "
                            + DmzRewards.formatWhole(def) + "]"));
        } catch (Throwable ignored) {
        }
    }

    private static void buffMob(LivingEntity entity, String kind, PlayerPower power) {
        MobTier tier = MobTier.of(kind);
        if (tier == null || power == null) {
            return;
        }
        if (alreadyBuffed(entity) && readDef(entity) > 0.0) {
            return;
        }
        double hp = mapDmzHp(power.maxHp, tier.hp, tier.hpCap);
        double def = calcMobDef(tier, power);
        double dmg = "enderman".equals(kind)
                ? ENDERMAN_ATTACK
                : Math.floor(tier.damage * Math.min(4.0, 1.0 + power.level / 100.0));
        setMaxHealth(entity, hp, hp);
        storeDef(entity, def);
        storeHits(entity, tier.hits);
        storeDmzHp(entity, power.maxHp);
        setAttack(entity, dmg);
        markBuffed(entity);
        try {
            entity.m_6593_(Component.m_237113_(
                    "§d" + tier.label + " §8[" + DmzRewards.formatWhole(hp) + " HP / DEF "
                            + DmzRewards.formatWhole(def) + "]"));
        } catch (Throwable ignored) {
        }
    }

    private static double calcDragonDef(PlayerPower p) {
        double def = Math.max(
                DRAGON_DEF_BASE,
                Math.max(p.defense * DRAGON_DEF_FROM_PLAYER, p.melee * DRAGON_DEF_FROM_MELEE)
        );
        def += p.level * DRAGON_DEF_PER_LEVEL;
        def += p.bp * DRAGON_DEF_PER_BP;
        return Math.min(DRAGON_DEF_CAP, Math.floor(def));
    }

    private static double calcMobDef(MobTier tier, PlayerPower p) {
        double base = tier.defense;
        double tierFactor = Math.max(1, tier.tier);
        double def = Math.max(
                base,
                Math.max(
                        p.defense * END_MOB_DEF_FROM_PLAYER * (0.55 + tierFactor * 0.2),
                        p.melee * END_MOB_DEF_FROM_MELEE * (0.45 + tierFactor * 0.15)
                )
        );
        def += p.level * END_MOB_DEF_PER_LEVEL * tierFactor;
        double maxDef = base * END_MOB_DEF_SCALE_CAP
                + p.defense * END_MOB_DEF_FROM_PLAYER * tierFactor
                + p.melee * END_MOB_DEF_FROM_MELEE * tierFactor;
        if (def > maxDef) {
            def = maxDef;
        }
        return Math.floor(def);
    }

    private static double mapDmzHp(double playerDmzHp, double baseHp, double capHp) {
        baseHp = Math.max(1.0, baseHp);
        capHp = Math.max(baseHp, capHp);
        double src = Math.max(20.0, playerDmzHp);
        double t = Math.log(src / 20.0) / Math.log(DRAGON_HP_LOG_REF / 20.0);
        if (!(t >= 0)) {
            t = 0;
        }
        if (t > 1) {
            t = 1;
        }
        return Math.floor(baseHp + (capHp - baseHp) * t);
    }

    private static double mitigate(
            double raw, double defense, double minFrac, double absorb, double redCap, double defScale
    ) {
        double afterAbsorb = raw * (1.0 - DmzRewards.clamp(absorb, 0.0, 0.95));
        double reduction = defense / (defense + Math.max(1.0, raw * defScale));
        reduction = Math.min(redCap, Math.max(0.0, reduction));
        double out = afterAbsorb * (1.0 - reduction);
        double floor = raw * minFrac;
        return Math.max(out, floor);
    }

    private static float capForHits(LivingEntity entity, String kind, float dmg) {
        int hits = (int) PersistentDataAccess.getLong(entity, NBT_HITS,
                "dragon".equals(kind) ? DRAGON_TARGET_HITS : 14);
        hits = Math.max(4, hits);
        double max = entity.m_21233_();
        double cap = max / hits;
        if (dmg > cap) {
            return (float) cap;
        }
        return dmg;
    }

    private static void scheduleKillTp(ServerPlayer player, String kind, double maxHp) {
        PENDING_TP.put(player.m_20148_(), new PendingTp(kind, maxHp, TP_SETTLE_DELAY_TICKS));
    }

    private static void processKillTpSettle(ServerPlayer player) {
        PendingTp pending = PENDING_TP.get(player.m_20148_());
        if (pending == null) {
            return;
        }
        if (pending.delayTicks > 0) {
            pending.delayTicks--;
            return;
        }
        PENDING_TP.remove(player.m_20148_());
        double awarded = estimateDmzKillTp(player, pending.maxHp);
        double bonus = endKillBonus(pending.kind) * endBpMult(readPower(player).bp);
        double target = Math.min(softCap(readPower(player).level, pending.kind), awarded + bonus);
        double delta = target - awarded;
        if (Math.abs(delta) < 50.0) {
            return;
        }
        if (delta > 0) {
            DmzRewards.awardTp(player, (float) Math.floor(delta), "end-" + pending.kind, true, "§6[End] ");
        } else {
            adjustTp(player, delta);
            DmzRewards.msg(player, "§7[End] Kill TP capped (" + DmzRewards.formatWhole(target)
                    + " TP max for " + pending.kind + ").");
        }
    }

    private static double estimateDmzKillTp(ServerPlayer player, double maxHp) {
        double ratio = 0.25;
        double tpPerHit = 2.0;
        double base = tpPerHit + Math.round(Math.max(0.0, maxHp) * ratio);
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return base;
        }
        try {
            return Math.max(0.0, Math.floor(data.calculateTPGain((int) Math.round(base))));
        } catch (Throwable ignored) {
            return base;
        }
    }

    private static double endKillBonus(String kind) {
        return switch (kind == null ? "" : kind) {
            case "dragon" -> 18_000;
            case "shulker" -> 6_000;
            case "enderman" -> 4_500;
            case "phantom" -> 3_000;
            case "endermite" -> 2_000;
            default -> 2_500;
        };
    }

    private static double endBpMult(double bp) {
        double raw = 1.0 + Math.log1p(Math.max(0.0, bp) / 100_000.0);
        double mult = Math.max(1.0, 1.0 + (raw - 1.0) * 0.06);
        return Math.min(2.5, mult);
    }

    private static double softCap(int level, String kind) {
        SoftCapRow[] table = softCapTable(kind);
        return Math.max(1000.0, interpolateSoftCap(table, level, 1_000_000.0));
    }

    private static SoftCapRow[] softCapTable(String kind) {
        return switch (kind == null ? "" : kind) {
            case "dragon" -> new SoftCapRow[]{
                    new SoftCapRow(1000, 280_000),
                    new SoftCapRow(2500, 450_000),
                    new SoftCapRow(4000, 650_000),
                    new SoftCapRow(7000, 950_000),
                    new SoftCapRow(10_000, 1_400_000),
                    new SoftCapRow(20_000, 2_200_000)
            };
            case "shulker" -> new SoftCapRow[]{
                    new SoftCapRow(1000, 90_000),
                    new SoftCapRow(2500, 160_000),
                    new SoftCapRow(4000, 260_000),
                    new SoftCapRow(7000, 400_000),
                    new SoftCapRow(10_000, 560_000),
                    new SoftCapRow(20_000, 850_000)
            };
            case "phantom" -> new SoftCapRow[]{
                    new SoftCapRow(1000, 50_000),
                    new SoftCapRow(2500, 95_000),
                    new SoftCapRow(4000, 150_000),
                    new SoftCapRow(7000, 240_000),
                    new SoftCapRow(10_000, 340_000),
                    new SoftCapRow(20_000, 520_000)
            };
            case "endermite" -> new SoftCapRow[]{
                    new SoftCapRow(1000, 35_000),
                    new SoftCapRow(2500, 65_000),
                    new SoftCapRow(4000, 100_000),
                    new SoftCapRow(7000, 160_000),
                    new SoftCapRow(10_000, 230_000),
                    new SoftCapRow(20_000, 360_000)
            };
            default -> new SoftCapRow[]{ // enderman
                    new SoftCapRow(1000, 70_000),
                    new SoftCapRow(2500, 130_000),
                    new SoftCapRow(4000, 200_000),
                    new SoftCapRow(7000, 320_000),
                    new SoftCapRow(10_000, 450_000),
                    new SoftCapRow(20_000, 700_000)
            };
        };
    }

    private static double interpolateSoftCap(SoftCapRow[] table, int level, double fallback) {
        if (table == null || table.length == 0) {
            return fallback;
        }
        if (level <= table[0].level) {
            return table[0].cap;
        }
        SoftCapRow last = table[table.length - 1];
        if (level >= last.level) {
            return last.cap;
        }
        for (int i = 0; i < table.length - 1; i++) {
            SoftCapRow a = table[i];
            SoftCapRow b = table[i + 1];
            if (level >= a.level && level <= b.level) {
                double t = (level - a.level) / (double) Math.max(1, b.level - a.level);
                return Math.floor(a.cap + (b.cap - a.cap) * t);
            }
        }
        return last.cap;
    }

    private record SoftCapRow(int level, double cap) {}

    private static void adjustTp(ServerPlayer player, double delta) {
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        try {
            Resources resources = data.getResources();
            if (resources == null) {
                return;
            }
            if (delta > 0) {
                DmzRewards.awardTp(player, (float) Math.floor(delta), "end-adjust", false, "");
                return;
            }
            try {
                resources.removeTrainingPoints((float) Math.floor(-delta));
            } catch (Throwable t) {
                float cur = resources.getTrainingPoints();
                resources.setTrainingPoints(Math.max(0.0f, cur + (float) delta));
            }
            try {
                com.dragonminez.common.network.NetworkHandler.sendToTrackingEntityAndSelf(
                        new com.dragonminez.common.network.S2C.StatsSyncS2C(player), player);
            } catch (Throwable ignored) {
            }
        } catch (Throwable ignored) {
        }
    }

    private static PlayerPower strongestInEnd(ServerLevel level, ServerPlayer fallback) {
        PlayerPower best = fallback == null ? null : readPower(fallback);
        double bestScore = best == null ? -1.0 : score(best);
        if (level == null) {
            return best != null ? best : new PlayerPower();
        }
        for (ServerPlayer p : level.m_6907_()) {
            if (p == null || !isTheEnd(p.m_9236_())) {
                continue;
            }
            PlayerPower power = readPower(p);
            double s = score(power);
            if (s > bestScore) {
                bestScore = s;
                best = power;
            }
        }
        if (best == null) {
            return new PlayerPower();
        }
        return best;
    }

    private static PlayerPower nearbyPower(LivingEntity entity, ServerLevel level, PlayerPower fallback) {
        PlayerPower best = fallback;
        double bestScore = score(best);
        AABB box = entity.m_20191_().m_82400_(96.0);
        for (ServerPlayer p : level.m_45976_(ServerPlayer.class, box)) {
            PlayerPower power = readPower(p);
            double s = power.level * 1000.0 + power.defense + power.bp * 0.01 + power.melee * 10.0 + power.maxHp;
            if (s > bestScore) {
                bestScore = s;
                best = power;
            }
        }
        return best;
    }

    private static PlayerPower readPower(ServerPlayer player) {
        PlayerPower out = new PlayerPower();
        if (player == null) {
            return out;
        }
        out.name = player.m_6302_();
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return out;
        }
        try {
            out.level = Math.max(1, data.getLevel());
        } catch (Throwable ignored) {
        }
        try {
            out.bp = Math.max(0.0, data.getBattlePowerExact());
            if (!(out.bp > 0)) {
                out.bp = Math.max(0.0, data.getBattlePower());
            }
        } catch (Throwable ignored) {
        }
        try {
            out.melee = Math.max(0.0, data.getMeleeDamage());
        } catch (Throwable ignored) {
        }
        try {
            out.maxHp = Math.max(20.0, data.getMaxHealth());
        } catch (Throwable ignored) {
        }
        try {
            out.defense = Math.max(0.0, data.getDefense());
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static double score(PlayerPower p) {
        if (p == null) {
            return -1;
        }
        return p.melee * 100.0 + p.bp + p.level * 1000.0 + p.maxHp + p.defense;
    }

    private static String classify(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity instanceof EnderDragon) {
            return "dragon";
        }
        if (entity instanceof EnderMan) {
            return "enderman";
        }
        if (entity instanceof Endermite) {
            return "endermite";
        }
        if (entity instanceof Shulker) {
            return "shulker";
        }
        if (entity instanceof Phantom) {
            return "phantom";
        }
        try {
            ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.m_6095_());
            if (id != null) {
                String path = id.m_135815_();
                if ("enderman".equals(path)) {
                    return "enderman";
                }
                if ("endermite".equals(path)) {
                    return "endermite";
                }
                if ("shulker".equals(path)) {
                    return "shulker";
                }
                if ("phantom".equals(path)) {
                    return "phantom";
                }
                if ("ender_dragon".equals(path)) {
                    return "dragon";
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static void setMaxHealth(LivingEntity entity, double max, double current) {
        try {
            AttributeInstance attr = entity.m_21051_(Attributes.f_22276_); // MAX_HEALTH
            if (attr != null) {
                attr.m_22100_(max); // setBaseValue
            }
            entity.m_21153_((float) Math.min(max, Math.max(1.0, current)));
            CompoundTag tag = PersistentDataAccess.get(entity);
            if (PersistentDataAccess.isWritable(tag)) {
                tag.m_128347_(NBT_MAX, max);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void setAttack(LivingEntity entity, double dmg) {
        try {
            AttributeInstance attr = entity.m_21051_(Attributes.f_22281_);
            if (attr != null) {
                attr.m_22100_(Math.max(1.0, dmg));
            }
        } catch (Throwable ignored) {
        }
    }

    private static void storeDef(LivingEntity entity, double def) {
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128347_(NBT_DEF, def);
        }
    }

    private static double readDef(LivingEntity entity) {
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (!PersistentDataAccess.isWritable(tag) || !tag.m_128441_(NBT_DEF)) {
            return 0.0;
        }
        try {
            return Math.max(0.0, tag.m_128459_(NBT_DEF));
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    private static void storeHits(LivingEntity entity, int hits) {
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128356_(NBT_HITS, hits);
        }
    }

    private static void storeDmzHp(LivingEntity entity, double hp) {
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128347_(NBT_DMZ_HP, hp);
        }
    }

    private static void markBuffed(LivingEntity entity) {
        CompoundTag tag = PersistentDataAccess.get(entity);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128379_(TAG_BUFFED, true);
        }
    }

    private static boolean alreadyBuffed(LivingEntity entity) {
        return PersistentDataAccess.flag(entity, TAG_BUFFED);
    }

    private static void msg(ServerPlayer player, String text) {
        DmzRewards.msg(player, text);
    }

    private static final class PlayerPower {
        int level = 1;
        double bp;
        double melee;
        double maxHp = 20;
        double defense;
        String name = "?";
    }

    private static final class PendingTp {
        final String kind;
        final double maxHp;
        int delayTicks;

        PendingTp(String kind, double maxHp, int delayTicks) {
            this.kind = kind;
            this.maxHp = maxHp;
            this.delayTicks = delayTicks;
        }
    }

    private enum MobTier {
        ENDERMITE("endermite", 1, 1200, 40, 5000, 10, "Endermite", 2200),
        PHANTOM("phantom", 2, 1800, 70, 9000, 14, "Phantom", 3200),
        ENDERMAN("enderman", 3, 2400, 12, 14000, 18, "Enderman", 4200),
        SHULKER("shulker", 4, 3200, 80, 18000, 22, "Shulker", 5500);

        final String id;
        final int tier;
        final double hp;
        final double damage;
        final double defense;
        final int hits;
        final String label;
        final double hpCap;

        MobTier(String id, int tier, double hp, double damage, double defense, int hits, String label, double hpCap) {
            this.id = id;
            this.tier = tier;
            this.hp = hp;
            this.damage = damage;
            this.defense = defense;
            this.hits = hits;
            this.label = label;
            this.hpCap = hpCap;
        }

        static MobTier of(String kind) {
            if (kind == null) {
                return null;
            }
            for (MobTier t : values()) {
                if (t.id.equals(kind)) {
                    return t;
                }
            }
            return null;
        }
    }
}
