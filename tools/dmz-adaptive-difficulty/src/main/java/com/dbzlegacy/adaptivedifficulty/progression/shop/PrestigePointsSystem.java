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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Prestige Points shop: turn in held prestiges for points, spend on DMZ skill levels,
 * Permanent Majin / Mutant, and personal level-cap breakthroughs.
 *
 * <p>Turn-in amounts: {@code 1, 2, 3, 6, 9} only.
 * Points: {@code N + T(N/3)} where {@code T(k)=k(k+1)/2}
 * (3→4, 6→9, 9→15).
 * Skills: 1 point = +1 permanent skill level for Skill Check skills only
 * (Natural + Saga — not ultimates / ki attacks). Survives prestige reset.
 * {@code potentialunlock} is the exception: 1 point = +2 levels.
 * Caps come from {@code skills.json} cost ladders.
 * Majin/Mutant: 5 points each, mutually exclusive; unpurchase free (no refund).
 * Breakthroughs: raise <b>your</b> DMZ level cap by +10k (max 5 → 150k) so you can
 * level normally into the new cap. Server {@code maxValue} is 150k (client UI/level
 * math); personal soft-locks keep everyone else at 100k. Costs 15, 20, 25, 30, 35.
 * Difficulty tiers: permanent unlock with prestige points —
 * T1–2 = 1pt, T3–4 = 2pt, T5–6 = 3pt, T7 = 4pt ({@code (tier+1)/2}).
 */
public final class PrestigePointsSystem {
    public static final int BASE_LEVEL_CAP = 100_000;
    public static final int BREAKTHROUGH_STEP = 10_000;
    public static final int MAX_BREAKTHROUGHS = 5;
    public static final int ABSOLUTE_LEVEL_CAP = BASE_LEVEL_CAP + MAX_BREAKTHROUGHS * BREAKTHROUGH_STEP;
    public static final int FORM_COST = 5;
    public static final int SKILL_POINT_COST = 1;
    /** Levels granted per point for Potential Unlock in the prestige shop. */
    public static final int POTENTIAL_UNLOCK_LEVELS_PER_POINT = 2;
    /** Skills shown per prestige shop inventory page. */
    public static final int SKILL_SHOP_PAGE_SIZE = 21;

    private static final String KEY_POINTS = "prestige_points";
    private static final String KEY_BREAKTHROUGHS = "pp_level_breakthroughs";
    private static final String KEY_MAJIN = "pp_perm_majin";
    private static final String KEY_MUTANT = "pp_perm_mutant";
    private static final String KEY_SKILL_PREFIX = "pp_skill_";
    /** Permanent difficulty-tier unlocks bought with prestige points ({@code pp_tier_1}…{@code 7}). */
    private static final String KEY_TIER_PREFIX = "pp_tier_";

    private static final String SKILL_MAJIN = "Permanent Majin";
    private static final String SKILL_MUTANT = "Permanent Mutant";
    private static final String NS_KEY = "legacymechanics";
    private static final String NS_PATH = "prestige-points";

    /**
     * Legacy named map kept for callers/audits — resolves to live catalog entries.
     * Prefer {@link #skillOffers()} / {@link #resolveOffer(String)}.
     */
    public static final Map<String, SkillOffer> SKILL_OFFERS = new LinkedHashMap<>();

    static {
        // Seed with Skill Check Natural skills so static audits / early boot still see entries.
        for (String id : DmzSkillUtil.SKILL_CHECK_NATURAL) {
            SkillOffer offer = resolveOffer(id);
            if (offer != null) {
                SKILL_OFFERS.put(id, offer);
            }
        }
    }

    private PrestigePointsSystem() {}

    public record SkillOffer(String id, String label, int maxLevel) {}

    /** Resolve a purchasable offer — Skill Check Natural/Saga skills only. */
    public static SkillOffer resolveOffer(String skillId) {
        if (skillId == null || skillId.isBlank()) {
            return null;
        }
        String id = skillId.toLowerCase(Locale.ROOT).trim();
        if (!DmzSkillUtil.isSkillCheckSkill(id) || DmzSkillUtil.isFormSkill(id)) {
            return null;
        }
        int max = DmzSkillUtil.configuredMaxLevel(id);
        if (max <= 0) {
            // Still allow known Skill Check ids before config is loaded.
            max = switch (id) {
                case "potentialunlock" -> 30;
                case "fusion", "kaioken" -> 5;
                case "kiboost" -> 4;
                case "kicontrol" -> 1;
                case "meditation", "fly", "sprint", "jump",
                     "kimanipulation", "kisense", "defense_penetration",
                     "healing_reduction", "instant_transmission",
                     "ki_infusion", "kiprotection" -> 10;
                default -> 0;
            };
        }
        if (max <= 0) {
            return null;
        }
        return new SkillOffer(id, DmzSkillUtil.prettySkillLabel(id), max);
    }

