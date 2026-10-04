package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceStatsConfig;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import java.util.Collection;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/**
 * Validates a fighting class the same way as {@code /dmzclass}, then sets the class id.
 * DragonMineZ and dmzrevamp own class stat scaling. This only pulls a current pool down
 * if it sits above the new live max. It does not add the max jump onto current ki,
 * stamina, or health.
 */
public final class DmzClassCommandApply {
    private DmzClassCommandApply() {}

    /** Same rules as {@code ClassCommand.isValidClass}. */
    public static boolean isValidClass(StatsData data, String classId) {
        if (data == null || classId == null || classId.isBlank()) {
            return false;
        }
        Character ch = data.getCharacter();
        if (ch == null) {
            return false;
        }
        String race = ch.getRaceName();
        if (race == null || race.isBlank()) {
            return false;
        }
        RaceStatsConfig raceStats;
        try {
            raceStats = ConfigManager.getRaceStats(race);
        } catch (Throwable ignored) {
            raceStats = null;
        }
        if (raceStats == null) {
            return false;
        }
        Collection<String> classes;
        try {
            classes = raceStats.getAllClasses();
        } catch (Throwable ignored) {
            return false;
        }
        if (classes == null) {
            return false;
        }
        return classes.contains(classId.trim().toLowerCase(Locale.ROOT));
    }

    public static String normalizeClassId(String classId) {
        if (classId == null || classId.isBlank()) {
            return "";
        }
        return classId.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * @param resourceSnapshot ignored. Kept so callers do not need a signature change.
     */
    public static void applyClass(
            ServerPlayer player, StatsData data, String classId, float[] resourceSnapshot) {
        if (player == null || data == null || classId == null || classId.isBlank()) {
            return;
        }
        String lowered = normalizeClassId(classId);
        if (!isValidClass(data, lowered)) {
            return;
        }
        Character ch = data.getCharacter();
        if (ch == null) {
            return;
        }
        ch.setCharacterClass(lowered);
        try {
            DmzResourcePoolClamp.clamp(data);
        } catch (Throwable ignored) {
        }
        pushStatsSync(player);
    }

    public static void pushStatsSync(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }
}
