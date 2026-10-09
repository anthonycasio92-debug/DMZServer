package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Forge-only staff inspect — CNPC panels with {@link AdminInspectSessions}. */
public final class ForgeInspectGui {
    private ForgeInspectGui() {}

    public static boolean open(ServerPlayer admin, ServerPlayer subject, String system) {
        if (admin == null || subject == null) {
            return false;
        }
        com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcUltraPreview.leave(admin);
        AdminInspectSessions.set(admin.m_20148_(), subject.m_20148_());
        admin.m_213846_(Component.m_237113_(
                "§eInspecting §f" + subject.m_7755_().getString()
                        + "§e. Edits in this menu apply to them. §8/lm admin inspect clear §7to stop."));
        String sys = system == null || system.isBlank() ? "hub" : system.toLowerCase(Locale.ROOT);
        switch (sys) {
            case "hub", "lm", "main" -> CnpcLmGui.openHub(admin, "main");
            case "difficulty", "diff" -> CnpcLmGui.open(admin, "difficulty", "main");
            case "rival" -> CnpcLmGui.open(admin, "rival", "main");
            case "spar", "sparring" -> CnpcLmGui.open(admin, "spar", "main");
            case "prestige" -> CnpcLmGui.open(admin, "prestige", "main");
            case "character", "char" -> CnpcLmGui.open(admin, "character", "main");
            case "skillcheck" -> CnpcLmGui.open(admin, "skillcheck", "main");
            case "progression", "prog" -> CnpcLmGui.open(admin, "progression", "main");
            case "skills" -> CnpcLmGui.open(admin, "skills", "core");
            case "logs", "syslog" -> CnpcLmGui.open(admin, "logs", "main");
            default -> {
                admin.m_213846_(Component.m_237113_("§cUnknown inspect system: §f" + sys));
                return false;
            }
        }
        return true;
    }

    public static boolean clear(ServerPlayer admin) {
        if (admin == null) {
            return false;
        }
        AdminInspectSessions.clear(admin.m_20148_());
        admin.m_213846_(Component.m_237113_("§7Inspect cleared."));
        MechanicsMenu.open(admin, "main");
        return true;
    }
}
