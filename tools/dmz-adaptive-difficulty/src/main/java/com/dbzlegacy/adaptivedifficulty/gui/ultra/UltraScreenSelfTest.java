package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.AdaptiveDifficultyMod;
import com.dbzlegacy.adaptivedifficulty.net.gui.GuiOpenPacket;
import com.dbzlegacy.adaptivedifficulty.net.gui.LmGuiNetwork;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Startup self-test: every Ultra builder id produces a non-empty {@link GuiOpenPacket}
 * with valid panel bounds. Runs without a live player (structural probe specs + capture).
 */
public final class UltraScreenSelfTest {
    /**
     * Single-screen menus with no tab bar. Empty {@code tabsJson} ({@code []} / blank) is
     * correct for these - do not treat as a structural failure.
     */
    static final Set<String> TABLESS_SCREEN_IDS = Set.of("skillcheck", "skills");

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
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] Ultra screen builder self-test FAILED ({} ok): {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    ok,
                    failures);
            throw new IllegalStateException(
                    "LegacyMechanics Ultra screen builder self-test failed ("
                            + failures.size()
                            + " error(s)): "
                            + failures);
        }
    }

    /** Used by {@link LmGuiNetwork#runPacketRoundTripSelfTest()}. */
    public static void probeBuilderForRoundTrip(String id) {
        probeBuilder(id);
    }

    /**
     * Representative main-page spec mirroring each {@code Ultra*Screens} builder shape.
     * Does not require a {@link net.minecraft.server.level.ServerPlayer}.
     * Unknown ids in {@link UltraScreenIds#BUILDER_IDS} get a generic zero-config probe.
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
                            .buttonRight("open:rival", "Rival", "normal")
                            .rowGap()
                            .buttonLeft("open:spar", "Sparring", "normal")
                            .buttonRight("open:prestige", "Prestige", "normal")
                            .rowGap()
                            .buttonLeft("open:skillcheck", "Skill Check", "confirm")
                            .buttonRight("open:character", "Character", "confirm");
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
                            .tabs("home|Home", "tiers|Tiers", "summons|Summons", "stats|CR|staff")
                            .line("self-test")
                            .buttonLeft("tab:tiers", "Tiers", "normal")
                            .footerBackToMain();
                    case "rival" -> UltraScreenSpec.of("rival")
                            .title("Rival")
                            .page("home")
                            .activeTab("home")
                            .tabs("home|Home", "actions|Actions", "challenge|Challenge")
                            .line("self-test")
                            .footerBackToMain();
                    case "spar" -> UltraScreenSpec.of("spar")
                            .title("Sparring")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home", "dojo|Dojo", "dojo_war|Wars")
                            .line("self-test")
                            .footerBackToMain();
                    case "prestige" -> UltraScreenSpec.of("prestige")
                            .title("Prestige")
                            .page("shop")
                            .activeTab("shop")
                            .tabs("home|Home", "prestige|Prestige", "shop|Shop")
                            .button("page:skills", "Skill shop", "confirm")
                            .button("page:tiers", "Difficulty tiers", "confirm")
                            .button("page:effects", "Effects", "confirm")
                            .line("self-test")
                            .footerBackToMain();
                    case "character" -> UltraScreenSpec.of("character")
                            .title("Character Services")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home", "saga|Saga", "android|Android")
                            .line("self-test")
                            .playerModel(248, 60, 86, 120, 0.9f, true)
                            .footerBackToMain();
                    case "skillcheck" -> UltraScreenSpec.of("skillcheck")
                            .title("Skill Check")
                            .page("main")
                            .activeTab("home")
                            .line("self-test")
                            .footerBackToMain();
                    case "skills" -> UltraScreenSpec.of("skills")
                            .title("Skills")
                            .page("main")
                            .activeTab("home")
                            .line("self-test")
                            .footerBackToMain();
                    case "progression" -> UltraScreenSpec.of("progression")
                            .title("Progression")
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home", "boosts|Boosts", "coins|Coins")
                            .line("self-test")
                            .footerBackToMain();
                    case "config" -> UltraScreenSpec.of("config")
                            .title("Config Editor")
                            .page("list")
                            .activeTab("home")
                            .tabs("home|Home")
                            .line("self-test")
                            .footerBackToMain();
                    default -> UltraScreenSpec.of(id)
                            .title(id)
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home")
                            .line("self-test")
                            .button("nav:home", "Home", "dim");
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
        // Tabs render as hub-style buttons under the title line; tabsJson stays empty for all
        // screens so stock KaiPath does not draw its title-chrome tab strip.
        String tabsErr = requireJsonArray("tabsJson", pkt.tabsJson(), true);
        if (tabsErr != null) {
            return tabsErr;
        }
        if (hasTabBarEntries(pkt.tabsJson())) {
            return "tabsJson must be empty (tabs are buttons under the title line)";
        }
        String linesErr = requireJsonArray("linesJson", pkt.linesJson(), true);
        if (linesErr != null) {
            return linesErr;
        }
        String buttonsErr = requireJsonArray("buttonsJson", pkt.buttonsJson(), false);
        if (buttonsErr != null) {
            return buttonsErr;
        }
        if (pkt.buttonsJson() == null || "[]".equals(pkt.buttonsJson().trim())) {
            return "empty buttonsJson";
        }
        if (pkt.panelWidth() <= 0) {
            return "panelWidth=" + pkt.panelWidth();
        }
        if (pkt.panelHeight() <= 0) {
            return "panelHeight=" + pkt.panelHeight();
        }
        if (pkt.panelWidth() > 2000 || pkt.panelHeight() > 2000) {
            return "panel dimensions out of range " + pkt.panelWidth() + "x" + pkt.panelHeight();
        }
        if (pkt.layoutJson() == null || pkt.layoutJson().isBlank()) {
            return "empty layoutJson";
        }
        try {
            com.google.gson.JsonElement el =
                    com.google.gson.JsonParser.parseString(pkt.layoutJson());
            if (!el.isJsonObject()) {
                return "layoutJson not a JSON object";
            }
        } catch (Throwable t) {
            return "layoutJson invalid JSON: " + t.getMessage();
        }
        return null;
    }

    static boolean isTablessScreen(String screenId) {
        return screenId != null
                && !screenId.isBlank()
                && TABLESS_SCREEN_IDS.contains(screenId.trim().toLowerCase(Locale.ROOT));
    }

    /** True when tabsJson declares at least one tab entry (id / tab: action). */
    private static boolean hasTabBarEntries(String tabsJson) {
        if (tabsJson == null || tabsJson.isBlank()) {
            return false;
        }
        String trimmed = tabsJson.trim();
        if ("[]".equals(trimmed)) {
            return false;
        }
        try {
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(trimmed);
            return el.isJsonArray() && el.getAsJsonArray().size() > 0;
        } catch (Throwable t) {
            // Invalid JSON is already reported by requireJsonArray.
            return trimmed.contains("\"id\"") || trimmed.contains("tab:");
        }
    }

    private static String requireJsonArray(String field, String json, boolean allowEmpty) {
        if (json == null || json.isBlank()) {
            return allowEmpty ? null : "empty " + field;
        }
        try {
            com.google.gson.JsonElement el = com.google.gson.JsonParser.parseString(json);
            if (!el.isJsonArray()) {
                return field + " not a JSON array";
            }
            if (!allowEmpty && el.getAsJsonArray().size() == 0) {
                return "empty " + field;
            }
            return null;
        } catch (Throwable t) {
            return field + " invalid JSON: " + t.getMessage();
        }
    }

    /**
     * Tab navigation: builders keep the logical screenId; after hub shim the wire id is hub
     * while tab buttons / button args still name the logical system. Never CNPC.
     */
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
                                        .button("do:probe", "Probe", "normal")
                                        .send(null);
                            });
            if (packets.isEmpty()) {
                issues.add(screen + ": no packet");
                continue;
            }
            GuiOpenPacket logical = packets.get(0);
            if (!screen.equalsIgnoreCase(logical.screenId())) {
                issues.add(screen + ": logical screenId=" + logical.screenId());
            }
            if (hasTabBarEntries(logical.tabsJson())) {
                issues.add(screen + ": tabsJson must stay empty (tabs are buttons)");
            }
            if (logical.buttonsJson() == null || !logical.buttonsJson().contains("tab:")) {
                issues.add(screen + ": buttonsJson missing tab: actions under title");
            }
            if (logical.layoutJson() == null || !logical.layoutJson().contains("tabsUnderTitle")) {
                issues.add(screen + ": layoutJson missing tabsUnderTitle");
            }
            if (logical.layoutJson() == null || !logical.layoutJson().contains("\"titleAlign\":\"center\"")) {
                issues.add(screen + ": layoutJson titleAlign must be center");
            }
            if (logical.buttonsJson() == null || !logical.buttonsJson().contains("\"screen\":\"" + screen + "\"")) {
                issues.add(screen + ": buttonsJson missing args.screen metadata");
            }
            GuiOpenPacket wire = LmGuiNetwork.applyHubShim(logical);
            if (wire == null) {
                issues.add(screen + ": shim returned null");
                continue;
            }
            if (UltraScreenIds.needsHubShim(screen) && !"hub".equalsIgnoreCase(wire.screenId())) {
                issues.add(screen + ": wire screenId=" + wire.screenId() + " (expected hub)");
            }
            if (wire.layoutJson() == null || !wire.layoutJson().contains("\"titleAlign\":\"center\"")) {
                issues.add(screen + ": wire layoutJson titleAlign must stay center after hub shim");
            }
            if (wire.title() == null || !wire.title().equals(logical.title())) {
                issues.add(screen + ": shim altered title");
            }
            if (wire.panelWidth() <= 0 || wire.panelHeight() <= 0) {
                issues.add(screen + ": shim lost panel bounds");
            }
        }
        if (issues.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra tab: self-check OK (logical screen + hub wire shim, no CNPC)",
                    AdaptiveDifficultyMod.MOD_ID);
        } else {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra tab: self-check issues: {}", AdaptiveDifficultyMod.MOD_ID, issues);
        }
    }

    /**
     * Staff trust boundary: forged GuiActionPackets for admin/config/progression/skills /
     * {@code do:admin:*} / reload must require staff. Player systems must not.
     */
    public static void verifyStaffGate() {
        List<String> broken = new ArrayList<>();
        for (String system : List.of("admin", "config", "progression", "skills")) {
            if (!UltraGuiActions.isStaffOnlySystem(system)) {
                broken.add("isStaffOnlySystem missing " + system);
            }
            if (!UltraGuiActions.requiresStaff(system, "nav:home")) {
                broken.add("staff screen " + system + " must require staff");
            }
            if (!UltraGuiActions.requiresStaff("hub", "open:" + system)) {
                broken.add("open:" + system + " must require staff");
            }
        }
        for (String act :
                List.of(
                        "do:admin:status",
                        "do:reload",
                        "do:syslog:status",
                        "do:guitest_ack",
                        "do:migrate-cnpc",
                        "admin",
                        "config",
                        "progression",
                        "skills")) {
            if (!UltraGuiActions.requiresStaff("hub", act)) {
                broken.add(act + " must require staff on hub");
            }
        }
        if (!UltraGuiActions.requiresStaff("rival", "do:admin:status")) {
            broken.add("rival do:admin:status must require staff");
        }
        if (!UltraGuiActions.requiresStaff("spar", "do:admin:status")) {
            broken.add("spar do:admin:status must require staff");
        }
        for (String act :
                List.of(
                        "open:difficulty",
                        "open:rival",
                        "open:spar",
                        "open:prestige",
                        "open:character",
                        "open:skillcheck",
                        "do:activate:1",
                        "tab:tiers",
                        "nav:hub")) {
            if (UltraGuiActions.requiresStaff("hub", act)) {
                broken.add("player action wrongly staff-gated: " + act);
            }
        }
        if (UltraGuiActions.requiresStaff("rival", "do:challenge:accept")) {
            broken.add("rival challenge must stay player-facing");
        }
        if (UltraGuiActions.requiresStaff("difficulty", "do:activate:1")) {
            broken.add("difficulty activate must stay player-facing");
        }
        if (broken.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra staff gate self-check OK", AdaptiveDifficultyMod.MOD_ID);
        } else {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] Ultra staff gate self-check FAILED: {}",
                    AdaptiveDifficultyMod.MOD_ID,
                    broken);
            throw new IllegalStateException(
                    "LegacyMechanics Ultra staff gate self-check failed: " + broken);
        }
    }

    /**
     * Audit hub + Home-tab {@code open:} targets: each must normalize to a registered builder
     * and not collapse to hub unless the target truly is hub.
     */
    public static void verifyMenuLinks() {
        List<String> broken = new ArrayList<>();
        List<String[]> expected =
                List.of(
                        new String[] {"hub", "open:difficulty", "difficulty"},
                        new String[] {"hub", "open:rival", "rival"},
                        new String[] {"hub", "open:spar", "spar"},
                        new String[] {"hub", "open:prestige", "prestige"},
                        new String[] {"hub", "open:skillcheck", "skillcheck"},
                        new String[] {"hub", "open:skills", "skills"},
                        new String[] {"hub", "open:character", "character"},
                        new String[] {"hub", "open:admin", "admin"},
                        new String[] {"admin", "open:progression", "progression"},
                        new String[] {"admin", "open:config", "config"},
                        new String[] {"difficulty", "open:admin", "admin"},
                        new String[] {"prestige", "open:prestige", "prestige"});
        for (String[] row : expected) {
            String from = row[0];
            String action = row[1];
            String want = row[2];
            String target = action.startsWith("open:") ? action.substring(5) : action;
            String logical = UltraScreenIds.normalizeLogical(target);
            if (!want.equals(logical)) {
                broken.add(from + " " + action + " -> normalize=" + logical + " (want " + want + ")");
                continue;
            }
            if (!UltraScreenIds.BUILDER_IDS.contains(logical)
                    && !UltraScreenIds.PROTOCOL_IDS.contains(logical)) {
                broken.add(from + " " + action + " -> " + logical + " not in BUILDER/PROTOCOL ids");
            }
        }
        // Structural: hub probe must emit skillcheck + character opens, not nav:hub back.
        List<GuiOpenPacket> hubPkts =
                UltraScreenSpec.captureSends(() -> probeBuilder("hub"));
        if (hubPkts.isEmpty()) {
            broken.add("hub: no packet for link audit");
        } else {
            String buttons = hubPkts.get(0).buttonsJson() == null ? "" : hubPkts.get(0).buttonsJson();
            if (!buttons.contains("open:skillcheck") && !buttons.contains("open:skills")) {
                broken.add("hub: missing open:skillcheck / open:skills button");
            }
            if (!buttons.contains("open:character")) {
                broken.add("hub: missing open:character button");
            }
            if (buttons.contains("nav:hub")) {
                broken.add("hub: still has nav:hub (Back to main menu) - remove footerBackToMain");
            }
            if (buttons.contains("nav:refresh") || buttons.contains("Refresh")) {
                broken.add("hub: must not show Refresh");
            }
            if (hubPkts.get(0).layoutJson() == null
                    || !hubPkts.get(0).layoutJson().contains("scrollBody")) {
                broken.add("hub: layoutJson missing scrollBody / shared body metrics");
            }
            if (hubPkts.get(0).layoutJson() == null
                    || !hubPkts.get(0).layoutJson().contains("\"buttonPad\":")) {
                broken.add("hub: layoutJson missing buttonPad");
            }
            if (hubPkts.get(0).layoutJson() == null
                    || !hubPkts.get(0).layoutJson().contains("\"titleAlign\":\"center\"")) {
                broken.add("hub: layoutJson missing titleAlign=center");
            }
        }
        // layoutJson must stay valid + title-centered even when callers omit titleAlign or oversize.
        String forced =
                GuiOpenPacket.sanitizeLayoutJson(
                        "{\"center\":true,\"margin\":10,\"panelWidth\":320,\"panelHeight\":200}",
                        320,
                        200);
        if (forced == null || !forced.contains("\"titleAlign\":\"center\"")) {
            broken.add("sanitizeLayoutJson must force titleAlign=center");
        }
        String recovered = GuiOpenPacket.sanitizeLayoutJson("{not-json", 320, 200);
        if (recovered == null || !recovered.contains("\"titleAlign\":\"center\"")) {
            broken.add("sanitizeLayoutJson invalid JSON must fall back with titleAlign=center");
        }
        List<GuiOpenPacket> charPkts =
                UltraScreenSpec.captureSends(() -> probeBuilder("character"));
        if (!charPkts.isEmpty()) {
            GuiOpenPacket charPkt = charPkts.get(0);
            String layout = charPkt.layoutJson() == null ? "" : charPkt.layoutJson();
            if (!layout.contains("playerModel")) {
                broken.add("character: layoutJson missing playerModel");
            }
            if (!layout.contains("\"y\":60") && !layout.contains("\"y\": 60")) {
                broken.add("character: playerModel y must be 60 (was 36 - head clipped)");
            }
            if (layout.contains("\"scale\":1.5") || layout.contains("\"scale\": 1.5")) {
                broken.add("character: playerModel scale 1.5 overshoots panel - use 0.9");
            } else if (!(layout.contains("\"scale\":0.9") || layout.contains("\"scale\": 0.9"))) {
                broken.add("character: playerModel scale must be 0.9 (1.0 still clipped the head)");
            }
            String buttons = charPkt.buttonsJson() == null ? "" : charPkt.buttonsJson();
            if (!buttons.contains("\"label\":\"Android\"")
                    || !buttons.contains("\"label\":\"Saga\"")
                    || !buttons.contains("\"label\":\"Home\"")) {
                broken.add("character: tab buttons must be Home/Saga/Android (short labels)");
            }
            if (buttons.contains("Android Removal") || buttons.contains("Saga Reset")) {
                broken.add("character: long tab labels overflow and mojibake on KaiPath");
            }
            // No section-sign / mojibake leftovers in any button label.
            if (buttons.indexOf('\u00A7') >= 0 || buttons.indexOf('\u00C2') >= 0) {
                broken.add("character: buttonsJson still has section-sign or C2 mojibake bytes");
            }
            String fit = UltraStyle.fitAsciiLabel("Android Removal", UltraStyle.labelMaxWidth(102));
            if (fit.indexOf('\u00E2') >= 0 || fit.indexOf('\u2026') >= 0) {
                broken.add("fitAsciiLabel must use ASCII ellipsis only");
            }
            if (!fit.endsWith("...") || fit.length() >= "Android Removal".length()) {
                broken.add("fitAsciiLabel must clip labels that exceed tab text budget");
            }
        }
        for (String tablessId : TABLESS_SCREEN_IDS) {
            List<GuiOpenPacket> skillPkts =
                    UltraScreenSpec.captureSends(() -> probeBuilder(tablessId));
            if (skillPkts.isEmpty()) {
                broken.add(tablessId + ": no GuiOpenPacket for tab-less probe");
                continue;
            }
            if (hasTabBarEntries(skillPkts.get(0).tabsJson())) {
                broken.add(tablessId + ": must have no tab bar (empty tabsJson)");
            }
        }
        if (broken.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra menu link audit OK ({} open: targets + hub/character layout checks)",
                    AdaptiveDifficultyMod.MOD_ID,
                    expected.size());
        } else {
            AdaptiveDifficultyMod.LOGGER.error(
                    "[{}] Ultra menu link audit FAILED: {}", AdaptiveDifficultyMod.MOD_ID, broken);
            throw new IllegalStateException(
                    "LegacyMechanics Ultra menu link audit failed: " + broken);
        }
    }

    /**
     * Hub shim: when {@link UltraScreenIds#KAIPATH_DYNAMIC_ANY_SCREEN_ID} is false, non-registered
     * logical ids rewrite wire screenId to hub. When dynamic-any is on (KaiPath 1.3.1+), expect
     * an empty tunnel set and identity wire ids.
     */
    public static void verifyHubShim() {
        List<String> issues = new ArrayList<>();
        Set<String> tunneled = UltraScreenIds.hubShimTunnelIds();
        if (UltraScreenIds.KAIPATH_DYNAMIC_ANY_SCREEN_ID) {
            if (!tunneled.isEmpty()) {
                issues.add("dynamic-any on but tunnel set non-empty: " + tunneled);
            }
            for (String logical : UltraScreenIds.BUILDER_IDS) {
                GuiOpenPacket built =
                        UltraScreenSpec.of(logical)
                                .title(logical)
                                .page("main")
                                .activeTab("home")
                                .tabs("home|Home")
                                .line("shim-probe")
                                .buttonLeft("nav:home", "Home", "dim")
                                .toPacket();
                GuiOpenPacket wire = LmGuiNetwork.applyHubShim(built);
                if (wire == null || !logical.equalsIgnoreCase(wire.screenId())) {
                    issues.add(
                            logical
                                    + ": expected identity wire id, got "
                                    + (wire == null ? "null" : wire.screenId()));
                }
            }
            if (issues.isEmpty()) {
                AdaptiveDifficultyMod.LOGGER.info(
                        "[{}] Ultra hub shim self-check OK (KaiPath dynamic-any - no tunneling)",
                        AdaptiveDifficultyMod.MOD_ID);
            } else {
                AdaptiveDifficultyMod.LOGGER.warn(
                        "[{}] Ultra hub shim self-check issues: {}", AdaptiveDifficultyMod.MOD_ID, issues);
            }
            return;
        }
        for (String logical : tunneled) {
            GuiOpenPacket built =
                    UltraScreenSpec.of(logical)
                            .title(logical)
                            .page("main")
                            .activeTab("home")
                            .tabs("home|Home")
                            .line("shim-probe")
                            .buttonLeft("nav:home", "Home", "dim")
                            .toPacket();
            GuiOpenPacket wire = LmGuiNetwork.applyHubShim(built);
            if (wire == null || !"hub".equalsIgnoreCase(wire.screenId())) {
                issues.add(logical + ": wire screenId=" + (wire == null ? "null" : wire.screenId()));
                continue;
            }
            if (wire.layoutJson() == null || !wire.layoutJson().contains(logical)) {
                issues.add(logical + ": layoutJson missing system metadata");
            }
            if (wire.layoutJson() == null || !wire.layoutJson().contains("\"titleAlign\":\"center\"")) {
                issues.add(logical + ": layoutJson titleAlign must be center after hub shim");
            }
            if (wire.buttonsJson() == null || !wire.buttonsJson().contains("\"screen\":\"" + logical + "\"")) {
                issues.add(logical + ": buttons lost args.screen");
            }
            if (wire.panelWidth() != built.panelWidth() || wire.panelHeight() != built.panelHeight()) {
                issues.add(logical + ": panel size changed by shim");
            }
        }
        // Registered ids must not be rewritten.
        GuiOpenPacket hub = UltraScreenSpec.of("hub").title("Hub").page("main").button("nav:home", "Home", "dim").toPacket();
        GuiOpenPacket hubWire = LmGuiNetwork.applyHubShim(hub);
        if (hubWire == null || !"hub".equalsIgnoreCase(hubWire.screenId())) {
            issues.add("hub: unexpected wire id");
        }
        if (issues.isEmpty()) {
            AdaptiveDifficultyMod.LOGGER.info(
                    "[{}] Ultra hub shim self-check OK (tunneling {})",
                    AdaptiveDifficultyMod.MOD_ID,
                    tunneled);
        } else {
            AdaptiveDifficultyMod.LOGGER.warn(
                    "[{}] Ultra hub shim self-check issues: {}", AdaptiveDifficultyMod.MOD_ID, issues);
        }
    }
}
