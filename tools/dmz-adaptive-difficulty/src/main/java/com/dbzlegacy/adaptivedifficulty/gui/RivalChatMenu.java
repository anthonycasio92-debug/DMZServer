package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.rival.RivalChallengeManager;
import com.dbzlegacy.adaptivedifficulty.rival.RivalConstants;
import com.dbzlegacy.adaptivedifficulty.rival.RivalInstinct;
import com.dbzlegacy.adaptivedifficulty.rival.RivalPlayerRecord;
import com.dbzlegacy.adaptivedifficulty.rival.RivalProgression;
import com.dbzlegacy.adaptivedifficulty.rival.RivalStore;
import com.dbzlegacy.adaptivedifficulty.rival.RivalSystem;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/** Rival chat GUI — {@code /rival gui} / bare {@code /rival}. */
public final class RivalChatMenu {
    private RivalChatMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (!DifficultyConfig.get().enableRivalSystem) {
            send(player, Component.m_237113_("§cRival system is disabled."));
            return;
        }
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page)) {
            main(player);
        } else if ("list".equalsIgnoreCase(page)) {
            list(player);
        } else if ("history".equalsIgnoreCase(page) || "past".equalsIgnoreCase(page)) {
            lines(player, RivalSystem.historyLines(player), "history");
        } else if ("actions".equalsIgnoreCase(page)) {
            lines(player, RivalGuiApi.linesForPage(player, "actions"), "actions");
        } else if ("stats".equalsIgnoreCase(page)) {
            stats(player);
        } else if ("challenge".equalsIgnoreCase(page)) {
            challenge(player);
        } else if ("top".equalsIgnoreCase(page)) {
            top(player);
        } else if ("season".equalsIgnoreCase(page)) {
            lines(player, RivalProgression.get().seasonLines(player), "season");
        } else if ("quests".equalsIgnoreCase(page)) {
            lines(player, RivalProgression.get().questLines(player), "quests");
        } else if ("achievements".equalsIgnoreCase(page) || "achs".equalsIgnoreCase(page)) {
            lines(player, RivalProgression.get().achievementLines(player), "achievements");
        } else if ("hof".equalsIgnoreCase(page)) {
            lines(player, RivalProgression.get().hofLines(), "hof");
        } else if ("journal".equalsIgnoreCase(page)) {
            lines(player, RivalProgression.get().journalLines(player), "journal");
        } else if ("title".equalsIgnoreCase(page) || "titles".equalsIgnoreCase(page)) {
            lines(player, RivalProgression.get().titleLines(player), "title");
        } else if ("help".equalsIgnoreCase(page)) {
            help(player);
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        RivalPlayerRecord me = RivalStore.get().ensurePlayer(player);
        RivalConstants.RpTier tier = RivalConstants.tierFor(me.totalRp);
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics · Rival §8──"));
        send(player, Component.m_237113_("§7RP §f" + (int) me.totalRp
                + " §8(§" + tier.color() + tier.name() + "§8)"
                + "  §7Mutual §f" + me.countMutual() + "§8/§f" + RivalConstants.MAX_MUTUAL_RIVALS));
        send(player, Component.m_237113_("§7Record §a" + me.officialWins + "§7/§c" + me.officialLosses
                + "§7/§e" + me.officialDraws + " §8(wins/losses/draws)"
                + "  §7TP §f" + (me.tpMessages ? "ON" : "OFF")));
        boolean inCh = RivalChallengeManager.get().isInChallenge(player.m_20148_());
        if (inCh) {
            send(player, Component.m_237113_("§eChallenge active"));
        }
        send(player, Component.m_237113_(""));
        MutableComponent row1 = Component.m_237113_("§7")
                .m_7220_(btn("§6[List]", "/rival do page list", "Current rivals"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§a[Actions]", "/rival do page actions", "Declare · silent · invites · remove"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§c[Challenge]", "/rival do page challenge", "Challenge controls"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§f[Leaderboard]", "/rival do page top", "RP leaderboard"));
        send(player, row1);
        MutableComponent rowHist = Component.m_237113_("§7")
                .m_7220_(btn("§8[History]", "/rival do page history", "Previous rivals"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§e[Stats]", "/rival do page stats", "Career stats"));
        send(player, rowHist);
        MutableComponent row2 = Component.m_237113_("§7")
                .m_7220_(btn("§a[Season]", "/rival do page season", "Season RP"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§b[Quests]", "/rival do page quests", "Weekly quests"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§d[Achs]", "/rival do page achievements", "Achievements"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§6[HOF]", "/rival do page hof", "Hall of Fame"));
        send(player, row2);
        MutableComponent row3 = Component.m_237113_("§7")
                .m_7220_(btn("§f[Journal]", "/rival do page journal", "Battle journal"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§e[Title]", "/rival do page title", "Rival title"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn(me.tpMessages ? "§a[TP ON]" : "§8[TP OFF]",
                        "/rival do tpmsg toggle main", "Toggle TP chat"));
        if (DifficultyConfig.get().rivalInstinct) {
            boolean instinctOn = RivalInstinct.isEnabled(player);
            row3.m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn(instinctOn ? "§a[Instinct ON]" : "§8[Instinct OFF]",
                            "/rival do instinct toggle main", "Toggle Rival Instinct"));
        }
        send(player, row3);
        send(player, btn("§7« Hub", "/lm", "Main menu"));
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void list(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        for (String line : RivalSystem.listLines(player)) {
            send(player, Component.m_237113_(line));
        }
        send(player, btn("§7« Back", "/rival do page main", "Main"));
    }

    private static void stats(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        for (String line : RivalSystem.statsLines(player)) {
            send(player, Component.m_237113_(line));
        }
        send(player, btn("§7« Back", "/rival do page main", "Main"));
    }

    private static void challenge(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §cChallenge §8──"));
        send(player, Component.m_237113_("§7Send: §e/rival challenge send <player> [min]"));
        boolean inChallenge = RivalChallengeManager.get().isInChallenge(player.m_20148_());
        boolean outgoing = false;
        for (String card : RivalChallengeManager.get().pendingRequestCards(player)) {
            String[] parts = card.split("\t", -1);
            if (parts.length > 2 && "OUT".equals(parts[2])) {
                outgoing = true;
                break;
            }
        }
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§a[Accept]", "/rival do challenge accept main", "Accept oldest pending"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§c[Decline]", "/rival do challenge decline main", "Decline oldest pending"))
                .m_7220_(Component.m_237113_("  "));
        if (inChallenge && !outgoing) {
            row.m_7220_(btn("§c[Forfeit]", "/rival do challenge cancel main", "Forfeit your active challenge"));
        } else {
            row.m_7220_(btn("§8[Cancel request]", "/rival do challenge cancel main",
                    "Cancel your pending challenge request"));
        }
        send(player, row);
        send(player, btn("§7« Back", "/rival do page main", "Main"));
    }

    private static void top(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        for (String line : RivalSystem.topLines(10)) {
            send(player, Component.m_237113_(line));
        }
        send(player, btn("§7« Back", "/rival do page main", "Main"));
    }

    private static void help(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§6§l/rival §8— Rivals"));
        send(player, Component.m_237113_("§e/rival <player> §7silent rival"));
        send(player, Component.m_237113_("§e/rival declare|accept|decline|remove <player>"));
        send(player, Component.m_237113_("§e/rival challenge send <player> [minutes]"));
        send(player, Component.m_237113_("§e/rival spectate [player]|stop"));
        send(player, Component.m_237113_("§e/rival season|quests|achievements|hof|journal|title"));
        send(player, btn("§7« Back", "/rival do page main", "Main"));
    }

    private static void lines(ServerPlayer player, java.util.List<String> list, String page) {
        send(player, Component.m_237113_(""));
        for (String line : list) {
            send(player, Component.m_237113_(line));
        }
        send(player, btn("§7« Back", "/rival do page main", "Main"));
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
