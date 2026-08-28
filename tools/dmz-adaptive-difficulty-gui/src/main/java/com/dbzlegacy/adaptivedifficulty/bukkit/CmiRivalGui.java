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
 * CMILib inventory GUI — Legacy Mechanics Rival.
 * Pages: main · list · challenge · progress · pick_* · stats · top · season · quests · achs · hof · journal · title.
 */
public final class CmiRivalGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;

    private CmiRivalGui() {}

    public static boolean available() {
        return CmiDifficultyGui.available();
    }

    public static boolean open(Player player, String page) {
        if (player == null || !available()) {
            return false;
        }
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        try {
            switch (p) {
                case "list" -> openList(player);
                case "pick_declare" -> openPicker(player, "declare", "list",
                        "&6Declare Rival", "&7Click to declare this player");
                case "pick_accept" -> openPendingPicker(player, "accept", "list",
                        "&aAccept Declare", "&7Click to accept their declare");
                case "pick_decline" -> openPendingPicker(player, "decline", "list",
                        "&cDecline Declare", "&7Click to decline their declare");
                case "pick_remove" -> openPicker(player, "remove", "list",
                        "&cRemove Rival", "&7Click to remove this rivalry");
                case "pick_challenge" -> openPicker(player, "challenge_send", "challenge",
                        "&cSend Challenge", "&7Click to challenge (1 min)");
                case "pick_spectate" -> openPicker(player, "spectate", "challenge",
                        "&bSpectate", "&7Watch their active challenge");
                case "pick_silent" -> openPicker(player, "silent", "list",
                        "&8Silent Rival", "&7Click for silent rivalry");
                case "stats", "statistics" -> openDetail(player, "stats", "&eRival Stats", Material.BOOK, "progress");
                case "challenge", "challenges" -> openChallenge(player);
                case "top", "leaderboard" -> openTop(player);
                case "progress" -> openProgress(player);
                case "season" -> openDetail(player, "season", "&aSeason", Material.CLOCK, "progress");
                case "quests", "quest" -> openDetail(player, "quests", "&bQuests", Material.WRITABLE_BOOK, "progress");
                case "achievements", "achs", "ach" ->
                        openDetail(player, "achievements", "&dAchievements", Material.DIAMOND, "progress");
                case "hof", "hall" -> openDetail(player, "hof", "&6Hall of Fame", Material.GOLD_BLOCK, "progress");
                case "journal" -> openDetail(player, "journal", "&fJournal", Material.MAP, "progress");
                case "title", "titles" -> openDetail(player, "title", "&eTitle", Material.NAME_TAG, "progress");
                case "help" -> openDetail(player, "help", "&7Help", Material.PAPER, "main");
                case "admin" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openAdmin(player);
                    } else {
                        openMain(player);
                    }
                }
                default -> openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cRival CMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.rivalPlaceholders(player);
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival", 5);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        CMIGuiButton status = new CMIGuiButton(4, Material.NETHER_STAR,
                !bridgeOk ? "&c&lUNAVAILABLE"
                        : !systemOn ? "&c&lRIVAL DISABLED"
                        : "&f&lRival");
        status.lockField();
        if (!bridgeOk || !systemOn) {
            status.addLore(unavailableLore(bridgeOk));
            gui.addButton(status);
            gui.addButton(hubBtn(40));
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            gui.open();
            return;
        }
        status.addLore(statusLore(ph));
        gui.addButton(status);

        // Main: Status · List · Challenge · Top · Progress · toggles · Hub
        gui.addButton(pageBtn(19, Material.PLAYER_HEAD, "&6List", "list",
                "&7Your rivals", "&8Declare · accept · remove"));
        gui.addButton(pageBtn(21, Material.IRON_SWORD, "&cChallenge", "challenge",
                "&7Send · accept · decline · spectate"));
        gui.addButton(pageBtn(23, Material.GOLDEN_HELMET, "&fTop", "top",
                "&7RP leaderboard"));
        gui.addButton(pageBtn(25, Material.WRITABLE_BOOK, "&bProgress", "progress",
                "&7Season · quests · achs · HOF · journal · title"));

        boolean tpOn = "true".equalsIgnoreCase(ph.getOrDefault("tpMsg", "false"));
        gui.addButton(actionBtn(29,
                tpOn ? Material.BELL : Material.PAPER,
                tpOn ? "&aTP Msg ON" : "&8TP Msg OFF",
                "tpmsg", "toggle", "main",
                List.of(
                        tpOn ? "&7Click to mute rival TP messages" : "&7Click to show rival TP messages",
                        "&8Only affects rivalry TP chat"
                )));

        boolean instinctFeature = "true".equalsIgnoreCase(ph.getOrDefault("instinct_feature", "false"));
        if (instinctFeature) {
            boolean instinctOn = "true".equalsIgnoreCase(ph.getOrDefault("instinct", "false"));
            gui.addButton(actionBtn(31,
                    instinctOn ? Material.LIME_DYE : Material.GRAY_DYE,
                    instinctOn ? "&aInstinct ON" : "&8Instinct OFF",
                    "instinct", "toggle", "main",
                    List.of(
                            instinctOn ? "&7Click to disable Rival Instinct" : "&7Click to enable Rival Instinct",
                            "&8Alerts for mutual / nemesis rivals"
                    )));
        }

        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(37, Material.REDSTONE, "&cAdmin", "admin",
                    "&7Save · refresh · status · commands"));
        }
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openProgress(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival Progress", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.WRITABLE_BOOK, "&b&lProgress");
        info.lockField();
        info.addLore(List.of("", "&7Each section is its own board",
                "&7Stats · Season · Quests · Achs · HOF · Journal · Title"));
        gui.addButton(info);

        String[] pages = {"stats", "season", "quests", "achievements", "hof", "journal", "title"};
        Material[] mats = {
                Material.BOOK, Material.CLOCK, Material.WRITABLE_BOOK, Material.DIAMOND,
                Material.GOLD_BLOCK, Material.MAP, Material.NAME_TAG
        };
        String[] titles = {"&eStats", "&aSeason", "&bQuests", "&dAchs", "&6HOF", "&fJournal", "&eTitle"};
        int[] slots = GuiBoardHelper.centeredRow(7);
        for (int i = 0; i < pages.length && i < slots.length; i++) {
            List<String> preview = previewLines(ForgeBridge.rivalLines(player, pages[i]), 4);
            List<String> lore = new ArrayList<>();
            lore.add("");
            lore.addAll(preview);
            lore.add("");
            lore.add("&eClick to open");
            gui.addButton(pageBtn(slots[i], mats[i], titles[i], pages[i],
                    lore.toArray(new String[0])));
        }

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openTop(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival", 5);
        List<String> raw = toAmp(ForgeBridge.rivalLines(player, "top"));
        List<GuiBoardHelper.TopEntry> entries = GuiBoardHelper.parseTopEntries(raw);
        CMIGuiButton header = new CMIGuiButton(4, Material.GOLDEN_HELMET, "&fRP Top");
        header.lockField();
        header.addLore(List.of("", "&7Top rivals by RP", "&8Player heads below"));
        gui.addButton(header);
        if (entries.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&7No rivalry data yet");
            empty.lockField();
            empty.addLore(List.of("", "&7Challenge rivals to earn RP"));
            gui.addButton(empty);
        } else {
            int[] slots = GuiBoardHelper.centeredSlots(Math.min(entries.size(), 21));
            for (int i = 0; i < slots.length && i < entries.size(); i++) {
                ItemStack head = GuiBoardHelper.topHead(entries.get(i));
                CMIGuiButton btn = new CMIGuiButton(slots[i], head);
                btn.lockField();
                gui.addButton(btn);
            }
        }
        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openDetail(Player player, String page, String title, Material mat, String backPage) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival", 5);
        String back = backPage == null || backPage.isBlank() ? "main" : backPage;
        List<String> lore = toAmp(ForgeBridge.rivalLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.", "&8Data: config/legacymechanics/");
        }
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        header.addLore(List.of("", "&7One item per entry", "&8Centered below"));
        gui.addButton(header);
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
        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", back, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openAdmin(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival Admin", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.REDSTONE, "&c&lRival Admin");
        info.lockField();
        info.addLore(List.of("", "&7Staff tools for the Rival system",
                "&8Click a button to run the linked command"));
        gui.addButton(info);
        gui.addButton(cmdBtn(19, Material.WRITABLE_BOOK, "&aSave", "rival admin save",
                "&7Write rivalry-v4 + progression-v4", "&8/rival admin save"));
        gui.addButton(cmdBtn(21, Material.CLOCK, "&eRefresh", "rival admin refresh",
                "&7Reload stores from disk", "&8/rival admin refresh"));
        gui.addButton(cmdBtn(23, Material.COMPASS, "&bStatus", "rival admin status",
                "&7Enabled + path summary", "&8/rival admin status"));
        gui.addButton(pageBtn(25, Material.NETHER_STAR, "&fOpen Rival GUI", "main",
                "&7Player rival menu"));
        gui.addButton(cmdBtn(29, Material.PAPER, "&7Help (chat)", "rival admin help",
                "&7Print admin command list"));
        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static List<String> previewLines(List<String> lines, int max) {
        List<String> out = new ArrayList<>();
        if (lines == null) {
            out.add("&7…");
            return out;
        }
        int n = 0;
        for (String line : lines) {
            if (line == null || line.isBlank()) {
                continue;
            }
            String plain = GuiBoardHelper.strip(line).trim();
            if (plain.startsWith("---") || plain.isEmpty()) {
                continue;
            }
            if (n == 0 && (plain.toLowerCase(Locale.ROOT).contains("stats")
                    || plain.toLowerCase(Locale.ROOT).contains("season")
                    || plain.toLowerCase(Locale.ROOT).contains("quest")
                    || plain.toLowerCase(Locale.ROOT).contains("achievement")
                    || plain.toLowerCase(Locale.ROOT).contains("hall")
                    || plain.toLowerCase(Locale.ROOT).contains("journal")
                    || plain.toLowerCase(Locale.ROOT).contains("title"))) {
                continue;
            }
            out.add(line.replace('§', '&'));
            n++;
            if (n >= max) {
                break;
            }
        }
        if (out.isEmpty()) {
            out.add("&7Open for details");
        }
        return out;
    }

    private static void openList(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, "&6&lRivals");
        info.lockField();
        List<String> lore = toAmp(ForgeBridge.rivalLines(player, "list"));
        List<String> withBlank = new ArrayList<>();
        withBlank.add("");
        withBlank.addAll(lore.isEmpty() ? List.of("&7No rivals yet.") : lore);
        info.addLore(withBlank);
        gui.addButton(info);

        gui.addButton(pageBtn(19, Material.LIME_CONCRETE, "&aDeclare…", "pick_declare",
                "&7Pick an online player to declare"));
        gui.addButton(pageBtn(21, Material.YELLOW_CONCRETE, "&eAccept…", "pick_accept",
                "&7Pending declares (no name guessing)"));
        gui.addButton(pageBtn(22, Material.ORANGE_CONCRETE, "&6Decline…", "pick_decline",
                "&7Decline a pending declare"));
        gui.addButton(pageBtn(23, Material.RED_CONCRETE, "&cRemove…", "pick_remove",
                "&7Pick a rival to remove"));
        gui.addButton(pageBtn(25, Material.GRAY_CONCRETE, "&8Silent…", "pick_silent",
                "&7Pick a player for silent rival"));

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openChallenge(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.IRON_SWORD, "&c&lChallenge");
        info.lockField();
        info.addLore(toAmp(ForgeBridge.rivalLines(player, "challenge")));
        gui.addButton(info);

        gui.addButton(pageBtn(19, Material.GOLDEN_SWORD, "&eSend Challenge…", "pick_challenge",
                "&7Pick an online rival to challenge"));
        gui.addButton(actionBtn(21, Material.LIME_CONCRETE, "&aAccept",
                "challenge", "accept", "challenge",
                List.of("&7Accept pending challenge")));
        gui.addButton(actionBtn(23, Material.RED_CONCRETE, "&cDecline",
                "challenge", "decline", "challenge",
                List.of("&7Decline pending challenge")));
        gui.addButton(actionBtn(25, Material.GRAY_CONCRETE, "&8Cancel",
                "challenge", "cancel", "challenge",
                List.of("&7Cancel your outgoing challenge")));
        gui.addButton(pageBtn(29, Material.ENDER_EYE, "&bSpectate…", "pick_spectate",
                "&7Watch an online player's challenge"));
        gui.addButton(actionBtn(31, Material.GRAY_DYE, "&8Stop Spectate",
                "spectate_stop", "0", "challenge",
                List.of("&7End spectating early")));

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openPicker(
            Player player, String action, String backPage, String title, String tip) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, title);
        info.lockField();
        info.addLore(List.of("", "&7Online players", "&8Click a head to confirm"));
        gui.addButton(info);

        List<Player> online = GuiPlayerPicker.onlineExcept(player);
        int placed = 0;
        for (Player other : online) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            ItemStack head = GuiPlayerPicker.head(other, "&f" + other.getName(), List.of(tip));
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("rival do " + action + " uuid:" + other.getUniqueId() + " " + backPage);
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&cNo one online");
            empty.lockField();
            empty.addLore(List.of("", "&7Other players must be online"));
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openPendingPicker(
            Player player, String action, String backPage, String title, String tip) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Rival", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.PLAYER_HEAD, title);
        info.lockField();
        info.addLore(List.of("", "&7Pending declares", "&8Online first · offline by name"));
        gui.addButton(info);

        List<String> pending = ForgeBridge.rivalPendingIncomingDeclareArgs(player);
        int placed = 0;
        for (String arg : pending) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed++];
            String display;
            ItemStack head;
            if (arg.regionMatches(true, 0, "uuid:", 0, 5)) {
                try {
                    java.util.UUID id = java.util.UUID.fromString(arg.substring(5).trim());
                    Player online = org.bukkit.Bukkit.getPlayer(id);
                    display = online != null ? online.getName() : arg.substring(5).trim();
                    head = online != null
                            ? GuiPlayerPicker.head(online, "&f" + display, List.of(tip, "&aOnline"))
                            : GuiPlayerPicker.headByName(display, "&f" + display, List.of(tip));
                } catch (IllegalArgumentException e) {
                    display = arg;
                    head = GuiPlayerPicker.headByName(display, "&f" + display, List.of(tip));
                }
            } else {
                display = arg;
                Player online = org.bukkit.Bukkit.getPlayerExact(arg);
                head = online != null
                        ? GuiPlayerPicker.head(online, "&f" + display, List.of(tip))
                        : GuiPlayerPicker.headByName(display, "&f" + display,
                                List.of(tip, "&8Offline — accept by name"));
            }
            CMIGuiButton btn = new CMIGuiButton(slot, head);
            btn.lockField();
            btn.addCommand("rival do " + action + " " + arg + " " + backPage);
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&eNo pending declares");
            empty.lockField();
            empty.addLore(List.of("", "&7When someone declares you,",
                    "&7they appear here to accept or decline."));
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openLines(Player player, String page, String title, Material mat, String backPage) {
        openDetail(player, page, title, mat, backPage);
    }

    private static List<String> statusLore(Map<String, String> ph) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.add("&7RP &f" + ph.getOrDefault("rp", "0")
                + " &8(&" + ph.getOrDefault("tier_color", "7")
                + ph.getOrDefault("tier", "?") + "&8)");
        lore.add("&7Mutual &f" + ph.getOrDefault("mutual", "0")
                + "&8/&f" + ph.getOrDefault("mutual_max", "3"));
        lore.add("&7Record &a" + ph.getOrDefault("wins", "0")
                + "&7/&c" + ph.getOrDefault("losses", "0")
                + "&7/&e" + ph.getOrDefault("draws", "0"));
        lore.add("&7TP msg &f"
                + ("true".equalsIgnoreCase(ph.get("tpMsg")) ? "ON" : "OFF"));
        if ("true".equalsIgnoreCase(ph.getOrDefault("challengeActive", "false"))) {
            lore.add("&eChallenge active");
        }
        lore.add("");
        lore.add("&8List · Challenge · Top · Progress");
        return lore;
    }

    private static List<String> unavailableLore(boolean bridgeOk) {
        if (!bridgeOk) {
            return List.of("", "&cForge LegacyMechanics mod unreachable",
                    "&7Check mods/ for LegacyMechanics-*.jar");
        }
        return List.of("", "&cRival system is disabled", "&7Ask an admin if you need access");
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
            int slot, Material mat, String name, String action, String arg, String returnPage, List<String> tip) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        btn.addLore(lore);
        btn.addCommand("rival do " + action + " " + arg + " " + returnPage);
        return btn;
    }

    private static CMIGuiButton pageBtn(int slot, Material mat, String name, String page, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        for (String tip : tips) {
            lore.add(tip);
        }
        btn.addLore(lore);
        btn.addCommand("rival do page " + page);
        return btn;
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
        btn.addLore(List.of("", "&7Legacy Mechanics hub"));
        btn.addCommand("lm");
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
