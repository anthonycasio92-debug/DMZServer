package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.SparGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Sparring system — Home / Dojo / Wars. */
public final class UltraSparScreens {
    private UltraSparScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        switch (p) {
            case "dojo", "roster", "mentor", "actions", "pending", "invites", "settings" -> openDojo(player, p);
            case "wars", "dojo_war", "dojo_war_pending" -> openWars(player, p);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("spar")
                .title("Sparring")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "dojo|Dojo", "wars|Wars");
    }

    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = SparGuiApi.placeholders(player);
        boolean active = "true".equalsIgnoreCase(ph.get("session_active"));
        String partner = ph.getOrDefault("partner", "");
        UltraScreenSpec spec = base("main", "home")
                .labelValue("Session", active ? "in progress" : "not running")
                .labelValue("Partner", partner == null || partner.isBlank() ? "none" : partner)
                .labelValue("Dojo", blank(ph.get("dojo_name")))
                .labelValue("Dojo RP", ph.getOrDefault("dojo_rp", "0"))
                .line(UltraStyle.LABEL + "Rankings show who has trained the most.")
                .setRow(130);
        spec.buttonLeft("tab:dojo", UltraStyle.confirm("Dojo"), "normal")
                .buttonRight("tab:wars", "Wars", "normal")
                .rowGap();
        spec.buttonLeft("do:tpmsg:toggle", UltraStyle.confirm("TP"), "normal")
                .buttonRight("do:end", UltraStyle.danger("End session"), "danger")
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openDojo(ServerPlayer player, String page) {
        Map<String, String> ph = SparGuiApi.placeholders(player);
        UltraScreenSpec spec = base(page == null ? "dojo" : page, "dojo");
        for (String line : SparGuiApi.dojoInfoLines(player)) {
            spec.line(line);
        }
        spec.labelValue("Mentor", blank(ph.get("mentor")))
                .labelValue("Streak", ph.getOrDefault("streak", "0")
                        + " best " + ph.getOrDefault("streak_best", "0"))
                .setRow(140);
        spec.buttonLeft("do:mentor_tpmsg:toggle", UltraStyle.confirm("Mentor TP"), "normal")
                .buttonRight("do:mentor:leave", UltraStyle.danger("Leave mentor"), "danger")
                .rowGap();
        if (StaffAccess.isStaff(player)) {
            spec.button("do:admin:status", UltraStyle.dim("Staff status"), "dim").rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openWars(ServerPlayer player, String page) {
        UltraScreenSpec spec = base(page == null || page.isBlank() ? "dojo_war" : page, "wars");
        for (String line : SparGuiApi.dojoWarLines(player)) {
            spec.line(line);
        }
        // Keep leaderboard category wording exact.
        spec.line(UltraStyle.labelValue("Board category", "War wins"));
        Map<String, String> ph = SparGuiApi.placeholders(player);
        spec.labelValue("War pending", ph.getOrDefault("dojo_war_pending", "0"))
                .setRow(140);
        spec.buttonLeft("do:dojo_accept", UltraStyle.confirm("Accept war"), "confirm")
                .buttonRight("do:dojo_war_cancel", UltraStyle.danger("Cancel war"), "danger")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static String blank(String s) {
        return s == null || s.isBlank() ? "—" : s;
    }
}
