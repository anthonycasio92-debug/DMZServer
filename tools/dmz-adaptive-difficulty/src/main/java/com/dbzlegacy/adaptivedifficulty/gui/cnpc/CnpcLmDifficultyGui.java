package com.dbzlegacy.adaptivedifficulty.gui.cnpc;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.LmOverhaulScaledCombat;
import com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
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
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        if (("stats".equals(p) || "statistics".equals(p) || "details".equals(p)) && !StaffAccess.isStaff(player)) {
            p = "main";
        }
        String pageFinal = p;
        int height = switch (pageFinal) {
            case "stats", "statistics", "details" -> 340;
            case "titles", "title" -> 360;
            default -> H;
        };
        CnpcGuiSupport.showSized(player, CnpcLmGui.ID_DIFFICULTY, CnpcGuiSupport.W, height, (pl, gui) -> {
            switch (pageFinal) {
                case "tiers", "buy", "tier", "purchase", "unlock", "adjust", "change", "set", "lower" ->
                        paintTiers(pl, gui);
                case "titles", "title" -> paintTitles(pl, gui);
                case "team", "teams" -> paintTeam(pl, gui);
                case "stats", "statistics", "details" -> paintStats(pl, gui);
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
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§aDifficulty", "§7Adaptive scaling & unlock tiers");

        List<String> lines = new ArrayList<>();
        if (!DifficultyConfig.isEnabled()) {
            lines.add("§cSystem disabled.");
        } else if (!SystemGate.allows(subject)) {
            lines.add("§eNot available on this account.");
        } else {
            PlayerDifficultyData data = DifficultyCache.data(subject);
            if (!data.isPersonalEnabled()) {
                lines.add("§cPersonal difficulty OFF");
            } else {
                lines.add("§7Tier §f" + snap.activeTierName + "  §8·  §7T" + snap.highestUnlockedTier);
            }
            lines.add("§6Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));
            String title = TitleSystem.activeDisplay(subject);
            if (title != null && !"None".equals(title)) {
                lines.add("§7Title §e" + title);
            }
            lines.add("§8Scaled mobs may hit other players.");
        }
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 4);
        CnpcGuiSupport.button(gui, 20, "§eUnlock tiers", CnpcGuiSupport.COL_L, row, () -> open(player, "tiers"));
        CnpcGuiSupport.button(gui, 21, "§dTitles", CnpcGuiSupport.COL_R, row, () -> open(player, "titles"));
        row += 24;
        CnpcGuiSupport.button(gui, 22, "§bTeam scaling", CnpcGuiSupport.COL_L, row, () -> open(player, "team"));
        CnpcGuiSupport.button(gui, 23, "§7Toggle personal", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "toggle_personal", "0", "main").message(),
                () -> open(player, "main")));
        row += 24;
        CnpcGuiSupport.button(gui, 24, "§5Summon end dragon", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "summon_end_dragon", "0", "main").message(),
                () -> open(player, "main")));
        CnpcGuiSupport.button(gui, 25, "§6Rival system", CnpcGuiSupport.COL_R, row,
                () -> CnpcLmGui.open(player, "rival", "main"));
        row += 24;
        PlayerDifficultyData d = DifficultyCache.data(subject);
        boolean coinChat = d != null && d.isCoinDropChat();
        CnpcGuiSupport.button(gui, 26, coinChat ? "§aCoin chat ON" : "§8Coin chat OFF", CnpcGuiSupport.COL_L, row,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> DifficultyActions.handleArg(subject, "toggle_coin_chat", "0", "main").message(),
                        () -> open(player, "main")));
        if (StaffAccess.isStaff(player)) {
            CnpcGuiSupport.button(gui, 27, "§8Staff details", CnpcGuiSupport.COL_R, row,
                    () -> open(player, "stats"));
        }
        row += 24;
        navFooter(player, gui, row);
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
        lines.add("§7Tier §f" + snap.activeTierName + "  §8·  §7State §" + snap.stateColorCode() + snap.state());
        lines.add("§7Combat CR §f" + cr + "  §8·  §7Unlocked §fT" + snap.highestUnlockedTier);
        lines.add("§7DMZ §f" + snap.dmzLevel + "  §7Prestige §f" + snap.prestige);
        lines.add("§7Title §e" + blankNone(TitleSystem.activeDisplay(subject)));
        lines.add("§7Overhaul §f" + ph.getOrDefault("overhaul_scale", "x1")
                + "  §8·  §7Melee §f" + ph.getOrDefault("melee_scaled", "?")
                + "  §7Strike §f" + ph.getOrDefault("strike_scaled", "?"));
        lines.add("§7Ki §f" + ph.getOrDefault("ki_scaled", "?")
                + "  §7Defense §f" + ph.getOrDefault("defense_scaled", "?"));
        String fightingClass = profile.fightingClass == null ? "" : profile.fightingClass;
        lines.add("§7Class §f" + blankNone(fightingClass)
                + "  §7Style §f" + (profile.style == null ? "HYBRID" : profile.style.name()));
        lines.add("§7Top stats §f" + blankNone(profile.topStatsLabel()));
        lines.add("§6Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));

        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§8Staff · Details",
                "§7CR · counters · scaled kit (read-only)");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 8);
        row += 8;
        navFooter(player, gui, row);
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
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§eDifficulty · Tiers",
                "§7Activate unlocked tiers · step down free");

        DifficultySnapshot snap = DifficultyCache.refresh(subject);
        PlayerDifficultyData data = DifficultyCache.data(subject);
        int max = Math.max(0, snap.highestUnlockedTier);
        List<String> lines = new ArrayList<>();
        lines.add("§7Active §fT" + data.getActiveTier() + "  §8·  §7Max unlocked §fT" + max);
        lines.add("§6Coins §f" + AncientCoinEconomy.inventoryBreakdown(subject));
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, lines, 3);
        for (int t = 0; t <= Math.min(7, max); t++) {
            int tier = t;
            int col = (t % 2 == 0) ? CnpcGuiSupport.COL_L : CnpcGuiSupport.COL_R;
            if (t % 2 == 0 && t > 0) {
                row += 24;
            }
            CnpcGuiSupport.buttonSmall(gui, 30 + t, "§fActivate T" + tier, col, row, 95, () -> CnpcGuiSupport.act(
                    player,
                    () -> DifficultyActions.handleArg(subject, "activate", String.valueOf(tier), "tiers").message(),
                    () -> open(player, "tiers")));
        }
        row += 36;
        CnpcGuiSupport.button(gui, 50, "§cClear active tier", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "lower_tier", "0", "tiers").message(),
                () -> open(player, "tiers")));
        for (UnlockTier ut : UnlockTier.values()) {
            if (ut.id > max && ut.id <= 7) {
                int buy = ut.id;
                CnpcGuiSupport.buttonSmall(gui, 60 + buy, "§aBuy T" + buy, CnpcGuiSupport.COL_R, row, 95,
                        () -> CnpcGuiSupport.act(
                                player,
                                () -> DifficultyActions.handleArg(subject, "activate", String.valueOf(buy), "tiers")
                                        .message(),
                                () -> open(player, "tiers")));
                break;
            }
        }
        row += 28;
        navFooter(player, gui, row);
    }

    private static void paintTitles(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        DifficultyActions.prepareGui(subject);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§dDifficulty · Titles",
                "§7Double-click unlocked title to equip");
        List<String> header = new ArrayList<>();
        header.add("§7Equipped §e" + TitleSystem.activeDisplay(subject));
        header.add("§7Score §6" + TitleSystem.computeTitleScore(subject));
        int listY = CnpcGuiSupport.paintInfoBlock(gui, infoY, header, 2);

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
        IScroll scroll = CnpcGuiSupport.scrollSearchable(gui, CnpcGuiSupport.ID_LIST_SCROLL, CnpcGuiSupport.M, listY,
                400, 120, labels.toArray(String[]::new));
        CnpcGuiSupport.wireScrollDoublePick(scroll, cards, 0, id -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "equip_title", id, "titles").message(),
                () -> open(player, "titles")));

        int row = listY + 128;
        PlayerDifficultyData data = DifficultyCache.data(subject);
        boolean sense = data.titleProgress().titleSenseChat();
        CnpcGuiSupport.buttonSmall(gui, 20, sense ? "§aSense ON" : "§8Sense OFF", CnpcGuiSupport.COL_L, row, 95,
                () -> CnpcGuiSupport.act(
                        player,
                        () -> DifficultyActions.handleArg(subject, "toggle_title_sense", "0", "titles").message(),
                        () -> open(player, "titles")));
        CnpcGuiSupport.buttonSmall(gui, 21, "§cClear title", CnpcGuiSupport.COL_R, row, 95, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyActions.handleArg(subject, "clear_title", "0", "titles").message(),
                () -> open(player, "titles")));
        row += 28;
        navFooter(player, gui, row);
    }

    private static void paintTeam(ServerPlayer player, ICustomGui gui) {
        ServerPlayer subject = who(player);
        int infoY = CnpcGuiSupport.paintHeader(player, gui, "§bDifficulty · Teams", "§7Mutual rival team bonuses");
        int row = CnpcGuiSupport.paintInfoBlock(gui, infoY, DifficultyTeamGuiApi.linesForPage(subject, "team"), 4);
        CnpcGuiSupport.button(gui, 40, "§7Personal only", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "personal_only", "team"),
                () -> open(player, "team")));
        CnpcGuiSupport.button(gui, 41, "§eThreshold bonus", CnpcGuiSupport.COL_R, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "threshold_bonus_only", "team"),
                () -> open(player, "team")));
        row += 24;
        CnpcGuiSupport.button(gui, 42, "§aFull team scaling", CnpcGuiSupport.COL_L, row, () -> CnpcGuiSupport.act(
                player,
                () -> DifficultyTeamGuiApi.handleDo(subject, "mode", "full_team_scaling", "team"),
                () -> open(player, "team")));
        row += 24;
        navFooter(player, gui, row);
    }

    private static void navFooter(ServerPlayer player, ICustomGui gui, int row) {
        CnpcGuiSupport.navHubMain(player, gui, row, () -> open(player, "main"));
        if (StaffAccess.isStaff(player)) {
            row += 24;
            CnpcGuiSupport.buttonSmall(gui, 98, "§8Staff: /difficulty admin", CnpcGuiSupport.COL_L, row, 195, () -> {
                player.m_213846_(net.minecraft.network.chat.Component.m_237113_(
                        "§7Staff difficulty settings: §f/difficulty admin"));
            });
        }
    }
}
