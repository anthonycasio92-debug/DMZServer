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
import com.dragonminez.common.util.TransformationsHelper;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of AndrioidConversion.js / DragonMineZ {@code NPCActionC2S.handleGero}
 * and the CNPC Android Upgrade Removal script — Dr. Gero convert + remove.
 */
public final class AndroidConversion {
    private static final Set<String> BLOCKED = Set.of("bioandroid");
    private static final String ANDROID_FORM_GROUP = "androidforms";
    private static final String ANDROID_BASE_FORM = "androidbase";
    private static final long CONFIRM_MS = 10_000L;

    /** Staff/self remove confirm: actor → pending target + expiry. */
    private static final Map<UUID, PendingRemove> PENDING_REMOVE = new ConcurrentHashMap<>();

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

    /**
     * Staff/self: remove Android upgrade (two-click confirm within 10s, same target).
     * Restores normal form skills deleted by Gero conversion.
     */
    public static String remove(ServerPlayer actor, ServerPlayer target) {
        if (!ProgressionConfig.androidConversion()) {
            return "§cAndroid tools are disabled. §7Staff: enable the android flag in /progression.";
        }
        if (actor == null || target == null) {
            return "§cPlayer required.";
        }
        try {
            StatsData data = DmzProgression.stats(target);
            if (data == null) {
                return "§c[Android] DragonMineZ data could not be loaded for §f"
                        + target.m_7755_().getString() + "§c.";
            }
            Status status = data.getStatus();
            if (status == null) {
                return "§c[Android] Status could not be loaded.";
            }
            if (!status.isAndroidUpgraded()) {
                clearPending(actor.m_20148_());
                return "§e[Android] §f" + target.m_7755_().getString()
                        + " §7is not an upgraded Android.";
            }

            long now = System.currentTimeMillis();
            PendingRemove pending = PENDING_REMOVE.get(actor.m_20148_());
            if (pending != null
                    && pending.until > now
                    && target.m_20148_().equals(pending.target)) {
                clearPending(actor.m_20148_());
                return doRemove(target, data);
            }

            PENDING_REMOVE.put(actor.m_20148_(), new PendingRemove(target.m_20148_(), now + CONFIRM_MS));
            String who = target.m_20148_().equals(actor.m_20148_())
                    ? "yourself"
                    : "§f" + target.m_7755_().getString();
            return "§6[Android] §cWarning: §7This will remove the Android upgrade from " + who + "§7.\n"
                    + "§7Race, stats, skills, and progression stay — form skills are restored.\n"
                    + "§eClick Remove again within 10 seconds to confirm.";
        } catch (Throwable t) {
            return "§c[Android Remove Error] §f" + t;
        }
    }

    private static String doRemove(ServerPlayer player, StatsData data) {
        Character character = data.getCharacter();
        Status status = data.getStatus();
        Skills skills = data.getSkills();
        if (character == null || status == null || skills == null) {
            return "§c[Android] Missing character/status/skills data.";
        }

        status.setAndroidUpgraded(false);

        try {
            TransformationsHelper.revertToBaseForm(player, data);
        } catch (Throwable ignored) {
            try {
                character.clearActiveForm(player);
            } catch (Throwable ignored2) {
                try {
                    character.clearActiveForm();
                } catch (Throwable ignored3) {
                }
            }
        }

        // Gero deletes these; re-add at 0 so trainers/quests can unlock again.
        ensureSkillAtZero(skills, "superforms");
        ensureSkillAtZero(skills, "legendaryforms");
        try {
            skills.removeSkill(ANDROID_FORM_GROUP);
        } catch (Throwable ignored) {
        }
        try {
            character.setSelectedFormGroup("");
            character.setSelectedForm("");
        } catch (Throwable ignored) {
        }

        String race = raceName(character);
        try {
            data.updateTransformationSkillLimits(race);
        } catch (Throwable ignored) {
            try {
                data.updateTransformationSkillLimits(character.getRace());
            } catch (Throwable ignored2) {
            }
        }
        try {
            TransformationsHelper.ensureSelectedFormDefault(data);
        } catch (Throwable ignored) {
        }
        try {
            TransformationsHelper.ensureSelectedStackFormDefault(data);
        } catch (Throwable ignored) {
        }
        try {
            player.m_6210_();
        } catch (Throwable ignored) {
        }
        DmzSkillUtil.sync(player);

        DmzRewards.msg(player, "§a[Android] §fUpgrade removed. §7You are no longer an upgraded Android.");
        DmzRewards.msg(player, "§7Race, stats, skills, and progression were preserved.");
        SystemTelemetry.log("progression", "android_remove", player, null,
                Map.of("race", race == null ? "" : race));
        return "§a[Android] Upgrade removed for §f" + player.m_7755_().getString() + "§a.";
    }

    private static void ensureSkillAtZero(Skills skills, String id) {
        try {
            if (!skills.hasSkill(id)) {
                skills.setSkillLevel(id, 0);
            }
        } catch (Throwable ignored) {
            try {
                skills.setSkillLevel(id, 0);
            } catch (Throwable ignored2) {
            }
        }
    }

    public static void clearPending(UUID actor) {
        if (actor != null) {
            PENDING_REMOVE.remove(actor);
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

    private record PendingRemove(UUID target, long until) {}
}
