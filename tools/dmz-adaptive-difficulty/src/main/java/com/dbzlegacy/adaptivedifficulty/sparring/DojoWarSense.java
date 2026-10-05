package com.dbzlegacy.adaptivedifficulty.sparring;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

/**
 * Adds the opposing dojo's online members to Noea's tracking-signature scan
 * while a dojo war is active. Same world only. Positions use Noea's 4-block snap.
 */
public final class DojoWarSense {
    private static final int MAX_SIGNALS = 24;
    private static final UUID NONE = new UUID(0L, 0L);

    private DojoWarSense() {}

    public static void addSignatures(ServerPlayer viewer, CompoundTag packet) {
        if (viewer == null || packet == null) {
            return;
        }
        List<ServerPlayer> foes = DojoRankings.onlineWarOpponents(viewer);
        if (foes.isEmpty()) {
            return;
        }
        ListTag signals = packet.m_128437_("Signals", 10);
        Set<UUID> foeIds = new HashSet<>();
        for (ServerPlayer foe : foes) {
            foeIds.add(foe.m_20148_());
        }
        for (int i = signals.size() - 1; i >= 0; i--) {
            CompoundTag existing = signals.m_128728_(i);
            if (existing != null && existing.m_128403_("Id") && foeIds.contains(existing.m_128342_("Id"))) {
                signals.remove(i);
            }
        }
        while (signals.size() + foes.size() > MAX_SIGNALS && signals.size() > 0) {
            signals.remove(signals.size() - 1);
        }
        ServerPlayer nearest = null;
        double nearestSq = Double.MAX_VALUE;
        for (ServerPlayer foe : foes) {
            if (signals.size() >= MAX_SIGNALS) {
                break;
            }
            signals.add(signature(foe));
            double dx = foe.m_20185_() - viewer.m_20185_();
            double dy = foe.m_20186_() - viewer.m_20186_();
            double dz = foe.m_20189_() - viewer.m_20189_();
            double dist = dx * dx + dy * dy + dz * dz;
            if (dist < nearestSq) {
                nearestSq = dist;
                nearest = foe;
            }
        }
        packet.m_128365_("Signals", signals);
        if (nearest != null && !hasChosenTarget(packet)) {
            packet.m_128362_("Tracked", nearest.m_20148_());
        }
    }

    private static boolean hasChosenTarget(CompoundTag packet) {
        if (!packet.m_128403_("Tracked")) {
            return false;
        }
        UUID tracked = packet.m_128342_("Tracked");
        return tracked != null && !NONE.equals(tracked);
    }

    private static CompoundTag signature(ServerPlayer foe) {
        CompoundTag tag = new CompoundTag();
        tag.m_128362_("Id", foe.m_20148_());
        tag.m_128359_("Name", name(foe));
        tag.m_128405_("Tier", 2);
        tag.m_128347_("Radius", 8.0);
        tag.m_128347_("X", snap(foe.m_20185_()));
        tag.m_128347_("Y", snap(foe.m_20186_()));
        tag.m_128347_("Z", snap(foe.m_20189_()));
        return tag;
    }

    private static String name(ServerPlayer foe) {
        String raw = foe.m_7755_() == null ? "War member" : foe.m_7755_().getString();
        if (raw == null || raw.isBlank()) {
            raw = "War member";
        }
        raw = raw.replaceAll("§.", "").trim();
        if (raw.length() > 48) {
            raw = raw.substring(0, 48);
        }
        return "War " + raw;
    }

    private static double snap(double value) {
        return Math.rint(value / 4.0) * 4.0;
    }
}
