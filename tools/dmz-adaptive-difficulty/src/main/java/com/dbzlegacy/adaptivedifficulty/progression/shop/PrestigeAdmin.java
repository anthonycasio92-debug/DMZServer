package com.dbzlegacy.adaptivedifficulty.progression.shop;

import com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeFactionSync;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff tools for adjusting prestige state (held, completed, points, breakthroughs, Fabled).
 * Points are the shared wallet for every prestige shop (skills, effects, breakthroughs, tiers).
 */
public final class PrestigeAdmin {
    private PrestigeAdmin() {}

    public static String help() {
        return "§6§l/padmin §8(or /prestige admin)\n"
                + "§e/padmin info [player]\n"
                + "§e/padmin points <player> <set|add|remove> <n>\n"
                + "§e/padmin addpoints <player> <n> §8— same wallet all shops use\n"
                + "§e/padmin removepoints <player> <n>\n"
                + "§e/padmin held <player> <set|add|remove> <n>\n"
                + "§e/padmin completed <player> <set|add|remove> <n>\n"
                + "§e/padmin breakthroughs <player> <set|add|remove> <n>\n"
                + "§e/padmin fabled <player> <set|add|take> <n>\n"
                + "§e/padmin tier <player> give <1-7> §8— grant permanent T1…Tn\n"
                + "§e/padmin tier <player> clear <1-7|all>\n"
                + "§e/padmin sync <player>\n"
                + "§8Points spend in: Skills · Effects · Breakthroughs · Difficulty Tiers.";
    }

