package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/** Hub chat menu for Legacy Mechanics — {@code /legacymechanics} / {@code /lm}. */
public final class MechanicsChatMenu {
    private MechanicsChatMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page) || "hub".equalsIgnoreCase(page)) {
            main(player);
        } else if ("help".equalsIgnoreCase(page)) {
            help(player);
        } else if ("logs".equalsIgnoreCase(page) || "syslog".equalsIgnoreCase(page)) {
            if (StaffAccess.isStaff(player)) {
                logs(player);
            } else {
                main(player);
            }
        } else if ("progression".equalsIgnoreCase(page)
                || "prog".equalsIgnoreCase(page)
                || "fabled".equalsIgnoreCase(page)
                || "bridge".equalsIgnoreCase(page)
                || "disable".equalsIgnoreCase(page)
                || "cnpc".equalsIgnoreCase(page)) {
            ProgressionChatMenu.open(player, page);
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics §8──"));
        send(player, Component.m_237113_("§7Difficulty · Rival · Sparring · Progression"));
        send(player, Component.m_237113_(""));
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§a[Difficulty]", "/difficulty", "Open difficulty menu"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§6[Rival]", "/rival gui", "Open rival menu"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§b[Sparring]", "/spar gui", "Open sparring menu"));
        send(player, row);
        MutableComponent row2 = Component.m_237113_("§7")
                .m_7220_(btn("§d[Progression]", "/lm do page progression", "Natural progression + Fabled"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§f[Help]", "/lm do page help", "Command overview"));
        if (StaffAccess.isStaff(player)) {
            row2.m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§8[Logs]", "/lm do page logs", "System telemetry status"));
        }
        send(player, row2);
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void help(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics · Help §8──"));
        send(player, Component.m_237113_("§e/difficulty §7— Unlock tiers & scaling"));
        send(player, Component.m_237113_("§e/rival §7— Rivalry, challenges, progression"));
        send(player, Component.m_237113_("§e/spar §7— Sparring TP & mentor"));
        send(player, Component.m_237113_("§e/lm §7— This hub"));
        send(player, Component.m_237113_("§e/lm do page progression §7— Natural progression / Fabled"));
        if (StaffAccess.isStaff(player)) {
            send(player, Component.m_237113_("§8Staff: /difficulty admin · /lm do page logs"));
        }
        send(player, btn("§7« Back", "/lm do page main", "Hub"));
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void logs(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics · Logs §8──"));
        boolean on = DifficultyConfig.get().enableSystemTelemetry;
        send(player, Component.m_237113_("§7System telemetry §f" + (on ? "ON" : "OFF")));
        send(player, Component.m_237113_("§8" + com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry.statusLine()));
        MutableComponent toggles = Component.m_237113_("§7")
                .m_7220_(btn(on ? "§c[Syslog OFF]" : "§a[Syslog ON]",
                        "/difficulty admin syslog " + (on ? "off" : "on"),
                        "Toggle system telemetry"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§e[Flush]", "/difficulty admin syslog flush", "Flush log writers"));
        send(player, toggles);
        send(player, btn("§7« Back", "/lm do page main", "Hub"));
        send(player, Component.m_237113_("§8────────────────"));
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
