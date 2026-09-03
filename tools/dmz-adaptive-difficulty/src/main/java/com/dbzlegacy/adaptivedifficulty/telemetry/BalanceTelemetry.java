package com.dbzlegacy.adaptivedifficulty.telemetry;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.scaling.MobScaling;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Combat telemetry for difficulty balance tuning.
 * <p>
 * When enabled, every AD-painted mob hit on a player actively using the difficulty
 * system is appended as one JSON line under
 * {@code config/legacymechanics/telemetry/hits-YYYY-MM-DD.jsonl}.
 * Capture happens before the DEF-cancel safety net mutates the event so cancelled
 * zeros are still visible in the log. Rate-limited per player
 * ({@code balanceTelemetryMaxPerSecond}).
 */
public final class BalanceTelemetry {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<UUID, RateWindow> RATE = new ConcurrentHashMap<>();
    private static final AtomicLong WRITTEN = new AtomicLong();
    private static final AtomicLong DROPPED = new AtomicLong();
    private static final Object WRITE_LOCK = new Object();
    private static volatile BufferedWriter writer;
    private static volatile String writerDay = "";

    private BalanceTelemetry() {}

    public static boolean isEnabled() {
        DifficultyConfig cfg = DifficultyConfig.get();
        return cfg != null && cfg.balanceTelemetryEnabled;
    }

