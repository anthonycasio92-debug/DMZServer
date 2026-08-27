package com.dbzlegacy.adaptivedifficulty.calc;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
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

    /**
     * Stable active form identity for future custom races that skip FormChangeEvent
     * or keep multipliers at 1 while swapping form strings.
     * Empty when fully base. Format: {@code race|formGroup|form|stackGroup|stackForm}.
     */
    public static String activeFormKey(Player player) {
        Character ch = character(player);
        if (ch == null) {
            return "";
        }
        try {
            boolean form = false;
            boolean stack = false;
            try {
                form = ch.hasActiveForm();
            } catch (Throwable ignored) {
            }
            try {
                stack = ch.hasActiveStackForm();
            } catch (Throwable ignored) {
            }
            if (!form && !stack) {
                return "";
            }
            String race = race(player);
            String fg = safeStr(ch.getActiveFormGroup());
            String f = safeStr(ch.getActiveForm());
            String sg = "";
            String s = "";
            try {
                sg = safeStr(ch.getActiveStackFormGroup());
                s = safeStr(ch.getActiveStackForm());
            } catch (Throwable ignored) {
            }
            return race + "|" + fg + "|" + f + "|" + sg + "|" + s;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static String safeStr(String v) {
        return v == null ? "" : v.trim().toLowerCase();
    }

    /**
     * Live DMZ {@code getLevel()} — a <b>stat-progress</b> estimate, not a stored
     * character counter. Clamp to the configured DMZ/AD max so costs / GUI never
     * show values above the server level ceiling (e.g. 100k players reading as
     * ~987k when maxValue / max-stats mode disagrees).
     */
    public static int dmzLevel(Player player) {
        StatsData data = stats(player);
        if (data == null) {
            return 1;
        }
        try {
            return clampDmzLevel(data.getLevel(), data);
        } catch (Throwable ignored) {
            return 1;
        }
    }

    /**
     * Hard ceiling for AD level reads: min(DMZ gameplay maxValue, AD referenceMaxLevel).
     * Always ≥ 1.
     */
    public static int configuredMaxDmzLevel(StatsData data) {
        int dmzMax = 0;
        try {
            if (data != null) {
                dmzMax = data.getConfiguredMaxValue();
            }
        } catch (Throwable ignored) {
        }
        if (dmzMax <= 1) {
            try {
                var gameplay = com.dragonminez.common.config.ConfigManager.getServerConfig().getGameplay();
                if (gameplay != null && gameplay.getMaxValue() != null) {
                    dmzMax = gameplay.getMaxValue();
                }
            } catch (Throwable ignored) {
            }
        }
        long adMax = 100_000L;
        try {
            adMax = Math.max(1L, DifficultyConfig.get().referenceMaxLevel);
        } catch (Throwable ignored) {
        }
        int ceiling;
        if (dmzMax > 1 && adMax > 0L) {
            ceiling = (int) Math.min(dmzMax, Math.min(Integer.MAX_VALUE, adMax));
        } else if (dmzMax > 1) {
            ceiling = dmzMax;
        } else {
            ceiling = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, adMax));
        }
        return Math.max(1, ceiling);
    }

    public static int clampDmzLevel(int raw) {
        return clampDmzLevel(raw, null);
    }

    public static int clampDmzLevel(int raw, StatsData data) {
        int max = configuredMaxDmzLevel(data);
        if (raw < 1) {
            return 1;
        }
        return Math.min(raw, max);
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
            return clampDmzLevel(cached);
        }
        if (fallbackWhenTransformed > 0L) {
            long capped = Math.min(fallbackWhenTransformed, configuredMaxDmzLevel(stats(player)));
            return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, capped));
        }
        // Last resort: live level (may be form-sensitive on some race setups).
        return live;
    }

    public static int dmzLevelForProgression(Player player) {
        return dmzLevelForProgression(player, 0L);
    }

    /**
     * DMZ level for unlock gates only.
     * Never uses historical {@code highestDmzLevel} or form-inflated live level —
     * those let post-prestige players keep high-tier access without Prestige N.
     * Base form → live level · transformed → last base-form sample · else 1.
     * Callers that revoke unlocks must check {@link #hasReliableUnlockGateSample} first —
     * a bare {@code 1} here means "unknown", not "player is level 1".
     */
    public static int dmzLevelForUnlockGate(Player player) {
        if (player == null) {
            return 1;
        }
        if (!isTransformed(player)) {
            int live = dmzLevel(player);
            BASE_FORM_LEVEL.put(player.m_20148_(), live);
            return live;
        }
        Integer cached = BASE_FORM_LEVEL.get(player.m_20148_());
        if (cached != null) {
            return Math.max(1, cached);
        }
        return 1;
    }

    /**
     * True when unlock-gate level is safe to use for revoke / prestige-up resets.
     * False when the player is transformed with no base-form sample this session
     * (login-already-transformed, or caches cleared by admin reload).
     */
    public static boolean hasReliableUnlockGateSample(Player player) {
        if (player == null) {
            return false;
        }
        if (!isTransformed(player)) {
            return true;
        }
        return BASE_FORM_LEVEL.containsKey(player.m_20148_());
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
     * Refresh the base-form DMZ sample when in base form; otherwise return the cached sample.
     */
    public static int refreshBaseFormSample(Player player) {
        if (player == null) {
            return 1;
        }
        if (!isTransformed(player)) {
            int live = dmzLevel(player);
            BASE_FORM_LEVEL.put(player.m_20148_(), live);
            return live;
        }
        Integer cached = BASE_FORM_LEVEL.get(player.m_20148_());
        return cached != null ? Math.max(1, cached) : 1;
    }

    /**
     * GUI-open hook: refresh the base-form DMZ sample, then return the unlock-gate level.
     * Does not require personal difficulty ON — opening the menu alone is enough.
     */
    public static int sampleLevelOnGuiOpen(Player player) {
        return refreshBaseFormSample(player);
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
        // Delegate to combat profile so unlock-gate transform detection matches
        // addition-mode form⊕stack folding (this server disables product mode).
        return PlayerCombatProfile.liveFormMultiplierPeak(data);
    }

    /**
     * DMZ prestige skill level (synced from Fabled Prestige class level - 1).
     */
    public static int prestige(Player player) {
        return skillLevel(player, "prestige");
    }

    /** Safe skill level lookup (0 when missing / getter fails). */
    public static int skillLevel(Player player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) {
            return 0;
        }
        StatsData data = stats(player);
        if (data == null) {
            return 0;
        }
        try {
            Skills skills = data.getSkills();
            if (skills == null) {
                return 0;
            }
            return Math.max(0, skills.getSkillLevel(skillId));
        } catch (Throwable ignored) {
            return 0;
        }
    }

    /** True when a toggleable skill is currently active. */
    public static boolean skillActive(Player player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) {
            return false;
        }
        StatsData data = stats(player);
        if (data == null) {
            return false;
        }
        try {
            Skills skills = data.getSkills();
            return skills != null && skills.isSkillActive(skillId);
        } catch (Throwable ignored) {
            return false;
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
