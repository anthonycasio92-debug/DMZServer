package com.dbzlegacy.adaptivedifficulty.progression.shop;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
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
 * Cost = DMZ level gate ({@code (current+1) * 20000}, capped at 100000).
 */
public final class PrestigeSystem {
    private static final int LEVELS_PER_PRESTIGE = 20_000;
    private static final int MAX_REQUIRED_LEVEL = 100_000;
    private static final int MAX_HELD = 10;
    private static final long CONFIRM_MS = 10_000L;
    /** Live Prestige NPC.js — held tokens live on CNPC faction 4. */
    private static final int FACTION_HELD_ID = 4;

    private static final String KEY_TOTAL = "prestige_total_completed";
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
        int required = requiredLevel(completed);
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
        resetPrestigeProgress(player);

        String name = player.m_6302_();
        MinecraftServer server = player.m_20194_();
        if (server != null) {
            try {
                server.m_129892_().m_230957_(
                        server.m_129893_(),
                        "class level " + name + " add 1 Prestige"
                );
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] prestige class level soft-fail: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            }
            try {
                server.m_129892_().m_230957_(server.m_129893_(), "dmzstats reset " + name);
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] prestige dmzstats reset soft-fail: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            }
        }

        int nextRequired = requiredLevel(newCompleted);
        String summary = "§aPrestige Level §f" + newCompleted + " §aComplete!\n"
                + "§7Held: §6" + newHeld + "§7/§f" + MAX_HELD + "\n"
                + "§7Next needs §e" + DmzRewards.formatWhole(nextRequired) + " §7DMZ levels.";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, "§8--------------------------------");
            send(player, "§aPrestige Level §f" + newCompleted + " §aComplete!");
            send(player, "§7Required level was §f" + DmzRewards.formatWhole(required));
            send(player, "§7Held Prestige Levels: §6" + newHeld + "§7/§f" + MAX_HELD);
            send(player, "§7Next Prestige requires §e" + DmzRewards.formatWhole(nextRequired) + " §7DMZ levels.");
            send(player, "§8--------------------------------");
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
        int required = requiredLevel(completed);
        send(player, "");
        send(player, "§8── §6Prestige §8──");
        send(player, "§7Completed: §f" + completed + " §8| §7Held: §6" + held + "§7/§f" + MAX_HELD);
        send(player, "§7DMZ Level: §f" + DmzRewards.formatWhole(level)
                + " §8| §7Need: §e" + DmzRewards.formatWhole(required));
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
        String summary = "§eConfirm Prestige Level §f" + next + "§e?\n"
                + "§7Click again within 10s · resets DMZ stats\n"
                + "§7Held after: §6" + (held + 1) + "§7/§f" + MAX_HELD;
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, "§8--------------------------------");
            send(player, "§eConfirm Prestige Level §f" + next + "§e?");
            send(player, "§7This resets DMZ stats and awards one held Prestige Level.");
            send(player, "§7Your level §f" + DmzRewards.formatWhole(level)
                    + " §7meets §e" + DmzRewards.formatWhole(required) + "§7.");
            send(player, "§7Held after: §6" + (held + 1) + "§7/§f" + MAX_HELD);
            send(player, "§8Click again within 10s to confirm.");
            MutableComponent row = Component.m_237113_("§7")
                    .m_7220_(btn("§a[Confirm Prestige]", "/prestige do confirm", "Complete prestige"))
                    .m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§c[Cancel]", "/prestige", "Cancel"));
            send(player, row);
            send(player, "§8--------------------------------");
        }
        return summary;
    }

    private static String replyCap(ServerPlayer player, int held) {
        String summary = "§cMaximum Prestige Levels Reached\n"
                + "§7Available: §6" + held + "§7/§f" + MAX_HELD + "\n"
                + "§eUse one before prestiging again.";
        if (!preferGuiFeedback()) {
            send(player, "");
            send(player, "§8--------------------------------");
            send(player, "§cMaximum Prestige Levels Reached");
            send(player, "§7Available Prestige Levels: §6" + held + "§7/§f" + MAX_HELD);
            send(player, "§eUse one before prestiging again.");
            send(player, "§8--------------------------------");
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
            send(player, "§8--------------------------------");
            send(player, "§cNot Ready for Prestige Level §f" + next);
            send(player, "§7Need §e" + DmzRewards.formatWhole(required)
                    + " §7DMZ levels (have §f" + DmzRewards.formatWhole(level) + "§7).");
            send(player, "§7Completed prestiges: §f" + completed);
            send(player, "§8--------------------------------");
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

    public static int requiredLevel(int currentCompleted) {
        int required = (currentCompleted + 1) * LEVELS_PER_PRESTIGE;
        return Math.min(MAX_REQUIRED_LEVEL, required);
    }

    public static int getCompleted(ServerPlayer player) {
        int fromSkill = Math.max(0, DmzProgression.prestige(player));
        CompoundTag tag = PersistentDataAccess.get(player);
        int stored = 0;
        if (PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_TOTAL)) {
            try {
                stored = Integer.parseInt(tag.m_128461_(KEY_TOTAL));
            } catch (Exception e) {
                stored = (int) tag.m_128451_(KEY_TOTAL);
            }
        }
        return Math.max(fromSkill, stored);
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
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128359_(KEY_TOTAL, Integer.toString(Math.max(0, value)));
        }
    }

    private static void setHeld(ServerPlayer player, int value) {
        int clamped = Math.max(0, Math.min(MAX_HELD, value));
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128405_(KEY_HELD, clamped);
        }
        // Dual-write to faction 4 so race-unlock shops that spend faction tokens stay in sync.
        Integer current = readFactionPoints(player, FACTION_HELD_ID);
        if (current != null) {
            int delta = clamped - current;
            if (delta != 0) {
                addFactionPoints(player, FACTION_HELD_ID, delta);
            }
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
        player.m_213846_(Component.m_237113_(text));
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
