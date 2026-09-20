package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.ClassPermissionSync;
import com.dbzlegacy.adaptivedifficulty.progression.race.AndroidConversion;
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

    /**
     * Preserved-stat race change when the fighting class is known (auto-mapped or after create/pick).
     * Mirrors {@code /dmzclass} for the class half, then DMZ race follow-up (transform limits, client sync).
     *
     * @param resourceSnapshotBeforeChange from {@link StatsData#snapshotMultiplierResources()} before
     *     race/class ids change
     */
    public static void onServicesRaceChangeApplied(
            ServerPlayer player,
            StatsData data,
            String newRaceId,
            String classId,
            float[] resourceSnapshotBeforeChange,
            boolean preserveExactPrimaries) {
        onServicesRaceChangeApplied(
                player, data, newRaceId, classId, resourceSnapshotBeforeChange, preserveExactPrimaries, classId);
    }

    public static void onServicesRaceChangeApplied(
            ServerPlayer player,
            StatsData data,
            String newRaceId,
            String classId,
            float[] resourceSnapshotBeforeChange,
            boolean preserveExactPrimaries,
            String fallbackPriorClassId) {
        if (player == null || data == null) {
            return;
        }
        clearPassiveRuntime(player);
        String preferred = classId == null ? "" : classId.trim();
        if (preferred.isBlank()) {
            preferred = DmzProgression.fightingClass(player);
        }
        String fallback =
                fallbackPriorClassId == null || fallbackPriorClassId.isBlank()
                        ? preferred
                        : fallbackPriorClassId.trim();
        String applied = preferred;
        if (newRaceId != null && !newRaceId.isBlank()) {
            applied =
                    RaceChangeClassMapper.commitFightingClassForRace(
                            data, newRaceId, preferred, fallback);
        }
        if (applied != null && !applied.isBlank()) {
            DmzClassCommandApply.applyClass(player, data, applied, resourceSnapshotBeforeChange);
        }
        DmzFightingClassStatsSync.afterFightingClassChange(player, data, preserveExactPrimaries);
        ClassPermissionSync.syncAuthoritativeClassChange(player);
        AndroidConversion.stripIfRaceIneligible(player, newRaceId);
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
        DmzClassChangeCapture.Pending pending = DmzClassChangeCapture.takePending(player);
        float[] snap = pending == null ? null : pending.resources();
        String classBeforePacket = pending == null ? "" : pending.fightingClassBefore();
        String pickedFromPacket = packetClassName == null ? "" : packetClassName.trim();
        boolean classPickSession = RaceChangeClassPickFlow.isActive(player);
        if (classPickSession) {
            String pickTargetRace = RaceChangeClassPickFlow.targetRaceId(player);
            String prior = RaceChangeClassPickFlow.priorFightingClass(player);
            if (snap == null) {
                snap = RaceChangeClassPickFlow.resourceSnapshotBackup(player);
            }
            String picked =
                    pickedFromPacket.isBlank() ? DmzProgression.fightingClass(player) : pickedFromPacket;
            RaceChangeClassPickFlow.clear(player);
            if (picked != null && !picked.isBlank()) {
                onServicesRaceChangeApplied(
                        player, data, pickTargetRace, picked, snap, true, prior);
            }
            return;
        }
        if (snap != null && !pickedFromPacket.isBlank()) {
            if (fightingClassUnchangedSincePacket(player, classBeforePacket)) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] Skipping UpdateCharacter class follow-up for {} — fighting class still {}"
                                + " (cosmetic / rejected packet class {})",
                        AdaptiveDifficultyMod.MOD_ID,
                        player.m_7755_().getString(),
                        classBeforePacket,
                        pickedFromPacket);
                return;
            }
            String race = DmzProgression.race(player);
            onServicesRaceChangeApplied(
                    player, data, race, pickedFromPacket, snap, true, pickedFromPacket);
        }
    }

    /**
     * DMZ may ignore {@code setCharacterClass} when the packet id is not in {@code getAllClasses()},
     * but Legacy Mechanics used to still run {@code updateTransformationSkillLimits} — wiping forms.
     */
    private static boolean fightingClassUnchangedSincePacket(
            ServerPlayer player, String classBeforePacket) {
        if (classBeforePacket == null || classBeforePacket.isBlank()) {
            return false;
        }
        String now = DmzProgression.fightingClass(player);
        return now != null && classBeforePacket.equalsIgnoreCase(now);
    }

    private static void clearPassiveRuntime(ServerPlayer player) {
        try {
            PassiveRuntimeState.clear(player.m_20148_());
        } catch (Throwable ignored) {
        }
    }
}
