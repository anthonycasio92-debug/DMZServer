package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.network.C2S.UpdateCharacterC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.AppearanceSyncS2C;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.character.Character;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/**
 * DMZ stores {@code activeHeadBone} on the character (ears, hair, etc.). Validates against the
 * player's race defaults plus {@link CosmeticHeadBoneService} unlocks (global head-bone shop).
 */
public final class RaceHeadBoneSync {
    private static final String DEFAULT_BONE = "hair";

    private RaceHeadBoneSync() {}

    public static String normalizeHeadBone(String raceId, String activeHeadBone) {
        return normalizeHeadBone(null, raceId, activeHeadBone);
    }

    public static String normalizeHeadBone(ServerPlayer player, String raceId, String activeHeadBone) {
        String bone = safe(activeHeadBone);
        if (player != null && !bone.isEmpty() && CosmeticHeadBoneService.isBoneAllowed(player, bone)) {
            return bone.trim().toLowerCase(Locale.ROOT);
        }
        if (raceId == null || raceId.isBlank()) {
            return bone.isEmpty() ? DEFAULT_BONE : bone;
        }
        String[] configured = headBonesForRace(raceId.trim().toLowerCase(Locale.ROOT));
        if (configured.length == 0) {
            return bone.isEmpty() ? DEFAULT_BONE : bone;
        }
        Set<String> allowed = new HashSet<>();
        for (String entry : configured) {
            if (entry != null && !entry.isBlank()) {
                allowed.add(entry.toLowerCase(Locale.ROOT));
            }
        }
        if (player != null) {
            allowed.addAll(CosmeticHeadBoneService.allowedBoneIds(player));
        }
        if (allowed.isEmpty()) {
            return bone.isEmpty() ? DEFAULT_BONE : bone;
        }
        if (!bone.isEmpty() && allowed.contains(bone.toLowerCase(Locale.ROOT))) {
            return bone;
        }
        if (player != null) {
            for (String unlocked : CosmeticHeadBoneService.allowedBoneIds(player)) {
                if (unlocked != null && !unlocked.isBlank()) {
                    return unlocked.toLowerCase(Locale.ROOT);
                }
            }
        }
        return configured[0];
    }

    /** @return true when server character data was updated */
    public static boolean syncCharacter(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return false;
        }
        String race;
        try {
            race = ch.getRace();
        } catch (Throwable t) {
            return false;
        }
        String normalized = normalizeHeadBone(player, race, safeActiveHeadBone(ch));
        String current = safeActiveHeadBone(ch);
        if (normalized.equals(current)) {
            return false;
        }
        try {
            ch.setActiveHeadBone(normalized);
            return true;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void syncClient(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            NetworkHandler.sendToTrackingEntityAndSelf(new AppearanceSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }

    public static void applyPacketHeadBone(UpdateCharacterC2S packet, ServerPlayer player) {
        if (packet == null || player == null) {
            return;
        }
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return;
        }
        String race;
        try {
            race = ch.getRace();
        } catch (Throwable t) {
            return;
        }
        String current;
        try {
            Field field = UpdateCharacterC2S.class.getDeclaredField("activeHeadBone");
            field.setAccessible(true);
            current = (String) field.get(packet);
            String normalized = normalizeHeadBone(player, race, current);
            if (!normalized.equals(safe(current))) {
                field.set(packet, normalized);
            }
        } catch (Throwable ignored) {
        }
    }

    private static String[] headBonesForRace(String raceId) {
        List<String> bones = DmzContentDiscovery.headBonesForRace(raceId);
        if (bones == null || bones.isEmpty()) {
            return new String[] {DEFAULT_BONE};
        }
        return bones.toArray(String[]::new);
    }

    private static String safeActiveHeadBone(Character ch) {
        try {
            return safe(ch.getActiveHeadBone());
        } catch (Throwable t) {
            return "";
        }
    }

    private static String safe(String raw) {
        return raw == null ? "" : raw.trim();
    }
}