    /** Prestige shop catalog — Skill Check Natural + Saga only (no ultimate / attacks). */
    public static List<SkillOffer> skillOffers() {
        List<SkillOffer> out = new ArrayList<>();
        for (String id : DmzSkillUtil.skillCheckSkillIds()) {
            SkillOffer offer = resolveOffer(id);
            if (offer != null) {
                out.add(offer);
            }
        }
        if (out.isEmpty()) {
            // Config unavailable — fall back to seeded natural skills.
            out.addAll(SKILL_OFFERS.values());
        }
        return out;
    }

    public static int skillShopPageCount() {
        int n = skillOffers().size();
        return Math.max(1, (n + SKILL_SHOP_PAGE_SIZE - 1) / SKILL_SHOP_PAGE_SIZE);
    }

    public static List<SkillOffer> skillOffersPage(int pageIndex) {
        List<SkillOffer> all = skillOffers();
        int pages = Math.max(1, (all.size() + SKILL_SHOP_PAGE_SIZE - 1) / SKILL_SHOP_PAGE_SIZE);
        int page = Math.max(0, Math.min(pages - 1, pageIndex));
        int from = page * SKILL_SHOP_PAGE_SIZE;
        if (from >= all.size()) {
            return List.of();
        }
        int to = Math.min(all.size(), from + SKILL_SHOP_PAGE_SIZE);
        return all.subList(from, to);
    }

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

    /** Allowed single turn-in pack sizes (GUI + command). */
    public static final int[] TURN_IN_AMOUNTS = {1, 2, 3, 6, 9};

    public static boolean isAllowedTurnInAmount(int amount) {
        for (int n : TURN_IN_AMOUNTS) {
            if (n == amount) {
                return true;
            }
        }
        return false;
    }

