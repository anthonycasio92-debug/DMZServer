package com.dbzlegacy.adaptivedifficulty.claim;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Mohist bridge: check GriefPrevention claims from the Forge side when FTB Chunks claims.
 */
final class GriefPreventionProbe {
    private static volatile boolean resolved;
    private static volatile boolean available;
    private static Object dataStore;
    private static Method getClaimAt;
    private static Class<?> claimClass;
    private static Class<?> locationClass;
    private static Constructor<?> locationCtor;
    private static Method getWorlds;
    private static Method getHandle;
    private static Method levelDimensionMethod;
    private static Method getHighestBlockYAt;
    private static Method getMinHeight;

    private GriefPreventionProbe() {}

    static boolean overlapsGriefPrevention(ChunkDimPos pos) {
        if (pos == null || !resolve()) {
            return false;
        }
        Object world = bukkitWorld(pos.dimension());
        if (world == null) {
            return false;
        }
        int baseX = pos.x() * 16;
        int baseZ = pos.z() * 16;
        int y = highestY(world, baseX + 8, baseZ + 8);
        for (int dx = 0; dx <= 15; dx += 15) {
            for (int dz = 0; dz <= 15; dz += 15) {
                if (claimAt(world, baseX + dx, y, baseZ + dz) != null) {
                    return true;
                }
            }
        }
        return false;
    }

    private static Object claimAt(Object world, int x, int y, int z) {
        try {
            Object loc = locationCtor.newInstance(world, x, y, z);
            return getClaimAt.invoke(dataStore, loc, true, null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static int highestY(Object world, int x, int z) {
        try {
            int y = (int) getHighestBlockYAt.invoke(world, x, z);
            int min = (int) getMinHeight.invoke(world);
            return y < min ? 64 : y;
        } catch (ReflectiveOperationException e) {
            return 64;
        }
    }

    private static Object bukkitWorld(ResourceKey<Level> dimension) {
        if (dimension == null) {
            return null;
        }
        MinecraftServer server = currentServer();
        if (server != null) {
            ServerLevel level = server.m_129880_(dimension);
            if (level != null) {
                Object mapped = worldForHandle(level);
                if (mapped != null) {
                    return mapped;
                }
            }
        }
        try {
            Iterable<?> worlds = (Iterable<?>) getWorlds.invoke(null);
            for (Object world : worlds) {
                if (world == null) {
                    continue;
                }
                Object handle = getHandle.invoke(world);
                Object dim = levelDimensionMethod.invoke(handle);
                if (dimension.equals(dim)) {
                    return world;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static Object worldForHandle(ServerLevel level) {
        try {
            Iterable<?> worlds = (Iterable<?>) getWorlds.invoke(null);
            for (Object world : worlds) {
                if (world == null) {
                    continue;
                }
                Object handle = getHandle.invoke(world);
                if (handle == level) {
                    return world;
                }
            }
        } catch (ReflectiveOperationException ignored) {
        }
        return null;
    }

    private static MinecraftServer currentServer() {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object craftServer = bukkit.getMethod("getServer").invoke(null);
            return (MinecraftServer) craftServer.getClass().getMethod("getServer").invoke(craftServer);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static boolean resolve() {
        if (resolved) {
            return available;
        }
        resolved = true;
        try {
            Class<?> gpCls = Class.forName("me.ryanhamshire.GriefPrevention.GriefPrevention");
            Object gpInstance = gpCls.getField("instance").get(null);
            if (gpInstance == null) {
                return false;
            }
            dataStore = gpCls.getField("dataStore").get(gpInstance);
            if (dataStore == null) {
                return false;
            }
            claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim");
            locationClass = Class.forName("org.bukkit.Location");
            locationCtor = locationClass.getConstructor(
                    Class.forName("org.bukkit.World"),
                    double.class,
                    double.class,
                    double.class
            );
            Class<?> worldClass = Class.forName("org.bukkit.World");
            getHighestBlockYAt = worldClass.getMethod("getHighestBlockYAt", int.class, int.class);
            getMinHeight = worldClass.getMethod("getMinHeight");
            getClaimAt = dataStore.getClass().getMethod(
                    "getClaimAt",
                    locationClass,
                    boolean.class,
                    claimClass
            );
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            getWorlds = bukkit.getMethod("getWorlds");
            getHandle = worldClass.getMethod("getHandle");
            levelDimensionMethod = Class.forName("net.minecraft.server.level.ServerLevel").getMethod("dimension");
            available = true;
            AdaptiveDifficultyMod.LOGGER.info("[{}] GriefPrevention overlap guard ready", AdaptiveDifficultyMod.MOD_ID);
        } catch (ReflectiveOperationException e) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GriefPrevention overlap guard unavailable: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    e.toString()
            );
            available = false;
        }
        return available;
    }
}
