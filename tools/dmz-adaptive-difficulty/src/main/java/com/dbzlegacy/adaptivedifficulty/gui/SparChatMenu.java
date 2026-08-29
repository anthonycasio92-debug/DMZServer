package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.sparring.SparPlayerRuntime;
import com.dbzlegacy.adaptivedifficulty.sparring.SparStore;
import com.dbzlegacy.adaptivedifficulty.sparring.SparringSystem;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/** Sparring chat GUI — {@code /spar gui} / bare {@code /spar}. */
public final class SparChatMenu {
    private SparChatMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (!DifficultyConfig.get().enableSparringSystem) {
            send(player, Component.m_237113_("§cSparring system is disabled."));
            return;
        }
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page)) {
            main(player);
        } else if ("stats".equalsIgnoreCase(page)) {
            stats(player);
        } else if ("top".equalsIgnoreCase(page) || page.toLowerCase().startsWith("top ")) {
            String cat = page.toLowerCase().startsWith("top ")
                    ? page.substring(4).trim()
                    : "tp";
            top(player, cat);
        } else if ("mentor".equalsIgnoreCase(page)) {
            mentor(player);
        } else if ("help".equalsIgnoreCase(page)) {
            help(player);
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        SparPlayerRuntime rt = SparringSystem.runtime(player.m_20148_());
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics · Sparring §8──"));
        if (rt != null && rt.active) {
            String partnerName = "?";
            if (rt.partner != null && player.m_20194_() != null) {
                ServerPlayer p = player.m_20194_().m_6846_().m_11259_(rt.partner);
                if (p != null) {
                    partnerName = p.m_7755_().getString();
                }
            }
            send(player, Component.m_237113_("§aSession ACTIVE §8with §f"
                    + partnerName
                    + "  §7TP §f" + (int) rt.sessionTp));
            if (rt.sessionPerfect) {
                send(player, Component.m_237113_("§6§lPERFECT TRAINING"));
            }
        } else {
            send(player, Component.m_237113_("§7No active spar — trade hits within 30 blocks to start."));
        }
        if (bond != null && bond.mentorUuid != null && !bond.mentorUuid.isBlank()
                && bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank()) {
            boolean mentor = player.m_20148_().toString().equals(bond.mentorUuid);
            String other = mentor ? bond.apprenticeName : bond.mentorName;
            send(player, Component.m_237113_("§bMentor bond §7as "
                    + (mentor ? "mentor" : "apprentice") + " §8with §f" + other
                    + "  §7streak §f" + bond.streakCurrent));
        } else {
            send(player, Component.m_237113_("§7No mentor bond. §8/spar mentor <player>"));
        }
        send(player, Component.m_237113_(""));
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§e[Stats]", "/spar do page stats", "Last 3 finished spars"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§f[Top]", "/spar do page top", "Leaderboard"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§b[Mentor]", "/spar do page mentor", "Mentor controls"));
        if (rt != null && rt.active) {
            row.m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§c[End]", "/spar do end main", "End spar session"));
        }
        send(player, row);
        send(player, btn("§7« Hub", "/lm", "Legacy Mechanics hub"));
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void stats(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        for (String line : SparringSystem.statsLines(player)) {
            send(player, Component.m_237113_(line));
        }
        send(player, btn("§7« Back", "/spar do page main", "Main"));
    }

    private static void top(ServerPlayer player, String category) {
        send(player, Component.m_237113_(""));
        for (String line : SparringSystem.topLines(category, 10)) {
            send(player, Component.m_237113_(line));
        }
        MutableComponent cats = Component.m_237113_("§7")
                .m_7220_(btn("§e[TP]", "/spar do page top_tp", "Total TP"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§a[Sessions]", "/spar do page top_sessions", "Sessions"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§b[Perfect]", "/spar do page top_perfect", "Perfect spars"));
        send(player, cats);
        send(player, btn("§7« Back", "/spar do page main", "Main"));
    }

    private static void mentor(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §bMentor §8──"));
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        boolean hasMentor = bond != null
                && bond.mentorUuid != null && !bond.mentorUuid.isBlank();
        boolean hasApprentice = bond != null
                && bond.apprenticeUuid != null && !bond.apprenticeUuid.isBlank();
        if (hasMentor) {
            send(player, Component.m_237113_("§7Your mentor §f" + bond.mentorName));
        }
        if (hasApprentice) {
            send(player, Component.m_237113_("§7Your apprentice §f" + bond.apprenticeName));
        }
        if (hasMentor || hasApprentice) {
            send(player, Component.m_237113_("§7Streak §f" + bond.streakCurrent + " §8best §f" + bond.streakBest));
        } else {
            send(player, Component.m_237113_("§7Invite: §e/spar mentor <player> §8or §e/spar apprentice <player>"));
        }
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§a[Accept]", "/spar do mentor accept mentor", "Accept invite"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§c[Decline]", "/spar do mentor decline mentor", "Decline invite"));
        send(player, row);
        MutableComponent leaveRow = Component.m_237113_("§7");
        if (hasMentor) {
            leaveRow = leaveRow.m_7220_(btn("§c[Leave mentor]", "/spar do mentor leave mentor", "Leave your mentor"));
        }
        if (hasMentor && hasApprentice) {
            leaveRow = leaveRow.m_7220_(Component.m_237113_("  "));
        }
        if (hasApprentice) {
            leaveRow = leaveRow.m_7220_(
                    btn("§6[Release apprentice]", "/spar do mentor release mentor", "Release your apprentice"));
        }
        if (hasMentor || hasApprentice) {
            send(player, leaveRow);
        }
        send(player, btn("§7« Back", "/spar do page main", "Main"));
    }

    private static void help(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§6§l/spar §8— Sparring TP"));
        send(player, Component.m_237113_("§e/spar stats|end|top [category]"));
        send(player, Component.m_237113_("§e/spar mentor <player>|accept|decline|leave"));
        send(player, Component.m_237113_("§e/spar apprentice <player>|remove"));
        send(player, btn("§7« Back", "/spar do page main", "Main"));
    }

    private static MutableComponent btn(String label, String command, String hover) {
        return Component.m_237113_(label).m_6270_(Style.f_131099_
                .m_131142_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .m_131144_(new HoverEvent(HoverEvent.Action.f_130831_, Component.m_237113_(hover))));
    }

    private static void send(ServerPlayer player, Component text) {
        player.m_213846_(text);
    }
}
