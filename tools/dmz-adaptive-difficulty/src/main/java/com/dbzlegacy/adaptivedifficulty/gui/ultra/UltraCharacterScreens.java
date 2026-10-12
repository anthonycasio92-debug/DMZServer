package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetConfig;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService.Offer;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/** Character Services — Home / Saga Reset / Android Removal. */
public final class UltraCharacterScreens {
    private UltraCharacterScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        if (p.startsWith("saga_confirm:")) {
            openSagaConfirm(player, p.substring("saga_confirm:".length()).trim());
            return;
        }
        switch (p) {
            case "reset", "saga", "sagareset", "saga_reset" -> openSagaReset(player);
            case "android_remove", "android", "androidremove", "remove_android" ->
                    openAndroidRemove(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        // Player-model preview on the right of the info band (client-rendered; optional).
        return UltraScreenSpec.of("character")
                .title("Character Services")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "saga|Saga Reset", "android|Android Removal")
                .playerModel(248, 36, 86, 120, 1.0f, true);
    }

    /** Overview + look tools only — Saga / Android live on their tabs. */
    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = CharacterServicesGuiApi.placeholders(player);
        UltraScreenSpec spec = base("main", "home")
                .labelValue("Race", ph.getOrDefault("current_race", "—"))
                .labelValue("Class", ph.getOrDefault("current_class", "—"))
                .labelValue("Services", "true".equals(ph.get("enabled")) ? "On" : "Off")
                .line(UltraStyle.LABEL + "Use the Saga Reset and Android Removal tabs for those services.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        if ("true".equals(ph.get("can_reskin"))) {
            spec.buttonLeft("do:reskin_confirm", UltraStyle.confirm("Reskin"), "confirm");
        }
        if ("true".equals(ph.get("can_head_bones"))) {
            spec.buttonRight("do:bone_race_default", "Race default look", "normal");
        }
        if ("true".equals(ph.get("can_reskin")) || "true".equals(ph.get("can_head_bones"))) {
            spec.rowGap();
        }
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openSagaReset(ServerPlayer player) {
        UltraScreenSpec spec = base("reset", "saga")
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
            // page: navigation — avoids do: parse issues with saga ids that contain colons.
            String action = "page:saga_confirm:" + offer.id();
            String label;
            String style;
            if (offer.resettable()) {
                label = UltraStyle.confirm("Reset " + offer.name());
                style = "confirm";
            } else {
                label = UltraStyle.dim(offer.name() + " · " + offer.statusLabel());
                style = "dim";
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
        UltraScreenSpec spec = base("saga_confirm:" + sagaId, "saga");
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
            // Prefer the canonical saga id from the offer (not the raw page token).
            spec.buttonLeft(
                            "do:saga_reset:" + offer.id(),
                            UltraStyle.danger("Confirm reset"),
                            "danger")
                    .buttonRight("tab:saga", UltraStyle.dim("Cancel"), "dim")
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
            spec.button("tab:saga", UltraStyle.dim("Back to saga list"), "dim").rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openAndroidRemove(ServerPlayer player) {
        UltraScreenSpec spec = base(UltraStyle.PAGE_ANDROID_REMOVE, "android")
                .line(UltraStyle.LABEL + "Removes Android forms and restores what you had.")
                .notice("Click again within 10 seconds. This cannot be undone.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        spec.button("do:" + UltraStyle.PAGE_ANDROID_REMOVE, UltraStyle.danger("Remove Android"), "danger")
                .rowGap();
        // Convert is staff-only (display + server gate in UltraGuiActions.characterDo).
        if (StaffAccess.isStaff(player) && CharacterServicesConfig.get().enabled) {
            spec.button("do:android", UltraStyle.confirm("Convert to Android"), "confirm").rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }
}
