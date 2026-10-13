package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.character.CosmeticHeadBoneService;
import com.dbzlegacy.adaptivedifficulty.character.HeadPartPieces;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetConfig;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService.Offer;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Character Services - Home / Saga Reset / Android Removal (+ race, class, model pages). */
public final class UltraCharacterScreens {
    private UltraCharacterScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String p = page == null || page.isBlank() ? "main" : page.trim();
        String lower = p.toLowerCase(Locale.ROOT);
        if (lower.startsWith("saga_confirm:")) {
            openSagaConfirm(player, p.substring("saga_confirm:".length()).trim());
            return;
        }
        if (lower.startsWith("race_confirm:")) {
            openRaceConfirm(player, p.substring("race_confirm:".length()).trim());
            return;
        }
        if (lower.startsWith("class_confirm:")) {
            openClassConfirm(player, p.substring("class_confirm:".length()).trim());
            return;
        }
        if (lower.startsWith("bones:") || "bones".equals(lower)) {
            openBones(player, parseBonePage(lower));
            return;
        }
        if (lower.startsWith("race:") || "race".equals(lower)) {
            openRace(player, parseRaceListPage(lower));
            return;
        }
        switch (lower) {
            case "reset", "saga", "sagareset", "saga_reset" -> openSagaReset(player);
            case "android_remove", "android", "androidremove", "remove_android" ->
                    openAndroidRemove(player);
            case "class" -> openClass(player);
            case "reskin" -> openReskin(player);
            case "reskin_confirm" -> openReskinConfirm(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(ServerPlayer player, String page, String tab) {
        // y=60 sits below the tab strip (CONTENT_ROW_TABS=58); y=36 clipped the head.
        return UltraScreenSpec.of("character")
                .title("Character Services")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "saga|Saga Reset", "android|Android Removal")
                // Scale 0.9 clears slight head clip at 1.0; 1.5 (4.6.116) overshot badly.
                .playerModel(player, 248, 60, 86, 120, 0.9f, true);
    }

    /** Overview + look tools - Change Race / Class / Model / Reskin on Home. */
    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = CharacterServicesGuiApi.placeholders(player);
        boolean canRace = "true".equals(ph.get("can_race_change"));
        boolean canClass = "true".equals(ph.get("can_class_change"));
        boolean canReskin = "true".equals(ph.get("can_reskin"));
        boolean canBones = "true".equals(ph.get("can_head_bones"));
        boolean monkeyFormModel = "true".equals(ph.get("head_bones_form_model_blocked"));
        UltraScreenSpec spec = base(player, "main", "home")
                .labelValue("Race", ph.getOrDefault("current_race", "-"))
                .labelValue("Class", ph.getOrDefault("current_class", "-"))
                .line(UltraStyle.LABEL + "Change your race, class, or look. Saga and Android have their own tabs.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);

        if (canRace) {
            spec.buttonLeft("page:race:0", UltraStyle.confirm("Change race"), UltraStyle.STYLE_CONFIRM);
        } else {
            spec.buttonLeft("page:race:0", UltraStyle.dim("Change race"), UltraStyle.STYLE_DIM);
        }
        if (canClass) {
            spec.buttonRight("page:class", UltraStyle.confirm("Change class"), UltraStyle.STYLE_CONFIRM);
        } else {
            spec.buttonRight("page:class", UltraStyle.dim("Change class"), UltraStyle.STYLE_DIM);
        }
        spec.rowGap();

        if (canBones) {
            spec.buttonLeft(
                    "page:bones:0",
                    UltraStyle.confirm("Model customization"),
                    UltraStyle.STYLE_CONFIRM);
        } else {
            spec.buttonLeft(
                    "page:bones:0", UltraStyle.dim("Model customization"), UltraStyle.STYLE_DIM);
        }
        if (canReskin) {
            spec.buttonRight("page:reskin", UltraStyle.confirm("Reskin"), UltraStyle.STYLE_CONFIRM);
        } else {
            spec.buttonRight("page:reskin", UltraStyle.dim("Reskin"), UltraStyle.STYLE_DIM);
        }
        spec.rowGap();
        if (monkeyFormModel) {
            spec.line(UltraStyle.DIM
                    + "Not available for Monkey - this race uses a form model.");
        }

        spec.footerBackToMain();
        spec.send(player);
    }

    /** Races per Ultra page (two columns). Hard-capping without paging hid later races. */
    private static final int RACES_PER_PAGE = 10;

    private static void openRace(ServerPlayer player, int page) {
        List<String> cards = CharacterServicesGuiApi.raceCards(player);
        int total = cards == null ? 0 : cards.size();
        int pages = Math.max(1, (total + RACES_PER_PAGE - 1) / RACES_PER_PAGE);
        int pg = Math.min(pages - 1, Math.max(0, page));
        int from = pg * RACES_PER_PAGE;
        int to = Math.min(total, from + RACES_PER_PAGE);
        UltraScreenSpec spec = base(player, "race:" + pg, "home")
                .line(UltraStyle.LABEL + "Choose the race you want to become.")
                .labelValue("Races", total + (pages > 1 ? (" | page " + (pg + 1) + "/" + pages) : ""))
                .notice("Race-only form ladders reset when the new race does not use them.");
        for (String line : CharacterServicesGuiApi.linesForPage(player, "race")) {
            if (line != null && !line.isBlank()) {
                spec.line(UltraStyle.LABEL + UltraStyle.plain(line));
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        int placed = 0;
        for (int i = from; i < to; i++) {
            String card = cards.get(i);
            if (card == null || card.isBlank()) {
                continue;
            }
            String[] p = card.split("\t", -1);
            String id = p.length > 0 ? p[0].trim() : "";
            String name = p.length > 1 ? p[1].trim() : id;
            boolean current = p.length > 2 && "1".equals(p[2]);
            if (id.isBlank()) {
                continue;
            }
            String action = "page:race_confirm:" + id + ":0";
            String label;
            String style;
            if (current) {
                label = UltraStyle.dim(name + " | current");
                style = UltraStyle.STYLE_DIM;
            } else {
                label = UltraStyle.confirm(name);
                style = UltraStyle.STYLE_CONFIRM;
            }
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, style);
            } else {
                spec.buttonRight(action, label, style);
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("No races are available right now."));
        }
        if (pg > 0) {
            spec.buttonLeft(
                    "page:race:" + (pg - 1), UltraStyle.dim("< Prev"), UltraStyle.STYLE_DIM);
        }
        if (pg + 1 < pages) {
            if (pg > 0) {
                spec.buttonRight(
                        "page:race:" + (pg + 1), UltraStyle.LABEL + "Next >", UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:race:" + (pg + 1), UltraStyle.LABEL + "Next >", UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (pg > 0) {
            spec.rowGap();
        }
        // footerBackHome only - a second tab:home Back stacked a duplicate button.
        spec.footerBackHome();
        spec.send(player);
    }

    private static int parseRaceListPage(String page) {
        if (page == null || page.isBlank() || "race".equalsIgnoreCase(page.trim())) {
            return 0;
        }
        String raw = page.trim();
        int colon = raw.indexOf(':');
        if (colon < 0 || colon + 1 >= raw.length()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(raw.substring(colon + 1).trim()));
        } catch (NumberFormatException ignored) {
            return 0;
        }
    }

    private static void openRaceConfirm(ServerPlayer player, String raceAndPct) {
        String[] bits = raceAndPct.split(":", 2);
        String raceId = bits[0] == null ? "" : bits[0].trim();
        int selected = 0;
        if (bits.length > 1) {
            try {
                selected = Integer.parseInt(bits[1].trim());
            } catch (NumberFormatException ignored) {
                selected = 0;
            }
        }
        String confirmArg = raceId + ":" + selected;
        UltraScreenSpec spec = base(player, "race_confirm:" + confirmArg, "home")
                .line(UltraStyle.LABEL + "Review cost, then confirm.");
        for (String line : CharacterServicesGuiApi.linesForPage(player, "race_confirm:" + confirmArg)) {
            if (line != null && !line.isBlank()) {
                spec.line(UltraStyle.LABEL + UltraStyle.plain(line));
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        int[] keepSteps = CharacterServicesConfig.preservationSteps();
        int placed = 0;
        for (int keep : keepSteps) {
            String action = "page:race_confirm:" + raceId + ":" + keep;
            String label = keep == selected
                    ? UltraStyle.confirm("Keep " + keep + "%")
                    : UltraStyle.LABEL + "Keep " + keep + "%";
            String style = keep == selected ? UltraStyle.STYLE_CONFIRM : UltraStyle.STYLE_NORMAL;
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, style);
            } else {
                spec.buttonRight(action, label, style);
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        spec.buttonLeft(
                        "do:race_confirm:" + confirmArg,
                        UltraStyle.confirm("Confirm & pay"),
                        UltraStyle.STYLE_CONFIRM)
                .buttonRight("page:race:0", UltraStyle.dim("Cancel"), UltraStyle.STYLE_DIM)
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openClass(ServerPlayer player) {
        UltraScreenSpec spec = base(player, "class", "home")
                .line(UltraStyle.LABEL + "Pick a new fighting class for your current race.");
        for (String line : CharacterServicesGuiApi.linesForPage(player, "class")) {
            if (line != null && !line.isBlank()) {
                spec.line(UltraStyle.LABEL + UltraStyle.plain(line));
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        List<String> cards = CharacterServicesGuiApi.classCards(player);
        int placed = 0;
        for (String card : cards) {
            if (card == null || card.isBlank()) {
                continue;
            }
            String[] p = card.split("\t", -1);
            String id = p.length > 0 ? p[0].trim() : "";
            String name = p.length > 1 ? p[1].trim() : id;
            boolean current = p.length > 2 && "1".equals(p[2]);
            if (id.isBlank()) {
                continue;
            }
            String action = "page:class_confirm:" + id;
            String label;
            String style;
            if (current) {
                label = UltraStyle.dim(name + " | current");
                style = UltraStyle.STYLE_DIM;
            } else {
                label = UltraStyle.confirm(name);
                style = UltraStyle.STYLE_CONFIRM;
            }
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, style);
            } else {
                spec.buttonRight(action, label, style);
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("No classes are available for your race."));
        }
        // footerBackHome only - avoid duplicate Back with tab:home.
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openClassConfirm(ServerPlayer player, String classId) {
        String id = classId == null ? "" : classId.trim();
        UltraScreenSpec spec = base(player, "class_confirm:" + id, "home")
                .line(UltraStyle.LABEL + "Review cost, then confirm.");
        for (String line : CharacterServicesGuiApi.linesForPage(player, "class_confirm:" + id)) {
            if (line != null && !line.isBlank()) {
                spec.line(UltraStyle.LABEL + UltraStyle.plain(line));
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.buttonLeft(
                        "do:class_confirm:" + id,
                        UltraStyle.confirm("Confirm & pay"),
                        UltraStyle.STYLE_CONFIRM)
                .buttonRight("page:class", UltraStyle.dim("Cancel"), UltraStyle.STYLE_DIM)
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openReskin(ServerPlayer player) {
        UltraScreenSpec spec = base(player, "reskin", "home")
                .line(UltraStyle.LABEL + "Opens the appearance editor after you confirm.");
        for (String line : CharacterServicesGuiApi.linesForPage(player, "reskin")) {
            if (line != null && !line.isBlank()) {
                spec.line(UltraStyle.LABEL + UltraStyle.plain(line));
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.button("page:reskin_confirm", UltraStyle.confirm("Review cost & continue"), UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openReskinConfirm(ServerPlayer player) {
        UltraScreenSpec spec = base(player, "reskin_confirm", "home")
                .line(UltraStyle.LABEL + "Review cost, then confirm.");
        for (String line : CharacterServicesGuiApi.linesForPage(player, "reskin_confirm")) {
            if (line != null && !line.isBlank()) {
                spec.line(UltraStyle.LABEL + UltraStyle.plain(line));
            }
        }
        String pre = com.dbzlegacy.adaptivedifficulty.character.CharacterServicesSystem.reskinPrecheck(player);
        boolean ready = pre == null || pre.isBlank();
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        if (ready) {
            spec.buttonLeft(
                            "do:reskin_confirm",
                            UltraStyle.confirm("Confirm & pay"),
                            UltraStyle.STYLE_CONFIRM)
                    .buttonRight("page:reskin", UltraStyle.dim("Cancel"), UltraStyle.STYLE_DIM)
                    .rowGap();
        } else {
            spec.line(UltraStyle.danger(UltraStyle.plain(pre)));
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openBones(ServerPlayer player, int page) {
        if (CosmeticHeadBoneService.isFormModelRace(player)) {
            UltraScreenSpec spec = base(player, "bones:0", "home")
                    .line(UltraStyle.LABEL + "Model customization")
                    .line(UltraStyle.DIM
                            + "Not available for Monkey - this race uses a form model.")
                    .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
            spec.footerBackHome();
            spec.send(player);
            return;
        }
        int pages = Math.max(1, CosmeticHeadBoneService.pageCount());
        int pg = Math.min(pages - 1, Math.max(0, page));
        String worn = CosmeticHeadBoneService.wornLabel(CosmeticHeadBoneService.activeBone(player));
        int wornCount = HeadPartPieces.fragments(CosmeticHeadBoneService.activeBone(player)).size();
        if (wornCount > 2) {
            worn = wornCount + " parts on";
        }
        UltraScreenSpec spec = base(player, "bones:" + pg, "home")
                .line(UltraStyle.LABEL
                        + HeadPartPieces.pageTitle(pg)
                        + " | On "
                        + UltraStyle.VALUE
                        + worn);
        for (String line : CharacterServicesGuiApi.linesForPage(player, "bones:" + pg)) {
            if (line != null && !line.isBlank()) {
                spec.line(UltraStyle.LABEL + UltraStyle.plain(line));
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        List<String> cards = CharacterServicesGuiApi.headBoneCards(player, pg);
        int placed = 0;
        for (String card : cards) {
            if (placed >= 8 || card == null || card.isBlank()) {
                continue;
            }
            String[] p = card.split("\t", -1);
            String id = p.length > 0 ? p[0].trim() : "";
            String name = p.length > 1 ? p[1].trim() : id;
            String state = p.length > 2 ? p[2].trim() : "";
            if (id.isBlank()) {
                continue;
            }
            String action = "do:bone_toggle:" + id;
            String label;
            String style;
            if ("E".equals(state)) {
                label = UltraStyle.confirm(name + " | on");
                style = UltraStyle.STYLE_CONFIRM;
            } else if ("L".equals(state)) {
                String cost = p.length > 3 ? p[3].trim() : "";
                label = UltraStyle.dim(name + (cost.isBlank() ? " | locked" : " | " + cost));
                style = UltraStyle.STYLE_DIM;
                action = "do:bone_unlock:" + id;
            } else {
                label = UltraStyle.LABEL + name;
                style = UltraStyle.STYLE_NORMAL;
            }
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, style);
            } else {
                spec.buttonRight(action, label, style);
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        spec.buttonLeft(
                        "do:bone_race_default",
                        UltraStyle.confirm("Race default look"),
                        UltraStyle.STYLE_CONFIRM)
                .buttonRight(
                        "do:bone_unequip",
                        UltraStyle.danger("Clear head parts"),
                        UltraStyle.STYLE_DANGER)
                .rowGap();
        if (pg > 0) {
            spec.buttonLeft(
                    "page:bones:" + (pg - 1), UltraStyle.dim("< Head"), UltraStyle.STYLE_DIM);
        }
        if (pg + 1 < pages) {
            if (pg > 0) {
                spec.buttonRight(
                        "page:bones:" + (pg + 1), UltraStyle.LABEL + "Horns >", UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:bones:" + (pg + 1), UltraStyle.LABEL + "Horns >", UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (pg > 0) {
            spec.rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
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

    private static void openSagaReset(ServerPlayer player) {
        UltraScreenSpec spec = base(player, "reset", "saga")
                .line(UltraStyle.LABEL + "Pay Ancient Coins to replay one saga.")
                .notice("Resetting clears that saga's progress. This cannot be undone.");
        if (!SagaResetConfig.get().enabled) {
            spec.line(UltraStyle.danger(SagaResetService.RESET_OFF))
                    .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
            spec.footerBackHome();
            spec.send(player);
            return;
        }
        List<Offer> offers = SagaResetService.offers(player);
        if (offers.isEmpty()) {
            spec.line(UltraStyle.dim("No sagas are loaded."));
        } else {
            int shown = 0;
            for (Offer offer : offers) {
                if (shown >= 10) {
                    break;
                }
                spec.line(UltraStyle.LABEL + offer.name() + " " + UltraStyle.VALUE + offer.rowDetail());
                shown++;
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        int placed = 0;
        for (Offer offer : offers) {
            if (placed >= 8 || offer == null || offer.id() == null || offer.id().isBlank()) {
                continue;
            }
            String action = "page:saga_confirm:" + offer.id();
            String label;
            String style;
            if (offer.resettable()) {
                label = UltraStyle.confirm("Reset " + offer.name());
                style = UltraStyle.STYLE_CONFIRM;
            } else {
                label = UltraStyle.dim(offer.name() + " | " + offer.statusLabel());
                style = UltraStyle.STYLE_DIM;
            }
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, style);
            } else {
                spec.buttonRight(action, label, style);
                spec.rowGap();
            }
            placed++;
        }
        if (placed % 2 == 1) {
            spec.rowGap();
        }
        if (placed == 0) {
            spec.line(UltraStyle.dim("Nothing to reset until you play a saga."));
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openSagaConfirm(ServerPlayer player, String sagaId) {
        Offer offer = SagaResetService.findOffer(player, sagaId);
        UltraScreenSpec spec = base(player, "saga_confirm:" + sagaId, "saga");
        for (String line : SagaResetService.confirmLines(player, sagaId)) {
            spec.line(line);
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        boolean ready = offer != null
                && offer.resettable()
                && SagaResetConfig.get().enabled
                && offer.id() != null
                && !offer.id().isBlank();
        if (ready) {
            spec.buttonLeft(
                            "do:saga_reset:" + offer.id(),
                            UltraStyle.danger("Confirm reset"),
                            UltraStyle.STYLE_DANGER)
                    .buttonRight("tab:saga", UltraStyle.dim("Cancel"), UltraStyle.STYLE_DIM)
                    .rowGap();
        } else {
            String why = offer == null
                    ? SagaResetService.NOT_LOADED
                    : offer.status() == SagaResetService.Status.LOCKED
                            ? SagaResetService.SAGA_LOCKED
                            : offer.status() == SagaResetService.Status.NOT_STARTED
                                    ? SagaResetService.NOTHING_TO_RESET
                                    : SagaResetService.RESET_OFF;
            spec.line(UltraStyle.danger(why));
            spec.button("tab:saga", UltraStyle.dim("Back to saga list"), UltraStyle.STYLE_DIM).rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openAndroidRemove(ServerPlayer player) {
        UltraScreenSpec spec = base(player, UltraStyle.PAGE_ANDROID_REMOVE, "android")
                .line(UltraStyle.LABEL + "Removes Android forms and restores what you had.")
                .notice("Click again within 10 seconds. This cannot be undone.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        spec.button(
                        "do:" + UltraStyle.PAGE_ANDROID_REMOVE,
                        UltraStyle.danger("Remove Android"),
                        UltraStyle.STYLE_DANGER)
                .rowGap();
        if (StaffAccess.isStaff(player) && CharacterServicesConfig.get().enabled) {
            spec.button("do:android", UltraStyle.confirm("Convert to Android"), UltraStyle.STYLE_CONFIRM)
                    .rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }
}
