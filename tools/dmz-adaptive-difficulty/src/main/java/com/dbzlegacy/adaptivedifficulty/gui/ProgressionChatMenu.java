package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.progression.skills.MeditationProgression;
import com.dbzlegacy.adaptivedifficulty.progression.tp.GlobalTpBoost;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
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
        if (!StaffAccess.isStaff(player)) {
            send(player, Component.m_237113_("§cStaff only."));
            return;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        switch (p) {
            case "main" -> main(player);
            case "skills", "tp", "race", "combat", "end", "shop", "fabled", "utility", "status", "help" ->
                    category(player, p);
            case "admin", "flags", "disable", "flags_fabled", "fabled_flags" -> admin(player);
            default -> main(player);
        }
    }

    private static void main(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fLegacy Mechanics · Progression §8──"));
        send(player, Component.m_237113_(GlobalTpBoost.statusLine()));
        send(player, Component.m_237113_(MeditationProgression.statusLine()));
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§7")
                .m_7220_(btn("§e[Skills]", "/prog do page skills", "Passive skill unlocks"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§6[TP]", "/prog do page tp", "TP gains"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§b[Race]", "/prog do page race", "Race & form"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§c[Combat]", "/prog do page combat", "Combat ports")));
        send(player, Component.m_237113_("§7")
                .m_7220_(btn("§5[End]", "/prog do page end", "End dimension"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§a[Shop]", "/prog do page shop", "Prestige & skills"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§d[Fabled]", "/prog do page fabled", "Fabled bridges"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§7[Utility]", "/prog do page utility", "Utility"))
                .m_7220_(Component.m_237113_(" "))
                .m_7220_(btn("§f[Help]", "/prog do page help", "Commands")));
        if (StaffAccess.isStaff(player)) {
            send(player, Component.m_237113_("§7")
                    .m_7220_(btn("§c[Flags]", "/prog do page admin", "Toggle features")));
        }
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void category(ServerPlayer player, String page) {
        String title = switch (page) {
            case "skills" -> "Skills";
            case "tp" -> "TP Gains";
            case "race" -> "Race & Form";
            case "combat" -> "Combat";
            case "end" -> "End";
            case "shop" -> "Shop";
            case "fabled" -> "Fabled Bridges";
            case "utility" -> "Utility";
            case "status" -> "Status";
            case "help" -> "Help";
            default -> page;
        };
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fProgression · " + title + " §8──"));
        List<String> lines = ProgressionGuiApi.linesForPage(player, page);
        for (String line : lines) {
            send(player, Component.m_237113_(line));
        }
        if ("shop".equals(page)) {
            send(player, Component.m_237113_("§7")
                    .m_7220_(btn("§6[Open Prestige]", "/lmdo lm open prestige", "Prestige GUI"))
                    .m_7220_(Component.m_237113_("  "))
                    .m_7220_(btn("§f[Open Skills]", "/skills", "Skills GUI")));
        }
        send(player, btn("§7« Back", "/prog do page main", "Main"));
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static void admin(ServerPlayer player) {
        send(player, Component.m_237113_(""));
        send(player, Component.m_237113_("§8── §fProgression · Staff Flags §8──"));
        List<String> lines = ProgressionGuiApi.linesForPage(player, "admin");
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            // Section headers stay plain; flag lines get toggle buttons
            if (line.contains("§l") && !line.contains("ON") && !line.contains("OFF")) {
                send(player, Component.m_237113_(line));
                continue;
            }
            // Parse "§7key §aON" / "§7key §cOFF"
            String stripped = line.replace("§7", "").replace("§a", "").replace("§c", "")
                    .replace("§f", "").trim();
            int space = stripped.lastIndexOf(' ');
            if (space > 0) {
                String key = stripped.substring(0, space).trim();
                boolean on = stripped.endsWith("ON");
                if (!key.isBlank() && (stripped.endsWith("ON") || stripped.endsWith("OFF"))) {
                    send(player, toggleRow(key, on));
                    continue;
                }
            }
            send(player, Component.m_237113_(line));
        }
        send(player, btn("§7« Back", "/prog do page main", "Main"));
        send(player, Component.m_237113_("§8────────────────"));
    }

    private static MutableComponent toggleRow(String key, boolean on) {
        String label = on ? "§aON" : "§cOFF";
        String next = on ? "off" : "on";
        return Component.m_237113_("§7" + key + " §f" + label + " ")
                .m_7220_(btn("§8[toggle]", "/prog admin " + key + " " + next, "Toggle " + key));
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
