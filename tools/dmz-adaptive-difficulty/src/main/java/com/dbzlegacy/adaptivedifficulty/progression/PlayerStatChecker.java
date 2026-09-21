package com.dbzlegacy.adaptivedifficulty.progression;

import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.calc.LmOverhaulScaledCombat;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.StatsData;
import com.dragonminez.common.stats.character.Character;
import com.dragonminez.common.stats.character.Stats;
import com.dragonminez.common.stats.skills.Skill;
import com.dragonminez.common.stats.skills.Skills;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;

/**
 * Port of {@code PlayerStatChecker.js}: sneak + right-click another player to dump DMZ stats.
 */
public final class PlayerStatChecker {
    private static final long COOLDOWN_MS = 1500L;
    private static final Map<UUID, Long> LAST_CHECK = new ConcurrentHashMap<>();

    private PlayerStatChecker() {}

    public static boolean enabled() {
        return ProgressionConfig.masterEnabled() && DifficultyConfig.get().enablePlayerStatChecker;
    }

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (!enabled() || event == null || event.getHand() != InteractionHand.MAIN_HAND) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer viewer)) {
            return;
        }
        if (!viewer.m_6144_()) { // isShiftKeyDown / sneaking
            return;
        }
        Entity targetEntity = event.getTarget();
        if (!(targetEntity instanceof ServerPlayer target)) {
            return;
        }
        if (viewer.m_20148_().equals(target.m_20148_())) {
            return;
        }
        long now = System.currentTimeMillis();
        Long prev = LAST_CHECK.get(viewer.m_20148_());
        if (prev != null && now - prev < COOLDOWN_MS) {
            return;
        }
        LAST_CHECK.put(viewer.m_20148_(), now);
        show(viewer, target);
        event.setCanceled(true);
    }

    public static void show(ServerPlayer viewer, ServerPlayer target) {
        if (viewer == null || target == null) {
            return;
        }
        try {
            StatsData data = DmzProgression.stats(target);
            if (data == null) {
                DmzRewards.msg(viewer, "§c[Stats] No DMZ data for " + target.m_7755_().getString());
                return;
            }
            String name = target.m_7755_().getString();
            msg(viewer, "§6======= DMZ Stats: " + name + " =======");

            Character ch = data.getCharacter();
            if (ch != null) {
                try {
                    msg(viewer, "§eRace: §f" + safe(ch.getRace()));
                } catch (Throwable ignored) {
                }
                try {
                    msg(viewer, "§eClass: §f" + safe(ch.getCharacterClass()));
                } catch (Throwable ignored) {
                }
            }

            String prestige = prestigePoints(target);
            if (prestige != null) {
                msg(viewer, "§ePrestiged: §f" + prestige);
            }
            msg(viewer, "§eOverhaul scale: §f" + LmOverhaulScaledCombat.formatScale(data));

            try {
                msg(viewer, "§eDMZ Level: §f" + data.getLevel());
            } catch (Throwable ignored) {
            }
            try {
                msg(viewer, "§eMax Health: §f" + format(target.m_21233_())); // getMaxHealth
            } catch (Throwable ignored) {
            }
            try {
                msg(viewer, "§eMax Energy / Ki: §f" + format(DmzResourcePoolClamp.displayMaxEnergy(data)));
            } catch (Throwable ignored) {
            }

            msg(viewer, "§6--- Core Stats ---");
            Stats stats = data.getStats();
            if (stats != null) {
                try {
                    msg(viewer, "§cSTR: §f" + format(LmOverhaulScaledCombat.effectiveInvested(data, "STR"))
                            + " §8(" + stats.getStrength() + " invested)");
                } catch (Throwable ignored) {
                }
                try {
                    // Script labeled SKP via getSpirit(); DMZ API is strike power.
                    msg(viewer, "§bSKP: §f" + format(LmOverhaulScaledCombat.effectiveInvested(data, "SKP"))
                            + " §8(" + stats.getStrikePower() + " invested)");
                } catch (Throwable ignored) {
                }
                try {
                    msg(viewer, "§aRES: §f" + format(LmOverhaulScaledCombat.effectiveInvested(data, "RES"))
                            + " §8(" + stats.getResistance() + " invested)");
                } catch (Throwable ignored) {
                }
                try {
                    msg(viewer, "§dVIT: §f" + format(LmOverhaulScaledCombat.effectiveInvested(data, "VIT"))
                            + " §8(" + stats.getVitality() + " invested)");
                } catch (Throwable ignored) {
                }
                try {
                    msg(viewer, "§ePWR: §f" + format(LmOverhaulScaledCombat.effectiveInvested(data, "PWR"))
                            + " §8(" + stats.getKiPower() + " invested)");
                } catch (Throwable ignored) {
                }
                try {
                    msg(viewer, "§3ENE: §f" + stats.getEnergy());
                } catch (Throwable ignored) {
                }
            }

            msg(viewer, "§6--- Damage / Defense ---");
            try {
                msg(viewer, "§cMelee Damage: §f" + format(LmOverhaulScaledCombat.melee(data)));
            } catch (Throwable ignored) {
            }
            try {
                msg(viewer, "§cStrike Damage: §f" + format(LmOverhaulScaledCombat.strike(data)));
            } catch (Throwable ignored) {
            }
            try {
                msg(viewer, "§bKi Damage: §f" + format(LmOverhaulScaledCombat.ki(data)));
            } catch (Throwable ignored) {
            }
            try {
                msg(viewer, "§aDefense: §f" + format(LmOverhaulScaledCombat.defense(data)));
            } catch (Throwable ignored) {
            }
            try {
                msg(viewer, "§dMax Stamina: §f" + format(DmzResourcePoolClamp.displayMaxStamina(data)));
            } catch (Throwable ignored) {
            }

            msg(viewer, "§6--- Skills ---");
            Skills skills = data.getSkills();
            if (skills != null) {
                try {
                    msg(viewer, "§5potentialunlock: §f" + skills.getSkillLevel("potentialunlock"));
                } catch (Throwable ignored) {
                }
                try {
                    Map<String, Skill> all = skills.getAllSkills();
                    if (all != null && !all.isEmpty()) {
                        List<String> names = new ArrayList<>(all.keySet());
                        names.sort(Comparator.naturalOrder());
                        for (String skillName : names) {
                            int level = skills.getSkillLevel(skillName);
                            if (level > 0) {
                                msg(viewer, "§7" + skillName + ": §f" + level);
                            }
                        }
                    }
                } catch (Throwable ignored) {
                }
            }

            msg(viewer, "§6============================");
            SystemTelemetry.log(
                    "progression",
                    "stat_check",
                    viewer,
                    target,
                    SystemTelemetry.fields("target", name)
            );
        } catch (Throwable ignored) {
        }
    }

    private static String prestigePoints(ServerPlayer target) {
        try {
            Object bukkit = target.getClass().getMethod("getBukkitEntity").invoke(target);
            if (bukkit == null) {
                return null;
            }
            // CNPC API: Player.getFactionPoints(int) on the CustomNPCs wrapper when present.
            try {
                Object pts = bukkit.getClass().getMethod("getFactionPoints", int.class).invoke(bukkit, 4);
                if (pts != null) {
                    return String.valueOf(pts);
                }
            } catch (NoSuchMethodException ignored) {
            }
            // Soft: NpcAPI / CustomNPCs player data if exposed elsewhere — best-effort only.
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String safe(String s) {
        return s == null || s.isBlank() ? "?" : s;
    }

    private static String format(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return "?";
        }
        if (Math.rint(v) == v) {
            return String.valueOf((long) v);
        }
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    private static void msg(ServerPlayer player, String text) {
        try {
            player.m_213846_(Component.m_237113_(text));
        } catch (Throwable ignored) {
        }
    }
}
