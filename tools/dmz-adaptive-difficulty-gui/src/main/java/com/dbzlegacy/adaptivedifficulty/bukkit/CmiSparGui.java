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
 * Pages: main · stats · top · mentor · pick_apprentice · pick_mentor.
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
                openDetail(player, "stats", "&eSpar Stats", Material.BOOK);
            } else if ("mentor".equals(p)) {
                openMentor(player);
            } else if ("pick_apprentice".equals(p)) {
                openPicker(player, "mentor_invite", "mentor",
                        "&aInvite Apprentice", "&7Ask them to be your apprentice");
            } else if ("pick_mentor".equals(p)) {
                openPicker(player, "apprentice_invite", "mentor",
                        "&bAsk Mentor", "&7Ask them to be your mentor");
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
        CMIGui gui = base(player, "&8Legacy Mechanics · Sparring", 5);

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
            gui.open();
            return;
        }
        status.addLore(statusLore(player, ph));
        gui.addButton(status);

        // Main: Status · Stats · Top · Mentor · End Session · Hub · Close (pickers on Mentor only)
        gui.addButton(pageBtn(player, 19, Material.PAPER, "&eStats", "stats",
                "&7Your spar stats"));
        gui.addButton(pageBtn(player, 21, Material.GOLDEN_HELMET, "&fTop", "top",
                "&7Leaderboard"));
        gui.addButton(pageBtn(player, 23, Material.EMERALD, "&bMentor", "mentor",
                "&7Invite · ask · accept · remove"));

        boolean session = "true".equalsIgnoreCase(ph.getOrDefault("sessionActive", "false"));
        if (session) {
            gui.addButton(actionBtn(player, 31, Material.RED_DYE, "&cEnd Session",
                    "end", "0", "main",
                    List.of("&7End your active spar session")));
        }

        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(player, 37, Material.COMMAND_BLOCK, "&cAdmin", "admin",
                    "&7Save · status · mentor resetcd"));
        }
        fillEmpty(gui, 5);
        gui.open();
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
        CMIGui gui = base(player, "&8Legacy Mechanics · Sparring", 5);
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
            CMIGuiButton empty = new CMIGuiButton(13, Material.BARRIER, "&7No sparring data yet");
            empty.lockField();
            empty.addLore(List.of("", "&7Spar nearby to earn TP"));
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
        gui.addButton(pageBtn(player, 29, Material.GOLD_INGOT, "&eTP", "top_tp", "&7Total TP"));
        gui.addButton(pageBtn(player, 31, Material.CLOCK, "&aSessions", "top_sessions", "&7Sessions"));
        gui.addButton(pageBtn(player, 33, Material.NETHER_STAR, "&bPerfect", "top_perfect", "&7Perfect spars"));
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openAdmin(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Spar Admin", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.COMMAND_BLOCK, "&c&lSpar Admin");
        info.lockField();
        info.addLore(List.of("", "&7Staff-only tools",
                "&8Save · status · mentor resetcd",
                "&8Player menus stay on the main Spar GUI"));
        gui.addButton(info);
        gui.addButton(actionBtn(player, 20, Material.WRITABLE_BOOK, "&aSave",
                "admin", "save", "admin",
                List.of("&7Write sparring.json", "&8/spar admin save")));
        gui.addButton(actionBtn(player, 22, Material.COMPASS, "&bStatus",
                "admin", "status", "admin",
                List.of("&7Enabled + path", "&8/spar admin status")));
        gui.addButton(actionBtn(player, 24, Material.EMERALD, "&eReset Mentor CD",
                "admin", "resetcd", "admin",
                List.of("&7Clear your mentor cooldown", "&8/spar admin mentor resetcd")));
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Player Spar menu"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openMentor(Player player) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Sparring", 5);
        CMIGuiButton info = new CMIGuiButton(4, Material.EMERALD, "&b&lMentor");
        info.lockField();
        info.addLore(toAmp(ForgeBridge.sparLines(player, "mentor")));
        gui.addButton(info);

        gui.addButton(pageBtn(player, 19, Material.LIME_CONCRETE, "&aInvite apprentice…", "pick_apprentice",
                "&7Pick a player to mentor"));
        gui.addButton(pageBtn(player, 21, Material.LIGHT_BLUE_CONCRETE, "&bAsk mentor…", "pick_mentor",
                "&7Pick a player to ask as mentor"));
        gui.addButton(actionBtn(player, 23, Material.LIME_DYE, "&aAccept",
                "mentor", "accept", "mentor",
                List.of("&7Accept mentor invite")));
        gui.addButton(actionBtn(player, 25, Material.RED_CONCRETE, "&cDecline",
                "mentor", "decline", "mentor",
                List.of("&7Decline mentor invite")));
        gui.addButton(actionBtn(player, 31, Material.GRAY_CONCRETE, "&8Remove",
                "mentor", "remove", "mentor",
                List.of("&7Clear mentor bond")));

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openPicker(
            Player player, String action, String backPage, String title, String tip) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Sparring", 5);
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
            btn.addCommand("spar do " + action + " uuid:" + other.getUniqueId() + " " + backPage);
            gui.addButton(btn);
        }
        if (placed == 0) {
            CMIGuiButton empty = new CMIGuiButton(22, Material.BARRIER, "&cNo one online");
            empty.lockField();
            List<String> emptyLore = new ArrayList<>();
            emptyLore.add("");
            emptyLore.addAll(GuiBoardHelper.tips(player, "&7Other players must be online"));
            empty.addLore(emptyLore);
            gui.addButton(empty);
        }

        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", backPage, "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openDetail(Player player, String page, String title, Material mat) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Sparring", 5);
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        List<String> detailHeader = new ArrayList<>();
        detailHeader.add("");
        detailHeader.addAll(GuiBoardHelper.tips(player, "&7One item per entry", "&8Centered below"));
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
        gui.addButton(pageBtn(player, 36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
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
            lore.add("&bMentor bond &7as &f" + ph.getOrDefault("mentor_role", "?")
                    + " &8with &f" + blank(ph.get("mentor"), "?")
                    + "  &7streak &f" + ph.getOrDefault("streak", "0"));
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
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tipsList(player, tip));
        btn.addLore(lore);
        btn.addCommand("spar do " + action + " " + arg + " " + returnPage);
        return btn;
    }

    private static CMIGuiButton pageBtn(Player player, int slot, Material mat, String name, String page, String... tips) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(GuiBoardHelper.tips(player, tips));
        btn.addLore(lore);
        btn.addCommand("spar do page " + page);
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
