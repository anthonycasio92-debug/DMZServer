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

    public static void confirmOrPrompt(ServerPlayer player) {
        if (!DifficultyConfig.get().enablePrestigeSystem || player == null) {
            return;
        }
        StatsData data = DmzProgression.stats(player);
        if (data == null) {
            DmzRewards.msg(player, "§cCould not read your Dragon Mine Z stats.");
            return;
        }
        int level;
        try {
            level = Math.max(0, data.getLevel());
        } catch (Throwable t) {
            DmzRewards.msg(player, "§cCould not determine your Dragon Mine Z level.");
            return;
        }

        int completed = getCompleted(player);
        int held = getHeld(player);
        int required = requiredLevel(completed);
        int next = completed + 1;

        if (held >= MAX_HELD) {
            sendCap(player, held);
            return;
        }
        if (level < required) {
            sendRequirement(player, completed, next, required, level);
            return;
        }

        long now = System.currentTimeMillis();
        Long until = CONFIRM_UNTIL.get(player.m_20148_());
        CompoundTag tag = PersistentDataAccess.get(player);
        if (until == null && PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_CONFIRM_UNTIL)) {
            until = tag.m_128454_(KEY_CONFIRM_UNTIL);
        }

        if (until != null && until > now) {
            purchase(player, data, completed, required, level);
            return;
        }

        long confirmUntil = now + CONFIRM_MS;
        CONFIRM_UNTIL.put(player.m_20148_(), confirmUntil);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128356_(KEY_CONFIRM_UNTIL, confirmUntil);
        }
        sendConfirm(player, completed, next, required, level, held);
    }

    private static void purchase(
            ServerPlayer player, StatsData data, int completed, int required, int level
    ) {
        CONFIRM_UNTIL.remove(player.m_20148_());
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128473_(KEY_CONFIRM_UNTIL);
        }

        int held = getHeld(player);
        if (held >= MAX_HELD) {
            sendCap(player, held);
            return;
        }
        if (level < required) {
            sendRequirement(player, completed, completed + 1, required, level);
            return;
        }

        int newHeld = held + 1;
        int newCompleted = completed + 1;
        setHeld(player, newHeld);
        setCompleted(player, newCompleted);

        String name = player.m_6302_();
        MinecraftServer server = player.m_20194_();
        if (server != null) {
            try {
                server.m_129892_().m_230957_(
                        server.m_129893_().m_81375_() == null
                                ? server.m_129892_().m_230957_(null, "/class level " + name + " add 1 Prestige")
                                : null);
            } catch (Throwable ignored) {
            }
            // Prefer dispatching via command source stack
            try {
                server.m_129892_().m_230957_(
                        server.m_129893_(),
                        "class level " + name + " add 1 Prestige"
                );
            } catch (Throwable t) {
                try {
                    server.m_129892_().m_230957_(server.m_129893_(), "dmzstats reset " + name);
                } catch (Throwable ignored) {
                }
            }
            try {
                server.m_129892_().m_230957_(server.m_129893_(), "dmzstats reset " + name);
            } catch (Throwable t) {
                AdaptiveDifficultyMod.LOGGER.debug(
                        "[{}] prestige dmzstats reset soft-fail: {}", AdaptiveDifficultyMod.MOD_ID, t.toString());
            }
        }

        int nextRequired = requiredLevel(newCompleted);
        send(player, "");
        send(player, "§8--------------------------------");
        send(player, "§aPrestige Level §f" + newCompleted + " §aComplete!");
        send(player, "§7Required level was §f" + DmzRewards.formatWhole(required));
        send(player, "§7Held Prestige Levels: §6" + newHeld + "§7/§f" + MAX_HELD);
        send(player, "§7Next Prestige requires §e" + DmzRewards.formatWhole(nextRequired) + " §7DMZ levels.");
        send(player, "§8--------------------------------");
        SystemTelemetry.log("prestige", "purchase", player, null, Map.of(
                "completed", newCompleted,
                "held", newHeld,
                "required", required
        ));
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

    private static void sendConfirm(
            ServerPlayer player, int completed, int next, int required, int level, int held
    ) {
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

    private static void sendCap(ServerPlayer player, int held) {
        send(player, "");
        send(player, "§8--------------------------------");
        send(player, "§cMaximum Prestige Levels Reached");
        send(player, "§7Available Prestige Levels: §6" + held + "§7/§f" + MAX_HELD);
        send(player, "§eUse one before prestiging again.");
        send(player, "§8--------------------------------");
    }

    private static void sendRequirement(
            ServerPlayer player, int completed, int next, int required, int level
    ) {
        send(player, "");
        send(player, "§8--------------------------------");
        send(player, "§cNot Ready for Prestige Level §f" + next);
        send(player, "§7Need §e" + DmzRewards.formatWhole(required)
                + " §7DMZ levels (have §f" + DmzRewards.formatWhole(level) + "§7).");
        send(player, "§7Completed prestiges: §f" + completed);
        send(player, "§8--------------------------------");
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
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag) && tag.m_128441_(KEY_HELD)) {
            return Math.max(0, tag.m_128451_(KEY_HELD));
        }
        return 0;
    }

    private static void setCompleted(ServerPlayer player, int value) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128359_(KEY_TOTAL, Integer.toString(Math.max(0, value)));
        }
    }

    private static void setHeld(ServerPlayer player, int value) {
        CompoundTag tag = PersistentDataAccess.get(player);
        if (PersistentDataAccess.isWritable(tag)) {
            tag.m_128405_(KEY_HELD, Math.max(0, Math.min(MAX_HELD, value)));
        }
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
