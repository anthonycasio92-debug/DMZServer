package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.Zrips.CMILib.GUI.CMIGui;
import net.Zrips.CMILib.GUI.CMIGuiButton;
import net.Zrips.CMILib.GUI.GUIManager.InvType;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * CMILib inventory GUI — Legacy Mechanics Sparring.
 * Pages: main · stats · top · mentor (Actions) · pending · pending_decide · dojo · pickers.
 */
public final class CmiSparGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private CmiSparGui() {}

    public static boolean available() {
        return CmiDifficultyGui.available();
    }

    public static boolean open(Player player, String page) {
        if (player == null || !available()) {
            return false;
        }
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        try {
            if (p.startsWith("pending_decide:")) {
                openPendingDecide(player, raw.substring("pending_decide:".length()).trim());
            } else if (p.startsWith("top_") || p.startsWith("top ") || "top".equals(p) || "leaderboard".equals(p)) {
                openTop(player, p);
            } else if ("stats".equals(p) || "statistics".equals(p)) {
                openDetail(player, "stats", "&eLast 3 Spar Reports", Material.WRITTEN_BOOK);
            } else if ("mentor".equals(p) || "actions".equals(p)) {
                openMentor(player);
            } else if ("pending".equals(p) || "invites".equals(p) || "pendinginvites".equals(p)) {
                openPending(player);
            } else if ("dojo_war".equals(p)) {
                openDojoWar(player);
            } else if ("dojo_war_pending".equals(p) || "dojo_war_invites".equals(p)) {
                openDojoWarPending(player);
            } else if (p.startsWith("dojo_war_pending_decide:")) {
                openDojoWarPendingDecide(player, raw.substring("dojo_war_pending_decide:".length()).trim());
            } else if (p.startsWith("dojo_top_") || p.startsWith("dojo_top ") || "dojo_top".equals(p)
                    || "dojo_rank".equals(p) || "dojo_rankings".equals(p)) {
                openDojoRank(player, p);
            } else if ("pick_dojo_challenge".equals(p)) {
                openDojoChallengePicker(player);
            } else if ("dojo_hof".equals(p) || "dojo_hall".equals(p)) {
                openDetail(player, "dojo_hof", "&6Dojo Hall of Fame", Material.GOLD_BLOCK);
            } else if ("dojo_members".equals(p) || "dojo_contributions".equals(p)) {
                openDojoMembers(player);
            } else if ("pick_dojo_banner".equals(p)) {
                openDojoBannerPicker(player);
            } else if ("dojo".equals(p) || "roster".equals(p) || "apprentices".equals(p)
                    || "dojo_member".equals(p) || "dojo_mine".equals(p) || "dojo_own".equals(p)) {
                openDojo(player, p);
            } else if ("pick_apprentice".equals(p)) {
                openPicker(player, "mentor_invite", "mentor",
                        "&aInvite Apprentice", "&7Ask them to be your apprentice");
            } else if ("pick_mentor".equals(p)) {
                openPicker(player, "apprentice_invite", "mentor",
                        "&bAsk Mentor", "&7Ask them to be your mentor");
            } else if ("pick_accept".equals(p)) {
                openPendingPicker(player, "mentor_accept", "pending",
                        "&aAccept Invite", "&7Accept this mentor invite", true);
            } else if ("pick_decline".equals(p)) {
                openPendingPicker(player, "mentor_decline", "pending",
                        "&cDecline Invite", "&7Decline this mentor invite", false);
            } else if ("pick_release".equals(p)) {
                openReleasePicker(player);
            } else if ("help".equals(p)) {
                openMain(player);
            } else if ("admin".equals(p)) {
                if (ForgeBridge.isStaff(player)) {
                    openAdmin(player);
                } else {
                    openMain(player);
                }
            } else {
                openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cSpar CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.sparPlaceholders(player);
        CMIGui gui = base(player, "&8Sparring", 5);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        CMIGuiButton status = new CMIGuiButton(4, Material.GOLDEN_SWORD,
                !bridgeOk ? "&c&lUNAVAILABLE"
                        : !systemOn ? "&c&lSPARRING DISABLED"
                        : "&b&lSparring");
        status.lockField();
        if (!bridgeOk || !systemOn) {
            status.addLore(unavailableLore(bridgeOk));
            gui.addButton(status);
            gui.addButton(hubBtn(40));
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            GuiFeedback.openCmi(gui);
            return;
        }
        status.addLore(statusLore(player, ph));
        gui.addButton(status);

        // Main: Status · Stats · Top · Mentor · End Session · Hub · Close (pickers on Mentor only)
        gui.addButton(pageBtn(player, 19, "spar.main.stats", Material.BOOK, "&eStats", "stats",
                "&7Last 3 spar reports", "&8One item per spar"));
        gui.addButton(pageBtn(player, 20, "spar.main.dojo_rank", Material.BOOKSHELF, "&6Dojo Rankings",
                "dojo_rank", "&7Season ladder · dojo wars", "&8Spar rival dojos to earn ranking points"));
        gui.addButton(pageBtn(player, 21, "spar.main.top", Material.GOLDEN_HELMET, "&fTop", "top",
                "&7Leaderboard"));
        gui.addButton(pageBtn(player, 23, "spar.main.mentor", Material.EMERALD, "&bMentor", "mentor",
                "&7Invite · Pending · Dojo · Leave / Release"));

        boolean mentorTpOn = "true".equalsIgnoreCase(ph.getOrDefault("mentorTpMsg", "true"));
        gui.addButton(actionBtn(player, 24,
                mentorTpOn ? "spar.main.mentor_tpmsg_on" : "spar.main.mentor_tpmsg_off",
                mentorTpOn ? Material.EMERALD : Material.GRAY_DYE,
                mentorTpOn ? "&aMentor TP ON" : "&8Mentor TP OFF",
                "mentor_tpmsg", "toggle", "main",
                List.of(
                        mentorTpOn ? "&7Click to mute mentor share TP chat"
                                : "&7Click to show apprentice share TP in chat",
                        "&8When your dojo earns TP from spars",
                        "",
                        "&eClick to toggle8Click to switch"
                )));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "true"));
        gui.addButton(actionBtn(player, 25,
                tpOn ? "spar.main.tpmsg_on" : "spar.main.tpmsg_off",
                tpOn ? Material.BELL : Material.GRAY_DYE,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                "tpmsg", "toggle", "main",
                List.of(
                        tpOn ? "&7Click to mute spar TP chat" : "&7Click to show spar TP chat while fighting",
                        "&8Players: +TP (style)",
                        "&8Staff: full bonus / stack detail",
                        "",
                        "&eClick to toggle8Click to switch"
                )));

        boolean session = "true".equalsIgnoreCase(ph.getOrDefault("sessionActive", "false"));
        if (session) {
            gui.addButton(actionBtn(player, 31, "spar.main.end_session", Material.RED_DYE, "&cEnd Session",
                    "end", "0", "main",
                    List.of("&7End your active spar session")));
        }

        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(player, 37, "spar.main.admin", Material.COMMAND_BLOCK, "&cAdmin", "admin",
                    "&7Save data, check status, reset cooldowns", "&8Staff only"));
        }
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openTop(Player player, String pageKey) {
        String cat = "tp";
        String lower = pageKey.toLowerCase(Locale.ROOT);
        if (lower.startsWith("top_")) {
            cat = lower.substring(4).trim();
        } else if (lower.startsWith("top ")) {
            cat = lower.substring(4).trim();
        }
        if (cat.isBlank()) {
            cat = "tp";
        }
        String lorePage = "top_" + cat;
        CMIGui gui = base(player, "&8Sparring", 5);
        List<String> raw = toAmp(ForgeBridge.sparLines(player, lorePage));
        List<GuiBoardHelper.TopEntry> entries = GuiBoardHelper.parseTopEntries(raw);
        CMIGuiButton info = new CMIGuiButton(4, Material.GOLDEN_HELMET, "&f&lTop — " + cat);
        info.lockField();
        List<String> topHeader = new ArrayList<>();
        topHeader.add("");
        topHeader.addAll(GuiBoardHelper.tips(player,
                "&7Sparring leaderboard", "&8Player heads below · categories on bottom"));
        info.addLore(topHeader);
        gui.addButton(info);
        if (entries.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(13, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_top", "&7No sparring data yet"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("spar.empty.no_top", List.of("&7Spar nearby to earn TP")));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(entries.size(), 14));
            for (int i = 0; i < slots.length && i < entries.size(); i++) {
                ItemStack head = GuiBoardHelper.topHead(entries.get(i));
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                gui.addButton(btn);
            }
        }
        gui.addButton(pageBtn(player, 29, "spar.top.tp", Material.GOLD_INGOT, "&eTP", "top_tp", "&7Total TP"));
        gui.addButton(pageBtn(player, 31, "spar.top.sessions", Material.CLOCK, "&aSessions", "top_sessions",
                "&7Sessions"));
        gui.addButton(pageBtn(player, 33, "spar.top.perfect", Material.NETHER_STAR, "&bPerfect", "top_perfect",
                "&7Perfect spars"));
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openDojoRank(Player player, String pageKey) {
        String cat = "rp";
        String lower = pageKey.toLowerCase(Locale.ROOT);
        if (lower.startsWith("dojo_top_")) {
            cat = lower.substring(9).trim();
        } else if (lower.startsWith("dojo_top ")) {
            cat = lower.substring(9).trim();
        }
        if (cat.isBlank() || "dojo_rank".equals(cat) || "dojo_rankings".equals(cat)) {
            cat = "rp";
        }
        Map<String, String> ph = ForgeBridge.sparPlaceholders(player);
        CMIGui gui = base(player, "&8Dojo Rankings", 5);
        List<String> info = toAmp(ForgeBridge.sparLines(player, "dojo_info"));
        List<String> raw = toAmp(ForgeBridge.sparLines(player, "dojo_top_" + cat));
        List<GuiBoardHelper.DojoTopCard> dojoCards = GuiBoardHelper.parseDojoTopCards(
                ForgeBridge.sparDojoTopCards(player, cat));
        List<GuiBoardHelper.TopEntry> entries = dojoCards.isEmpty()
                ? GuiBoardHelper.parseTopEntries(raw) : List.of();
        String sortLabel = dojoSortLabel(cat);
        CMIGuiButton header = new CMIGuiButton(4, Material.BOOKSHELF, "&6&lDojo Rankings — " + sortLabel);
        header.lockField();
        List<String> headerLore = new ArrayList<>(info);
        headerLore.add("");
        headerLore.addAll(GuiBoardHelper.tips(player,
                "&7Dojo season ladder · &f" + sortLabel,
                "&8Banners show each dojo · bottom row changes sort"));
        header.addLore(headerLore);
        gui.addButton(header);
        boolean isMaster = "true".equalsIgnoreCase(ph.getOrDefault("dojo_master", "false"));
        // Menu buttons on row 3 (and war row 2) — register before ladder so CMI clicks hit commands.
        if (isMaster) {
            int warPending = 0;
            try {
                warPending = Integer.parseInt(ph.getOrDefault("dojo_war_pending", "0"));
            } catch (NumberFormatException ignored) {
                warPending = 0;
            }
            gui.addButton(pageBtn(player, 19, "spar.dojo.war", Material.DIAMOND_SWORD,
                    warPending > 0 ? "&cDojo War &f(" + warPending + ")" : "&cDojo War",
                    "dojo_war",
                    "&7Declare · pending · banner",
                    warPending > 0 ? "&ePending wars — click to respond" : "&8Same layout as Mentor Actions"));
        }
        gui.addButton(pageBtn(player, 30, "spar.dojo.members", Material.PLAYER_HEAD, "&bMembers",
                "dojo_members", "&7Season contributions"));
        gui.addButton(pageBtn(player, 32, "spar.dojo.hof", Material.GOLD_BLOCK, "&6Hall of Fame",
                "dojo_hof", "&7Past season champions"));
        Material rpMat = "rp".equals(cat) ? Material.GOLD_BLOCK : Material.GOLD_INGOT;
        Material winsMat = "wins".equals(cat) || "win".equals(cat) ? Material.DIAMOND_SWORD : Material.IRON_SWORD;
        Material tpMat = "tp".equals(cat) ? Material.EXPERIENCE_BOTTLE : Material.GLASS_BOTTLE;
        gui.addButton(pageBtn(player, 29, "spar.dojo.rp", rpMat, "&eSeason Points", "dojo_top_rp",
                "&7Rank by season RP"));
        gui.addButton(pageBtn(player, 31, "spar.dojo.wins", winsMat, "&aWins", "dojo_top_wins",
                "&7Rank by wins"));
        gui.addButton(pageBtn(player, 33, "spar.dojo.tp", tpMat, "&bSpar TP", "dojo_top_tp",
                "&7Rank by TP earned vs other dojos"));
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        int rowCount = !dojoCards.isEmpty() ? dojoCards.size() : entries.size();
        if (rowCount == 0) {
            CMIGuiButton empty = new CMIGuiButton(13, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_dojo_rank", "&7No dojo data yet"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("spar.empty.no_dojo_rank",
                    List.of("&7Join a dojo and spar rivals")));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.dojoRankLadderSlots(rowCount, isMaster);
            for (int i = 0; i < slots.length && i < rowCount; i++) {
                ItemStack icon = !dojoCards.isEmpty()
                        ? GuiBoardHelper.dojoTopBanner(dojoCards.get(i))
                        : GuiBoardHelper.topHead(entries.get(i));
                CMIGuiButton btn = new CMIGuiButton(slots[i], icon);
                btn.lockField();
                gui.addButton(btn);
            }
        }
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    /** Dojo War hub — mirrors Mentor Actions layout. */
    private static void openDojoWar(Player player) {
        Map<String, String> ph = ForgeBridge.sparPlaceholders(player);
        boolean isMaster = "true".equalsIgnoreCase(ph.getOrDefault("dojo_master", "false"));
        int warPending = 0;
        try {
            warPending = Integer.parseInt(ph.getOrDefault("dojo_war_pending", "0"));
        } catch (NumberFormatException ignored) {
            warPending = 0;
        }
        CMIGui gui = base(player, "&8Dojo War", 5);
        CMIGuiButton header = new CMIGuiButton(4, Material.DIAMOND_SWORD, "&c&lDojo War");
        header.lockField();
        header.addLore(toAmp(ForgeBridge.sparLines(player, "dojo_war")));
        gui.addButton(header);
        if (isMaster) {
            gui.addButton(pageBtn(player, 19, "spar.dojo.challenge", Material.LIME_DYE, "&cDeclare War…",
                    "pick_dojo_challenge", "&7Pick a rival dojo",
                    "&82× RP during active wars"));
            gui.addButton(pageBtn(player, 21, "spar.dojo.war_pending", Material.CLOCK,
                    warPending > 0 ? "&ePending &f(" + warPending + ")" : "&ePending",
                    "dojo_war_pending",
                    "&7Incoming + outgoing wars",
                    warPending > 0 ? "&aAccept, decline, or revoke" : "&8No pending wars"));
            gui.addButton(pageBtn(player, 22, "spar.dojo.banner", Material.WHITE_BANNER, "&fBanner…",
                    "pick_dojo_banner", "&7Pick banner color", "&8Shows on rankings"));
            gui.addButton(pageBtn(player, 25, "spar.dojo.war_rankings", Material.BOOKSHELF, "&6Rankings",
                    "dojo_rank", "&7Season ladder"));
        } else {
            CMIGuiButton declareOff = new CMIGuiButton(19, Material.GRAY_DYE,
                    GuiTooltips.name("spar.dojo.war_master_only", "&8Declare War…"));
            declareOff.lockField();
            declareOff.addLore(GuiTooltips.buttonLore("spar.dojo.war_master_only",
                    List.of("&7Only dojo masters manage wars")));
            gui.addButton(declareOff);
            CMIGuiButton pendingOff = new CMIGuiButton(21, Material.GRAY_DYE,
                    GuiTooltips.name("spar.dojo.war_pending", "&8Pending"));
            pendingOff.lockField();
            pendingOff.addLore(List.of("", "&7Masters only"));
            gui.addButton(pendingOff);
            CMIGuiButton bannerOff = new CMIGuiButton(22, Material.GRAY_DYE,
                    GuiTooltips.name("spar.dojo.banner", "&8Banner…"));
            bannerOff.lockField();
            bannerOff.addLore(List.of("", "&7Masters only"));
            gui.addButton(bannerOff);
            gui.addButton(pageBtn(player, 25, "spar.dojo.war_rankings", Material.BOOKSHELF, "&6Rankings",
                    "dojo_rank", "&7View season ladder"));
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "dojo_rank", "&7Dojo Rankings"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openDojoWarPending(Player player) {
        CMIGui gui = base(player, "&8Pending Dojo Wars", 5);
        List<GuiBoardHelper.PendingInvite> wars = GuiBoardHelper.parsePendingInvites(
                ForgeBridge.sparPendingDojoWarCards(player));
        CMIGuiButton info = new CMIGuiButton(4, Material.CLOCK, "&e&lPending Dojo Wars");
        info.lockField();
        List<String> header = new ArrayList<>();
        header.add("");
        header.add(wars.isEmpty() ? "&7No pending wars." : "&7" + wars.size() + " pending");
        header.addAll(GuiBoardHelper.tips(player,
                "&a◀ Incoming &7= Accept / Decline",
                "&6▶ Outgoing &7= Revoke challenge"));
        info.addLore(header);
        gui.addButton(info);
        if (wars.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_dojo_war_pending", "&7No pending wars"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("spar.empty.no_dojo_war_pending", List.of(
                    "&7Declare war from Dojo War",
                    "&7Incoming shows when challenged")));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(wars.size(), 21));
            for (int i = 0; i < slots.length && i < wars.size(); i++) {
                GuiBoardHelper.PendingInvite war = wars.get(i);
                CMIGuiButton btn = new CMIGuiButton(slots[i], GuiBoardHelper.pendingInviteHead(player, war));
                btn.lockField();
                if (war.incoming) {
                    btn.addCommand("lmdo spar page dojo_war_pending_decide:" + war.pickerArg());
                } else {
                    btn.addCommand("lmdo spar dojo_war_cancel " + war.pickerArg() + " dojo_war_pending");
                }
                gui.addButton(btn);
            }
        }
        gui.addButton(pageBtn(player, 39, "spar.dojo.war_hub", Material.DIAMOND_SWORD, "&cDojo War", "dojo_war",
                "&7Declare · banner"));
        gui.addButton(pageBtn(player, 36, "spar.dojo.war_pending_back", Material.ARROW, "&7Back", "dojo_war",
                "&7Dojo War"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openDojoWarPendingDecide(Player player, String arg) {
        CMIGui gui = base(player, "&8Dojo War Request", 5);
        GuiBoardHelper.PendingInvite war = findDojoWarPendingInvite(player, arg);
        String display = war != null ? war.name : (arg == null || arg.isBlank() ? "?" : arg.trim());
        if (display.regionMatches(true, 0, "uuid:", 0, 5)) {
            display = display.substring(5).trim();
        }
        CMIGuiButton info = new CMIGuiButton(4, Material.CLOCK,
                GuiTooltips.name("spar.dojo.war_decide_info", "&e&lRespond"));
        info.lockField();
        info.addLore(GuiTooltips.buttonLore("spar.dojo.war_decide_info", List.of(
                "&7Challenge from dojo &f" + display,
                "&aAccept &7→ 24h war · &f2× RP",
                "&cDecline &7→ refuse challenge")));
        gui.addButton(info);
        CMIGuiButton headBtn = new CMIGuiButton(13,
                war != null ? GuiBoardHelper.pendingInviteHead(player, war)
                        : new ItemStack(Material.DIAMOND_SWORD));
        headBtn.lockField();
        gui.addButton(headBtn);
        gui.addButton(actionBtn(player, 20, "spar.dojo.accept", Material.LIME_DYE, "&aAccept War",
                "dojo_accept", "0", "dojo_war_pending",
                List.of("&7Accept war with &f" + display), Map.of("name", display)));
        gui.addButton(actionBtn(player, 24, "spar.dojo.decline", Material.RED_DYE, "&cDecline War",
                "dojo_decline", "0", "dojo_war_pending",
                List.of("&7Decline challenge from &f" + display), Map.of("name", display)));
        gui.addButton(pageBtn(player, 36, "spar.dojo.war_decide_back", Material.ARROW, "&7Back",
                "dojo_war_pending", "&7Pending wars"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static GuiBoardHelper.PendingInvite findDojoWarPendingInvite(Player player, String arg) {
        if (arg == null || arg.isBlank()) {
            return null;
        }
        String raw = arg.trim();
        String uuid = "";
        String name = raw;
        if (raw.regionMatches(true, 0, "uuid:", 0, 5)) {
            uuid = raw.substring(5).trim();
            name = "";
        }
        for (GuiBoardHelper.PendingInvite war : GuiBoardHelper.parsePendingInvites(
                ForgeBridge.sparPendingDojoWarCards(player))) {
            if (war == null || !war.incoming || !war.isDojoWar()) {
                continue;
            }
            if (!uuid.isBlank() && uuid.equalsIgnoreCase(war.uuid)) {
                return war;
            }
            if (!name.isBlank() && name.equalsIgnoreCase(war.name)) {
                return war;
            }
        }
        return null;
    }

    private static void openDojoChallengePicker(Player player) {
        CMIGui gui = base(player, "&8Declare Dojo War", 5);
        List<String> cards = ForgeBridge.sparRivalDojoCards(player);
        CMIGuiButton header = new CMIGuiButton(4, Material.DIAMOND_SWORD, "&c&lDeclare War");
        header.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add(cards.isEmpty() ? "&7No rival dojos ranked yet." : "&7" + cards.size() + " rival dojos");
        lore.addAll(GuiBoardHelper.tips(player, "&8Click to challenge", "&7Active wars earn 2× RP"));
        header.addLore(lore);
        gui.addButton(header);
        if (cards.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_dojo_rival", "&7No rivals yet"));
            empty.lockField();
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                String[] parts = cards.get(i).split("\t", -1);
                String uuid = parts.length > 0 ? parts[0] : "";
                String name = parts.length > 1 ? parts[1] : uuid;
                String rp = parts.length > 2 ? parts[2] : "0";
                ItemStack head;
                try {
                    head = GuiPlayerPicker.headByUuid(
                            java.util.UUID.fromString(uuid), name, "&f" + name,
                            List.of("&7Season RP &f" + rp, "&cChallenge to war"));
                } catch (IllegalArgumentException ex) {
                    head = GuiPlayerPicker.headByName(name, "&f" + name,
                            List.of("&7Season RP &f" + rp, "&cChallenge to war"));
                }
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.addCommand("lmdo spar dojo_challenge uuid:" + uuid + " dojo_war");
                gui.addButton(btn);
            }
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "dojo_war", "&7Dojo War"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openDojoMembers(Player player) {
        CMIGui gui = base(player, "&8Dojo Members", 5);
        List<String> cards = ForgeBridge.sparDojoMemberCards(player);
        List<String> headerLore = toAmp(ForgeBridge.sparLines(player, "dojo_members"));
        CMIGuiButton header = new CMIGuiButton(4, Material.PLAYER_HEAD, "&b&lDojo Members");
        header.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(headerLore.isEmpty() ? List.of("&7Season contributions from your dojo") : headerLore);
        header.addLore(lore);
        gui.addButton(header);
        if (cards.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_dojo_members", "&7No data yet"));
            empty.lockField();
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                String[] parts = cards.get(i).split("\t", -1);
                String uuid = parts.length > 0 ? parts[0] : "";
                String name = parts.length > 1 ? parts[1] : uuid;
                String rp = parts.length > 2 ? parts[2] : "0";
                ItemStack head;
                try {
                    head = GuiPlayerPicker.headByUuid(
                            java.util.UUID.fromString(uuid), name, "&f" + name,
                            List.of("&7" + rp + " RP contributed"));
                } catch (IllegalArgumentException ex) {
                    head = GuiPlayerPicker.headByName(name, "&f" + name,
                            List.of("&7" + rp + " RP contributed"));
                }
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                gui.addButton(btn);
            }
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "dojo_rank", "&7Dojo Rankings"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openDojoBannerPicker(Player player) {
        CMIGui gui = base(player, "&8Dojo Banner", 5);
        CMIGuiButton header = new CMIGuiButton(4, Material.WHITE_BANNER, "&f&lDojo Banner");
        header.lockField();
        header.addLore(List.of("", "&7Pick your dojo banner color", "&8Shows on rankings"));
        gui.addButton(header);
        String[] banners = {
                "WHITE_BANNER", "ORANGE_BANNER", "MAGENTA_BANNER", "LIGHT_BLUE_BANNER",
                "YELLOW_BANNER", "LIME_BANNER", "PINK_BANNER", "GRAY_BANNER",
                "LIGHT_GRAY_BANNER", "CYAN_BANNER", "PURPLE_BANNER", "BLUE_BANNER",
                "BROWN_BANNER", "GREEN_BANNER", "RED_BANNER", "BLACK_BANNER"
        };
        int[] slots = GuiBoardHelper.centeredSlots(banners.length);
        for (int i = 0; i < slots.length && i < banners.length; i++) {
            Material mat = Material.matchMaterial(banners[i]);
            if (mat == null) {
                mat = Material.WHITE_BANNER;
            }
            String label = banners[i].replace('_', ' ');
            gui.addButton(actionBtn(player, slots[i], "spar.dojo.banner_pick", mat,
                    "&f" + label, "dojo_banner", banners[i], "dojo_war",
                    List.of("&7Use this banner for your dojo", "", "&eClick to apply8Set as your dojo banner"),
                    Map.of("name", label)));
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "dojo_war", "&7Dojo War"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openAdmin(Player player) {
        CMIGui gui = base(player, "&8Spar Admin", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.COMMAND_BLOCK,
                GuiTooltips.name("spar.admin.header", "&c&lSpar Admin"));
        info.lockField();
        info.addLore(GuiTooltips.buttonLore("spar.admin.header", List.of(
                "&7Staff-only tools",
                "&8Save · status · mentor cooldown reset",
                "&8Player menus stay on the main Spar GUI")));
        gui.addButton(info);
        gui.addButton(actionBtn(player, 20, "spar.admin.save", Material.WRITABLE_BOOK, "&aSave",
                "admin", "save", "admin",
                List.of("&7Save sparring data to disk", "&8/spar admin save")));
        gui.addButton(actionBtn(player, 22, "spar.admin.status", Material.SPYGLASS, "&bStatus",
                "admin", "status", "admin",
                List.of("&7Enabled + path summary", "&8/spar admin status")));
        gui.addButton(actionBtn(player, 24, "spar.admin.resetcd", Material.EMERALD, "&eReset Mentor Cooldown",
                "admin", "resetcd", "admin",
                List.of("&7Clear your mentor change cooldown", "&8/spar admin mentor resetcd")));
        gui.addButton(pageBtn(player, 36, "spar.admin.back", Material.ARROW, "&7Back", "main",
                "&7Player Spar menu"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    /**
     * Mentor Actions hub — mirrors Rival Actions:
     * Invite · Ask · Pending · Leave · Release · Dojo.
     */
    private static void openMentor(Player player) {
        CMIGui gui = base(player, "&8Mentor Actions", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.EMERALD, "&b&lMentor Actions");
        info.lockField();
        info.addLore(toAmp(ForgeBridge.sparLines(player, "mentor")));
        gui.addButton(info);

        gui.addButton(pageBtn(player, 19, "spar.mentor.invite", Material.LIME_DYE, "&aInvite apprentice…",
                "pick_apprentice", "&7Pick a player to join your dojo"));
        gui.addButton(pageBtn(player, 20, "spar.mentor.ask", Material.LIGHT_BLUE_DYE, "&bAsk mentor…",
                "pick_mentor", "&7Pick a player to ask as your master"));
        Map<String, String> ph = ForgeBridge.sparPlaceholders(player);
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        gui.addButton(pageBtn(player, 21, "spar.mentor.pending", Material.CLOCK,
                pendingCount > 0 ? "&ePending &f(" + pendingCount + ")" : "&ePending",
                "pending",
                "&7Incoming + outgoing invites",
                pendingCount > 0 ? "&aClick to Accept / Decline" : "&8No pending invites"));
        boolean hasMentor = "true".equalsIgnoreCase(ph.getOrDefault("has_mentor", "false"));
        boolean hasApprentice = "true".equalsIgnoreCase(ph.getOrDefault("has_apprentice", "false"));
        String mentorName = blank(ph.get("mentor_name"), "your mentor");
        String apprenticeName = blank(ph.get("apprentice_name"), "your dojo");
        int appCount = 0;
        try {
            appCount = Integer.parseInt(ph.getOrDefault("apprentice_count", "0"));
        } catch (NumberFormatException ignored) {
            appCount = 0;
        }
        String appMax = blank(ph.get("apprentice_max"), "8");
        String dojoLabel = appCount > 0
                ? appCount + "/" + appMax + " · " + apprenticeName
                : apprenticeName;
        if (hasMentor) {
            gui.addButton(actionBtn(player, 23, "spar.mentor.leave", Material.RED_DYE, "&cLeave mentor",
                    "mentor_leave", "0", "mentor",
                    List.of("&7End bond with &f" + mentorName,
                            "&812-hour cooldown after leaving"),
                    Map.of("name", mentorName)));
        } else {
            String display = GuiTooltips.name("spar.mentor.leave_none", "&8Leave mentor");
            CMIGuiButton leaveOff = new CMIGuiButton(23, Material.GRAY_DYE, display);
            leaveOff.lockField();
            leaveOff.addLore(GuiTooltips.buttonLore("spar.mentor.leave_none",
                    List.of("&7You have no mentor")));
            gui.addButton(leaveOff);
        }
        if (hasApprentice) {
            gui.addButton(pageBtn(player, 24, "spar.mentor.release", Material.ORANGE_DYE, "&6Release…",
                    "pick_release",
                    Map.of("name", dojoLabel),
                    "&7Dojo &f" + dojoLabel,
                    "&7Pick who to release",
                    "&812-hour cooldown after releasing"));
        } else {
            String display = GuiTooltips.name("spar.mentor.release_none", "&8Release…");
            CMIGuiButton releaseOff = new CMIGuiButton(24, Material.GRAY_DYE, display);
            releaseOff.lockField();
            releaseOff.addLore(GuiTooltips.buttonLore("spar.mentor.release_none",
                    List.of("&7You have no apprentices")));
            gui.addButton(releaseOff);
        }
        if (hasMentor || hasApprentice) {
            List<String> dojoTip = new ArrayList<>();
            if (hasMentor) {
                dojoTip.add("&7Master &f" + mentorName);
                dojoTip.add("&7See mentor + apprentices");
            }
            if (hasApprentice) {
                dojoTip.add("&7Your dojo &f" + appCount + "&7/&f" + appMax);
                dojoTip.add("&8" + apprenticeName);
            }
            if (hasMentor && hasApprentice) {
                dojoTip.add("&eSwitch views inside Dojo");
            }
            gui.addButton(pageBtn(player, 25, "spar.mentor.dojo", Material.BOOKSHELF, "&bDojo",
                    "dojo",
                    Map.of("name", dojoLabel),
                    dojoTip.toArray(new String[0])));
        } else {
            String dojoName = GuiTooltips.name("spar.mentor.dojo", "&8Dojo", Map.of("name", ""));
            CMIGuiButton dojoOff = new CMIGuiButton(25, Material.GRAY_DYE, dojoName);
            dojoOff.lockField();
            dojoOff.addLore(GuiTooltips.buttonLore("spar.mentor.dojo",
                    List.of("&7Invite apprentices or ask a mentor", "&8Max &f" + appMax),
                    Map.of("name", ""), null));
            gui.addButton(dojoOff);
        }

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openPending(Player player) {
        CMIGui gui = base(player, "&8Pending Mentor Invites", 5);
        List<GuiBoardHelper.PendingInvite> invites = GuiBoardHelper.parsePendingInvites(
                ForgeBridge.sparPendingMentorInviteCards(player));
        CMIGuiButton info = new CMIGuiButton(4, Material.YELLOW_DYE, "&e&lPending Invites");
        info.lockField();
        List<String> pendingHeader = new ArrayList<>();
        pendingHeader.add("");
        pendingHeader.add(invites.isEmpty() ? "&7No pending invites." : "&7" + invites.size() + " pending");
        pendingHeader.addAll(GuiBoardHelper.tips(player,
                "&a◀ Incoming &7= click to Accept / Decline",
                "&6▶ Outgoing &7= click to cancel"));
        info.addLore(pendingHeader);
        gui.addButton(info);

        if (invites.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_pending", "&7No pending invites"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("spar.empty.no_pending", GuiBoardHelper.tipsList(player, List.of(
                    "&7Invite apprentice or ask a mentor",
                    "&7Incoming shows when they invite you"))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(invites.size(), 21));
            for (int i = 0; i < slots.length && i < invites.size(); i++) {
                GuiBoardHelper.PendingInvite invite = invites.get(i);
                ItemStack head = GuiBoardHelper.pendingInviteHead(player, invite);
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                if (invite.incoming) {
                    btn.addCommand("lmdo spar page pending_decide:" + invite.pickerArg());
                } else {
                    btn.addCommand("lmdo spar mentor_cancel " + invite.pickerArg() + " pending");
                }
                gui.addButton(btn);
            }
        }

        gui.addButton(pageBtn(player, 39, "spar.pending.nav_mentor", Material.EMERALD, "&bMentor", "mentor",
                "&7Mentor Actions"));
        gui.addButton(pageBtn(player, 36, "spar.pending.back", Material.ARROW, "&7Back", "mentor", "&7Mentor"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    /** Per-request Accept / Decline submenu — Rival pending_decide parity. */
    private static void openPendingDecide(Player player, String arg) {
        CMIGui gui = base(player, "&8Pending Mentor Request", 5);
        GuiBoardHelper.PendingInvite invite = findMentorPendingInvite(player, arg);
        String display = invite != null ? invite.name : (arg == null || arg.isBlank() ? "?" : arg.trim());
        if (display.regionMatches(true, 0, "uuid:", 0, 5)) {
            display = display.substring(5).trim();
        }
        String pickerArg = invite != null ? invite.pickerArg()
                : (arg == null || arg.isBlank() ? display : arg.trim());
        boolean theyAskYouMentor = invite != null && "mentor".equalsIgnoreCase(invite.kind);

        CMIGuiButton info = new CMIGuiButton(4, Material.YELLOW_DYE,
                GuiTooltips.name("spar.pending.decide_info", "&e&lRespond"));
        info.lockField();
        info.addLore(GuiTooltips.buttonLore("spar.pending.decide_info", List.of(
                "&7Invite from &f" + display,
                theyAskYouMentor
                        ? "&7They want you as their &bMentor"
                        : "&7They want you as their &aApprentice",
                "&aAccept &7→ create bond",
                "&cDecline &7→ refuse")));
        gui.addButton(info);

        ItemStack head = invite != null
                ? GuiBoardHelper.pendingInviteHead(player, invite)
                : new ItemStack(Material.PLAYER_HEAD);
        CMIGuiButton headBtn = new CMIGuiButton(13, head);
        headBtn.lockField();
        gui.addButton(headBtn);

        gui.addButton(actionBtn(player, 20, "spar.pending.accept", Material.LIME_DYE, "&aAccept",
                "mentor_accept", pickerArg, "pending",
                List.of("&7Accept " + display + "'s invite")));
        gui.addButton(actionBtn(player, 24, "spar.pending.decline", Material.ORANGE_DYE, "&cDecline",
                "mentor_decline", pickerArg, "pending",
                List.of("&7Decline " + display + "'s invite")));
        gui.addButton(pageBtn(player, 36, "spar.pending.decide_back", Material.ARROW, "&7Back", "pending",
                "&7Pending invites"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static GuiBoardHelper.PendingInvite findMentorPendingInvite(Player player, String arg) {
        if (arg == null || arg.isBlank()) {
            return null;
        }
        String raw = arg.trim();
        String uuid = "";
        String name = raw;
        if (raw.regionMatches(true, 0, "uuid:", 0, 5)) {
            uuid = raw.substring(5).trim();
            name = "";
        }
        for (GuiBoardHelper.PendingInvite invite : GuiBoardHelper.parsePendingInvites(
                ForgeBridge.sparPendingMentorInviteCards(player))) {
            if (invite == null || !invite.incoming) {
                continue;
            }
            if (!uuid.isBlank() && uuid.equalsIgnoreCase(invite.uuid)) {
                return invite;
            }
            if (!name.isBlank() && name.equalsIgnoreCase(invite.name)) {
                return invite;
            }
            if (raw.equalsIgnoreCase(invite.pickerArg())) {
                return invite;
            }
        }
        return null;
    }

    /**
     * Dojo roster — membership view (mentor + peers) and/or your own apprentices.
     * Pages: {@code dojo} (default), {@code dojo_member}, {@code dojo_mine}.
     */
    private static void openDojo(Player player, String page) {
        Map<String, String> ph = ForgeBridge.sparPlaceholders(player);
        boolean hasMentor = "true".equalsIgnoreCase(ph.getOrDefault("has_mentor", "false"));
        boolean hasApprentice = "true".equalsIgnoreCase(ph.getOrDefault("has_apprentice", "false"));
        String raw = page == null ? "dojo" : page.trim().toLowerCase(Locale.ROOT);
        boolean forceMine = "dojo_mine".equals(raw) || "dojo_own".equals(raw) || "apprentices".equals(raw);
        boolean forceMember = "dojo_member".equals(raw);
        boolean showMine = forceMine || (!forceMember && !hasMentor && hasApprentice);
        if (!forceMine && !forceMember) {
            showMine = !hasMentor && hasApprentice;
        }
        if (showMine && !hasApprentice && hasMentor) {
            showMine = false;
        }
        if (!showMine && !hasMentor && hasApprentice) {
            showMine = true;
        }

        CMIGui gui = base(player, showMine ? "&8My Dojo" : "&8Dojo Roster", 5);
        if (showMine) {
            fillOwnDojoCmi(gui, player, ph, hasMentor);
        } else {
            fillMemberDojoCmi(gui, player, ph, hasApprentice);
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "mentor", "&7Mentor"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void fillMemberDojoCmi(
            CMIGui gui, Player player, Map<String, String> ph, boolean hasOwnDojo
    ) {
        List<String> cards = ForgeBridge.sparMembershipDojoCards(player);
        String mentorName = blank(ph.get("mentor_name"), "Mentor");
        int peerCount = Math.max(0, cards.size() - 1);
        CMIGuiButton info = new CMIGuiButton(4, Material.BOOKSHELF, "&b&lDojo");
        info.lockField();
        List<String> header = new ArrayList<>();
        header.add("");
        header.add("&7Master &f" + mentorName);
        header.add(peerCount <= 0 ? "&7No apprentices listed yet" : "&7" + peerCount + " apprentice"
                + (peerCount == 1 ? "" : "s"));
        header.addAll(GuiBoardHelper.tips(player, "&8Mentor on top · apprentices below"));
        info.addLore(header);
        gui.addButton(info);

        String mentorCard = null;
        List<String> peers = new ArrayList<>();
        for (String card : cards) {
            String[] p = card.split("\t", 3);
            String role = p.length > 0 ? p[0] : "";
            if ("mentor".equalsIgnoreCase(role)) {
                mentorCard = card;
            } else {
                peers.add(card);
            }
        }
        if (mentorCard != null) {
            CMIGuiButton mentorBtn = new CMIGuiButton(13, dojoRoleHead(mentorCard));
            mentorBtn.lockField();
            gui.addButton(mentorBtn);
        }
        if (peers.isEmpty() && mentorCard == null) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_apprentice", "&7Not in a dojo"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("spar.empty.no_apprentice",
                    GuiBoardHelper.tipsList(player, List.of("&7Ask a mentor from Mentor Actions"))));
            gui.addButton(empty);
        } else if (!peers.isEmpty()) {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(peers.size(), 21));
            for (int i = 0; i < slots.length && i < peers.size(); i++) {
                CMIGuiButton btn = new CMIGuiButton(slots[i], dojoRoleHead(peers.get(i)));
                btn.lockField();
                gui.addButton(btn);
            }
        }
        if (hasOwnDojo) {
            gui.addButton(pageBtn(player, 39, "spar.mentor.dojo", Material.LIME_DYE, "&aMy Dojo",
                    "dojo_mine", "&7View apprentices you mentor"));
        }
    }

    private static void fillOwnDojoCmi(
            CMIGui gui, Player player, Map<String, String> ph, boolean hasMentor
    ) {
        List<String> cards = ForgeBridge.sparApprenticeCards(player);
        int appCount = cards.size();
        String appMax = blank(ph.get("apprentice_max"), "8");
        CMIGuiButton info = new CMIGuiButton(4, Material.BOOKSHELF, "&b&lMy Dojo");
        info.lockField();
        List<String> header = new ArrayList<>();
        header.add("");
        header.add(appCount <= 0 ? "&7No apprentices yet." : "&7" + appCount + "/" + appMax + " apprentices");
        header.add("&7You are the master of this dojo");
        header.addAll(GuiBoardHelper.tips(player, "&8Release from Mentor Actions"));
        info.addLore(header);
        gui.addButton(info);

        if (cards.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_apprentice", "&7Empty dojo"));
            empty.lockField();
            empty.addLore(GuiTooltips.buttonLore("spar.empty.no_apprentice",
                    GuiBoardHelper.tipsList(player, List.of("&7Invite apprentices from Mentor Actions"))));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(cards.size(), 21));
            for (int i = 0; i < slots.length && i < cards.size(); i++) {
                String card = cards.get(i);
                String[] parts = card.split("\t", 2);
                String uuid = parts.length > 0 ? parts[0] : "";
                String name = parts.length > 1 ? parts[1] : uuid;
                ItemStack head;
                try {
                    head = GuiPlayerPicker.headByUuid(
                            java.util.UUID.fromString(uuid), name, "&f" + name,
                            List.of("&7Your apprentice", "&8Release via Mentor → Release…"));
                } catch (IllegalArgumentException ex) {
                    head = GuiPlayerPicker.headByName(name, "&f" + name,
                            List.of("&7Your apprentice", "&8Release via Mentor → Release…"));
                }
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                gui.addButton(btn);
            }
        }
        if (appCount > 0) {
            String apprenticeName = blank(ph.get("apprentice_name"), "your dojo");
            String dojoLabel = appCount + "/" + appMax + " · " + apprenticeName;
            gui.addButton(pageBtn(player, 40, "spar.mentor.release", Material.ORANGE_DYE, "&6Release…",
                    "pick_release",
                    Map.of("name", dojoLabel),
                    "&7Dojo &f" + dojoLabel,
                    "&7Pick who to release",
                    "&812-hour cooldown after releasing"));
        }
        if (hasMentor) {
            gui.addButton(pageBtn(player, 39, "spar.mentor.dojo", Material.EMERALD, "&bTheir Dojo",
                    "dojo_member", "&7View your mentor's roster"));
        }
    }

    private static ItemStack dojoRoleHead(String card) {
        String[] p = card.split("\t", 3);
        String role = p.length > 0 ? p[0] : "apprentice";
        String uuid = p.length > 1 ? p[1] : "";
        String name = p.length > 2 ? p[2] : uuid;
        List<String> lore = new ArrayList<>();
        String title;
        if ("mentor".equalsIgnoreCase(role)) {
            title = "&6&lMentor &f" + name;
            lore.add("&6Dojo master");
        } else if ("you".equalsIgnoreCase(role)) {
            title = "&a&lYou &f" + name;
            lore.add("&aApprentice (you)");
        } else {
            title = "&f" + name;
            lore.add("&7Apprentice");
        }
        try {
            return GuiPlayerPicker.headByUuid(java.util.UUID.fromString(uuid), name, title, lore);
        } catch (IllegalArgumentException ex) {
            return GuiPlayerPicker.headByName(name, title, lore);
        }
    }

    private static void openPendingPicker(
            Player player, String action, String backPage, String title, String tip, boolean acceptMode) {
        CMIGui gui = base(player, "&8Spar Mentor", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, title);
        info.lockField();
        List<String> pendingPickHeader = new ArrayList<>();
        pendingPickHeader.add("");
        pendingPickHeader.add("&7Incoming mentor invites");
        pendingPickHeader.addAll(GuiBoardHelper.tips(player,
                "&8Click a head to " + (acceptMode ? "accept" : "decline")));
        info.addLore(pendingPickHeader);
        gui.addButton(info);

        List<String> pending = ForgeBridge.sparPendingIncomingMentorArgs(player);
        int placed = 0;
        for (String arg : pending) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            String display;
            ItemStack head;
            List<String> tipLore = new ArrayList<>(GuiBoardHelper.tips(player, tip));
            if (arg.regionMatches(true, 0, "uuid:", 0, 5)) {
                try {
                    java.util.UUID id = java.util.UUID.fromString(arg.substring(5).trim());
                    Player online = org.bukkit.Bukkit.getPlayer(id);
                    display = online != null ? online.getName() : arg.substring(5).trim();
                    if (online != null) {
                        tipLore.add("&aOnline");
                        head = GuiPlayerPicker.head(online, "&f" + display, tipLore);
                    } else {
                        head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                    }
                } catch (IllegalArgumentException e) {
                    display = arg;
                    head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                }
            } else {
                display = arg;
                Player online = org.bukkit.Bukkit.getPlayerExact(arg);
                if (online != null) {
                    tipLore.add("&aOnline");
                    head = GuiPlayerPicker.head(online, "&f" + display, tipLore);
                } else {
                    head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                }
            }
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("lmdo spar " + action + " " + arg + " " + backPage);
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_pending",
                            acceptMode ? "&eNothing to accept" : "&eNo pending invites"));
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiTooltips.lore("spar.empty.no_pending", GuiBoardHelper.tipsList(player,
                    List.of("&7When someone invites you,", "&7they appear here."))));
            empty.addLore(emptyLore);
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openReleasePicker(Player player) {
        CMIGui gui = base(player, "&8Release Apprentice", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.ORANGE_DYE, "&6&lRelease Apprentice");
        info.lockField();
        List<String> cards = ForgeBridge.sparApprenticeCards(player);
        List<String> header = new ArrayList<>();
        header.add("");
        header.add(cards.isEmpty() ? "&7No apprentices." : "&7" + cards.size() + " in your dojo");
        header.addAll(GuiBoardHelper.tips(player, "&8Click a head to release", "&812-hour cooldown"));
        info.addLore(header);
        gui.addButton(info);

        int placed = 0;
        for (String card : cards) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            String[] parts = card.split("\t", 2);
            String uuid = parts.length > 0 ? parts[0] : "";
            String name = parts.length > 1 ? parts[1] : uuid;
            List<String> tipLore = List.of("&cRelease &f" + name, "&812-hour cooldown");
            ItemStack head;
            try {
                java.util.UUID id = java.util.UUID.fromString(uuid);
                Player online = org.bukkit.Bukkit.getPlayer(id);
                if (online != null) {
                    head = GuiPlayerPicker.head(online, "&f" + name, tipLore);
                } else {
                    head = GuiPlayerPicker.headByUuid(id, name, "&f" + name, tipLore);
                }
            } catch (IllegalArgumentException e) {
                head = GuiPlayerPicker.headByName(name, "&f" + name, tipLore);
            }
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("lmdo spar mentor_release uuid:" + uuid + " mentor");
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&7No apprentices");
            empty.lockField();
            empty.addLore(List.of("", "&7Invite apprentices from the Mentor page"));
            gui.addButton(empty);
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "mentor", "&7Mentor"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openPicker(
            Player player, String action, String backPage, String title, String tip) {
        CMIGui gui = base(player, "&8Sparring", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, title);
        info.lockField();
        List<String> pickerHeader = new ArrayList<>();
        pickerHeader.add("");
        pickerHeader.add("&7Online players");
        pickerHeader.addAll(GuiBoardHelper.tips(player, "&8Click a head to confirm"));
        info.addLore(pickerHeader);
        gui.addButton(info);

        List<Player> online = GuiPlayerPicker.onlineExcept(player);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            ItemStack head = GuiPlayerPicker.head(other, "&f" + other.getName(),
                    GuiBoardHelper.tips(player, tip));
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("lmdo spar " + action + " uuid:" + other.getUniqueId() + " " + backPage);
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_online", "&cNo one online"));
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiTooltips.lore("spar.empty.no_online",
                    GuiBoardHelper.tips(player, "&7Other players must be online")));
            empty.addLore(emptyLore);
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openDetail(Player player, String page, String title, Material mat) {
        CMIGui gui = base(player, "&8Sparring", 5);
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        List<String> detailHeader = new ArrayList<>();
        detailHeader.add("");
        detailHeader.addAll(GuiBoardHelper.tips(player,
                page.equals("stats") || page.equals("statistics")
                        ? "&7One book per spar · hover for full report"
                        : "&7One item per entry",
                "&8Centered below"));
        header.addLore(detailHeader);
        gui.addButton(header);
        List<String> lore = toAmp(ForgeBridge.sparLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.");
        }
        List<GuiBoardHelper.DetailTile> tiles = GuiBoardHelper.detailTiles(lore);
        int[] slots = GuiBoardHelper.centeredSlots(Math.min(tiles.size(), 21));
        for (int i = 0; i < slots.length && i < tiles.size(); i++) {
            GuiBoardHelper.DetailTile tile = tiles.get(i);
            CMIGuiButton btn = new CMIGuiButton(slots[i], tile.icon, tile.title);
            btn.lockField();
            List<String> tip = new ArrayList<>();
            tip.add("");
            tip.addAll(tile.lore);
            btn.addLore(tip);
            gui.addButton(btn);
        }
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openLines(Player player, String page, String title, Material mat) {
        openDetail(player, page, title, mat);
    }

    private static List<String> statusLore(Player player, Map<String, String> ph) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if ("true".equalsIgnoreCase(ph.getOrDefault("sessionActive", "false"))) {
            lore.add("&aSession ACTIVE &8with &f" + blank(ph.get("partner"), "?")
                    + "  &7TP &f" + ph.getOrDefault("session_tp", "0"));
            if ("true".equalsIgnoreCase(ph.get("perfect"))) {
                lore.add("&6&lPERFECT TRAINING");
            }
        } else {
            lore.add("&7No active spar");
            lore.addAll(GuiBoardHelper.tips(player, "&7Trade hits within 30 blocks to start."));
        }
        if ("true".equalsIgnoreCase(ph.getOrDefault("mentor_bonded", "false"))) {
            String role = ph.getOrDefault("mentor_role", "?");
            if ("both".equalsIgnoreCase(role)) {
                lore.add("&bMentor &f" + blank(ph.get("mentor_name"), "?")
                        + " &8· &bApprentice &f" + blank(ph.get("apprentice_name"), "?")
                        + "  &7streak &f" + ph.getOrDefault("streak", "0"));
            } else if ("mentor".equalsIgnoreCase(role)) {
                lore.add("&bMentoring &f" + blank(ph.get("apprentice_name"), "?")
                        + " &8(" + ph.getOrDefault("apprentice_count", "?")
                        + "/" + ph.getOrDefault("apprentice_max", "8") + ")"
                        + "  &7streak &f" + ph.getOrDefault("streak", "0"));
            } else {
                lore.add("&bApprentice of &f" + blank(ph.get("mentor_name"), "?")
                        + "  &7streak &f" + ph.getOrDefault("streak", "0"));
            }
        } else {
            lore.add("&7No mentor bond");
            lore.addAll(GuiBoardHelper.tips(player, "&8Use Mentor page to invite"));
        }
        if (ForgeBridge.isStaff(player)) {
            lore.add("");
            lore.add("&8Stats · Top · Mentor");
        }
        return lore;
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable",
                    "&7Check mods/ for LegacyMechanics-*.jar");
        }
        return List.of("", "&cSparring system is disabled", "&7Ask an admin if you need access");
    }

    private static String blank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String dojoSortLabel(String cat) {
        if (cat == null) {
            return "Season RP";
        }
        return switch (cat.toLowerCase(Locale.ROOT)) {
            case "wins", "win" -> "Wins";
            case "tp" -> "Spar TP";
            default -> "Season RP";
        };
    }

    private static List<String> toAmp(List<String> lines) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            return out;
        }
        for (String line : lines) {
            out.add(line == null ? "" : line.replace('§', '&'));
        }
        return out;
    }

    private static void fillEmpty(CMIGui gui, int rows) {
        int size = rows * 9;
        Map<Integer, CMIGuiButton> existing = gui.getButtons();
        for (int i = 0; i < size; i++) {
            if (existing != null && existing.containsKey(i)) {
                continue;
            }
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            CMIGuiButton pane = new CMIGuiButton(i, edge ? ACCENT : FILL, " ");
            pane.lockField();
            gui.addButton(pane);
        }
    }

    private static CMIGui base(Player player, String title, int rows) {
        CMIGui gui = new CMIGui(player);
        gui.setTitle(title);
        gui.setInvSize(rows);
        gui.addLock(InvType.Gui);
        return gui;
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, Material mat, String name, String action, String arg, String returnPage,
            List<String> tip) {
        return actionBtn(player, slot, null, mat, name, action, arg, returnPage, tip, null);
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, String key, Material mat, String name, String action, String arg,
            String returnPage, List<String> tip
    ) {
        return actionBtn(player, slot, key, mat, name, action, arg, returnPage, tip, null);
    }

    private static CMIGuiButton actionBtn(
            Player player, int slot, String key, Material mat, String name, String action, String arg,
            String returnPage, List<String> tip, Map<String, String> vars
    ) {
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name, vars);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(tip)
                : GuiTooltips.buttonLore(key, tip, vars, null));
        btn.addCommand("lmdo spar " + action + " " + arg + " " + returnPage);
        return btn;
    }

    private static CMIGuiButton pageBtn(Player player, int slot, Material mat, String name, String page, String... tips) {
        return pageBtn(player, slot, null, mat, name, page, null, tips);
    }

    private static CMIGuiButton pageBtn(
            Player player, int slot, String key, Material mat, String name, String page, String... tips
    ) {
        return pageBtn(player, slot, key, mat, name, page, null, tips);
    }

    private static CMIGuiButton pageBtn(
            Player player, int slot, String key, Material mat, String name, String page,
            Map<String, String> vars, String... tips
    ) {
        List<String> defaults = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip != null) {
                    defaults.add(tip);
                }
            }
        }
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name, vars);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(defaults)
                : GuiTooltips.buttonLore(key, defaults, vars, null));
        btn.addCommand("lmdo spar page " + page);
        return btn;
    }

    private static List<String> withBlank(List<String> tip) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (tip != null) {
            lore.addAll(tip);
        }
        return lore;
    }

    private static CMIGuiButton cmdBtn(int slot, Material mat, String name, String command, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
        btn.addCommand(command);
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton hubBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.COMPASS, "&7« Hub");
        btn.lockField();
        btn.addCommand("lmdo lm open hub");
        btn.setCloseInv(true);
        return btn;
    }

    private static CMIGuiButton closeBtn(int slot) {
        CMIGuiButton btn = new CMIGuiButton(slot, Material.BARRIER, "&cClose");
        btn.lockField();
        btn.setCloseInv(true);
        return btn;
    }
}
