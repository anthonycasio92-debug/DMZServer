package com.dbzlegacy.mohistmelee;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class RateLog {
    public static final String MOD_ID = "dmz_mohist_melee_fix";
    private static final Logger LOGGER = LogManager.getLogger((String)"dmz_mohist_melee_fix");
    private static final ConcurrentHashMap<String, AtomicInteger> COUNTS = new ConcurrentHashMap();

    private RateLog() {
    }

    public static Logger logger() {
        return LOGGER;
    }

    private static boolean under(String key, int cap) {
        return COUNTS.computeIfAbsent(key, k -> new AtomicInteger()).incrementAndGet() <= cap;
    }

    public static void info(String key, int cap, String format, Object ... args) {
        if (RateLog.under(key, cap)) {
            LOGGER.info("[dmz_mohist_melee_fix] " + format, args);
        }
    }

    public static void warn(String key, int cap, String format, Object ... args) {
        if (RateLog.under(key, cap)) {
            LOGGER.warn("[dmz_mohist_melee_fix] " + format, args);
        }
    }
}

