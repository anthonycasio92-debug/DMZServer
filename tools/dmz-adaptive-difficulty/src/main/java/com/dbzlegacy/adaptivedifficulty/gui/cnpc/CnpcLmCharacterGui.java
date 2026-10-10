package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneService;
import com.dbzlegacy.adaptivedifficulty.character.HeadPartPieces;
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
            String rest = p.substring("race_pct:".length());
            paintRaceConfirm(player, rest.contains(":") ? rest : rest + ":0");
            return;
        }
        if (p.startsWith("class_confirm:")) {
            paintClassConfirm(player, p.substring("class_confirm:".length()));
            return;
        }
        if ("reskin_confirm".equals(p)) {
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                    CnpcGuiSupport.window(320),
                    (pl, gui) -> paintReskinConfirm(pl, gui));
            return;
        }
        if (p.startsWith("saga_confirm:")) {
            CnpcLmSagaGui.openConfirm(player, p.substring("saga_confirm:".length()));
            return;
        }
        if ("saga".equals(p) || p.startsWith("saga:")) {
            CnpcLmSagaGui.openList(player, p);
            return;
        }
        if (p.startsWith("bones:") || "bones".equals(p)) {
            int bonePage = parseBonePage(p);
            int boneExtra = CnpcGuiSupport.TAB_BAR_H;
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                    CnpcGuiSupport.heightForScrollPage(420 + boneExtra),
                    (pl, gui) -> paintBones(pl, gui, bonePage));
            return;
        }
        int tab = CnpcGuiSupport.TAB_BAR_H;
        int height = switch (p) {
            case "race", "class" -> CnpcGuiSupport.suggestHeight(280);
            case "reskin" -> CnpcGuiSupport.window(320 + tab);
            default -> CnpcGuiSupport.window(H_MAIN + ("main".equals(p) ? tab : 0));
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
        infoY = characterTabs(player, gui, infoY, "services");

        List<String> lines = new ArrayList<>(CharacterServicesGuiApi.linesForPage(player, "main"));
        String coinColor = CnpcUltraStyle.ACCENT;
        lines.add(0, coinColor + "Ancient Coins §f" + ph.getOrDefault("ancient_coins", "0"));
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));

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
                actions.add(CnpcGuiLayout.GridButton.run("§fHead parts", () -> open(player, "bones:0")));
            }
            if (com.dbzlegacy.adaptivedifficulty.quest.SagaResetConfig.get().enabled) {
                actions.add(CnpcGuiLayout.GridButton.run("§6Saga reset", () -> open(player, "saga")));
            }
            actions.add(CnpcGuiLayout.GridButton.run("§cRemove Android Upgrade",
                    () -> CnpcLmGui.open(player, "android_remove", "main")));
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
            CnpcGuiSupport.button(gui, 40, "§cRemove Android Upgrade", CnpcGuiSupport.COL_L, row,
                    () -> CnpcLmGui.open(player, "android_remove", "main"));
            row += CnpcGuiSupport.ROW_STEP;
            if (com.dbzlegacy.adaptivedifficulty.quest.SagaResetConfig.get().enabled) {
                CnpcGuiSupport.button(gui, 41, "§6Saga reset", CnpcGuiSupport.COL_L, row,
                        () -> open(player, "saga"));
                row += CnpcGuiSupport.ROW_STEP;
            }
        }
        footer(player, gui, row, null, subject);
    }

    private static void paintRace(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Change race"),
                "§7Select a race, then choose it");
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "race"), 2));

        List<String> cards = CharacterServicesGuiApi.raceCards(player);
        String[] labels = raceClassLabels(cards);

        int rowsBelow = 2;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, "§eChoose this race", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> selectedCardId(cards, scroll),
                id -> open(player, "race_confirm:" + id + ":0"),
                () -> open(player, "race"));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, "main", subject);
    }

    private static void paintRaceConfirm(ServerPlayer player, String raceAndPct) {
        String[] bits = raceAndPct.split(":", 2);
        String raceId = bits[0];
        int selected = 0;
        if (bits.length > 1) {
            try {
                selected = Integer.parseInt(bits[1].trim());
            } catch (NumberFormatException ignored) {
                selected = 0;
            }
        }
        int keepSelected = selected;
        String confirmArg = raceId + ":" + keepSelected;
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                CnpcGuiSupport.window(420), (pl, gui) -> {
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage("§f", "Character", "Confirm race"),
                    "§7Becoming §f" + titleRace(raceId) + " §8· §7keep §f" + keepSelected + "%");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                            CharacterServicesGuiApi.linesForPage(player, "race_confirm:" + confirmArg)));
            int[] pcts = {0, 25, 50, 75, 100};
            CnpcGuiLayout.GridButton[] grid = new CnpcGuiLayout.GridButton[pcts.length];
            for (int i = 0; i < pcts.length; i++) {
                int keep = pcts[i];
                String label = (keep == keepSelected ? "§6" : "§7") + "Keep " + keep + "%";
                grid[i] = CnpcGuiLayout.GridButton.run(label,
                        () -> open(player, "race_confirm:" + raceId + ":" + keep));
            }
            row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, CnpcGuiSupport.ID_GRID_BASE, grid, () -> {});
            CnpcGuiSupport.button(gui, 20, "§aConfirm & pay", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> CharacterServicesGuiApi.handleDo(player, "race_confirm", confirmArg, "main"),
                    () -> CnpcLmHubGui.open(player, "main")));
            row += CnpcGuiSupport.ROW_STEP + 8;
            CnpcGuiSupport.navSubmenu(player, gui, row, () -> open(player, "race"), "§7« Back");
        });
    }

    private static void paintClass(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Change class"),
                "§7Select a class, then choose it");
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "class"), 2));

        List<String> cards = CharacterServicesGuiApi.classCards(player);
        String[] labels = raceClassLabels(cards);

        int rowsBelow = 2;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, "§eChoose this class", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> selectedCardId(cards, scroll),
                id -> open(player, "class_confirm:" + id),
                () -> open(player, "class"));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, "main", subject);
    }

    private static void paintClassConfirm(ServerPlayer player, String classId) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                CnpcGuiSupport.window(320), (pl, gui) -> {
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
        int pages = Math.max(1, CosmeticHeadBoneService.pageCount());
        int pg = Math.min(pages - 1, Math.max(0, page));
        String worn = CosmeticHeadBoneService.wornLabel(CosmeticHeadBoneService.activeBone(player));
        int wornCount = HeadPartPieces.fragments(CosmeticHeadBoneService.activeBone(player)).size();
        if (wornCount > 2) {
            worn = wornCount + " parts on";
        }
        String subtitle = "§7" + HeadPartPieces.pageTitle(pg) + CnpcGuiStyle.SEP + "§7On §f" + worn;
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Head parts"),
                subtitle);
        infoY = characterTabs(player, gui, infoY, "bones");
        int y = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "bones:" + pg), 2));
        y = CnpcRowList.paintRow(gui, 184, y, HeadPartPieces.pageTitle(pg), "On " + worn, null);

        List<String> cards = CharacterServicesGuiApi.headBoneCards(player, pg);
        int rowStep = CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER, "§7No head parts in this group.",
                    CnpcGuiSupport.M, y, CnpcGuiSupport.listWidth(), 14);
            y += rowStep;
        }
        for (int i = 0; i < cards.size(); i++) {
            String[] parts = cards.get(i).split("\t", -1);
            String id = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 ? parts[1] : id;
            String state = parts.length > 2 ? parts[2] : "";
            String cost = parts.length > 3 ? parts[3] : "";
            String caption = ultraPartCaption(state, name, cost);
            CnpcGuiSupport.buttonSmallFull(gui, CnpcGuiSupport.ID_GRID_BASE + i, caption,
                    CnpcGuiSupport.M, y, CnpcGuiSupport.textBandWidth(),
                    () -> CnpcGuiSupport.act(player,
                            () -> CharacterServicesGuiApi.handleDo(player, "bone_toggle", id, "bones"),
                            () -> open(player, "bones:" + pg)));
            y += rowStep;
        }

        String raceDefault = CnpcUltraStyle.CONFIRM + "Equip race default";
        String natural = CnpcUltraStyle.SUBTITLE + "Reset to natural look";
        CnpcGuiSupport.buttonSmall(gui, 60, raceDefault, CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> CharacterServicesGuiApi.handleDo(player, "bone_race_default", "", "bones"),
                        () -> open(player, "bones:" + pg)));
        CnpcGuiSupport.buttonSmall(gui, 61, natural, CnpcGuiSupport.COL_R, y, CnpcGuiSupport.BTN_W,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> CharacterServicesGuiApi.handleDo(player, "bone_unequip", "", "bones"),
                        () -> open(player, "bones:" + pg)));
        y += CnpcGuiSupport.ROW_STEP;
        if (pg > 0) {
            CnpcGuiSupport.buttonSmall(gui, 62, CnpcUltraStyle.SUBTITLE + "« Head",
                    CnpcGuiSupport.COL_L, y, 95, () -> open(player, "bones:" + (pg - 1)));
        }
        if (pg + 1 < pages) {
            CnpcGuiSupport.buttonSmall(gui, 63, CnpcUltraStyle.SUBTITLE + "Horns »",
                    CnpcGuiSupport.COL_R, y, 95, () -> open(player, "bones:" + (pg + 1)));
        }
        y += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navSubmenu(player, gui, y, () -> {
            CosmeticHeadBoneService.restorePreview(player);
            open(player, "main");
        }, "§7« Back");
        CnpcGuiSupport.paintLivePlayerPreview(player, gui);
    }

    /** On/off caption. Keeps {@code §lON } / {@code §lOFF } so the label is not cut. */
    private static String ultraPartCaption(String state, String name, String cost) {
        if ("E".equals(state)) {
            return CnpcUltraStyle.CONFIRM + "§lON §r" + CnpcUltraStyle.BODY + name;
        }
        if ("L".equals(state)) {
            return CnpcUltraStyle.DIM + "§lOFF §r" + CnpcUltraStyle.SUBTITLE + name
                    + " " + CnpcUltraStyle.ACCENT + cost;
        }
        return CnpcUltraStyle.DIM + "§lOFF §r" + CnpcUltraStyle.SUBTITLE + name;
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
                "§7Opens the appearance editor");
        infoY = characterTabs(player, gui, infoY, "reskin");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, CharacterServicesGuiApi.linesForPage(player, "reskin"),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eReview cost & continue", CnpcGuiSupport.COL_L, row,
                () -> open(player, "reskin_confirm"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "main", subject);
    }

    private static void paintReskinConfirm(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§f", "Character", "Confirm Reskin"),
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

    static int characterTabs(ServerPlayer player, ICustomGui gui, int y, String active) {
        return CnpcGuiSupport.paintTabBar(gui, y, new String[] {
                "services|Services", "reskin|Reskin", "bones|Headbones", "saga|Saga"
        }, active, action -> {
            String id = action.startsWith("tab:") ? action.substring(4) : action;
            switch (id) {
                case "reskin" -> open(player, "reskin");
                case "bones" -> open(player, "bones:0");
                case "saga" -> open(player, "saga");
                default -> open(player, "main");
            }
        });
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
