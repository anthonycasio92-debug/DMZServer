package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DmzProgression;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

/**
 * Player UI entrypoint for {@code /difficulty}.
 * Prefers CMI/CMILib inventory GUI, then Bukkit chest companion, then chat.
 */
public final class DifficultyMenu {
    private DifficultyMenu() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        // Re-read DMZ level on every open — do not wait for death/respawn.
        int sampled = DmzProgression.sampleLevelOnGuiOpen(player);
        if (!DmzProgression.isTransformed(player)) {
            PlayerDifficultyData data = DifficultyCache.data(player);
            data.noteDmzLevel(sampled);
        }
        DifficultyCache.refresh(player);
        String target = page == null || page.isBlank() ? "main" : page;
        if ("settings".equalsIgnoreCase(target)) {
            if (!StaffAccess.isStaff(player)) {
                target = "main";
            } else {
                DifficultyChatMenu.open(player, "settings");
                return;
            }
        }

        GuiBackend backend = GuiBackend.fromConfig();
        boolean opened = switch (backend) {
            case CMI -> openInventory(player, target);
            case CHEST -> BukkitGuiBridge.open(player, target);
            case CHAT -> false;
            case AUTO -> openInventory(player, target);
        };

        if (!opened) {
            if (backend != GuiBackend.CHAT) {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] guiBackend={} inventory open failed for {} — "
                                + "falling back to chat. Try /dmzdiffgui if this persists.",
                        AdaptiveDifficultyMod.MOD_ID,
                        backend.name().toLowerCase(),
                        player.m_6302_()
                );
            }
            DifficultyChatMenu.open(player, target);
        }
    }

    /** CMI first, then plain chest — never skip the companion plugin for a CMILib check. */
    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.open(player, page) || BukkitGuiBridge.open(player, page);
    }
}
