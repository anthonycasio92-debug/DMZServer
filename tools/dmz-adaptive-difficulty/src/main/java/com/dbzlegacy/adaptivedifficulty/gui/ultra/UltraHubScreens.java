package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
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
        boolean staff = StaffAccess.isStaff(player);
        UltraScreenSpec spec = UltraScreenSpec.of("hub")
                .title("Legacy Mechanics")
                .page("main")
                .activeTab("home")
                .line(UltraStyle.LABEL + "Choose a system to open.")
                .setRow(80);
        systemButton(
                spec, ph, "difficulty", UltraStyle.confirm("Difficulty"), UltraStyle.STYLE_CONFIRM, true);
        systemButton(spec, ph, "rival", "Rival", UltraStyle.STYLE_NORMAL, false);
        spec.rowGap();
        systemButton(spec, ph, "spar", "Sparring", UltraStyle.STYLE_NORMAL, true);
        systemButton(spec, ph, "prestige", "Prestige", UltraStyle.STYLE_NORMAL, false);
        spec.rowGap();
        boolean skillCheck = SkillCheckService.canUse(player);
        if (skillCheck) {
            spec.buttonLeft(
                    "open:skillcheck", UltraStyle.confirm("Skill Check"), UltraStyle.STYLE_CONFIRM);
        } else if (staff) {
            spec.buttonLeft("open:skills", UltraStyle.confirm("Skills"), UltraStyle.STYLE_CONFIRM);
        } else {
            // Unavailable — dim style on the wire for KaiPath (no on/off status text).
            spec.buttonLeft(
                    "do:skillcheck_denied",
                    UltraStyle.dim("Skill Check"),
                    UltraStyle.STYLE_DIM);
        }
        spec.buttonRight(
                "open:character",
                UltraStyle.confirm("Character Services"),
                UltraStyle.STYLE_CONFIRM);
        spec.rowGap();
        if (staff) {
            spec.buttonLeft("open:admin", UltraStyle.danger("Staff Admin"), UltraStyle.STYLE_DANGER);
            spec.rowGap();
        }
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
                .setRow(80);
        spec.button("do:reload", UltraStyle.confirm("Reload LM config"), UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.buttonLeft("open:progression", "Progression panel", UltraStyle.STYLE_NORMAL)
                .buttonRight("open:config", "Config editor", UltraStyle.STYLE_NORMAL)
                .rowGap();
        spec.setRow(spec.row() + UltraStyle.BTN_GAP)
                .button(
                        "nav:hub",
                        UltraStyle.BACK_TO_MAIN,
                        UltraStyle.STYLE_DIM,
                        UltraStyle.COL_L,
                        spec.row(),
                        UltraStyle.BTN_W);
        spec.send(player);
    }

    /**
     * Hub system tile. When the system is unavailable, force {@link UltraStyle#STYLE_DIM}
     * so KaiPath can render a muted button (no on/off status text for players).
     */
    private static void systemButton(
            UltraScreenSpec spec,
            Map<String, String> ph,
            String system,
            String label,
            String onStyle,
            boolean left) {
        if (spec == null || system == null) {
            return;
        }
        boolean on = "difficulty".equals(system)
                || (ph != null && "true".equalsIgnoreCase(ph.get(system)));
        String action = "open:" + ("spar".equals(system) ? "spar" : system);
        String styled = on ? label : UltraStyle.dim(UltraStyle.plain(label));
        String style = on
                ? (onStyle == null || onStyle.isBlank() ? UltraStyle.STYLE_NORMAL : onStyle)
                : UltraStyle.STYLE_DIM;
        if (left) {
            spec.buttonLeft(action, styled, style);
        } else {
            spec.buttonRight(action, styled, style);
        }
    }
}
