package com.dbzlegacy.adaptivedifficulty.character;

import com.dragonminez.common.network.C2S.CreateCharacterC2S;
import java.lang.reflect.Field;
import net.minecraft.server.level.ServerPlayer;

/** Ensures DMZ {@link CreateCharacterC2S} uses the paid race-change target race + valid class. */
public final class RaceChangeCreationPacketGuard {
    private RaceChangeCreationPacketGuard() {}

    public static void applyCreateCharacterPacket(CreateCharacterC2S packet, ServerPlayer player) {
        if (packet == null || player == null || !RaceChangeCreationFlow.isActive(player)) {
            return;
        }
        String targetRace = RaceChangeCreationFlow.targetRaceId(player);
        String priorClass = RaceChangeCreationFlow.priorFightingClass(player);
        if (targetRace == null || targetRace.isBlank()) {
            return;
        }
        try {
            Field raceField = CreateCharacterC2S.class.getDeclaredField("raceName");
            raceField.setAccessible(true);
            raceField.set(packet, targetRace.trim().toLowerCase());
        } catch (Throwable ignored) {
        }
        try {
            Field classField = CreateCharacterC2S.class.getDeclaredField("className");
            classField.setAccessible(true);
            String incoming = (String) classField.get(packet);
            String mapped =
                    RaceChangeClassMapper.fightingClassForRace(incoming, priorClass, targetRace);
            classField.set(packet, mapped);
        } catch (Throwable ignored) {
        }
    }
}
