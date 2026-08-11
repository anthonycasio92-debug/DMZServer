package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.util.List;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Dimension deny-list for expensive adaptive-difficulty systems.
 * <p>
 * The End is enabled by default so End hostiles can scale; the Ender Dragon
 * stays exempt in {@code MobScaling} (End Strength script owns that fight).
 */
public final class DimensionGates {
    private DimensionGates() {}

    public static boolean isDisabled(Entity entity) {
        return entity != null && isDisabled(entity.m_9236_());
    }

    public static boolean isDisabled(Level level) {
        if (level == null) {
            return false;
        }
        List<String> denied = DifficultyConfig.get().disabledDimensions;
        if (denied == null || denied.isEmpty()) {
            return false;
        }
        ResourceKey<Level> key = level.m_46472_(); // dimension()
        ResourceLocation loc = key == null ? null : key.m_135782_(); // location()
        String id = loc == null ? "" : loc.toString();
        String path = loc == null ? "" : loc.m_135815_(); // getPath()
        for (String raw : denied) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String want = raw.trim().toLowerCase();
            if (want.equals(id.toLowerCase())
                    || want.equals(path.toLowerCase())
                    || (want.contains(":") && id.equalsIgnoreCase(want))
                    || (!want.contains(":") && path.equalsIgnoreCase(want))) {
                return true;
            }
        }
        return false;
    }
}
