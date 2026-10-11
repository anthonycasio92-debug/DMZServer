package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.gui.GuiBackend;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ultra / KaiPath entry. Falls back to CustomNPCs when the client lacks KaiPath
 * or when {@code guiBackend} is not ultra.
 */
public final class UltraGui {
    private UltraGui() {}

    public static boolean preferUltra(ServerPlayer player) {
        return GuiBackend.fromConfig() == GuiBackend.ULTRA && LmGuiNetwork.clientHasKaiPath(player);
    }

    /** Open a system page on Ultra, or CNPC when KaiPath is missing. */
    public static void open(ServerPlayer player, String system, String page) {
        if (player == null) {
            return;
        }
        if (!preferUltra(player)) {
            CnpcLmGui.open(player, system, page);
            return;
        }
        String sys = system == null || system.isBlank() ? "hub" : system.toLowerCase();
        String pg = page == null || page.isBlank() ? "main"
                : (page.indexOf(':') >= 0 ? page.trim() : page.toLowerCase(java.util.Locale.ROOT));
        switch (sys) {
            case "hub", "lm", "legacymechanics", "main" -> UltraHubScreens.open(player, pg);
            case "difficulty", "diff" -> UltraDifficultyScreens.open(player, pg);
            case "rival" -> UltraRivalScreens.open(player, pg);
            case "spar", "sparring" -> UltraSparScreens.open(player, pg);
            case "prestige" -> UltraPrestigeScreens.open(player, pg);
            case "character", "charservices", "char" -> UltraCharacterScreens.open(player, pg);
            case "saga", "sagareset", "saga_reset" -> UltraCharacterScreens.open(player, "saga".equals(pg) ? "reset" : pg);
            case "progression", "prog" -> UltraProgressionScreens.open(player, pg);
            case "config", "cfgedit", "configeditor" -> UltraConfigScreens.open(player, pg);
            case "admin" -> UltraHubScreens.open(player, "admin");
            case "android_remove", "androidremove" ->
                    UltraCharacterScreens.open(player, UltraStyle.PAGE_ANDROID_REMOVE);
            case "skills", "skill", "skillcheck", "skill_check" -> UltraHubScreens.open(player, "main");
            case "logs", "syslog" -> UltraHubScreens.open(player, "main");
            default -> UltraHubScreens.open(player, "main");
        }
    }

    public static void openHub(ServerPlayer player, String page) {
        open(player, "hub", page);
    }
}
