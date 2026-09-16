package com.dbzlegacy.adaptivedifficulty.claim;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import dev.ftb.mods.ftblibrary.math.ChunkDimPos;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * Mohist bridge: check GriefPrevention claims from the Forge side when FTB Chunks claims.
 * GP lives in the Bukkit plugin classloader — never use {@code Class.forName} on the mod loader.
 */
final class GriefPreventionProbe {
    private static volatile boolean available;
    private static volatile long lastResolveAttemptMs;
    private static final long RESOLVE_RETRY_MS = 5000L;

    private static Object dataStore;
    private static Class<?> claimClass;
    private static Constructor<?> locationCtor;
    private static Method getClaimAt;
    private static Method getWorlds;
    private static Method getHighestBlockYAt;
    private static Method getMinHeight;

    private GriefPreventionProbe() {}

    static boolean overlapsGriefPrevention(ChunkDimPos pos) {
        if (pos == null || !ensureReady()) {
            return false;
        }
        Object world = bukkitWorld(pos.dimension());
        if (world == null) {
            return false;
        }
        int baseX = pos.x() * 16;
        int baseZ = pos.z() * 16;
        int ySurface = highestY(world, baseX + 8, baseZ + 8);
        int[] ys = {ySurface, 64, Math.max(getMinY(world) + 1, 1)};
        for (int y : ys) {
            for (int dx = 0; dx <= 15; dx += 15) {
                for (int dz = 0; dz <= 15; dz += 15) {
                    if (claimAt(world, baseX + dx, y, baseZ + dz) != null) {
                        return true;
                    }
                }
            }
            if (claimAt(world, baseX + 8, y, baseZ + 8) != null) {
                return true;
            }
        }
        return false;
    }

    private static Object claimAt(Object world, int x, int y, int z) {
        try {
            Object loc = locationCtor.newInstance(world, (double) x, (double) y, (double) z);
            return getClaimAt.invoke(dataStore, loc, true, null);
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static int getMinY(Object world) {
        try {
            return (int) getMinHeight.invoke(world);
        } catch (ReflectiveOperationException e) {
            return -64;
        }
    }

    private static int highestY(Object world, int x, int z) {
        try {
            int y = (int) getHighestBlockYAt.invoke(world, x, z);
            int min = getMinY(world);
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
                Object handle = world.getClass().getMethod("getHandle").invoke(world);
                Object dim = handle.getClass().getMethod("dimension").invoke(handle);
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
                Object handle = world.getClass().getMethod("getHandle").invoke(world);
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

    private static boolean ensureReady() {
        if (available) {
            return true;
        }
        long now = System.currentTimeMillis();
        if (now - lastResolveAttemptMs < RESOLVE_RETRY_MS) {
            return false;
        }
        lastResolveAttemptMs = now;
        return resolve();
    }

    private static boolean resolve() {
        try {
            Class<?> bukkit = Class.forName("org.bukkit.Bukkit");
            Object craftServer = bukkit.getMethod("getServer").invoke(null);
            Object pluginManager = craftServer.getClass().getMethod("getPluginManager").invoke(craftServer);
            Object gpPlugin = pluginManager.getClass()
                    .getMethod("getPlugin", String.class)
                    .invoke(pluginManager, "GriefPrevention");
            if (gpPlugin == null) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] GriefPrevention overlap guard: plugin not loaded yet",
                        AdaptiveDifficultyMod.MOD_ID);
                return false;
            }
            ClassLoader gpLoader = gpPlugin.getClass().getClassLoader();
            dataStore = gpPlugin.getClass().getField("dataStore").get(gpPlugin);
            if (dataStore == null) {
                return false;
            }
            claimClass = Class.forName("me.ryanhamshire.GriefPrevention.Claim", true, gpLoader);
            Class<?> locationClass = Class.forName("org.bukkit.Location", true, gpLoader);
            Class<?> worldClass = Class.forName("org.bukkit.World", true, gpLoader);
            locationCtor = locationClass.getConstructor(worldClass, double.class, double.class, double.class);
            getHighestBlockYAt = worldClass.getMethod("getHighestBlockYAt", int.class, int.class);
            getMinHeight = worldClass.getMethod("getMinHeight");
            getClaimAt = dataStore.getClass().getMethod(
                    "getClaimAt",
                    locationClass,
                    boolean.class,
                    claimClass
            );
            getWorlds = bukkit.getMethod("getWorlds");
            for (Object world : (Iterable<?>) getWorlds.invoke(null)) {
                if (world == null) {
                    continue;
                }
                world.getClass().getMethod("getHandle").invoke(world);
                available = true;
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] GriefPrevention overlap guard ready (plugin classloader, world={})",
                        AdaptiveDifficultyMod.MOD_ID,
                        world.getClass().getName());
                return true;
            }
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] GriefPrevention overlap guard: Bukkit worlds not ready",
                    AdaptiveDifficultyMod.MOD_ID);
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
