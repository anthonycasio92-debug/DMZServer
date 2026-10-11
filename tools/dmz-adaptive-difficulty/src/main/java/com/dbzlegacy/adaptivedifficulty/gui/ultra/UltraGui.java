package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.GuiBackend;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ultra / KaiPath entry. Falls back to CustomNPCs when the client lacks KaiPath,
 * when {@code guiBackend} is not ultra, or when KaiPath has no handler for the screen id
 * (live KaiPath 1.3.0 {@code LmScreens} only registers {@code hub}).
 */
public final class UltraGui {
    private UltraGui() {}

    public static boolean preferUltra(ServerPlayer player) {
        return GuiBackend.fromConfig() == GuiBackend.ULTRA && LmGuiNetwork.clientHasKaiPath(player);
    }

    /** Open a system page on Ultra, or CNPC when KaiPath cannot render that screenId. */
    public static void open(ServerPlayer player, String system, String page) {
        if (player == null) {
            return;
        }
        String logical = UltraScreenIds.normalizeLogical(system);
        String pg = page == null || page.isBlank() ? "main"
                : (page.indexOf(':') >= 0 ? page.trim() : page.toLowerCase(java.util.Locale.ROOT));

        if (!preferUltra(player)) {
            CnpcLmGui.open(player, system, page);
            return;
        }

        // KaiPath MissingScreen ("Unknown screen") if wire id is not registered.
        if (!UltraScreenIds.kaipathCanOpen(logical)) {
            AdaptiveDifficultyMod.LOGGER.debug(
                    "[{}] Ultra → CNPC fallback: KaiPath has no screenId={} (logical open)",
                    AdaptiveDifficultyMod.MOD_ID,
                    logical);
            CnpcLmGui.open(player, system, page);
            return;
        }

        switch (logical) {
            case "hub" -> UltraHubScreens.open(player, pg);
            // Future: when KaiPath registers more ids, add cases here matching PROTOCOL_IDS.
            default -> {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Ultra kaipathCanOpen({}) but no Ultra builder routed — CNPC fallback",
                        AdaptiveDifficultyMod.MOD_ID,
                        logical);
                CnpcLmGui.open(player, system, page);
            }
        }
    }

    public static void openHub(ServerPlayer player, String page) {
        open(player, "hub", page);
    }
}
