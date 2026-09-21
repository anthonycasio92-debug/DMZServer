package com.dbzlegacy.adaptivedifficulty.character;

import com.dragonminez.common.network.C2S.UpdateCharacterC2S;
import java.lang.reflect.Field;
import net.minecraft.server.level.ServerPlayer;

/** Validates class on {@link UpdateCharacterC2S} during free post-race-change class pick. */
public final class RaceChangeClassPickPacketGuard {
    private RaceChangeClassPickPacketGuard() {}

    public static void applyUpdateCharacterPacket(UpdateCharacterC2S packet, ServerPlayer player) {
        if (packet == null || player == null || !RaceChangeClassPickFlow.isActive(player)) {
            return;
        }
        String targetRace = RaceChangeClassPickFlow.targetRaceId(player);
        String priorClass = RaceChangeClassPickFlow.priorFightingClass(player);
        if (targetRace == null || targetRace.isBlank()) {
            return;
        }
        try {
            Field classField = UpdateCharacterC2S.class.getDeclaredField("className");
            classField.setAccessible(true);
            String incoming = (String) classField.get(packet);
            String mapped =
                    RaceChangeClassMapper.canonicalPacketClass(targetRace, incoming, priorClass);
            classField.set(packet, mapped);
        } catch (Throwable ignored) {
        }
    }
}
