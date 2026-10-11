package com.dbzlegacy.adaptivedifficulty.kube.hotpath;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.registries.ForgeRegistries;

/**
 * Puts nearby Apotheosis spawners back on vanilla delays and clears banned flags.
 * SpawnData and redstone_control stay. Comparator use and the redstone recipes live in this mod.
 */
public final class SpawnerVanillaize {
    private static final String[] NBT_BOOLS = {
        "ignore_players", "ignore_conditions", "ignore_light", "no_ai", "silent", "baby"
    };
    private static final String[] TILE_BOOLS = {
        "ignoresPlayers", "ignoresConditions", "ignoresLight", "hasNoAI", "silent", "baby"
    };
    private static final String[] LOGIC_INTS = {
        "minSpawnDelay", "maxSpawnDelay", "spawnCount", "maxNearbyEntities",
        "requiredPlayerRange", "spawnRange"
    };
    private static Class<?> apothClass;
    private static boolean apothLooked;

    private SpawnerVanillaize() {}

    public static boolean staggered(ServerPlayer player) {
        if (!active()) {
            return false;
        }
        int age = player.f_19797_;
        if (age < 0) {
            return false;
        }
        String key = String.valueOf(player.m_20148_());
        int hash = 0;
        for (int i = 0; i < key.length(); i++) {
            hash = (hash * 31) + key.charAt(i);
        }
        int span = scanInterval();
        int slot = Math.floorMod(hash, span);
        return Math.floorMod(age, span) == slot;
    }

    public static void scan(ServerPlayer player) {
        if (!active() || player == null || !(player.m_9236_() instanceof ServerLevel level)) {
            return;
        }
        int changed = vanillaizeNear(level, player.m_20185_(), player.m_20189_());
        int items = ItemIds.editPlayer(player, (container, slot, stack) -> {
            if (!stripItem(stack)) {
                return false;
            }
            container.m_6836_(slot, stack);
            return true;
        });
        if (changed > 0 || items > 0) {
            HotLog.debug("[Apotheosis Spawner] vanillaized=" + changed + " items=" + items);
        }
    }

    public static void onItemSpawn(ItemStack stack) {
        if (!active()) {
            return;
        }
        stripItem(stack);
    }

    static boolean stripItem(ItemStack stack) {
        if (!"minecraft:spawner".equals(ItemIds.id(stack))) {
            return false;
        }
        CompoundTag tag = stack.m_41783_();
        if (tag == null) {
            return false;
        }
        boolean changed = false;
        if (tag.m_128441_("BlockEntityTag")) {
            CompoundTag blockEntity = tag.m_128469_("BlockEntityTag");
            if (needsReset(blockEntity)) {
                boolean keep = blockEntity.m_128471_("redstone_control");
                changed = applyVanilla(blockEntity, keep) || changed;
                tag.m_128365_("BlockEntityTag", blockEntity);
            }
        }
        if (needsReset(tag)) {
            changed = applyVanilla(tag, tag.m_128471_("redstone_control")) || changed;
        }
        return changed;
    }

    public static int vanillaizeChunk(ServerLevel level, int chunkX, int chunkZ) {
        if (!active() || level == null) {
            return 0;
        }
        LevelChunk chunk = level.m_7726_().m_7131_(chunkX, chunkZ);
        if (chunk == null) {
            return 0;
        }
        Map<BlockPos, BlockEntity> tiles = chunk.m_62954_();
        if (tiles == null || tiles.isEmpty()) {
            return 0;
        }
        int total = 0;
        for (BlockEntity be : new ArrayList<>(tiles.values())) {
            if (be != null && vanillaize(be)) {
                total++;
            }
        }
        return total;
    }

    public static void vanillaizeAt(ServerLevel level, BlockPos pos) {
        if (!active() || level == null || pos == null) {
            return;
        }
        BlockEntity be = level.m_7702_(pos);
        if (be != null) {
            vanillaize(be);
        }
    }

    public static boolean redstoneEnabled(BlockEntity be) {
        return be != null && readRedstone(be);
    }

    public static boolean setRedstone(BlockEntity be, boolean enable) {
        if (!active() || be == null) {
            return false;
        }
        if (readRedstone(be) == enable) {
            return true;
        }
        writeBool(be, "redstoneControl", enable);
        Class<?> apoth = apothClass();
        if (apoth == null || !apoth.isInstance(be)) {
            try {
                CompoundTag tag = be.m_187482_();
                if (tag != null) {
                    tag.m_128379_("redstone_control", enable);
                    be.m_142466_(tag);
                }
            } catch (Throwable t) {
                HotLog.debug("[Apotheosis Spawner] redstone nbt failed: " + t);
            }
        }
        writeBool(be, "redstoneControl", enable);
        sync(be);
        return readRedstone(be) == enable;
    }

