package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmHubGui;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Staff entry for CNPC hub ({@code /lm admin testgui}) — same UI as {@code /lm} when {@code guiBackend=cnpc}. */
public final class CnpcStaffTestGui {
    private CnpcStaffTestGui() {}

    public static boolean open(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!StaffAccess.isStaff(player)) {
            player.m_213846_(Component.m_237113_("§cStaff only."));
            return false;
        }
        if (!DifficultyConfig.get().enableStaffCnpcTestGui) {
            player.m_213846_(Component.m_237113_("§eStaff CNPC test GUI is disabled in config."));
            return false;
        }
        CnpcLmHubGui.open(player, "main");
        return true;
    }
}
