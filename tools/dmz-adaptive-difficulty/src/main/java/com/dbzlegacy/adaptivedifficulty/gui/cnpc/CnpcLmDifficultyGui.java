package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.LmOverhaulScaledCombat;
import com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.progression.end.EndDimensionStrength;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi;
import com.dbzlegacy.adaptivedifficulty.gui.MechanicsGuiApi;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import noppes.npcs.api.gui.ICustomGui;
import noppes.npcs.api.gui.IScroll;

public final class CnpcLmDifficultyGui {
    private static final int H = 320;

    private CnpcLmDifficultyGui() {}

    public static void open(ServerPlayer player, String page) {
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        if (p.startsWith("title_detail:")) {
            String titleId = raw.substring("title_detail:".length()).trim();
            CnpcGuiSupport.showSized(player, CnpcLmGui.ID_DIFFICULTY, CnpcGuiSupport.W, 320, (pl, gui) ->
                    paintTitleDetail(pl, gui, titleId));
            return;
        }
        if (("stats".equals(p) || "statistics".equals(p) || "details".equals(p)) && !StaffAccess.isStaff(player)) {
            p = "main";
        }
        String pageFinal = p;
        int height = switch (pageFinal) {
            case "stats", "statistics", "details" -> 340;
            case "titles", "title" -> 360;
            case "end_dragon", "dragon", "summon_dragon" -> 340;
            case "admin" -> 300;
            default -> H;
        };
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_DIFFICULTY, CnpcGuiSupport.W, height, (pl, gui) -> {
            switch (pageFinal) {
                case "tiers", "buy", "tier", "purchase", "unlock", "adjust", "change", "set", "lower" ->
                        paintTiers(pl, gui);
                case "titles", "title" -> paintTitles(pl, gui);
                case "team", "teams" -> paintTeam(pl, gui);
                case "end_dragon", "dragon", "summon_dragon" -> paintEndDragon(pl, gui);
                case "admin" -> paintStaffAdmin(pl, gui);
                case "stats", "statistics", "details" -> paintStats(pl, gui);
                case "settings" -> paintSettings(pl, gui);
                default -> paintMain(pl, gui);
            }
        });
    }

    private static ServerPlayer who(ServerPlayer viewer) {
        return CnpcGuiSupport.target(viewer);
    }

    private static void paintMain(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        DifficultySnapshot snap = DifficultyCache.refresh(subject);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§aDifficulty",
                "§7Tougher mobs, tiers, and optional titles");

        List<String> lines = new ArrayList<>();
        if (!DifficultyConfig.isEnabled()) {
            lines.add("§cDifficulty is turned off on this server.");
        } else if (!SystemGate.allows(subject)) {
            lines.add("§eThis account cannot use personal difficulty.");
        } else {
            PlayerDifficultyData data = DifficultyCache.data(subject);
            if (!data.isPersonalEnabled()) {
                lines.add("§eYour personal difficulty is off.");
            } else {
                lines.add("§7Active tier §f" + snap.activeTierName + " §8· §7Unlocked up to §fT"
                        + snap.highestUnlockedTier);
            }
            lines.add("§6Ancient Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));
            String title = TitleSystem.activeDisplay(subject);
            if (title != null && !"None".equals(title)) {
                lines.add("§7Equipped title §e" + title);
            }
            lines.add("§8Scaled mobs can hurt other players nearby.");
        }
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 20, "§eTiers", CnpcGuiSupport.COL_L, row, () -> open(player, "tiers"));
        CnpcGuiSupport.button(gui, 21, "§dTitles", CnpcGuiSupport.COL_R, row, () -> open(player, "titles"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 22, "§bRival Teams", CnpcGuiSupport.COL_L, row, () -> open(player, "team"));
        PlayerDifficultyData personalData = DifficultyCache.data(subject);
        boolean personalOn = personalData != null && personalData.isPersonalEnabled();
        CnpcGuiSupport.button(gui, 23,
                personalOn ? CnpcGuiStyle.toggleOn("Personal difficulty") : CnpcGuiStyle.toggleOff("Personal difficulty"),
                CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArgNoReopen(subject, "toggle_personal", "0", "main").message(),
                () -> open(player, "main")));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 24, "§aEnd Dragon", CnpcGuiSupport.COL_L, row,
                () -> open(player, "end_dragon"));
        CnpcGuiSupport.button(gui, 25, "§7Settings", CnpcGuiSupport.COL_R, row,
                () -> open(player, "settings"));
        row += CnpcGuiSupport.ROW_STEP;
        navFooter(player, gui, row, null, subject);
    }

    private static void paintSettings(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        PlayerDifficultyData d = DifficultyCache.data(subject);
        boolean coinChat = d != null && d.isCoinDropChat();
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§a", "Difficulty", "Settings"),
                "§7Messages and staff readouts");
        int row = CnpcGuiSupport.bodyBelowHeader(infoY);
        CnpcGuiSupport.button(gui, 20, coinChat ? CnpcGuiStyle.toggleOn("Coin messages")
                : CnpcGuiStyle.toggleOff("Coin messages"), CnpcGuiSupport.COL_L, row,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> DifficultyActions.handleArgNoReopen(subject, "toggle_coin_chat", "0", "settings").message(),
                        () -> open(player, "settings")));
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.button(gui, 21, "§8Staff details", CnpcGuiSupport.COL_R, row,
                    () -> open(player, "stats"));
        }
        row += CnpcGuiSupport.ROW_STEP;
        navFooter(player, gui, row, "main", subject);
    }

    private static void paintStats(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        DifficultySnapshot snap = DifficultyCache.refresh(subject);
        PlayerCombatProfile profile = PlayerCombatProfile.of(subject);
        Map<String, String> ph = new HashMap<>();
        LmOverhaulScaledCombat.putPlaceholders(ph, subject);

        long cr = snap.combatRating > 0 ? snap.combatRating : snap.calculated;
        List<String> lines = new ArrayList<>();
        lines.add("§7Tier §f" + snap.activeTierName + CnpcGuiStyle.SEP + "§7State §" + snap.stateColorCode()
                + snap.state());
        lines.add("§7Combat Rating §f" + cr + CnpcGuiStyle.SEP + "§7Unlocked §fT" + snap.highestUnlockedTier);
        lines.add("§7Level §f" + snap.dmzLevel + CnpcGuiStyle.SEP + "§7Prestige §f" + snap.prestige);
        lines.add("§7Title §e" + blankNone(TitleSystem.activeDisplay(subject)));
        lines.add("§7Overhaul §f" + ph.getOrDefault("overhaul_scale", "x1") + CnpcGuiStyle.SEP + "§7Melee §f"
                + ph.getOrDefault("melee_scaled", "?") + CnpcGuiStyle.SEP + "§7Strike §f"
                + ph.getOrDefault("strike_scaled", "?"));
        lines.add("§7Ki §f" + ph.getOrDefault("ki_scaled", "?")
                + "  §7Defense §f" + ph.getOrDefault("defense_scaled", "?"));
        String fightingClass = profile.fightingClass == null ? "" : profile.fightingClass;
        lines.add("§7Class §f" + blankNone(fightingClass)
                + "  §7Style §f" + (profile.style == null ? "HYBRID" : profile.style.name()));
        lines.add("§7Top stats §f" + blankNone(profile.topStatsLabel()));
        lines.add("§6Ancient Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));

        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§8Staff details",
                "§7Combat rating and scaled stats (read-only)");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
        row += 8;
        navFooter(player, gui, row, "settings", subject);
    }

    private static String blankNone(String s) {
        if (s == null || s.isBlank() || "None".equalsIgnoreCase(s.trim())) {
            return "—";
        }
        return s;
    }

    private static void paintTiers(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        if (!DifficultyConfig.isEnabled()) {
            paintFeatureLocked(player, gui, "Tiers locked",
                    "§7Enable personal difficulty before changing tiers",
                    List.of(
                    "§cAdaptive Difficulty is off on this server.",
                    "§7Ask staff to enable it before using tiers."));
            return;
        }
        if (!SystemGate.allows(subject)) {
            paintFeatureLocked(player, gui, "Tiers locked",
                    "§7Enable personal difficulty before changing tiers",
                    List.of(
                    "§eYou cannot use personal difficulty yet.",
                    "§7Ask staff to add you to the whitelist."));
            return;
        }
        PlayerDifficultyData gateData = DifficultyCache.data(subject);
        if (gateData == null || !gateData.isPersonalEnabled()) {
            paintFeatureLocked(player, gui, "Tiers locked",
                    "§7Enable personal difficulty before changing tiers",
                    List.of(
                    "§cTurn personal difficulty ON first.",
                    "§7Use §ePersonal difficulty §7on the main menu, then return here to pick a tier."));
            return;
        }
        UnlockSystem.syncUnlocks(subject, DifficultyCache.data(subject));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§a", "Difficulty", "Tiers"),
                "§7Costs show on each button · click again within 10 seconds to buy");

        DifficultySnapshot snap = DifficultyCache.refresh(subject);
        PlayerDifficultyData data = DifficultyCache.data(subject);
        int max = Math.max(0, snap.highestUnlockedTier);
        int active = data.getActiveTier();
        List<String> lines = new ArrayList<>();
        if (active <= 0) {
            lines.add("§7Active tier §fNone");
        } else {
            lines.add("§7Active tier §f" + snap.activeTierName);
        }
        lines.add("§7Unlocked up to §fT" + max);
        lines.add("§6Ancient Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));
        if (!PaidFeatureAccess.bypassAncientCoinCost(subject)) {
            lines.add("§8Activation cost scales with your level");
        }
        UnlockTier nextLocked = UnlockTier.byId(max + 1);
        if (nextLocked != null) {
            lines.add("§7Next tier §fT" + nextLocked.id + CnpcGuiStyle.SEP
                    + CnpcDifficultyTierUi.humanRequirement(nextLocked));
            lines.add("§7Cost to activate §f"
                    + CnpcDifficultyTierUi.formatActivationCost(subject, nextLocked));
        }
        int row = CnpcGuiSupport.bodyBelowInfo(
                CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));

        List<CnpcGuiLayout.GridButton> grid = new ArrayList<>();
        for (int t = 1; t <= 7; t++) {
            UnlockTier ut = UnlockTier.byId(t);
            if (ut == null) {
                continue;
            }
            boolean unlocked = data.hasUnlockedTier(t) || t <= max;
            boolean eligible = CnpcDifficultyTierUi.isEligible(subject, ut);
            String label = CnpcDifficultyTierUi.tierActionLabel(subject, t, active, unlocked, eligible);
            int tier = t;
            if (CnpcDifficultyTierUi.tierButtonEnabled(t, active, unlocked, eligible)) {
                grid.add(CnpcGuiLayout.GridButton.action(
                        label,
                        () -> DifficultyActions.handleArgNoReopen(subject, "activate", String.valueOf(tier), "tiers")
                                .message(),
                        () -> open(player, "tiers")));
            } else if (!unlocked) {
                grid.add(CnpcGuiLayout.GridButton.disabled(
                        "§eT" + t + " · " + CnpcDifficultyTierUi.humanRequirement(ut)));
            } else {
                grid.add(CnpcGuiLayout.GridButton.disabled(label));
            }
        }
        grid.add(CnpcGuiLayout.GridButton.action(
                "§cClear active tier",
                () -> DifficultyActions.handleArgNoReopen(subject, "lower_tier", "0", "tiers").message(),
                () -> open(player, "tiers")));
        row = CnpcGuiLayout.paintTwoColumnButtonGrid(
                player, gui, row, CnpcGuiSupport.ID_GRID_BASE,
                grid.toArray(CnpcGuiLayout.GridButton[]::new),
                () -> open(player, "tiers"));
        navFooter(player, gui, row, "main", subject);
    }

    /**
     * Tiers / team scaling when AD is off, whitelist blocks, or personal difficulty is OFF —
     * no mode buttons (avoids CNPC errors).
     */
    private static void paintFeatureLocked(
            ServerPlayer player, ICustomGui gui, String lockedTitle, String headerHint, List<String> body) {
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§c", "Difficulty", lockedTitle),
                headerHint);
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, body, CnpcGuiStyle.INFO_INLINE_MAX));
        ServerPlayer subject = who(player);
        if (DifficultyConfig.isEnabled() && SystemGate.allows(subject)) {
            CnpcGuiSupport.button(gui, 40, "§aTurn personal ON", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> DifficultyActions.handleArgNoReopen(subject, "toggle_personal", "0", "main").message(),
                    () -> open(player, "main")));
            row += CnpcGuiSupport.ROW_STEP;
        }
        navFooter(player, gui, row, "main", subject);
    }

    private static void paintTitles(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§a", "Difficulty", "Titles"),
                CnpcGuiStyle.HINT_TITLE_EQUIP);
        List<String> header = new ArrayList<>();
        header.add("§7Equipped §e" + blankNone(TitleSystem.activeDisplay(subject)));
        header.add("§7Title score §6" + TitleSystem.computeTitleScore(subject));
        int listY = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBeforePickList(gui, infoY, header,
                CnpcGuiStyle.INFO_LIST_HEADER_MAX));

        List<String> cards = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        String active = TitleSystem.activeId(subject);
        for (DifficultyTitle t : DifficultyTitle.values()) {
            boolean earned = TitleSystem.has(subject, t);
            String label = (earned ? "§a" : "§8") + t.display
                    + (t.id.equals(active) ? " §e(equipped)" : "");
            labels.add(label);
            cards.add(t.id + "\t" + label);
        }
        int rowsBelow = 2;
        int bandY = CnpcGuiSupport.pickListBandY(listY, rowsBelow, gui, labels.size());
        int scrollH = CnpcGuiSupport.listScrollHeight(gui, bandY, rowsBelow);
        IScroll scroll = CnpcGuiSupport.scrollPickList(gui, listY, rowsBelow, labels.toArray(String[]::new));
        CnpcGuiSupport.wireScrollOpenDetail(scroll, cards, 0, id -> open(player, "title_detail:" + id));
        CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0, id -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArgNoReopen(subject, "equip_title", id, "titles").message(),
                () -> open(player, "titles")));

        int row = CnpcGuiSupport.navRowAfterScroll(bandY, scrollH);
        PlayerDifficultyData data = DifficultyCache.data(subject);
        boolean sense = data.titleProgress().titleSenseChat();
        CnpcGuiSupport.buttonSmall(gui, 20, sense ? CnpcGuiStyle.toggleOn("Title Sense")
                : CnpcGuiStyle.toggleOff("Title Sense"), CnpcGuiSupport.COL_L, row, 95,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> DifficultyActions.handleArgNoReopen(subject, "toggle_title_sense", "0", "titles").message(),
                        () -> open(player, "titles")));
        CnpcGuiSupport.buttonSmall(gui, 21, "§cUnequip title", CnpcGuiSupport.COL_R, row, 95, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArgNoReopen(subject, "clear_title", "0", "titles").message(),
                () -> open(player, "titles")));
        row += CnpcGuiSupport.ROW_STEP;
        navFooter(player, gui, row, "main", subject);
    }

    private static void paintTitleDetail(ServerPlayer player, ICustomGui gui, String titleIdRaw) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        DifficultyTitle title = DifficultyTitle.byId(titleIdRaw);
        if (title == null) {
            int infoY = CnpcGuiSupport.paintHeader(player, gui,
                    CnpcGuiStyle.subPage("§d", "Difficulty", "Title"), "§7Unknown entry");
            int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY,
                    List.of("§7That title could not be found."), CnpcGuiStyle.INFO_INLINE_MAX));
            navFooter(player, gui, row + 8, "titles", subject);
            return;
        }
        boolean earned = TitleSystem.has(subject, title);
        boolean equipped = title.id.equals(TitleSystem.activeId(subject));
        int mastery = DifficultyCache.data(subject).titleProgress().masteryLevel(title.id);

        int infoY = CnpcGuiSupport.paintHeader(player, gui,
                CnpcGuiStyle.subPage("§d", "Difficulty", title.masteryDisplay(mastery)),
                earned ? "§7Unlocked · tap Equip to wear" : "§7Locked · see how to earn it");

        List<String> lines = new ArrayList<>();
        lines.add(title.rarity.coloredLabel() + " §8· §7" + kindLabel(title.kind));
        lines.add(earned ? "§aYou have this title." : "§8You do not have this yet.");
        if (equipped) {
            lines.add("§eCurrently equipped.");
        }
        lines.add("§7How to earn §f" + humanRequirement(title));
        String perk = title.perkTip(mastery).replace(" §8| ", " · ");
        lines.add("§7Bonus when worn §f" + perk);
        lines.add("§7Title score §6+" + title.scorePoints);

        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, CnpcGuiStyle.INFO_INLINE_MAX));
        if (earned) {
            CnpcGuiSupport.button(gui, 20, equipped ? "§aAlready equipped" : "§aEquip this title",
                    CnpcGuiSupport.COL_L, row, () -> {
                        if (!equipped) {
                            CnpcGuiSupport.act(
                                    player,
                                    () -> DifficultyActions.handleArgNoReopen(subject, "equip_title", title.id, "titles")
                                            .message(),
                                    () -> open(player, "title_detail:" + title.id));
                        } else {
                            open(player, "title_detail:" + title.id);
                        }
                    });
        } else {
            gui.addLabel(CnpcGuiSupport.ID_INLINE_NOTE,
                    CnpcGuiSupport.safeChat(CnpcGuiStyle.readableInfoLine(
                            "§eEquip unlocks after you earn the title.")),
                    CnpcGuiSupport.COL_L, row + 4, CnpcGuiSupport.BTN_W, 14);
        }
        row += CnpcGuiSupport.ROW_STEP;
        navFooter(player, gui, row, "titles", subject);
    }

    private static String kindLabel(DifficultyTitle.Kind kind) {
        return switch (kind) {
            case TIER -> "Tier milestone";
            case COMBAT -> "Combat challenge";
            case CHALLENGE -> "Special challenge";
        };
    }

    private static String humanRequirement(DifficultyTitle title) {
        String raw = title.requirementTip();
        return raw.replace("DMZ ", "Level ")
                .replace(" or Prestige ", " or prestige ")
                .replace(" §8(keeps after lowering tier)", " (stays unlocked if you lower tier)");
    }

    private static void paintTeam(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        if (!DifficultyConfig.isEnabled()) {
            paintFeatureLocked(player, gui, "Teams locked",
                    "§7Enable personal difficulty before changing team modes",
                    List.of(
                            "§cAdaptive Difficulty is off on this server.",
                            "§7Ask staff to enable it before using team scaling."));
            return;
        }
        if (!SystemGate.allows(subject)) {
            paintFeatureLocked(player, gui, "Teams locked",
                    "§7Enable personal difficulty before changing team modes",
                    List.of(
                            "§eYou cannot use personal difficulty yet.",
                            "§7Ask staff to add you to the whitelist."));
            return;
        }
        PlayerDifficultyData teamGate = DifficultyCache.data(subject);
        if (teamGate == null || !teamGate.isPersonalEnabled()) {
            paintFeatureLocked(player, gui, "Teams locked",
                    "§7Enable personal difficulty before changing team modes",
                    List.of(
                            "§cTurn personal difficulty ON first.",
                            "§7Use §ePersonal difficulty §7on the main menu, then return here to pick a team mode."));
            return;
        }
        TeamMode mode = teamGate.getTeamMode();
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§a", "Difficulty", "Teams"),
                "§7How rival teams affect scaling");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, DifficultyTeamGuiApi.linesForPage(subject, "team"),
                        CnpcGuiStyle.INFO_INLINE_MAX));
        CnpcGuiSupport.button(gui, 40,
                mode == TeamMode.PERSONAL_ONLY ? CnpcGuiStyle.toggleOn("Personal") : "§7Personal",
                CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "personal", "team"),
                () -> open(player, "team")));
        CnpcGuiSupport.button(gui, 41,
                mode == TeamMode.THRESHOLD_BONUS_ONLY ? CnpcGuiStyle.toggleOn("Threshold") : "§7Threshold",
                CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "threshold", "team"),
                () -> open(player, "team")));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 42,
                mode == TeamMode.FULL_TEAM_SCALING ? CnpcGuiStyle.toggleOn("Full team") : "§7Full team",
                CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "full", "team"),
                () -> open(player, "team")));
        row += CnpcGuiSupport.ROW_STEP;
        navFooter(player, gui, row, "main", subject);
    }

    private static void paintStaffAdmin(ServerPlayer player, ICustomGui gui) {
        if (!StaffAccess.isStaff(player)) {
            CnpcGuiSupport.pushMenuMessage(player, "§cStaff only.");
            open(player, "main");
            return;
        }
        DifficultyActions.prepareGui(who(player));
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§c", "Difficulty", "Staff Admin"),
                "§7Server tools · typed admin: §f/difficulty admin");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY, List.of(
                "§7Reload config, event log, and LM Staff Admin hub",
                "§8Whitelist · tier costs · coin rates: §f/difficulty admin …"
        ), 3));
        row += 8;
        CnpcGuiSupport.button(gui, 20, "§aReload LM config", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> MechanicsGuiApi.handleDo(player, "reload", "", "admin"),
                () -> open(player, "admin")));
        CnpcGuiSupport.button(gui, 21, "§8Staff details", CnpcGuiSupport.COL_R, row, () -> open(player, "stats"));
        row += CnpcGuiSupport.ROW_STEP;
        CnpcGuiSupport.button(gui, 22, "§cLM Staff Admin", CnpcGuiSupport.COL_L, row,
                () -> CnpcLmAdminGui.open(player, "main"));
        CnpcGuiSupport.button(gui, 23, "§8Event log", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmLogsGui.open(player, "main"));
        row += CnpcGuiSupport.ROW_STEP;
        navFooter(player, gui, row, "main", who(player));
    }

    private static void paintEndDragon(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        boolean canSummon = EndDimensionStrength.canOpenSummonMenu(subject);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, CnpcGuiStyle.subPage("§a", "Difficulty", "End Dragon"),
                "§7Paid summon · AD boss profile · summoner-only damage");
        int row = CnpcGuiSupport.bodyBelowInfo(CnpcGuiSupport.paintInfoBlock(gui, infoY,
                EndDimensionStrength.summonMenuLines(subject), CnpcGuiStyle.INFO_INLINE_MAX));
        row += 8;
        if (canSummon) {
            CnpcGuiSupport.button(gui, 20, "§aConfirm summon", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                    player,
                    () -> DifficultyActions.handleArgNoReopen(subject, "summon_end_dragon", "0", "end_dragon").message(),
                    () -> open(player, "end_dragon")));
        } else {
            gui.addLabel(CnpcGuiSupport.ID_INLINE_NOTE,
                    CnpcGuiSupport.safeChat(CnpcGuiStyle.readableInfoLine(
                            DifficultyConfig.get().enableEndPlayerDragonSummon
                                    ? "§eFix requirements above to summon."
                                    : "§7Ask staff to clear the dragon.")),
                    CnpcGuiSupport.COL_L, row + 4, CnpcGuiSupport.BTN_W, 14);
            row += 8;
        }
        row += CnpcGuiSupport.ROW_STEP;
        navFooter(player, gui, row, "main", subject);
    }

    /** {@code parentPage} null on difficulty main; otherwise Back reopens that page. */
    private static void navFooter(
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
        if (StaffAccess.isStaff(player) && parentPage == null) {
            row += CnpcGuiSupport.ROW_STEP;
            CnpcGuiSupport.buttonSmall(gui, CnpcGuiSupport.ID_STAFF_EXTRA, "§cStaff Admin…",
                    CnpcGuiSupport.COL_L, row, 95, () -> open(player, "admin"));
        }
        if (previewSubject != null && parentPage == null) {
            CnpcGuiSupport.paintSystemMainPreview(previewSubject, gui, player);
        }
    }
}
