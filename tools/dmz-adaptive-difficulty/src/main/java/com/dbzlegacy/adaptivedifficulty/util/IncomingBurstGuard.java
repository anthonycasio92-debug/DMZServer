package com.dbzlegacy.adaptivedifficulty.util;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/**
 * Caps stacked AD opener damage from a single mob in a short rolling window.
 * <p>
 * T6 kits (shock + slam + melee / ki) each get a landing-floor fill independently,
 * so one mob's first second could dump 1.2–2.0× bag while later singles sat ~0.4×.
 * Budget is <b>per attacker</b> so a pack of five still presses; only same-mob
 * multi-hit openers are flattened.
 */
public final class IncomingBurstGuard {
    private static final long WINDOW_MS = 1000L;
    /** player → (attacker → samples) */
    private static final Map<UUID, Map<UUID, ArrayDeque<Sample>>> HITS = new ConcurrentHashMap<>();

    private IncomingBurstGuard() {}

    private record Sample(long atMs, float amount) {}

    /**
     * Clamp {@code proposed} so this attacker's recent + this hit ≤ {@code budgetFrac × bag}.
     * Hard cap — high natural soft-cap hits must not bypass the budget. When spent,
     * leave only a small crumb (≤10% bag) so the fight never goes fully toothless.
     */
    public static float clamp(
            ServerPlayer player,
            Mob attacker,
            float proposed,
            float naturalFloor,
            double bag,
            double budgetFrac) {
        if (player == null || attacker == null || proposed <= 0.0f || bag <= 0.0) {
            return proposed;
        }
        float budget = (float) (Math.max(20.0, bag) * Math.max(0.20, Math.min(1.50, budgetFrac)));
        float recent = recentDamage(player, attacker);
        float remaining = budget - recent;
        float crumb = (float) (Math.max(20.0, bag) * 0.08);
        if (remaining <= 0.0f) {
            // Budget spent — crumb only (natural soft-cap hits used to bypass here).
            float floor = Math.max(0.0f, naturalFloor);
            return Math.min(proposed, Math.min(crumb, Math.max(floor, crumb * 0.5f)));
        }
        return Math.min(proposed, remaining);
    }

    public static void record(ServerPlayer player, Mob attacker, float applied) {
        if (player == null || attacker == null || applied <= 0.0f) {
            return;
        }
        long now = System.currentTimeMillis();
        Map<UUID, ArrayDeque<Sample>> byAtk =
                HITS.computeIfAbsent(player.m_20148_(), u -> new ConcurrentHashMap<>());
        ArrayDeque<Sample> q = byAtk.computeIfAbsent(attacker.m_20148_(), u -> new ArrayDeque<>());
        prune(q, now);
        q.addLast(new Sample(now, applied));
        while (q.size() > 32) {
            q.removeFirst();
        }
    }

    public static float recentDamage(ServerPlayer player, Mob attacker) {
        if (player == null || attacker == null) {
            return 0.0f;
        }
        Map<UUID, ArrayDeque<Sample>> byAtk = HITS.get(player.m_20148_());
        if (byAtk == null) {
            return 0.0f;
        }
        ArrayDeque<Sample> q = byAtk.get(attacker.m_20148_());
        if (q == null || q.isEmpty()) {
            return 0.0f;
        }
        long now = System.currentTimeMillis();
        prune(q, now);
        float sum = 0.0f;
        for (Sample s : q) {
            sum += s.amount;
        }
        return sum;
    }

    public static void clear(ServerPlayer player) {
        if (player != null) {
            HITS.remove(player.m_20148_());
        }
    }

    public static void clearAll() {
        HITS.clear();
    }

    /**
     * Per-attacker budgets ≈ one soft-cap bite (+ a sliver). Stops shock+slam+melee
     * from each claiming a full landing floor in the same second.
     */
    public static double budgetFrac(int activeTier) {
        // Sized to clip 1.2–2.0× opener stacks without starving the next melee
        // swing (~0.5–0.7s later). T6 land≈0.48 → budget 0.80 leaves room for
        // one follow-up while still cutting triple landing-fills.
        return switch (activeTier) {
            case 7 -> 0.88;
            case 6 -> 0.80;
            case 5 -> 0.75;
            case 4 -> 0.70;
            case 3 -> 0.75;
            default -> 0.80;
        };
    }

    private static void prune(ArrayDeque<Sample> q, long now) {
        long cutoff = now - WINDOW_MS;
        Iterator<Sample> it = q.iterator();
        while (it.hasNext()) {
            if (it.next().atMs < cutoff) {
                it.remove();
            } else {
                break;
            }
        }
    }
}
