package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.character.CharacterServicesConfig;
import com.dbzlegacy.adaptivedifficulty.gui.CharacterServicesGuiApi;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService;
import com.dbzlegacy.adaptivedifficulty.quest.SagaResetService.Offer;
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
            case "android_remove", "android", "androidremove", "remove_android" -> openAndroidRemove(player);
            default -> openHome(player);
        }
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("character")
                .title("Character Services")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "saga|Saga Reset", "android|Android Removal");
    }

    private static void openHome(ServerPlayer player) {
        Map<String, String> ph = CharacterServicesGuiApi.placeholders(player);
        UltraScreenSpec spec = base("main", "home")
                .labelValue("Race", ph.getOrDefault("current_race", "—"))
                .labelValue("Class", ph.getOrDefault("current_class", "—"))
                .labelValue("Services", "true".equals(ph.get("enabled")) ? "On" : "Off")
                .line(UltraStyle.LABEL + "Race and class pickers use the full Character menu when needed.")
                .setRow(110);
        if ("true".equals(ph.get("can_reskin"))) {
            spec.buttonLeft("do:reskin_confirm", UltraStyle.confirm("Reskin"), "confirm");
        }
        if ("true".equals(ph.get("can_head_bones"))) {
            spec.buttonRight("do:bone_race_default", "Race default look", "normal");
        }
        if ("true".equals(ph.get("can_reskin")) || "true".equals(ph.get("can_head_bones"))) {
            spec.rowGap();
        }
        spec.buttonLeft("tab:saga", "Saga Reset", "normal")
                .buttonRight("tab:android", UltraStyle.danger("Android Removal"), "danger")
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openSagaReset(ServerPlayer player) {
        UltraScreenSpec spec = base("reset", "saga")
                .line(UltraStyle.LABEL + "Pay Ancient Coins to replay one saga.")
                .notice("Resetting clears that saga's progress. This cannot be undone.");
        List<Offer> offers = SagaResetService.offers(player);
        if (offers.isEmpty()) {
            spec.line(UltraStyle.dim("No sagas are loaded."));
        } else {
            int shown = 0;
            for (Offer offer : offers) {
                if (shown >= 8) {
                    break;
                }
                spec.line(UltraStyle.LABEL + offer.name() + " " + UltraStyle.VALUE + offer.rowDetail());
                shown++;
            }
        }
        spec.setRow(150);
        int placed = 0;
        for (Offer offer : offers) {
            if (placed >= 6) {
                break;
            }
            if (!offer.resettable()) {
                continue;
            }
            // Open confirm page first; Confirm calls do:saga_reset:<id>.
            String action = "do:saga_confirm:" + offer.id();
            String label = UltraStyle.confirm("Reset " + offer.name());
            if (placed % 2 == 0) {
                spec.buttonLeft(action, label, "confirm");
            } else {
                spec.buttonRight(action, label, "confirm");
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
        UltraScreenSpec spec = base("saga_confirm:" + sagaId, "saga");
        for (String line : SagaResetService.confirmLines(player, sagaId)) {
            spec.line(line);
        }
        spec.setRow(120);
        spec.buttonLeft("do:saga_reset:" + sagaId, UltraStyle.danger("Confirm reset"), "danger")
                .buttonRight("tab:saga", UltraStyle.dim("Cancel"), "dim")
                .rowGap();
        spec.footerBackHome();
        spec.send(player);
    }

    private static void openAndroidRemove(ServerPlayer player) {
        UltraScreenSpec spec = base("android_remove", "android")
                .line(UltraStyle.LABEL + "Removes Android forms and restores what you had.")
                .notice("Click again within 10 seconds. This cannot be undone.")
                .setRow(100);
        spec.button("do:android_remove", UltraStyle.danger("Remove Android"), "danger").rowGap();
        if (CharacterServicesConfig.get().enabled) {
            spec.button("do:android", UltraStyle.confirm("Convert to Android"), "confirm").rowGap();
        }
        spec.footerBackHome();
        spec.send(player);
    }
}
