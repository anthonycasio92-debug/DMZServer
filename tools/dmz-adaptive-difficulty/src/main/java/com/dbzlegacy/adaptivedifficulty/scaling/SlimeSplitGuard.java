package com.dbzlegacy.adaptivedifficulty.scaling;

import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.MagmaCube;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Vanilla slimes / magma cubes spawn smaller copies on death via {@code addFreshEntity}.
 * Those split children must never receive Adaptive Difficulty paint (would multiply one
 * scaled kill into several scaled cubs).
 * <p>
 * Forge 1.20.1 has no split event — we record a pending window on parent death and
 * stamp children that join nearby. Matching is intentionally loose for Mohist timing.
 */
public final class SlimeSplitGuard {
    /** Long enough for delayed Mohist join / next-tick addFreshEntity. */
    private static final long WINDOW_TICKS = 60L;
    private static final double MATCH_DIST_SQ = 24.0 * 24.0;
    /** Session fallback when entity persistent-data is not writable yet. */
    private static final Map<UUID, Long> MEMORY_EXEMPT = new ConcurrentHashMap<>();
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private SlimeSplitGuard() {}

    /** Call from {@code LivingDeathEvent} when a slime/magma dies. */
    public static void onParentDeath(LivingEntity dead) {
        if (!isSlimeFamily(dead) || !(dead.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        int size = slimeSize(dead);
        if (size <= 1) {
            return;
        }
        EntityType<?> type = dead.m_6095_();
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(type);
        String family = familyKey(typeId, dead);
        int childSize = Math.max(1, size / 2);
        PENDING.put(dead.m_20148_(), new Pending(
                level.m_46472_(),
                family,
                size,
                childSize,
                dead.m_20185_(),
                dead.m_20186_(),
                dead.m_20189_(),
                level.m_46467_() + WINDOW_TICKS,
                8
        ));
        if (PENDING.size() > 256) {
            prune(level.m_46467_());
        }
    }

    /**
     * If {@code child} looks like a just-spawned split offspring, stamp exempt and return true.
     */
    public static boolean tryMarkSplitChild(LivingEntity child) {
        if (!isSlimeFamily(child) || !(child.m_9236_() instanceof ServerLevel level)) {
            return false;
        }
        UUID id = child.m_20148_();
        long now = level.m_46467_();
        if (isMemoryExempt(id, now)) {
            MobScaling.markFromSlimeSplit(child);
            return true;
        }

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(child.m_6095_());
        String family = familyKey(typeId, child);
        int size = slimeSize(child);
        ResourceKey<Level> dim = level.m_46472_();
        double x = child.m_20185_();
        double y = child.m_20186_();
        double z = child.m_20189_();

        prune(now);
        for (Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Pending> e = it.next();
            Pending p = e.getValue();
            if (now > p.expireGameTime || p.remaining <= 0) {
                it.remove();
                continue;
            }
            if (!p.dimension.equals(dim) || !p.family.equals(family)) {
                continue;
            }
            // Prefer exact half-size; also accept any strictly smaller cub (Mohist size races).
            boolean sizeOk = size == p.childSize || (size > 0 && size < p.parentSize);
            if (!sizeOk) {
                continue;
            }
            double dx = x - p.x;
            double dy = y - p.y;
            double dz = z - p.z;
            if (dx * dx + dy * dy + dz * dz > MATCH_DIST_SQ) {
                continue;
            }
            p.remaining--;
            if (p.remaining <= 0) {
                it.remove();
            }
            MEMORY_EXEMPT.put(id, now + 1200L); // ~60s session guard
            MobScaling.markFromSlimeSplit(child);
            return true;
        }
        return false;
    }

    /** True when this entity was marked as a split child (NBT or session memory). */
    public static boolean isSplitChild(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (MobScaling.isSlimeSplitTagged(entity)) {
            return true;
        }
        long now = 0L;
        if (entity.m_9236_() instanceof ServerLevel level) {
            now = level.m_46467_();
        }
        return isMemoryExempt(entity.m_20148_(), now);
    }

    private static boolean isMemoryExempt(UUID id, long now) {
        if (id == null) {
            return false;
        }
        Long until = MEMORY_EXEMPT.get(id);
        if (until == null) {
            return false;
        }
        if (now > 0L && now > until) {
            MEMORY_EXEMPT.remove(id, until);
            return false;
        }
        return true;
    }

    static boolean isSlimeFamily(LivingEntity entity) {
        if (entity == null) {
            return false;
        }
        if (entity instanceof MagmaCube || entity instanceof Slime) {
            return true;
        }
        ResourceLocation id = ForgeRegistries.ENTITY_TYPES.getKey(entity.m_6095_());
        if (id == null) {
            return false;
        }
        String path = id.m_135815_(); // getPath
        return "magma_cube".equals(path) || "slime".equals(path);
    }

    private static String familyKey(ResourceLocation typeId, LivingEntity entity) {
        if (entity instanceof MagmaCube) {
            return "magma_cube";
        }
        if (typeId != null) {
            String path = typeId.m_135815_();
            if ("magma_cube".equals(path) || "slime".equals(path)) {
                return path;
            }
        }
        return entity instanceof Slime ? "slime" : "unknown";
    }

    private static int slimeSize(LivingEntity entity) {
        if (entity instanceof Slime slime) {
            return Math.max(0, slime.m_33632_());
        }
        return 0;
    }

    private static void prune(long now) {
        PENDING.entrySet().removeIf(e -> now > e.getValue().expireGameTime || e.getValue().remaining <= 0);
        if (MEMORY_EXEMPT.size() > 512) {
            MEMORY_EXEMPT.entrySet().removeIf(e -> now > e.getValue());
        }
    }

    private static final class Pending {
        final ResourceKey<Level> dimension;
        final String family;
        final int parentSize;
        final int childSize;
        final double x;
        final double y;
        final double z;
        final long expireGameTime;
        int remaining;

        Pending(
                ResourceKey<Level> dimension,
                String family,
                int parentSize,
                int childSize,
                double x,
                double y,
                double z,
                long expireGameTime,
                int remaining
        ) {
            this.dimension = dimension;
            this.family = family;
            this.parentSize = parentSize;
            this.childSize = childSize;
            this.x = x;
            this.y = y;
            this.z = z;
            this.expireGameTime = expireGameTime;
            this.remaining = remaining;
        }
    }
}
