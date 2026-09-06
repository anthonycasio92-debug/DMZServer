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
        private Method getHandle;
        private Method levelDimensionMethod;

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
                Object dim = levelDimensionMethod.invoke(getHandle.invoke(world));
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

        private boolean resolve() {
            if (resolved) {
                return available;
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
                Class<?> worldCls = Class.forName("org.bukkit.World");
                getHandle = worldCls.getMethod("getHandle");
                levelDimensionMethod = Class.forName("net.minecraft.server.level.ServerLevel").getMethod("dimension");
                available = true;
                log.info("[ClaimOverlap] FTB Chunks probe ready");
            } catch (ReflectiveOperationException e) {
                log.warning("[ClaimOverlap] FTB Chunks probe unavailable: " + e.toString());
                available = false;
            }
            return available;
        }
    }
}
