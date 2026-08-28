package com.dbzlegacy.adaptivedifficulty.progression.end;

import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.util.ScreenNotify;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Port of Disable End Portals.js — block vanilla End portal / gateway travel and
 * Eye-of-Ender frame lighting so The End is reached only via intentional TPs.
 * Deny feedback is a screen title (not chat). Disable the matching CNPC script
 * so it does not double-fire chat messages.
 */
public final class EndPortalGuard {
    private static final String TEMP_BYPASS = "end.travel.allow";
    private static final String TEMP_MSG = "end.portal.msg";
    private static final long MSG_COOLDOWN_MS = 12_000L;
    private static final String TITLE_PORTAL = "End portals disabled";
    private static final String SUB_PORTAL = "Use a teleport to reach The End";
    private static final String TITLE_EYE = "Cannot activate portal";
    private static final String SUB_EYE = "Use a teleport to reach The End";

    private EndPortalGuard() {}

    public static void onTravelToDimension(EntityTravelToDimensionEvent event) {
        if (!ProgressionConfig.endPortalGuard() || event == null) {
            return;
        }
        try {
            if (!isTheEndDimensionKey(event.getDimension())) {
                return;
            }
            Entity entity = event.getEntity();
            if (!(entity instanceof ServerPlayer player)) {
                return;
            }
            if (hasBypass(player)) {
                return;
            }
            // Only block if the player is actually inside portal/gateway blocks.
            if (!standingInPortalBlock(player)) {
                return;
            }
            event.setCanceled(true);
            ScreenNotify.blocked(player, TITLE_PORTAL, SUB_PORTAL, TEMP_MSG, MSG_COOLDOWN_MS);
            ejectFromPortal(player);
        } catch (Throwable ignored) {
        }
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (!ProgressionConfig.endPortalGuard() || event == null) {
            return;
        }
        try {
            if (event.getLevel() == null || event.getLevel().m_5776_()) {
                return;
            }
            if (!(event.getEntity() instanceof ServerPlayer player)) {
                return;
            }
            if (hasBypass(player)) {
                return;
            }
            ItemStack stack = event.getItemStack();
            if (stack == null || stack.m_41619_() || !isEnderEye(stack)) {
                return;
            }
            BlockPos pos = event.getPos();
            if (pos == null) {
                return;
            }
            BlockState state = event.getLevel().m_8055_(pos);
            if (!isEndPortalFrame(state)) {
                return;
            }
            event.setCanceled(true);
            ScreenNotify.blocked(player, TITLE_EYE, SUB_EYE, TEMP_MSG, MSG_COOLDOWN_MS);
        } catch (Throwable ignored) {
        }
    }