    /**
     * Points for turning in {@code amount} prestiges in one action.
     * {@code N + triangular(N/3)} → 1→1, 2→2, 3→4, 6→9, 9→15.
     */
    public static int pointsForTurnIn(int amount) {
        int n = Math.max(0, amount);
        int packs = n / 3;
        int bonus = packs * (packs + 1) / 2;
        return n + bonus;
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
        if (!isAllowedTurnInAmount(want)) {
            return "§cTurn in §f1§7, §f2§7, §f3§7, §f6§7, or §f9 §cprestiges at a time.";
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

    /**
     * Staff: set the prestige-invested skill floor (levels bought in the prestige shop).
     * Does not change the wallet. Re-applies the live DMZ skill to match.
     */
    public static String adminAdjustSkill(
            ServerPlayer player, String skillId, String mode, int amount
    ) {
        if (player == null) {
            return "§cPlayer not online.";
        }
        SkillOffer offer = resolveOffer(skillId);
        if (offer == null) {
            // Allow adjusting a legacy stored id even if it left the catalog.
            String id = skillId == null ? "" : skillId.toLowerCase(Locale.ROOT).trim();
            if (id.isBlank()) {
                return "§cUsage: skill <player> <skillId> <set|add|remove> <levels>"
                        + "\n§8Example: skill Steve potentialunlock set 10";
            }
            int max = DmzSkillUtil.configuredMaxLevel(id);
            if (max <= 0) {
                max = Math.max(getPurchasedSkillLevels(player, id), 10);
            }
            offer = new SkillOffer(id, DmzSkillUtil.prettySkillLabel(id), max);
        }
        if (!ProgressionData.storedWritable(player)) {
            return "§cCould not save prestige data for §f" + player.m_6302_() + "§c.";
        }
        int before = getPurchasedSkillLevels(player, offer.id);
        Integer next = applyIntMode(before, mode, amount, 0, offer.maxLevel);
        if (next == null) {
            return "§cUsage: skill <player> <skillId> <set|add|remove> <levels>"
                    + "\n§8Example: skill Steve potentialunlock add 2";
        }
        ProgressionData.storedPut(player, KEY_SKILL_PREFIX + offer.id, next);
        Skills skills = DmzSkillUtil.skills(player);
        int liveBefore = 0;
        int liveAfter = 0;
        if (skills != null) {
            DmzSkillUtil.ensureRegistered(skills, offer.id, offer.maxLevel);
            liveBefore = DmzSkillUtil.level(skills, offer.id);
            int max = Math.max(offer.maxLevel, DmzSkillUtil.maxLevel(skills, offer.id, offer.maxLevel));
            if (next > before) {
                liveAfter = Math.min(max, Math.max(liveBefore, next));
            } else if (next < before && liveBefore > next) {
                // Lowering invested floor — clamp live skill down with it.
                liveAfter = Math.min(max, next);
            } else {
                liveAfter = liveBefore;
            }
            if (liveAfter != liveBefore) {
                DmzSkillUtil.setLevel(skills, offer.id, liveAfter);
                DmzSkillUtil.sync(player);
            }
        }
        reapplySkillBonuses(player);
        SystemTelemetry.log("prestige_admin", "admin_skill", player, null, Map.of(
                "skill", offer.id,
                "mode", mode == null ? "" : mode,
                "before", before,
                "after", next,
                "live_before", liveBefore,
                "live_after", liveAfter
        ));
        String perPoint = levelsPerPoint(offer.id) > 1
                ? " §8(" + levelsPerPoint(offer.id) + " levels/point in shop)"
                : "";
        return "§a" + offer.label + " §7prestige floor §f" + before + " §7→ §f" + next
                + " §8/ §f" + offer.maxLevel + perPoint
                + "\n§7Live skill §f" + liveBefore + " §7→ §f" + liveAfter
                + " §8(" + player.m_6302_() + ")";
    }

    /** Skills with a prestige-invested floor &gt; 0 (for admin info). */
    public static List<String> investedSkillSummary(ServerPlayer player) {
        List<String> out = new ArrayList<>();
        if (player == null) {
            return out;
        }
        for (SkillOffer offer : skillOffers()) {
            int bought = getPurchasedSkillLevels(player, offer.id);
            if (bought > 0) {
                out.add(offer.label + " §f" + bought + "§7/§f" + offer.maxLevel);
            }
        }
        return out;
    }

    private static Integer applyIntMode(int current, String mode, int amount, int min, int max) {
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

    /**
     * Buy prestige-invested skill floor levels.
     * Gated only on the stored prestige floor vs catalog max — live DMZ skill
     * level may already be maxed from training; players can still raise the
     * permanent floor that survives prestige reset.
     */
    public static String buySkillLevel(ServerPlayer player, String skillId) {
        if (player == null) {
            return "§cPlayers only.";
        }
        SkillOffer offer = resolveOffer(skillId);
        if (offer == null) {
            return "§cOnly Skill Check skills are purchasable here (Natural + Saga).";
        }
        int purchased = getPurchasedSkillLevels(player, offer.id);
        int floorMax = Math.max(1, offer.maxLevel);
        if (purchased >= floorMax) {
            return "§c" + offer.label + " prestige floor is maxed (§f" + floorMax + "§c).";
        }
        int points = getPoints(player);
        if (points < SKILL_POINT_COST) {
            return "§cNeed §e" + SKILL_POINT_COST + " §cpoint (have §e" + points + "§c).";
        }
        int levelsPerPoint = levelsPerPoint(offer.id);
        int room = Math.max(0, floorMax - purchased);
        if (room <= 0) {
            return "§c" + offer.label + " prestige floor is maxed (§f" + floorMax + "§c).";
        }
        int gain = Math.min(levelsPerPoint, room);
        setPoints(player, points - SKILL_POINT_COST);
        int nextPurchased = Math.min(floorMax, purchased + gain);
        ProgressionData.storedPut(player, KEY_SKILL_PREFIX + offer.id, nextPurchased);

        // Raise live skill only when below the new floor; never block the floor buy.
        int liveBefore = 0;
        int liveAfter = 0;
        Skills skills = DmzSkillUtil.skills(player);
        if (skills != null) {
            DmzSkillUtil.ensureRegistered(skills, offer.id, floorMax);
            liveBefore = DmzSkillUtil.level(skills, offer.id);
            int liveMax = Math.max(floorMax, DmzSkillUtil.maxLevel(skills, offer.id, floorMax));
            liveAfter = Math.min(liveMax, Math.max(liveBefore, nextPurchased));
            if (liveAfter != liveBefore) {
                DmzSkillUtil.setLevel(skills, offer.id, liveAfter);
                DmzSkillUtil.sync(player);
            }
        }
        SystemTelemetry.log("prestige_points", "buy_skill", player, null, Map.of(
                "skill", offer.id,
                "purchased", nextPurchased,
                "level", liveAfter,
                "gain", gain,
                "points", getPoints(player)
        ));
        String liveNote = liveAfter > liveBefore
                ? " → §fLv " + liveAfter
                : " §8(live already §f" + liveBefore + "§8)";
        return "§a+" + gain + " §7" + offer.label + " prestige floor §f" + purchased
                + " §7→ §f" + nextPurchased + liveNote
                + " §8(§e" + SKILL_POINT_COST + "§8 pt)"
                + "\n§7Points left: §e" + getPoints(player);
    }

    /** Prestige-shop levels gained per spent point for {@code skillId}. */
    public static int levelsPerPoint(String skillId) {
        if (skillId != null && "potentialunlock".equalsIgnoreCase(skillId.trim())) {
            return POTENTIAL_UNLOCK_LEVELS_PER_POINT;
        }
        return 1;
    }

    /** Re-apply purchased skill floors after prestige reset / on login. */
    public static void reapplySkillBonuses(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Skills skills = DmzSkillUtil.skills(player);
        if (skills != null) {
            boolean changed = false;
            // Shop catalog + any legacy purchased ids still stored on the player.
            LinkedHashMap<String, SkillOffer> apply = new LinkedHashMap<>();
            for (SkillOffer offer : skillOffers()) {
                apply.put(offer.id, offer);
                SKILL_OFFERS.put(offer.id, offer);
            }
            for (String id : DmzSkillUtil.allNonFormSkillIds()) {
                if (apply.containsKey(id)) {
                    continue;
                }
                int purchased = getPurchasedSkillLevels(player, id);
                if (purchased <= 0) {
                    continue;
                }
                int max = DmzSkillUtil.configuredMaxLevel(id);
                if (max <= 0) {
                    max = Math.max(purchased, 10);
                }
                apply.put(id, new SkillOffer(id, DmzSkillUtil.prettySkillLabel(id), max));
            }
            for (SkillOffer offer : apply.values()) {
                int purchased = getPurchasedSkillLevels(player, offer.id);
                if (purchased <= 0) {
                    continue;
                }
                DmzSkillUtil.ensureRegistered(skills, offer.id, offer.maxLevel);
                int current = DmzSkillUtil.level(skills, offer.id);
                int max = Math.max(offer.maxLevel, DmzSkillUtil.maxLevel(skills, offer.id, offer.maxLevel));
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
        reapplyTierUnlocks(player);
    }

    // ── Permanent difficulty tiers (prestige points) ───────────────────

    /**
     * Prestige-point cost for permanent unlock of difficulty tier {@code tierId} (1–7).
     * T1–2 → 1, T3–4 → 2, T5–6 → 3, T7 → 4.
     */
    public static int tierPointCost(int tierId) {
        if (tierId < 1 || tierId > 7) {
            return 0;
        }
        return (tierId + 1) / 2;
    }

    public static boolean hasPurchasedTier(ServerPlayer player, int tierId) {
        if (player == null || tierId < 1 || tierId > 7) {
            return false;
        }
        return ProgressionData.storedGetBool(player, KEY_TIER_PREFIX + tierId);
    }

    /** Highest permanently purchased tier (0 if none). */
    public static int highestPurchasedTier(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        int best = 0;
        for (int t = 1; t <= 7; t++) {
            if (hasPurchasedTier(player, t)) {
                best = t;
            }
        }
        return best;
    }

    /**
     * True when the tier is permanently purchased <b>or</b> actually unlocked in
     * difficulty data (level/prestige sync).
     */
    public static boolean isTierUnlockedOrPurchased(ServerPlayer player, int tierId) {
        if (player == null || tierId < 1 || tierId > 7) {
            return false;
        }
        if (hasPurchasedTier(player, tierId)) {
            return true;
        }
        try {
            var data = com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.data(player);
            return data != null && data.hasUnlockedTier(tierId);
        } catch (Throwable ignored) {
            return false;
        }
    }

    /**
     * Whether the player may buy permanent T{@code tierId} with prestige points.
     * Shop ladder: every lower tier must already be <b>permanently purchased</b>
     * (level unlock alone does not skip the chain — that left gaps after prestige).
     */
    public static boolean canBuyDifficultyTier(ServerPlayer player, int tierId) {
        if (player == null || tierId < 1 || tierId > 7) {
            return false;
        }
        if (hasPurchasedTier(player, tierId)) {
            return false;
        }
        for (int prev = 1; prev < tierId; prev++) {
            if (!hasPurchasedTier(player, prev)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Buy a permanent difficulty-tier unlock with prestige points.
     * Requires T1…T(n-1) permanently purchased first (shop ladder).
     * Allowed even when the tier is already unlocked via level — purchase makes it
     * permanent so it survives prestige / level-gate revoke.
     */
    public static String buyDifficultyTier(ServerPlayer player, int tierId) {
        if (player == null) {
            return "§cPlayers only.";
        }
        var tier = com.dbzlegacy.adaptivedifficulty.tier.UnlockTier.byId(tierId);
        if (tier == null) {
            return "§cUnknown tier. Use 1–7.";
        }
        if (!ProgressionData.storedWritable(player)) {
            return "§cCould not save prestige data — try relogging, then buy again.";
        }
        if (hasPurchasedTier(player, tierId)) {
            return "§eYou already own permanent §fT" + tierId + " " + tier.display + "§e.";
        }
        if (tierId > 1) {
            for (int prev = 1; prev < tierId; prev++) {
                if (!hasPurchasedTier(player, prev)) {
                    return "§cBuy permanent §fT" + prev + " §cfirst (shop ladder T1→T"
                            + tierId + ").";
                }
            }
        }
        int cost = tierPointCost(tierId);
        int points = getPoints(player);
        if (points < cost) {
            return "§cNeed §e" + cost + " §cpoint" + (cost == 1 ? "" : "s")
                    + " (have §e" + points + "§c).";
        }
        setPoints(player, points - cost);
        ProgressionData.storedPutBool(player, KEY_TIER_PREFIX + tierId, true);
        // Apply unlock bits for every permanently purchased tier (fills gaps).
        var data = com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.data(player);
        reapplyTierUnlocks(player);
        try {
            com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.save(player);
        } catch (Throwable ignored) {
        }
        try {
            if (data != null) {
                com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem.syncUnlocks(player, data);
            }
        } catch (Throwable ignored) {
        }
        reapplyTierUnlocks(player);
        try {
            com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.save(player);
        } catch (Throwable ignored) {
        }
        try {
            com.dbzlegacy.adaptivedifficulty.title.TitleSystem.syncTierTitles(player, true);
        } catch (Throwable ignored) {
        }
        SystemTelemetry.log("prestige_points", "buy_tier", player, null, Map.of(
                "tier", tierId,
                "cost", cost,
                "points", getPoints(player)
        ));
        return "§aPermanent unlock §fT" + tierId + " " + tier.display
                + " §7(§e-" + cost + " §7point" + (cost == 1 ? "" : "s") + ")"
                + "\n§7Survives prestige · still activate with Ancient Coins via §f/difficulty §7Tiers"
                + "\n§7Points left: §e" + getPoints(player);
    }

    /** Staff: grant permanent tier unlock(s) without spending points. */
    public static String adminGrantTier(ServerPlayer player, int tierId) {
        if (player == null) {
            return "§cPlayer not online.";
        }
        if (tierId < 1 || tierId > 7) {
            return "§cTier must be 1–7.";
        }
        if (!ProgressionData.storedWritable(player)) {
            return "§cCould not save prestige data for §f" + player.m_6302_() + "§c.";
        }
        // Grant the full ladder through tierId so activation has no gaps.
        for (int t = 1; t <= tierId; t++) {
            ProgressionData.storedPutBool(player, KEY_TIER_PREFIX + t, true);
        }
        reapplyTierUnlocks(player);
        try {
            com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.save(player);
        } catch (Throwable ignored) {
        }
        return "§aGranted permanent difficulty tiers §fT1–T" + tierId
                + " §8(" + player.m_6302_() + ")"
                + "\n§7Activate via §f/difficulty §7Tiers (Ancient Coins).";
    }

    /**
     * Staff: set/add/remove highest permanent difficulty tier (shop ladder).
     * {@code set 3} → owns T1–T3; {@code set 0} / clear all → none.
     */
    public static String adminAdjustHighestTier(ServerPlayer player, String mode, int amount) {
        if (player == null) {
            return "§cPlayer not online.";
        }
        int before = highestPurchasedTier(player);
        Integer next = applyIntMode(before, mode, amount, 0, 7);
        if (next == null) {
            return "§cUsage: tier <player> <set|add|remove> <0-7>"
                    + "\n§8Or: tier <player> give <1-7> | clear <1-7|all>";
        }
        if (!ProgressionData.storedWritable(player)) {
            return "§cCould not save prestige data for §f" + player.m_6302_() + "§c.";
        }
        for (int t = 1; t <= 7; t++) {
            if (t <= next) {
                ProgressionData.storedPutBool(player, KEY_TIER_PREFIX + t, true);
            } else {
                ProgressionData.storedRemove(player, KEY_TIER_PREFIX + t);
            }
        }
        reapplyTierUnlocks(player);
        try {
            com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.save(player);
        } catch (Throwable ignored) {
        }
        return "§aPermanent difficulty tiers §fT" + before + " §7→ §fT" + next
                + " §8(" + player.m_6302_() + ")"
                + (next > 0
                ? "\n§7Owns permanent §fT1–T" + next + "§7. Activate via §f/difficulty§7."
                : "\n§7No permanent prestige tiers.");
    }

    /** Staff: clear one or all permanent prestige-purchased tiers. */
    public static String adminClearTier(ServerPlayer player, int tierId) {
        if (player == null) {
            return "§cPlayer not online.";
        }
        if (!ProgressionData.storedWritable(player)) {
            return "§cCould not save prestige data for §f" + player.m_6302_() + "§c.";
        }
        if (tierId <= 0) {
            for (int t = 1; t <= 7; t++) {
                ProgressionData.storedRemove(player, KEY_TIER_PREFIX + t);
            }
            return "§7Cleared all permanent prestige difficulty tiers for §f"
                    + player.m_6302_() + "§7.";
        }
        if (tierId > 7) {
            return "§cTier must be 1–7 (or 0 for all).";
        }
        ProgressionData.storedRemove(player, KEY_TIER_PREFIX + tierId);
        return "§7Cleared permanent §fT" + tierId + " §7for §f" + player.m_6302_() + "§7."
                + "\n§8Level/prestige unlocks are unchanged.";
    }

    /** Ensure prestige-purchased tiers stay unlocked (survives sync revoke + prestige). */
    public static void reapplyTierUnlocks(ServerPlayer player) {
        if (player == null) {
            return;
        }
        var data = com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache.data(player);
        if (data == null) {
            return;
        }
        for (int t = 1; t <= 7; t++) {
            if (hasPurchasedTier(player, t)) {
                data.unlockTier(t);
            }
        }
    }

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
        // Fabled Permanent Majin/Mutant skills removed — LM owns the effect via dmzeffect.
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
        // Drop any leftover Fabled grant if an older skill still exists on the server.
        removeFormSkill(player, majin);
        runDmzEffect(player, majin ? "majin" : "mutant", false);
    }

    private static void reapplyForms(ServerPlayer player) {
        if (hasMajin(player)) {
            runDmzEffect(player, "majin", true);
        } else {
            removeFormSkill(player, true);
        }
        if (hasMutant(player)) {
            runDmzEffect(player, "mutant", true);
        } else {
            removeFormSkill(player, false);
        }
    }

    // ── Personal level-cap breakthroughs (maxValue 150k + personal soft-lock) ─

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

    public static void setBreakthroughs(ServerPlayer player, int breakthroughs) {
        if (player == null) {
            return;
        }
        int n = Math.max(0, Math.min(MAX_BREAKTHROUGHS, breakthroughs));
        ProgressionData.storedPut(player, KEY_BREAKTHROUGHS, n);
        try {
            DmzSkillUtil.sync(player);
        } catch (Throwable ignored) {
        }
    }

    /** Personal DMZ level cap: 100k + breakthroughs×10k (mixin + soft-locks). */
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
            return "§cPersonal level cap fully raised (§f"
                    + DmzRewards.formatWhole(ABSOLUTE_LEVEL_CAP) + "§c).";
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
        int newCap = effectiveMaxLevel(player);
        // Force a live DMZ read so the client/stat screen picks up the raised max.
        int liveCap = newCap;
        try {
            var data = com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.stats(player);
            if (data != null) {
                liveCap = Math.max(newCap, data.getConfiguredMaxValue());
                com.dbzlegacy.adaptivedifficulty.progression.DmzSkillUtil.sync(player);
            }
        } catch (Throwable ignored) {
        }
        SystemTelemetry.log("prestige_points", "breakthrough", player, null, Map.of(
                "breakthrough", next,
                "cap", newCap,
                "live_cap", liveCap,
                "cost", cost,
                "points", getPoints(player)
        ));
        String note = liveCap >= newCap
                ? ""
                : "\n§cWarning: live DMZ max still §f" + DmzRewards.formatWhole(liveCap)
                        + " §c(expected §f" + DmzRewards.formatWhole(newCap)
                        + "§c) — remount / report if this persists.";
        return "§aPersonal level cap raised to §f" + DmzRewards.formatWhole(newCap)
                + " §7(§e-" + cost + " §7points)"
                + "\n§7Keep leveling with TP / buy stats into the new cap."
                + "\n§7Future prestige requirements now scale up to §f"
                + DmzRewards.formatWhole(newCap) + "§7."
                + "\n§8Others stay soft-locked at their personal cap until they breakthrough too."
                + "\n§7Breakthrough §f" + next + "§7/§f" + MAX_BREAKTHROUGHS
                + " · Points left: §e" + getPoints(player)
                + "\n§7Live DMZ max now: §f" + DmzRewards.formatWhole(liveCap)
                + note;
    }

    // ── Login / post-prestige ──────────────────────────────────────────

    private static final String KEY_REAPPLY_AT = "pp_reapply_at_ms";
    private static final String KEY_FORM_PULSE_AT = "pp_form_pulse_at_ms";
    /** Match old Fabled Passive interval that re-gave majin/mutant when missing. */
    private static final long FORM_PULSE_MS = 5000L;

    public static void onLogin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        // Stagger reapply so DMZ / Fabled finish loading.
        ProgressionData.tempPut(player, KEY_REAPPLY_AT, System.currentTimeMillis() + 2000L);
        reapplyAllShopPurchases(player);
    }

    public static void scheduleReapplyAfterPrestige(ServerPlayer player) {
        scheduleReapplyAfterDeath(player);
    }

    /**
     * After death respawn (or prestige reset), re-apply every prestige-shop purchase:
     * skill floors, permanent difficulty tiers, Majin/Mutant, and breakthrough soft-lock.
     * Immediate + next-tick + pulse window cover DMZ skill rebuild races.
     */
    public static void scheduleReapplyAfterDeath(ServerPlayer player) {
        if (player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        // Pulse path: keep reapplying until this deadline (skills/forms/tiers).
        ProgressionData.tempPut(player, KEY_REAPPLY_AT, now + 8000L);
        reapplyAllShopPurchases(player);
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return;
        }
        final java.util.UUID id = player.m_20148_();
        // Next-tick pass after DMZ finishes respawn rebuild (UUID — clone/original share id).
        server.execute(() -> {
            ServerPlayer p = server.m_6846_().m_11259_(id);
            if (p != null && p.m_6084_()) {
                reapplyAllShopPurchases(p);
            }
        });
    }

    /**
     * Restore live DMZ / difficulty state from stored prestige-shop purchases.
     * Does not spend points — only reapplies owned floors / effects / unlocks.
     */
    public static void reapplyAllShopPurchases(ServerPlayer player) {
        if (player == null) {
            return;
        }
        reapplySkillBonuses(player);
        // Breakthroughs are read live from NBT (effectiveMaxLevel); force a sync so
        // clients / soft-locks see the personal cap immediately after respawn.
        try {
            DmzSkillUtil.sync(player);
        } catch (Throwable ignored) {
        }
    }

    /** Called from shop pulse — drains delayed reapply markers + keeps forms live. */
    public static void pulsePlayer(ServerPlayer player, long nowMs) {
        if (player == null) {
            return;
        }
        long at = ProgressionData.tempGetLong(player, KEY_REAPPLY_AT, 0L);
        if (at > 0L) {
            // While a post-death / login window is open, keep reapplying every pulse
            // until the deadline (DMZ may wipe skills mid-window).
            reapplyAllShopPurchases(player);
            if (nowMs >= at) {
                ProgressionData.tempRemove(player, KEY_REAPPLY_AT);
            }
        }
        // Fabled Permanent Majin/Mutant skills are gone — LM must keep dmzeffect applied.
        if (hasMajin(player) || hasMutant(player)) {
            long next = ProgressionData.tempGetLong(player, KEY_FORM_PULSE_AT, 0L);
            if (next <= 0L || nowMs >= next) {
                ProgressionData.tempPut(player, KEY_FORM_PULSE_AT, nowMs + FORM_PULSE_MS);
                reapplyForms(player);
            }
        }
    }

    // ── Fabled / DMZ helpers ───────────────────────────────────────────

    private static void reducePrestigeClass(ServerPlayer player, int amount) {
        if (player == null || amount <= 0) {
            return;
        }
        // Prefer Fabled API — console "class level … take" often no-ops on Mohist,
        // leaving Prestige class high so faction sync restores held tokens.
        int lost = 0;
        try {
            lost = com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync
                    .takePrestigeLevels(player, amount);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] prestige points API take soft-fail: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
        if (lost >= amount) {
            return;
        }
        int remain = amount - lost;
        MinecraftServer server = player.m_20194_();
        if (server == null) {
            return;
        }
        String name = player.m_6302_();
        try {
            server.m_129892_().m_230957_(
                    server.m_129893_(),
                    "class level " + name + " take " + remain + " Prestige"
            );
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] prestige points class take soft-fail: {}",
                    AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
        // Console path still needs DMZ skill + faction catch-up.
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync.sync(player);
        } catch (Throwable ignored) {
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeFactionSync.forceSync(player);
        } catch (Throwable ignored) {
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

    /**
     * Apply / clear DMZ majin or mutant <b>silently</b>.
     * <p>
     * Never call {@code MutantManager.grant} — it always chats
     * {@code message.dragonminez.mutant.gained} even when already mutant, which
     * spammed every form pulse / death reapply. Prefer Effects + MutantSavedData
     * (or {@code reconcileHolder} when already present). No {@code dmzeffect}
     * command fallback on give — Brigadier {@code sendSuccess} also chats.
     */
    private static void runDmzEffect(ServerPlayer player, String effect, boolean give) {
        if (player == null || effect == null || effect.isBlank()) {
            return;
        }
        String effectId = effect.trim().toLowerCase(Locale.ROOT);
        boolean mutant = "mutant".equals(effectId);
        try {
            var data = com.dbzlegacy.adaptivedifficulty.calc.DmzProgression.stats(player);
            if (data == null) {
                return;
            }
            var effects = data.getEffects();
            if (effects == null) {
                return;
            }
            if (mutant) {
                if (give) {
                    ensureMutantSilent(player, data, effects);
                } else if (com.dragonminez.server.util.MutantManager.isMutant(data)) {
                    com.dragonminez.server.util.MutantManager.revoke(player, data);
                }
                return;
            }
            // Majin (and any other non-mutant effect id).
            if (give) {
                ensureEffectSilent(player, effects, effectId, majinEffectPower());
            } else if (effects.hasEffect(effectId)) {
                effects.removeEffect(effectId);
                DmzSkillUtil.sync(player);
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] prestige form API soft-fail ({}): {}",
                    AdaptiveDifficultyMod.MOD_ID, effectId, t.toString());
        }
    }

    /**
     * Keep permanent Mutant applied without {@link com.dragonminez.server.util.MutantManager#grant}'s chat.
     */
    private static void ensureMutantSilent(
            ServerPlayer player,
            com.dragonminez.common.stats.StatsData data,
            com.dragonminez.common.stats.character.Effects effects
    ) {
        if (com.dragonminez.server.util.MutantManager.isMutant(data)) {
            // Already has effect — only ensure lottery holder registration (no chat).
            try {
                com.dragonminez.server.util.MutantManager.reconcileHolder(player, data);
            } catch (Throwable ignored) {
            }
            return;
        }
        effects.addEffect("mutant", 1.0, -1);
        try {
            MinecraftServer server = player.m_20194_();
            if (server != null) {
                com.dragonminez.server.world.data.MutantSavedData.get(server)
                        .addHolder(player.m_20148_());
            }
        } catch (Throwable ignored) {
        }
        DmzSkillUtil.sync(player);
    }

    /** Apply or upgrade an effect to permanent duration without command chat. */
    private static void ensureEffectSilent(
            ServerPlayer player,
            com.dragonminez.common.stats.character.Effects effects,
            String effectId,
            double power
    ) {
        if (effects.hasEffect(effectId) && effects.getEffectDuration(effectId) == -1) {
            return;
        }
        effects.addEffect(effectId, power, -1);
        DmzSkillUtil.sync(player);
    }

    private static double majinEffectPower() {
        try {
            var gameplay = com.dragonminez.common.config.ConfigManager.getServerConfig().getGameplay();
            // Prefer configured majin power when present; 1.0 matches typical give defaults.
            Method m = gameplay.getClass().getMethod("getMajinPower");
            Object v = m.invoke(gameplay);
            if (v instanceof Number n && n.doubleValue() > 0.0) {
                return n.doubleValue();
            }
        } catch (Throwable ignored) {
        }
        return 1.0;
    }
}