    private static int vanillaizeNear(ServerLevel level, double x, double z) {
        int originX = (int) Math.floor(x / 16.0);
        int originZ = (int) Math.floor(z / 16.0);
        int radius = chunkRadius();
        int total = 0;
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                total += vanillaizeChunk(level, originX + dx, originZ + dz);
            }
        }
        return total;
    }

    private static boolean vanillaize(BlockEntity be) {
        BlockState state = be.m_58900_();
        if (state == null) {
            return false;
        }
        ResourceLocation blockId = ForgeRegistries.BLOCKS.getKey(state.m_60734_());
        if (blockId == null || !"minecraft:spawner".equals(blockId.toString())) {
            return false;
        }
        boolean keepRedstone = readRedstone(be);
        boolean changed = false;
        Class<?> apoth = apothClass();
        if (apoth != null && apoth.isInstance(be)) {
            changed = vanillaizeFields(be, keepRedstone);
            keepRedstone = keepRedstone || readRedstone(be);
        } else {
            try {
                CompoundTag tag = be.m_187482_();
                keepRedstone = keepRedstone || (tag != null && tag.m_128471_("redstone_control"));
                if (tag != null && needsReset(tag) && applyVanilla(tag, keepRedstone)) {
                    be.m_142466_(tag);
                    changed = true;
                }
            } catch (Throwable t) {
                HotLog.debug("[Apotheosis Spawner] nbt path failed: " + t);
            }
        }
        if (keepRedstone) {
            Boolean now = readBool(be, "redstoneControl");
            if ((now == null || !now) && writeBool(be, "redstoneControl", true)) {
                changed = true;
            }
        }
        if (changed) {
            sync(be);
        }
        return changed;
    }

    private static boolean vanillaizeFields(BlockEntity be, boolean keepRedstone) {
        boolean changed = false;
        for (String name : TILE_BOOLS) {
            Boolean current = readBool(be, name);
            if (current != null && current && writeBool(be, name, false)) {
                changed = true;
            }
        }
        if (keepRedstone) {
            writeRedstone(be, true);
        }
        Object logic = spawnerLogic(be);
        if (logic != null) {
            int[] values = logicValues();
            for (int i = 0; i < LOGIC_INTS.length; i++) {
                Integer current = readInt(logic, LOGIC_INTS[i]);
                if (current == null || current == values[i]) {
                    continue;
                }
                if (writeInt(logic, LOGIC_INTS[i], values[i])) {
                    changed = true;
                }
            }
        }
        if (keepRedstone) {
            writeRedstone(be, true);
        }
        return changed;
    }

    private static Object spawnerLogic(BlockEntity be) {
        Field field = findField(be.getClass(), "spawner");
        if (field != null) {
            try {
                Object logic = field.get(be);
                if (logic != null) {
                    return logic;
                }
            } catch (Throwable ignored) {
            }
        }
        try {
            Method getter = be.getClass().getMethod("getSpawner");
            return getter.invoke(be);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean needsReset(CompoundTag tag) {
        if (tag == null) {
            return false;
        }
        for (String key : NBT_BOOLS) {
            if (tag.m_128441_(key) && tag.m_128471_(key)) {
                return true;
            }
        }
        int[] values = logicValues();
        return intDiffers(tag, "MinSpawnDelay", values[0])
                || intDiffers(tag, "MaxSpawnDelay", values[1])
                || intDiffers(tag, "SpawnCount", values[2])
                || intDiffers(tag, "MaxNearbyEntities", values[3])
                || intDiffers(tag, "RequiredPlayerRange", values[4])
                || intDiffers(tag, "SpawnRange", values[5]);
    }

    private static boolean intDiffers(CompoundTag tag, String key, int vanilla) {
        if (!tag.m_128441_(key)) {
            return false;
        }
        return tag.m_128451_(key) != vanilla;
    }

    static boolean applyVanilla(CompoundTag tag, boolean keepRedstone) {
        if (tag == null) {
            return false;
        }
        boolean changed = false;
        for (String key : NBT_BOOLS) {
            if (tag.m_128441_(key)) {
                tag.m_128473_(key);
                changed = true;
            }
        }
        if (keepRedstone) {
            if (!tag.m_128471_("redstone_control")) {
                changed = true;
            }
            tag.m_128379_("redstone_control", true);
        }
        int[] values = logicValues();
        changed = putIntIfDiffers(tag, "MinSpawnDelay", values[0]) || changed;
        changed = putIntIfDiffers(tag, "MaxSpawnDelay", values[1]) || changed;
        changed = putIntIfDiffers(tag, "SpawnCount", values[2]) || changed;
        changed = putIntIfDiffers(tag, "MaxNearbyEntities", values[3]) || changed;
        changed = putIntIfDiffers(tag, "RequiredPlayerRange", values[4]) || changed;
        changed = putIntIfDiffers(tag, "SpawnRange", values[5]) || changed;
        return changed;
    }

    private static boolean putIntIfDiffers(CompoundTag tag, String key, int vanilla) {
        if (!tag.m_128441_(key) || tag.m_128451_(key) == vanilla) {
            return false;
        }
        tag.m_128405_(key, vanilla);
        return true;
    }

    private static boolean readRedstone(BlockEntity be) {
        Boolean field = readBool(be, "redstoneControl");
        if (field != null) {
            return field;
        }
        try {
            CompoundTag tag = be.m_187482_();
            return tag != null && tag.m_128471_("redstone_control");
        } catch (Throwable t) {
            return false;
        }
    }

    private static void writeRedstone(BlockEntity be, boolean value) {
        writeBool(be, "redstoneControl", value);
    }

    private static void sync(BlockEntity be) {
        try {
            be.m_6596_();
        } catch (Throwable ignored) {
        }
        try {
            if (be.m_58904_() != null) {
                BlockState state = be.m_58900_();
                be.m_58904_().m_7260_(be.m_58899_(), state, state, 3);
            }
        } catch (Throwable ignored) {
        }
    }

    private static boolean active() {
        return HotPathConfig.get().spawners.enabled;
    }

    private static int scanInterval() {
        return HotPathConfig.ticks(HotPathConfig.get().spawners.scanInterval);
    }

    private static int chunkRadius() {
        int radius = HotPathConfig.get().spawners.chunkRadius;
        if (radius < 0) {
            return 0;
        }
        return Math.min(16, radius);
    }

    private static int[] logicValues() {
        HotPathConfig.Spawners spawners = HotPathConfig.get().spawners;
        return new int[] {
                HotPathConfig.ticks(spawners.minSpawnDelay),
                HotPathConfig.ticks(spawners.maxSpawnDelay),
                HotPathConfig.ticks(spawners.spawnCount),
                HotPathConfig.ticks(spawners.maxNearby),
                HotPathConfig.ticks(spawners.requiredPlayerRange),
                HotPathConfig.ticks(spawners.spawnRange)
        };
    }

    private static Class<?> apothClass() {
        if (apothLooked) {
            return apothClass;
        }
        apothLooked = true;
        try {
            apothClass = Class.forName("dev.shadowsoffire.apotheosis.spawn.spawner.ApothSpawnerTile");
        } catch (Throwable t) {
            apothClass = null;
            HotLog.debug("[Apotheosis Spawner] ApothSpawnerTile missing: " + t);
        }
        return apothClass;
    }

    private static Boolean readBool(Object target, String name) {
        Field field = findField(target.getClass(), name);
        if (field == null) {
            return null;
        }
        try {
            return field.getBoolean(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean writeBool(Object target, String name, boolean value) {
        Field field = findField(target.getClass(), name);
        if (field == null) {
            return false;
        }
        try {
            field.setBoolean(target, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Integer readInt(Object target, String name) {
        Field field = findField(target.getClass(), name);
        if (field == null) {
            return null;
        }
        try {
            return field.getInt(target);
        } catch (Throwable t) {
            return null;
        }
    }

    private static boolean writeInt(Object target, String name, int value) {
        Field field = findField(target.getClass(), name);
        if (field == null) {
            return false;
        }
        try {
            field.setInt(target, value);
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    private static Field findField(Class<?> type, String name) {
        Class<?> cursor = type;
        while (cursor != null && cursor != Object.class) {
            try {
                Field field = cursor.getDeclaredField(name);
                field.setAccessible(true);
                return field;
            } catch (NoSuchFieldException ignored) {
                cursor = cursor.getSuperclass();
            }
        }
        return null;
    }
}
