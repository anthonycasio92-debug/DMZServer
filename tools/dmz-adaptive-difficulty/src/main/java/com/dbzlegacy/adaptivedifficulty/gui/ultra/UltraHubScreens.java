package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Legacy Mechanics hub — system entry points and staff admin. */
public final class UltraHubScreens {
    private UltraHubScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        if ("admin".equals(p)) {
            openAdmin(player);
            return;
        }
        openMain(player);
    }

    private static void openMain(ServerPlayer player) {
        if (player == null) {
            return;
        }
        Map<String, String> ph = MechanicsGuiApi.placeholders(player);
        if (ph == null) {
            ph = Map.of();
        }
        List<String> status = MechanicsGuiApi.linesForPage(player, "main");
        UltraScreenSpec spec = UltraScreenSpec.of("hub")
                .title("Legacy Mechanics")
                .page("main")
                .activeTab("home");
        if (status != null) {
            for (String line : status) {
                if (line != null && !line.isBlank()) {
                    spec.line(line);
                }
            }
        }
        spec.setRow(72);
        systemButton(spec, ph, "difficulty", UltraStyle.confirm("Difficulty"), true);
        systemButton(spec, ph, "rival", "Rival", false);
        spec.rowGap();
        systemButton(spec, ph, "spar", "Sparring", true);
        systemButton(spec, ph, "prestige", "Prestige", false);
        spec.rowGap();
        spec.buttonLeft("open:character", UltraStyle.confirm("Character Services"), "normal");
        if (StaffAccess.isStaff(player)) {
            spec.buttonRight("open:admin", UltraStyle.danger("Staff Admin"), "danger");
        }
        spec.rowGap().setRow(spec.row() + UltraStyle.BTN_GAP).footerBackToMain();
        spec.send(player);
    }

    private static void openAdmin(ServerPlayer player) {
        if (player == null) {
            return;
        }
        if (!StaffAccess.isStaff(player)) {
            openMain(player);
            return;
        }
        UltraScreenSpec spec = UltraScreenSpec.of("admin")
                .title("Staff Admin")
                .page("admin")
                .activeTab("home")
                .line(UltraStyle.LABEL + "Reload, config editor, progression tools")
                .line(UltraStyle.DIM + "Full commands: /lm admin help")
                .setRow(64);
        spec.button("do:reload", UltraStyle.confirm("Reload LM config"), "confirm")
                .rowGap();
        spec.buttonLeft("open:progression", "Progression panel", "normal")
                .buttonRight("open:config", "Config editor", "normal")
                .rowGap();
        spec.setRow(spec.row() + 8).footerBackToMain();
        spec.send(player);
    }

    private static void systemButton(
            UltraScreenSpec spec, Map<String, String> ph, String system, String label, boolean left) {
        if (spec == null || system == null) {
            return;
        }
        boolean on = "difficulty".equals(system)
                || (ph != null && "true".equalsIgnoreCase(ph.get(system)));
        String action = "open:" + ("spar".equals(system) ? "spar" : system);
        String styled = on ? label : UltraStyle.dim(UltraStyle.plain(label) + " · off");
        String style = on ? "normal" : "dim";
        if (left) {
            spec.buttonLeft(action, styled, style);
        } else {
            spec.buttonRight(action, styled, style);
        }
    }
}
