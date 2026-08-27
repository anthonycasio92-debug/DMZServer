package com.dbzlegacy.adaptivedifficulty.util;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dragonminez.common.combat.clash.BeamClashManager;
import com.dragonminez.common.init.MainDamageTypes;
import com.dragonminez.common.network.NetworkHandler;
import com.dragonminez.common.network.S2C.StatsSyncS2C;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Resources;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;

/**
 * Shared DMZ reward / combat helpers for Rival + Sparring systems.
 */
public final class DmzRewards {
    private DmzRewards() {}

    public static boolean awardTp(
            ServerPlayer player,
            float amount,
            String reason,
            boolean showMessage,
            String prefix
    ) {
        if (player == null || !(amount > 0.0f)) {
            return false;
        }
        try {
            StatsData data = DmzProgression.stats(player);
            if (data == null) {
                return false;
            }
            Resources resources = data.getResources();
            if (resources == null) {
                return false;
            }
            resources.addTrainingPoints(amount);
            try {
                NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(player), player);
            } catch (Throwable syncErr) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] TP sync failed for {}: {}",
                        AdaptiveDifficultyMod.MOD_ID,
                        player.m_7755_().getString(),
                        syncErr.toString()
                );
            }
            if (showMessage) {
                String pfx = prefix == null || prefix.isBlank() ? "§6[TP] " : prefix;
                String why = reason == null || reason.isBlank() ? "" : (" §8(" + reason + ")");
                msg(player, pfx + "§a+" + formatWhole(amount) + " TP" + why);
            }
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] awardTp failed: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            return false;
        }
    }

    public static double battlePower(ServerPlayer player) {
        try {
            StatsData data = DmzProgression.stats(player);
            if (data == null) {
                return 0.0;
            }
            double exact = data.getBattlePowerExact();
            if (Double.isFinite(exact) && exact > 0.0) {
                return exact;
            }
            float bp = data.getBattlePower();
            return Double.isFinite(bp) && bp > 0.0f ? bp : 0.0;
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    public static double releasedBattlePower(ServerPlayer player) {
        try {
            double bp = battlePower(player);
            double release = powerReleasePercent(player) / 100.0;
            if (!(release > 0.0)) {
                release = 1.0;
            }
            return bp * release;
        } catch (Throwable ignored) {
            return battlePower(player);
        }
    }

    public static double powerReleasePercent(ServerPlayer player) {
        try {
            StatsData data = DmzProgression.stats(player);
            if (data == null) {
                return 100.0;
            }
            Resources resources = data.getResources();
            if (resources == null) {
                return 100.0;
            }
            int release = resources.getPowerRelease();
            if (release <= 0) {
                release = resources.getRelease();
            }
            return release > 0 ? release : 100.0;
        } catch (Throwable ignored) {
            return 100.0;
        }
    }

    public static boolean isKiDamage(DamageSource source) {
        if (source == null) {
            return false;
        }
        try {
            return MainDamageTypes.isKiblastDamage(source);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isClashing(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        try {
            return BeamClashManager.isClashing(uuid);
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void msg(ServerPlayer player, String text) {
        if (player == null || text == null) {
            return;
        }
        try {
            player.m_213846_(Component.m_237113_(text));
        } catch (Throwable ignored) {
        }
    }

    public static String formatWhole(double value) {
        long n = Math.max(0L, Math.round(value));
        String raw = Long.toString(n);
        StringBuilder out = new StringBuilder();
        int len = raw.length();
        for (int i = 0; i < len; i++) {
            if (i > 0 && (len - i) % 3 == 0) {
                out.append(',');
            }
            out.append(raw.charAt(i));
        }
        return out.toString();
    }

    public static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }

    public static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }
}
