package com.dbzlegacy.adaptivedifficulty.progression.tp;

import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.init.MainEffects;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Port of Global TP Boost.js + TP boost end.js (triggers 30/31).
 */
public final class GlobalTpBoost {
    private static final int ENCODED_MINUTE_DIVISOR = 10_000;
    private static final int EFFECT_REFRESH_BUFFER_TICKS = 100;
    private static final long TRIGGER_LOCK_MS = 3000L;

    private static final AtomicBoolean ACTIVE = new AtomicBoolean(false);
    private static final AtomicReference<Double> MULTIPLIER = new AtomicReference<>(0.0);
    private static final AtomicInteger AMPLIFIER = new AtomicInteger(-1);
    private static final AtomicLong END_TIME = new AtomicLong(0L);
    private static final AtomicReference<String> PURCHASER = new AtomicReference<>("");
    private static final AtomicLong TRIGGER_LOCK = new AtomicLong(0L);
    private static final AtomicReference<String> TRIGGER_SIGNATURE = new AtomicReference<>("");

    private GlobalTpBoost() {}

    public static void pulse(MinecraftServer server, int tick) {
        if (!ProgressionConfig.globalTpBoost() || server == null) {
            return;
        }
        if (tick % 20 != 0) {
            return;
        }
        try {
            long now = System.currentTimeMillis();
            if (!ACTIVE.get()) {
                return;
            }
            long end = END_TIME.get();
            if (end <= now) {
                endBoost(true);
                return;
            }
            int amp = AMPLIFIER.get();
            if (amp < 0) {
                return;
            }
            long remainingMs = end - now;
            int durationTicks = Math.max(
                    EFFECT_REFRESH_BUFFER_TICKS,
                    (int) Math.ceil(remainingMs / 50.0) + EFFECT_REFRESH_BUFFER_TICKS
            );
            for (ServerPlayer player : server.m_6846_().m_11314_()) {
                applyEffect(player, amp, durationTicks);
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onLogin(ServerPlayer player) {
        if (!ProgressionConfig.globalTpBoost() || player == null || !ACTIVE.get()) {
            return;
        }
        long remaining = END_TIME.get() - System.currentTimeMillis();
        if (remaining <= 0L) {
            return;
        }
        int amp = AMPLIFIER.get();
        if (amp < 0) {
            return;
        }
        int ticks = Math.max(
                EFFECT_REFRESH_BUFFER_TICKS,
                (int) Math.ceil(remaining / 50.0) + EFFECT_REFRESH_BUFFER_TICKS
        );
        applyEffect(player, amp, ticks);
    }

    /** Trigger 30 / {@code /progression boost start}. */
    public static String startBoost(ServerPlayer actor, int encoded, String purchaser) {
        if (!ProgressionConfig.globalTpBoost()) {
            return "§cGlobal TP Boost is disabled.";
        }
        Decoded decoded = decode(encoded);
        if (decoded == null) {
            return "§cInvalid encoded boost value: " + encoded;
        }
        long now = System.currentTimeMillis();
        String sig = (actor == null ? "console" : actor.m_7755_().getString().toLowerCase())
                + "|" + decoded.encoded + "|"
                + (purchaser == null ? "" : purchaser.toLowerCase());
        if (now < TRIGGER_LOCK.get() && sig.equals(TRIGGER_SIGNATURE.get())) {
            return "§7Boost trigger locked (duplicate).";
        }
        TRIGGER_LOCK.set(now + TRIGGER_LOCK_MS);
        TRIGGER_SIGNATURE.set(sig);
        String who = purchaser == null || purchaser.isBlank()
                ? (actor == null ? "Server" : actor.m_7755_().getString())
                : purchaser;
        String result = activate(decoded.multiplier, decoded.durationMinutes, who);
        SystemTelemetry.log("progression", "tp_boost_start", actor, null,
                Map.of("mult", decoded.multiplier, "minutes", decoded.durationMinutes, "purchaser", who));
        return result;
    }

    public static String startBoostMinutes(ServerPlayer actor, double multiplier, int minutes, String purchaser) {
        if (!ProgressionConfig.globalTpBoost()) {
            return "§cGlobal TP Boost is disabled.";
        }
        String who = purchaser == null || purchaser.isBlank()
                ? (actor == null ? "Server" : actor.m_7755_().getString())
                : purchaser;
        String result = activate(multiplier, minutes, who);
        SystemTelemetry.log("progression", "tp_boost_start", actor, null,
                Map.of("mult", multiplier, "minutes", minutes, "purchaser", who));
        return result;
    }

    /** Trigger 31 / {@code /progression boost end}. */
    public static String endBoost(boolean announce) {
        if (!ACTIVE.get() && AMPLIFIER.get() < 0) {
            return "§7No global TP boost is active.";
        }
        clear();
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            for (ServerPlayer player : server.m_6846_().m_11314_()) {
                removeEffect(player);
            }
            if (announce) {
                broadcast("§6§lGLOBAL TP BOOST §r§7- §cThe global TP boost has ended.");
            }
        }
        SystemTelemetry.log("progression", "tp_boost_end", null, null, Map.of());
        return "§aGlobal TP boost ended.";
    }

    public static String statusLine() {
        if (!ACTIVE.get()) {
            return "§7Global TP boost: §cOFF";
        }
        long rem = Math.max(0L, END_TIME.get() - System.currentTimeMillis());
        return "§7Global TP boost: §a" + formatMult(MULTIPLIER.get())
                + "x §7(" + (rem / 60000L) + "m left, by §f" + PURCHASER.get() + "§7)";
    }

    private static String activate(double multiplier, int durationMinutes, String purchaser) {
        int amplifier = multiplierToAmplifier(multiplier);
        if (amplifier < 0) {
            return "§cMultiplier must be 1.25+ in 0.25 steps.";
        }
        if (durationMinutes <= 0) {
            return "§cDuration must be > 0 minutes.";
        }
        long now = System.currentTimeMillis();
        long durationMs = durationMinutes * 60_000L;
        long currentEnd = END_TIME.get();
        boolean extending = ACTIVE.get() && currentEnd > now;
        long newEnd = extending ? Math.max(now, currentEnd) + durationMs : now + durationMs;

        ACTIVE.set(true);
        MULTIPLIER.set(multiplier);
        AMPLIFIER.set(amplifier);
        END_TIME.set(newEnd);
        PURCHASER.set(purchaser == null ? "" : purchaser);

        applyToAll(amplifier, newEnd - now);
        broadcast("§6§lGLOBAL TP BOOST ACTIVATED!");
        broadcast("§e" + purchaser + " §7activated a §a" + formatMult(multiplier)
                + "x TP Boost§7!");
        broadcast("§7All online players receive boosted TP for §f"
                + formatDuration(newEnd - now) + "§7.");
        return "§a" + formatMult(multiplier) + "x TP activated for "
                + formatDuration(durationMinutes * 60_000L) + ".";
    }

    private static void applyToAll(int amplifier, long remainingMs) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        int ticks = Math.max(
                EFFECT_REFRESH_BUFFER_TICKS,
                (int) Math.ceil(remainingMs / 50.0) + EFFECT_REFRESH_BUFFER_TICKS
        );
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            applyEffect(player, amplifier, ticks);
        }
    }

