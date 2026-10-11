package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcStyledTestHub;
import com.dbzlegacy.adaptivedifficulty.gui.ultra.UltraGui;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Staff shortcut for {@code /lm admin testgui}. {@code /lm} uses the same look. */
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
            player.m_213846_(Component.m_237113_("§eStaff test GUI is disabled in config."));
            return false;
        }
        player.m_213846_(Component.m_237113_("§6Test menu."));
        if (GuiBackend.fromConfig() == GuiBackend.ULTRA) {
            UltraGui.openHub(player, "main");
            return true;
        }
        CnpcStyledTestHub.open(player);
        return true;
    }
}
