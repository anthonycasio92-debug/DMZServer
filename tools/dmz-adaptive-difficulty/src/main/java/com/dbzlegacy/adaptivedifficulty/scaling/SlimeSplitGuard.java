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
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Vanilla slimes / magma cubes spawn smaller copies on death via {@code addFreshEntity}.
 * Those split children must never receive Adaptive Difficulty paint (would multiply one
 * scaled kill into several scaled cubs).
 * <p>
 * Forge 1.20.1 has no split event — we record a short pending window on parent death and
 * stamp children that join nearby with matching size/type. Magma cubes extend {@link Slime},
 * so both are covered.
 */
public final class SlimeSplitGuard {
    private static final long WINDOW_TICKS = 10L;
    private static final double MATCH_DIST_SQ = 6.0 * 6.0;
    /** Parent UUID → pending split window. */
    private static final Map<UUID, Pending> PENDING = new ConcurrentHashMap<>();

    private SlimeSplitGuard() {}

    /** Call from {@code LivingDeathEvent} when a slime/magma dies. */
    public static void onParentDeath(LivingEntity dead) {
        if (!(dead instanceof Slime slime) || !(dead.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        int size = slime.m_33632_(); // getSize
        if (size <= 1) {
            return;
        }
        EntityType<?> type = slime.m_6095_();
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(type);
        if (typeId == null) {
            return;
        }
        int childSize = size / 2;
        // Vanilla spawns 2–4 children; allow headroom for modded splits.
        PENDING.put(slime.m_20148_(), new Pending(
                level.m_46472_(),
                typeId,
                childSize,
                slime.m_20185_(),
                slime.m_20186_(),
                slime.m_20189_(),
                level.m_46467_() + WINDOW_TICKS,
                6
        ));
        if (PENDING.size() > 256) {
            prune(level.m_46467_());
        }
    }

    /**
     * If {@code child} looks like a just-spawned split offspring, stamp exempt and return true.
     */
    public static boolean tryMarkSplitChild(LivingEntity child) {
        if (!(child instanceof Slime slime) || !(child.m_9236_() instanceof ServerLevel level)) {
            return false;
        }
        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(slime.m_6095_());
        if (typeId == null) {
            return false;
        }
        int size = slime.m_33632_();
        long now = level.m_46467_();
        ResourceKey<Level> dim = level.m_46472_();
        double x = slime.m_20185_();
        double y = slime.m_20186_();
        double z = slime.m_20189_();

        prune(now);
        for (Iterator<Map.Entry<UUID, Pending>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, Pending> e = it.next();
            Pending p = e.getValue();
            if (now > p.expireGameTime || p.remaining <= 0) {
                it.remove();
                continue;
            }
            if (!p.dimension.equals(dim) || !p.typeId.equals(typeId) || p.childSize != size) {
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
            MobScaling.markFromSlimeSplit(slime);
            return true;
        }
        return false;
    }

    private static void prune(long now) {
        PENDING.entrySet().removeIf(e -> now > e.getValue().expireGameTime || e.getValue().remaining <= 0);
    }

    private static final class Pending {
        final ResourceKey<Level> dimension;
        final ResourceLocation typeId;
        final int childSize;
        final double x;
        final double y;
        final double z;
        final long expireGameTime;
        int remaining;

        Pending(
                ResourceKey<Level> dimension,
                ResourceLocation typeId,
                int childSize,
                double x,
                double y,
                double z,
                long expireGameTime,
                int remaining
        ) {
            this.dimension = dimension;
            this.typeId = typeId;
            this.childSize = childSize;
            this.x = x;
            this.y = y;
            this.z = z;
            this.expireGameTime = expireGameTime;
            this.remaining = remaining;
        }
    }
}