    private static void applyEffect(ServerPlayer player, int amplifier, int durationTicks) {
        try {
            MobEffect effect = MainEffects.TP_GAIN.get();
            if (effect == null || player == null) {
                return;
            }
            player.m_7292_(new MobEffectInstance(
                    effect,
                    Math.max(1, durationTicks),
                    Math.max(0, amplifier),
                    false,
                    true,
                    true
            ));
        } catch (Throwable ignored) {
        }
    }

    private static void removeEffect(ServerPlayer player) {
        try {
            MobEffect effect = MainEffects.TP_GAIN.get();
            if (effect != null && player != null) {
                player.m_21195_(effect);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void clear() {
        ACTIVE.set(false);
        MULTIPLIER.set(0.0);
        AMPLIFIER.set(-1);
        END_TIME.set(0L);
        PURCHASER.set("");
    }

    private static Decoded decode(int encoded) {
        if (encoded <= 0) {
            return null;
        }
        int multiplierCode = encoded / ENCODED_MINUTE_DIVISOR;
        int durationMinutes = encoded % ENCODED_MINUTE_DIVISOR;
        double multiplier = multiplierCode / 100.0;
        if (multiplier <= 1.0 || durationMinutes <= 0) {
            return null;
        }
        return new Decoded(encoded, multiplier, durationMinutes);
    }

    private static int multiplierToAmplifier(double multiplier) {
        if (!(multiplier >= 1.25)) {
            return -1;
        }
        int amplifier = (int) Math.round(((multiplier - 1.0) / 0.25) - 1.0);
        double confirmed = 1.0 + ((amplifier + 1) * 0.25);
        if (Math.abs(confirmed - multiplier) > 0.0001 || amplifier < 0) {
            return -1;
        }
        return amplifier;
    }

    private static void broadcast(String msg) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            return;
        }
        for (ServerPlayer player : server.m_6846_().m_11314_()) {
            DmzRewards.msg(player, msg);
        }
    }

    private static String formatMult(double m) {
        if (Math.abs(m - Math.rint(m)) < 0.001) {
            return Integer.toString((int) Math.rint(m));
        }
        return String.format(java.util.Locale.ROOT, "%.2f", m);
    }

    private static String formatDuration(long ms) {
        long minutes = Math.max(1L, ms / 60_000L);
        if (minutes < 60) {
            return minutes + "m";
        }
        long hours = minutes / 60;
        long rem = minutes % 60;
        return hours + "h" + (rem > 0 ? " " + rem + "m" : "");
    }

    private record Decoded(int encoded, double multiplier, int durationMinutes) {}
}
