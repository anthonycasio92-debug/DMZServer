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
    private static final int H_MAIN = 340;

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
        if ("reskin_confirm".equals(p)) {
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, 320,
                    (pl, gui) -> paintReskinConfirm(pl, gui));
            return;
        }
        if (p.startsWith("bones:") || "bones".equals(p)) {
            int bonePage = parseBonePage(p);
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                    CnpcGuiSupport.heightForScrollPage(420),
                    (pl, gui) -> paintBones(pl, gui, bonePage));
            return;
        }
        int height = switch (p) {
            case "race", "class" -> CnpcGuiSupport.suggestHeight(280);
            case "reskin" -> 320;
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
        ServerPlayer subject = CnpcGuiSupport.target(player);
        var ph = CharacterServicesGuiApi.placeholders(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§fCharacter Services",
                "§7Race §f" + ph.getOrDefault("current_race", "?") + CnpcGuiStyle.SEP + "§7Class §f"
                        + ph.getOrDefault("current_class", "?"));

        List<String> lines = new ArrayList<>(CharacterServicesGuiApi.linesForPage(player, "main"));
        lines.add(0, "§6Coins §f" + ph.getOrDefault("ancient_coins", "0") + " §7Ancient Coins");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 3));

        boolean ok = "true".equals(ph.get("bridge_ok")) && "true".equals(ph.get("enabled"));
        if (ok) {
            List<CnpcGuiLayout.GridButton> actions = new ArrayList<>();
            if ("true".equals(ph.get("can_race_change"))) {
                actions.add(CnpcGuiLayout.GridButton.run("§eChange race", () -> open(player, "race")));
            }
            if ("true".equals(ph.get("can_class_change"))) {
                actions.add(CnpcGuiLayout.GridButton.run("§bChange class", () -> open(player, "class")));
            }
            if ("true".equals(ph.get("can_reskin"))) {
                actions.add(CnpcGuiLayout.GridButton.run("§dReskin", () -> open(player, "reskin")));
            }
            if ("true".equals(ph.get("can_head_bones"))) {
                actions.add(CnpcGuiLayout.GridButton.run("§fHead bones", () -> open(player, "bones:0")));
            }
            if (actions.isEmpty()) {
                String note = "false".equals(ph.get("can_services"))
                        ? "§7Character Services are locked for your account. Ask staff if you need access."
                        : "§7No character services are enabled on this server right now.";
                gui.addLabel(CnpcGuiSupport.ID_INLINE_NOTE, note,
                        CnpcGuiSupport.M, row + 4, CnpcGuiSupport.listWidth(), 14);
                row += CnpcGuiSupport.ROW_STEP;
            } else {
                row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                        player, gui, row + 4, 20, actions.toArray(CnpcGuiLayout.GridButton[]::new),
                        () -> open(player, "main"));
                row += 4;
            }
        } else {
            gui.addLabel(CnpcGuiSupport.ID_INLINE_NOTE, "§cCharacter Services unavailable.",
                    CnpcGuiSupport.M, row + 4, CnpcGuiSupport.listWidth(), 14);
            row += CnpcGuiSupport.ROW_STEP;
        }
        footer(player, gui, row, null, subject);
    }

    private static void paintRace(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Change race"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "race"), 2));

        List<String> cards = CharacterServicesGuiApi.raceCards(player);
        String[] labels = raceClassLabels(cards);

        int rowsBelow = 1;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        scroll.setOnClick((g, sc) -> {
            String id = selectedCardId(cards, sc);
            if (id != null) {
                open(player, "race_pct:" + id);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "main", subject);
    }

    private static void paintRacePct(ServerPlayer player, String raceAndMaybePct) {
        String[] bits = raceAndMaybePct.split(":", 2);
        String raceId = bits[0];
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, 360, (pl, gui) -> {
            ServerPlayer subject = CnpcGuiSupport.target(pl);
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, "§eKeep progress?",
                    "§7Becoming §f" + titleRace(raceId));
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                            CharacterServicesGuiApi.linesForPage(player, "race_pct:" + raceId + ":0")));
            row = CnpcGuiSupport.paintSectionTag(gui, 15, row + 4, "§8Progress kept after change");
            int[] pcts = {0, 25, 50, 75, 100};
            CnpcGuiLayout.GridButton[] grid = new CnpcGuiLayout.GridButton[pcts.length];
            for (int i = 0; i < pcts.length; i++) {
                int keep = pcts[i];
                grid[i] = CnpcGuiLayout.GridButton.run("§fKeep " + keep + "% progress",
                        () -> open(player, "race_confirm:" + raceId + ":" + keep));
            }
            row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, 40, grid, () -> {});
            row += 4;
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "race"), "§7« Back");
        });
    }

    private static void paintRaceConfirm(ServerPlayer player, String raceAndPct) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, 340, (pl, gui) -> {
            ServerPlayer subject = CnpcGuiSupport.target(pl);
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§f", "Character", "Confirm race"),
                    CnpcGuiStyle.HINT_REVIEW_PAY);
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                            CharacterServicesGuiApi.linesForPage(player, "race_confirm:" + raceAndPct)));
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
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Change class"),
                CnpcGuiStyle.HINT_CLICK_ENTRY);
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "class"), 2));

        List<String> cards = CharacterServicesGuiApi.classCards(player);
        String[] labels = raceClassLabels(cards);

        int rowsBelow = 1;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        scroll.setOnClick((g, sc) -> {
            String id = selectedCardId(cards, sc);
            if (id != null) {
                open(player, "class_confirm:" + id);
            }
        });
        footer(player, gui, CnpcGuiSupport.navRowAfterScroll(bandY, scrollH), "main", subject);
    }

    private static void paintClassConfirm(ServerPlayer player, String classId) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, 320, (pl, gui) -> {
            ServerPlayer subject = CnpcGuiSupport.target(pl);
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§f", "Character", "Confirm class"),
                    CnpcGuiStyle.HINT_REVIEW_PAY);
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                            CharacterServicesGuiApi.linesForPage(player, "class_confirm:" + classId)));
            CnpcGuiSupport.button(gui, 20, "§aConfirm & pay", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> CharacterServicesGuiApi.handleDo(player, "class_confirm", classId, "main"),
                    () -> CnpcLmHubGui.open(player, "main")));
            row += CnpcGuiSupport.ROW_STEP + 8;
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "class"), "§7« Back");
        });
    }

    private static void paintBones(ServerPlayer player, ICustomGui gui, int page) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        var ph = CharacterServicesGuiApi.placeholders(player);
        int pages = Math.max(1, CosmeticHeadBoneService.pageCount());
        int pg = Math.min(pages - 1, Math.max(0, page));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Head bones"),
                "§7Page §f" + (pg + 1) + "/" + pages + CnpcGuiStyle.SEP + "§7Active §f"
                        + ph.getOrDefault("active_head_bone", "none"));
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "bones:" + pg), 2));

        List<String> cards = CharacterServicesGuiApi.headBoneCards(player, pg);
        int rowsBelow = 3;
        String[] labels = CnpcGuiSupport.cardLabels(cards, 1);
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollBottom = CnpcGuiSupport.pickListScrollBottom(listY, rowsBelow, gui, labels.length);
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_INLINE_NOTE, "§7No head parts on this page.",
                    CnpcGuiSupport.M, bandY + 4, CnpcGuiSupport.listWidth(), 14);
            scrollBottom = bandY + 20;
        } else {
            IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
            scroll.setOnClick((g, sc) -> {
                g.close();
                String id = selectedCardId(cards, sc);
                if (id != null) {
                    CnpcGuiSupport.act(player,
                            () -> CharacterServicesGuiApi.handleDo(player, "bone_unlock", id, "bones"),
                            () -> open(player, "bones:" + pg));
                }
            });
        }
        int row = scrollBottom + 8;
        CnpcGuiSupport.buttonSmall(gui, 60, "§aEquip race default", CnpcGuiSupport.COL_L, row, CnpcGuiSupport.BTN_W,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> CharacterServicesGuiApi.handleDo(player, "bone_race_default", "", "bones"),
                        () -> open(player, "bones:" + pg)));
        CnpcGuiSupport.buttonSmall(gui, 61, "§7Unequip bone", CnpcGuiSupport.COL_R, row, CnpcGuiSupport.BTN_W,
                () -> CnpcGuiSupport.act(
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
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Reskin"),
                "§7Opens the in-game editor");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, CharacterServicesGuiApi.linesForPage(player, "reskin"),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eReview cost & continue", CnpcGuiSupport.COL_L, row,
                () -> open(player, "reskin_confirm"));
        row += CnpcGuiSupport.ROW_STEP + 4;
        footer(player, gui, row, "main", subject);
    }

    private static void paintReskinConfirm(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Confirm reskin"),
                CnpcGuiStyle.HINT_REVIEW_PAY);
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "reskin_confirm")));
        String pre = com.dbzlegacy.adaptivedifficulty.character.CharacterServicesSystem.reskinPrecheck(player);
        boolean ready = pre == null || pre.isBlank();
        CnpcGuiSupport.button(gui, 20, ready ? "§aConfirm & pay" : "§8Cannot reskin yet", CnpcGuiSupport.COL_L, row,
                () -> {
                    if (!ready) {
                        CnpcGuiSupport.feedback(player, pre);
                        open(player, "reskin_confirm");
                        return;
                    }
                    CnpcGuiSupport.actExternalUi(
                            player,
                            () -> CharacterServicesGuiApi.handleDo(player, "reskin_confirm", "", "reskin_confirm"),
                            () -> open(player, "reskin_confirm"));
                });
        row += CnpcGuiSupport.ROW_STEP + 8;
        CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "reskin"), "§7« Back");
    }

    private static void footer(
            ServerPlayer player,
            ICustomGui gui,
            int row,
            String parentPage,
            ServerPlayer previewSubject) {
        if (parentPage == null) {
            CnpcGuiSupport.navSystemRoot(player, gui, row);
        } else {
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, parentPage), "§7« Back");
        }
        if (parentPage == null) {
            CnpcGuiSupport.paintSystemMainPreview(previewSubject, gui, player);
        }
    }

    private static String[] raceClassLabels(List<String> cards) {
        return cards.stream()
                .map(c -> {
                    String[] p = c.split("\t", -1);
                    if (p.length > 2 && "1".equals(p[2])) {
                        return "§a" + p[1] + " §8(current)";
                    }
                    return p.length > 1 ? p[1] : c;
                })
                .toArray(String[]::new);
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
