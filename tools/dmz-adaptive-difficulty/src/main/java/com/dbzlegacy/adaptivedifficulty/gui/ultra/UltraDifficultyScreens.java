package com.dbzlegacy.adaptivedifficulty.gui.ultra;

import com.dbzlegacy.adaptivedifficulty.cache.DifficultyCache;
import com.dbzlegacy.adaptivedifficulty.calc.DifficultySnapshot;
import com.dbzlegacy.adaptivedifficulty.calc.PlayerCombatProfile;
import com.dbzlegacy.adaptivedifficulty.config.DifficultyConfig;
import com.dbzlegacy.adaptivedifficulty.currency.AncientCoinEconomy;
import com.dbzlegacy.adaptivedifficulty.data.PlayerDifficultyData;
import com.dbzlegacy.adaptivedifficulty.data.TeamMode;
import com.dbzlegacy.adaptivedifficulty.gui.DifficultyTeamGuiApi;
import com.dbzlegacy.adaptivedifficulty.calc.LmOverhaulScaledCombat;
import com.dbzlegacy.adaptivedifficulty.progression.end.EndDimensionStrength;
import com.dbzlegacy.adaptivedifficulty.service.DifficultyActions;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockSystem;
import com.dbzlegacy.adaptivedifficulty.tier.UnlockTier;
import com.dbzlegacy.adaptivedifficulty.title.DifficultyTitle;
import com.dbzlegacy.adaptivedifficulty.title.TitleSystem;
import com.dbzlegacy.adaptivedifficulty.util.PaidFeatureAccess;
import com.dbzlegacy.adaptivedifficulty.util.StaffAccess;
import com.dbzlegacy.adaptivedifficulty.util.SystemGate;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;

/**
 * Difficulty - Home / Tiers / Summons with CNPC-parity sub-pages (teams, titles, stats,
 * coin settings, staff tools). Home is overview only.
 */
public final class UltraDifficultyScreens {
    /** Titles shown per list page (enum can grow past this). */
    private static final int TITLES_PER_PAGE = 8;

    private UltraDifficultyScreens() {}

    public static void open(ServerPlayer player, String page) {
        if (player == null) {
            return;
        }
        DifficultyActions.prepareGui(player);
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String lower = raw.toLowerCase(Locale.ROOT);
        if (lower.startsWith("title_detail:")) {
            openTitleDetail(player, raw.substring("title_detail:".length()).trim());
            return;
        }
        if (lower.startsWith("weights_input:")) {
            UltraWeightShopScreens.openInput(player, raw.substring("weights_input:".length()).trim());
            return;
        }
        if (lower.startsWith("weights_confirm:")) {
            openWeightsConfirm(player, raw.substring("weights_confirm:".length()).trim());
            return;
        }
        if (lower.startsWith("titles_p") || lower.startsWith("titles:")) {
            String num = lower.startsWith("titles_p")
                    ? lower.substring("titles_p".length())
                    : lower.substring("titles:".length());
            int pageIndex = 0;
            try {
                pageIndex = Integer.parseInt(num.trim());
            } catch (NumberFormatException ignored) {
                pageIndex = 0;
            }
            openTitles(player, Math.max(0, pageIndex));
            return;
        }
        switch (lower) {
            case "tiers", "buy", "tier", "purchase", "unlock", "adjust", "change", "set", "lower" ->
                    openTiers(player);
            case "summons", "summon" -> openSummons(player);
            case "end_dragon", "dragon", "summon_dragon", "summon_confirm" -> openSummonConfirm(player);
            case "team", "teams" -> openTeam(player);
            case "titles", "title", "titles_p0" -> openTitles(player, 0);
            case "settings", "coins" -> openSettings(player);
            case "stats", "statistics", "details" -> openStats(player);
            case "admin", "staff" -> openStaffAdmin(player);
            case "eventlog", "event_log", "logs", "syslog" -> openEventLog(player);
            case "weights", "weight", "weight_shop" -> UltraWeightShopScreens.openShop(player);
            default -> openHome(player);
        }
    }

