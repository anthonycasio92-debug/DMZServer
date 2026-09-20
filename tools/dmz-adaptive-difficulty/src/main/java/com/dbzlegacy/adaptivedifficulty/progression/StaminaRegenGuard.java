package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Resources;
import net.minecraft.server.level.ServerPlayer;

/**
 * DMZ stamina/ki regen is skipped while {@link Cooldowns#STAMINA_PAUSE} or drain cooldowns are active.
 * On Mohist, cooldown decay often stalls until relog.
 */
public final class StaminaRegenGuard {
    /** Dash sets pause to ~20 ticks — clear once dash is over. */
    private static final int STALE_PAUSE_TICKS = 40;
    private static final int STALE_DRAIN_TICKS = 80;

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

        boolean dashing = cds.hasCooldown(Cooldowns.DASH_ACTIVE);
        if (!dashing && cds.hasCooldown(Cooldowns.STAMINA_PAUSE)) {
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

        Resources res = data.getResources();
        if (res != null) {
            try {
                float maxS = data.getMaxStamina();
                float curS = res.getCurrentStamina();
                if (maxS > 1f && curS >= 0f && curS < maxS && !dashing
                        && !cds.hasCooldown(Cooldowns.STAMINA_PAUSE)) {
                    // Nudge client if pools look frozen at 0 while caps are healthy.
                    if (curS <= 0.01f) {
                        res.setCurrentStamina(Math.min(maxS, maxS * 0.02f));
                    }
                }
            } catch (Throwable ignored) {
            }
        }
    }
}
