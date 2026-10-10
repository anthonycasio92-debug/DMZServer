package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneService;
import com.dbzlegacy.adaptivedifficulty.character.HeadPartPieces;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import java.util.List;
import java.util.Locale;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

public final class CnpcLmCharacterGui {
    private static final int H_MAIN = 340;

    private CnpcLmCharacterGui() {}

    public static void open(ServerPlayer player, String page) {
        CnpcUltraStyle.withAccent(CnpcUltraStyle.ACCENT_CHARACTER, () -> openCharacter(player, page));
    }

    private static void openCharacter(ServerPlayer player, String page) {
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
            String resetPage = "saga".equals(p) ? "reset" : "reset:" + p.substring("saga:".length());
            CnpcLmSagaGui.open(player, resetPage);
            return;
        }
        if ("android_remove".equals(p) || "android_convert".equals(p)) {
            int androidH = CnpcGuiSupport.window(360 + CnpcGuiSupport.TAB_BAR_H);
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, androidH, (pl, gui) -> {
                if ("android_convert".equals(p)) {
                    paintAndroidConvert(pl, gui);
                } else {
                    paintAndroidRemove(pl, gui);
                }
            });
            return;
        }
        if ("main".equals(p) || "services".equals(p) || "home".equals(p)) {
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                    CnpcGuiSupport.window(H_MAIN + CnpcGuiSupport.TAB_BAR_H),
                    (pl, gui) -> paintHome(pl, gui));
            return;
        }
        String pageKey = p;
        if (pageKey.startsWith("bones:") || "bones".equals(pageKey)) {
            int bonePage = parseBonePage(pageKey);
            int boneExtra = CnpcGuiSupport.TAB_BAR_H;
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                    CnpcGuiSupport.heightForScrollPage(420 + boneExtra),
                    (pl, gui) -> paintBones(pl, gui, bonePage));
            return;
        }
        int tab = CnpcGuiSupport.TAB_BAR_H;
        int height = switch (pageKey) {
            case "race", "class" -> CnpcGuiSupport.suggestHeight(280);
            case "reskin" -> CnpcGuiSupport.window(320 + tab);
            default -> CnpcGuiSupport.window(H_MAIN + tab);
        };
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W, height, (pl, gui) -> {
            switch (pageKey) {
                case "race" -> paintRace(pl, gui);
                case "class" -> paintClass(pl, gui);
                case "raceclass", "race_class" -> paintHome(pl, gui);
                case "reskin" -> paintReskin(pl, gui);
                default -> paintBones(pl, gui, 0);
            }
        });
    }

    private static void paintRace(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Change race"),
                CnpcUltraStyle.SUBTITLE + "Select a race, then choose it");
        infoY = servicesTabs(player, gui, infoY, "home");
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "race"), 2));

        List<String> cards = CharacterServicesGuiApi.raceCards(player);
        String[] labels = raceClassLabels(cards);

        int rowsBelow = 2;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose this race", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> selectedCardId(cards, scroll),
                id -> open(player, "race_confirm:" + id + ":0"),
                () -> open(player, "race"));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, "raceclass", subject);
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
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Confirm race"),
                    CnpcUltraStyle.SUBTITLE + "Becoming " + CnpcUltraStyle.BODY + titleRace(raceId) + " " + CnpcUltraStyle.DIM + "· " + CnpcUltraStyle.SUBTITLE + "keep " + CnpcUltraStyle.BODY + keepSelected + "%");
            infoY = servicesTabs(player, gui, infoY, "home");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                            CharacterServicesGuiApi.linesForPage(player, "race_confirm:" + confirmArg)));
            int[] pcts = {0, 25, 50, 75, 100};
            CnpcGuiLayout.GridButton[] grid = new CnpcGuiLayout.GridButton[pcts.length];
            for (int i = 0; i < pcts.length; i++) {
                int keep = pcts[i];
                String label = (keep == keepSelected ? CnpcUltraStyle.ACCENT : CnpcUltraStyle.SUBTITLE) + "Keep " + keep + "%";
                grid[i] = CnpcGuiLayout.GridButton.run(label,
                        () -> open(player, "race_confirm:" + raceId + ":" + keep));
            }
            row = CnpcGuiLayout.paintTwoColumnButtonGrid(player, gui, row, CnpcGuiSupport.ID_GRID_BASE, grid, () -> {});
            CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Confirm & pay", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> CharacterServicesGuiApi.handleDo(player, "race_confirm", confirmArg, "raceclass"),
                    () -> open(player, "raceclass")));
            row += CnpcGuiSupport.ROW_STEP + 8;
            CnpcGuiSupport.navBackToParent(player, gui, row, () -> open(player, "main"));
        });
    }

    private static void paintClass(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Change class"),
                CnpcUltraStyle.SUBTITLE + "Select a class, then choose it");
        infoY = servicesTabs(player, gui, infoY, "home");
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "class"), 2));

        List<String> cards = CharacterServicesGuiApi.classCards(player);
        String[] labels = raceClassLabels(cards);

        int rowsBelow = 2;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.length);
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels);
        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        CnpcGuiSupport.selectionButton(player, gui, 30, CnpcUltraStyle.INFO + "Choose this class", CnpcGuiSupport.COL_L, row,
                CnpcGuiSupport.BTN_W, () -> selectedCardId(cards, scroll),
                id -> open(player, "class_confirm:" + id),
                () -> open(player, "class"));
        footer(player, gui, row + CnpcGuiSupport.ROW_STEP, "raceclass", subject);
    }

    private static void paintClassConfirm(ServerPlayer player, String classId) {
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_CHARACTER, CnpcGuiSupport.W,
                CnpcGuiSupport.window(320), (pl, gui) -> {
            ServerPlayer subject = CnpcGuiSupport.target(pl);
            int infoY = CnpcGuiSupport.paintHeader(pl, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Confirm class"),
                    CnpcGuiStyle.HINT_REVIEW_PAY);
            infoY = servicesTabs(player, gui, infoY, "home");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                            CharacterServicesGuiApi.linesForPage(player, "class_confirm:" + classId)));
            CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.CONFIRM + "Confirm & pay", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> CharacterServicesGuiApi.handleDo(player, "class_confirm", classId, "raceclass"),
                    () -> open(player, "raceclass")));
            row += CnpcGuiSupport.ROW_STEP + 8;
            CnpcGuiSupport.navBackToParent(player, gui, row, () -> open(player, "main"));
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
        String subtitle = CnpcUltraStyle.SUBTITLE + HeadPartPieces.pageTitle(pg) + CnpcGuiStyle.SEP + CnpcUltraStyle.SUBTITLE + "On " + CnpcUltraStyle.BODY + worn;
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Model customization"),
                subtitle);
        infoY = servicesTabs(player, gui, infoY, "home");
        int y = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "bones:" + pg), 2));
        y = CnpcRowList.paintRow(gui, 184, y, HeadPartPieces.pageTitle(pg), "On " + worn, null);

        List<String> cards = CharacterServicesGuiApi.headBoneCards(player, pg);
        int rowStep = CnpcGuiSupport.BTN_H + CnpcRowList.ROW_GAP;
        if (cards.isEmpty()) {
            gui.addLabel(CnpcGuiSupport.ID_EMPTY_PLACEHOLDER, CnpcUltraStyle.SUBTITLE + "No model pieces in this group. Try the other page.",
                    CnpcGuiSupport.M, y, CnpcGuiSupport.listWidth(), 14);
            y += rowStep;
        }
        for (int i = 0; i < cards.size(); i++) {
            String[] parts = cards.get(i).split("\t", -1);
            String id = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 ? parts[1] : id;
            String state = parts.length > 2 ? parts[2] : "";
            String cost = parts.length > 3 ? parts[3] : "";
            String caption = headPartCaption(state, name, cost);
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
        CnpcGuiSupport.navBackToParent(player, gui, y, () -> open(player, "main"));
        CnpcGuiSupport.paintLivePlayerPreview(player, gui);
    }

    /** On or off, plus the unlock cost when the part is still locked. */
    private static String headPartCaption(String state, String name, String cost) {
        if ("E".equals(state)) {
            return CnpcGuiStyle.toggleOn(name);
        }
        if ("L".equals(state)) {
            return CnpcGuiStyle.toggleOff(name) + " " + CnpcUltraStyle.ACCENT + cost;
        }
        return CnpcGuiStyle.toggleOff(name);
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
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Reskin"),
                CnpcUltraStyle.SUBTITLE + "Opens the appearance editor");
        infoY = servicesTabs(player, gui, infoY, "home");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, CharacterServicesGuiApi.linesForPage(player, "reskin"),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, CnpcUltraStyle.INFO + "Review cost & continue", CnpcGuiSupport.COL_L, row,
                () -> open(player, "reskin_confirm"));
        row += CnpcGuiSupport.ROW_STEP;
        footer(player, gui, row, "main", subject);
    }

    private static void paintReskinConfirm(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Confirm Reskin"),
                CnpcGuiStyle.HINT_REVIEW_PAY);
        infoY = servicesTabs(player, gui, infoY, "home");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintReadOnlyScroll(gui, infoY,
                CharacterServicesGuiApi.linesForPage(player, "reskin_confirm")));
        String pre = com.dbzlegacy.adaptivedifficulty.character.CharacterServicesSystem.reskinPrecheck(player);
        boolean ready = pre == null || pre.isBlank();
        CnpcGuiSupport.button(gui, 20, ready ? CnpcUltraStyle.CONFIRM + "Confirm & pay" : CnpcUltraStyle.DIM + "Cannot reskin yet", CnpcGuiSupport.COL_L, row,
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
        CnpcGuiSupport.navBackToParent(player, gui, row, () -> open(player, "main"));
    }

    static int servicesTabs(ServerPlayer player, ICustomGui gui, int y, String active) {
        return CnpcGuiSupport.paintTabBar(gui, y, new String[] {
                "home|Home", "saga|Saga Reset", "android|Android Removal"
        }, active, action -> {
            String id = action.startsWith("tab:") ? action.substring(4) : action;
            switch (id) {
                case "saga" -> CnpcLmSagaGui.open(player, "reset");
                case "android" -> open(player, "android_remove");
                default -> open(player, "main");
            }
        });
    }

    /** Race, class, look, and anything that does not have its own tab. */
    private static void paintHome(ServerPlayer player, ICustomGui gui) {
        var ph = CharacterServicesGuiApi.placeholders(player);
        boolean race = "true".equals(ph.get("can_race_change"));
        boolean klass = "true".equals(ph.get("can_class_change"));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcUltraStyle.header("Character Services"),
                CnpcUltraStyle.SUBTITLE + "Change your race, class, or look");
        infoY = servicesTabs(player, gui, infoY, "home");
        int y = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (race) {
            CnpcGuiSupport.buttonSmall(gui, 64, CnpcUltraStyle.INFO + "Change race",
                    CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W, () -> open(player, "race"));
        }
        if (klass) {
            CnpcGuiSupport.buttonSmall(gui, 65, CnpcUltraStyle.ACCENT + "Change class",
                    race ? CnpcGuiSupport.COL_R : CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W,
                    () -> open(player, "class"));
        }
        if (race || klass) {
            y += CnpcGuiSupport.ROW_STEP;
        }
        CnpcGuiSupport.buttonSmall(gui, 68, CnpcUltraStyle.ACCENT + "Model customization",
                CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W, () -> open(player, "bones:0"));
        CnpcGuiSupport.buttonSmall(gui, 69, CnpcUltraStyle.INFO + "Reskin",
                CnpcGuiSupport.COL_R, y, CnpcGuiSupport.BTN_W, () -> open(player, "reskin"));
        y += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.buttonSmall(gui, 74, CnpcUltraStyle.SUBTITLE + "Browse sagas",
                CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W, () -> CnpcLmSagaGui.open(player, "sagas"));
        CnpcGuiSupport.buttonSmall(gui, 75, CnpcUltraStyle.SUBTITLE + "Saga progress",
                CnpcGuiSupport.COL_R, y, CnpcGuiSupport.BTN_W, () -> CnpcLmSagaGui.open(player, "progress"));
        y += CnpcGuiSupport.ROW_STEP;
        if (com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig.androidConversion()) {
            CnpcGuiSupport.buttonSmall(gui, 67, CnpcUltraStyle.CONFIRM + "Convert to Android",
                    CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W, () -> open(player, "android_convert"));
            y += CnpcGuiSupport.ROW_STEP;
        }
        CnpcGuiSupport.navBackToMainMenu(player, gui, y);
        CnpcGuiSupport.paintLivePlayerPreview(player, gui);
    }

    /** Change race, change class, and Android convert or remove. */
    private static void paintRaceClass(ServerPlayer player, ICustomGui gui) {
        var ph = CharacterServicesGuiApi.placeholders(player);
        boolean race = "true".equals(ph.get("can_race_change"));
        boolean klass = "true".equals(ph.get("can_class_change"));
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Race & Class"),
                CnpcUltraStyle.SUBTITLE + "Race, class, and Android");
        infoY = servicesTabs(player, gui, infoY, "home");
        int y = CnpcGuiSupport.bodyBelowHeader(infoY);
        if (race) {
            CnpcGuiSupport.buttonSmall(gui, 64, CnpcUltraStyle.INFO + "Change race",
                    CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W, () -> open(player, "race"));
        }
        if (klass) {
            CnpcGuiSupport.buttonSmall(gui, 65, CnpcUltraStyle.ACCENT + "Change class",
                    klass && race ? CnpcGuiSupport.COL_R : CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W,
                    () -> open(player, "class"));
        }
        if (race || klass) {
            y += CnpcGuiSupport.ROW_STEP;
        }
        if (com.dbzlegacy.adaptivedifficulty.progression.ProgressionConfig.androidConversion()) {
            CnpcGuiSupport.buttonSmall(gui, 67, CnpcUltraStyle.CONFIRM + "Convert to Android",
                    CnpcGuiSupport.COL_L, y, CnpcGuiSupport.BTN_W, () -> open(player, "android_convert"));
            CnpcGuiSupport.buttonSmall(gui, 66, CnpcUltraStyle.DANGER + "Remove Android",
                    CnpcGuiSupport.COL_R, y, CnpcGuiSupport.BTN_W, () -> open(player, "android_remove"));
            y += CnpcGuiSupport.ROW_STEP;
        }
        CnpcGuiSupport.navSystemRoot(player, gui, y);
        CnpcGuiSupport.paintLivePlayerPreview(player, gui);
    }

    private static void paintAndroidConvert(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = CnpcGuiSupport.target(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Android convert"),
                CnpcGuiStyle.HINT_DOUBLE_CLICK_PLAYER);
        infoY = servicesTabs(player, gui, infoY, "home");
        boolean self = subject.m_20148_().equals(player.m_20148_());
        String who = subject.m_7755_().getString();
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                self ? CnpcUltraStyle.SUBTITLE + "This converts you." : CnpcUltraStyle.SUBTITLE + "This converts " + CnpcUltraStyle.BODY + who,
                CnpcUltraStyle.DANGER + "Super forms and Legendary forms are deleted."
        ), CnpcGuiStyle.INFO_INLINE_MAX));
        String convertLabel = self
                ? CnpcUltraStyle.CONFIRM + "Convert yourself"
                : CnpcUltraStyle.CONFIRM + "Convert " + who;
        CnpcGuiSupport.buttonSmallFull(gui, 62, convertLabel, CnpcGuiSupport.M, row, CnpcGuiSupport.textBandWidth(), () -> CnpcGuiSupport.act(
                player,
                () -> com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi.handleDo(
                        player, "android", subject.m_7755_().getString(), "android_convert"),
                () -> open(player, "android_convert")));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navBackToParent(player, gui, row, () -> open(player, "main"));
    }

    private static void paintAndroidRemove(ServerPlayer player, ICustomGui gui) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage(CnpcUltraStyle.BODY, "Character", "Android Removal"),
                CnpcUltraStyle.SUBTITLE + "Click again within 10 seconds. This cannot be undone.");
        infoY = servicesTabs(player, gui, infoY, "android");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                CnpcUltraStyle.SUBTITLE + "Removes Android forms and restores what you had.",
                CnpcUltraStyle.DANGER + "This cannot be undone."
        ), CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 63, CnpcUltraStyle.DANGER + "Remove Android", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> com.dbzlegacy.adaptivedifficulty.gui.ProgressionGuiApi.handleDo(
                        player, "android_remove", "", "android_remove"),
                () -> open(player, "android_remove")));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.navBackToParent(player, gui, row, () -> open(player, "main"));
    }

    private static void footer(
            ServerPlayer player,
            ICustomGui gui,
            int row,
            String parentPage,
            ServerPlayer previewSubject) {
        if (parentPage == null) {
            CnpcGuiSupport.navBackToMainMenu(player, gui, row);
        } else {
            CnpcGuiSupport.navBackToParent(player, gui, row, () -> open(player, "main"));
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
                        return CnpcUltraStyle.CONFIRM + p[1] + " " + CnpcUltraStyle.DIM + "(current)";
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
