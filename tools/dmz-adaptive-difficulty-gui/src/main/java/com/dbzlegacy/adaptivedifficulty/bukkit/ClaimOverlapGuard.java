package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.logging.Logger;
import me.ryanhamshire.GriefPrevention.Claim;
import org.bukkit.ChatColor;
import me.ryanhamshire.GriefPrevention.events.ClaimChangeEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimCreatedEvent;
import me.ryanhamshire.GriefPrevention.events.ClaimResizeEvent;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Blocks GriefPrevention claims that overlap another team's FTB Chunks claim.
 * Team members may GP-claim on chunks their FTB team already owns.
 */
public final class ClaimOverlapGuard implements Listener {
    private static final String DENY =
            "This area overlaps another team's FTB Chunks claim. Unclaim their FTB chunk first.";
    private static final String BYPASS_PERM = "legacymechanics.claimoverlap.bypass";

    private final JavaPlugin plugin;
    private final FtChunksProbe ftb;

    public ClaimOverlapGuard(JavaPlugin plugin) {
        this.plugin = plugin;
        this.ftb = new FtChunksProbe(plugin.getLogger());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClaimCreated(ClaimCreatedEvent event) {
        if (event == null || event.getClaim() == null) {
            return;
        }
        if (canBypass(event.getCreator())) {
            return;
        }
        if (!ftb.overlapsForeignFtChunk(event.getClaim(), actorUuid(event.getCreator(), event.getClaim()))) {
            return;
        }
        event.setCancelled(true);
        sendDeny(event.getCreator());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onClaimChanged(ClaimChangeEvent event) {
        if (event == null || event.getTo() == null) {
            return;
        }
        CommandSender modifier = event instanceof ClaimResizeEvent ? ((ClaimResizeEvent) event).getModifier() : null;
        if (canBypass(modifier)) {
            return;
        }
        if (!ftb.overlapsForeignFtChunk(event.getTo(), actorUuid(modifier, event.getTo()))) {
            return;
        }
        event.setCancelled(true);
        sendDeny(modifier);
    }

    private boolean canBypass(CommandSender sender) {
        if (sender instanceof Player player) {
            return player.isOp() || player.hasPermission(BYPASS_PERM);
        }
        return sender != null && !(sender instanceof Player);
    }

    private UUID actorUuid(CommandSender sender, Claim claim) {
        if (sender instanceof Player player) {
            return player.getUniqueId();
        }
        if (claim != null && claim.getOwnerID() != null) {
            return claim.getOwnerID();
        }
        return null;
    }

    private void sendDeny(CommandSender sender) {
        if (sender instanceof Player player) {
            player.sendMessage(ChatColor.RED + DENY);
            return;
        }
        if (sender != null) {
            sender.sendMessage(DENY);
        }
    }

    private static final class FtChunksProbe {
        private final Logger log;
        private volatile boolean resolved;
        private volatile boolean available;
        private Object manager;
        private Method getChunk;
        private Constructor<?> chunkDimPosCtor;
        private Method levelDimensionMethod;
        private Method worldHandleMethod;
        private Class<?> worldHandleClass;

        private Method getTeamData;
        private Method isTeamMember;

        private FtChunksProbe(Logger log) {
            this.log = log;
        }

        boolean overlapsForeignFtChunk(Claim claim, UUID actorId) {
            if (claim == null || !resolve()) {
                return false;
            }
            for (Chunk chunk : claim.getChunks()) {
                if (chunk == null) {
                    continue;
                }
                if (isForeignFtChunk(chunk.getWorld(), chunk.getX(), chunk.getZ(), actorId)) {
                    return true;
                }
            }
            return false;
        }

        private boolean isForeignFtChunk(World world, int chunkX, int chunkZ, UUID actorId) {
            try {
                Object dim = dimensionKey(world, chunkX, chunkZ);
                if (dim == null) {
                    return false;
                }
                Object pos = chunkDimPosCtor.newInstance(dim, chunkX, chunkZ);
                Object claimed = getChunk.invoke(manager, pos);
                if (claimed == null) {
                    return false;
                }
                if (actorId == null) {
                    return true;
                }
                Object teamData = getTeamData.invoke(claimed);
                if (teamData == null) {
                    return true;
                }
                Object allowed = isTeamMember.invoke(teamData, actorId);
                return !Boolean.TRUE.equals(allowed);
            } catch (ReflectiveOperationException e) {
                return false;
            }
        }

        private Object dimensionKey(World world, int chunkX, int chunkZ) throws ReflectiveOperationException {
            if (world == null) {
                return null;
            }
            try {
                Object level = serverLevel(world);
                if (level != null) {
                    return levelDimensionMethod.invoke(level);
                }
            } catch (ReflectiveOperationException ignored) {
            }
            // Mohist/Paper: CraftChunk → LevelChunk → ServerLevel
            Chunk chunk = world.getChunkAt(chunkX, chunkZ);
            if (chunk == null) {
                return null;
            }
            Object handle = chunk.getClass().getMethod("getHandle").invoke(chunk);
            Object level = handle.getClass().getMethod("getLevel").invoke(handle);
            return levelDimensionMethod.invoke(level);
        }

        private Object serverLevel(World world) throws ReflectiveOperationException {
            Method handleMethod = worldHandleMethod(world);
            return handleMethod.invoke(world);
        }

        private Method worldHandleMethod(World world) throws ReflectiveOperationException {
            Class<?> craft = world.getClass();
            if (worldHandleClass != null && worldHandleClass.isAssignableFrom(craft)) {
                return worldHandleMethod;
            }
            worldHandleMethod = craft.getMethod("getHandle");
            worldHandleClass = craft;
            return worldHandleMethod;
        }

        private boolean resolve() {
            if (available) {
                return true;
            }
            if (resolved) {
                return false;
            }
            resolved = true;
            try {
                Class<?> apiCls = Class.forName("dev.ftb.mods.ftbchunks.api.FTBChunksAPI");
                Object api = apiCls.getMethod("api").invoke(null);
                manager = api.getClass().getMethod("getManager").invoke(api);
                Class<?> chunkDimPosCls = Class.forName("dev.ftb.mods.ftblibrary.math.ChunkDimPos");
                chunkDimPosCtor = chunkDimPosCls.getConstructor(
                        Class.forName("net.minecraft.resources.ResourceKey"),
                        int.class,
                        int.class
                );
                getChunk = manager.getClass().getMethod("getChunk", chunkDimPosCls);
                Class<?> claimedChunkCls = Class.forName("dev.ftb.mods.ftbchunks.api.ClaimedChunk");
                getTeamData = claimedChunkCls.getMethod("getTeamData");
                Class<?> teamDataCls = Class.forName("dev.ftb.mods.ftbchunks.api.ChunkTeamData");
                isTeamMember = teamDataCls.getMethod("isTeamMember", UUID.class);
                levelDimensionMethod = Class.forName("net.minecraft.server.level.ServerLevel").getMethod("dimension");
                // Warm CraftWorld/CraftChunk handle path (Mohist: getHandle is not on org.bukkit.World).
                for (World world : org.bukkit.Bukkit.getWorlds()) {
                    if (world == null) {
                        continue;
                    }
                    try {
                        worldHandleMethod(world);
                        available = true;
                        log.info("[ClaimOverlap] FTB Chunks probe ready (" + world.getClass().getName() + ")");
                        return true;
                    } catch (ReflectiveOperationException ignored) {
                    }
                }
                log.warning("[ClaimOverlap] FTB Chunks probe: no CraftWorld handle yet; will retry on claim");
                resolved = false;
            } catch (ReflectiveOperationException e) {
                log.warning("[ClaimOverlap] FTB Chunks probe unavailable: " + e.toString());
                available = false;
            }
            return available;
        }
    }
}
