package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;

/** Master on/off for the whole Adaptive Difficulty system. */
public final class SystemGate {
    private SystemGate() {}

    public static boolean isEnabled() {
        return DifficultyConfig.isEnabled();
    }

    public static boolean isDisabled() {
        return !isEnabled();
    }
}
