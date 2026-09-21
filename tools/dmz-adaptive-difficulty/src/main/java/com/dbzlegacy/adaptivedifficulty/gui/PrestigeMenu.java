package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player UI entrypoint for {@code /prestige}.
 * Prefers CMI/CMILib inventory GUI, then Bukkit chest companion, then chat.
 */
public final class PrestigeMenu {
    private PrestigeMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!DifficultyConfig.get().enablePrestigeSystem) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CNPC -> {
                com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmGui.open(player, "prestige", target);
                yield true;
            }
            case CMI -> openInventory(player, target);
            case CHEST -> BukkitGuiBridge.openPrestige(player, target);
            case CHAT -> false;
            case AUTO -> openInventory(player, target);
        };

        if (!opened) {
            if (backend != GuiBackend.CHAT) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] prestige guiBackend={} inventory open failed for {} — "
                                + "falling back to chat.",
                        AdaptiveDifficultyMod.MOD_ID,
                        backend.name().toLowerCase(),
                        player.m_6302_()
                );
            }
            com.dbzlegacy.adaptivedifficulty.progression.shop.PrestigeSystem.open(player);
        }
    }

    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.openPrestige(player, page) || BukkitGuiBridge.openPrestige(player, page);
    }
}
