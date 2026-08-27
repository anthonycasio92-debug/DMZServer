package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.ProgressionSystem;
import com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression;
import com.dbzlegacy.adaptivedifficulty.progression.tp.GlobalTpBoost;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;

/** Progression chat GUI — {@code /progression} / {@code /prog}. */
public final class ProgressionChatMenu {
    private ProgressionChatMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (!DifficultyConfig.get().enableProgression) {
            send(player, Component.m_237113_("§cProgression system is disabled."));
            return;
        }
        if (page == null || page.isBlank() || "main".equalsIgnoreCase(page)) {
            main(player);
        } else if ("status".equalsIgnoreCase(page)) {
            status(player);
        } else if ("admin".equalsIgnoreCase(page)) {
            if (StaffAccess.isStaff(player)) {
                admin(player);
            } else {
                main(player);
            }
        } else if ("help".equalsIgnoreCase(page)) {
            help(player);
        } else {
            main(player);
        }
    }

    private static void main(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics · Progression §8──"));
        send(player, Component.m_237113_(GlobalTpBoost.statusLine()));
        send(player, Component.m_237113_(MeditationProgression.statusLine()));
        send(player, Component.m_237113_(""));
        MutableComponent row = Component.m_237113_("§7")
                .m_7220_(btn("§e[Status]", "/prog do page status", "Feature flags"))
                .m_7220_(Component.m_237113_("  "))
                .m_7220_(btn("§f[Help]", "/prog do page help", "Commands"));
        if (StaffAccess.isStaff(player)) {
            row.m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§c[Admin]", "/prog do page admin", "Toggle features"));
        }
        send(player, row);
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void status(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fProgression · Status §8──"));
        for (String line : ProgressionSystem.statusSummary().split("\n")) {
            send(player, Component.m_237113_("§7" + line));
        }
        send(player, btn("§7« Back", "/prog do page main", "Main"));
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void admin(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fProgression · Admin §8──"));
        DifficultyConfig c = DifficultyConfig.get();
        send(player, toggleRow("master", c.enableProgression));
        send(player, toggleRow("flight", c.enableFlightProgression));
        send(player, toggleRow("sprint", c.enableSprintJump));
        send(player, toggleRow("meditation", c.enableMeditation));
        send(player, toggleRow("potential", c.enablePotential));
        send(player, toggleRow("farming", c.enableFarmingTp));
        send(player, toggleRow("boost", c.enableGlobalTpBoost));
        send(player, toggleRow("bio", c.enableBioAndroid));
        send(player, toggleRow("racelock", c.enableRaceLock));
        send(player, toggleRow("yardrat", c.enableYardrat));
        send(player, toggleRow("spiritualist", c.enableSpiritualistKi));
        send(player, toggleRow("android", c.enableAndroidConversion));
        send(player, btn("§7« Back", "/prog do page main", "Main"));
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static MutableComponent toggleRow(String key, boolean on) {
        String label = on ? "§aON" : "§cOFF";
        String next = on ? "off" : "on";
        return Component.m_237113_("§7" + key + " §f" + label + " ")
                .m_7220_(btn("§8[toggle]", "/prog admin " + key + " " + next, "Toggle " + key));
    }

    private static void help(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fProgression · Help §8──"));
        send(player, Component.m_237113_("§e/progression §7— This menu"));
        send(player, Component.m_237113_("§e/progression boost start|end §7— Global TP boost (30/31)"));
        send(player, Component.m_237113_("§e/progression meditation next §7— Cycle trial (41)"));
        send(player, Component.m_237113_("§e/progression android §7— Android convert (45)"));
        if (StaffAccess.isStaff(player)) {
            send(player, Component.m_237113_("§8Staff: /prog admin <flag> on|off"));
        }
        send(player, btn("§7« Back", "/prog do page main", "Main"));
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
