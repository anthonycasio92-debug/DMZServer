package com.dbzlegacy.mohistmelee;

import java.lang.reflect.Method;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;

/** Forge {@code Entity#getPersistentData()} without requiring patched Entity on javac CP. */
public final class PersistentDataAccess {
    private static final Method GET;

    static {
        Method m = null;
        try {
            m = Entity.class.getMethod("getPersistentData");
        } catch (Throwable ignored) {
        }
        GET = m;
    }

    private PersistentDataAccess() {}

    public static CompoundTag get(Entity entity) {
        if (entity == null || GET == null) {
            return new CompoundTag();
        }
        try {
            Object tag = GET.invoke(entity);
            return tag instanceof CompoundTag c ? c : new CompoundTag();
        } catch (Throwable t) {
            return new CompoundTag();
        }
    }
}
