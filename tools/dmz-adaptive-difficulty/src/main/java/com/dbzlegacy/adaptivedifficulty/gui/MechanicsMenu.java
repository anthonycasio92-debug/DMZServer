package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player UI entrypoint for {@code /lm} / {@code /legacymechanics}.
 * Prefers CMI/CMILib inventory GUI, then Bukkit chest companion, then chat.
 */
public final class MechanicsMenu {
    private MechanicsMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CMI -> openInventory(player, target);
            case CHEST -> BukkitGuiBridge.openHub(player, target);
            case CHAT -> false;
            case AUTO -> openInventory(player, target);
        };

        if (!opened) {
            if (backend != GuiBackend.CHAT) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] hub guiBackend={} inventory open failed for {} — "
                                + "falling back to chat. Try /dmzdiffgui if this persists.",
                        AdaptiveDifficultyMod.MOD_ID,
                        backend.name().toLowerCase(),
                        player.m_6302_()
                );
            }
            MechanicsChatMenu.open(player, target);
        }
    }

    /** CMI first, then plain chest — never skip the companion plugin for a CMILib check. */
    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.openHub(player, page) || BukkitGuiBridge.openHub(player, page);
    }
}
