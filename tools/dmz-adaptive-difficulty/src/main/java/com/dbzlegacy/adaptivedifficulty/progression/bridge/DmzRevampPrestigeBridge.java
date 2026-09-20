package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.DmzResourcePoolClamp;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import java.lang.reflect.Method;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;

/**
 * Keeps dmzrevamp Overhaul {@link com.dmzrevamp.revamp.prestige.PrestigeSystem} prestige
 * count aligned with Legacy Mechanics <b>held</b> prestiges (wallet / scale), not lifetime completed.
 * Playable stat totals come from {@link com.dbzlegacy.adaptivedifficulty.mixin.DmzRevampPrestigeCapMixin}.
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
     * {@code dmzstats reset} / {@link StatsData#resetPlayerProgress} clears Overhaul's
     * prestige counter on {@link StatsData}; re-apply LM held after rebuild.
     */
    public static void scheduleSyncAfterStatsReset(ServerPlayer player) {
        if (player == null) {
            return;
        }
        syncFromLegacy(player);
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return;
        }
        UUID id = player.m_20148_();
        server.execute(() -> {
            ServerPlayer p = server.m_6846_().m_11259_(id);
            if (p != null && p.m_6084_()) {
                syncFromLegacy(p);
            }
        });
        for (int delay : new int[] {5, 20, 40, 80, 160, 300, 600}) {
            final int ticks = delay;
            try {
                server.m_6937_(new TickTask(server.m_129921_() + ticks, () -> {
                    ServerPlayer p = server.m_6846_().m_11259_(id);
                    if (p != null && p.m_6084_()) {
                        syncFromLegacy(p);
                    }
                }));
            } catch (Throwable ignored) {
            }
        }
    }

    public static void syncFromLegacy(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enablePrestigeSystem || !revampPresent()) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        int held = Math.max(0, Math.min(
                com.dbzlegacy.adaptivedifficulty.progression.LmOverhaulPrestigeIntegration.OVERHAUL_MAX_PRESTIGE,
                PrestigeSystem.getHeld(player)));
        if (!ensureMethods()) {
            return;
        }
        try {
            int current = overhaulCount(data);
            if (current == held) {
                try {
                    OverhaulPrestigeResourceScale.pulse(player);
                } catch (Throwable ignored) {
                }
                return;
            }
            float maxEBefore = data.getMaxEnergy();
            float maxSBefore = data.getMaxStamina();
            setCount.invoke(null, data, held);
            try {
                OverhaulPrestigeResourceScale.afterSetCount(player, data, maxEBefore, maxSBefore);
            } catch (Throwable ignored) {
            }
            try {
                refillPoolsLikeOverhaulPrestige(player, data, maxEBefore, maxSBefore, held);
            } catch (Throwable ignored) {
            }
            try {
                DmzSkillUtil.sync(player);
            } catch (Throwable ignored) {
            }
            FabledBridge.logSync(player, "overhaul_prestige_sync", "count", held, "was", current);
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

    /**
     * Native Overhaul {@code PrestigeService} refills ki/stamina after prestige; LM only syncs count.
     */
    private static void refillPoolsLikeOverhaulPrestige(
            ServerPlayer player,
            StatsData data,
            float maxEnergyBefore,
            float maxStaminaBefore,
            int prestigeCount
    ) {
        if (player == null || data == null || prestigeCount <= 0) {
            return;
        }
        Resources res = data.getResources();
        if (res == null) {
            return;
        }
        float maxE = DmzResourcePoolClamp.displayMaxEnergy(data);
        float maxS = DmzResourcePoolClamp.displayMaxStamina(data);
        float curE = res.getCurrentEnergy();
        float curS = res.getCurrentStamina();
        boolean changed = false;
        if (maxE > 1f && (curE >= maxEnergyBefore * 0.97f || curE + 0.5f < maxE * 0.55f)) {
            res.setCurrentEnergy(maxE);
            changed = true;
        }
        if (maxS > 1f && (curS >= maxStaminaBefore * 0.97f || curS + 0.5f < maxS * 0.55f)) {
            res.setCurrentStamina(maxS);
            changed = true;
        }
        if (changed) {
            try {
                OverhaulPrestigeResourceScale.pulse(player);
            } catch (Throwable ignored) {
            }
            try {
                EnergyManaSync.sync(player, true);
            } catch (Throwable ignored) {
            }
        }
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
