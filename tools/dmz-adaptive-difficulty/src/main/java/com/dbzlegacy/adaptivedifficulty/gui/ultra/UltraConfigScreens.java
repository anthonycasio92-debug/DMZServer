package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.config.editor.ConfigEditor;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;

/**
 * Staff config list — modules, save / discard / reload.
 * Functional overview; not every detail editor page.
 */
public final class UltraConfigScreens {
    private UltraConfigScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        if (!StaffAccess.isStaff(player)) {
            UltraHubScreens.open(player, "main");
            return;
        }
        String p = page == null || page.isBlank() ? "list" : page.trim();
        if (p.toLowerCase(Locale.ROOT).startsWith("module:")) {
            openModule(player, p.substring("module:".length()).trim());
            return;
        }
        openList(player);
    }

    private static void openList(ServerPlayer player) {
        int dirty = ConfigEditor.dirtyCount(player.m_20148_());
        UltraScreenSpec spec = UltraScreenSpec.of("config")
                .title("Config Editor")
                .page("list")
                .activeTab("home")
                .line(UltraStyle.LABEL + "Edits stay in memory until Save all.")
                .labelValue("Unsaved changes", String.valueOf(dirty))
                .setRow(90);
        int placed = 0;
        for (ConfigEditor.ModuleLink link : ConfigEditor.moduleLinks()) {
            if (placed >= 10) {
                break;
            }
            String label = link.open()
                    ? link.label() + " (" + link.fields() + ")"
                    : UltraStyle.dim(link.label() + " · closed");
            String action = "do:open_module:" + link.id();
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, link.open() ? "normal" : "dim");
            } else {
                spec.buttonRight(action, label, link.open() ? "normal" : "dim");
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        spec.buttonLeft("do:save_all", UltraStyle.confirm("Save all"), "confirm")
                .buttonRight("do:discard_all", UltraStyle.danger("Discard all"), "danger")
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openModule(ServerPlayer player, String moduleId) {
        String id = moduleId == null || moduleId.isBlank() ? "difficulty" : moduleId.trim();
        ConfigEditor.EditorTab tab = ConfigEditor.tab(id);
        String title = tab == null ? id : tab.label();
        int fields = ConfigEditor.fieldCount(id);
        int dirty = ConfigEditor.dirtyCount(player.m_20148_());
        UltraScreenSpec spec = UltraScreenSpec.of("config")
                .title("Config · " + title)
                .page("module:" + id)
                .activeTab("home")
                .labelValue("Module", title)
                .labelValue("Fields", String.valueOf(fields))
                .labelValue("Unsaved (all)", String.valueOf(dirty))
                .line(UltraStyle.LABEL + "Detail field editors stay in the CNPC/chest editor.")
                .setRow(120);
        spec.buttonLeft("do:reload_module:" + id, UltraStyle.confirm("Reload module"), "confirm")
                .buttonRight("do:save_all", UltraStyle.confirm("Save all"), "confirm")
                .rowGap();
        spec.buttonLeft("do:discard_all", UltraStyle.danger("Discard all"), "danger")
                .buttonRight("nav:home", UltraStyle.dim("Module list"), "dim")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    /**
     * Config do-actions. Returns a status message for chat/toast.
     * {@code open_module} is handled by reopening via page in the caller after this returns —
     * we reopen ourselves by returning empty and letting dispatch open {@code page}, so
     * {@link UltraGuiActions} should pass page {@code module:&lt;id&gt;} when using open_module.
     */
    public static String handleAction(ServerPlayer player, String action, String arg, String page) {
        if (player == null) {
            return UltraStyle.dangerLine("This only works in-game.");
        }
        if (!StaffAccess.isStaff(player)) {
            return UltraStyle.dangerLine("Staff only.");
        }
        String act = action == null ? "" : action.toLowerCase(Locale.ROOT).trim();
        String a = arg == null ? "" : arg.trim();
        return switch (act) {
            case "save_all", "save" -> {
                ConfigEditor.SaveResult result =
                        ConfigEditor.saveAll(player.m_20148_(), player.m_7755_().getString());
                if (result.error() != null && !result.error().isBlank()) {
                    yield UltraStyle.dangerLine(result.error());
                }
                yield result.message() == null || result.message().isBlank()
                        ? UltraStyle.noticeLine("Saved.")
                        : UltraStyle.noticeLine(result.message());
            }
            case "discard_all", "discard" -> {
                int count = ConfigEditor.discardAll(player.m_20148_());
                yield count <= 0
                        ? UltraStyle.noticeLine("Nothing to discard.")
                        : UltraStyle.noticeLine("Discarded " + count + " change(s).");
            }
            case "reload_module", "reload" -> {
                String module = !a.isBlank() ? a : moduleFromPage(page);
                String error = ConfigEditor.reloadModule(player.m_20148_(), module);
                yield error == null
                        ? UltraStyle.noticeLine("Reloaded " + module + ".")
                        : UltraStyle.dangerLine(error);
            }
            case "open_module" -> {
                // UltraGuiActions reopens to module:<arg> after this returns.
                yield "";
            }
            case "page", "refresh" -> "";
            default -> UltraStyle.dangerLine("That config action is not available.");
        };
    }

    private static String moduleFromPage(String page) {
        if (page == null || page.isBlank()) {
            return "difficulty";
        }
        String p = page.trim();
        if (p.regionMatches(true, 0, "module:", 0, 7)) {
            String id = p.substring(7).trim();
            return id.isBlank() ? "difficulty" : id;
        }
        if (p.regionMatches(true, 0, "list:", 0, 5)) {
            String[] parts = p.split(":", 3);
            if (parts.length > 1 && !parts[1].isBlank()) {
                return parts[1].trim();
            }
        }
        return "difficulty";
    }
}
