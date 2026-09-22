package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneService;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

public final class CnpcLmCharacterGui {
    private static final int H_MAIN = 320;
    private static final int H_LIST = 340;

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
        if (p.startsWith("bones:") || "bones".equals(p)) {
            int bonePage = parseBonePage(p);
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, 360,
                    (pl, gui) -> paintBones(pl, gui, bonePage));
            return;
        }
        int height = switch (p) {
            case "race", "class" -> H_LIST;
            case "reskin" -> 300;
            default -> H_MAIN;
        };
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, height, (pl, gui) -> {
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
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§fCharacter Services",
                "§7Race §f" + ph.getOrDefault("current_race", "?") + CnpcGuiStyle.SEP + "§7Class §f"
                        + ph.getOrDefault("current_class", "?"));

        List<String> lines = new ArrayList<>(CharacterServicesGuiApi.linesForPage(player, "main"));
        lines.add(0, "§6Coins §f" + ph.getOrDefault("ancient_coins", "0") + " §7Ancient Coins");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 3);

        boolean ok = "true".equals(ph.get("bridge_ok")) && "true".equals(ph.get("enabled"));
        if (ok) {
            if ("true".equals(ph.get("can_race_change"))) {
                CnpcGuiSupport.button(gui, 20, "§eChange race", CnpcGuiSupport.COL_L, row,
                        () -> open(player, "race"));
            }
            if ("true".equals(ph.get("can_class_change"))) {
                CnpcGuiSupport.button(gui, 21, "§bChange class", CnpcGuiSupport.COL_R, row,
                        () -> open(player, "class"));
            }
            row += CnpcGuiSupport.ROW_STEP;
            if ("true".equals(ph.get("can_reskin"))) {
                CnpcGuiSupport.button(gui, 22, "§dReskin", CnpcGuiSupport.COL_L, row,
                        () -> open(player, "reskin"));
            }
            if ("true".equals(ph.get("can_head_bones"))) {
                CnpcGuiSupport.button(gui, 23, "§fHead bone shop", CnpcGuiSupport.COL_R, row,
                        () -> open(player, "bones:0"));
            }
        } else {
            gui.addLabel(50, "§cCharacter Services unavailable.", CnpcGuiSupport.M, row, 400, 14);
            row += 20;
        }
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, null);
    }

    private static void paintRace(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Change race"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        int listY = CnpcGuiSupport.paintInfoBlock(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "race"), 2);

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

        int scrollH = CnpcGuiSupport.listScrollHeight(gui, listY, 1);
        IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, listY,
                CnpcGuiSupport.W - CnpcGuiSupport.M * 2, scrollH, labels);
        scroll.setOnClick((g, sc) -> {
            String id = selectedCardId(cards, sc);
            if (id != null) {
                open(player, "race_pct:" + id);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(listY, scrollH), "main");
    }

    private static void paintRacePct(ServerPlayer player, String raceAndMaybePct) {
        String[] bits = raceAndMaybePct.split(":", 2);
        String raceId = bits[0];
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, 320, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§eKeep progress?",
                    "§7Becoming §f" + titleRace(raceId));
            int row = CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                    CharacterServicesGuiApi.linesForPage(player, "race_pct:" + raceId + ":0"));
            row += 8;
            for (int pct : new int[] {0, 25, 50, 75, 100}) {
                int col = (pct == 0 || pct == 50 || pct == 100) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
                if (pct == 25 || pct == 75) {
                    row += CnpcGuiSupport.ROW_STEP;
                }
                int keep = pct;
                CnpcGuiSupport.buttonSmall(gui, 40 + pct, "§f" + pct + "%", col, row, 95,
                        () -> open(player, "race_confirm:" + raceId + ":" + keep));
            }
            row += CnpcGuiSupport.ROW_STEP + 12;
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "race"), "§7« Back");
        });
    }

    private static void paintRaceConfirm(ServerPlayer player, String raceAndPct) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, 320, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§f", "Character", "Confirm race"),
                    CnpcGuiStyle.HINT_REVIEW_PAY);
            int row = CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                    CharacterServicesGuiApi.linesForPage(player, "race_confirm:" + raceAndPct));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§aConfirm & pay", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> CharacterServicesGuiApi.handleDo(player, "race_confirm", raceAndPct, "main"),
                    () -> CnpcLmHubGui.open(player, "main")));
            row += CnpcGuiSupport.ROW_STEP + 8;
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> {
                String race = raceAndPct.split(":", 2)[0];
                open(player, "race_pct:" + race);
            }, "§7« Back");
        });
    }

    private static void paintClass(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Change class"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        int listY = CnpcGuiSupport.paintInfoBlock(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "class"), 2);

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

        int scrollH = CnpcGuiSupport.listScrollHeight(gui, listY, 1);
        IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, listY,
                CnpcGuiSupport.W - CnpcGuiSupport.M * 2, scrollH, labels);
        scroll.setOnClick((g, sc) -> {
            String id = selectedCardId(cards, sc);
            if (id != null) {
                open(player, "class_confirm:" + id);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(listY, scrollH), "main");
    }

    private static void paintClassConfirm(ServerPlayer player, String classId) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, 300, (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§f", "Character", "Confirm class"),
                    CnpcGuiStyle.HINT_REVIEW_PAY);
            int row = CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                    CharacterServicesGuiApi.linesForPage(player, "class_confirm:" + classId));
            row += 8;
            CnpcGuiSupport.button(gui, 20, "§aConfirm & pay", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> CharacterServicesGuiApi.handleDo(player, "class_confirm", classId, "main"),
                    () -> CnpcLmHubGui.open(player, "main")));
            row += CnpcGuiSupport.ROW_STEP + 8;
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "class"), "§7« Back");
        });
    }

    private static void paintBones(ServerPlayer player, ICustomGui gui, int page) {
        var ph = CharacterServicesGuiApi.placeholders(player);
        int pages = Math.max(1, CosmeticHeadBoneService.pageCount());
        int pg = Math.min(pages - 1, Math.max(0, page));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Head bone shop"),
                "§7Page §f" + (pg + 1) + "/" + pages + CnpcGuiStyle.SEP + "§7Active §f"
                        + ph.getOrDefault("active_head_bone", "none"));
        int listY = CnpcGuiSupport.paintInfoBlock(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "bones:" + pg), 2);

        List<String> cards = CharacterServicesGuiApi.headBoneCards(player, pg);
        int scrollBottom = listY;
        if (cards.isEmpty()) {
            gui.addLabel(50, "§7No head parts on this page.", CnpcGuiSupport.M, listY + 4, 400, 14);
            scrollBottom = listY + 20;
        } else {
            int scrollH = CnpcGuiSupport.listScrollHeight(gui, listY, 3);
            IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M,
                    listY, CnpcGuiSupport.W - CnpcGuiSupport.M * 2, scrollH,
                    CnpcGuiSupport.cardLabels(cards, 1));
            scroll.setOnDoubleClick((g, sc) -> {
                g.close();
                String id = selectedCardId(cards, sc);
                if (id != null) {
                    CnpcGuiSupport.act(player,
                            () -> CharacterServicesGuiApi.handleDo(player, "bone_unlock", id, "bones"),
                            () -> open(player, "bones:" + pg));
                }
            });
            scrollBottom = listY + scrollH;
        }
        int row = scrollBottom + 8;
        CnpcGuiSupport.buttonSmall(gui, 60, "§aEquip race default", CnpcGuiSupport.COL_L, row, 195, () -> CnpcGuiSupport.act(
                player,
                () -> CharacterServicesGuiApi.handleDo(player, "bone_race_default", "", "bones"),
                () -> open(player, "bones:" + pg)));
        CnpcGuiSupport.buttonSmall(gui, 61, "§7Unequip bone", CnpcGuiSupport.COL_R, row, 195, () -> CnpcGuiSupport.act(
                player,
                () -> CharacterServicesGuiApi.handleDo(player, "bone_unequip", "", "bones"),
                () -> open(player, "bones:" + pg)));
        row += CnpcGuiSupport.ROW_STEP;
        if (pg > 0) {
            CnpcGuiSupport.buttonSmall(gui, 62, "§7« Prev page", CnpcGuiSupport.COL_L, row, 95,
                    () -> open(player, "bones:" + (pg - 1)));
        }
        if (pg + 1 < pages) {
            CnpcGuiSupport.buttonSmall(gui, 63, "§7Next page »", CnpcGuiSupport.COL_R, row, 95,
                    () -> open(player, "bones:" + (pg + 1)));
        }
        row += CnpcGuiSupport.ROW_STEP + 4;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "main"), "§7« Back");
    }

    private static int parseBonePage(String page) {
        if (page == null || page.isBlank() || "bones".equals(page)) {
            return 0;
        }
        if (page.startsWith("bones:")) {
            try {
                return Integer.parseInt(page.substring("bones:".length()).trim());
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }
        return 0;
    }

    private static void paintReskin(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Reskin"),
                "§7Opens the in-game editor");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, CharacterServicesGuiApi.linesForPage(player, "reskin"),
                CnpcGuiStyle.INFO_INLINE_MAX);
        CnpcGuiSupport.button(gui, 20, "§aOpen reskin editor", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> CharacterServicesGuiApi.handleDo(player, "reskin_confirm", "", "reskin"),
                () -> open(player, "main")));
        row += CnpcGuiSupport.ROW_STEP + 4;
        footer(player, gui, row, "main");
    }

    /** {@code parentPage} null on character main; otherwise Back reopens that page. Main always → LM hub. */
    private static void footer(ServerPlayer player, ICustomGui gui, int row, String parentPage) {
        if (parentPage == null) {
            CnpcGuiSupport.navSystemRoot(player, gui, row);
        } else {
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, parentPage), "§7« Back");
        }
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
