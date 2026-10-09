package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmHubGui;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcUltraPreview;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Staff audit of the new CNPC colors ({@code /lm admin testgui}). {@code /lm} stays on the current menus. */
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
        CnpcUltraPreview.enter(player);
        player.m_213846_(Component.m_237113_(
                "§6Test menu. §7This is the new look. §f/lm §7still opens the current menus."));
        CnpcLmHubGui.open(player, "main");
        return true;
    }
}
