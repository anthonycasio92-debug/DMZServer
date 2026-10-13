package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/**
 * Ultra / KaiPath entry. Every system opens via {@code Ultra*Screens} → {@code GuiOpenPacket}.
 * Players without KaiPath get an install message — there is no CNPC fallback.
 */
public final class UltraGui {
    private UltraGui() {}

    public static boolean preferUltra(ServerPlayer player) {
        return player != null && LmGuiNetwork.clientHasKaiPath(player);
    }

    /**
     * Open a system page on Ultra ({@code GuiOpenPacket}). If KaiPath is missing, tell the
     * player to install it and return without opening another GUI backend.
     */
    public static void open(ServerPlayer player, String system, String page) {
        if (player == null) {
            return;
        }
        String logical = UltraScreenIds.normalizeLogical(system);
        String pg = normalizePage(page);

        if (!preferUltra(player)) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra refused (KaiPath missing) player={} system={} page={}",
                    AdaptiveDifficultyMod.MOD_ID,
                    safeName(player),
                    logical,
                    pg);
            DmzRewards.msg(
                    player,
                    UltraStyle.dangerLine(
                            "Install KaiPath (DMZ Legacy client mod) to open Legacy Mechanics menus."));
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
                case "skillcheck" -> UltraSkillCheckScreens.open(player, pg);
                case "skills" -> UltraSkillCheckScreens.openSkillsAdmin(player, pg);
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
