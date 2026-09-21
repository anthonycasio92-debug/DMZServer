package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

public final class CnpcLmCharacterGui {
    private CnpcLmCharacterGui() {}

    public static void open(ServerPlayer player, String page) {
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        if (p.startsWith("race_confirm:")) {
            paintRaceConfirm(player, p.substring("race_confirm:".length()));
            return;
        }
        if (p.startsWith("race_pct:")) {
            paintRacePct(player, p.substring("race_pct:".length()));
            return;
        }
        if (p.startsWith("class_confirm:")) {
            paintClassConfirm(player, p.substring("class_confirm:".length()));
            return;
        }
        CnpcGuiSupport.show(player, CnpcLmGui.ID_CHARACTER, (pl, gui) -> {
            switch (p) {
                case "race" -> paintRace(pl, gui);
                case "class" -> paintClass(pl, gui);
                case "reskin" -> paintReskin(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        var ph = CharacterServicesGuiApi.placeholders(player);
        CnpcGuiSupport.title(gui, 1, "§fCharacter Services");
        CnpcGuiSupport.subtitle(gui, 2, "§7Race §f" + ph.getOrDefault("current_race", "?")
                + "  §8·  §7Class §f" + ph.getOrDefault("current_class", "?"));
        CnpcGuiSupport.divider(gui, 3, 38);

        List<String> lines = CharacterServicesGuiApi.linesForPage(player, "main");
        lines.add(0, "§6Coins §f" + ph.getOrDefault("ancient_coins", "0") + " §7AC copper-value");
        CnpcGuiSupport.bodyLines(gui, 10, 44, lines, 5);

        boolean ok = "true".equals(ph.get("bridge_ok")) && "true".equals(ph.get("enabled"));
        int row = 108;
        if (ok) {
            if ("true".equals(ph.get("can_race_change"))) {
                CnpcGuiSupport.button(gui, 20, "§eChange race", CnpcGuiSupport.COL_L, row,
                        () -> open(player, "race"));
            }
            if ("true".equals(ph.get("can_class_change"))) {
                CnpcGuiSupport.button(gui, 21, "§bChange class", CnpcGuiSupport.COL_R, row,
                        () -> open(player, "class"));
            }
            row += 24;
            if ("true".equals(ph.get("can_reskin"))) {
                CnpcGuiSupport.button(gui, 22, "§dReskin", CnpcGuiSupport.COL_L, row,
                        () -> open(player, "reskin"));
            }
        } else {
            gui.addLabel(4, "§cCharacter Services unavailable.", CnpcGuiSupport.M, row, 400, 14);
            row += 20;
        }
        footer(player, gui, row);
    }

    private static void paintRace(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§eChange race");
        CnpcGuiSupport.subtitle(gui, 2, "§7Double-click a race to continue");
        CnpcGuiSupport.bodyLines(gui, 10, 44, CharacterServicesGuiApi.linesForPage(player, "race"), 4);

        List<String> cards = CharacterServicesGuiApi.raceCards(player);
        String[] labels = cards.stream()
                .map(c -> {
                    String[] p = c.split("\t", -1);
                    if (p.length > 2 && "1".equals(p[2])) {
                        return "§a" + p[1] + " §8(current)";
                    }
                    return p.length > 1 ? p[1] : c;
                })
                .toArray(String[]::new);

        IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, 100, CnpcGuiSupport.M, 92, 400, 100, labels);
        scroll.setOnDoubleClick((g, sc) -> {
            g.close();
            String id = selectedCardId(cards, sc);
            if (id != null) {
                open(player, "race_pct:" + id);
            }
        });
        footer(player, gui, 200);
    }

    private static void paintRacePct(ServerPlayer player, String raceAndMaybePct) {
        String[] bits = raceAndMaybePct.split(":", 2);
        String raceId = bits[0];
        CnpcGuiSupport.show(player, CnpcLmGui.ID_CHARACTER, (pl, gui) -> {
            CnpcGuiSupport.title(gui, 1, "§eKeep progress?");
            CnpcGuiSupport.subtitle(gui, 2, "§7Becoming §f" + titleRace(raceId));
            CnpcGuiSupport.bodyLines(gui, 10, 44,
                    CharacterServicesGuiApi.linesForPage(player, "race_pct:" + raceId + ":0"), 6);
            int row = 130;
            for (int pct : new int[] {0, 25, 50, 75, 100}) {
                int col = (pct == 0 || pct == 50 || pct == 100) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
                if (pct == 25 || pct == 75) {
                    row += 24;
                }
                int keep = pct;
                CnpcGuiSupport.buttonSmall(gui, 40 + pct, "§f" + pct + "%", col, row, 95,
                        () -> open(player, "race_confirm:" + raceId + ":" + keep));
            }
            row += 36;
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Back", CnpcGuiSupport.COL_L, row, 95,
                    () -> open(player, "race"));
            CnpcGuiSupport.buttonSmall(gui, 97, "§7Main", CnpcGuiSupport.COL_R, row, 95,
                    () -> open(player, "main"));
        });
    }

    private static void paintRaceConfirm(ServerPlayer player, String raceAndPct) {
        CnpcGuiSupport.show(player, CnpcLmGui.ID_CHARACTER, (pl, gui) -> {
            CnpcGuiSupport.title(gui, 1, "§cConfirm race change");
            CnpcGuiSupport.bodyLines(gui, 10, 40,
                    CharacterServicesGuiApi.linesForPage(player, "race_confirm:" + raceAndPct), 10);
            CnpcGuiSupport.button(gui, 20, "§aConfirm & pay", CnpcGuiSupport.COL_L, 200, () -> CnpcGuiSupport.act(
                    player,
                    () -> CharacterServicesGuiApi.handleDo(player, "race_confirm", raceAndPct, "main"),
                    () -> CnpcLmHubGui.open(player, "main")));
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Back", CnpcGuiSupport.COL_L, 230, 95,
                    () -> {
                        String race = raceAndPct.split(":", 2)[0];
                        open(player, "race_pct:" + race);
                    });
        });
    }

    private static void paintClass(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§bChange class");
        CnpcGuiSupport.subtitle(gui, 2, "§7Double-click a class to continue");
        CnpcGuiSupport.bodyLines(gui, 10, 44, CharacterServicesGuiApi.linesForPage(player, "class"), 4);

        List<String> cards = CharacterServicesGuiApi.classCards(player);
        String[] labels = cards.stream()
                .map(c -> {
                    String[] p = c.split("\t", -1);
                    if (p.length > 2 && "1".equals(p[2])) {
                        return "§a" + p[1] + " §8(current)";
                    }
                    return p.length > 1 ? p[1] : c;
                })
                .toArray(String[]::new);

        IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, 100, CnpcGuiSupport.M, 92, 400, 100, labels);
        scroll.setOnDoubleClick((g, sc) -> {
            g.close();
            String id = selectedCardId(cards, sc);
            if (id != null) {
                open(player, "class_confirm:" + id);
            }
        });
        footer(player, gui, 200);
    }

