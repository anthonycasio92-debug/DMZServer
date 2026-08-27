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
                openLines(player, "stats", "&eSpar Stats", Material.BOOK);
            } else if ("mentor".equals(p)) {
                openMentor(player);
            } else if ("pick_apprentice".equals(p)) {
                openPicker(player, "mentor_invite", "mentor",
                        "&aInvite Apprentice", "&7Ask them to be your apprentice");
            } else if ("pick_mentor".equals(p)) {
                openPicker(player, "apprentice_invite", "mentor",
                        "&bAsk Mentor", "&7Ask them to be your mentor");
            } else if ("help".equals(p)) {
                openLines(player, "help", "&7Help", Material.PAPER);
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
        CMIGuiButton status = new CMIGuiButton(4, Material.NETHER_STAR,
                !bridgeOk ? "&c&lUNAVAILABLE"
                        : !systemOn ? "&c&lSPARRING DISABLED"
                        : "&f&lSparring");
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

        // Main: Status · Stats · Top · Mentor · End Session · Hub · Close (pickers on Mentor only)
        gui.addButton(pageBtn(19, Material.BOOK, "&eStats", "stats",
                "&7Your spar stats"));
        gui.addButton(pageBtn(21, Material.GOLDEN_HELMET, "&fTop", "top",
                "&7Leaderboard"));
        gui.addButton(pageBtn(23, Material.EMERALD, "&bMentor", "mentor",
                "&7Invite · ask · accept · remove"));

        boolean session = "true".equalsIgnoreCase(ph.getOrDefault("sessionActive", "false"));
        if (session) {
            gui.addButton(actionBtn(31, Material.RED_CONCRETE, "&cEnd Session",
                    "end", "0", "main",
                    List.of("&7End your active spar session")));
        }

        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
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
        CMIGuiButton info = new CMIGuiButton(4, Material.GOLDEN_HELMET, "&f&lTop — " + cat);
        info.lockField();
        List<String> lore = toAmp(ForgeBridge.sparLines(player, lorePage));
        List<String> withBlank = new ArrayList<>();
        withBlank.add("");
        withBlank.addAll(lore);
        info.addLore(withBlank);
        gui.addButton(info);

        gui.addButton(pageBtn(29, Material.GOLD_INGOT, "&eTP", "top_tp", "&7Total TP"));
        gui.addButton(pageBtn(31, Material.CLOCK, "&aSessions", "top_sessions", "&7Sessions"));
        gui.addButton(pageBtn(33, Material.NETHER_STAR, "&bPerfect", "top_perfect", "&7Perfect spars"));

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
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

        gui.addButton(pageBtn(19, Material.LIME_CONCRETE, "&aInvite apprentice…", "pick_apprentice",
                "&7Pick a player to mentor"));
        gui.addButton(pageBtn(21, Material.LIGHT_BLUE_CONCRETE, "&bAsk mentor…", "pick_mentor",
                "&7Pick a player to ask as mentor"));
        gui.addButton(actionBtn(23, Material.LIME_DYE, "&aAccept",
                "mentor", "accept", "mentor",
                List.of("&7Accept mentor invite")));
        gui.addButton(actionBtn(25, Material.RED_CONCRETE, "&cDecline",
                "mentor", "decline", "mentor",
                List.of("&7Decline mentor invite")));
        gui.addButton(actionBtn(31, Material.GRAY_CONCRETE, "&8Remove",
                "mentor", "remove", "mentor",
                List.of("&7Clear mentor bond")));

        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static void openPicker(
            Player player, String action, String backPage, String title, String tip) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Sparring", 5);
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
            btn.addCommand("spar do " + action + " uuid:" + other.getUniqueId() + " " + backPage);
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

    private static void openLines(Player player, String page, String title, Material mat) {
        CMIGui gui = base(player, "&8Legacy Mechanics · Sparring", 5);
        CMIGuiButton header = new CMIGuiButton(4, mat, title);
        header.lockField();
        header.addLore(List.of("", "&7Content slots below",
                "&8Each paper holds part of this page"));
        gui.addButton(header);
        List<String> lore = toAmp(ForgeBridge.sparLines(player, page));
        if (lore.isEmpty()) {
            lore = List.of("&7Nothing here yet.");
        }
        List<List<String>> parts = GuiLoreChunks.chunk(lore);
        int placed = 0;
        for (List<String> part : parts) {
            if (placed >= GuiPlayerPicker.CONTENT_SLOTS.length) {
                break;
            }
            int slot = GuiPlayerPicker.CONTENT_SLOTS[placed];
            String partTitle = parts.size() == 1
                    ? "&fDetails"
                    : "&fPart &e" + (placed + 1) + "&8/&e" + parts.size();
            CMIGuiButton chunk = new CMIGuiButton(slot, Material.PAPER, partTitle);
            chunk.lockField();
            List<String> withBlank = new ArrayList<>();
            withBlank.add("");
            withBlank.addAll(part);
            chunk.addLore(withBlank);
            gui.addButton(chunk);
            placed++;
        }
        gui.addButton(pageBtn(36, Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        gui.open();
    }

    private static List<String> statusLore(Map<String, String> ph) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if ("true".equalsIgnoreCase(ph.getOrDefault("sessionActive", "false"))) {
            lore.add("&aSession ACTIVE &8with &f" + blank(ph.get("partner"), "?")
                    + "  &7TP &f" + ph.getOrDefault("session_tp", "0"));
            if ("true".equalsIgnoreCase(ph.get("perfect"))) {
                lore.add("&6&lPERFECT TRAINING");
            }
        } else {
            lore.add("&7No active spar — trade hits within 30 blocks to start.");
        }
        if ("true".equalsIgnoreCase(ph.getOrDefault("mentor_bonded", "false"))) {
            lore.add("&bMentor bond &7as &f" + ph.getOrDefault("mentor_role", "?")
                    + " &8with &f" + blank(ph.get("mentor"), "?")
                    + "  &7streak &f" + ph.getOrDefault("streak", "0"));
        } else {
            lore.add("&7No mentor bond. &8Use Mentor page to invite");
        }
        lore.add("");
        lore.add("&8Stats · Top · Mentor");
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
            int slot, Material mat, String name, String action, String arg, String returnPage, List<String> tip) {
        CMIGuiButton btn = new CMIGuiButton(slot, mat, name);
        btn.lockField();
        List<String> lore = new ArrayList<>();
        lore.add("");
        lore.addAll(tip);
        btn.addLore(lore);
        btn.addCommand("spar do " + action + " " + arg + " " + returnPage);
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
        btn.addCommand("spar do page " + page);
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
