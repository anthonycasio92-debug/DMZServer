package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.ClassPermissionSync;
import com.dragonminez.common.passives.PassiveRuntimeState;
import com.dragonminez.common.stats.StatsData;
import net.minecraft.server.level.ServerPlayer;

/** Runs after DMZ applies a fighting class on the server (packets or Character Services). */
public final class DmzCharacterClassChangeHooks {
    private DmzCharacterClassChangeHooks() {}

    /**
     * Paid Character Services <em>class</em> change — mirrors {@code /dmzclass} after the class id is
     * chosen (call after optional stat preservation).
     */
    public static void onPaidClassChange(
            ServerPlayer player,
            StatsData data,
            String classId,
            float[] resourceSnapshotBeforeClassChange,
            boolean preserveExactPrimaries) {
        if (player == null || data == null) {
            return;
        }
        clearPassiveRuntime(player);
        DmzClassCommandApply.applyClass(player, data, classId, resourceSnapshotBeforeClassChange);
        if (!preserveExactPrimaries) {
            try {
                data.relocateStats(player);
            } catch (Throwable ignored) {
            }
            DmzClassCommandApply.pushStatsSync(player);
        }
        ClassPermissionSync.syncAuthoritativeClassChange(player);
    }

    /** Race change (or 0% wipe follow-up) where race + class mapping already ran on {@link StatsData}. */
    public static void onServicesRaceClassApplied(
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
        float[] snap = DmzClassChangeCapture.take(player);
        String pickedFromPacket = packetClassName == null ? "" : packetClassName.trim();
        boolean classPickSession = RaceChangeClassPickFlow.isActive(player);
        String appliedClass = "";
        if (classPickSession) {
            String race = RaceChangeClassPickFlow.targetRaceId(player);
            String picked =
                    pickedFromPacket.isBlank() ? DmzProgression.fightingClass(player) : pickedFromPacket;
            String prior = RaceChangeClassPickFlow.priorFightingClass(player);
            appliedClass =
                    RaceChangeClassMapper.commitFightingClassForRace(data, race, picked, prior);
            RaceChangeClassPickFlow.clear(player);
        } else if (snap != null && !pickedFromPacket.isBlank()) {
            String race = DmzProgression.race(player);
            appliedClass =
                    RaceChangeClassMapper.commitFightingClassForRace(
                            data, race, pickedFromPacket, pickedFromPacket);
        }
        if (classPickSession || snap != null) {
            if (appliedClass != null && !appliedClass.isBlank()) {
                DmzClassCommandApply.applyClass(player, data, appliedClass, snap);
            } else if (!pickedFromPacket.isBlank()) {
                DmzClassCommandApply.applyClass(player, data, pickedFromPacket, snap);
            }
            ClassPermissionSync.syncAuthoritativeClassChange(player);
        }
    }

    private static void clearPassiveRuntime(ServerPlayer player) {
        try {
            PassiveRuntimeState.clear(player.m_20148_());
        } catch (Throwable ignored) {
        }
    }
}
