package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.DmzRewards;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff UI entrypoint for {@code /skills} (SkillUnlock admin browser).
 * Donators use {@link #openSkillCheck} via {@code /skillcheck} / CNPC.
 */
public final class SkillsMenu {
    private SkillsMenu() {}

    /** Staff-only SkillUnlock browser. */
    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!StaffAccess.isStaff(player)) {
            DmzRewards.msg(player, "§cStaff only.");
            return;
        }
        com.dbzlegacy.adaptivedifficulty.progression.shop.SkillCheckService.clearSession(player);
        openInternal(player, page, false);
    }

    /** Donator Skill Check path — permission already verified by SkillCheckService. */
    public static void openSkillCheck(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        openInternal(player, page, true);
    }

    private static void openInternal(ServerPlayer player, String page, boolean skillCheck) {
        if (!DifficultyConfig.get().enableSkillUnlockService) {
            return;
        }
        if (skillCheck && !DifficultyConfig.get().enableSkillCheck) {
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
            com.dbzlegacy.adaptivedifficulty.progression.shop.SkillUnlockService.open(
                    player, target, skillCheck);
        }
    }

    private static boolean openInventory(ServerPlayer player, String page) {
        return CmiGuiBridge.openSkills(player, page) || BukkitGuiBridge.openSkills(player, page);
    }
}
