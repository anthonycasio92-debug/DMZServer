package com.dbzlegacy.adaptivedifficulty.progression.shop;

import com.dbzlegacy.adaptivedifficulty.command.LmCommandHelp;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeFactionSync;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff tools for adjusting prestige state and prestige-shop investments
 * (skill floors, difficulty tiers, breakthroughs, wallet points).
 */
public final class PrestigeAdmin {
    private PrestigeAdmin() {}

    public static String help() {
        return LmCommandHelp.prestigeAdmin();
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
        StringBuilder sb = new StringBuilder();
        sb.append("§6Prestige §f").append(target.m_6302_()).append('\n');
        sb.append("§7Completed: §f").append(completed)
                .append(" §8| §7Held: §6").append(held).append("§7/§f")
                .append(PrestigeSystem.maxHeld()).append('\n');
        sb.append("§7Points: §e").append(points)
                .append(" §8| §7Breakthroughs: §b").append(bt).append("§7/§f")
                .append(PrestigePointsSystem.MAX_BREAKTHROUGHS).append('\n');
        sb.append("§7Personal cap: §f").append(DmzRewards.formatWhole(cap))
                .append(" §8| §7Next prestige need: §e")
                .append(DmzRewards.formatWhole(need)).append('\n');
        sb.append("§7Fabled Prestige class: §f").append(fabled)
                .append(" §8| §7Permanent tiers: §fT").append(highestTier).append('\n');
        List<String> invested = PrestigePointsSystem.investedSkillSummary(target);
        if (invested.isEmpty()) {
            sb.append("§7Invested skills: §8none");
        } else {
            sb.append("§7Invested skills:\n");
            for (String line : invested) {
                sb.append("§8  · §7").append(line).append('\n');
            }
        }
        return sb.toString().trim();
    }

    public static String listSkills(ServerPlayer target) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        List<String> invested = PrestigePointsSystem.investedSkillSummary(target);
        StringBuilder sb = new StringBuilder();
        sb.append("§6Prestige skill floors §f").append(target.m_6302_()).append('\n');
        if (invested.isEmpty()) {
            sb.append("§8No prestige-invested skills.");
        } else {
            for (String line : invested) {
                sb.append("§8  · §7").append(line).append('\n');
            }
        }
        sb.append("\n§8Adjust: /padmin skill ").append(target.m_6302_())
                .append(" <id> set|add|remove <levels>");
        return sb.toString().trim();
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
        PrestigeSystem.setCompletedStaff(target, next);
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
                    + "§8Or: points set <n> · points <player> <n>\n"
                    + "§8Or: addpoints|setpoints|removepoints <player> <n>";
        }
        PrestigePointsSystem.setPoints(target, next);
        log(target, "admin_points", mode, before, next);
        return "§aPrestige points §e" + before + " §7→ §e" + PrestigePointsSystem.getPoints(target)
                + " §8(" + target.m_6302_() + ")"
                + "\n§8Wallet for Skills · Effects · Breakthroughs · Difficulty Tiers.";
    }

    public static String adjustSkill(
            ServerPlayer target, String skillId, String mode, int amount
    ) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        return PrestigePointsSystem.adminAdjustSkill(target, skillId, mode, amount);
    }

    public static String adjustBreakthroughs(ServerPlayer target, String mode, int amount) {
        if (target == null) {
            return "§cPlayer not online.";
        }
        int before = PrestigePointsSystem.getBreakthroughs(target);
        Integer next = applyMode(before, mode, amount, 0, PrestigePointsSystem.MAX_BREAKTHROUGHS);
        if (next == null) {
            return "§cUsage: breakthroughs <player> <set|add|remove> <0-5>";
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
        // Unified set/add/remove on highest permanent tier.
        if ("set".equals(m) || "add".equals(m) || "remove".equals(m) || "take".equals(m)
                || "sub".equals(m)) {
            String msg = PrestigePointsSystem.adminAdjustHighestTier(target, m, tierId);
            log(target, "admin_tier_adjust", m,
                    PrestigePointsSystem.highestPurchasedTier(target), tierId);
            return msg;
        }
        return switch (m) {
            case "give", "grant" -> {
                String msg = PrestigePointsSystem.adminGrantTier(target, tierId);
                log(target, "admin_tier_give", m, 0, tierId);
                yield msg;
            }
            case "clear" -> {
                String msg = PrestigePointsSystem.adminClearTier(target, tierId);
                log(target, "admin_tier_clear", m, tierId, 0);
                yield msg;
            }
            default -> "§cUsage: tier <player> <set|add|remove> <0-7>"
                    + "\n§c       tier <player> give <1-7> | clear <1-7|all>";
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
            PrestigePointsSystem.reapplySkillBonuses(target);
            PrestigePointsSystem.reapplyTierUnlocks(target);
            com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.save(target);
        } catch (Throwable ignored) {
        }
        log(target, "admin_sync", "sync", 0, 0);
        return "§aSynced Fabled Prestige → skills + faction + prestige shop floors for §f"
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
