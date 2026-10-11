package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.net.gui.GuiOpenPacket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Startup self-test: every Ultra builder id produces a non-empty {@link GuiOpenPacket}
 * with valid panel bounds. Runs without a live player (structural probe specs + capture).
 */
public final class UltraScreenSelfTest {
    private UltraScreenSelfTest() {}

    public static void runAtStartup() {
        Map<String, String> failures = new LinkedHashMap<>();
        int ok = 0;
        for (String id : UltraScreenIds.BUILDER_IDS) {
            if (id == null || id.isBlank()) {
                continue;
            }
            try {
                List<GuiOpenPacket> packets = UltraScreenSpec.captureSends(() -> probeBuilder(id));
                if (packets == null || packets.isEmpty()) {
                    failures.put(id, "no GuiOpenPacket produced");
                    continue;
                }
                for (GuiOpenPacket pkt : packets) {
                    String err = validatePacket(pkt, id);
                    if (err != null) {
                        failures.put(id, err);
                    } else {
                        ok++;
                    }
                }
            } catch (Throwable t) {
                failures.put(
                        id,
                        t.getClass().getSimpleName()
                                + ": "
                                + (t.getMessage() == null ? "" : t.getMessage()));
            }
        }
        if (failures.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra screen builder self-test OK ({} packets)",
                    AdaptiveDifficultyMod.MOD_ID,
                    ok);
        } else {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra screen builder self-test failures ({} ok): {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    ok,
                    failures);
        }
    }

    /**
     * Representative main-page spec mirroring each {@code Ultra*Screens} builder shape.
     * Does not require a {@link net.minecraft.server.level.ServerPlayer}.
     */
    private static void probeBuilder(String id) {
        UltraScreenSpec spec =
                switch (id) {
                    case "hub" -> UltraScreenSpec.of("hub")
                            .title("Legacy Mechanics")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home")
                            .line("self-test")
                            .buttonLeft("open:difficulty", "Difficulty", "normal")
                            .buttonRight("open:character", "Character", "normal")
                            .footerBackToMain();
                    case "admin" -> UltraScreenSpec.of("admin")
                            .title("Staff Admin")
                            .page("admin")
                            .activeTab("home")
                            .tabs("home|Home")
                            .line("self-test")
                            .button("do:reload", "Reload", "confirm")
                            .buttonLeft("open:progression", "Progression", "normal")
                            .buttonRight("open:config", "Config", "normal")
                            .footerBackToMain();
                    case "difficulty" -> UltraScreenSpec.of("difficulty")
                            .title("Difficulty")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home", "tiers|Tiers", "summons|Summons")
                            .line("self-test")
                            .buttonLeft("tab:tiers", "Tiers", "normal")
                            .footerBackHome();
                    case "rival" -> UltraScreenSpec.of("rival")
                            .title("Rival")
                            .page("home")
                            .activeTab("home")
                            .tabs("home|Home", "actions|Actions", "challenge|Challenge")
                            .line("self-test")
                            .footerBackHome();
                    case "spar" -> UltraScreenSpec.of("spar")
                            .title("Sparring")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home", "dojo|Dojo", "dojo_war|Wars")
                            .line("self-test")
                            .footerBackHome();
                    case "prestige" -> UltraScreenSpec.of("prestige")
                            .title("Prestige")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home", "turnin|Turn-in", "shop|Shop")
                            .line("self-test")
                            .footerBackHome();
                    case "character" -> UltraScreenSpec.of("character")
                            .title("Character Services")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home", "saga|Saga Reset", "android|Android Removal")
                            .line("self-test")
                            .footerBackHome();
                    case "progression" -> UltraScreenSpec.of("progression")
                            .title("Progression")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home", "boosts|Boosts", "coins|Coins")
                            .line("self-test")
                            .footerBackHome();
                    case "config" -> UltraScreenSpec.of("config")
                            .title("Config Editor")
                            .page("list")
                            .activeTab("home")
                            .tabs("home|Home")
                            .line("self-test")
                            .button("nav:refresh", "Refresh", "dim")
                            .footerBackToMain();
                    default -> UltraScreenSpec.of(id)
                            .title(id)
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home")
                            .line("self-test")
                            .button("nav:refresh", "Refresh", "dim");
                };
        spec.send(null); // probe mode captures without a player
    }

    /** @return error message or null when valid */
    static String validatePacket(GuiOpenPacket pkt, String expectedId) {
        if (pkt == null) {
            return "null packet";
        }
        if (pkt.screenId() == null || pkt.screenId().isBlank()) {
            return "empty screenId";
        }
        if (expectedId != null
                && !expectedId.isBlank()
                && !expectedId.equalsIgnoreCase(pkt.screenId())) {
            return "screenId=" + pkt.screenId() + " (expected " + expectedId + ")";
        }
        if (pkt.title() == null || pkt.title().isBlank()) {
            return "empty title";
        }
        if (pkt.tabsJson() == null || pkt.tabsJson().isBlank()) {
            return "empty tabsJson";
        }
        if (pkt.buttonsJson() == null || pkt.buttonsJson().isBlank() || "[]".equals(pkt.buttonsJson())) {
            return "empty buttonsJson";
        }
        if (pkt.panelWidth() <= 0) {
            return "panelWidth=" + pkt.panelWidth();
        }
        if (pkt.panelHeight() <= 0) {
            return "panelHeight=" + pkt.panelHeight();
        }
        if (pkt.layoutJson() == null || pkt.layoutJson().isBlank()) {
            return "empty layoutJson";
        }
        return null;
    }

    /** Extra tab-navigation check: tab: must stay on the same screenId. */
    public static void verifyTabStaysOnScreen() {
        List<String> issues = new ArrayList<>();
        String[] screens = {"difficulty", "character", "rival", "spar", "prestige", "progression"};
        for (String screen : screens) {
            List<GuiOpenPacket> packets =
                    UltraScreenSpec.captureSends(
                            () -> {
                                UltraScreenSpec.of(screen)
                                        .title(screen)
                                        .page("main")
                                        .activeTab("home")
                                        .tabs("home|Home", "other|Other")
                                        .line("tab-probe")
                                        .button("tab:other", "Other", "normal")
                                        .send(null);
                            });
            if (packets.isEmpty()) {
                issues.add(screen + ": no packet");
                continue;
            }
            GuiOpenPacket pkt = packets.get(0);
            if (!screen.equalsIgnoreCase(pkt.screenId())) {
                issues.add(screen + ": got " + pkt.screenId());
            }
            if (pkt.tabsJson() == null || !pkt.tabsJson().contains("tab:")) {
                issues.add(screen + ": tabsJson missing tab: actions");
            }
        }
        if (issues.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra tab: self-check OK (same screenId, tab actions in tabsJson)",
                    AdaptiveDifficultyMod.MOD_ID);
        } else {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra tab: self-check issues: {}", AdaptiveDifficultyMod.MOD_ID, issues);
        }
    }
}
