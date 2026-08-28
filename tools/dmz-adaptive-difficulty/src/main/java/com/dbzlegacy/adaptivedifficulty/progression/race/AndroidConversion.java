package com.dbzlegacy.adaptivedifficulty.progression.race;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.RaceCharacterConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skills;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of AndrioidConversion.js / DragonMineZ {@code NPCActionC2S.handleGero} —
 * Dr. Gero android upgrade path.
 */
public final class AndroidConversion {
    private static final Set<String> BLOCKED = Set.of("bioandroid");
    private static final String ANDROID_FORM_GROUP = "androidforms";
    private static final String ANDROID_BASE_FORM = "androidbase";

    private AndroidConversion() {}

    public static String convert(ServerPlayer player) {
        if (!ProgressionConfig.androidConversion()) {
            return "§cAndroid conversion is disabled. §7Staff: enable the android flag in /progression.";
        }
        if (player == null) {
            return "§cPlayer required.";
        }
        try {
            StatsData data = DmzProgression.stats(player);
            if (data == null) {
                return "§c[Android] DragonMineZ data could not be loaded.";
            }
            Character character = data.getCharacter();
            Status status = data.getStatus();
            Skills skills = data.getSkills();
            if (character == null || status == null || skills == null) {
                return "§c[Android] Missing character/status/skills data.";
            }
            if (status.isAndroidUpgraded()) {
                return "§c[Android] §fYou are already an Android.";
            }
            String raceName = raceName(character);
            if (raceName.isBlank()) {
                return "§c[Android] Could not read player race.";
            }
            String lower = raceName.toLowerCase(Locale.ROOT);
            if (BLOCKED.contains(lower)) {
                return "§c[Android] §f" + raceName + " cannot be android-upgraded.";
            }
            // Match Gero: race must have androidforms TP costs configured (humans).
            if (!raceAllowsAndroidForms(raceName)) {
                return "§c[Android] §fOnly races with android forms (humans) can be converted. §7Race: §f"
                        + raceName;
            }

            status.setAndroidUpgraded(true);
            DmzSkillUtil.setLevel(skills, ANDROID_FORM_GROUP, 1);
            try {
                skills.removeSkill("superforms");
            } catch (Throwable ignored) {
            }
            try {
                skills.removeSkill("legendaryforms");
            } catch (Throwable ignored) {
            }
            try {
                data.updateTransformationSkillLimits(raceName);
            } catch (Throwable ignored) {
            }
            character.setSelectedFormGroup(ANDROID_FORM_GROUP);
            character.setSelectedForm(ANDROID_BASE_FORM);
            character.setActiveForm(ANDROID_FORM_GROUP, ANDROID_BASE_FORM);
            try {
                character.clearActiveStackForm();
            } catch (Throwable ignored) {
            }
            try {
                player.m_6210_(); // refreshDimensions
            } catch (Throwable ignored) {
            }
            DmzSkillUtil.sync(player);
            DmzRewards.msg(player, "§a[Android] §fConversion complete. §7Android forms unlocked.");
            SystemTelemetry.log("progression", "android_conversion", player, null,
                    Map.of("race", raceName));
            return "§a[Android] Conversion complete for §f" + player.m_7755_().getString() + "§a.";
        } catch (Throwable t) {
            return "§c[Android Trigger Error] §f" + t;
        }
    }

    private static String raceName(Character character) {
        // Gero uses getRaceName() only.
        try {
            String n = character.getRaceName();
            if (n != null && !n.isBlank()) {
                return n;
            }
        } catch (Throwable ignored) {
        }
        try {
            String n = character.getRace();
            return n == null ? "" : n;
        } catch (Throwable ignored) {
            return "";
        }
    }

    private static boolean raceAllowsAndroidForms(String raceName) {
        try {
            RaceCharacterConfig cfg = ConfigManager.getRaceCharacter(raceName);
            if (cfg == null) {
                return false;
            }
            Integer[] costs = cfg.getFormSkillTpCosts(ANDROID_FORM_GROUP);
            return costs != null && costs.length > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