    private static void openWeightsConfirm(ServerPlayer player, String rest) {
        if (rest == null || rest.isBlank()) {
            UltraWeightShopScreens.openShop(player);
            return;
        }
        int colon = rest.lastIndexOf(':');
        if (colon <= 0 || colon >= rest.length() - 1) {
            UltraWeightShopScreens.openShop(player);
            return;
        }
        String typeId = rest.substring(0, colon).trim();
        Integer value = UltraWeightShopScreens.parseWeightValue(rest.substring(colon + 1));
        if (value == null) {
            UltraWeightShopScreens.openShop(player);
            return;
        }
        UltraWeightShopScreens.openConfirm(player, typeId, value);
    }

    private static UltraScreenSpec base(String page, String tab) {
        return UltraScreenSpec.of("difficulty")
                .title("Difficulty")
                .page(page)
                .activeTab(tab)
                .tabs("home|Home", "tiers|Tiers", "summons|Summons");
    }

    /** Subpage: no tab row — dual footer (Back + Main menu). */
    private static UltraScreenSpec sub(String page) {
        return UltraScreenSpec.of("difficulty")
                .title("Difficulty")
                .page(page);
    }


    /** Overview - where you stand, titles, rival teams, coins. Not a copy of Tiers/Summons. */
    private static void openHome(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean personal = data != null && data.isPersonalEnabled();
        String tierName = snap == null || snap.activeTier <= 0 ? "None" : snap.activeTierName;
        long cr = snap == null ? 0L : (snap.combatRating > 0 ? snap.combatRating : snap.calculated);
        UltraScreenSpec spec = base("main", "home")
                .labelValue("Personal difficulty", personal ? "On" : "Off")
                .labelValue("Active tier", tierName)
                .labelValue("Combat rating", String.valueOf(cr))
                .labelValue("Title", blank(TitleSystem.activeDisplay(player)))
                .labelValue("Title score", String.valueOf(TitleSystem.computeTitleScore(player)))
                .labelValue("Ancient Coins", AncientCoinEconomy.inventoryBreakdown(player))
                .line(UltraStyle.LABEL + "Where you stand, your titles, and rival teams.")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        UltraPickHelper.placePageButtons(
                spec,
                "titles|Titles",
                "team|Rival teams",
                "settings|Coin settings",
                "stats|Combat rating",
                "weights|Weights");
        if (StaffAccess.isStaff(player)) {
            UltraPickHelper.placePageButtons(spec, "admin|Server tools", "eventlog|Event log");
        }
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openTiers(ServerPlayer player) {
        if (!DifficultyConfig.isEnabled()) {
            openFeatureLocked(
                    player,
                    "tiers",
                    "tiers",
                    "Tiers locked",
                    "Adaptive Difficulty is off on this server. Ask staff to enable it.");
            return;
        }
        if (!SystemGate.allows(player)) {
            openFeatureLocked(
                    player,
                    "tiers",
                    "tiers",
                    "Tiers locked",
                    "This account cannot use personal difficulty. Ask staff to add you to the whitelist.");
            return;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        if (data == null || !data.isPersonalEnabled()) {
            UltraScreenSpec spec = base("tiers", "tiers")
                    .line(UltraStyle.LABEL + "Your personal difficulty is off.")
                    .line(UltraStyle.LABEL + "Turn personal ON, then pick a tier.")
                    .line(UltraStyle.DIM + "Activation cost scales with your level.")
                    .setRow(UltraStyle.CONTENT_ROW_TABS + 90);
            spec.buttonLeft("do:toggle_personal", UltraStyle.confirm("Turn personal ON"), "confirm")
                    .rowGap();
            spec.footerBackToMain();
            spec.send(player);
            return;
        }
        UnlockSystem.syncUnlocks(player, data);
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        int max = snap == null ? 0 : Math.max(0, snap.highestUnlockedTier);
        int active = data.getActiveTier();
        UltraScreenSpec spec = base("tiers", "tiers");
        if (active <= 0) {
            spec.labelValue("Active tier", "None");
        } else {
            spec.labelValue("Active tier", snap == null ? "T" + active : snap.activeTierName);
        }
        spec.labelValue("Unlocked up to", "T" + max)
                .labelValue("Ancient Coins", AncientCoinEconomy.inventoryBreakdown(player))
                .line(UltraStyle.DIM + "Scaled mobs can hurt other players nearby.")
                .line(UltraStyle.DIM + "Upgrades cost Ancient Coins - downgrades are free")
                .line(UltraStyle.DIM + "Click again within 10 seconds to confirm");
        if (!PaidFeatureAccess.bypassAncientCoinCost(player)) {
            spec.line(UltraStyle.DIM + "Upgrade cost scales with your level");
        }
        UnlockTier nextLocked = UnlockTier.byId(max + 1);
        if (nextLocked != null) {
            spec.labelValue("Next tier", "T" + nextLocked.id + " - " + UltraStyle.plain(UltraDifficultyTierUi.humanRequirement(nextLocked)))
                    .labelValue(
                            "Cost to activate",
                            UltraStyle.plain(UltraDifficultyTierUi.formatActivationCost(player, nextLocked)));
        }
        spec.setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        for (int t = 1; t <= 7; t += 2) {
            placeTierButton(spec, player, data, max, active, t, true);
            placeTierButton(spec, player, data, max, active, t + 1, false);
            spec.rowGap();
        }
        spec.buttonLeft("do:lower_tier:0", UltraStyle.danger("Clear active tier"), "danger")
                .buttonRight(
                        "do:toggle_personal",
                        data.isPersonalEnabled()
                                ? UltraStyle.confirm("Personal difficulty - ON")
                                : UltraStyle.dim("Personal difficulty - OFF"),
                        data.isPersonalEnabled() ? "confirm" : "dim")
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void placeTierButton(
            UltraScreenSpec spec,
            ServerPlayer player,
            PlayerDifficultyData data,
            int max,
            int active,
            int t,
            boolean left) {
        UnlockTier ut = UnlockTier.byId(t);
        if (ut == null) {
            return;
        }
        boolean unlocked = data.hasUnlockedTier(t) || t <= max;
        boolean eligible = UltraDifficultyTierUi.isEligible(player, ut);
        String rawLabel = UltraDifficultyTierUi.tierActionLabel(player, t, active, unlocked, eligible);
        String label = UltraStyle.plain(rawLabel);
        boolean enabled = UltraDifficultyTierUi.tierButtonEnabled(t, active, unlocked, eligible);
        String style;
        String action;
        if (active == t) {
            style = "confirm";
            label = UltraStyle.confirm(label);
            action = "do:activate:" + t;
        } else if (enabled) {
            style = "confirm";
            label = UltraStyle.confirm(label);
            action = "do:activate:" + t;
        } else {
            style = "dim";
            label = UltraStyle.dim(label.isBlank() ? ("T" + t + " - locked") : label);
            action = "do:activate:" + t;
        }
        if (left) {
            spec.buttonLeft(action, label, style);
        } else {
            spec.buttonRight(action, label, style);
        }
    }

    private static void openFeatureLocked(
            ServerPlayer player, String page, String tab, String title, String body) {
        UltraScreenSpec spec = base(page, tab)
                .line(UltraStyle.danger(title))
                .line(UltraStyle.LABEL + body)
                .setRow(UltraStyle.CONTENT_ROW_TABS + 80);
        if (DifficultyConfig.isEnabled() && SystemGate.allows(player)) {
            spec.buttonLeft("do:toggle_personal", UltraStyle.confirm("Turn personal ON"), "confirm")
                    .rowGap();
        }
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openSummons(ServerPlayer player) {
        boolean ready = EndDimensionStrength.canOpenSummonMenu(player);
        boolean free = PaidFeatureAccess.bypassAncientCoinCost(player);
        String cost = free
                ? "Free for staff"
                : AncientCoinEconomy.formatExactCost(EndDimensionStrength.summonCopperCost());
        UltraScreenSpec spec = base("summons", "summons")
                .line(UltraStyle.LABEL + "Select a boss, then confirm. More bosses can be added to this list.");
        for (String line : summonStatusLines(player, ready)) {
            spec.line(line);
        }
        spec.labelValue("End Dragon", ready ? "Ready - " + cost : "Requirements not met")
                .setRow(UltraStyle.CONTENT_ROW_TABS + UltraStyle.BODY_MIN_H);
        spec.buttonLeft("page:end_dragon", UltraStyle.confirm("Summon End Dragon"), "confirm")
                .rowGap();
        spec.footerBackToMain();
        spec.send(player);
    }

    private static void openSummonConfirm(ServerPlayer player) {
        boolean ready = EndDimensionStrength.canOpenSummonMenu(player);
        boolean free = PaidFeatureAccess.bypassAncientCoinCost(player);
        String cost = free
                ? "Free for staff"
                : AncientCoinEconomy.formatExactCost(EndDimensionStrength.summonCopperCost());
        UltraScreenSpec spec = sub("end_dragon")
                .line(UltraStyle.LABEL + "End Dragon")
                .labelValue("Cost", cost)
                .labelValue("Status", ready ? "Ready" : "Requirements not met");
        for (String line : summonStatusLines(player, ready)) {
            spec.line(line);
        }
        spec.setRow(UltraStyle.CONTENT_ROW + UltraStyle.BODY_MIN_H);
        if (ready) {
            spec.button("do:summon_end_dragon", UltraStyle.confirm("Confirm summon"), "confirm").rowGap();
        } else {
            spec.line(UltraStyle.dim("Select End Dragon first - clear the notes above, then return."))
                    .button("do:summon_end_dragon", UltraStyle.dim("Confirm summon - locked"), "dim")
                    .rowGap();
        }
        spec.footerBack("page:summons", "Summons");
        spec.send(player);
    }

    /** CNPC {@code summonStatusLines} - Ultra colors, same guidance. */
    private static java.util.List<String> summonStatusLines(ServerPlayer subject, boolean canSummon) {
        java.util.List<String> lines = new java.util.ArrayList<>();
        if (!DifficultyConfig.get().enableEndDimensionStrength) {
            lines.add(UltraStyle.LABEL + "End strength is turned off. Ask staff to enable it.");
            return lines;
        }
        if (!DifficultyConfig.get().enableEndPlayerDragonSummon) {
            lines.add(UltraStyle.LABEL + "Boss summoning is turned off. Ask staff to turn it on.");
            return lines;
        }
        lines.add(UltraStyle.LABEL + "The dragon matches your difficulty. Only you can damage it.");
        lines.add(
                UltraStyle.LABEL
                        + "You need a tier from "
                        + UltraStyle.VALUE
                        + "T"
                        + EndDimensionStrength.PLAYER_SUMMON_MIN_TIER
                        + UltraStyle.LABEL
                        + " through "
                        + UltraStyle.VALUE
                        + "T"
                        + EndDimensionStrength.PLAYER_SUMMON_MAX_TIER
                        + UltraStyle.LABEL
                        + ", and you must be in The End.");
        if (!canSummon) {
            if (!SystemGate.participates(subject)) {
                lines.add(UltraStyle.LABEL + "Turn on personal difficulty on the Tiers tab first.");
            } else {
                lines.add(UltraStyle.LABEL + "Activate a tier in that range on the Tiers tab first.");
            }
        } else if (subject != null && !EndDimensionStrength.isTheEnd(subject.m_9236_())) {
            lines.add(UltraStyle.LABEL + "Travel to The End, then confirm the summon.");
        }
        return lines;
    }

    private static void openTeam(ServerPlayer player) {
        if (!DifficultyConfig.isEnabled()) {
            openFeatureLocked(
                    player,
                    "team",
                    "home",
                    "Teams locked",
                    "Adaptive Difficulty is off. Ask staff to enable it before using team scaling.");
            return;
        }
        if (!SystemGate.allows(player)) {
            openFeatureLocked(
                    player,
                    "team",
                    "home",
                    "Teams locked",
                    "You cannot use personal difficulty yet. Ask staff to add you to the whitelist.");
            return;
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        if (data == null || !data.isPersonalEnabled()) {
            openFeatureLocked(
                    player,
                    "team",
                    "home",
                    "Teams locked",
                    "Turn on Personal difficulty on the Tiers tab first, then return here to pick a team mode.");
            return;
        }
        TeamMode mode = data.getTeamMode();
        UltraScreenSpec spec = sub("team")
                .line(UltraStyle.LABEL + "How rival teams affect scaling");
        for (String line : DifficultyTeamGuiApi.linesForPage(player, "team")) {
            if (line != null && !line.isBlank()) {
                spec.line(line);
            }
        }
        spec.setRow(UltraStyle.CONTENT_ROW + UltraStyle.BODY_MIN_H);
        spec.buttonLeft(
                        "do:mode:personal",
                        mode == TeamMode.PERSONAL_ONLY
                                ? UltraStyle.confirm("Personal - on")
                                : "Personal",
                        mode == TeamMode.PERSONAL_ONLY ? "confirm" : "normal")
                .buttonRight(
                        "do:mode:threshold",
                        mode == TeamMode.THRESHOLD_BONUS_ONLY
                                ? UltraStyle.confirm("Threshold - on")
                                : "Threshold",
                        mode == TeamMode.THRESHOLD_BONUS_ONLY ? "confirm" : "normal")
                .rowGap();
        spec.buttonLeft(
                        "do:mode:full",
                        mode == TeamMode.FULL_TEAM_SCALING
                                ? UltraStyle.confirm("Full team - on")
                                : "Full team",
                        mode == TeamMode.FULL_TEAM_SCALING ? "confirm" : "normal")
                .buttonRight(
                        "do:toggle_personal",
                        data.isPersonalEnabled()
                                ? UltraStyle.confirm("Personal difficulty - ON")
                                : UltraStyle.dim("Personal difficulty - OFF"),
                        data.isPersonalEnabled() ? "confirm" : "dim")
                .rowGap();
        spec.footerBackHome("Difficulty Home");
        spec.send(player);
    }

    private static void openTitles(ServerPlayer player, int pageIndex) {
        DifficultyTitle[] all = DifficultyTitle.values();
        int pageSize = TITLES_PER_PAGE;
        int pages = Math.max(1, (all.length + pageSize - 1) / pageSize);
        int page = Math.min(pages - 1, Math.max(0, pageIndex));
        String active = TitleSystem.activeId(player);
        UltraScreenSpec spec = sub("titles_p" + page)
                .labelValue("Equipped", blank(TitleSystem.activeDisplay(player)))
                .labelValue("Title score", String.valueOf(TitleSystem.computeTitleScore(player)))
                .line(UltraStyle.LABEL + "Equip a title you have unlocked.")
                .labelValue("Titles page", (page + 1) + "/" + pages)
                .setRow(UltraStyle.CONTENT_ROW + 80);
        int from = page * pageSize;
        int placed = 0;
        for (int i = from; i < all.length && placed < pageSize; i++) {
            DifficultyTitle t = all[i];
            if (t == null) {
                continue;
            }
            boolean earned = TitleSystem.has(player, t);
            boolean on = t.id != null && t.id.equals(active);
            String label = on
                    ? UltraStyle.confirm(t.display + " - equipped")
                    : (earned ? t.display : UltraStyle.dim(t.display + " - locked"));
            String style = on ? "confirm" : (earned ? "normal" : "dim");
            String action = "page:title_detail:" + t.id;
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
        if (page > 0) {
            spec.buttonLeft(
                    "page:titles_p" + (page - 1),
                    UltraStyle.dim("Prev titles page"),
                    UltraStyle.STYLE_DIM);
        }
        if (page + 1 < pages) {
            if (page > 0) {
                spec.buttonRight(
                        "page:titles_p" + (page + 1),
                        UltraStyle.LABEL + "Next titles page",
                        UltraStyle.STYLE_NORMAL);
            } else {
                spec.buttonLeft(
                        "page:titles_p" + (page + 1),
                        UltraStyle.LABEL + "Next titles page",
                        UltraStyle.STYLE_NORMAL);
            }
            spec.rowGap();
        } else if (page > 0) {
            spec.rowGap();
        }
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean sense = data != null && data.titleProgress().titleSenseChat();
        spec.buttonLeft(
                        "do:toggle_title_sense",
                        sense ? UltraStyle.confirm("Title Sense - ON") : UltraStyle.dim("Title Sense - OFF"),
                        sense ? "confirm" : "dim")
                .buttonRight("do:clear_title", UltraStyle.danger("Unequip title"), "danger")
                .rowGap();
        spec.footerBackHome("Difficulty Home");
        spec.send(player);
    }

    private static void openTitleDetail(ServerPlayer player, String titleIdRaw) {
        DifficultyTitle title = DifficultyTitle.byId(titleIdRaw);
        UltraScreenSpec spec = sub("title_detail:" + (titleIdRaw == null ? "" : titleIdRaw));
        if (title == null) {
            spec.line(UltraStyle.dim("Couldn't find that - try refreshing the list."))
                    .setRow(UltraStyle.CONTENT_ROW + 80);
            spec.footerBack("page:titles_p0", "Titles");
            spec.send(player);
            return;
        }
        boolean earned = TitleSystem.has(player, title);
        boolean equipped = title.id.equals(TitleSystem.activeId(player));
        int mastery = DifficultyCache.data(player).titleProgress().masteryLevel(title.id);
        spec.line(UltraStyle.LABEL + title.masteryDisplay(mastery))
                .labelValue("Kind", kindLabel(title.kind))
                .line(earned
                        ? UltraStyle.confirm("You have this title.")
                        : UltraStyle.dim("You do not have this yet."))
                .line(equipped ? UltraStyle.confirm("Currently equipped.") : "")
                .line(UltraStyle.LABEL + "How to earn " + UltraStyle.VALUE + humanRequirement(title))
                .line(UltraStyle.LABEL + "Bonus when worn " + UltraStyle.VALUE
                        + UltraStyle.plain(title.perkTip(mastery)))
                .labelValue("Title score", "+" + title.scorePoints)
                .setRow(UltraStyle.CONTENT_ROW + UltraStyle.BODY_MIN_H);
        if (earned && !equipped) {
            spec.buttonLeft("do:equip_title:" + title.id, UltraStyle.confirm("Equip this title"), "confirm")
                    .rowGap();
        } else if (earned) {
            spec.buttonLeft("do:equip_title:" + title.id, UltraStyle.confirm("Already equipped"), "confirm")
                    .rowGap();
        } else {
            spec.line(UltraStyle.dim("Equip unlocks after you earn the title."));
        }
        spec.footerBack("page:titles_p0", "Titles");
        spec.send(player);
    }

    private static String kindLabel(DifficultyTitle.Kind kind) {
        if (kind == null) {
            return "-";
        }
        return switch (kind) {
            case TIER -> "Tier milestone";
            case COMBAT -> "Combat challenge";
            case CHALLENGE -> "Special challenge";
        };
    }

    private static String humanRequirement(DifficultyTitle title) {
        if (title == null) {
            return "-";
        }
        return UltraStyle.plain(title.requirementTip())
                .replace("DMZ ", "Level ")
                .replace(" or Prestige ", " or prestige ");
    }

    private static void openSettings(ServerPlayer player) {
        PlayerDifficultyData data = DifficultyCache.data(player);
        boolean coinChat = data != null && data.isCoinDropChat();
        UltraScreenSpec spec = sub("settings")
                .line(UltraStyle.LABEL + "Ancient Coin settings")
                .labelValue("Ancient Coins", AncientCoinEconomy.inventoryBreakdown(player))
                .line(UltraStyle.LABEL + "Coin drop messages in chat.")
                .labelValue("Coin messages", coinChat ? "On" : "Off")
                .line(UltraStyle.DIM + "Earn coins from scaled kills and tier play.")
                .setRow(UltraStyle.CONTENT_ROW + 90);
        spec.button(
                        "do:toggle_coin_chat",
                        coinChat
                                ? UltraStyle.confirm("Coin messages - ON")
                                : UltraStyle.dim("Coin messages - OFF"),
                        coinChat ? "confirm" : "dim")
                .rowGap();
        if (StaffAccess.isStaff(player)) {
            spec.buttonLeft("page:stats", UltraStyle.dim("Staff details"), "dim").rowGap();
        }
        spec.footerBackHome("Difficulty Home");
        spec.send(player);
    }

    private static void openStats(ServerPlayer player) {
        DifficultySnapshot snap = DifficultyCache.refresh(player);
        PlayerCombatProfile profile = PlayerCombatProfile.of(player);
        Map<String, String> ph = new HashMap<>();
        LmOverhaulScaledCombat.putPlaceholders(ph, player);
        long cr = snap == null ? 0L : (snap.combatRating > 0 ? snap.combatRating : snap.calculated);
        UltraScreenSpec spec = sub("stats")
                .line(UltraStyle.LABEL + "Combat rating and scaled stats")
                .labelValue(
                        "Tier",
                        (snap == null ? "-" : snap.activeTierName)
                                + " - State "
                                + (snap == null ? "-" : snap.state()))
                .labelValue(
                        "Combat Rating",
                        String.valueOf(cr)
                                + " - Unlocked T"
                                + (snap == null ? 0 : snap.highestUnlockedTier))
                .labelValue(
                        "Level",
                        (snap == null ? "-" : String.valueOf(snap.dmzLevel))
                                + " - Prestige "
                                + (snap == null ? "-" : String.valueOf(snap.prestige)))
                .labelValue("Title", blank(TitleSystem.activeDisplay(player)))
                .labelValue(
                        "Overhaul",
                        ph.getOrDefault("overhaul_scale", "x1")
                                + " - Melee "
                                + ph.getOrDefault("melee_scaled", "?")
                                + " - Strike "
                                + ph.getOrDefault("strike_scaled", "?"))
                .labelValue(
                        "Ki / Defense",
                        ph.getOrDefault("ki_scaled", "?") + " / " + ph.getOrDefault("defense_scaled", "?"))
                .labelValue(
                        "Class / Style",
                        blank(profile.fightingClass)
                                + " / "
                                + (profile.style == null ? "HYBRID" : profile.style.name()))
                .labelValue("Top stats", blank(profile.topStatsLabel()))
                .labelValue("Ancient Coins", AncientCoinEconomy.inventoryBreakdown(player))
                .setRow(UltraStyle.CONTENT_ROW + UltraStyle.BODY_MIN_H);
        spec.footerBackHome("Difficulty Home");
        spec.send(player);
    }

    private static void openStaffAdmin(ServerPlayer player) {
        if (!StaffAccess.isStaff(player)) {
            openHome(player);
            return;
        }
        UltraScreenSpec spec = sub("admin")
                .line(UltraStyle.LABEL + "Server tools - typed admin: /difficulty admin")
                .line(UltraStyle.DIM + "Reload config, event log, and LM Staff Admin hub")
                .setRow(UltraStyle.CONTENT_ROW + 90);
        spec.buttonLeft("do:reload", UltraStyle.confirm("Reload LM config"), "confirm")
                .buttonRight("page:stats", UltraStyle.dim("Staff details"), "dim")
                .rowGap();
        spec.buttonLeft("open:admin", UltraStyle.danger("LM Staff Admin"), "danger")
                .buttonRight("page:eventlog", UltraStyle.dim("Event log"), "dim")
                .rowGap();
        spec.footerBackHome("Difficulty Home");
        spec.send(player);
    }

    private static void openEventLog(ServerPlayer player) {
        if (!StaffAccess.isStaff(player)) {
            openHome(player);
            return;
        }
        UltraScreenSpec spec = sub("eventlog")
                .line(UltraStyle.LABEL + "Unified system event log")
                .line(UltraStyle.DIM + "Use the toggles, or typed: /lm admin syslog ...")
                .setRow(UltraStyle.CONTENT_ROW + 90);
        spec.buttonLeft("do:syslog:status", UltraStyle.confirm("Status"), "confirm")
                .buttonRight("do:syslog:on", UltraStyle.confirm("Turn ON"), "confirm")
                .rowGap();
        spec.buttonLeft("do:syslog:off", UltraStyle.danger("Turn OFF"), "danger")
                .buttonRight("do:syslog:flush", UltraStyle.dim("Flush"), "dim")
                .rowGap();
        spec.footerBack("page:admin", "Staff Admin");
        spec.send(player);
    }

    private static String blank(String s) {
        if (s == null || s.isBlank() || "None".equalsIgnoreCase(s.trim())) {
            return "-";
        }
        return s;
    }
}
