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
        } else if ("top".equalsIgnoreCase(page) || page.toLowerCase().startsWith("top ")
                || page.toLowerCase().startsWith("top_")) {
            String lower = page.toLowerCase();
            String cat = lower.startsWith("top ")
                    ? page.substring(4).trim()
                    : (lower.startsWith("top_") ? page.substring(4).trim() : "tp");
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
                send(player, Component.m_237113_("§6Perfect spar §7— hits are close enough for the training bonus."));
            }
        } else {
            send(player, Component.m_237113_("§7No sparring session right now — hit each other within 30 blocks to start one."));
        }
        boolean hasMentor = bond != null && bond.mentorUuid != null && !bond.mentorUuid.isBlank();
        boolean hasApps = bond != null && bond.apprenticeCount() > 0;
        if (hasMentor || hasApps) {
            if (hasMentor) {
                send(player, Component.m_237113_("§bApprentice of §f" + blank(bond.mentorName, "?")
                        + "  §7streak §f" + bond.streakCurrent));
            }
            if (hasApps) {
                send(player, Component.m_237113_("§bMentoring §f" + bond.apprenticeCount()
                        + "§8/§f" + SparringSystem.MAX_APPRENTICES
                        + " §8— §f" + bond.apprenticeNamesSummary()
                        + "  §7streak §f" + bond.streakCurrent));
            }
        } else {
            send(player, Component.m_237113_("§7No mentor bond. §8Spar GUI → Training bonds"));
        }
        send(player, Component.m_237113_(""));
        boolean tpOn = SparStore.get().tpMessagesOn(player.m_20148_());
        boolean mentorTpOn = SparStore.get().mentorTpMessagesOn(player.m_20148_());
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§e[Stats]", "/spar do page stats", "Last 3 spar reports"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§f[Top]", "/spar do page top", "Leaderboard"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§b[Training bonds]", "/spar do page mentor", "Training bonds"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn(tpOn ? "§a[TP ON]" : "§8[TP OFF]",
                        "/spar do tpmsg toggle main",
                        tpOn ? "Mute spar TP chat while fighting" : "Show spar TP chat while fighting"));
        row.m_7220_(Component.m_237113_("  "))
                .m_7220_(btn(mentorTpOn ? "§a[Bond TP ON]" : "§8[Bond TP OFF]",
                        "/spar do mentor_tpmsg toggle main",
                        mentorTpOn ? "Mute bond share TP chat" : "Show bond share TP in chat"));
        if (rt != null && rt.active) {
            row.m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§c[End]", "/spar do end main", "End spar session"));
        }
        send(player, row);
        send(player, btn("§7« Hub", "/lm", "Main hub"));
    }

    private static void stats(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§6§lLast 3 Spar Reports"));
        for (String line : SparringSystem.statsLines(player)) {
            if (line != null && line.contains("Spar Report #") && !line.contains("Spar Report #1")) {
                send(player, Component.m_237113_(""));
            }
            send(player, Component.m_237113_(line));
        }
        send(player, btn("§7« Back", "/spar do page main", "Go up one level"));
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
        send(player, btn("§7« Back", "/spar do page main", "Go up one level"));
    }

    private static void mentor(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §bTraining bonds §8──"));
        SparStore.MentorBond bond = SparStore.get().bond(player.m_20148_());
        boolean hasMentor = bond != null
                && bond.mentorUuid != null && !bond.mentorUuid.isBlank();
        int appCount = bond == null ? 0 : bond.apprenticeCount();
        boolean hasApprentice = appCount > 0;
        if (hasMentor) {
            send(player, Component.m_237113_("§7Your mentor §f" + bond.mentorName));
        }
        if (hasApprentice) {
            send(player, Component.m_237113_("§7Your apprentices §f" + appCount
                    + "§8/§f" + SparringSystem.MAX_APPRENTICES + " §8— §f" + bond.apprenticeNamesSummary()));
        }
        if (hasMentor || hasApprentice) {
            send(player, Component.m_237113_("§7Streak §f" + bond.streakCurrent + " §8best §f" + bond.streakBest));
        } else {
            send(player, Component.m_237113_("§7Use §bTraining bonds §7in the Spar GUI"));
            send(player, Component.m_237113_("§8Invite apprentice · Request a master · Bond invites · Dojo · Leave / Release"));
            send(player, Component.m_237113_("§8Dojo up to §f" + SparringSystem.MAX_APPRENTICES
                    + " §8apprentices · one master · 12h cooldown"));
        }
        send(player, btn("§b[Open Training bonds]", "/spar do page mentor", "Training bonds"));
        send(player, btn("§7« Back", "/spar do page main", "Go up one level"));
    }

    private static void help(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§6§l/spar §8— Sparring TP"));
        send(player, Component.m_237113_("§e/spar stats|end|top [category]"));
        send(player, Component.m_237113_("§7Mentor bonds: §bSpar GUI → Training bonds"));
        send(player, btn("§7« Back", "/spar do page main", "Go up one level"));
    }

    private static MutableComponent btn(String label, String command, String hover) {
        return Component.m_237113_(label).m_6270_(Style.f_131099_
                .m_131142_(new ClickEvent(ClickEvent.Action.RUN_COMMAND, command))
                .m_131144_(new HoverEvent(HoverEvent.Action.f_130831_, Component.m_237113_(hover))));
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static void send(ServerPlayer player, Component text) {
        player.m_213846_(text);
    }
}
