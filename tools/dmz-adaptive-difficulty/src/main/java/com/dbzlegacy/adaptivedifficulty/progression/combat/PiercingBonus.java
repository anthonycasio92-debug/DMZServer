package com.dbzlegacy.adaptivedifficulty.progression.combat;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.util.ApothicAttributes;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.BonusStats;
import com.dragonminez.common.stats.character.Resources;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code Piercing.js} — PROT_PIERCE ranks add a SKP strike bonus while ki weapons
 * are not active.
 */
public final class PiercingBonus {
    private static final String BONUS_NAME = "dmzbridge_apothic_strike";
    private static final double BONUS_PER_RANK = 0.10;
    private static final long TICK_MS = 500L;
    private static final Map<UUID, Long> LAST_TICK = new ConcurrentHashMap<>();

    private PiercingBonus() {}

    public static void pulse(ServerPlayer player) {
        if (!DifficultyConfig.get().enablePiercingBonus || player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        UUID id = player.m_20148_();
        Long last = LAST_TICK.get(id);
        if (last != null && now - last < TICK_MS) {
            return;
        }
        LAST_TICK.put(id, now);

        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return;
        }
        BonusStats bonusStats;
        try {
            bonusStats = data.getBonusStats();
        } catch (Throwable t) {
            return;
        }
        if (bonusStats == null) {
            return;
        }

        try {
            bonusStats.removeBonus("SKP", BONUS_NAME);
        } catch (Throwable ignored) {
        }

        if (KiWeapons.isKiWeaponActive(data)) {
            return;
        }

        double strikeRank = ApothicAttributes.protPierce(player);
        if (!(strikeRank > 0.0)) {
            return;
        }

        double baseStrike;
        try {
            baseStrike = data.getStrikeDamage();
        } catch (Throwable t) {
            return;
        }
        if (!(baseStrike > 0.0)) {
            return;
        }

        double skpScaling = 1.0;
        try {
            skpScaling = data.getStatScaling("SKP");
        } catch (Throwable ignored) {
        }
        if (!(skpScaling > 0.0)) {
            skpScaling = 1.0;
        }

        double powerRelease = 1.0;
        try {
            Resources resources = data.getResources();
            if (resources != null) {
                int release = resources.getPowerRelease();
                if (release <= 0) {
                    release = resources.getRelease();
                }
                if (release > 0) {
                    powerRelease = release / 100.0;
                }
            }
        } catch (Throwable ignored) {
        }
        if (!(powerRelease > 0.0)) {
            powerRelease = 1.0;
        }

        double desired = baseStrike * (strikeRank * BONUS_PER_RANK);
        double needed = desired / (skpScaling * powerRelease);
        if (!(needed > 0.0) || !Double.isFinite(needed)) {
            return;
        }
        try {
            bonusStats.addBonus("SKP", BONUS_NAME, "+", needed);
        } catch (Throwable ignored) {
        }
    }

    public static void clearPlayer(UUID uuid) {
        if (uuid != null) {
            LAST_TICK.remove(uuid);
        }
    }
}
