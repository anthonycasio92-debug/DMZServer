package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import net.minecraft.server.level.ServerPlayer;

/** Routes all CNPC-only Legacy Mechanics screens (no chest/CMI). */
public final class CnpcLmGui {
    public static final int ID_HUB = 18430;
    public static final int ID_DIFFICULTY = 18431;
    public static final int ID_RIVAL = 18432;
    public static final int ID_SPAR = 18433;
    public static final int ID_PRESTIGE = 18434;
    public static final int ID_CHARACTER = 18435;
    public static final int ID_SKILLCHECK = 18436;
    public static final int ID_PROGRESSION = 18437;
    public static final int ID_LOGS = 18438;
    public static final int ID_ADMIN = 18439;

    private CnpcLmGui() {}

    public static void open(ServerPlayer player, String system, String page) {
        if (player == null || !CnpcGuiSupport.cnpcReady(player)) {
            return;
        }
        String sys = system == null || system.isBlank() ? "hub" : system.toLowerCase();
        String pg = page == null || page.isBlank() ? "main" : page.toLowerCase();
        switch (sys) {
            case "hub", "lm", "legacymechanics", "main" -> CnpcLmHubGui.open(player, pg);
            case "difficulty", "diff" -> CnpcLmDifficultyGui.open(player, pg);
            case "rival" -> CnpcLmRivalGui.open(player, pg);
            case "spar", "sparring" -> CnpcLmSparGui.open(player, pg);
            case "prestige" -> CnpcLmPrestigeGui.open(player, pg);
            case "character", "charservices", "char" -> CnpcLmCharacterGui.open(player, pg);
            case "skillcheck", "skill_check" -> CnpcLmSkillCheckGui.open(player, pg);
            case "progression", "prog" -> CnpcLmProgressionGui.open(player, pg);
            case "logs", "syslog" -> CnpcLmLogsGui.open(player, pg);
            case "admin" -> CnpcLmAdminGui.open(player, pg);
            case "skills", "skill" -> CnpcLmSkillCheckGui.openSkillsAdmin(player, pg);
            case "android_remove", "androidremove" -> CnpcLmProgressionGui.open(player, "android_remove");
            default -> CnpcLmHubGui.open(player, "main");
        }
    }

    public static void openHub(ServerPlayer player, String page) {
        open(player, "hub", page);
    }
}
