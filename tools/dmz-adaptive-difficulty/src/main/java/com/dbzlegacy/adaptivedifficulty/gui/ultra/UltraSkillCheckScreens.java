package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi;
import com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/**
 * Skill Check (donator) and Skills (staff) — Ultra port of {@code CnpcLmSkillCheckGui}.
 *
 * <p>Three-tab standard: Home / Skills / Help. Human labels only (no raw skill ids on buttons).
 */
public final class UltraSkillCheckScreens {
    private UltraSkillCheckScreens() {}

    /** Donator Skill Check. */
    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!SkillCheckService.canUse(player) && !SkillCheckService.inSession(player)) {
            DmzRewards.msg(
                    player,
                    UltraStyle.noticeLine(
                            "Skill Check isn't available to you. Ask staff if you think that's wrong."));
            UltraHubScreens.open(player, "main");
            return;
        }
        show(player, page, false);
    }

    /** Staff skill-unlock browser (not the donator Skill Check). */
    public static void openSkillsAdmin(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!StaffAccess.isStaff(player)) {
            DmzRewards.msg(player, UltraStyle.noticeLine("Staff only."));
            UltraHubScreens.open(player, "main");
            return;
        }
        show(player, page, true);
    }

    private static void show(ServerPlayer player, String page, boolean staffAdmin) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        switch (p) {
            case "skills", "core", "natural", "saga", "list" -> openSkills(player, staffAdmin);
            case "help", "about" -> openHelp(player, staffAdmin);
            default -> openHome(player, staffAdmin);
        }
    }

    private static UltraScreenSpec base(boolean staffAdmin, String page, String tab) {
        String screenId = staffAdmin ? "skills" : "skillcheck";
        String title = staffAdmin ? "Skills" : "Skill Check";
        return UltraScreenSpec.of(screenId)
                .title(title)
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "skills|Skills", "help|Help");
    }

    private static void openHome(ServerPlayer player, boolean staffAdmin) {
        UltraScreenSpec spec = base(staffAdmin, "main", "home")
                .line(UltraStyle.LABEL + "Level, locked, unlocked, or max.")
                .line(
                        staffAdmin
                                ? UltraStyle.LABEL + "Staff browser — every tracked skill on one page."
                                : UltraStyle.LABEL + "Donator Skill Check — your unlock progress.")
                .setRow(110);
        spec.buttonLeft("tab:skills", UltraStyle.confirm("View skills"), "confirm")
                .buttonRight("tab:help", "Help", "normal")
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openSkills(ServerPlayer player, boolean staffAdmin) {
        List<String> lines = ProgressionGuiApi.skillsLines(player, "core");
        UltraScreenSpec spec = base(staffAdmin, "skills", "skills");
        if (skillListMissing(lines)) {
            spec.line(
                    UltraStyle.dim(
                            "No skills are listed. Ask staff to turn skill unlock on, then open this page again."));
        } else {
            for (String line : lines) {
                if (line != null && !line.isBlank()) {
                    // Strip raw id clutter if a line looks like an id-only token — keep human text.
                    spec.line(line);
                }
            }
        }
        // Long skill lists scroll in the body region; buttons stay below.
        spec.setRow(Math.max(200, UltraStyle.BODY_TOP + UltraStyle.BODY_MIN_H + UltraStyle.BTN_GAP));
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openHelp(ServerPlayer player, boolean staffAdmin) {
        UltraScreenSpec spec = base(staffAdmin, "help", "help");
        for (String line : ProgressionGuiApi.skillsLines(player, "help")) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        if (staffAdmin) {
            spec.line(UltraStyle.DIM + "Staff: /skills · Donators: /skillcheck");
        } else {
            spec.line(UltraStyle.DIM + "Ask staff if a skill looks wrong.");
        }
        spec.setRow(160);
        spec.footerBackHome();
        spec.send(player);
    }

    /** A one-line service error is not a skill list. */
    private static boolean skillListMissing(List<String> lines) {
        if (lines == null || lines.isEmpty()) {
            return true;
        }
        if (lines.size() != 1) {
            return false;
        }
        String plain = UltraStyle.plain(lines.get(0)).toLowerCase(Locale.ROOT);
        return plain.contains("error") || plain.contains("disabled");
    }
}
