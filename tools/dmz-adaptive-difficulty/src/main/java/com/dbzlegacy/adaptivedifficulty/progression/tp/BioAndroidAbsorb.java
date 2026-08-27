package com.dbzlegacy.adaptivedifficulty.progression.tp;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.config.ConfigManager;
import com.dragonminez.common.config.SkillsConfig;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Cooldowns;
import com.dragonminez.common.stats.character.Resources;
import com.dragonminez.common.stats.character.Status;
import com.dragonminez.common.stats.skills.Skills;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Port of BioAndroid.js — absorb TP / learn skills while DrainActive.
 */
public final class BioAndroidAbsorb {
    private BioAndroidAbsorb() {}

    public static void pulse(ServerPlayer player, long nowMs) {
        if (!ProgressionConfig.bioAndroid() || player == null) {
            return;
        }
        if (nowMs < ProgressionData.tempGetLong(player, "bioandroid_next_check", 0L)) {
            return;
        }
        ProgressionData.tempPut(player, "bioandroid_next_check", nowMs + 250L);
        try {
            StatsData bioData = DmzProgression.stats(player);
            if (bioData == null) {
                return;
            }
            Character bioChar = bioData.getCharacter();
            if (bioChar == null) {
                return;
            }
            String race = String.valueOf(bioChar.getRace()).toLowerCase();
            if (!"bioandroid".equals(race)) {
                return;
            }
            Cooldowns cds = bioData.getCooldowns();
            if (cds == null || !cds.hasCooldown(Cooldowns.DRAIN_ACTIVE)) {
                ProgressionData.tempRemove(player, "bioandroid_absorb_processed_target");
                return;
            }
            Status status = bioData.getStatus();
            if (status == null) {
                return;
            }
            int targetId = status.getDrainingTargetId();
            if (targetId <= 0) {
                return;
            }
            String last = ProgressionData.tempGet(player, "bioandroid_absorb_processed_target", "");
            if (String.valueOf(targetId).equals(last)) {
                return;
            }
            ProgressionData.tempPut(player, "bioandroid_absorb_processed_target", String.valueOf(targetId));

            Entity target = findEntity(player, targetId);
            if (!(target instanceof LivingEntity living)) {
                return;
            }
            Resources bioResources = bioData.getResources();
            if (bioResources == null) {
                return;
            }

            if (!(living instanceof ServerPlayer targetPlayer)) {
                int npcBonus = (int) Math.floor(living.m_21233_() * 0.10);
                if (npcBonus > 0) {
                    DmzRewards.awardTp(player, npcBonus, "bio absorb npc", true, "§a[Absorb] ");
                    SystemTelemetry.log("progression", "bio_absorb_npc", player, null,
                            Map.of("tp", npcBonus));
                }
                return;
            }

            StatsData targetData = DmzProgression.stats(targetPlayer);
            if (targetData == null) {
                return;
            }
            Resources targetResources = targetData.getResources();
            if (targetResources == null) {
                return;
            }
            int stolen = (int) Math.floor(targetResources.getTrainingPoints() * 0.10f);
            if (stolen > 0) {
                try {
                    targetResources.removeTrainingPoints(stolen);
                } catch (Throwable ignored) {
                }
                bioResources.addTrainingPoints(stolen);
                DmzSkillUtil.sync(player);
                DmzSkillUtil.sync(targetPlayer);
                DmzRewards.msg(player, "§a[Absorb] Absorbed §e" + stolen + " TP§a from player.");
            }

            Skills bioSkills = bioData.getSkills();
            Skills targetSkills = targetData.getSkills();
            if (bioSkills == null || targetSkills == null) {
                return;
            }
            List<String> possible = learnableSkills(bioSkills, targetSkills);
            if (possible.isEmpty()) {
                int bonus = (int) Math.floor(targetPlayer.m_21233_() * 0.05);
                if (bonus > 0) {
                    bioResources.addTrainingPoints(bonus);
                    DmzSkillUtil.sync(player);
                    DmzRewards.msg(player,
                            "§a[Absorb] Target had no learnable skills. Gained §e" + bonus
                                    + " TP§a from max health.");
                }
                return;
            }
            int bioLvl = bioData.getLevel();
            int targetLvl = targetData.getLevel();
            double chance = 0.50;
            if (targetLvl > 0) {
                chance = 0.50 + ((bioLvl - targetLvl) / (targetLvl * 2.0));
            }
            chance = Math.max(0.05, Math.min(0.95, chance));
            if (ThreadLocalRandom.current().nextDouble() > chance) {
                return;
            }
            String picked = possible.get(ThreadLocalRandom.current().nextInt(possible.size()));
            int newLevel = Math.min(
                    DmzSkillUtil.level(bioSkills, picked) + 1,
                    DmzSkillUtil.level(targetSkills, picked)
            );
            DmzSkillUtil.setLevel(bioSkills, picked, newLevel);
            DmzSkillUtil.sync(player);
            DmzRewards.msg(player, "§d[Absorb] Learned §b" + picked + " §dlevel " + newLevel + "§d.");
            SystemTelemetry.log("progression", "bio_absorb_skill", player, targetPlayer,
                    Map.of("skill", picked, "level", newLevel));
        } catch (Throwable ignored) {
        }
    }

    private static List<String> learnableSkills(Skills bioSkills, Skills targetSkills) {
        List<String> out = new ArrayList<>();
        try {
            SkillsConfig cfg = ConfigManager.getSkillsConfig();
            List<String> formSkills = cfg == null ? List.of() : cfg.getFormSkills();
            List<String> stackSkills = cfg == null ? List.of() : cfg.getStackSkills();
            for (String skillName : targetSkills.getAllSkills().keySet()) {
                if (formSkills != null && formSkills.contains(skillName)) {
                    continue;
                }
                if (stackSkills != null && stackSkills.contains(skillName)) {
                    continue;
                }
                if (DmzSkillUtil.level(targetSkills, skillName) > DmzSkillUtil.level(bioSkills, skillName)) {
                    out.add(skillName);
                }
            }
        } catch (Throwable ignored) {
        }
        return out;
    }

    private static Entity findEntity(ServerPlayer player, int entityId) {
        try {
            ServerLevel level = player.m_284548_();
            if (level == null) {
                return null;
            }
            return level.m_6815_(entityId);
        } catch (Throwable ignored) {
            return null;
        }
    }
}
