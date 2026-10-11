package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.GuiBackend;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ultra / KaiPath entry. When the client has KaiPath and {@code guiBackend=ultra}, every
 * system opens via {@code Ultra*Screens} → {@code GuiOpenPacket}. CNPC is used only when
 * Ultra cannot run (missing KaiPath / non-ultra backend) — never as a submenu fallback.
 */
public final class UltraGui {
    private UltraGui() {}

    public static boolean preferUltra(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        return GuiBackend.fromConfig() == GuiBackend.ULTRA && LmGuiNetwork.clientHasKaiPath(player);
    }

    /**
     * Open a system page on Ultra ({@code GuiOpenPacket}), or CNPC only when Ultra is
     * unavailable for this player.
     */
    public static void open(ServerPlayer player, String system, String page) {
        if (player == null) {
            return;
        }
        String logical = UltraScreenIds.normalizeLogical(system);
        String pg = normalizePage(page);

        if (!preferUltra(player)) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra → CNPC fallback (KaiPath missing or guiBackend≠ultra) "
                            + "player={} system={} page={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeName(player),
                    logical,
                    pg);
            CnpcLmGui.open(player, system, page);
            return;
        }

        openUltra(player, logical, pg);
    }

    /** Ultra primary path: always an {@code Ultra*Screens} builder → {@code GuiOpenPacket}. */
    static void openUltra(ServerPlayer player, String logical, String page) {
        if (player == null) {
            return;
        }
        String id = logical == null || logical.isBlank() ? "hub" : logical;
        String pg = normalizePage(page);
        AdaptiveDifficultyMod.LOGGER.debug(
                "[{}] Ultra open screenId={} page={} player={}",
                AdaptiveDifficultyMod.MOD_ID,
                id,
                pg,
                safeName(player));
        try {
            switch (id) {
                case "hub" -> UltraHubScreens.open(player, pg);
                case "admin" -> UltraHubScreens.open(player, "admin");
                case "difficulty" -> UltraDifficultyScreens.open(player, pg);
                case "rival" -> UltraRivalScreens.open(player, pg);
                case "spar" -> UltraSparScreens.open(player, pg);
                case "prestige" -> UltraPrestigeScreens.open(player, pg);
                case "character" -> UltraCharacterScreens.open(player, pg);
                case "progression" -> UltraProgressionScreens.open(player, pg);
                case "config" -> UltraConfigScreens.open(player, pg);
                default -> {
                    AdaptiveDifficultyMod.LOGGER.warn(
                            "[{}] Ultra unknown logical screenId=\"{}\" page=\"{}\" — opening hub",
                            AdaptiveDifficultyMod.MOD_ID,
                            id,
                            pg);
                    UltraHubScreens.open(player, "main");
                }
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] Ultra openUltra failed screenId={} page={} player={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    id,
                    pg,
                    safeName(player),
                    t);
        }
    }

    public static void openHub(ServerPlayer player, String page) {
        open(player, "hub", page);
    }

    private static String normalizePage(String page) {
        if (page == null || page.isBlank()) {
            return "main";
        }
        // Keep colon pages (e.g. saga_confirm:id, module:foo) case-sensitive trim.
        if (page.indexOf(':') >= 0) {
            return page.trim();
        }
        return page.toLowerCase(Locale.ROOT);
    }

    private static String safeName(ServerPlayer player) {
        if (player == null) {
            return "?";
        }
        try {
            return player.m_7755_().getString();
        } catch (Throwable ignored) {
            return "?";
        }
    }
}
