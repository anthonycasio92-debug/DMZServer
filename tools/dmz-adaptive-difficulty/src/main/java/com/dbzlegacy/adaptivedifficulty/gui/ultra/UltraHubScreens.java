package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
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
        spec.setRow(80);
        systemButton(
                spec, ph, "difficulty", UltraStyle.confirm("Difficulty"), UltraStyle.STYLE_CONFIRM, true);
        systemButton(spec, ph, "rival", "Rival", UltraStyle.STYLE_NORMAL, false);
        spec.rowGap();
        systemButton(spec, ph, "spar", "Sparring", UltraStyle.STYLE_NORMAL, true);
        systemButton(spec, ph, "prestige", "Prestige", UltraStyle.STYLE_NORMAL, false);
        spec.rowGap();
        // Match CNPC hub: Skill Check (or staff Skills) left, Character Services right.
        boolean skillCheck = SkillCheckService.canUse(player);
        boolean staff = StaffAccess.isStaff(player);
        if (skillCheck) {
            spec.buttonLeft(
                    "open:skillcheck", UltraStyle.confirm("Skill Check"), UltraStyle.STYLE_CONFIRM);
        } else if (staff) {
            spec.buttonLeft("open:skills", UltraStyle.confirm("Skills"), UltraStyle.STYLE_CONFIRM);
        } else {
            // Unavailable to this player — dim style must be on the wire for KaiPath.
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
        // Hub IS the main menu — do not call footerBackToMain() here.
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
        // Admin is not the hub — back to hub is fine. Do not call footerBackToMain()
        // anywhere in this class (hub main must never ship « Back to main menu).
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
     * Hub system tile. When the system is off / unavailable, force {@link UltraStyle#STYLE_DIM}
     * so KaiPath can render a muted button (not only a dim-colored label).
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
        String styled = on ? label : UltraStyle.dim(UltraStyle.plain(label) + " · off");
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
