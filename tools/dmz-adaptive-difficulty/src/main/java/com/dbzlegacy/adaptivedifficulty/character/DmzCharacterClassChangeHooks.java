package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.ClassPermissionSync;
import com.dragonminez.common.passives.PassiveRuntimeState;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

/** Runs after DMZ applies a fighting class on the server (packets or Character Services). */
public final class DmzCharacterClassChangeHooks {
    private DmzCharacterClassChangeHooks() {}

    /** Paid Character Services class/race change — class already committed on {@link StatsData}. */
    public static void onServicesClassApplied(
            ServerPlayer player, StatsData data, boolean preserveExactPrimaries) {
        if (player == null || data == null) {
            return;
        }
        clearPassiveRuntime(player);
        DmzFightingClassStatsSync.afterFightingClassChange(player, data, preserveExactPrimaries);
        ClassPermissionSync.syncAuthoritativeClassChange(player);
    }

    /**
     * After DMZ {@code UpdateCharacterC2S} applied on the server.
     *
     * @param packetClassName class from the packet (DMZ may skip {@code setCharacterClass} when
     *     {@code getAllClasses().contains} fails)
     */
    public static void onDmzPacketFinished(ServerPlayer player, StatsData data, String packetClassName) {
        if (player == null || data == null) {
            return;
        }
        clearPassiveRuntime(player);
        String pickedFromPacket = packetClassName == null ? "" : packetClassName.trim();
        if (RaceChangeClassPickFlow.isActive(player)) {
            String race = RaceChangeClassPickFlow.targetRaceId(player);
            String picked =
                    pickedFromPacket.isBlank() ? DmzProgression.fightingClass(player) : pickedFromPacket;
            String prior = RaceChangeClassPickFlow.priorFightingClass(player);
            RaceChangeClassMapper.commitFightingClassForRace(data, race, picked, prior);
            RaceChangeClassPickFlow.clear(player);
        } else {
            String race = DmzProgression.race(player);
            String picked =
                    pickedFromPacket.isBlank() ? DmzProgression.fightingClass(player) : pickedFromPacket;
            if (race != null && !race.isBlank() && picked != null && !picked.isBlank()) {
                RaceChangeClassMapper.commitFightingClassForRace(data, race, picked, picked);
            }
        }
        DmzFightingClassStatsSync.afterFightingClassChange(player, data, true);
        ClassPermissionSync.syncAuthoritativeClassChange(player);
    }

    private static void clearPassiveRuntime(ServerPlayer player) {
        try {
            PassiveRuntimeState.clear(player.m_20148_());
        } catch (Throwable ignored) {
        }
    }
}
