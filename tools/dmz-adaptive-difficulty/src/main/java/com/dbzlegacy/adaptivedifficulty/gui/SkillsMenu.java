package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player UI entrypoint for {@code /skills}.
 * Prefers CMI/CMILib inventory GUI, then Bukkit chest companion, then chat.
 */
public final class SkillsMenu {
    private SkillsMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!DifficultyConfig.get().enableSkillUnlockService) {
            return;
        }
        String target = page == null || page.isBlank() ? "core" : page;

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CMI -> openInventory(player, target);
            case CHEST -> BukkitGuiBridge.openSkills(player, target);
            case CHAT -> false;
            case AUTO -> openInventory(player, target);
        };

        if (!opened) {
            if (backend != GuiBackend.CHAT) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] skills guiBackend={} inventory open failed for {} — "
                                + "falling back to chat.",
                        AdaptiveDifficultyMod.MOD_ID,
                        backend.name().toLowerCase(),
                        player.m_6302_()
                );
            }
            com.dbzlegacy.adaptivedifficulty.progression.shop.SkillUnlockService.open(player, target);
        }
    }

    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.openSkills(player, page) || BukkitGuiBridge.openSkills(player, page);
    }
}
