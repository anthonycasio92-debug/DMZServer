package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.FabledBridge;
import com.dbzlegacy.adaptivedifficulty.progression.combat.CombatProgression;
import com.dbzlegacy.adaptivedifficulty.progression.dummy.DummyProgression;
import com.dbzlegacy.adaptivedifficulty.progression.end.EndProgression;
import com.dbzlegacy.adaptivedifficulty.progression.race.AndroidConversion;
import com.dbzlegacy.adaptivedifficulty.progression.race.RaceLock;
import com.dbzlegacy.adaptivedifficulty.progression.race.SpiritualistKiControl;
import com.dbzlegacy.adaptivedifficulty.progression.race.YardratProgression;
import com.dbzlegacy.adaptivedifficulty.progression.shop.ShopProgression;
import com.dbzlegacy.adaptivedifficulty.progression.skills.FlightProgression;
import com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression;
import com.dbzlegacy.adaptivedifficulty.progression.skills.PotentialProgression;
import com.dbzlegacy.adaptivedifficulty.progression.skills.SprintJumpProgression;
import com.dbzlegacy.adaptivedifficulty.progression.tp.BioAndroidAbsorb;
import com.dbzlegacy.adaptivedifficulty.progression.tp.BuildingTp;
import com.dbzlegacy.adaptivedifficulty.progression.tp.FarmingTp;
import com.dbzlegacy.adaptivedifficulty.progression.tp.GlobalTpBoost;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * Facade for natural-progression CNPC ports — skills/tp/race (sibling agents) plus
 * combat / End / shadow-dummy / shop modules from this agent.
 */
public final class ProgressionSystem {
    private ProgressionSystem() {}

