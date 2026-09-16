package com.dbzlegacy.adaptivedifficulty.character;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.network.C2S.UpdateCharacterC2S;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.character.Character;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * While a paid reskin editor is open, DMZ still exposes the class tab — we lock class server-side
 * on {@link UpdateCharacterC2S} so appearance edits cannot change fighting class.
 */
public final class ReskinSessionGuard {
    private static final long SESSION_MS = 20L * 60L * 1000L;
    private static final Map<UUID, Session> ACTIVE = new ConcurrentHashMap<>();

    private ReskinSessionGuard() {}

    public static void begin(ServerPlayer player) {
        if (player == null || !CharacterServicesConfig.get().reskin.lockClassDuringEditor) {
            return;
        }
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return;
        }
        String cls;
        try {
            cls = ch.getCharacterClass();
        } catch (Throwable t) {
            return;
        }
        if (cls == null || cls.isBlank()) {
            return;
        }
        ACTIVE.put(player.m_20148_(), new Session(cls, System.currentTimeMillis()));
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            ACTIVE.remove(player.m_20148_());
        }
    }

    public static String lockedClass(ServerPlayer player) {
        if (player == null) {
            return null;
        }
        Session s = ACTIVE.get(player.m_20148_());
        if (s == null) {
            return null;
        }
        if (System.currentTimeMillis() - s.startedAt > SESSION_MS) {
            ACTIVE.remove(player.m_20148_());
            return null;
        }
        return s.className;
    }

    public static void applyPacketClassLock(UpdateCharacterC2S packet, ServerPlayer player) {
        String locked = lockedClass(player);
        if (locked == null || packet == null) {
            return;
        }
        try {
            Field field = UpdateCharacterC2S.class.getDeclaredField("className");
            field.setAccessible(true);
            field.set(packet, locked);
        } catch (Throwable ignored) {
        }
    }

    public static void enforceOnCharacter(ServerPlayer player) {
        String locked = lockedClass(player);
        if (locked == null || player == null) {
            return;
        }
        Character ch = DmzProgression.character(player);
        if (ch == null) {
            return;
        }
        try {
            String now = ch.getCharacterClass();
            if (now != null && now.equals(locked)) {
                return;
            }
            ch.setCharacterClass(locked);
            NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
    }

    private record Session(String className, long startedAt) {}
}
