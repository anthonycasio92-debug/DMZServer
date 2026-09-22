package com.dbzlegacy.adaptivedifficulty.progression.shop;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.LmChat;
import com.dbzlegacy.adaptivedifficulty.util.PersistentDataAccess;
import com.dragonminez.common.stats.StatsData;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Port of {@code Prestige NPC.js} purchase logic as {@code /prestige} chat GUI.
 * Level gates: first five lifetime prestiges use {@code (completed+1)×20k} up to 100k;
 * after that, Need follows held wallet (50k / 100k / 145k / 150k). Playable cap is 150k
 * (Overhaul — no breakthrough shop).
 *
 * <p><b>Lifetime completed</b> never drops when held/Fabled Prestige is turned in —
 * otherwise Need would snap back to 20k after a completed prestige.
 */
public final class PrestigeSystem {
    private static final int LEVELS_PER_PRESTIGE = 20_000;
    /** First {@value} completed prestiges use the 20k ladder (max Need 100k). */
    public static final int COMPLETED_LADDER_PRESTIGES = 5;
    private static final int MAX_LADDER_REQUIRED = 100_000;
    private static final int[] HELD_REQUIRED_LEVELS = {50_000, 100_000, 145_000, 150_000};
    private static final int MAX_HELD = 10;
    private static final long CONFIRM_MS = 10_000L;
    /** Live Prestige NPC.js — held tokens live on CNPC faction 4. */
    private static final int FACTION_HELD_ID = 4;

    private static final String KEY_TOTAL = "prestige_total_completed";
    /**
     * When set, {@link #getCompleted} returns stored {@link #KEY_TOTAL} only — staff
     * {@code /padmin completed set 0} is not bumped by DMZ skill or shop inference.
     */
    private static final String KEY_TOTAL_STAFF_OVERRIDE = "prestige_total_staff_override";
    /** Highest Need already earned — never let Need fall below this (capped at personal cap). */
    private static final String KEY_NEED_FLOOR = "prestige_need_floor";
    private static final String KEY_HELD = "lm_prestige_held";
    private static final String KEY_CONFIRM_UNTIL = "lm_prestige_confirm_until";

    private static final Map<UUID, Long> CONFIRM_UNTIL = new ConcurrentHashMap<>();

    private PrestigeSystem() {}

    public static void open(ServerPlayer player) {
        if (!DifficultyConfig.get().enablePrestigeSystem || player == null) {
            return;
        }
        showStatus(player);
    }

    public static String confirmOrPrompt(ServerPlayer player) {
        if (!DifficultyConfig.get().enablePrestigeSystem || player == null) {
            return "§cPrestige system is disabled.";
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            return reply(player, "§cCould not read your Dragon Mine Z stats.");
        }
        int level;
        try {
            level = Math.max(0, data.getLevel());
        } catch (Throwable t) {
            return reply(player, "§cCould not determine your Dragon Mine Z level.");
        }

        int completed = getCompleted(player);
        int held = getHeld(player);
        int required = requiredLevel(player);
        int next = completed + 1;

        if (held >= MAX_HELD) {
            return replyCap(player, held);
        }
        if (level < required) {
            return replyRequirement(player, completed, next, required, level);
        }

        long now = System.currentTimeMillis();
        Long until = CONFIRM_UNTIL.get(player.m_20148_());
        CompoundTag tag = PersistentDataAccess.get(player);
        if (until == null && PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_CONFIRM_UNTIL)) {
            until = tag.m_128454_(KEY_CONFIRM_UNTIL);
        }

        if (until != null && until > now) {
            return purchase(player, data, completed, required, level);
        }

