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
                .m_7220_(btn("§a[Difficulty]", "/lmdo lm open difficulty", "Unlock tiers & scaling"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§6[Rival]", "/lmdo lm open rival", "Rivalry & challenges"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§b[Spar]", "/lmdo lm open spar", "Sparring TP & mentor"));
        send(player, row);
        boolean staff = StaffAccess.isStaff(player);
        boolean skillCheck = SkillCheckService.canUse(player);
        if (skillCheck || staff) {
            MutableComponent row2 = Component.m_237113_("§7");
            if (skillCheck || staff) {
                row2.m_7220_(btn(staff && !skillCheck ? "§e[Skills]" : "§e[Skill Check]",
                        staff && !skillCheck ? "/lmdo lm open skills" : "/lmdo lm open skillcheck",
                        staff && !skillCheck ? "Skill unlock admin" : "Donator skill progress"));
            }
            if (staff) {
                row2.m_7220_(Component.m_237113_("  "))
                        .m_7220_(btn("§6[Prestige]", "/lmdo lm open prestige", "Prestige shop"));
            }
            send(player, row2);
        }
        if (staff) {
            MutableComponent row3 = Component.m_237113_("§7")
                    .m_7220_(btn("§d[Progression]", "/lmdo lm open progression", "Natural progression"))
                    .m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§c[Admin]", "/lm admin help", "Admin commands"))
                    .m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§8[Logs]", "/lmdo lm page logs", "System telemetry"));
            send(player, row3);
        }
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
                        "/lmdo lm syslog " + (on ? "off" : "on") + " logs",
                        "Toggle system telemetry"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§e[Flush]", "/lmdo lm syslog flush logs", "Flush log writers"));
        send(player, toggles);
        send(player, btn("§7« Back", "/lmdo lm page main", "Hub"));
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
