package com.dbzlegacy.adaptivedifficulty.rival;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** Active official rival challenge fight state. */
public final class RivalChallenge {
    public enum Phase {
        PENDING,
        COUNTDOWN,
        ACTIVE,
        ENDED
    }

    public String id = "";
    public UUID a;
    public UUID b;
    public String nameA = "";
    public String nameB = "";
    public long startAt;
    public long endsAt;
    public long countdownUntil;
    public long durationMs = RivalConstants.CH_DURATION_MS;
    public double damageA;
    public double damageB;
    public boolean kiA;
    public boolean kiB;
    public Phase status = Phase.PENDING;
    public UUID winner;
    public UUID loser;
    public boolean knockout;
    public String endReason = "";
    public long lastBroadcastAt;

    public final Map<UUID, Combat> combat = new ConcurrentHashMap<>();

    public static final class Combat {
        public double damage;
        public int hits;
        public boolean usedKi;
    }

    public Combat combatOf(UUID id) {
        return combat.computeIfAbsent(id, u -> new Combat());
    }

    public boolean involves(UUID uuid) {
        return uuid != null && (uuid.equals(a) || uuid.equals(b));
    }

    public UUID other(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        if (uuid.equals(a)) {
            return b;
        }
        if (uuid.equals(b)) {
            return a;
        }
        return null;
    }

    public double damageOf(UUID uuid) {
        if (uuid == null) {
            return 0.0;
        }
        if (uuid.equals(a)) {
            return damageA;
        }
        if (uuid.equals(b)) {
            return damageB;
        }
        Combat c = combat.get(uuid);
        return c == null ? 0.0 : c.damage;
    }

    public void addDamage(UUID attacker, double amount, boolean ki) {
        if (attacker == null || !(amount > 0.0)) {
            return;
        }
        Combat c = combatOf(attacker);
        c.damage += amount;
        c.hits++;
        if (ki) {
            c.usedKi = true;
        }
        if (attacker.equals(a)) {
            damageA += amount;
            if (ki) {
                kiA = true;
            }
        } else if (attacker.equals(b)) {
            damageB += amount;
            if (ki) {
                kiB = true;
            }
        }
    }
}
