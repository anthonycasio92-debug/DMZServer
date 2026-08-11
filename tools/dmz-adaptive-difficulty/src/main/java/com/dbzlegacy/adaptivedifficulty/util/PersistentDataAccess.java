package com.dbzlegacy.adaptivedifficulty.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/**
 * Forge {@code Entity#getPersistentData()} without requiring patched Entity on javac CP.
 * Uses a cached {@link MethodHandle} — hot paths call this frequently.
 */
public final class PersistentDataAccess {
    private static final MethodHandle GET;
    private static final CompoundTag EMPTY = new CompoundTag();

    static {
        MethodHandle handle = null;
        try {
            Method m = Entity.class.getMethod("getPersistentData");
            m.setAccessible(true);
            handle = MethodHandles.lookup().unreflect(m);
        } catch (Throwable ignored) {
        }
        GET = handle;
    }

    private PersistentDataAccess() {}

    /**
     * Real persistent data, or a shared empty sentinel if unavailable.
     * <p>
     * Never write to the sentinel — use {@link #isWritable(CompoundTag)} first.
     * Callers that ignore this can corrupt shared state across entities.
     */
    public static CompoundTag get(Entity entity) {
        if (entity == null || GET == null) {
            return EMPTY;
        }
        try {
            Object tag = GET.invoke(entity);
            return tag instanceof CompoundTag c ? c : EMPTY;
        } catch (Throwable t) {
            return EMPTY;
        }
    }

    /** True when {@code tag} is a real entity persistent-data compound (safe to mutate). */
    public static boolean isWritable(CompoundTag tag) {
        return tag != null && tag != EMPTY;
    }

    public static boolean has(Entity entity, String key) {
        CompoundTag tag = get(entity);
        return tag != EMPTY && tag.m_128441_(key);
    }

    public static boolean flag(Entity entity, String key) {
        CompoundTag tag = get(entity);
        return tag != EMPTY && tag.m_128471_(key);
    }

    public static long getLong(Entity entity, String key, long fallback) {
        CompoundTag tag = get(entity);
        if (tag == EMPTY || !tag.m_128441_(key)) {
            return fallback;
        }
        return tag.m_128454_(key);
    }

    public static String getString(Entity entity, String key) {
        CompoundTag tag = get(entity);
        if (tag == EMPTY || !tag.m_128441_(key)) {
            return "";
        }
        return tag.m_128461_(key);
    }
}
