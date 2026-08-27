package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code Universal Fabled Value Cleaner.js} —
 * rewrite scientific-notation persistent values to plain strings.
 */
public final class ValueCleaner {
    private static final boolean ROUND_TO_WHOLE = false;

    private ValueCleaner() {}

    @SuppressWarnings("unchecked")
    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableValueCleaner) {
            return;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return;
        }
        Map<String, ?> all;
        try {
            Object raw = data.getClass().getMethod("getAllPersistentData").invoke(data);
            if (!(raw instanceof Map<?, ?> map)) {
                return;
            }
            all = (Map<String, ?>) map;
        } catch (Throwable t) {
            return;
        }
        if (all.isEmpty()) {
            return;
        }

        int changed = 0;
        for (Map.Entry<String, ?> entry : all.entrySet()) {
            if (entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            String rawText = String.valueOf(entry.getValue());
            try {
                BigDecimal bd = new BigDecimal(rawText);
                if (ROUND_TO_WHOLE) {
                    bd = bd.setScale(0, RoundingMode.DOWN);
                }
                String clean = bd.toPlainString();
                if (!clean.equals(rawText)) {
                    data.getClass()
                            .getMethod("setPersistentData", String.class, Object.class)
                            .invoke(data, entry.getKey(), clean);
                    changed++;
                }
            } catch (Throwable ignored) {
                // Non-numeric values left alone
            }
        }
        if (changed > 0) {
            FabledBridge.logSync(player, "value_clean", "changed", changed);
        }
    }
}