    public static String info(ServerPlayer target) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        int held = PrestigeSystem.getHeld(target);
        int completed = PrestigeSystem.getCompleted(target);
        int points = PrestigePointsSystem.getPoints(target);
        int bt = PrestigePointsSystem.getBreakthroughs(target);
        int cap = PrestigePointsSystem.effectiveMaxLevel(target);
        int need = PrestigeSystem.requiredLevel(target);
        int fabled = PrestigeSkillSync.fabledPrestigeLevel(target);
        int highestTier = PrestigePointsSystem.highestPurchasedTier(target);
        return "§6Prestige §f" + target.m_6302_() + "\n"
                + "§7Completed: §f" + completed
                + " §8| §7Held: §6" + held + "§7/§f" + PrestigeSystem.maxHeld() + "\n"
                + "§7Points: §e" + points
                + " §8| §7Breakthroughs: §b" + bt + "§7/§f" + PrestigePointsSystem.MAX_BREAKTHROUGHS + "\n"
                + "§7Personal cap: §f" + DmzRewards.formatWhole(cap)
                + " §8| §7Next prestige need: §e" + DmzRewards.formatWhole(need) + "\n"
                + "§7Fabled Prestige class: §f" + fabled
                + " §8| §7Permanent tiers: §fT" + highestTier;
    }

    public static String adjustHeld(ServerPlayer target, String mode, int amount) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        int before = PrestigeSystem.getHeld(target);
        Integer next = applyMode(before, mode, amount, 0, PrestigeSystem.maxHeld());
        if (next == null) {
            return "§cUsage: held <player> <set|add|remove> <n>";
        }
        PrestigeSystem.setHeldPublic(target, next);
        log(target, "admin_held", mode, before, next);
        return "§aHeld prestige §f" + before + " §7→ §f" + PrestigeSystem.getHeld(target)
                + " §8(" + target.m_6302_() + ")";
    }

    public static String adjustCompleted(ServerPlayer target, String mode, int amount) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        int before = PrestigeSystem.getCompleted(target);
        Integer next = applyMode(before, mode, amount, 0, 10_000);
        if (next == null) {
            return "§cUsage: completed <player> <set|add|remove> <n>";
        }
        PrestigeSystem.setCompletedPublic(target, next);
        log(target, "admin_completed", mode, before, next);
        return "§aCompleted prestige §f" + before + " §7→ §f" + PrestigeSystem.getCompleted(target)
                + " §8| next need §e" + DmzRewards.formatWhole(PrestigeSystem.requiredLevel(target))
                + " §8(" + target.m_6302_() + ")";
    }

    public static String adjustPoints(ServerPlayer target, String mode, int amount) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        int before = PrestigePointsSystem.getPoints(target);
        Integer next = applyMode(before, mode, amount, 0, 1_000_000);
        if (next == null) {
            return "§cUsage: points <player> <set|add|remove> <n>\n"
                    + "§8Or: addpoints <player> <n> · removepoints <player> <n>";
        }
        PrestigePointsSystem.setPoints(target, next);
        log(target, "admin_points", mode, before, next);
        return "§aPrestige points §e" + before + " §7→ §e" + PrestigePointsSystem.getPoints(target)
                + " §8(" + target.m_6302_() + ")"
                + "\n§8Wallet for Skills · Effects · Breakthroughs · Difficulty Tiers.";
    }

    public static String adjustBreakthroughs(ServerPlayer target, String mode, int amount) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        int before = PrestigePointsSystem.getBreakthroughs(target);
        Integer next = applyMode(before, mode, amount, 0, PrestigePointsSystem.MAX_BREAKTHROUGHS);
        if (next == null) {
            return "§cUsage: breakthroughs <player> <set|add|remove> <n>";
        }
        PrestigePointsSystem.setBreakthroughs(target, next);
        int cap = PrestigePointsSystem.effectiveMaxLevel(target);
        log(target, "admin_breakthroughs", mode, before, next);
        return "§aBreakthroughs §b" + before + " §7→ §b" + PrestigePointsSystem.getBreakthroughs(target)
                + " §8| cap §f" + DmzRewards.formatWhole(cap)
                + " §8| next prestige need §e" + DmzRewards.formatWhole(PrestigeSystem.requiredLevel(target))
                + " §8(" + target.m_6302_() + ")";
    }

    public static String adjustFabled(ServerPlayer target, String mode, int amount) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        String m = mode == null ? "" : mode.toLowerCase(Locale.ROOT).trim();
        int before = PrestigeSkillSync.fabledPrestigeLevel(target);
        int after;
        switch (m) {
            case "set" -> {
                int want = Math.max(1, amount);
                if (want > before) {
                    PrestigeSkillSync.addPrestigeLevels(target, want - before);
                } else if (want < before) {
                    PrestigeSkillSync.takePrestigeLevels(target, before - want);
                } else {
                    PrestigeFactionSync.forceSync(target);
                    PrestigeSkillSync.sync(target);
                }
            }
            case "add" -> {
                if (amount <= 0) {
                    return "§cAmount must be > 0.";
                }
                PrestigeSkillSync.addPrestigeLevels(target, amount);
            }
            case "take", "remove" -> {
                if (amount <= 0) {
                    return "§cAmount must be > 0.";
                }
                PrestigeSkillSync.takePrestigeLevels(target, amount);
            }
            default -> {
                return "§cUsage: fabled <player> <set|add|take> <n>";
            }
        }
        after = PrestigeSkillSync.fabledPrestigeLevel(target);
        log(target, "admin_fabled", m, before, after);
        return "§aFabled Prestige §f" + before + " §7→ §f" + after
                + " §8(" + target.m_6302_() + ")";
    }

    public static String adjustTier(ServerPlayer target, String mode, int tierId) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        String m = mode == null ? "" : mode.toLowerCase(Locale.ROOT).trim();
        return switch (m) {
            case "give", "grant", "set", "add" -> {
                String msg = PrestigePointsSystem.adminGrantTier(target, tierId);
                log(target, "admin_tier_give", m, 0, tierId);
                yield msg;
            }
            case "clear", "remove", "take" -> {
                String msg = PrestigePointsSystem.adminClearTier(target, tierId);
                log(target, "admin_tier_clear", m, tierId, 0);
                yield msg;
            }
            default -> "§cUsage: tier <player> give <1-7> | tier <player> clear <1-7|all>";
        };
    }

    public static String sync(ServerPlayer target) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        try {
            PrestigeSkillSync.sync(target);
        } catch (Throwable ignored) {
        }
        try {
            PrestigeFactionSync.forceSync(target);
        } catch (Throwable ignored) {
        }
        try {
            PrestigePointsSystem.reapplyTierUnlocks(target);
            com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.save(target);
        } catch (Throwable ignored) {
        }
        log(target, "admin_sync", "sync", 0, 0);
        return "§aSynced Fabled Prestige → DMZ skill + faction + permanent tiers for §f"
                + target.m_6302_();
    }

    private static Integer applyMode(int current, String mode, int amount, int min, int max) {
        if (mode == null) {
            return null;
        }
        String m = mode.toLowerCase(Locale.ROOT).trim();
        int next;
        switch (m) {
            case "set" -> next = amount;
            case "add" -> next = current + amount;
            case "remove", "take", "sub" -> next = current - amount;
            default -> {
                return null;
            }
        }
        return Math.max(min, Math.min(max, next));
    }

    private static void log(ServerPlayer target, String action, String mode, int before, int after) {
        SystemTelemetry.log("prestige_admin", action, target, null, Map.of(
                "mode", mode == null ? "" : mode,
                "before", before,
                "after", after
        ));
    }
}
