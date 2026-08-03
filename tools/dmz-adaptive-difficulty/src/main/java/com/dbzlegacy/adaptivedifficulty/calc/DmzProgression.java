package com.dbzlegacy.adaptivedifficulty.calc;

import com.dragonminez.common.stats.StatsCapability;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.StatsProvider;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.skills.Skills;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.world.entity.player.Player;

/**
 * Reads DMZ progression for V3 unlocks + combat rating + class / race identity.
 */
public final class DmzProgression {
    /**
     * Last DMZ level observed while the player was in base form.
     * Tier buy costs / unlock gates must not follow form-inflated live levels.
     */
    private static final Map<UUID, Integer> BASE_FORM_LEVEL = new ConcurrentHashMap<>();

    private DmzProgression() {}

    public static StatsData stats(Player player) {
        if (player == null) {
            return null;
        }
        try {
            return StatsProvider.get(StatsCapability.INSTANCE, player).orElse(null);
        } catch (Throwable ignored) {
            return null;
        }
    }

    public static Character character(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return null;
        }
        try {
            return data.getCharacter();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** DMZ race id (e.g. {@code saiyan}, {@code human}). Empty when unavailable. */
    public static String race(Player player) {
        Character ch = character(player);
        if (ch == null) {
            return "";
        }
        try {
            String race = ch.getRace();
            if (race == null || race.isBlank()) {
                race = ch.getRaceName();
            }
            return race == null ? "" : race.trim().toLowerCase();
        } catch (Throwable ignored) {
            return "";
        }
    }

    /**
     * DMZ fighting class id (e.g. {@code warrior}, {@code spiritualist}, {@code tank}).
     * Empty when unavailable.
     */
    public static String fightingClass(Player player) {
        Character ch = character(player);
        if (ch == null) {
            return "";
        }
        try {
            String cls = ch.getCharacterClass();
            return cls == null ? "" : cls.trim().toLowerCase();
        } catch (Throwable ignored) {
            return "";
        }
    }

    /** Live DMZ {@code getLevel()} (may move with form on some race setups). */
    public static int dmzLevel(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return 1;
        }
        try {
            return Math.max(1, data.getLevel());
        } catch (Throwable ignored) {
            return 1;
        }
    }

    /**
     * Form-stable DMZ level for tier buy costs, unlock gates, and titles.
     * Updates only while the player is in base form so transforming cannot
     * raise (or lower) Ancient Coin prices mid-fight.
     *
     * @param fallbackWhenTransformed preferred level when transformed with no
     *        base-form sample yet (e.g. {@code highestDmzLevel}); {@code <=0} ignored
     */
    public static int dmzLevelForProgression(Player player, long fallbackWhenTransformed) {
        if (player == null) {
            return 1;
        }
        int live = dmzLevel(player);
        UUID id = player.m_20148_();
        if (!isTransformed(player)) {
            BASE_FORM_LEVEL.put(id, live);
            return live;
        }
        Integer cached = BASE_FORM_LEVEL.get(id);
        if (cached != null) {
            return Math.max(1, cached);
        }
        if (fallbackWhenTransformed > 0L) {
            return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, fallbackWhenTransformed));
        }
        // Last resort: live level (may be form-sensitive on some race setups).
        return live;
    }

    public static int dmzLevelForProgression(Player player) {
        return dmzLevelForProgression(player, 0L);
    }

    public static void clearBaseFormLevel(UUID playerId) {
        if (playerId != null) {
            BASE_FORM_LEVEL.remove(playerId);
        }
    }

    public static void clearAllBaseFormLevels() {
        BASE_FORM_LEVEL.clear();
    }

    /**
     * True when an active form / stack form is set, or form multipliers clearly exceed base.
     */
    public static boolean isTransformed(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return false;
        }
        try {
            Character ch = data.getCharacter();
            if (ch != null) {
                if (ch.hasActiveForm()) {
                    return true;
                }
                if (ch.hasActiveStackForm()) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return formMultiplierPeak(data) > 1.12;
    }

    private static double formMultiplierPeak(StatsData data) {
        double peak = 1.0;
        for (String key : new String[] {"STR", "SKP", "PWR", "RES", "VIT"}) {
            try {
                double form = Math.max(0.0, data.getFormMultiplier(key));
                double stack = Math.max(0.0, data.getStackFormMultiplier(key));
                double combined = Math.max(form, 1.0) * Math.max(stack, 1.0);
                if (form > 0.0 && form < 1.0 && stack <= 1.0) {
                    combined = 1.0 + form;
                }
                if (combined > peak) {
                    peak = combined;
                }
            } catch (Throwable ignored) {
            }
        }
        if (!(peak > 0.0) || Double.isNaN(peak) || Double.isInfinite(peak)) {
            return 1.0;
        }
        // Match PlayerCombatProfile — planned ×80 forms need headroom past 50.
        return Math.max(1.0, Math.min(100.0, peak));
    }

    /**
     * DMZ prestige skill level (synced from Fabled Prestige class level - 1).
     */
    public static int prestige(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return 0;
        }
        try {
            Skills skills = data.getSkills();
            if (skills == null) {
                return 0;
            }
            return Math.max(0, skills.getSkillLevel("prestige"));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /**
     * Transformation / form power contribution for Combat Rating.
     * Uses battle power (includes form multipliers) scaled into CR units.
     */
    public static double transformationPower(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return 0.0;
        }
        try {
            double bp = data.getBattlePowerExact();
            if (!(bp > 0.0) || Double.isNaN(bp) || Double.isInfinite(bp)) {
                bp = data.getBattlePower();
            }
            // Keep CR readable — raw BP can be multi-million.
            return Math.max(0.0, bp / 1000.0);
        } catch (Throwable ignored) {
            return 0.0;
        }
    }
}
