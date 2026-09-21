package com.dbzlegacy.adaptivedifficulty.gui;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.command.MechanicsCommands;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff-only CustomNPCs test hub — same concept as {@code MenaceGui} (server-built
 * {@code ICustomGui}, client rendered by CustomNPCs). Opens the normal LM inventory menus
 * when a system is picked.
 */
public final class CnpcStaffTestGui {
    /** Not {@code 18420} — reserved by dmzlegacy_menaces. */
    public static final int GUI_ID = 18421;
    public static final int WIDTH = 430;
    public static final int HEIGHT = 280;

    private static final int COL_L = 15;
    private static final int COL_R = 220;
    private static final int BTN_W = 195;
    private static final int BTN_H = 20;

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
        if (!CnpcReflection.available()) {
            player.m_213846_(Component.m_237113_("§cCustomNPCs API not available."));
            return false;
        }
        Object iPlayer = CnpcReflection.wrapPlayer(player);
        if (iPlayer == null) {
            player.m_213846_(Component.m_237113_("§cCould not wrap player for CNPC GUI."));
            return false;
        }
        try {
            Object gui = CnpcReflection.createGui(GUI_ID, WIDTH, HEIGHT, false, iPlayer);
            CnpcReflection.setClosesOnEsc(gui, true);
            paint(player, gui);
            CnpcReflection.showCustomGui(iPlayer, gui);
            return true;
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] staff CNPC test GUI failed for {}: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    player.m_6302_(),
                    t.toString());
            player.m_213846_(Component.m_237113_("§cCNPC test GUI failed: " + t.getMessage()));
            return false;
        }
    }

    private static void paint(ServerPlayer player, Object gui) throws ReflectiveOperationException {
        String backend = DifficultyConfig.get().guiBackend == null ? "cmi" : DifficultyConfig.get().guiBackend;

        CnpcReflection.addLabel(gui, 1, "§6Legacy Mechanics §f— Staff CNPC Test", COL_L, 8, 400, 18);
        CnpcReflection.addLabel(
                gui,
                2,
                "§7Inventory backend §f" + backend + " §8| §7Pick a system (opens normal LM menu)",
                COL_L,
                26,
                400,
                16);

        int row = 48;
        int gap = 24;
        addSystem(gui, player, 10, "§aDifficulty", COL_L, row, () -> MechanicsCommands.openSystemMenu(player, "difficulty"));
        addSystem(gui, player, 11, "§6Rival", COL_R, row, () -> MechanicsCommands.openSystemMenu(player, "rival"));
        row += gap;
        addSystem(gui, player, 12, "§bSpar", COL_L, row, () -> MechanicsCommands.openSystemMenu(player, "spar"));
        addSystem(gui, player, 13, "§dPrestige", COL_R, row, () -> MechanicsCommands.openSystemMenu(player, "prestige"));
        row += gap;
        addSystem(gui, player, 14, "§eSkill Check", COL_L, row, () -> MechanicsCommands.openSystemMenu(player, "skillcheck"));
        addSystem(gui, player, 15, "§fCharacter Services", COL_R, row, () -> MechanicsCommands.openSystemMenu(player, "character"));
        row += gap;
        addSystem(gui, player, 16, "§aProgression §8(staff)", COL_L, row, () -> MechanicsCommands.openSystemMenu(player, "progression"));
        addSystem(gui, player, 17, "§eSkills §8(staff)", COL_R, row, () -> MechanicsCommands.openSystemMenu(player, "skills"));
        row += gap;
        addSystem(gui, player, 18, "§fLM Hub §7(chest/CMI)", COL_L, row, () -> MechanicsMenu.open(player, "main"));
        addSystem(gui, player, 19, "§7Event log", COL_R, row, () -> MechanicsMenu.open(player, "logs"));
        row += gap;
        addSystem(gui, player, 20, "§5World Menaces §8(CNPC)", COL_L, row, () -> openMenacesList(player));
        CnpcReflection.addButton(gui, 99, "§7Refresh panel", COL_R, row, BTN_W, BTN_H, () -> open(player));
        row += gap;
        CnpcReflection.addButton(gui, 98, "§cClose", COL_L, row, BTN_W, BTN_H, () -> {});
    }

    private static void addSystem(Object gui, ServerPlayer player, int id, String label, int x, int y, Runnable open)
            throws ReflectiveOperationException {
        CnpcReflection.addButton(gui, id, label, x, y, BTN_W, BTN_H, open);
    }

    private static void openMenacesList(ServerPlayer player) {
        try {
            Class<?> gui = Class.forName("com.dmzlegacy.menaces.MenaceGui");
            gui.getMethod("openList", ServerPlayer.class, int.class).invoke(null, player, 0);
        } catch (ClassNotFoundException missing) {
            player.m_213846_(Component.m_237113_("§cMenaces mod not loaded."));
        } catch (Throwable t) {
            player.m_213846_(Component.m_237113_("§cMenaces GUI failed: " + t.getMessage()));
        }
    }
}
