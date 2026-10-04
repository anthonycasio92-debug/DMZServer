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
 * Level gate: the first four prestiges each need 20k. From the fifth on, 50k if you
 * hold none and 100k if you hold one or more. Capped by the personal level cap.
 *
 * <p><b>Lifetime completed</b> never drops when held/Fabled Prestige is turned in —
 * otherwise Need would snap back to 20k after a completed prestige.
 */
public final class PrestigeSystem {
    private static final int LEVELS_PER_PRESTIGE = 20_000;
    private static final int MAX_REQUIRED_LEVEL = 100_000;
    /** Lifetime completed count where the next prestige starts using the held gate. */
    public static final int HELD_GATE_MIN_COMPLETED = 4;
    private static final int HELD0_NEED = 50_000;
    private static final int HELD1_PLUS_NEED = 100_000;
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
        raiseNeedFloor(player, required);
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
        String summary = "§aThat's prestige §f" + newCompleted + "§a.\n"
                + "§7You're holding §6" + newHeld + "§7/§f" + MAX_HELD + ".\n"
                + "§7The next one needs level §e" + DmzRewards.formatWhole(nextRequired) + "§7.";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, LmChat.DIVIDER);
            send(player, "§aThat's prestige §f" + newCompleted + "§a.");
            send(player, "§7You're holding §6" + newHeld + "§7/§f" + MAX_HELD + ".");
            send(player, "§7The next one needs level §e" + DmzRewards.formatWhole(nextRequired) + "§7.");
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
        send(player, "§7Prestiged §f" + completed + " §7times · holding §6" + held + "§7/§f" + MAX_HELD);
        send(player, "§7Level §f" + DmzRewards.formatWhole(level)
                + " §7· next one needs §e" + DmzRewards.formatWhole(required));
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§a[Prestige]", "/prestige do confirm", "Confirm prestige purchase"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§7[Refresh]", "/prestige", "Refresh status"));
        send(player, row);
        send(player, "§8────────────────");
    }

    private static String replyConfirm(
            ServerPlayer player, int completed, int next, int required, int level, int held
    ) {
        String summary = "§ePrestige now?\n"
                + "§7This resets your stats. Click again within 10 seconds.\n"
                + "§7You'll be holding §6" + (held + 1) + "§7/§f" + MAX_HELD + ".";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, LmChat.DIVIDER);
            send(player, "§ePrestige now?");
            send(player, "§7This resets your stats and gives you one to hold.");
            send(player, "§7Your level §f" + DmzRewards.formatWhole(level)
                    + " §7is enough (needs §e" + DmzRewards.formatWhole(required) + "§7).");
            send(player, "§7You'll be holding §6" + (held + 1) + "§7/§f" + MAX_HELD + ".");
            send(player, "§7Click again within 10 seconds.");
            MutableComponent row = Component.m_237113_("§7")
                    .m_7220_(btn("§a[Confirm Prestige]", "/prestige do confirm", "Complete prestige"))
                    .m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§c[Cancel]", "/prestige", "Cancel"));
            send(player, row);
            send(player, LmChat.DIVIDER);
        }
        return summary;
    }

    private static String replyCap(ServerPlayer player, int held) {
        String summary = "§cYou're holding as many as you can.\n"
                + "§7That's §6" + held + "§7/§f" + MAX_HELD + ".\n"
                + "§7Spend one, then you can prestige again.";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, LmChat.DIVIDER);
            send(player, "§cYou're holding as many as you can.");
            send(player, "§7That's §6" + held + "§7/§f" + MAX_HELD + ".");
            send(player, "§7Spend one, then you can prestige again.");
            send(player, LmChat.DIVIDER);
        }
        return summary;
    }

    private static String replyRequirement(
            ServerPlayer player, int completed, int next, int required, int level
    ) {
        String summary = "§cNot high enough yet.\n"
                + "§7You need level §e" + DmzRewards.formatWhole(required)
                + "§7. You're at §f" + DmzRewards.formatWhole(level) + "§7.";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, LmChat.DIVIDER);
            send(player, "§cNot high enough yet.");
            send(player, "§7You need level §e" + DmzRewards.formatWhole(required)
                    + "§7. You're at §f" + DmzRewards.formatWhole(level) + "§7.");
            send(player, "§7You've prestiged §f" + completed + " §7times.");
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
     * Next prestige DMZ level gate — see class javadoc for the completed / held ladders.
     */
    public static int requiredLevel(ServerPlayer player) {
        if (player == null) {
            return LEVELS_PER_PRESTIGE;
        }
        int personalCap = PrestigePointsSystem.effectiveMaxLevel(player);
        int completed = getCompleted(player);
        int heldForNeed = heldCountForNeed(player);
        int need;
        if (completed >= HELD_GATE_MIN_COMPLETED) {
            // Veteran band — held table only; legacy (completed+1)×20k floors must not apply.
            need = requiredLevelForHeld(heldForNeed);
            clampVeteranNeedFloor(player, need);
        } else {
            need = needForProgress(player, completed, heldForNeed);
        }
        return Math.min(personalCap, need);
    }

    /** Pure ladder (no personal cap) — for admin previews and tests. */
    public static int needForProgress(int completed, int heldWallet) {
        int c = Math.max(0, completed);
        int h = Math.max(0, Math.min(MAX_HELD, heldWallet));
        if (c < HELD_GATE_MIN_COMPLETED) {
            return LEVELS_PER_PRESTIGE;
        }
        return requiredLevelForHeld(h);
    }

    private static int needForProgress(ServerPlayer player, int completed, int heldWallet) {
        int need = needForProgress(completed, heldWallet);
        if (completed < HELD_GATE_MIN_COMPLETED) {
            int ladder = LEVELS_PER_PRESTIGE;
            int floor = Math.min(getNeedFloor(player), MAX_REQUIRED_LEVEL);
            if (floor > ladder) {
                shrinkNeedFloor(player, ladder);
                floor = ladder;
            }
            need = Math.max(ladder, floor);
        }
        return need;
    }

    /** Need from the fifth prestige on — 50k with none held, 100k with one or more. */
    public static int requiredLevelForHeld(int held) {
        int h = Math.max(0, Math.min(MAX_HELD, held));
        return h <= 0 ? HELD0_NEED : HELD1_PLUS_NEED;
    }

    /** @deprecated prefer {@link #requiredLevel(ServerPlayer)} — uses absolute 150k ceiling. */
    public static int requiredLevel(int currentCompleted) {
        return requiredLevel(currentCompleted, PrestigePointsSystem.ABSOLUTE_LEVEL_CAP);
    }

    public static int requiredLevel(int currentCompleted, int personalCap) {
        int cap = personalCap > 0 ? personalCap : MAX_REQUIRED_LEVEL;
        int required = currentCompleted < HELD_GATE_MIN_COMPLETED ? LEVELS_PER_PRESTIGE : HELD0_NEED;
        return Math.min(cap, required);
    }

    public static int maxHeld() {
        return MAX_HELD;
    }

    /**
     * NBT held wallet only (not CNPC faction inflate). Fabled Prestige class
     * is always {@code wallet + 1}.
     */
    public static int getHeldWallet(ServerPlayer player) {
        return heldCountForNeed(player);
    }

    /** Wallet NBT for Need / Fabled sync — never CNPC faction (avoids 150k held≥3 gates). */
    public static int heldCountForNeed(ServerPlayer player) {
        if (player == null) {
            return 0;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_HELD)) {
            return Math.max(0, Math.min(MAX_HELD, tag.m_128451_(KEY_HELD)));
        }
        return 0;
    }

    /** Drop legacy need floors above the veteran held-table gate (e.g. old 140k/150k ladders). */
    public static void reconcileNeedFloor(ServerPlayer player) {
        if (player == null) {
            return;
        }
        int completed = getCompleted(player);
        if (completed < HELD_GATE_MIN_COMPLETED) {
            shrinkNeedFloor(player, LEVELS_PER_PRESTIGE);
            return;
        }
        int need = requiredLevelForHeld(heldCountForNeed(player));
        clampVeteranNeedFloor(player, need);
    }

    /** Lower corrupt {@link #KEY_NEED_FLOOR} values (never raise). */
    private static void shrinkNeedFloor(ServerPlayer player, int maxAllowed) {
        if (player == null || maxAllowed < 0) {
            return;
        }
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag) || !tag.m_128441_(KEY_NEED_FLOOR)) {
            return;
        }
        int floor = Math.max(0, readStoredInt(tag, KEY_NEED_FLOOR));
        if (floor > maxAllowed) {
            tag.m_128405_(KEY_NEED_FLOOR, maxAllowed);
        }
    }

    private static void clampVeteranNeedFloor(ServerPlayer player, int veteranNeed) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (!PersistentDataAccess.isWritable(tag) || !tag.m_128441_(KEY_NEED_FLOOR)) {
            return;
        }
        int floor = Math.max(0, readStoredInt(tag, KEY_NEED_FLOOR));
        if (floor > veteranNeed) {
            tag.m_128405_(KEY_NEED_FLOOR, veteranNeed);
        }
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
                raiseNeedFloor(player, needForProgress(player, legacy, getHeldWallet(player)));
                return legacy;
            }
            // Shop evidence of a past prestige when total was zeroed by turn-in.
            if (inferCompletedFromShop(player)) {
                setCompletedPublic(player, 1);
                raiseNeedFloor(player, needForProgress(player, 1, getHeldWallet(player)));
                return 1;
            }
            return 0;
        }

        // DMZ prestige skill = held wallet (Fabled−1), not lifetime completed — never bump total from it.
        if (stored <= 0 && inferCompletedFromShop(player)) {
            setCompletedPublic(player, 1);
            raiseNeedFloor(player, needForProgress(player, 1, getHeldWallet(player)));
            return 1;
        }
        return Math.max(0, stored);
    }

    /** Points / breakthroughs / permanent forms imply at least one completed prestige. */
    private static boolean inferCompletedFromShop(ServerPlayer player) {
        try {
            if (PrestigePointsSystem.getPoints(player) > 0) {
                return true;
            }
            if (PrestigePointsSystem.getBreakthroughs(player) > 0) {
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
        if (requiredMet > cur) {
            tag.m_128405_(KEY_NEED_FLOOR, requiredMet);
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
        tag.m_128405_(KEY_NEED_FLOOR, requiredLevel(player));
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
        if (value > 0) {
            raiseNeedFloor(player, requiredLevel(player));
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