        long confirmUntil = now + CONFIRM_MS;
        CONFIRM_UNTIL.put(player.m_20148_(), confirmUntil);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128356_(KEY_CONFIRM_UNTIL, confirmUntil);
        }
        return replyConfirm(player, completed, next, required, level, held);
    }

    private static String purchase(
            ServerPlayer player, StatsData data, int completed, int required, int level
    ) {
        CONFIRM_UNTIL.remove(player.m_20148_());
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128473_(KEY_CONFIRM_UNTIL);
        }

        int held = getHeld(player);
        if (held >= MAX_HELD) {
            return replyCap(player, held);
        }
        if (level < required) {
            return replyRequirement(player, completed, completed + 1, required, level);
        }

        int newHeld = held + 1;
        int newCompleted = completed + 1;
        setHeld(player, newHeld);
        setCompleted(player, newCompleted);
        // Lock Need so turn-in / Fabled spend cannot snap the next gate back to 20k.
        if (newCompleted < COMPLETED_LADDER_PRESTIGES) {
            raiseNeedFloor(player, Math.min(required, MAX_LADDER_REQUIRED));
        } else {
            clearNeedFloor(player);
        }
        resetPrestigeProgress(player);

        String name = player.m_6302_();
        MinecraftServer server = player.m_20194_();
        boolean apiAdded = false;
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync
                    .alignFabledToHeld(player);
            int want = com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync
                    .fabledLevelForHeld(newHeld);
            apiAdded = com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync
                    .fabledPrestigeLevel(player) == want;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] prestige class level API soft-fail: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
        }
        if (server != null) {
            if (!apiAdded) {
                try {
                    server.m_129892_().m_230957_(
                            server.m_129893_(),
                            "class level " + name + " add 1 Prestige"
                    );
                    try {
                        com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync.sync(player);
                    } catch (Throwable ignored) {
                    }
                    try {
                        com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeFactionSync.forceSync(player);
                    } catch (Throwable ignored) {
                    }
                } catch (Throwable t) {
                    AdaptiveDifficultyMod.LOGGER.debug(
                            "[{}] prestige class level soft-fail: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
                }
            }
            try {
                server.m_129892_().m_230957_(server.m_129893_(), "dmzstats reset " + name);
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] prestige dmzstats reset soft-fail: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            }
        }
        // Prestige-point skill floors + Permanent Majin/Mutant must survive dmzstats reset.
        PrestigePointsSystem.scheduleReapplyAfterPrestige(player);
        try {
            com.dbzlegacy.adaptivedifficulty.progression.PrestigeResourceRecovery.afterDmzStatsReset(player);
        } catch (Throwable ignored) {
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge
                    .scheduleSyncAfterStatsReset(player);
        } catch (Throwable ignored) {
        }

        int nextRequired = requiredLevel(player);
        String summary = "§aPrestige Level §f" + newCompleted + " §aComplete!\n"
                + "§7Held: §6" + newHeld + "§7/§f" + MAX_HELD + "\n"
                + "§7Next needs §e" + DmzRewards.formatWhole(nextRequired) + " §7DMZ levels"
                + " §8(max level §f" + DmzRewards.formatWhole(PrestigePointsSystem.effectiveMaxLevel(player)) + "§8).";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, LmChat.DIVIDER);
            send(player, "§aPrestige Level §f" + newCompleted + " §aComplete!");
            send(player, "§7Required level was §f" + DmzRewards.formatWhole(required));
            send(player, "§7Held Prestige Levels: §6" + newHeld + "§7/§f" + MAX_HELD);
            send(player, "§7Next Prestige requires §e" + DmzRewards.formatWhole(nextRequired) + " §7DMZ levels.");
            send(player, LmChat.DIVIDER);
        }
        SystemTelemetry.log("prestige", "purchase", player, null, Map.of(
                "completed", newCompleted,
                "held", newHeld,
                "required", required
        ));
        return summary;
    }

    private static void showStatus(ServerPlayer player) {
        StatsData data = DmzProgression.stats(player);
        int level = 0;
        if (data != null) {
            try {
                level = data.getLevel();
            } catch (Throwable ignored) {
            }
        }
        int completed = getCompleted(player);
        int held = getHeld(player);
        int required = requiredLevel(player);
        send(player, "");
        send(player, "§8── §6Prestige §8──");
        send(player, "§7Completed: §f" + completed + " §8| §7Held: §6" + held + "§7/§f" + MAX_HELD);
        send(player, "§7DMZ Level: §f" + DmzRewards.formatWhole(level)
                + " §8| §7Need: §e" + DmzRewards.formatWhole(required)
                + " §8| §7Max: §f" + DmzRewards.formatWhole(PrestigePointsSystem.effectiveMaxLevel(player)));
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§a[Prestige]", "/lmdo prestige confirm 0 main", "Confirm prestige purchase"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§7[Refresh]", "/lmdo lm open prestige", "Refresh status"));
        send(player, row);
        send(player, "§8────────────────");
    }

    private static String replyConfirm(
            ServerPlayer player, int completed, int next, int required, int level, int held
    ) {
        String summary = "§eConfirm Prestige Level §f" + next + "§e?\n"
                + "§7Click again within 10s · resets DMZ stats\n"
                + "§7Held after: §6" + (held + 1) + "§7/§f" + MAX_HELD;
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, LmChat.DIVIDER);
            send(player, "§eConfirm Prestige Level §f" + next + "§e?");
            send(player, "§7This resets DMZ stats and awards one held Prestige Level.");
            send(player, "§7Your level §f" + DmzRewards.formatWhole(level)
                    + " §7meets §e" + DmzRewards.formatWhole(required) + "§7.");
            send(player, "§7Held after: §6" + (held + 1) + "§7/§f" + MAX_HELD);
            send(player, "§8Click again within 10s to confirm.");
            MutableComponent row = Component.m_237113_("§7")
                    .m_7220_(btn("§a[Confirm Prestige]", "/lmdo prestige confirm 0 main", "Complete prestige"))
                    .m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§c[Cancel]", "/lmdo lm open prestige", "Cancel"));
            send(player, row);
            send(player, LmChat.DIVIDER);
        }
        return summary;
    }

    private static String replyCap(ServerPlayer player, int held) {
        String summary = "§cMaximum Prestige Levels Reached\n"
                + "§7Available: §6" + held + "§7/§f" + MAX_HELD + "\n"
                + "§eUse one before prestiging again.";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, LmChat.DIVIDER);
            send(player, "§cMaximum Prestige Levels Reached");
            send(player, "§7Available Prestige Levels: §6" + held + "§7/§f" + MAX_HELD);
            send(player, "§eUse one before prestiging again.");
            send(player, LmChat.DIVIDER);
        }
        return summary;
    }

    private static String replyRequirement(
            ServerPlayer player, int completed, int next, int required, int level
    ) {
        String summary = "§cNot Ready for Prestige Level §f" + next + "\n"
                + "§7Need §e" + DmzRewards.formatWhole(required)
                + " §7DMZ levels (have §f" + DmzRewards.formatWhole(level) + "§7).";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, LmChat.DIVIDER);
            send(player, "§cNot Ready for Prestige Level §f" + next);
            send(player, "§7Need §e" + DmzRewards.formatWhole(required)
                    + " §7DMZ levels (have §f" + DmzRewards.formatWhole(level) + "§7).");
            send(player, "§7Completed prestiges: §f" + completed);
            send(player, LmChat.DIVIDER);
        }
        return summary;
    }

    private static String reply(ServerPlayer player, String summary) {
        if (!preferGuiFeedback()) {
            DmzRewards.msg(player, summary);
        }
        return summary;
    }

    /** Inventory/CMI backends show results in the GUI header; chat backend keeps chat. */
    private static boolean preferGuiFeedback() {
        try {
            String backend = DifficultyConfig.get().guiBackend;
            if (backend == null || backend.isBlank()) {
                return true;
            }
            return !"chat".equalsIgnoreCase(backend.trim());
        } catch (Throwable ignored) {
            return true;
        }
    }

    /**
     * Next prestige DMZ level gate. First five completed use {@code (completed+1)×20k}
     * (max 100k) with a lifetime floor so turn-in does not snap Need backward. After five
     * completed, Need follows held wallet (turn in prestiges to lower the next gate).
     */
    public static int requiredLevel(ServerPlayer player) {
        if (player == null) {
            return requiredLevelForCompleted(0);
        }
        sanitizeNeedFloor(player);
        int cap = PrestigePointsSystem.effectiveMaxLevel(player);
        int completed = getCompleted(player);
        if (completed < COMPLETED_LADDER_PRESTIGES) {
            int ladder = requiredLevelForCompleted(completed);
            int floor = Math.min(getNeedFloor(player), MAX_LADDER_REQUIRED);
            return Math.min(cap, Math.max(ladder, floor));
        }
        return Math.min(cap, requiredLevelForHeld(getHeld(player)));
    }

    /**
     * Legacy {@code prestige_need_floor} could be pinned at 150k from the removed breakthrough
     * cap shop — that must not override held-based Need or the 100k ladder ceiling.
     */
    private static void sanitizeNeedFloor(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag) || !tag.m_128441_(KEY_NEED_FLOOR)) {
            return;
        }
        int completed = readStoredInt(tag, KEY_TOTAL);
        if (staffOverrideActive(tag)) {
            completed = Math.max(0, completed);
        } else {
            completed = getCompleted(player);
        }
        if (completed >= COMPLETED_LADDER_PRESTIGES) {
            tag.m_128473_(KEY_NEED_FLOOR);
            return;
        }
        int floor = readStoredInt(tag, KEY_NEED_FLOOR);
        if (floor > MAX_LADDER_REQUIRED) {
            tag.m_128405_(KEY_NEED_FLOOR, MAX_LADDER_REQUIRED);
        }
    }

    private static void clearNeedFloor(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128473_(KEY_NEED_FLOOR);
        }
    }

    /** Ladder phase only — {@code (completed+1)×20k}, max 100k. */
    public static int requiredLevelForCompleted(int currentCompleted) {
        int required = Math.max(0, currentCompleted + 1) * LEVELS_PER_PRESTIGE;
        return Math.min(MAX_LADDER_REQUIRED, required);
    }

    /** Post-ladder phase — keyed on held wallet before the next purchase. */
    public static int requiredLevelForHeld(int held) {
        int h = Math.max(0, Math.min(MAX_HELD, held));
        if (h >= 3) {
            return HELD_REQUIRED_LEVELS[3];
        }
        return HELD_REQUIRED_LEVELS[h];
    }

    /** @deprecated prefer {@link #requiredLevel(ServerPlayer)} */
    public static int requiredLevel(int currentCompleted) {
        return requiredLevelForCompleted(currentCompleted);
    }

    /** @deprecated prefer {@link #requiredLevel(ServerPlayer)} */
    public static int requiredLevel(int currentCompleted, int personalCap) {
        int cap = personalCap > 0 ? personalCap : PrestigePointsSystem.ABSOLUTE_LEVEL_CAP;
        return Math.min(cap, requiredLevelForCompleted(currentCompleted));
    }

    public static int maxHeld() {
        return MAX_HELD;
    }

    /**
     * NBT held wallet only (not CNPC faction inflate). Fabled Prestige class
     * is always {@code wallet + 1}.
     */
    public static int getHeldWallet(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_HELD)) {
            return Math.max(0, Math.min(MAX_HELD, tag.m_128451_(KEY_HELD)));
        }
        return getHeld(player);
    }

    public static int levelsPerPrestige() {
        return LEVELS_PER_PRESTIGE;
    }

    /**
     * Lifetime prestiges completed. Never decreases when held/Fabled Prestige is
     * spent on turn-in (DMZ {@code prestige} skill tracks current Fabled, not lifetime).
     */
    public static int getCompleted(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (staffOverrideActive(tag)) {
            return Math.max(0, readStoredInt(tag, KEY_TOTAL));
        }
        boolean hasKey = PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_TOTAL);
        int stored = readStoredInt(tag, KEY_TOTAL);

        if (!hasKey) {
            // One-time migrate from legacy Fabled/DMZ skill (old NPC overwrote total from Fabled).
            int legacy = Math.max(0, DmzProgression.prestige(player));
            try {
                int fabled = com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync
                        .fabledPrestigeLevel(player);
                legacy = Math.max(legacy, Math.max(0, fabled - 1));
            } catch (Throwable ignored) {
            }
            if (legacy > 0) {
                setCompletedPublic(player, legacy);
                raiseNeedFloor(player, requiredLevelForCompleted(legacy));
                return legacy;
            }
            // Shop evidence of a past prestige when total was zeroed by turn-in.
            if (inferCompletedFromShop(player)) {
                setCompletedPublic(player, 1);
                raiseNeedFloor(player, LEVELS_PER_PRESTIGE);
                return 1;
            }
            return 0;
        }

        // High-watermark: if DMZ/Fabled still show a higher lifetime, keep it.
        int fromSkill = Math.max(0, DmzProgression.prestige(player));
        int best = Math.max(stored, fromSkill);
        if (best > stored) {
            setCompletedPublic(player, best);
        }
        if (best <= 0 && inferCompletedFromShop(player)) {
            setCompletedPublic(player, 1);
            raiseNeedFloor(player, LEVELS_PER_PRESTIGE);
            return 1;
        }
        return best;
    }

    /** Points / breakthroughs / permanent forms imply at least one completed prestige. */
    private static boolean inferCompletedFromShop(ServerPlayer player) {
        try {
            if (PrestigePointsSystem.getPoints(player) > 0) {
                return true;
            }
            if (PrestigePointsSystem.hasMajin(player) || PrestigePointsSystem.hasMutant(player)) {
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static int getNeedFloor(ServerPlayer player) {
        CompoundTag tag = PersistentDataAccess.get(player);
        return Math.max(0, readStoredInt(tag, KEY_NEED_FLOOR));
    }

    private static void raiseNeedFloor(ServerPlayer player, int requiredMet) {
        if (player == null || requiredMet <= 0) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag)) {
            return;
        }
        int cur = Math.max(0, readStoredInt(tag, KEY_NEED_FLOOR));
        int met = Math.min(requiredMet, MAX_LADDER_REQUIRED);
        if (met > cur) {
            tag.m_128405_(KEY_NEED_FLOOR, met);
        }
    }

    private static int readStoredInt(CompoundTag tag, String key) {
        if (!PersistentDataAccess.isWritable(tag) || !tag.m_128441_(key)) {
            return 0;
        }
        try {
            return Integer.parseInt(tag.m_128461_(key));
        } catch (Exception e) {
            return (int) tag.m_128451_(key);
        }
    }

    private static boolean staffOverrideActive(CompoundTag tag) {
        return PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_TOTAL_STAFF_OVERRIDE)
                && tag.m_128471_(KEY_TOTAL_STAFF_OVERRIDE);
    }

    private static void clearStaffOverride(ServerPlayer player) {
        if (player == null) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128473_(KEY_TOTAL_STAFF_OVERRIDE);
        }
    }

    private static void alignNeedFloorForStaff(ServerPlayer player, int completed) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag)) {
            return;
        }
        if (completed <= 0) {
            tag.m_128473_(KEY_NEED_FLOOR);
            return;
        }
        int cap = PrestigePointsSystem.effectiveMaxLevel(player);
        tag.m_128405_(KEY_NEED_FLOOR, Math.min(cap, requiredLevelForCompleted(completed)));
    }

    public static int getHeld(ServerPlayer player) {
        int nbt = 0;
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_HELD)) {
            nbt = Math.max(0, tag.m_128451_(KEY_HELD));
        }
        // Live Prestige NPC used CNPC faction 4 as held tokens — prefer the higher value.
        Integer faction = readFactionPoints(player, FACTION_HELD_ID);
        if (faction != null) {
            return Math.max(0, Math.min(MAX_HELD, Math.max(nbt, faction)));
        }
        return Math.max(0, Math.min(MAX_HELD, nbt));
    }

    private static void setCompleted(ServerPlayer player, int value) {
        clearStaffOverride(player);
        setCompletedPublic(player, value);
    }

    /** Staff {@code /padmin completed …} — value sticks even when DMZ skill / shop would infer higher. */
    public static void setCompletedStaff(ServerPlayer player, int value) {
        if (player == null) {
            return;
        }
        int clamped = Math.max(0, value);
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128359_(KEY_TOTAL, Integer.toString(clamped));
            tag.m_128379_(KEY_TOTAL_STAFF_OVERRIDE, true);
            alignNeedFloorForStaff(player, clamped);
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge
                    .syncFromLegacy(player);
        } catch (Throwable ignored) {
        }
    }

    /** Public for staff admin tools. */
    public static void setCompletedPublic(ServerPlayer player, int value) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128359_(KEY_TOTAL, Integer.toString(Math.max(0, value)));
        }
        // Keep Need floor aligned when staff raise completed (never lower floor here).
        if (value > 0 && value < COMPLETED_LADDER_PRESTIGES) {
            raiseNeedFloor(player, requiredLevelForCompleted(value));
        } else if (value >= COMPLETED_LADDER_PRESTIGES) {
            clearNeedFloor(player);
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge
                    .syncFromLegacy(player);
        } catch (Throwable ignored) {
        }
    }

    private static void setHeld(ServerPlayer player, int value) {
        setHeldPublic(player, value);
    }

    /** Public for prestige-points turn-in and padmin. Wallet NBT is source of truth. */
    public static void setHeldPublic(ServerPlayer player, int value) {
        int clamped = Math.max(0, Math.min(MAX_HELD, value));
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128405_(KEY_HELD, clamped);
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeSkillSync
                    .alignFabledToHeld(player);
        } catch (Throwable ignored) {
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.PrestigeFactionSync
                    .forceSync(player);
        } catch (Throwable ignored) {
        }
        try {
            com.dbzlegacy.adaptivedifficulty.progression.bridge.DmzRevampPrestigeBridge
                    .scheduleSyncAfterStatsReset(player);
        } catch (Throwable ignored) {
        }
    }

    /** Live Prestige NPC.js — clear saga quests/dialogs after purchase. */
    private static void resetPrestigeProgress(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int[] quests = {26, 2, 27};
        int[] dialogs = {21, 20, 18, 19};
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            if (!(available instanceof Boolean ok) || !ok) {
                return;
            }
            Object api = npcApi.getMethod("Instance").invoke(null);
            Object entity = api.getClass()
                    .getMethod("getIEntity", net.minecraft.world.entity.Entity.class)
                    .invoke(api, player);
            if (entity == null) {
                return;
            }
            for (int q : quests) {
                try {
                    entity.getClass().getMethod("removeQuest", int.class).invoke(entity, q);
                } catch (Throwable ignored) {
                }
            }
            for (int d : dialogs) {
                try {
                    entity.getClass().getMethod("removeDialog", int.class).invoke(entity, d);
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }
    }

    private static Integer readFactionPoints(ServerPlayer player, int factionId) {
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            if (available instanceof Boolean ok && ok) {
                Object api = npcApi.getMethod("Instance").invoke(null);
                Object entity = api.getClass()
                        .getMethod("getIEntity", net.minecraft.world.entity.Entity.class)
                        .invoke(api, player);
                if (entity != null) {
                    Object pts = entity.getClass()
                            .getMethod("getFactionPoints", int.class)
                            .invoke(entity, factionId);
                    if (pts instanceof Number n) {
                        return n.intValue();
                    }
                }
            }
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static boolean addFactionPoints(ServerPlayer player, int factionId, int delta) {
        try {
            Class<?> npcApi = Class.forName("noppes.npcs.api.NpcAPI");
            Object available = npcApi.getMethod("IsAvailable").invoke(null);
            if (available instanceof Boolean ok && ok) {
                Object api = npcApi.getMethod("Instance").invoke(null);
                Object entity = api.getClass()
                        .getMethod("getIEntity", net.minecraft.world.entity.Entity.class)
                        .invoke(api, player);
                if (entity != null) {
                    entity.getClass()
                            .getMethod("addFactionPoints", int.class, int.class)
                            .invoke(entity, factionId, delta);
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static MutableComponent btn(String label, String command, String hover) {
        return Component.m_237113_(label).m_6270_(Style.f_131099_
                .m_131142_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .m_131144_(new HoverEvent(HoverEvent.Action.f_130831_, Component.m_237113_(hover))));
    }

    private static void send(ServerPlayer player, String text) {
        if (text == null || text.isEmpty()) {
            player.m_213846_(Component.m_237113_(""));
            return;
        }
        LmChat.send(player, text);
    }

    private static void send(ServerPlayer player, Component text) {
        player.m_213846_(text);
    }

    public static void clearPlayer(UUID uuid) {
        if (uuid != null) {
            CONFIRM_UNTIL.remove(uuid);
        }
    }
}
