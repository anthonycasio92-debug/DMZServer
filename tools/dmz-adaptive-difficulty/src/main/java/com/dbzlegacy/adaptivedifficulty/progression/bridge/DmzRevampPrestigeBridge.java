package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem;
import com.dragonminez.common.stats.StatsData;
import java.lang.reflect.Method;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Keeps dmzrevamp Overhaul {@link com.dmzrevamp.revamp.prestige.PrestigeSystem} prestige
 * count aligned with Legacy Mechanics lifetime completed prestiges (statistics UI only).
 * Level/stat caps come from {@link com.dbzlegacy.adaptivedifficulty.mixin.DmzRevampPrestigeCapMixin}.
 */
public final class DmzRevampPrestigeBridge {
    private static final String REVAMP_MOD = "dmzrevamp";
    private static final String PRESTIGE_SYSTEM = "com.dmzrevamp.revamp.prestige.PrestigeSystem";

    private static volatile Boolean revampLoaded;
    private static volatile Method setCount;
    private static volatile Method readCount;

    private DmzRevampPrestigeBridge() {}

    public static boolean revampPresent() {
        if (revampLoaded == null) {
            revampLoaded = ModList.get().isLoaded(REVAMP_MOD);
        }
        return revampLoaded;
    }

    /**
     * Push LM lifetime completed → Overhaul prestige count on {@link StatsData}.
     */
    public static void syncFromLegacy(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enablePrestigeSystem || !revampPresent()) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        int completed = Math.max(0, PrestigeSystem.getCompleted(player));
        if (!ensureMethods()) {
            return;
        }
        try {
            int current = overhaulCount(data);
            if (current == completed) {
                return;
            }
            setCount.invoke(null, data, completed);
            try {
                DmzSkillUtil.sync(player);
            } catch (Throwable ignored) {
            }
            FabledBridge.logSync(player, "overhaul_prestige_sync", "count", completed, "was", current);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] overhaul prestige sync soft-fail: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    public static int overhaulCount(StatsData data) {
        if (data == null || !ensureMethods()) {
            return 0;
        }
        try {
            Object v = readCount.invoke(null, data);
            if (v instanceof Integer i) {
                return Math.max(0, i);
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    private static boolean ensureMethods() {
        if (setCount != null && readCount != null) {
            return true;
        }
        if (!revampPresent()) {
            return false;
        }
        try {
            Class<?> cls = Class.forName(PRESTIGE_SYSTEM);
            setCount = cls.getMethod("setCount", StatsData.class, int.class);
            readCount = cls.getMethod("count", StatsData.class);
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] dmzrevamp PrestigeSystem reflection unavailable: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    t.toString());
            return false;
        }
    }
}
