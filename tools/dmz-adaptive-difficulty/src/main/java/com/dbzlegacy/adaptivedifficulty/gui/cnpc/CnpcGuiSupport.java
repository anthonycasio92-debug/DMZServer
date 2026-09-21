package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.gui.AdminInspectSessions;
import java.util.List;
import java.util.function.Supplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.entity.IEntity;
import noppes.npcs.api.entity.IPlayer;
import noppes.npcs.api.gui.IButton;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.ILabel;
import noppes.npcs.api.gui.IScroll;

/** Shared CustomNPCs layout + show helpers for Legacy Mechanics UI. */
public final class CnpcGuiSupport {
    public static final int W = 430;
    public static final int H = 280;
    public static final int M = 14;
    public static final int BTN_W = 195;
    public static final int BTN_H = 20;
    public static final int COL_L = M;
    public static final int COL_R = 220;

    private CnpcGuiSupport() {}

    public static boolean cnpcReady(ServerPlayer player) {
        if (player == null) {
            return false;
        }
        if (!NpcAPI.IsAvailable()) {
            player.m_213846_(Component.m_237113_("§cCustomNPCs is required for the LM menu (install on client + server)."));
            return false;
        }
        return true;
    }

    public static IPlayer<?> wrap(ServerPlayer player) {
        try {
            IEntity entity = NpcAPI.Instance().getIEntity(player);
            if (entity instanceof IPlayer<?> ip) {
                return ip;
            }
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] CNPC wrap failed: {}", AdaptiveDifficultyMod.MOD_ID, t);
        }
        return null;
    }

    public static void show(ServerPlayer player, int guiId, Painter painter) {
        IPlayer<?> ip = wrap(player);
        if (ip == null) {
            player.m_213846_(Component.m_237113_("§cCould not open LM menu (CNPC player wrap failed)."));
            return;
        }
        try {
            ICustomGui gui = NpcAPI.Instance().createCustomGui(guiId, W, H, false, ip);
            gui.setClosesOnEsc(true);
            painter.paint(player, gui);
            ip.showCustomGui(gui);
        } catch (Throwable t) {
            AdaptiveDifficultyMod.LOGGER.warn("[{}] CNPC GUI {} failed: {}", AdaptiveDifficultyMod.MOD_ID, guiId, t);
            player.m_213846_(Component.m_237113_("§cMenu error: " + t.getMessage()));
        }
    }

    public static ILabel title(ICustomGui gui, int id, String text) {
        ILabel l = gui.addLabel(id, text, M, 8, W - M * 2, 18);
        try {
            l.setScale(1.15f);
        } catch (Throwable ignored) {
        }
        return l;
    }

    public static void subtitle(ICustomGui gui, int id, String text) {
        gui.addLabel(id, text, M, 28, W - M * 2, 14);
    }

    /** Data/actions target (inspect subject when staff is inspecting). */
    public static net.minecraft.server.level.ServerPlayer target(net.minecraft.server.level.ServerPlayer viewer) {
        return AdminInspectSessions.resolveSubject(viewer);
    }

    public static void inspectBanner(net.minecraft.server.level.ServerPlayer viewer, ICustomGui gui) {
        String line = AdminInspectSessions.inspectBannerLine(viewer);
        if (line != null) {
            gui.addLabel(4, line, M, 40, W - M * 2, 12);
        }
    }

    public static void divider(ICustomGui gui, int id, int y) {
        gui.addLabel(id, "§8────────────────────────────────────────", M, y, W - M * 2, 10);
    }

    public static void bodyLines(ICustomGui gui, int startId, int y, List<String> lines, int maxLines) {
        int row = y;
        int n = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            if (n >= maxLines) {
                break;
            }
            gui.addLabel(startId + n, line, M, row, W - M * 2, 12);
            row += 13;
            n++;
        }
    }

    public static IScroll scroll(ICustomGui gui, int id, int x, int y, int w, int h, String[] items) {
        return gui.addScroll(id, x, y, w, h, items == null ? new String[0] : items);
    }

    public static IScroll scrollSearchable(ICustomGui gui, int id, int x, int y, int w, int h, String[] items) {
        IScroll scroll = scroll(gui, id, x, y, w, h, items);
        try {
            scroll.setHasSearch(true);
        } catch (Throwable ignored) {
        }
        return scroll;
    }

    public static IButton button(ICustomGui gui, int id, String label, int x, int y, Runnable onPress) {
        IButton b = gui.addButton(id, label, x, y, BTN_W, BTN_H);
        b.setOnPress((g, btn) -> {
            g.close();
            onPress.run();
        });
        return b;
    }

    public static IButton buttonSmall(ICustomGui gui, int id, String label, int x, int y, int w, Runnable onPress) {
        IButton b = gui.addButton(id, label, x, y, w, BTN_H);
        b.setOnPress((g, btn) -> {
            g.close();
            onPress.run();
        });
        return b;
    }

    public static void feedback(ServerPlayer player, String msg) {
        if (player == null || msg == null || msg.isBlank()) {
            return;
        }
        for (String line : msg.split("\n")) {
            if (line != null && !line.isBlank()) {
                player.m_213846_(Component.m_237113_(line));
            }
        }
    }

    public static void act(ServerPlayer player, Supplier<String> action, Runnable reopen) {
        feedback(player, action.get());
        reopen.run();
    }

    @FunctionalInterface
    public interface Painter {
        void paint(ServerPlayer player, ICustomGui gui);
    }
}