    public static void onLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (ProgressionConfig.masterEnabled()) {
            try {
                GlobalTpBoost.onLogin(player);
            } catch (Throwable ignored) {
            }
        }
        if (DifficultyConfig.get().enableFabledBridge) {
            try {
                FabledBridge.onLogin(player);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void onLogout(ServerPlayer player) {
        if (player == null) {
            return;
        }
        try {
            FlightProgression.onLogout(player);
        } catch (Throwable ignored) {
        }
        try {
            MeditationProgression.onLogout(player);
        } catch (Throwable ignored) {
        }
        try {
            CombatProgression.onLogout(player);
        } catch (Throwable ignored) {
        }
        try {
            DummyProgression.onLogout(player);
        } catch (Throwable ignored) {
        }
        try {
            ShopProgression.onLogout(player);
        } catch (Throwable ignored) {
        }
        try {
            if (DifficultyConfig.get().enableFabledBridge) {
                FabledBridge.onLogout(player);
            }
        } catch (Throwable ignored) {
        }
        ProgressionData.clearPlayer(player.m_20148_());
    }

    public static void pulse(MinecraftServer server, int tick) {
        if (server == null) {
            return;
        }
        if (ProgressionConfig.masterEnabled()) {
            long now = System.currentTimeMillis();
            try {
                GlobalTpBoost.pulse(server, tick);
            } catch (Throwable ignored) {
            }
            try {
                EndProgression.pulse(server, tick);
            } catch (Throwable ignored) {
            }
            try {
                DummyProgression.pulse(server, tick);
            } catch (Throwable ignored) {
            }
            try {
                CombatProgression.pulse(server, tick);
            } catch (Throwable ignored) {
            }
            for (ServerPlayer player : server.m_6846_().m_11314_()) {
                if (player == null) {
                    continue;
                }
                try {
                    FlightProgression.pulse(player, now);
                } catch (Throwable ignored) {
                }
                try {
                    SprintJumpProgression.pulse(player, now);
                } catch (Throwable ignored) {
                }
                try {
                    MeditationProgression.pulse(player, now);
                } catch (Throwable ignored) {
                }
                try {
                    BioAndroidAbsorb.pulse(player, now);
                } catch (Throwable ignored) {
                }
                try {
                    RaceLock.pulse(player, now);
                } catch (Throwable ignored) {
                }
                try {
                    YardratProgression.pulse(player, now);
                } catch (Throwable ignored) {
                }
                try {
                    SpiritualistKiControl.pulse(player, now);
                } catch (Throwable ignored) {
                }
            }
        }
        if (DifficultyConfig.get().enableFabledBridge) {
            try {
                FabledBridge.pulse(server, tick);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Broad hurt fan-out (End mitigation, ki weapons, DoT, shadow protect, potential). */
    public static void onHurt(LivingHurtEvent event) {
        if (event == null || !ProgressionConfig.masterEnabled()) {
            return;
        }
        try {
            EndProgression.onHurt(event);
        } catch (Throwable ignored) {
        }
        try {
            DummyProgression.onHurt(event);
        } catch (Throwable ignored) {
        }
        LivingEntity victim = event.getEntity();
        var source = event.getSource();
        var causing = source == null ? null : source.m_7639_();
        if (victim instanceof ServerPlayer victimPlayer) {
            try {
                CombatProgression.onPlayerHurt(event, victimPlayer);
            } catch (Throwable ignored) {
            }
            if (causing instanceof ServerPlayer attacker) {
                try {
                    PotentialProgression.onPlayerHurt(victimPlayer, attacker, source);
                } catch (Throwable ignored) {
                }
                try {
                    MeditationProgression.onHurt(victimPlayer);
                } catch (Throwable ignored) {
                }
            } else {
                try {
                    MeditationProgression.onHurt(victimPlayer);
                } catch (Throwable ignored) {
                }
            }
        }
        if (causing instanceof ServerPlayer attacker && victim != null) {
            try {
                CombatProgression.onHurt(event, attacker, victim);
            } catch (Throwable ignored) {
            }
        }
    }

    /** Legacy PvP-only entry kept for existing DifficultyEvents call sites. */
    public static void onPlayerHurt(ServerPlayer victim, ServerPlayer attacker, DamageSource source) {
        if (!ProgressionConfig.masterEnabled()) {
            return;
        }
        try {
            PotentialProgression.onPlayerHurt(victim, attacker, source);
        } catch (Throwable ignored) {
        }
        try {
            MeditationProgression.onHurt(victim);
        } catch (Throwable ignored) {
        }
    }

    public static void onDeath(LivingDeathEvent event) {
        if (event == null || !ProgressionConfig.masterEnabled()) {
            return;
        }
        var killer = event.getSource() == null ? null : event.getSource().m_7639_();
        if (killer instanceof ServerPlayer player) {
            try {
                EndProgression.onKill(event, player);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event == null || !ProgressionConfig.masterEnabled()) {
            return;
        }
        try {
            DummyProgression.onJoin(event);
        } catch (Throwable ignored) {
        }
    }

    public static void onBlockBreak(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!ProgressionConfig.masterEnabled()) {
            return;
        }
        try {
            FarmingTp.onBlockBreak(player, pos, state);
        } catch (Throwable ignored) {
        }
    }

    public static void onBlockPlace(ServerPlayer player, BlockPos pos, BlockState state) {
        if (!ProgressionConfig.masterEnabled()) {
            return;
        }
        try {
            BuildingTp.onBlockPlace(player, pos, state);
        } catch (Throwable ignored) {
        }
    }

    /* ========================= Command API ========================= */

    public static String boostStartEncoded(ServerPlayer actor, int encoded, String purchaser) {
        return GlobalTpBoost.startBoost(actor, encoded, purchaser);
    }

    public static String boostStart(ServerPlayer actor, double mult, int minutes, String purchaser) {
        return GlobalTpBoost.startBoostMinutes(actor, mult, minutes, purchaser);
    }

    public static String boostEnd() {
        return GlobalTpBoost.endBoost(true);
    }

    public static String meditationNext(ServerPlayer actor) {
        return MeditationProgression.advanceTrial(actor);
    }

    public static String androidConvert(ServerPlayer target) {
        return AndroidConversion.convert(target);
    }

    public static String statusSummary() {
        return ProgressionConfig.statusSummary()
                + "\n" + GlobalTpBoost.statusLine()
                + "\n" + MeditationProgression.statusLine()
                + "\n combat/end/dummy/shop flags via /lm";
    }

    public static boolean setFlag(String key, boolean on) {
        DifficultyConfig cfg = DifficultyConfig.get();
        if (cfg == null || key == null) {
            return false;
        }
        switch (key.toLowerCase(java.util.Locale.ROOT)) {
            case "master", "progression", "enableprogression" -> cfg.enableProgression = on;
            case "flight", "fly", "enableflightprogression" -> cfg.enableFlightProgression = on;
            case "sprint", "sprintjump", "enablesprintjump" -> cfg.enableSprintJump = on;
            case "meditation", "med", "enablemeditation" -> cfg.enableMeditation = on;
            case "potential", "enablepotential" -> cfg.enablePotential = on;
            case "farming", "farmingtp", "enablefarmingtp" -> cfg.enableFarmingTp = on;
            case "building", "buildingtp", "enablebuildingtp" -> cfg.enableBuildingTp = on;
            case "boost", "globaltpboost", "enableglobaltpboost" -> cfg.enableGlobalTpBoost = on;
            case "bio", "bioandroid", "enablebioandroid" -> cfg.enableBioAndroid = on;
            case "racelock", "lock", "enableracelock" -> cfg.enableRaceLock = on;
            case "yardrat", "enableyardrat" -> cfg.enableYardrat = on;
            case "spiritualist", "spirit", "enablespiritualistki" -> cfg.enableSpiritualistKi = on;
            case "android", "enableandroidconversion" -> cfg.enableAndroidConversion = on;
            case "kiweapons", "enablekiweapons" -> cfg.enableKiWeapons = on;
            case "piercing", "enablepiercingbonus" -> cfg.enablePiercingBonus = on;
            case "dot", "enabledotextradamage" -> cfg.enableDotExtraDamage = on;
            case "apothic", "enableapothicelemental" -> cfg.enableApothicElemental = on;
            case "end", "endstrength", "enableenddimensionstrength" -> cfg.enableEndDimensionStrength = on;
            case "endportal", "endportalguard", "enableendportalguard" -> cfg.enableEndPortalGuard = on;
            case "endnatural", "enableendnaturaldragonspawn" -> cfg.enableEndNaturalDragonSpawn = on;
            case "shadow", "shadowdummy", "enableshadowdummylimiter" -> cfg.enableShadowDummyLimiter = on;
            case "skills", "skillunlock", "enableskillunlockservice" -> cfg.enableSkillUnlockService = on;
            case "prestige", "enableprestigesystem" -> cfg.enablePrestigeSystem = on;
            case "fabled", "enablefabledbridge" -> cfg.enableFabledBridge = on;
            default -> {
                return false;
            }
        }
        DifficultyConfig.save();
        return true;
    }
}
