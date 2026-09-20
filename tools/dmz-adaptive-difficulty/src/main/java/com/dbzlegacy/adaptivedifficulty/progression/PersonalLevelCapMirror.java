package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigePointsSystem;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

/**
 * Mirrors the server personal level cap onto player persistent data for DMZ client UI
 * (LegacyMechanics is server-only — clients read Overhaul/DMZ configured max, not our mixins).
 */
public final class PersonalLevelCapMirror {
    public static final String KEY = "lm_personal_level_cap";

    private PersonalLevelCapMirror() {}

    public static void publish(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int cap = PrestigePointsSystem.effectiveMaxLevel(player);
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag)) {
            return;
        }
        if (tag.m_128451_(KEY) != cap) {
            tag.m_128405_(KEY, cap);
        }
    }

    public static int read(Player player) {
        if (player == null) {
            return 0;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!tag.m_128441_(KEY)) {
            return 0;
        }
        return Math.max(0, tag.m_128451_(KEY));
    }
}
