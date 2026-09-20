package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Resources;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * DMZ stamina/ki regen is skipped while {@link Cooldowns#STAMINA_PAUSE} or drain cooldowns are active.
 * On Mohist, cooldown decay often stalls until relog.
 */
public final class StaminaRegenGuard {
    /** Dash sets pause to ~20 ticks — clear once dash is over. */
    private static final int STALE_PAUSE_TICKS = 40;
    private static final int STALE_DRAIN_TICKS = 80;
    private static final int STALL_TICKS_BEFORE_NUDGE = 30;
    private static final int NUDGE_INTERVAL_TICKS = 10;

    private static final Map<UUID, Float> LAST_STAMINA = new ConcurrentHashMap<>();
    private static final Map<UUID, Integer> STALL_STAMINA = new ConcurrentHashMap<>();

    private StaminaRegenGuard() {}

    public static void pulse(ServerPlayer player) {
        if (player == null) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        Cooldowns cds = data.getCooldowns();
        if (cds == null) {
            return;
        }
        try {
            cds.tick();
        } catch (Throwable ignored) {
        }

        Resources res = data.getResources();
        if (res == null) {
            return;
        }

        boolean dashing = cds.hasCooldown(Cooldowns.DASH_ACTIVE);
        float maxSPreview = 0f;
        float curSPreview = 0f;
        try {
            maxSPreview = DmzResourcePoolClamp.displayMaxStamina(data);
            curSPreview = res.getCurrentStamina();
        } catch (Throwable ignored) {
        }
        boolean belowMaxStamina = maxSPreview > 1f && curSPreview < maxSPreview - 0.5f;
        if (belowMaxStamina && !dashing) {
            cds.removeCooldown(Cooldowns.STAMINA_PAUSE);
            cds.removeCooldown(Cooldowns.DRAIN);
            cds.removeCooldown(Cooldowns.DRAIN_ACTIVE);
        } else if (!dashing && cds.hasCooldown(Cooldowns.STAMINA_PAUSE)) {
            int pause = cds.getCooldown(Cooldowns.STAMINA_PAUSE);
            if (pause > STALE_PAUSE_TICKS || pause > 0) {
                cds.removeCooldown(Cooldowns.STAMINA_PAUSE);
            }
        }

        if (cds.hasCooldown(Cooldowns.DRAIN_ACTIVE)) {
            int drain = cds.getCooldown(Cooldowns.DRAIN_ACTIVE);
            if (drain > STALE_DRAIN_TICKS) {
                cds.removeCooldown(Cooldowns.DRAIN_ACTIVE);
            }
        }
        if (cds.hasCooldown(Cooldowns.DRAIN)) {
            int drain = cds.getCooldown(Cooldowns.DRAIN);
            if (drain > STALE_DRAIN_TICKS * 3) {
                cds.removeCooldown(Cooldowns.DRAIN);
            }
        }

        UUID id = player.m_20148_();
        try {
            float maxS = DmzResourcePoolClamp.displayMaxStamina(data);
            float curS = res.getCurrentStamina();
            if (maxS > 1f && curS >= 0f && curS < maxS && !dashing
                    && !cds.hasCooldown(Cooldowns.STAMINA_PAUSE)) {
                if (curS <= 0.01f) {
                    res.setCurrentStamina(Math.min(maxS, maxS * 0.02f));
                } else {
                    nudgeIfStalled(id, curS, maxS, res, cds, dashing);
                }
                // Mohist often skips stamina regen while drain flags linger after prestige.
                if (cds.hasCooldown(Cooldowns.DRAIN) || cds.hasCooldown(Cooldowns.DRAIN_ACTIVE)) {
                    int d = Math.max(cds.getCooldown(Cooldowns.DRAIN), cds.getCooldown(Cooldowns.DRAIN_ACTIVE));
                    if (d > 0 && d < STALE_DRAIN_TICKS * 4 && curS < maxS - 0.5f) {
                        cds.removeCooldown(Cooldowns.DRAIN);
                        cds.removeCooldown(Cooldowns.DRAIN_ACTIVE);
                    }
                }
            } else {
                clearStall(id);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void nudgeIfStalled(
            UUID id,
            float cur,
            float max,
            Resources res,
            Cooldowns cds,
            boolean dashing
    ) {
        if (dashing || cds.hasCooldown(Cooldowns.STAMINA_PAUSE)) {
            clearStall(id);
            return;
        }
        float prev = LAST_STAMINA.getOrDefault(id, -1f);
        float now = res.getCurrentStamina();
        if (prev >= 0f && Math.abs(now - prev) < 0.08f) {
            int stall = STALL_STAMINA.merge(id, 1, (a, b) -> a + b);
            if (stall >= STALL_TICKS_BEFORE_NUDGE && stall % NUDGE_INTERVAL_TICKS == 0) {
                float step = Math.max(0.5f, max * 0.015f);
                float gap = max - now;
                step = Math.min(step, gap);
                if (step > 0.01f) {
                    res.addStamina(step);
                }
            }
        } else {
            STALL_STAMINA.put(id, 0);
        }
        LAST_STAMINA.put(id, res.getCurrentStamina());
    }

    private static void clearStall(UUID id) {
        LAST_STAMINA.remove(id);
        STALL_STAMINA.remove(id);
    }
}
