package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Legacy Mechanics hub - system entry points and staff admin.
 *
 * <p>Tiles are collected into a list then packed two-up so adding or gating systems
 * automatically grows or shrinks the panel (no fixed empty body band / choose prompt).
 * Hub tiles use {@link UltraStyle#STYLE_NORMAL} (dark) plus a per-system {@code accent}
 * letter for KaiPath to tint. Confirm/yellow is reserved for selected actions inside
 * system menus — never on the hub landing. Staff Admin uses accent {@code 4} (dark red),
 * not {@link UltraStyle#STYLE_DANGER} (action danger is for destructive buttons).
 */
public final class UltraHubScreens {
    private UltraHubScreens() {}

    /** One hub system tile. {@code alwaysOn} skips the placeholder availability check. */
    private record HubTile(String system, String label, String onStyle, boolean alwaysOn) {}

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
                .activeTab("home");

        // Hub tiles: normal (dark) + per-system accent. Nothing is "selected" on the hub.
        List<HubTile> tiles = new ArrayList<>();
        tiles.add(new HubTile("difficulty", "Difficulty", UltraStyle.STYLE_NORMAL, true));
        tiles.add(new HubTile("rival", "Rival", UltraStyle.STYLE_NORMAL, false));
        tiles.add(new HubTile("spar", "Sparring", UltraStyle.STYLE_NORMAL, false));
        tiles.add(new HubTile("prestige", "Prestige", UltraStyle.STYLE_NORMAL, false));

        boolean skillCheck = SkillCheckService.canUse(player);
        if (skillCheck) {
            tiles.add(new HubTile("skillcheck", "Skill Check", UltraStyle.STYLE_NORMAL, true));
        } else if (staff) {
            tiles.add(new HubTile("skills", "Skills", UltraStyle.STYLE_NORMAL, true));
        } else {
            // Dim locked tile - still occupies a slot so layout stays predictable.
            tiles.add(new HubTile("skillcheck_denied", "Skill Check", UltraStyle.STYLE_DIM, true));
        }
        tiles.add(new HubTile("character", "Character Services", UltraStyle.STYLE_NORMAL, true));
        if (staff) {
            tiles.add(new HubTile("admin", "Staff Admin", UltraStyle.STYLE_NORMAL, true));
        }

        placeTiles(spec, ph, tiles);
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
                .setRow(UltraStyle.CONTENT_ROW + UltraStyle.BODY_MIN_H);
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
     * Pack hub tiles two-up. Panel height follows {@link UltraScreenSpec} content packing
     * (compact min height when there is no body copy), so more systems just add rows.
     */
    private static void placeTiles(UltraScreenSpec spec, Map<String, String> ph, List<HubTile> tiles) {
        if (spec == null || tiles == null || tiles.isEmpty()) {
            return;
        }
        int placed = 0;
        for (HubTile tile : tiles) {
            if (tile == null || tile.system == null || tile.system.isBlank()) {
                continue;
            }
            boolean left = placed % 2 == 0;
            placeTile(spec, ph, tile, left);
            placed++;
            if (!left) {
                spec.rowGap();
            }
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
    }

    private static void placeTile(
            UltraScreenSpec spec, Map<String, String> ph, HubTile tile, boolean left) {
        String system = tile.system();
        boolean on = tile.alwaysOn()
                || "difficulty".equals(system)
                || (ph != null && "true".equalsIgnoreCase(ph.get(system)));
        String action = actionFor(system);
        String styled = on ? tile.label() : UltraStyle.dim(UltraStyle.plain(tile.label()));
        String style = on
                ? (tile.onStyle() == null || tile.onStyle().isBlank()
                        ? UltraStyle.STYLE_NORMAL
                        : tile.onStyle())
                : UltraStyle.STYLE_DIM;
        String accent = UltraStyle.hubTileAccentCode(system);
        if (left) {
            spec.buttonLeft(action, styled, style, accent);
        } else {
            spec.buttonRight(action, styled, style, accent);
        }
    }

    private static String actionFor(String system) {
        if (system == null || system.isBlank()) {
            return "open:hub";
        }
        return switch (system) {
            case "spar" -> "open:spar";
            case "skillcheck" -> "open:skillcheck";
            case "skills" -> "open:skills";
            case "skillcheck_denied" -> "do:skillcheck_denied";
            case "admin" -> "open:admin";
            case "character" -> "open:character";
            default -> "open:" + system;
        };
    }
}
