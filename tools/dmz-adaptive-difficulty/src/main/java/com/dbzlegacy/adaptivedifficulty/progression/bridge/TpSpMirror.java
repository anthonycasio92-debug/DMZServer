package com.dbzlegacy.adaptivedifficulty.progression.bridge;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code TP IS SP Fabled.js} — DMZ training points mirror Fabled skill points.
 * DMZ TP is authoritative; Fabled SP is a capped display that spends back into TP.
 */
public final class TpSpMirror {
    private static final int MAX_FABLED_SP = Integer.MAX_VALUE;
    private static final String KEY_INITIALIZED = "dmz_fabled_sp_initialized";
    private static final String KEY_LAST_DISPLAYED_SP = "dmz_fabled_sp_last_displayed";

    private TpSpMirror() {}

    public static void clear(UUID id) {
        // Session state lives in ProgressionData temp/stored; logout clears temp via ProgressionSystem.
    }

    public static void sync(ServerPlayer player) {
        if (player == null || !DifficultyConfig.get().enableTpSpMirror) {
            return;
        }
        Object data = FabledBridge.fabledData(player);
        if (data == null) {
            return;
        }
        StatsData dmz = DmzProgression.stats(player);
        if (dmz == null) {
            return;
        }
        Resources resources = dmz.getResources();
        if (resources == null) {
            return;
        }

        double currentTp = resources.getTrainingPoints();
        if (!Double.isFinite(currentTp) || currentTp < 0) {
            currentTp = 0;
        }

        int currentSp = Math.max(0, FabledBridge.invokeInt(data, "getPoints"));
        int targetDisplayedSp = (int) Math.min(MAX_FABLED_SP, Math.max(0, Math.floor(currentTp)));

        if (!ProgressionData.storedGetBool(player, KEY_INITIALIZED)) {
            if (currentSp != targetDisplayedSp) {
                FabledBridge.invokeVoid(data, "setPoints", targetDisplayedSp);
                try {
                    data.getClass().getMethod("updateScoreboard").invoke(data);
                } catch (Throwable ignored) {
                }
            }
            ProgressionData.storedPutBool(player, KEY_INITIALIZED, true);
            ProgressionData.storedPut(player, KEY_LAST_DISPLAYED_SP, targetDisplayedSp);
            return;
        }

        int lastDisplayedSp = (int) ProgressionData.storedGetLong(player, KEY_LAST_DISPLAYED_SP, targetDisplayedSp);
        if (lastDisplayedSp < 0) {
            lastDisplayedSp = targetDisplayedSp;
        }
        if (lastDisplayedSp > MAX_FABLED_SP) {
            lastDisplayedSp = MAX_FABLED_SP;
        }

        int spentSp = 0;
        if (currentSp < lastDisplayedSp) {
            spentSp = lastDisplayedSp - currentSp;
        }

        boolean tpChanged = false;
        boolean spChanged = false;

        if (spentSp > 0) {
            double newTp = Math.max(0, currentTp - spentSp);
            resources.setTrainingPoints((float) newTp);
            currentTp = newTp;
            tpChanged = true;
            FabledBridge.logSync(player, "tp_sp_spend", "spent", spentSp, "tp", currentTp);
        }

        targetDisplayedSp = (int) Math.min(MAX_FABLED_SP, Math.max(0, Math.floor(currentTp)));
        if (currentSp != targetDisplayedSp) {
            FabledBridge.invokeVoid(data, "setPoints", targetDisplayedSp);
            spChanged = true;
        }

        if (tpChanged) {
            try {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            } catch (Throwable ignored) {
            }
        }
        if (spChanged) {
            try {
                data.getClass().getMethod("updateScoreboard").invoke(data);
            } catch (Throwable ignored) {
            }
        }

        ProgressionData.storedPut(player, KEY_LAST_DISPLAYED_SP, targetDisplayedSp);
    }
}
