package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesSystem;
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
        if (lower.startsWith("class:") || "class".equals(lower)) {
            openClass(player, parseListPage(lower, "class"));
            return;
        }
        if (lower.startsWith("reset:")
                || lower.startsWith("saga:")
                || "reset".equals(lower)
                || "saga".equals(lower)
                || "sagareset".equals(lower)
                || "saga_reset".equals(lower)) {
            openSagaReset(player, parseListPage(lower, "reset", "saga", "sagareset", "saga_reset"));
            return;
        }
        switch (lower) {
            case "android_remove", "android", "androidremove", "remove_android" ->
                    openAndroidRemove(player);
            case "reskin" -> openReskin(player);
            case "reskin_confirm" -> openReskinConfirm(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(ServerPlayer player, String page, String tab, boolean preview) {
        // y=60 sits below the tab button row (TAB_ROW_Y + BTN_H); y=36 clipped the head.
        UltraScreenSpec spec = UltraScreenSpec.of("character")
                .title("Character Services")
                .page(page)
                .activeTab(tab)
                // Exact short labels only — UltraScreenSelfTest.verifyMenuLinks requires
                // buttonsJson "label":"Home"|"Saga"|"Android" with no section-sign/color prefixes.
                .tabs("home|Home", "saga|Saga", "android|Android")
                .lineGap(2)
                .bodyPad(4);
        // Preview only on Home (and model customize via sub) — not on Saga/Android dumps.
        if (preview) {
            spec.playerModel(player, 248, 60, 86, 120, 0.9f, true);
        }
        return spec;
    }

    /** Subpage: no tab row — dual footer (Back + Main menu). */
    private static UltraScreenSpec sub(ServerPlayer player, String page, boolean preview) {
        UltraScreenSpec spec = UltraScreenSpec.of("character")
                .title("Character Services")
                .page(page)
                .lineGap(2)
                .bodyPad(4);
        // Preview only on model customize (bones). Race/class/reskin stay text-only.
        if (preview) {
            spec.playerModel(player, 248, 60, 86, 120, 0.9f, true);
        }
        return spec;
    }


    /** Overview + look tools - Change Race / Class / Model / Reskin on Home. */
    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = CharacterServicesGuiApi.placeholders(player);
        boolean canRace = "true".equals(ph.get("can_race_change"));
        boolean canClass = "true".equals(ph.get("can_class_change"));
        boolean canReskin = "true".equals(ph.get("can_reskin"));
        boolean canBones = "true".equals(ph.get("can_head_bones"));
        boolean monkeyFormModel = "true".equals(ph.get("head_bones_form_model_blocked"));
        UltraScreenSpec spec = base(player, "main", "home", true)
                .labelValue("Race", ph.getOrDefault("current_race", "-"))
                .labelValue("Class", ph.getOrDefault("current_class", "-"))
                .setRow(UltraStyle.CONTENT_ROW_TABS + 90);

        if (canRace) {
            spec.buttonLeft("page:race:0", UltraStyle.confirm("Change race"), UltraStyle.STYLE_CONFIRM);
        } else {
            spec.buttonLeft("page:race:0", UltraStyle.dim("Change race"), UltraStyle.STYLE_DIM);
        }
        if (canClass) {
            spec.buttonRight("page:class:0", UltraStyle.confirm("Change class"), UltraStyle.STYLE_CONFIRM);
        } else {
            spec.buttonRight("page:class:0", UltraStyle.dim("Change class"), UltraStyle.STYLE_DIM);
        }
        spec.rowGap();

        if (canBones) {
            spec.buttonLeft(
                    "page:bones:0",
                    UltraStyle.confirm("Customize model"),
                    UltraStyle.STYLE_CONFIRM);
        } else {
            spec.buttonLeft(
                    "page:bones:0", UltraStyle.dim("Customize model"), UltraStyle.STYLE_DIM);
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
    /** Saga reset / class pickers: two-up rows; page so discovery growth never truncates. */
    private static final int DISCOVERY_PER_PAGE = 8;

    private static void openRace(ServerPlayer player, int page) {
        List<String> cards = CharacterServicesGuiApi.raceCards(player);
        int total = cards == null ? 0 : cards.size();
        int pages = Math.max(1, (total + RACES_PER_PAGE - 1) / RACES_PER_PAGE);
        int pg = Math.min(pages - 1, Math.max(0, page));
        int from = pg * RACES_PER_PAGE;
        int to = Math.min(total, from + RACES_PER_PAGE);
        long keep100Cost = CharacterServicesSystem.raceAmountDue(player, 100);
        String keep100Text = keep100Cost <= 0L ? "Free" : CharacterServicesSystem.formatCost(keep100Cost);
        UltraScreenSpec spec = sub(player, "race:" + pg, false)
                .line(UltraStyle.LABEL + "Choose your new race.")
                .labelValue("Races", total + (pages > 1 ? (" page " + (pg + 1) + "/" + pages) : ""))
                .labelValue("Cost (keep 100%)", keep100Text)
                .labelValue("Keep 0%", "Free");
        // Cost is on labelValue; body lines keep the keep-% note / skills / cooldown.
        appendCompactLines(spec, CharacterServicesGuiApi.linesForPage(player, "race"), 3, true);
        spec.setRow(UltraStyle.CONTENT_ROW + 80);
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
                label = UltraStyle.dim(name + " (you)");
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
                    "page:race:" + (pg - 1), UltraStyle.dim("Prev"), UltraStyle.STYLE_DIM);
        }
        if (pg + 1 < pages) {
            if (pg > 0) {
                spec.buttonRight(
                        "page:race:" + (pg + 1), UltraStyle.LABEL + "Next", UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:race:" + (pg + 1), UltraStyle.LABEL + "Next", UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (pg > 0) {
            spec.rowGap();
        }
        // footerBackHome only - a second tab:home Back stacked a duplicate button.
        spec.footerBackHome("Character Home");
        spec.send(player);
    }

    private static int parseRaceListPage(String page) {
        return parseListPage(page, "race");
    }

    /** Parse {@code prefix} or {@code prefix:N} page index (0-based). */
    private static int parseListPage(String page, String... prefixes) {
        if (page == null || page.isBlank()) {
            return 0;
        }
        String raw = page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (prefixes != null) {
            for (String prefix : prefixes) {
                if (prefix != null && lower.equals(prefix.toLowerCase(Locale.ROOT))) {
                    return 0;
                }
            }
        }
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
        long due = CharacterServicesSystem.raceAmountDue(player, selected);
        String costText = due <= 0L ? "Free" : CharacterServicesSystem.formatCost(due);
        String raceLabel = raceId.isBlank() ? "?" : raceId;
        UltraScreenSpec spec = sub(player, "race_confirm:" + confirmArg, false)
                .line(UltraStyle.LABEL + "Confirm race change")
                .labelValue("Race", raceLabel)
                .labelValue("Keep", selected + "%")
                .labelValue("Cost", costText);
        // Cost/Keep are on labelValue; body keeps pricing note + carry-over + preview.
        appendCompactLines(
                spec, CharacterServicesGuiApi.linesForPage(player, "race_confirm:" + confirmArg), 6, true);
        spec.setRow(UltraStyle.CONTENT_ROW + 90);
        int[] keepSteps = CharacterServicesConfig.preservationSteps();
        int placed = 0;
        for (int keep : keepSteps) {
            String action = "page:race_confirm:" + raceId + ":" + keep;
            long keepDue = CharacterServicesSystem.raceAmountDue(player, keep);
            String keepPrice = keepDue <= 0L ? "free" : CharacterServicesSystem.formatCost(keepDue);
            String keepLabel = "Keep " + keep + "% - " + keepPrice;
            String label = keep == selected
                    ? UltraStyle.confirm(keepLabel)
                    : UltraStyle.LABEL + keepLabel;
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
        String confirmLabel = due <= 0L ? "Confirm (free)" : "Confirm & pay - " + costText;
        spec.button(
                        "do:race_confirm:" + confirmArg,
                        UltraStyle.confirm(confirmLabel),
                        UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.footerBack("page:race:0", "Race list");
        spec.send(player);
    }

    private static void openClass(ServerPlayer player, int page) {
        List<String> cards = CharacterServicesGuiApi.classCards(player);
        int total = cards == null ? 0 : cards.size();
        int pages = Math.max(1, (total + DISCOVERY_PER_PAGE - 1) / DISCOVERY_PER_PAGE);
        int pg = Math.min(pages - 1, Math.max(0, page));
        int from = pg * DISCOVERY_PER_PAGE;
        int to = Math.min(total, from + DISCOVERY_PER_PAGE);
        UltraScreenSpec spec = sub(player, "class:" + pg, false)
                .line(UltraStyle.LABEL + "Pick a class for your current race.")
                .labelValue(
                        "Classes",
                        total + (pages > 1 ? (" page " + (pg + 1) + "/" + pages) : ""));
        appendCompactLines(spec, CharacterServicesGuiApi.linesForPage(player, "class"), 3);
        spec.setRow(UltraStyle.CONTENT_ROW + 80);
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
            String action = "page:class_confirm:" + id;
            String label;
            String style;
            if (current) {
                label = UltraStyle.dim(name + " (you)");
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
        if (pg > 0) {
            spec.buttonLeft("page:class:" + (pg - 1), UltraStyle.dim("Prev"), UltraStyle.STYLE_DIM);
        }
        if (pg + 1 < pages) {
            if (pg > 0) {
                spec.buttonRight(
                        "page:class:" + (pg + 1), UltraStyle.LABEL + "Next", UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:class:" + (pg + 1), UltraStyle.LABEL + "Next", UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (pg > 0) {
            spec.rowGap();
        }
        // footerBackHome only - avoid duplicate Back with tab:home.
        spec.footerBackHome("Character Home");
        spec.send(player);
    }

    private static void openClassConfirm(ServerPlayer player, String classId) {
        String id = classId == null ? "" : classId.trim();
        UltraScreenSpec spec = sub(player, "class_confirm:" + id, false)
                .line(UltraStyle.LABEL + "Confirm class change");
        appendCompactLines(
                spec, CharacterServicesGuiApi.linesForPage(player, "class_confirm:" + id), 4);
        spec.setRow(UltraStyle.CONTENT_ROW + 90);
        spec.button(
                        "do:class_confirm:" + id,
                        UltraStyle.confirm("Confirm & pay"),
                        UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.footerBack("page:class:0", "Class list");
        spec.send(player);
    }

    private static void openReskin(ServerPlayer player) {
        UltraScreenSpec spec = sub(player, "reskin", false)
                .line(UltraStyle.LABEL + "Change hair, colors, and other cosmetics.");
        appendCompactLines(spec, CharacterServicesGuiApi.linesForPage(player, "reskin"), 4);
        spec.setRow(UltraStyle.CONTENT_ROW + 80);
        spec.button("page:reskin_confirm", UltraStyle.confirm("Review cost"), UltraStyle.STYLE_CONFIRM)
                .rowGap();
        spec.footerBackHome("Character Home");
        spec.send(player);
    }

    private static void openReskinConfirm(ServerPlayer player) {
        UltraScreenSpec spec = sub(player, "reskin_confirm", false)
                .line(UltraStyle.LABEL + "Confirm reskin");
        appendCompactLines(spec, CharacterServicesGuiApi.linesForPage(player, "reskin_confirm"), 5);
        String pre = com.dbzlegacy.adaptivedifficulty.character.CharacterServicesSystem.reskinPrecheck(player);
        boolean ready = pre == null || pre.isBlank();
        spec.setRow(UltraStyle.CONTENT_ROW + 90);
        if (ready) {
            spec.button(
                            "do:reskin_confirm",
                            UltraStyle.confirm("Confirm & pay"),
                            UltraStyle.STYLE_CONFIRM)
                    .rowGap();
        } else {
            spec.line(UltraStyle.danger(UltraStyle.plain(pre)));
        }
        spec.footerBack("page:reskin", "Reskin");
        spec.send(player);
    }

    private static void openBones(ServerPlayer player, int page) {
        if (CosmeticHeadBoneService.isFormModelRace(player)) {
            UltraScreenSpec spec = sub(player, "bones:0", true)
                    .line(UltraStyle.LABEL + "Model customization")
                    .line(UltraStyle.DIM
                            + "Not available for Monkey - this race uses a form model.")
                    .setRow(UltraStyle.CONTENT_ROW + 80);
            spec.footerBackHome("Character Home");
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
        UltraScreenSpec spec = sub(player, "bones:" + pg, true)
                .labelValue(HeadPartPieces.pageTitle(pg), "On: " + UltraStyle.plain(worn));
        appendCompactLines(spec, CharacterServicesGuiApi.linesForPage(player, "bones:" + pg), 2);
        spec.setRow(UltraStyle.CONTENT_ROW + 80);
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
                label = UltraStyle.confirm(name + " (on)");
                style = UltraStyle.STYLE_CONFIRM;
            } else if ("L".equals(state)) {
                String cost = p.length > 3 ? p[3].trim() : "";
                label = UltraStyle.dim(name + (cost.isBlank() ? " (locked)" : " " + cost));
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
                        UltraStyle.confirm("Race default"),
                        UltraStyle.STYLE_CONFIRM)
                .buttonRight(
                        "do:bone_unequip",
                        UltraStyle.danger("Clear parts"),
                        UltraStyle.STYLE_DANGER)
                .rowGap();
        if (pg > 0) {
            spec.buttonLeft(
                    "page:bones:" + (pg - 1), UltraStyle.dim("Prev"), UltraStyle.STYLE_DIM);
        }
        if (pg + 1 < pages) {
            if (pg > 0) {
                spec.buttonRight(
                        "page:bones:" + (pg + 1), UltraStyle.LABEL + "Next", UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:bones:" + (pg + 1), UltraStyle.LABEL + "Next", UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (pg > 0) {
            spec.rowGap();
        }
        spec.footerBackHome("Character Home");
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

    private static void openSagaReset(ServerPlayer player, int page) {
        List<Offer> offers = SagaResetService.offers(player);
        int total = offers == null ? 0 : offers.size();
        int pages = Math.max(1, (total + DISCOVERY_PER_PAGE - 1) / DISCOVERY_PER_PAGE);
        int pg = Math.min(pages - 1, Math.max(0, page));
        int from = pg * DISCOVERY_PER_PAGE;
        int to = Math.min(total, from + DISCOVERY_PER_PAGE);
        UltraScreenSpec spec = base(player, "reset:" + pg, "saga", false)
                .line(UltraStyle.LABEL + "Pay Ancient Coins to replay one saga.")
                .labelValue(
                        "Sagas",
                        total + (pages > 1 ? (" page " + (pg + 1) + "/" + pages) : ""))
                .notice("Clears that saga's progress. This cannot be undone.");
        if (!SagaResetConfig.get().enabled) {
            spec.line(UltraStyle.danger(SagaResetService.RESET_OFF))
                    .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
            spec.footerBackToMain();
            spec.send(player);
            return;
        }
        if (total == 0) {
            spec.line(UltraStyle.dim("No sagas are loaded right now."));
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + 90);
        int placed = 0;
        for (int i = from; i < to; i++) {
            Offer offer = offers.get(i);
            if (offer == null || offer.id() == null || offer.id().isBlank()) {
                continue;
            }
            String action = "page:saga_confirm:" + offer.id();
            String label;
            String style;
            if (offer.resettable()) {
                label = UltraStyle.confirm("Reset " + offer.name());
                style = UltraStyle.STYLE_CONFIRM;
            } else {
                label = UltraStyle.dim(offer.name() + " (" + offer.statusLabel() + ")");
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
        if (pg > 0) {
            spec.buttonLeft("page:reset:" + (pg - 1), UltraStyle.dim("Prev"), UltraStyle.STYLE_DIM);
        }
        if (pg + 1 < pages) {
            if (pg > 0) {
                spec.buttonRight(
                        "page:reset:" + (pg + 1), UltraStyle.LABEL + "Next", UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:reset:" + (pg + 1), UltraStyle.LABEL + "Next", UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (pg > 0) {
            spec.rowGap();
        }
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openSagaConfirm(ServerPlayer player, String sagaId) {
        Offer offer = SagaResetService.findOffer(player, sagaId);
        UltraScreenSpec spec = sub(player, "saga_confirm:" + sagaId, false);
        appendCompactLines(spec, SagaResetService.confirmLines(player, sagaId), 6);
        spec.setRow(UltraStyle.CONTENT_ROW + 90);
        boolean ready = offer != null
                && offer.resettable()
                && SagaResetConfig.get().enabled
                && offer.id() != null
                && !offer.id().isBlank();
        if (ready) {
            spec.button(
                            "do:saga_reset:" + offer.id(),
                            UltraStyle.danger("Confirm reset"),
                            UltraStyle.STYLE_DANGER)
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
        }
        spec.footerBack("tab:saga", "Saga list");
        spec.send(player);
    }

    private static void openAndroidRemove(ServerPlayer player) {
        UltraScreenSpec spec = base(player, UltraStyle.PAGE_ANDROID_REMOVE, "android", false)
                .line(UltraStyle.LABEL + "Remove Android forms and restore your previous look.")
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
        spec.footerBackToMain();
        spec.send(player);
    }

    /** Append up to {@code max} non-blank lines; skip Ultra intro duplicates. */
    private static void appendCompactLines(UltraScreenSpec spec, List<String> lines, int max) {
        appendCompactLines(spec, lines, max, false);
    }

    /**
     * @param skipCost when true, omit {@code Cost ...} body lines (Ultra already shows Cost via
     *     {@code labelValue}, e.g. race select / confirm)
     */
    private static void appendCompactLines(
            UltraScreenSpec spec, List<String> lines, int max, boolean skipCost) {
        if (spec == null || lines == null || max <= 0) {
            return;
        }
        int placed = 0;
        for (String line : lines) {
            if (placed >= max || line == null || line.isBlank()) {
                continue;
            }
            String plain = UltraStyle.plain(line).trim();
            if (plain.isEmpty()) {
                continue;
            }
            // Skip robotic duplicates already covered by the page title line.
            String lower = plain.toLowerCase(Locale.ROOT);
            if (lower.startsWith("choose the race")
                    || lower.startsWith("pick a new fighting class")
                    || lower.startsWith("opens the appearance editor")
                    || lower.startsWith("race | class")
                    || lower.startsWith("rebuild your character")
                    || (skipCost && lower.startsWith("cost "))) {
                continue;
            }
            spec.line(UltraStyle.LABEL + plain);
            placed++;
        }
    }
}
