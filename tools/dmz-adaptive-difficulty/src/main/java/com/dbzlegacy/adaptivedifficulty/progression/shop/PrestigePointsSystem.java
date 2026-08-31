package com.dbzlegacy.adaptivedifficulty.progression.shop;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionData;
import com.dbzlegacy.adaptivedifficulty.progression.bridge.FabledBridge;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dragonminez.common.stats.skills.Skills;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Prestige Points shop: turn in held prestiges for points, spend on DMZ skill levels,
 * Permanent Majin / Mutant, and hard-stat breakthroughs past the 100k body.
 *
 * <p>Turn-in: 1 point per prestige + 1 bonus per 3 turned in ({@code N + floor(N/3)}).
 * Skills: 1 point = +1 permanent skill level (survives prestige reset).
 * Majin/Mutant: 5 points each, mutually exclusive; unpurchase free (no refund).
 * Breakthroughs: grant hard stats worth +10k level power each (max 5 → ~150k equivalent)
 * without raising the server-wide DMZ {@code maxValue} (stays 100k for everyone).
 * Costs 15, 20, 25, 30, 35.
 */
public final class PrestigePointsSystem {
    public static final int BASE_LEVEL_CAP = 100_000;
    public static final int BREAKTHROUGH_STEP = 10_000;
    public static final int MAX_BREAKTHROUGHS = 5;
    public static final int ABSOLUTE_LEVEL_CAP = BASE_LEVEL_CAP + MAX_BREAKTHROUGHS * BREAKTHROUGH_STEP;
    public static final int FORM_COST = 5;
    public static final int SKILL_POINT_COST = 1;
    /**
     * DMZ level ≈ totalHardStats / 6 when maxTotalStats = maxValue×6.
     * One breakthrough (+10k level equivalent) → +10k on each of the 6 hard stats.
     */
    public static final int HARD_STAT_PER_BREAKTHROUGH = 10_000;

    private static final String KEY_POINTS = "prestige_points";
    private static final String KEY_BREAKTHROUGHS = "pp_level_breakthroughs";
    /** How many breakthroughs' hard-stat grants are currently on this body. */
    private static final String KEY_BT_HARD_APPLIED = "pp_bt_hard_applied";
    private static final String KEY_MAJIN = "pp_perm_majin";
    private static final String KEY_MUTANT = "pp_perm_mutant";
    private static final String KEY_SKILL_PREFIX = "pp_skill_";

    private static final String[] HARD_STAT_IDS = {
            "str", "skp", "res", "vit", "pwr", "ene"
    };

    private static final String SKILL_MAJIN = "Permanent Majin";
    private static final String SKILL_MUTANT = "Permanent Mutant";
    private static final String NS_KEY = "legacymechanics";
    private static final String NS_PATH = "prestige-points";

    /** Purchasable DMZ skills → display name + soft cap. */
    public static final Map<String, SkillOffer> SKILL_OFFERS = new LinkedHashMap<>();

    static {
        SKILL_OFFERS.put("meditation", new SkillOffer("meditation", "Meditation", 10));
        SKILL_OFFERS.put("fly", new SkillOffer("fly", "Fly", 10));
        SKILL_OFFERS.put("sprint", new SkillOffer("sprint", "Sprint", 10));
        SKILL_OFFERS.put("jump", new SkillOffer("jump", "Jump", 10));
        SKILL_OFFERS.put("potentialunlock", new SkillOffer("potentialunlock", "Potential Unlock", 30));
    }

    private PrestigePointsSystem() {}

    public record SkillOffer(String id, String label, int maxLevel) {}

    // ── Points balance ─────────────────────────────────────────────────

