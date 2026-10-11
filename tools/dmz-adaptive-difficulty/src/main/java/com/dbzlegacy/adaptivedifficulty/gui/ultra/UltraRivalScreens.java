package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.RivalGuiApi;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Rival system — Home / Actions / Challenge. */
public final class UltraRivalScreens {
    private UltraRivalScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String p = page == null || page.isBlank() || "main".equalsIgnoreCase(page)
                ? "home"
                : page.toLowerCase(Locale.ROOT);
        switch (p) {
            case "actions", "action", "settings", "pending", "invites", "requests" -> openActions(player, p);
            case "challenge", "challenges" -> openChallenge(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("rival")
                .title("Rivals")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "actions|Actions", "challenge|Challenge");
    }

    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = RivalGuiApi.placeholders(player);
        UltraScreenSpec spec = base("home", "home")
                .labelValue("RP", ph.getOrDefault("rp", "0"))
                .labelValue("Tier", ph.getOrDefault("tier", "—"))
                .labelValue("Wins", ph.getOrDefault("wins", "0"))
                .labelValue("Losses", ph.getOrDefault("losses", "0"))
                .labelValue("Mutual", ph.getOrDefault("mutual", "0") + "/" + ph.getOrDefault("mutual_max", "0"))
                .line(UltraStyle.LABEL + "Declare and challenge from the other tabs.")
                .setRow(140);
        spec.buttonLeft("tab:actions", UltraStyle.confirm("Actions"), "normal")
                .buttonRight("tab:challenge", "Challenge", "normal")
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openActions(ServerPlayer player, String page) {
        Map<String, String> ph = RivalGuiApi.placeholders(player);
        UltraScreenSpec spec = base(page == null ? "actions" : page, "actions")
                .line(UltraStyle.LABEL + "Send a rival request, or answer one you have.")
                .labelValue("Pending invites", ph.getOrDefault("pending_invites", "0"))
                .setRow(100);
        spec.buttonLeft("do:tpmsg:toggle", UltraStyle.confirm("TP"), "normal")
                .buttonRight("do:instinct", "Toggle instinct", "normal")
                .rowGap();
        if (RivalGuiApi.needsMutualReplacePick(player)) {
            spec.button("do:accept_replace", UltraStyle.danger("Replace mutual slot"), "danger").rowGap();
        }
        if (StaffAccess.isStaff(player)) {
            spec.button("do:admin:status", UltraStyle.dim("Staff status"), "dim").rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openChallenge(ServerPlayer player) {
        UltraScreenSpec spec = base("challenge", "challenge");
        for (String line : RivalGuiApi.challengeLines(player)) {
            spec.line(line);
        }
        for (String line : RivalGuiApi.pendingChallengeLines(player)) {
            spec.line(line);
        }
        spec.setRow(140);
        spec.buttonLeft("do:challenge:accept", UltraStyle.confirm("Accept challenge"), "confirm")
                .buttonRight("do:challenge:decline", UltraStyle.danger("Decline challenge"), "danger")
                .rowGap();
        spec.buttonLeft("do:challenge:cancel", UltraStyle.danger("Cancel challenge"), "danger")
                .buttonRight("do:spectate_stop", UltraStyle.dim("Stop spectate"), "dim")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }
}
