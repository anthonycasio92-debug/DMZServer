package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** Bukkit chest GUI fallback — Legacy Mechanics Sparring. */
public final class SparChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private final AdaptiveDifficultyGuiPlugin plugin;

    public SparChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }


    private static boolean inspecting(Player viewer, Player subject) {
        return viewer != null && subject != null
                && !viewer.getUniqueId().equals(subject.getUniqueId());
    }

    private static String invTitle(Player viewer, Player subject, String base) {
        if (inspecting(viewer, subject)) {
            return color(base + " · &c" + subject.getName());
        }
        return color(base);
    }

    public void open(Player player, String page) {
        Player viewer = player;
        Player subject = AdminInspectSessions.resolveSubject(viewer);
        String raw = page == null || page.isBlank() ? "main" : page.trim();
        String p = raw.toLowerCase(Locale.ROOT);
        Inventory inv;
        if (p.startsWith("pending_decide:")) {
            inv = pendingDecide(viewer, subject, raw.substring("pending_decide:".length()).trim());
        } else if (p.startsWith("top_") || p.startsWith("top ") || "top".equals(p) || "leaderboard".equals(p)) {
            inv = top(viewer, subject, p);
        } else if ("stats".equals(p) || "statistics".equals(p)) {
            inv = detailBoard(viewer, subject, "stats", "&eLast 3 Spar Reports", Material.WRITTEN_BOOK);
        } else if ("mentor".equals(p) || "actions".equals(p)) {
            inv = mentor(viewer, subject);
        } else if ("pending".equals(p) || "invites".equals(p) || "pendinginvites".equals(p)) {
            inv = pending(viewer, subject);
        } else if ("dojo".equals(p) || "roster".equals(p) || "apprentices".equals(p)) {
            inv = dojo(viewer, subject);
        } else if ("pick_apprentice".equals(p)) {
            inv = picker(viewer, subject, "mentor_invite", "mentor",
                    "&aInvite Apprentice", "&7Ask them to be your apprentice");
        } else if ("pick_mentor".equals(p)) {
            inv = picker(viewer, subject, "apprentice_invite", "mentor",
                    "&bAsk Mentor", "&7Ask them to be your mentor");
        } else if ("pick_accept".equals(p)) {
            // Legacy deep-link — prefer pending_decide from Pending board.
            inv = pendingPicker(viewer, subject, "mentor_accept", "pending",
                    "&aAccept Invite", "&7Accept this mentor invite", true);
        } else if ("pick_decline".equals(p)) {
            inv = pendingPicker(viewer, subject, "mentor_decline", "pending",
                    "&cDecline Invite", "&7Decline this mentor invite", false);
        } else if ("pick_release".equals(p)) {
            inv = releasePicker(viewer, subject);
        } else if ("help".equals(p)) {
            inv = main(viewer, subject);
        } else if ("admin".equals(p)) {
            inv = ForgeBridge.isStaff(viewer) ? admin(viewer, subject) : main(viewer, subject);
        } else {
            inv = main(viewer, subject);
        }
        GuiFeedback.openChest(viewer, inv);
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.sparPlaceholders(subject);
        Holder holder = new Holder("main");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Sparring"));
        holder.bind(inv);
        frame(inv, 45);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        if (!bridgeOk || !systemOn) {
            put(holder, inv, 4, item(Material.NETHER_STAR,
                    !bridgeOk ? "&c&lUNAVAILABLE" : "&c&lSPARRING DISABLED",
                    unavailableLore(bridgeOk)));
            put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        put(holder, inv, 4, item(Material.GOLDEN_SWORD, "&b&lSparring", statusLore(viewer, ph)));
        // Main: Status · Stats · Top · Mentor · End Session · Hub · Close (pickers on Mentor only)
        put(holder, inv, 19, tipBtn(viewer, "spar.main.stats", Material.BOOK, "&eStats",
                        List.of("&7Last 3 spar reports", "&8One item per spar")),
                SlotAction.page("stats"));
        put(holder, inv, 21, tipBtn(viewer, "spar.main.top", Material.GOLDEN_HELMET, "&fTop",
                List.of("&7Leaderboard")),
                SlotAction.page("top"));
        put(holder, inv, 23, tipBtn(viewer, "spar.main.mentor", Material.EMERALD, "&bMentor",
                List.of("&7Invite · Pending · Dojo · Leave / Release")), SlotAction.page("mentor"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "true"));
        put(holder, inv, 25, tipBtn(viewer,
                tpOn ? "spar.main.tpmsg_on" : "spar.main.tpmsg_off",
                tpOn ? Material.BELL : Material.GRAY_DYE,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                List.of(
                        tpOn ? "&7Click to mute spar TP chat" : "&7Click to show spar TP chat while fighting",
                        "&8Players: +TP (style)",
                        "&8Staff: full bonus / stack detail",
                        "",
                        "&eClick to toggle"
                )), SlotAction.act("tpmsg", "toggle", "main"));

        boolean session = "true".equalsIgnoreCase(ph.getOrDefault("sessionActive", "false"));
        if (session) {
            put(holder, inv, 31, tipBtn(viewer, "spar.main.end_session", Material.RED_DYE, "&cEnd Session",
                    List.of("&7End your active spar session")),
                    SlotAction.act("end", "0", "main"));
        }

        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        if (ForgeBridge.isStaff(viewer)) {
            put(holder, inv, 37, tipBtn(viewer, "spar.main.admin", Material.COMMAND_BLOCK, "&cAdmin",
                    List.of("&7Save · status · mentor resetcd")), SlotAction.page("admin"));
        }
        return inv;
    }

    private Inventory top(Player viewer, Player subject, String pageKey) {
        String cat = "tp";
        String lower = pageKey.toLowerCase(Locale.ROOT);
        if (lower.startsWith("top_")) {
            cat = lower.substring(4).trim();
        } else if (lower.startsWith("top ")) {
            cat = lower.substring(4).trim();
        }
        if (cat.isBlank() || "top".equals(cat) || "leaderboard".equals(cat)) {
            cat = "tp";
        }
        Holder holder = new Holder("top_" + cat);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Sparring"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> raw = toAmp(ForgeBridge.sparLines(subject, "top_" + cat));
        List<GuiBoardHelper.TopEntry> entries = GuiBoardHelper.parseTopEntries(raw);
        List<String> topHeader = new ArrayList<>();
        topHeader.add("");
        topHeader.addAll(GuiBoardHelper.tips(viewer,
                "&7Sparring leaderboard", "&8Player heads below · categories on bottom"));
        put(holder, inv, 4, item(Material.GOLDEN_HELMET, "&f&lTop — " + cat, topHeader));
        if (entries.isEmpty()) {
            put(holder, inv, 13, tipBtn(viewer, "spar.empty.no_top", Material.BARRIER, "&7No sparring data yet",
                    List.of("&7Spar nearby to earn TP", "&8Categories: TP · Sessions · Perfect")));
        } else {
            // Heads on rows 1–2 only; category buttons sit on row 3 (29/31/33).
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(entries.size(), 14));
            for (int i = 0; i < slots.length && i < entries.size(); i++) {
                put(holder, inv, slots[i], GuiBoardHelper.topHead(entries.get(i)));
            }
        }
        put(holder, inv, 29, pageBtn(viewer, "spar.top.tp", Material.GOLD_INGOT, "&eTP", "&7Total TP"),
                SlotAction.page("top_tp"));
        put(holder, inv, 31, pageBtn(viewer, "spar.top.sessions", Material.CLOCK, "&aSessions", "&7Sessions"),
                SlotAction.page("top_sessions"));
        put(holder, inv, 33, pageBtn(viewer, "spar.top.perfect", Material.NETHER_STAR, "&bPerfect",
                "&7Perfect spars"), SlotAction.page("top_perfect"));
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory detailBoard(Player viewer, Player subject, String page, String title, Material mat) {
        Holder holder = new Holder(page);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Sparring"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> lore = toAmp(ForgeBridge.sparLines(subject, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.");
        }
        List<String> detailHeader = new ArrayList<>();
        detailHeader.add("");
        if ("stats".equals(page) || "statistics".equals(page)) {
            detailHeader.addAll(GuiBoardHelper.tips(viewer,
                    "&7One book per spar · hover for full report", "&8Centered below"));
        } else {
            detailHeader.addAll(GuiBoardHelper.tips(viewer, "&7One item per entry", "&8Centered below"));
        }
        put(holder, inv, 4, item(mat, title, detailHeader));
        List<GuiBoardHelper.DetailTile> tiles = GuiBoardHelper.detailTiles(lore);
        int[] slots = GuiBoardHelper.centeredSlots(Math.min(tiles.size(), 21));
        for (int i = 0; i < slots.length && i < tiles.size(); i++) {
            GuiBoardHelper.DetailTile tile = tiles.get(i);
            List<String> tip = new ArrayList<>();
            tip.add("");
            tip.addAll(tile.lore);
            put(holder, inv, slots[i], item(tile.icon, tile.title, tip));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory admin(Player viewer, Player subject) {
        Holder holder = new Holder("admin");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Spar Admin"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.COMMAND_BLOCK, "&c&lSpar Admin",
                List.of("", "&7Staff-only tools",
                        "&8Save · status · mentor resetcd",
                        "&8Player menus stay on the main Spar GUI")));
        put(holder, inv, 20, tipBtn(viewer, "spar.admin.save", Material.WRITABLE_BOOK, "&aSave",
                List.of("&7Write sparring.json", "&8/spar admin save")),
                SlotAction.act("admin", "save", "admin"));
        put(holder, inv, 22, tipBtn(viewer, "spar.admin.status", Material.SPYGLASS, "&bStatus",
                List.of("&7Enabled + path", "&8/spar admin status")),
                SlotAction.act("admin", "status", "admin"));
        put(holder, inv, 24, tipBtn(viewer, "spar.admin.resetcd", Material.EMERALD, "&eReset Mentor CD",
                List.of("&7Clear your mentor cooldown", "&8/spar admin mentor resetcd")),
                SlotAction.act("admin", "resetcd", "admin"));
        put(holder, inv, 36, pageBtn(viewer, "spar.admin.back", Material.ARROW, "&7Back", "&7Player Spar menu"),
                SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /**
     * Mentor Actions hub — mirrors Rival Actions:
     * Invite · Ask · Pending · Leave · Release · Dojo.
     */
    private Inventory mentor(Player viewer, Player subject) {
        Holder holder = new Holder("mentor");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Mentor Actions"));
        holder.bind(inv);
        frame(inv, 45);
        put(holder, inv, 4, item(Material.EMERALD, "&b&lMentor Actions",
                prependBlank(toAmp(ForgeBridge.sparLines(subject, "mentor")))));
        put(holder, inv, 19, pageBtn(viewer, "spar.mentor.invite", Material.LIME_DYE, "&aInvite apprentice…",
                "&7Pick a player to join your dojo"), SlotAction.page("pick_apprentice"));
        put(holder, inv, 20, pageBtn(viewer, "spar.mentor.ask", Material.LIGHT_BLUE_DYE, "&bAsk mentor…",
                "&7Pick a player to ask as your master"), SlotAction.page("pick_mentor"));
        Map<String, String> ph = ForgeBridge.sparPlaceholders(subject);
        int pendingCount = 0;
        try {
            pendingCount = Integer.parseInt(ph.getOrDefault("pending_invites", "0"));
        } catch (NumberFormatException ignored) {
            pendingCount = 0;
        }
        put(holder, inv, 21, pageBtn(viewer, "spar.mentor.pending", Material.CLOCK,
                pendingCount > 0 ? "&ePending &f(" + pendingCount + ")" : "&ePending",
                "&7Incoming + outgoing invites",
                pendingCount > 0 ? "&aClick to Accept / Decline" : "&8No pending invites"),
                SlotAction.page("pending"));
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
            put(holder, inv, 23, tipBtn(viewer, "spar.mentor.leave", Material.RED_DYE, "&cLeave mentor",
                    List.of("&7End bond with &f" + mentorName,
                            "&812-hour cooldown after leaving"),
                    Map.of("name", mentorName)),
                    SlotAction.act("mentor_leave", "0", "mentor"));
        } else {
            put(holder, inv, 23, tipBtn(viewer, "spar.mentor.leave_none", Material.GRAY_DYE, "&8Leave mentor",
                    List.of("&7You have no mentor")));
        }
        if (hasApprentice) {
            put(holder, inv, 24, tipBtn(viewer, "spar.mentor.release", Material.ORANGE_DYE, "&6Release…",
                    List.of("&7Dojo &f" + dojoLabel,
                            "&7Pick who to release",
                            "&812-hour cooldown after releasing"),
                    Map.of("name", dojoLabel)),
                    SlotAction.page("pick_release"));
            put(holder, inv, 25, tipBtn(viewer, "spar.mentor.dojo", Material.BOOKSHELF, "&bDojo",
                    List.of("&7View your apprentices",
                            "&f" + appCount + "&7/&f" + appMax,
                            "&8" + apprenticeName),
                    Map.of("name", dojoLabel)),
                    SlotAction.page("dojo"));
        } else {
            put(holder, inv, 24, tipBtn(viewer, "spar.mentor.release_none", Material.GRAY_DYE, "&8Release…",
                    List.of("&7You have no apprentices")));
            put(holder, inv, 25, tipBtn(viewer, "spar.mentor.dojo", Material.GRAY_DYE, "&8Dojo",
                    List.of("&7Invite apprentices to fill your dojo",
                            "&8Max &f" + appMax),
                    Map.of("name", "")));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory pending(Player viewer, Player subject) {
        Holder holder = new Holder("pending");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Pending Mentor Invites"));
        holder.bind(inv);
        frame(inv, 45);
        List<GuiBoardHelper.PendingInvite> invites = GuiBoardHelper.parsePendingInvites(
                ForgeBridge.sparPendingMentorInviteCards(subject));
        List<String> pendingHeader = new ArrayList<>();
        pendingHeader.add("");
        pendingHeader.add(invites.isEmpty() ? "&7No pending invites." : "&7" + invites.size() + " pending");
        pendingHeader.addAll(GuiBoardHelper.tips(viewer,
                "&a◀ Incoming &7= click to Accept / Decline",
                "&6▶ Outgoing &7= click to cancel"));
        put(holder, inv, 4, item(Material.YELLOW_DYE, "&e&lPending Invites", pendingHeader));
        if (invites.isEmpty()) {
            put(holder, inv, 22, tipBtn(viewer, "spar.empty.no_pending", Material.BARRIER, "&7No pending invites",
                    List.of("&7Invite apprentice or ask a mentor",
                            "&7Incoming shows when they invite you")));
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(invites.size(), 21));
            for (int i = 0; i < slots.length && i < invites.size(); i++) {
                GuiBoardHelper.PendingInvite invite = invites.get(i);
                ItemStack head = GuiBoardHelper.pendingInviteHead(viewer, invite);
                if (invite.incoming) {
                    put(holder, inv, slots[i], head,
                            SlotAction.page("pending_decide:" + invite.pickerArg()));
                } else {
                    put(holder, inv, slots[i], head,
                            SlotAction.act("mentor_cancel", invite.pickerArg(), "pending"));
                }
            }
        }
        put(holder, inv, 39, pageBtn(viewer, "spar.pending.nav_mentor", Material.EMERALD, "&bMentor",
                "&7Mentor Actions"), SlotAction.page("mentor"));
        put(holder, inv, 36, pageBtn(viewer, "spar.pending.back", Material.ARROW, "&7Back", "&7Mentor"),
                SlotAction.page("mentor"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Per-request Accept / Decline submenu — Rival pending_decide parity. */
    private Inventory pendingDecide(Player viewer, Player subject, String arg) {
        Holder holder = new Holder("pending_decide");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Pending Mentor Request"));
        holder.bind(inv);
        frame(inv, 45);
        GuiBoardHelper.PendingInvite invite = findMentorPendingInvite(subject, arg);
        String display = invite != null ? invite.name : (arg == null || arg.isBlank() ? "?" : arg);
        if (display.regionMatches(true, 0, "uuid:", 0, 5)) {
            display = display.substring(5).trim();
        }
        String pickerArg = invite != null ? invite.pickerArg()
                : (arg == null || arg.isBlank() ? display : arg.trim());
        ItemStack head = invite != null
                ? GuiBoardHelper.pendingInviteHead(viewer, invite)
                : item(Material.PLAYER_HEAD, "&f" + display, List.of("&7Pending mentor invite"));
        put(holder, inv, 13, head);
        boolean theyAskYouMentor = invite != null && "mentor".equalsIgnoreCase(invite.kind);
        String roleLine = theyAskYouMentor
                ? "&7They want you as their &bMentor"
                : "&7They want you as their &aApprentice";
        put(holder, inv, 4, tipBtn(viewer, "spar.pending.decide_info", Material.YELLOW_DYE, "&e&lRespond",
                List.of("&7Invite from &f" + display,
                        roleLine,
                        "&aAccept &7→ create bond",
                        "&cDecline &7→ refuse")));
        put(holder, inv, 20, tipBtn(viewer, "spar.pending.accept", Material.LIME_DYE, "&aAccept",
                List.of("&7Accept " + display + "'s invite")),
                SlotAction.act("mentor_accept", pickerArg, "pending"));
        put(holder, inv, 24, tipBtn(viewer, "spar.pending.decline", Material.ORANGE_DYE, "&cDecline",
                List.of("&7Decline " + display + "'s invite")),
                SlotAction.act("mentor_decline", pickerArg, "pending"));
        put(holder, inv, 36, pageBtn(viewer, "spar.pending.decide_back", Material.ARROW, "&7Back",
                "&7Pending invites"), SlotAction.page("pending"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static GuiBoardHelper.PendingInvite findMentorPendingInvite(Player subject, String arg) {
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
                ForgeBridge.sparPendingMentorInviteCards(subject))) {
            if (invite == null || !invite.incoming) {
                continue;
            }
            if (!uuid.isBlank() && uuid.equalsIgnoreCase(invite.uuid)) {
                return invite;
            }
            if (!name.isBlank() && name.equalsIgnoreCase(invite.name)) {
                return invite;
            }
            if (!uuid.isBlank() && uuid.equalsIgnoreCase(invite.pickerArg())) {
                return invite;
            }
        }
        return null;
    }

    /** Read-only dojo roster (Rival List twin). */
    private Inventory dojo(Player viewer, Player subject) {
        Holder holder = new Holder("dojo");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Dojo Roster"));
        holder.bind(inv);
        frame(inv, 45);
        Map<String, String> ph = ForgeBridge.sparPlaceholders(subject);
        List<String> cards = ForgeBridge.sparApprenticeCards(subject);
        int appCount = cards.size();
        String appMax = blank(ph.get("apprentice_max"), "8");
        List<String> header = new ArrayList<>();
        header.add("");
        header.add(appCount <= 0 ? "&7No apprentices yet." : "&7" + appCount + "/" + appMax + " apprentices");
        if ("true".equalsIgnoreCase(ph.getOrDefault("has_mentor", "false"))) {
            header.add("&7Your master &f" + blank(ph.get("mentor_name"), "?"));
        }
        header.addAll(GuiBoardHelper.tips(viewer, "&8Release from Mentor Actions"));
        put(holder, inv, 4, item(Material.BOOKSHELF, "&b&lDojo", header));
        if (cards.isEmpty()) {
            put(holder, inv, 22, tipBtn(viewer, "spar.empty.no_apprentice", Material.BARRIER, "&7Empty dojo",
                    List.of("&7Invite apprentices from Mentor Actions")));
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
                            List.of("&7Apprentice", "&8Release via Mentor → Release…"));
                } catch (IllegalArgumentException ex) {
                    head = GuiPlayerPicker.headByName(name, "&f" + name,
                            List.of("&7Apprentice", "&8Release via Mentor → Release…"));
                }
                put(holder, inv, slots[i], head);
            }
        }
        if (appCount > 0) {
            put(holder, inv, 39, pageBtn(viewer, "spar.mentor.release", Material.ORANGE_DYE, "&6Release…",
                    "&7Pick an apprentice to release"), SlotAction.page("pick_release"));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Mentor"),
                SlotAction.page("mentor"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Accept/decline picker — only incoming mentor invites. */
    private Inventory pendingPicker(
            Player viewer, Player subject, String action, String backPage, String title, String tip, boolean acceptMode) {
        Holder holder = new Holder("pick_" + (acceptMode ? "accept" : "decline"));
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Spar Mentor"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> pendingPickHeader = new ArrayList<>();
        pendingPickHeader.add("");
        pendingPickHeader.add("&7Incoming mentor invites");
        pendingPickHeader.addAll(GuiBoardHelper.tips(viewer, "&8Click a head to " + (acceptMode ? "accept" : "decline")));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title, pendingPickHeader));
        List<String> pending = ForgeBridge.sparPendingIncomingMentorArgs(subject);
        int placed = 0;
        for (String arg : pending) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            String display;
            ItemStack head;
            List<String> tipLore = new ArrayList<>(GuiBoardHelper.tips(viewer, tip));
            if (arg.regionMatches(true, 0, "uuid:", 0, 5)) {
                try {
                    java.util.UUID id = java.util.UUID.fromString(arg.substring(5).trim());
                    Player online = Bukkit.getPlayer(id);
                    display = online != null ? online.getName() : arg.substring(5).trim();
                    List<String> headLore = new ArrayList<>(tipLore);
                    if (online != null) {
                        headLore.add("&aOnline");
                        head = GuiPlayerPicker.head(online, "&f" + display, headLore);
                    } else {
                        head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                    }
                } catch (IllegalArgumentException e) {
                    display = arg;
                    head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                }
            } else {
                display = arg;
                Player online = Bukkit.getPlayerExact(arg);
                if (online != null) {
                    tipLore.add("&aOnline");
                    head = GuiPlayerPicker.head(online, "&f" + display, tipLore);
                } else {
                    head = GuiPlayerPicker.headByName(display, "&f" + display, tipLore);
                }
            }
            put(holder, inv, slot, head, SlotAction.act(action, arg, backPage));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(viewer, "spar.empty.no_pending", Material.BARRIER,
                    acceptMode ? "&eNothing to accept" : "&eNo pending invites",
                    List.of("&7When someone invites you,",
                            "&7they appear here.")));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    /** Release picker — current dojo apprentices only. */
    private Inventory releasePicker(Player viewer, Player subject) {
        Holder holder = new Holder("pick_release");
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Release Apprentice"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> cards = ForgeBridge.sparApprenticeCards(subject);
        List<String> header = new ArrayList<>();
        header.add("");
        header.add(cards.isEmpty() ? "&7No apprentices." : "&7" + cards.size() + " in your dojo");
        header.addAll(GuiBoardHelper.tips(viewer, "&8Click a head to release", "&812-hour cooldown"));
        put(holder, inv, 4, item(Material.ORANGE_DYE, "&6&lRelease Apprentice", header));
        if (cards.isEmpty()) {
            put(holder, inv, 22, tipBtn(viewer, "spar.empty.no_apprentice", Material.BARRIER, "&7No apprentices",
                    List.of("&7Invite apprentices from the Mentor page")));
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
                            List.of("&cRelease &f" + name, "&812-hour cooldown"));
                } catch (IllegalArgumentException ex) {
                    head = GuiPlayerPicker.headByName(name, "&f" + name,
                            List.of("&cRelease &f" + name, "&812-hour cooldown"));
                }
                put(holder, inv, slots[i], head,
                        SlotAction.act("mentor_release", "uuid:" + uuid, "mentor"));
            }
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Mentor"),
                SlotAction.page("mentor"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory picker(
            Player viewer, Player subject, String action, String backPage, String title, String tip) {
        Holder holder = new Holder("pick_" + action);
        Inventory inv = Bukkit.createInventory(holder, 45, invTitle(viewer, subject, "&8Sparring"));
        holder.bind(inv);
        frame(inv, 45);
        List<String> pickerHeader = new ArrayList<>();
        pickerHeader.add("");
        pickerHeader.add("&7Online players");
        pickerHeader.addAll(GuiBoardHelper.tips(viewer, "&8Click a head to confirm"));
        put(holder, inv, 4, item(Material.PLAYER_HEAD, title, pickerHeader));
        List<Player> online = GuiPlayerPicker.onlineExcept(subject);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            put(holder, inv, slot,
                    GuiPlayerPicker.head(other, "&f" + other.getName(), GuiBoardHelper.tips(viewer, tip)),
                    SlotAction.act(action, "uuid:" + other.getUniqueId(), backPage));
        }
        if (placed == 0) {
            put(holder, inv, 22, tipBtn(viewer, "spar.empty.no_online", Material.BARRIER, "&cNo one online",
                    List.of("&7Other players must be online")));
        }
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page(backPage));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
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

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
    }

    private static void frame(Inventory inv, int size) {
        for (int i = 0; i < size; i++) {
            boolean edge = i < 9 || i >= size - 9 || i % 9 == 0 || i % 9 == 8;
            inv.setItem(i, item(edge ? ACCENT : FILL, " ", List.of()));
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof Holder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        SlotAction slotAction = holder.actionAt(event.getSlot());
        if (slotAction == null) {
            return;
        }
        if (slotAction.shouldClose) {
            player.closeInventory();
            return;
        }
        if (slotAction.page != null) {
            final String targetPage = slotAction.page;
            Bukkit.getScheduler().runTask(plugin, () -> open(player, targetPage));
            return;
        }
        if (slotAction.rawCommand != null && !slotAction.rawCommand.isBlank()) {
            final String cmd = slotAction.rawCommand;
            Bukkit.getScheduler().runTask(plugin, () -> {
                player.closeInventory();
                player.performCommand(cmd);
            });
            return;
        }
        if (slotAction.action == null || slotAction.action.isBlank()) {
            return;
        }
        final String ret = slotAction.returnPage == null || slotAction.returnPage.isBlank()
                ? "main" : slotAction.returnPage;
        final String action = slotAction.action;
        final String arg = slotAction.arg == null || slotAction.arg.isBlank() ? "0" : slotAction.arg;
        final Player subject = AdminInspectSessions.resolveSubject(player);
        Bukkit.getScheduler().runTask(plugin, () -> {
            String msg = ForgeBridge.sparHandleDo(subject, action, arg, ret);
            if (msg != null && !msg.isBlank()) {
                GuiChat.sendResult(player, msg);
            }
            open(player, ret);
        });
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack) {
        put(holder, inv, slot, stack, null);
    }

    private static void put(Holder holder, Inventory inv, int slot, ItemStack stack, SlotAction action) {
        inv.setItem(slot, stack);
        if (holder != null && action != null) {
            holder.bindAction(slot, action);
        }
    }

    private static ItemStack tipBtn(Player player, Material mat, String name, List<String> tip) {
        return tipBtn(player, null, mat, name, tip, null);
    }

    private static ItemStack tipBtn(
            Player player, String key, Material mat, String name, List<String> tip
    ) {
        return tipBtn(player, key, mat, name, tip, null);
    }

    private static ItemStack tipBtn(
            Player player, String key, Material mat, String name, List<String> tip,
            Map<String, String> vars
    ) {
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            if (tip != null) {
                lore.addAll(tip);
            }
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name, vars), GuiTooltips.buttonLore(key, tip, vars, null));
    }

    private static ItemStack pageBtn(Player player, Material mat, String name, String... tips) {
        return pageBtn(player, null, mat, name, tips);
    }

    private static ItemStack pageBtn(Player player, String key, Material mat, String name, String... tips) {
        List<String> defaults = new ArrayList<>();
        if (tips != null) {
            for (String tip : tips) {
                if (tip != null) {
                    defaults.add(tip);
                }
            }
        }
        if (key == null || key.isBlank()) {
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(defaults);
            return item(mat, name, lore);
        }
        return item(mat, GuiTooltips.name(key, name), GuiTooltips.buttonLore(key, defaults));
    }

    private static ItemStack hubBtn() {
        return item(Material.COMPASS, "&7« Hub", List.of());
    }

    private static ItemStack closeBtn() {
        return item(Material.BARRIER, "&cClose", List.of());
    }

    private static ItemStack item(Material mat, String name, List<String> lore) {
        ItemStack stack = new ItemStack(mat);
        ItemMeta meta = stack.getItemMeta();
        meta.setDisplayName(color(name));
        List<String> colored = new ArrayList<>();
        for (String line : lore) {
            colored.add(color(line));
        }
        meta.setLore(colored);
        stack.setItemMeta(meta);
        return stack;
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
    }

    private static final class SlotAction {
        final String action;
        final String arg;
        final String returnPage;
        final String page;
        final String rawCommand;
        final boolean shouldClose;

        private SlotAction(
                String action, String arg, String returnPage, String page, String rawCommand, boolean shouldClose) {
            this.action = action;
            this.arg = arg;
            this.returnPage = returnPage;
            this.page = page;
            this.rawCommand = rawCommand;
            this.shouldClose = shouldClose;
        }

        static SlotAction act(String action, String arg, String returnPage) {
            return new SlotAction(action, arg, returnPage, null, null, false);
        }

        static SlotAction page(String page) {
            return new SlotAction(null, null, null, page, null, false);
        }

        static SlotAction cmd(String command) {
            return new SlotAction(null, null, null, null, command, false);
        }

        static SlotAction dismiss() {
            return new SlotAction(null, null, null, null, null, true);
        }
    }

    static final class Holder implements InventoryHolder {
        final String page;
        final Map<Integer, SlotAction> actions = new HashMap<>();
        Inventory inventory;

        Holder(String page) {
            this.page = page;
        }

        void bind(Inventory inventory) {
            this.inventory = inventory;
        }

        void bindAction(int slot, SlotAction action) {
            if (action != null) {
                actions.put(slot, action);
            }
        }

        SlotAction actionAt(int slot) {
            return actions.get(slot);
        }

        @Override
        public Inventory getInventory() {
            return inventory;
        }
    }
}
