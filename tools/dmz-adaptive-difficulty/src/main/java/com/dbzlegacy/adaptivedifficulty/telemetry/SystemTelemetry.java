package com.dbzlegacy.adaptivedifficulty.telemetry;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.ConfigPaths;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.server.level.ServerPlayer;

/**
 * Unified system event log for difficulty / rival / sparring (and related).
 * Writes {@code config/legacymechanics/telemetry/systems-YYYY-MM-DD.jsonl}.
 * When enabled, logs all players (rate-limited). Balance hit telemetry also logs all AD players.
 */
public final class SystemTelemetry {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Map<UUID, RateWindow> RATE = new ConcurrentHashMap<>();
    private static final AtomicLong WRITTEN = new AtomicLong();
    private static final AtomicLong DROPPED = new AtomicLong();
    private static final Object WRITE_LOCK = new Object();
    private static volatile BufferedWriter writer;
    private static volatile String writerDay = "";

    private SystemTelemetry() {}

    public static boolean isEnabled() {
        DifficultyConfig cfg = DifficultyConfig.get();
        return cfg != null && cfg.enableSystemTelemetry;
    }

    public static void setEnabled(boolean on) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg == null) {
            return;
        }
        cfg.enableSystemTelemetry = on;
        DifficultyConfig.save();
        if (!on) {
            flushAndClose();
        }
    }

    public static Path telemetryDir() {
        return ConfigPaths.telemetryDir();
    }

    public static void log(
            String system,
            String event,
            ServerPlayer actor,
            ServerPlayer other,
            Map<String, ?> fields
    ) {
        if (!isEnabled() || system == null || event == null) {
            return;
        }
        UUID rateId = actor != null ? actor.m_20148_()
                : (other != null ? other.m_20148_() : new UUID(0L, 0L));
        if (!allowRate(rateId)) {
            DROPPED.incrementAndGet();
            return;
        }
        try {
            StringBuilder sb = new StringBuilder(280);
            sb.append('{')
                    .append("\"ts\":\"").append(Instant.now()).append('"')
                    .append(",\"system\":\"").append(escape(system)).append('"')
                    .append(",\"event\":\"").append(escape(event)).append('"');
            if (actor != null) {
                sb.append(",\"actor\":\"").append(escape(actor.m_6302_())).append('"')
                        .append(",\"actorUuid\":\"").append(actor.m_20148_()).append('"');
            }
            if (other != null) {
                sb.append(",\"other\":\"").append(escape(other.m_6302_())).append('"')
                        .append(",\"otherUuid\":\"").append(other.m_20148_()).append('"');
            }
            if (fields != null) {
                for (Map.Entry<String, ?> e : fields.entrySet()) {
                    if (e.getKey() == null || e.getValue() == null) {
                        continue;
                    }
                    sb.append(',').append('"').append(escape(e.getKey())).append('"').append(':');
                    Object v = e.getValue();
                    if (v instanceof Number || v instanceof Boolean) {
                        sb.append(v);
                    } else {
                        sb.append('"').append(escape(String.valueOf(v))).append('"');
                    }
                }
            }
            sb.append('}').append('\n');
            writeLine(sb.toString());
            WRITTEN.incrementAndGet();
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] system telemetry skip: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public static void log(String system, String event, ServerPlayer actor, ServerPlayer other) {
        log(system, event, actor, other, null);
    }

    public static void log(String system, String event, ServerPlayer actor) {
        log(system, event, actor, null, null);
    }

    public static Map<String, Object> fields(Object... kv) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (kv == null) {
            return map;
        }
        for (int i = 0; i + 1 < kv.length; i += 2) {
            if (kv[i] != null) {
                map.put(String.valueOf(kv[i]), kv[i + 1]);
            }
        }
        return map;
    }

    public static String statusLine() {
        return "syslog=" + (isEnabled() ? "ON" : "OFF")
                + " written=" + WRITTEN.get()
                + " rateDropped=" + DROPPED.get()
                + " dir=" + telemetryDir();
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

    private static boolean allowRate(UUID id) {
        DifficultyConfig cfg = DifficultyConfig.get();
        int maxPerSec = cfg == null ? 20 : Math.max(1, Math.min(60, cfg.systemTelemetryMaxPerSecond));
        long now = System.currentTimeMillis();
        RateWindow w = RATE.computeIfAbsent(id, u -> new RateWindow());
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
                Path file = dir.resolve("systems-" + day + ".jsonl");
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

    private static final class RateWindow {
        long windowStartMs;
        int count;
    }
}