    public static int getPoints(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, (int) ProgressionData.storedGetLong(player, KEY_POINTS, 0L));
    }

    public static void setPoints(ServerPlayer player, int points) {
        if (player == null) {
            return;
        }
        ProgressionData.storedPut(player, KEY_POINTS, Math.max(0, points));
    }

    /** True when the player can afford {@code amount} prestige points. */
    public static boolean hasPoints(ServerPlayer player, int amount) {
        return player != null && amount >= 0 && getPoints(player) >= amount;
    }

    /**
     * Spend prestige points for external shops / NPCs.
     * @return {@code true} if deducted; {@code false} if insufficient or invalid
     */
    public static boolean trySpend(ServerPlayer player, int amount, String reason) {
        if (player == null || amount <= 0) {
            if (player != null) {
                ProgressionData.tempPut(player, "pp_last_spend_ok", "false");
            }
            return false;
        }
        int points = getPoints(player);
        if (points < amount) {
            ProgressionData.tempPut(player, "pp_last_spend_ok", "false");
            return false;
        }
        setPoints(player, points - amount);
        ProgressionData.tempPut(player, "pp_last_spend_ok", "true");
        SystemTelemetry.log("prestige_points", "spend", player, null, Map.of(
                "amount", amount,
                "reason", reason == null ? "" : reason,
                "points", getPoints(player)
        ));
        return true;
    }

    /** Grant prestige points (admin / quest rewards). */
    public static void grantPoints(ServerPlayer player, int amount, String reason) {
        if (player == null || amount <= 0) {
            return;
        }
        int balance = getPoints(player) + amount;
        setPoints(player, balance);
        SystemTelemetry.log("prestige_points", "grant", player, null, Map.of(
                "amount", amount,
                "reason", reason == null ? "" : reason,
                "points", balance
        ));
    }

    public static String spendForNpc(ServerPlayer player, int amount, String reason) {
        if (player == null) {
            return "§cPlayers only.";
        }
        if (amount <= 0) {
            return "§cAmount must be positive.";
        }
        if (!trySpend(player, amount, reason)) {
            return "§cNeed §e" + amount + " §cprestige points (have §e" + getPoints(player) + "§c).";
        }
        String tag = reason == null || reason.isBlank() ? "shop" : reason.trim();
        return "§aSpent §e" + amount + " §apoint" + (amount == 1 ? "" : "s")
                + " §7(" + tag + ") · Balance: §e" + getPoints(player);
    }

    public static String grantForNpc(ServerPlayer player, int amount, String reason) {
        if (player == null) {
            return "§cPlayers only.";
        }
        if (amount <= 0) {
            return "§cAmount must be positive.";
        }
        grantPoints(player, amount, reason);
        return "§aGranted §e" + amount + " §aprestige point" + (amount == 1 ? "" : "s")
                + " · Balance: §e" + getPoints(player);
    }

    public static int pointsForTurnIn(int amount) {
        int n = Math.max(0, amount);
        return n + (n / 3);
    }

    /**
     * Turn in {@code amount} held prestiges for points.
     * Also lowers Fabled Prestige class so faction sync cannot restore held.
     */
    public static String turnIn(ServerPlayer player, int amount) {
        if (player == null) {
            return "§cPlayers only.";
        }
        int held = PrestigeSystem.getHeld(player);
        int want = Math.max(0, amount);
        if (want <= 0) {
            return "§cChoose how many prestiges to turn in.";
        }
        if (want > held) {
            return "§cYou only hold §6" + held + " §cprestige" + (held == 1 ? "" : "s") + ".";
        }
        int gained = pointsForTurnIn(want);
        int newHeld = held - want;
        PrestigeSystem.setHeldPublic(player, newHeld);
        reducePrestigeClass(player, want);
        int balance = getPoints(player) + gained;
        setPoints(player, balance);
        SystemTelemetry.log("prestige_points", "turn_in", player, null, Map.of(
                "amount", want,
                "gained", gained,
                "held", newHeld,
                "points", balance
        ));
        return "§aTurned in §6" + want + " §aprestige" + (want == 1 ? "" : "s")
                + " §7→ §e+" + gained + " §7point" + (gained == 1 ? "" : "s")
                + " §8(+" + (gained - want) + " bonus)"
                + "\n§7Balance: §e" + balance + " §7· Held: §6" + newHeld;
    }

    // ── Skill upgrades ─────────────────────────────────────────────────

    public static int getPurchasedSkillLevels(ServerPlayer player, String skillId) {
        if (player == null || skillId == null || skillId.isBlank()) {
            return 0;
        }
        return Math.max(0, (int) ProgressionData.storedGetLong(
                player, KEY_SKILL_PREFIX + skillId.toLowerCase(Locale.ROOT), 0L));
    }

    public static String buySkillLevel(ServerPlayer player, String skillId) {
        if (player == null) {
            return "§cPlayers only.";
        }
        SkillOffer offer = SKILL_OFFERS.get(skillId == null ? "" : skillId.toLowerCase(Locale.ROOT));
        if (offer == null) {
            return "§cUnknown skill. Use: meditation, fly, sprint, jump, potentialunlock.";
        }
        int purchased = getPurchasedSkillLevels(player, offer.id);
        if (purchased >= offer.maxLevel) {
            return "§c" + offer.label + " prestige upgrades are maxed (§f" + offer.maxLevel + "§c).";
        }
        Skills skills = DmzSkillUtil.skills(player);
        if (skills == null) {
            return "§cCould not read your DMZ skills.";
        }
        DmzSkillUtil.ensureRegistered(skills, offer.id, offer.maxLevel);
        int current = DmzSkillUtil.level(skills, offer.id);
        int max = DmzSkillUtil.maxLevel(skills, offer.id, offer.maxLevel);
        if (current >= max && purchased >= max) {
            return "§c" + offer.label + " is already at max level.";
        }
        int points = getPoints(player);
        if (points < SKILL_POINT_COST) {
            return "§cNeed §e" + SKILL_POINT_COST + " §cpoint (have §e" + points + "§c).";
        }
        setPoints(player, points - SKILL_POINT_COST);
        int nextPurchased = purchased + 1;
        ProgressionData.storedPut(player, KEY_SKILL_PREFIX + offer.id, nextPurchased);
        int newLevel = Math.min(max, Math.max(current + 1, nextPurchased));
        DmzSkillUtil.setLevel(skills, offer.id, newLevel);
        DmzSkillUtil.sync(player);
        SystemTelemetry.log("prestige_points", "buy_skill", player, null, Map.of(
                "skill", offer.id,
                "purchased", nextPurchased,
                "level", newLevel,
                "points", getPoints(player)
        ));
        return "§a+" + SKILL_POINT_COST + " §7" + offer.label + " → §fLv " + newLevel
                + " §8(prestige floor §f" + nextPurchased + "§8)"
                + "\n§7Points left: §e" + getPoints(player);
    }

    /** Re-apply purchased skill floors + breakthrough hard stats after prestige reset / on login. */
    public static void reapplySkillBonuses(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Skills skills = DmzSkillUtil.skills(player);
        if (skills != null) {
            boolean changed = false;
            for (SkillOffer offer : SKILL_OFFERS.values()) {
                int purchased = getPurchasedSkillLevels(player, offer.id);
                if (purchased <= 0) {
                    continue;
                }
                DmzSkillUtil.ensureRegistered(skills, offer.id, offer.maxLevel);
                int current = DmzSkillUtil.level(skills, offer.id);
                int max = DmzSkillUtil.maxLevel(skills, offer.id, offer.maxLevel);
                int target = Math.min(max, Math.max(current, purchased));
                if (target > current) {
                    DmzSkillUtil.setLevel(skills, offer.id, target);
                    changed = true;
                }
            }
            if (changed) {
                DmzSkillUtil.sync(player);
            }
        }
        reapplyForms(player);
        syncBreakthroughHardStats(player);
    }

    // ── Permanent Majin / Mutant ────────────────────────────────────────

    public static boolean hasMajin(ServerPlayer player) {
        return player != null && ProgressionData.storedGetBool(player, KEY_MAJIN);
    }

    public static boolean hasMutant(ServerPlayer player) {
        return player != null && ProgressionData.storedGetBool(player, KEY_MUTANT);
    }

    public static String buyMajin(ServerPlayer player) {
        return buyForm(player, true);
    }

    public static String buyMutant(ServerPlayer player) {
        return buyForm(player, false);
    }

    public static String unbuyMajin(ServerPlayer player) {
        return unbuyForm(player, true);
    }

    public static String unbuyMutant(ServerPlayer player) {
        return unbuyForm(player, false);
    }

    private static String buyForm(ServerPlayer player, boolean majin) {
        if (player == null) {
            return "§cPlayers only.";
        }
        String label = majin ? "Permanent Majin" : "Permanent Mutant";
        if (majin ? hasMajin(player) : hasMutant(player)) {
            return "§eYou already own §f" + label + "§e.";
        }
        int points = getPoints(player);
        if (points < FORM_COST) {
            return "§cNeed §e" + FORM_COST + " §cpoints (have §e" + points + "§c).";
        }
        // XOR: drop the other form with no refund before purchasing.
        if (majin && hasMutant(player)) {
            clearForm(player, false);
        } else if (!majin && hasMajin(player)) {
            clearForm(player, true);
        }
        setPoints(player, points - FORM_COST);
        ProgressionData.storedPutBool(player, majin ? KEY_MAJIN : KEY_MUTANT, true);
        grantFormSkill(player, majin);
        runDmzEffect(player, majin ? "majin" : "mutant", true);
        SystemTelemetry.log("prestige_points", majin ? "buy_majin" : "buy_mutant", player, null, Map.of(
                "points", getPoints(player)
        ));
        return "§aUnlocked §f" + label + " §7(§e-" + FORM_COST + " §7points)"
                + "\n§7Points left: §e" + getPoints(player)
                + "\n§8Switch later via unpurchase (no refund) then buy the other.";
    }

    private static String unbuyForm(ServerPlayer player, boolean majin) {
        if (player == null) {
            return "§cPlayers only.";
        }
        String label = majin ? "Permanent Majin" : "Permanent Mutant";
        if (!(majin ? hasMajin(player) : hasMutant(player))) {
            return "§eYou do not own §f" + label + "§e.";
        }
        clearForm(player, majin);
        SystemTelemetry.log("prestige_points", majin ? "unbuy_majin" : "unbuy_mutant", player, null, Map.of());
        return "§7Removed §f" + label + "§7. §cNo points refunded."
                + "\n§7Points: §e" + getPoints(player);
    }

    private static void clearForm(ServerPlayer player, boolean majin) {
        ProgressionData.storedPutBool(player, majin ? KEY_MAJIN : KEY_MUTANT, false);
        removeFormSkill(player, majin);
        runDmzEffect(player, majin ? "majin" : "mutant", false);
    }

    private static void reapplyForms(ServerPlayer player) {
        if (hasMajin(player)) {
            grantFormSkill(player, true);
            runDmzEffect(player, "majin", true);
        } else {
            removeFormSkill(player, true);
        }
        if (hasMutant(player)) {
            grantFormSkill(player, false);
            runDmzEffect(player, "mutant", true);
        } else {
            removeFormSkill(player, false);
        }
    }

    // ── Hard-stat breakthroughs (personal; server maxValue stays 100k) ─

    public static int getBreakthroughs(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        return Math.max(0, Math.min(MAX_BREAKTHROUGHS,
                (int) ProgressionData.storedGetLong(player, KEY_BREAKTHROUGHS, 0L)));
    }

    public static int breakthroughCost(int nextIndex) {
        // nextIndex 1..5 → 15, 20, 25, 30, 35
        if (nextIndex < 1 || nextIndex > MAX_BREAKTHROUGHS) {
            return Integer.MAX_VALUE;
        }
        return 15 + (nextIndex - 1) * 5;
    }

    /** Equivalent power level: 100k + breakthroughs×10k (DMZ displayed level stays ≤100k). */
    public static int effectiveMaxLevel(ServerPlayer player) {
        return Math.min(ABSOLUTE_LEVEL_CAP,
                BASE_LEVEL_CAP + getBreakthroughs(player) * BREAKTHROUGH_STEP);
    }

    public static int effectiveMaxLevel(int breakthroughs) {
        int n = Math.max(0, Math.min(MAX_BREAKTHROUGHS, breakthroughs));
        return Math.min(ABSOLUTE_LEVEL_CAP, BASE_LEVEL_CAP + n * BREAKTHROUGH_STEP);
    }

    public static String buyBreakthrough(ServerPlayer player) {
        if (player == null) {
            return "§cPlayers only.";
        }
        int current = getBreakthroughs(player);
        if (current >= MAX_BREAKTHROUGHS) {
            return "§cHard-stat breakthroughs maxed (§f~"
                    + DmzRewards.formatWhole(ABSOLUTE_LEVEL_CAP) + " §cequivalent).";
        }
        int next = current + 1;
        int cost = breakthroughCost(next);
        int points = getPoints(player);
        if (points < cost) {
            return "§cNeed §e" + cost + " §cpoints for breakthrough §f#" + next
                    + " §c(have §e" + points + "§c).";
        }
        setPoints(player, points - cost);
        ProgressionData.storedPut(player, KEY_BREAKTHROUGHS, next);
        int granted = grantHardStatBatches(player, 1);
        if (granted > 0) {
            int applied = getHardStatsApplied(player) + granted;
            ProgressionData.storedPut(player, KEY_BT_HARD_APPLIED, applied);
        }
        int equiv = effectiveMaxLevel(player);
        SystemTelemetry.log("prestige_points", "breakthrough", player, null, Map.of(
                "breakthrough", next,
                "equiv", equiv,
                "hardPerStat", HARD_STAT_PER_BREAKTHROUGH,
                "cost", cost,
                "points", getPoints(player)
        ));
        return "§aBreakthrough §f#" + next + "§a — hard stats +"
                + DmzRewards.formatWhole(HARD_STAT_PER_BREAKTHROUGH)
                + " §ato each core stat"
                + "\n§7Equivalent power ~§f" + DmzRewards.formatWhole(equiv)
                + " §8(server level cap stays §f100000§8)"
                + "\n§7Cost §e" + cost + " §7· Points left: §e" + getPoints(player);
    }

    /** Call before/with prestige reset so hard-stat grants are re-seeded onto the new body. */
    public static void markHardStatsCleared(ServerPlayer player) {
        if (player != null) {
            ProgressionData.storedPut(player, KEY_BT_HARD_APPLIED, 0L);
        }
    }

    private static int getHardStatsApplied(ServerPlayer player) {
        return Math.max(0, (int) ProgressionData.storedGetLong(player, KEY_BT_HARD_APPLIED, 0L));
    }

    /**
     * Ensure owned breakthroughs have matching hard-stat grants on the current body.
     * Safe to call repeatedly — only grants the missing delta.
     */
    public static void syncBreakthroughHardStats(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int owned = getBreakthroughs(player);
        int applied = getHardStatsApplied(player);
        if (owned <= applied) {
            return;
        }
        int missing = owned - applied;
        int granted = grantHardStatBatches(player, missing);
        if (granted > 0) {
            ProgressionData.storedPut(player, KEY_BT_HARD_APPLIED, applied + granted);
        }
    }

    /**
     * Add {@code batches} × {@link #HARD_STAT_PER_BREAKTHROUGH} to each hard core stat.
     * Uses real {@link com.dragonminez.common.stats.character.Stats} — not BonusStats.
     */
    private static int grantHardStatBatches(ServerPlayer player, int batches) {
        if (player == null || batches <= 0) {
            return 0;
        }
        var data = com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.stats(player);
        if (data == null) {
            return 0;
        }
        com.dragonminez.common.stats.character.Stats stats;
        try {
            stats = data.getStats();
        } catch (Throwable t) {
            return 0;
        }
        if (stats == null) {
            return 0;
        }
        int per = HARD_STAT_PER_BREAKTHROUGH * batches;
        // Prefer named adders; fall back to addStat ids used by DMZ.
        try {
            stats.addStrength(per);
            stats.addStrikePower(per);
            stats.addResistance(per);
            stats.addVitality(per);
            stats.addKiPower(per);
            stats.addEnergy(per);
        } catch (Throwable t) {
            try {
                for (String id : HARD_STAT_IDS) {
                    stats.addStat(id, per);
                }
            } catch (Throwable t2) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] breakthrough hard-stat grant failed: {}",
                        AdaptiveDifficultyMod.MOD_ID, t2.toString());
                return 0;
            }
        }
        try {
            com.dragonminez.common.network.NetworkHandler.sendToTrackingEntityAndSelf(
                    new com.dragonminez.common.network.S2C.StatsSyncS2C(player), player);
        } catch (Throwable ignored) {
        }
        return batches;
    }

    // ── Login / post-prestige ──────────────────────────────────────────

    private static final String KEY_REAPPLY_AT = "pp_reapply_at_ms";

    public static void onLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        // Stagger reapply so DMZ / Fabled finish loading.
        ProgressionData.tempPut(player, KEY_REAPPLY_AT, System.currentTimeMillis() + 2000L);
        reapplySkillBonuses(player);
    }

    public static void scheduleReapplyAfterPrestige(ServerPlayer player) {
        if (player == null) {
            return;
        }
        markHardStatsCleared(player);
        long now = System.currentTimeMillis();
        ProgressionData.tempPut(player, KEY_REAPPLY_AT, now + 3000L);
        MinecraftServer server = player.m_20194_();
        if (server != null) {
            server.execute(() -> {
                if (player.m_6084_()) {
                    reapplySkillBonuses(player);
                }
            });
        }
    }

    /** Called from shop pulse — drains delayed reapply markers. */
    public static void pulsePlayer(ServerPlayer player, long nowMs) {
        if (player == null) {
            return;
        }
        long at = ProgressionData.tempGetLong(player, KEY_REAPPLY_AT, 0L);
        if (at <= 0L || nowMs < at) {
            return;
        }
        ProgressionData.tempRemove(player, KEY_REAPPLY_AT);
        reapplySkillBonuses(player);
    }

    // ── Fabled / DMZ helpers ───────────────────────────────────────────

    private static void reducePrestigeClass(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return;
        }
        String name = player.m_6302_();
        try {
            server.m_129892_().m_230957_(
                    server.m_129893_(),
                    "class level " + name + " take " + amount + " Prestige"
            );
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] prestige points class take soft-fail: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }

    private static void grantFormSkill(ServerPlayer player, boolean majin) {
        String skillName = majin ? SKILL_MAJIN : SKILL_MUTANT;
        Object fabledData = FabledBridge.fabledData(player);
        if (fabledData == null) {
            return;
        }
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return;
        }
        Object skill = findSkill(fabledClass, skillName);
        if (skill == null) {
            return;
        }
        Object playerClass = null;
        try {
            playerClass = fabledData.getClass().getMethod("getMainClass").invoke(fabledData);
        } catch (Throwable ignored) {
        }
        if (!addSkillExternally(fabledData, skill, playerClass, 1)) {
            try {
                for (Method m : fabledData.getClass().getMethods()) {
                    if (!"giveSkill".equals(m.getName())) {
                        continue;
                    }
                    if (m.getParameterCount() == 2 && playerClass != null) {
                        m.invoke(fabledData, skill, playerClass);
                        break;
                    }
                    if (m.getParameterCount() == 1) {
                        m.invoke(fabledData, skill);
                        break;
                    }
                }
            } catch (Throwable ignored) {
            }
        }
        forceUp(fabledData, skillName, 1);
    }

    private static void removeFormSkill(ServerPlayer player, boolean majin) {
        String skillName = majin ? SKILL_MAJIN : SKILL_MUTANT;
        Object fabledData = FabledBridge.fabledData(player);
        if (fabledData == null) {
            return;
        }
        Class<?> fabledClass = FabledBridge.fabledClass();
        if (fabledClass == null) {
            return;
        }
        Object skill = findSkill(fabledClass, skillName);
        if (skill == null) {
            return;
        }
        try {
            ClassLoader loader = fabledData.getClass().getClassLoader();
            Class<?> namespacedKey = Class.forName("org.bukkit.NamespacedKey", true, loader);
            Object key = namespacedKey.getConstructor(String.class, String.class)
                    .newInstance(NS_KEY, NS_PATH);
            for (Method m : fabledData.getClass().getMethods()) {
                if ("removeSkillExternally".equals(m.getName()) && m.getParameterCount() == 2) {
                    m.invoke(fabledData, skill, key);
                    break;
                }
            }
        } catch (Throwable ignored) {
        }
        // Also try refund/remove APIs if external remove missed a class-owned grant.
        try {
            Object playerSkill = fabledData.getClass()
                    .getMethod("getSkill", String.class)
                    .invoke(fabledData, skillName);
            if (playerSkill != null) {
                for (Method m : fabledData.getClass().getMethods()) {
                    if ("removeSkill".equals(m.getName()) && m.getParameterCount() == 1) {
                        m.invoke(fabledData, playerSkill);
                        break;
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Object findSkill(Class<?> fabledClass, String skillName) {
        try {
            Object skill = fabledClass.getMethod("getSkill", String.class).invoke(null, skillName);
            if (skill != null) {
                return skill;
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean addSkillExternally(
            Object fabledData, Object skill, Object playerClass, int level
    ) {
        try {
            ClassLoader loader = fabledData.getClass().getClassLoader();
            Class<?> namespacedKey = Class.forName("org.bukkit.NamespacedKey", true, loader);
            Constructor<?> ctor = namespacedKey.getConstructor(String.class, String.class);
            Object key = ctor.newInstance(NS_KEY, NS_PATH);
            for (Method m : fabledData.getClass().getMethods()) {
                if (!"addSkillExternally".equals(m.getName()) || m.getParameterCount() != 4) {
                    continue;
                }
                m.invoke(fabledData, skill, playerClass, key, level);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void forceUp(Object fabledData, String skillName, int targetLevel) {
        try {
            Object playerSkill = fabledData.getClass()
                    .getMethod("getSkill", String.class)
                    .invoke(fabledData, skillName);
            if (playerSkill == null) {
                return;
            }
            int current = 0;
            Object lv = playerSkill.getClass().getMethod("getLevel").invoke(playerSkill);
            if (lv instanceof Number n) {
                current = n.intValue();
            }
            if (current >= targetLevel) {
                return;
            }
            int delta = targetLevel - Math.max(0, current);
            try {
                fabledData.getClass()
                        .getMethod("forceUpSkill", playerSkill.getClass(), int.class)
                        .invoke(fabledData, playerSkill, delta);
            } catch (NoSuchMethodException e) {
                playerSkill.getClass().getMethod("setLevel", int.class).invoke(playerSkill, targetLevel);
            }
        } catch (Throwable ignored) {
        }
    }

    private static void runDmzEffect(ServerPlayer player, String effect, boolean give) {
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return;
        }
        String name = player.m_6302_();
        String cmd = give
                ? "dmzeffect give " + name + " " + effect + " -1"
                : "dmzeffect remove " + name + " " + effect;
        try {
            server.m_129892_().m_230957_(server.m_129893_(), cmd);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] prestige points dmzeffect soft-fail: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
    }
}
