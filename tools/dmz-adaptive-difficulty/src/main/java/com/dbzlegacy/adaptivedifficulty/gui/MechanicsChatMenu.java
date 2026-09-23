package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
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
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page) || "hub".equalsIgnoreCase(page)
                || "help".equalsIgnoreCase(page)) {
            main(player);
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
            if (StaffAccess.isStaff(player)) {
                ProgressionChatMenu.open(player, page);
            } else {
                main(player);
            }
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics §8──"));
        send(player, Component.m_237113_("§7Choose a system"));
        send(player, Component.m_237113_(""));
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§a[Difficulty]", "/difficulty", "Unlock tiers & scaling"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§6[Rival]", "/rival", "Rivalry & challenges"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§b[Spar]", "/spar", "Sparring TP & mentor"));
        send(player, row);
        boolean staff = StaffAccess.isStaff(player);
        boolean skillCheck = SkillCheckService.canUse(player);
        MutableComponent row2 = Component.m_237113_("§7");
        boolean row2Used = false;
        if (skillCheck || staff) {
            row2.m_7220_(btn(staff && !skillCheck ? "§e[Skills]" : "§e[Skill Check]",
                    staff && !skillCheck ? "/skills" : "/skillcheck",
                    staff && !skillCheck ? "Skill unlock admin" : "Donator skill progress"));
            row2Used = true;
        }
        if (row2Used) {
            row2.m_7220_(Component.m_237113_("  "));
        }
        row2.m_7220_(btn("§6[Prestige]", "/prestige",
                "Turn in · skill/forms shop · level-cap"));
        row2.m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§c[Remove Android]", "/lm open android_remove",
                        "Remove Android upgrade"));
        send(player, row2);
        if (staff) {
            MutableComponent row3 = Component.m_237113_("§7")
                    .m_7220_(btn("§d[Progression]", "/progression", "Natural progression"))
                    .m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§c[Admin]", "/lm admin help", "Admin commands"))
                    .m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§8[Logs]", "/lm page logs", "Server event logs"));
            send(player, row3);
        }
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void logs(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics · Logs §8──"));
        boolean on = DifficultyConfig.get().enableSystemTelemetry;
        send(player, Component.m_237113_("§7Event log §f" + (on ? "ON" : "OFF")));
        send(player, Component.m_237113_("§8" + com.dbzlegacy.adaptivedifficulty.telemetry.SystemTelemetry.statusLine()));
        MutableComponent toggles = Component.m_237113_("§7")
                .m_7220_(btn(on ? "§c[Event Log OFF]" : "§a[Event Log ON]",
                        "/lm do syslog " + (on ? "off" : "on"),
                        "Toggle server event logging"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§e[Flush]", "/lm do syslog flush", "Write buffered logs to disk"));
        send(player, toggles);
        send(player, btn("§7« Back", "/lm", "Main menu"));
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
