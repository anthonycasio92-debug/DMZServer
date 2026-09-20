package com.dbzlegacy.adaptivedifficulty.progression;

import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;

/** Keep live ki/stamina at or below {@link StatsData#getMaxEnergy()} / {@link StatsData#getMaxStamina()}. */
public final class DmzResourcePoolClamp {
    private static final float EPS = 0.08f;

    private DmzResourcePoolClamp() {}

    /** @return true if either pool was lowered */
    public static boolean clamp(StatsData data) {
        if (data == null) {
            return false;
        }
        Resources res = data.getResources();
        if (res == null) {
            return false;
        }
        boolean changed = false;
        try {
            float maxE = data.getMaxEnergy();
            if (Float.isFinite(maxE) && maxE > 0f) {
                float curE = res.getCurrentEnergy();
                if (Float.isFinite(curE) && curE > maxE + EPS) {
                    res.setCurrentEnergy(maxE);
                    changed = true;
                }
            }
        } catch (Throwable ignored) {
        }
        try {
            float maxS = data.getMaxStamina();
            if (Float.isFinite(maxS) && maxS > 0f) {
                float curS = res.getCurrentStamina();
                if (Float.isFinite(curS) && curS > maxS + EPS) {
                    res.setCurrentStamina(maxS);
                    changed = true;
                }
            }
        } catch (Throwable ignored) {
        }
        return changed;
    }
}