    /** Backup eject while standing in portal blocks (not already in The End). Silent. */
    public static void pulse(MinecraftServer server, int tick) {
        if (!ProgressionConfig.endPortalGuard() || server == null) {
            return;
        }
        // Every 10 ticks — less aggressive than every-other-tick (reduces lag/fly jitter).
        if ((tick % 10) != 0) {
            return;
        }
        try {
            for (ServerPlayer player : server.m_6846_().m_11314_()) {
                if (player == null || hasBypass(player)) {
                    continue;
                }
                if (isTheEndDimensionKey(player.m_9236_().m_46472_())) {
                    continue;
                }
                if (!standingInPortalBlock(player)) {
                    continue;
                }
                // Silent eject only — title is reserved for real travel / eye attempts
                // so lag/fly near portals does not spam screen titles.
                ejectFromPortal(player);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean isTheEndDimensionKey(ResourceKey<Level> dim) {
        if (dim == null) {
            return false;
        }
        if (dim == Level.f_46430_) { // Level.END
            return true;
        }
        try {
            ResourceLocation loc = dim.m_135782_();
            return isTheEndId(loc == null ? "" : loc.toString());
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isTheEndId(String id) {
        if (id == null || id.isBlank()) {
            return false;
        }
        String lower = id.toLowerCase();
        return "minecraft:the_end".equals(lower)
                || "the_end".equals(lower)
                || lower.endsWith(":the_end");
    }

    private static boolean hasBypass(ServerPlayer player) {
        try {
            String raw = ProgressionData.tempGet(player, TEMP_BYPASS, "");
            if (raw != null && !raw.isBlank()) {
                long until = Long.parseLong(raw.trim());
                if (until > System.currentTimeMillis()) {
                    return true;
                }
                ProgressionData.tempPut(player, TEMP_BYPASS, "");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    /**
     * Feet / lower body must intersect an end_portal or end_gateway block.
     * Ignores frames and wide neighborhood probes (those false-fired while flying/lagging).
     */
    private static boolean standingInPortalBlock(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        try {
            Level level = player.m_9236_();
            if (level == null) {
                return false;
            }
            AABB bb = player.m_20191_();
            // Feet / ankles only — ignore upper body so flying past frames
            // or lag-desynced hitboxes don't count as "in portal".
            double pad = 0.12;
            int minX = (int) Math.floor(bb.f_82288_ + pad);
            int minY = (int) Math.floor(bb.f_82289_);
            int minZ = (int) Math.floor(bb.f_82290_ + pad);
            int maxX = (int) Math.floor(bb.f_82291_ - pad);
            int maxY = (int) Math.floor(Math.min(bb.f_82292_, bb.f_82289_ + 0.35) - 0.01);
            int maxZ = (int) Math.floor(bb.f_82293_ - pad);
            if (maxX < minX) {
                maxX = minX;
            }
            if (maxY < minY) {
                maxY = minY;
            }
            if (maxZ < minZ) {
                maxZ = minZ;
            }
            for (int px = minX; px <= maxX; px++) {
                for (int py = minY; py <= maxY; py++) {
                    for (int pz = minZ; pz <= maxZ; pz++) {
                        if (isPortalTravelBlock(level.m_8055_(new BlockPos(px, py, pz)))) {
                            return true;
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static boolean isPortalTravelBlock(BlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.m_60734_();
        if (block == Blocks.f_50259_) { // END_PORTAL
            return true;
        }
        if (block == Blocks.f_50260_) { // END_GATEWAY
            return true;
        }
        try {
            var key = ForgeRegistries.BLOCKS.getKey(block);
            if (key == null) {
                return false;
            }
            String name = key.toString().toLowerCase();
            if (name.contains("end_portal_frame")) {
                return false;
            }
            return name.endsWith("end_portal") || name.endsWith("end_gateway")
                    || name.contains(":end_portal") || name.contains(":end_gateway");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isEndPortalFrame(BlockState state) {
        if (state == null) {
            return false;
        }
        Block block = state.m_60734_();
        if (block == Blocks.f_50258_) { // END_PORTAL_FRAME
            return true;
        }
        try {
            var key = ForgeRegistries.BLOCKS.getKey(block);
            return key != null && key.toString().toLowerCase().contains("end_portal_frame");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static boolean isEnderEye(ItemStack stack) {
        try {
            if (stack.m_41720_() == Items.f_42686_) { // ENDER_EYE
                return true;
            }
        } catch (Throwable ignored) {
        }
        try {
            var key = ForgeRegistries.ITEMS.getKey(stack.m_41720_());
            if (key == null) {
                return false;
            }
            String id = key.toString().toLowerCase();
            return id.contains("ender_eye") || id.contains("eye_of_ender");
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void ejectFromPortal(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            Level level = player.m_9236_();
            double x = player.m_20185_();
            double y = player.m_20186_();
            double z = player.m_20189_();
            double nx = x + 2.0;
            double nz = z + 2.0;
            boolean found = false;
            for (int ox = -2; ox <= 2 && !found; ox++) {
                for (int oz = -2; oz <= 2 && !found; oz++) {
                    if (ox == 0 && oz == 0) {
                        continue;
                    }
                    double tx = x + ox;
                    double tz = z + oz;
                    BlockPos feet = BlockPos.m_274561_(tx, y, tz);
                    BlockPos below = BlockPos.m_274561_(tx, y - 1.0, tz);
                    if (!isPortalTravelBlock(level.m_8055_(feet))
                            && !isPortalTravelBlock(level.m_8055_(below))) {
                        nx = tx;
                        nz = tz;
                        found = true;
                    }
                }
            }
            player.m_6021_(nx, y + 0.5, nz);
            player.m_20256_(new Vec3(0.0, 0.1, 0.0));
            player.f_19789_ = 0.0f; // fallDistance
        } catch (Throwable ignored) {
        }
    }
}