    public static void setEnabled(boolean on) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg == null) {
            return;
        }
        cfg.balanceTelemetryEnabled = on;
        DifficultyConfig.save();
        if (!on) {
            flushAndClose();
        }
    }

    /** True when this player should be sampled (telemetry on + valid player). */
    public static boolean shouldLog(ServerPlayer player) {
        return isEnabled() && player != null;
    }

    /**
     * Record one AD mob → player hit. Call <b>before</b> mutating cancelled damage.
     *
     * @param preAmount amount on the event before safety-net restore
     * @param wasCancelled true when DMZ/event had zeroed or cancelled the hit
     * @param postAmount amount after safety-net (may equal preAmount)
     */
    public static void logIncomingHit(
            ServerPlayer player,
            Mob mob,
            PlayerCombatProfile profile,
            float preAmount,
            boolean wasCancelled,
            float postAmount
    ) {
        if (!shouldLog(player) || profile == null || !profile.active()) {
            return;
        }
        if (!allowRate(player)) {
            DROPPED.incrementAndGet();
            return;
        }
        try {
            double paintedAtk = 0.0;
            var atkAttr = mob.m_21051_(Attributes.f_22281_); // ATTACK_DAMAGE
            if (atkAttr != null) {
                paintedAtk = atkAttr.m_22115_();
            }
            double liveHp = Math.max(1.0, profile.liveMaxHealth);
            double flat = profile.liveFlatMitigation;
            double thr = PlayerCombatProfile.dmzCancelMitigationThreshold();
            double cancelRatio = paintedAtk > 0.0 ? flat / paintedAtk : 999.0;
            boolean wouldCancel = paintedAtk > 0.0 && flat >= paintedAtk * thr;
            int unlock = MobScaling.unlockTierOf(mob);
            String mobId = mob.m_6095_().toString();
            String line = new StringBuilder(420)
                    .append('{')
                    .append("\"ts\":\"").append(Instant.now()).append('"')
                    .append(",\"player\":\"").append(escape(player.m_6302_())).append('"')
                    .append(",\"uuid\":\"").append(player.m_20148_()).append('"')
                    .append(",\"race\":\"").append(escape(profile.race)).append('"')
                    .append(",\"class\":\"").append(escape(profile.fightingClass)).append('"')
                    .append(",\"android\":").append(
                            com.dbzlegacy.adaptivedifficulty.progression.race.AndroidConversion
                                    .isAndroidUpgraded(player))
                    .append(",\"tier\":").append(profile.activeTier)
                    .append(",\"dmzLevel\":").append(profile.progressionDmzLevel)
                    .append(",\"paintEase\":").append(round3(profile.paintEase))
                    .append(",\"formBoost\":").append(round3(profile.formBoost))
                    .append(",\"kp\":").append(profile.kiProtectionLevel)
                    .append(",\"inf\":").append(profile.kiInfusionLevel)
                    .append(",\"pu\":").append(profile.potentialUnlockLevel)
                    .append(",\"liveHp\":").append(round1(liveHp))
                    .append(",\"flatMit\":").append(round1(flat))
                    .append(",\"paintedAtk\":").append(round1(paintedAtk))
                    .append(",\"cancelThr\":").append(round3(thr))
                    .append(",\"cancelRatio\":").append(round3(cancelRatio))
                    .append(",\"wouldCancel\":").append(wouldCancel)
                    .append(",\"preDmg\":").append(round2(preAmount))
                    .append(",\"cancelled\":").append(wasCancelled)
                    .append(",\"postDmg\":").append(round2(postAmount))
                    .append(",\"landing\":").append(round1(profile.targetLandingDamage(DifficultyConfig.get())))
                    .append(",\"hitFracPre\":").append(round4(preAmount / liveHp))
                    .append(",\"hitFracPost\":").append(round4(postAmount / liveHp))
                    .append(",\"mob\":\"").append(escape(mobId)).append('"')
                    .append(",\"unlock\":").append(unlock)
                    .append(",\"adPainted\":").append(MobScaling.isAdPainted(mob))
                    .append('}')
                    .append('\n')
                    .toString();
            writeLine(line);
            WRITTEN.incrementAndGet();
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug("[{}] telemetry skip: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public static String statusLine() {
        Path dir = telemetryDir();
        return "telemetry=" + (isEnabled() ? "ON" : "OFF")
                + " written=" + WRITTEN.get()
                + " rateDropped=" + DROPPED.get()
                + " dir=" + dir;
    }

    public static Path telemetryDir() {
        return ConfigPaths.telemetryDir();
    }

    public static void flushAndClose() {
        synchronized (WRITE_LOCK) {
            if (writer != null) {
                try {
                    writer.flush();
                    writer.close();
                } catch (IOException ignored) {
                    // ignore
                }
                writer = null;
                writerDay = "";
            }
        }
    }

    /**
     * Write one synthetic probe line so admins can verify the telemetry folder
     * without needing a live fight. Returns the file path written.
     */
    public static String writeTestProbe(String playerName) {
        String name = playerName == null || playerName.isBlank() ? "console" : playerName.trim();
        String line = new StringBuilder(220)
                .append('{')
                .append("\"ts\":\"").append(Instant.now()).append('"')
                .append(",\"player\":\"").append(escape(name)).append('"')
                .append(",\"probe\":true")
                .append(",\"note\":\"telemetry test probe\"")
                .append('}')
                .append('\n')
                .toString();
        try {
            writeLine(line);
            WRITTEN.incrementAndGet();
            flushAndClose();
            String day = LocalDate.now(ZoneOffset.UTC).format(DAY);
            return telemetryDir().resolve("hits-" + day + ".jsonl").toAbsolutePath().toString();
        } catch (IOException e) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] telemetry probe failed: {}", AdaptiveDifficultyMod.MOD_ID, e.toString());
            return "";
        }
    }

    private static boolean allowRate(ServerPlayer player) {
        DifficultyConfig cfg = DifficultyConfig.get();
        int maxPerSec = cfg == null ? 8 : Math.max(1, Math.min(40, cfg.balanceTelemetryMaxPerSecond));
        long now = System.currentTimeMillis();
        RateWindow w = RATE.computeIfAbsent(player.m_20148_(), id -> new RateWindow());
        synchronized (w) {
            if (now - w.windowStartMs >= 1000L) {
                w.windowStartMs = now;
                w.count = 0;
            }
            if (w.count >= maxPerSec) {
                return false;
            }
            w.count++;
            return true;
        }
    }

    private static void writeLine(String line) throws IOException {
        String day = LocalDate.now(ZoneOffset.UTC).format(DAY);
        synchronized (WRITE_LOCK) {
            if (writer == null || !day.equals(writerDay)) {
                if (writer != null) {
                    try {
                        writer.flush();
                        writer.close();
                    } catch (IOException ignored) {
                        // ignore
                    }
                }
                Path dir = telemetryDir();
                Files.createDirectories(dir);
                Path file = dir.resolve("hits-" + day + ".jsonl");
                writer = Files.newBufferedWriter(
                        file,
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE,
                        StandardOpenOption.WRITE,
                        StandardOpenOption.APPEND
                );
                writerDay = day;
            }
            writer.write(line);
            // Flush periodically cheaply — every write is fine for tester volumes.
            if ((WRITTEN.get() & 7L) == 0L) {
                writer.flush();
            }
        }
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }

    private static double round2(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    private static double round3(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }

    private static double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    private static final class RateWindow {
        long windowStartMs;
        int count;
    }
}
