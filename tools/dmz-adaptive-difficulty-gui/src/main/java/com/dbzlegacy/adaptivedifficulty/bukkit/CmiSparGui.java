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
 * Pages: main · stats · top · mentor · pending · pick_apprentice · pick_mentor · pick_accept/decline.
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
            if (p.startsWith("top_") || p.startsWith("top ") || "top".equals(p) || "leaderboard".equals(p)) {
                openTop(player, p);
            } else if ("stats".equals(p) || "statistics".equals(p)) {
                openDetail(player, "stats", "&eLast 3 Spar Reports", Material.WRITTEN_BOOK);
            } else if ("mentor".equals(p)) {
                openMentor(player);
            } else if ("pending".equals(p) || "invites".equals(p) || "pendinginvites".equals(p)) {
                openPending(player);
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
        gui.addButton(pageBtn(player, 21, "spar.main.top", Material.GOLDEN_HELMET, "&fTop", "top",
                "&7Leaderboard"));
        gui.addButton(pageBtn(player, 23, "spar.main.mentor", Material.EMERALD, "&bMentor", "mentor",
                "&7Invite · pending · accept · remove"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "true"));
        gui.addButton(actionBtn(player, 25,
                tpOn ? "spar.main.tpmsg_on" : "spar.main.tpmsg_off",
                tpOn ? Material.BELL : Material.GRAY_DYE,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                "tpmsg", "toggle", "main",
                List.of(
                        tpOn ? "&7Click to mute spar TP chat" : "&7Click to show spar TP chat while fighting",
                        "&8Batched +TP lines during spars",
                        "",
                        "&eClick to toggle"
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
                    "&7Save · status · mentor resetcd"));
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

    private static void openAdmin(Player player) {
        CMIGui gui = base(player, "&8Spar Admin", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.COMMAND_BLOCK, "&c&lSpar Admin");
        info.lockField();
        info.addLore(List.of("", "&7Staff-only tools",
                "&8Save · status · mentor resetcd",
                "&8Player menus stay on the main Spar GUI"));
        gui.addButton(info);
        gui.addButton(actionBtn(player, 20, "spar.admin.save", Material.WRITABLE_BOOK, "&aSave",
                "admin", "save", "admin",
                List.of("&7Write sparring.json", "&8/spar admin save")));
        gui.addButton(actionBtn(player, 22, "spar.admin.status", Material.SPYGLASS, "&bStatus",
                "admin", "status", "admin",
                List.of("&7Enabled + path", "&8/spar admin status")));
        gui.addButton(actionBtn(player, 24, "spar.admin.resetcd", Material.EMERALD, "&eReset Mentor CD",
                "admin", "resetcd", "admin",
                List.of("&7Clear your mentor cooldown", "&8/spar admin mentor resetcd")));
        gui.addButton(pageBtn(player, 36, "spar.admin.back", Material.ARROW, "&7Back", "main",
                "&7Player Spar menu"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openMentor(Player player) {
        CMIGui gui = base(player, "&8Spar Mentor", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.EMERALD, "&b&lMentor");
        info.lockField();
        info.addLore(toAmp(ForgeBridge.sparLines(player, "mentor")));
        gui.addButton(info);

        gui.addButton(pageBtn(player, 19, "spar.mentor.invite", Material.LIME_DYE, "&aInvite apprentice…",
                "pick_apprentice", "&7Pick a player to mentor"));
        gui.addButton(pageBtn(player, 20, "spar.mentor.ask", Material.LIGHT_BLUE_DYE, "&bAsk mentor…",
                "pick_mentor", "&7Pick a player to ask as mentor"));
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
                "&7View incoming + outgoing invites",
                pendingCount > 0 ? "&aYou have pending invites" : "&8No pending invites"));
        gui.addButton(pageBtn(player, 22, "spar.mentor.accept", Material.YELLOW_DYE, "&eAccept…", "pick_accept",
                "&7Accept an incoming mentor invite"));
        gui.addButton(pageBtn(player, 23, "spar.mentor.decline", Material.ORANGE_DYE, "&6Decline…", "pick_decline",
                "&7Decline an incoming mentor invite"));
        boolean hasMentor = "true".equalsIgnoreCase(ph.getOrDefault("has_mentor", "false"));
        boolean hasApprentice = "true".equalsIgnoreCase(ph.getOrDefault("has_apprentice", "false"));
        String mentorName = blank(ph.get("mentor_name"), "?");
        String apprenticeName = blank(ph.get("apprentice_name"), "?");
        if (hasMentor) {
            gui.addButton(actionBtn(player, 24, "spar.mentor.leave", Material.RED_DYE, "&cLeave mentor",
                    "mentor_leave", "0", "mentor",
                    List.of("&7End bond with &f" + mentorName,
                            "&87-day cooldown after leaving"),
                    Map.of("name", mentorName)));
        } else {
            String display = GuiTooltips.name("spar.mentor.leave", "&8Leave mentor");
            CMIGuiButton leaveOff = new CMIGuiButton(24, Material.GRAY_DYE, display);
            leaveOff.lockField();
            leaveOff.addLore(GuiTooltips.buttonLore("spar.mentor.leave", List.of("&7You have no mentor"),
                    Map.of("name", mentorName), null));
            gui.addButton(leaveOff);
        }
        if (hasApprentice) {
            gui.addButton(actionBtn(player, 25, "spar.mentor.release", Material.ORANGE_DYE, "&6Release apprentice",
                    "mentor_release", "0", "mentor",
                    List.of("&7End bond with &f" + apprenticeName,
                            "&87-day cooldown after releasing"),
                    Map.of("name", apprenticeName)));
        } else {
            String display = GuiTooltips.name("spar.mentor.release", "&8Release apprentice");
            CMIGuiButton releaseOff = new CMIGuiButton(25, Material.GRAY_DYE, display);
            releaseOff.lockField();
            releaseOff.addLore(GuiTooltips.buttonLore("spar.mentor.release", List.of("&7You have no apprentice"),
                    Map.of("name", apprenticeName), null));
            gui.addButton(releaseOff);
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
                "&a◀ Incoming &7= they invited you",
                "&6▶ Outgoing &7= waiting on them"));
        info.addLore(pendingHeader);
        gui.addButton(info);

        if (invites.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER,
                    GuiTooltips.name("spar.empty.no_pending", "&7No pending invites"));
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiTooltips.lore("spar.empty.no_pending", GuiBoardHelper.tipsList(player, List.of(
                    "&7Invite apprentice or ask a mentor",
                    "&7Incoming shows when they invite you"))));
            empty.addLore(emptyLore);
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(invites.size(), 21));
            for (int i = 0; i < slots.length && i < invites.size(); i++) {
                GuiBoardHelper.PendingInvite invite = invites.get(i);
                ItemStack head = GuiBoardHelper.pendingInviteHead(player, invite);
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                if (invite.incoming) {
                    btn.addCommand("lmdo spar mentor_accept " + invite.pickerArg() + " pending");
                } else {
                    btn.addCommand("lmdo spar mentor_cancel " + invite.pickerArg() + " pending");
                }
                gui.addButton(btn);
            }
        }

        gui.addButton(pageBtn(player, 37, "spar.pending.accept_pick", Material.YELLOW_DYE, "&eAccept…",
                "pick_accept", "&7Accept an incoming invite"));
        gui.addButton(pageBtn(player, 38, "spar.pending.decline_pick", Material.ORANGE_DYE, "&6Decline…",
                "pick_decline", "&7Decline an incoming invite"));
        gui.addButton(pageBtn(player, 39, "spar.pending.nav_mentor", Material.EMERALD, "&bMentor", "mentor",
                "&7Full mentor menu"));
        gui.addButton(pageBtn(player, 36, "spar.pending.back", Material.ARROW, "&7Back", "mentor", "&7Mentor"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
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
        return pageBtn(player, slot, null, mat, name, page, tips);
    }

    private static CMIGuiButton pageBtn(
            Player player, int slot, String key, Material mat, String name, String page, String... tips
    ) {
        List<String> defaults = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip != null) {
                    defaults.add(tip);
                }
            }
        }
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(defaults)
                : GuiTooltips.buttonLore(key, defaults));
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
