package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.gui.cnpc.CnpcLmHubGui;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff entry for {@code /lm admin testgui}.
 * Prefer native Ultra when {@code guiBackend=ultra} / {@code useNativeTestGui}.
 */
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
        if (DifficultyConfig.get().useNativeTestGui
                || GuiBackend.fromConfig() == GuiBackend.ULTRA) {
            if (com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork.sendOpen(player, "hub")) {
                return true;
            }
            player.m_213846_(Component.m_237113_(
                    "§eUltra client jar missing. Opening chat hub."));
            MechanicsChatMenu.open(player, "main");
            return true;
        }
        if (GuiBackend.fromConfig() == GuiBackend.CNPC) {
            CnpcLmHubGui.open(player, "main");
            return true;
        }
        MechanicsChatMenu.open(player, "main");
        return true;
    }
}