    private static void paintClassConfirm(ServerPlayer player, String classId) {
        CnpcGuiSupport.show(player, CnpcLmGui.ID_CHARACTER, (pl, gui) -> {
            CnpcGuiSupport.title(gui, 1, "§cConfirm class change");
            CnpcGuiSupport.bodyLines(gui, 10, 40,
                    CharacterServicesGuiApi.linesForPage(player, "class_confirm:" + classId), 8);
            CnpcGuiSupport.button(gui, 20, "§aConfirm & pay", CnpcGuiSupport.COL_L, 180, () -> CnpcGuiSupport.act(
                    player,
                    () -> CharacterServicesGuiApi.handleDo(player, "class_confirm", classId, "main"),
                    () -> CnpcLmHubGui.open(player, "main")));
            CnpcGuiSupport.buttonSmall(gui, 96, "§7« Back", CnpcGuiSupport.COL_L, 210, 95,
                    () -> open(player, "class"));
        });
    }

    private static void paintReskin(ServerPlayer player, ICustomGui gui) {
        CnpcGuiSupport.title(gui, 1, "§dReskin");
        CnpcGuiSupport.bodyLines(gui, 10, 44, CharacterServicesGuiApi.linesForPage(player, "reskin"), 8);
        CnpcGuiSupport.button(gui, 20, "§aOpen reskin editor", CnpcGuiSupport.COL_L, 150, () -> CnpcGuiSupport.act(
                player,
                () -> CharacterServicesGuiApi.handleDo(player, "reskin_confirm", "", "reskin"),
                () -> open(player, "main")));
        footer(player, gui, 190);
    }

    private static void footer(ServerPlayer player, ICustomGui gui, int row) {
        CnpcGuiSupport.buttonSmall(gui, 96, "§7« Hub", CnpcGuiSupport.COL_L, row, 95,
                () -> CnpcLmHubGui.open(player, "main"));
        CnpcGuiSupport.buttonSmall(gui, 98, "§cClose", CnpcGuiSupport.COL_R, row, 95, () -> {});
    }

    private static String selectedCardId(List<String> cards, IScroll scroll) {
        if (cards == null || cards.isEmpty() || scroll == null) {
            return null;
        }
        int[] sel = scroll.getSelection();
        if (sel == null || sel.length == 0 || sel[0] < 0 || sel[0] >= cards.size()) {
            return null;
        }
        String card = cards.get(sel[0]);
        String[] p = card.split("\t", -1);
        return p.length > 0 ? p[0] : null;
    }

    private static String titleRace(String id) {
        if (id == null || id.isBlank()) {
            return "?";
        }
        String s = id.replace('_', ' ');
        return s.substring(0, 1).toUpperCase(Locale.ROOT) + s.substring(1);
    }
}
