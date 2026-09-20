package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Cooldowns;
import net.minecraft.server.level.ServerPlayer;

/**
 * DMZ stamina regen is skipped while {@link Cooldowns#STAMINA_PAUSE} is active (1s after dash).
 * On some Mohist builds cooldown decay stalls and players never regen stamina while ki still does.
 */
public final class StaminaRegenGuard {
    /** Dash sets pause to 20 ticks — allow a little slack, then force-clear if dash is over. */
    private static final int STALE_PAUSE_TICKS = 40;

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
        if (cds == null || !cds.hasCooldown(Cooldowns.STAMINA_PAUSE)) {
            return;
        }
        if (cds.hasCooldown(Cooldowns.DASH_ACTIVE)) {
            return;
        }
        int pause = cds.getCooldown(Cooldowns.STAMINA_PAUSE);
        if (pause > STALE_PAUSE_TICKS) {
            // Corrupt / stuck NBT — clear immediately.
            cds.removeCooldown(Cooldowns.STAMINA_PAUSE);
            return;
        }
        // Mohist: pause often never ticks down — clear once dash animation ended.
        cds.removeCooldown(Cooldowns.STAMINA_PAUSE);
    }
}
