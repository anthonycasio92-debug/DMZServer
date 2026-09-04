package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * Bukkit chest GUI fallback — tier-centric Adaptive Difficulty.
 * Pages: Hub · Tiers (buy / lower / reset) · Titles. Details is staff/ops only.
 */
public final class DifficultyChestGui implements Listener {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;
    private static final int[] TIER_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    private final AdaptiveDifficultyGuiPlugin plugin;

    public DifficultyChestGui(AdaptiveDifficultyGuiPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player viewer, String page) {
        openAs(viewer, AdminInspectSessions.resolveSubject(viewer), page);
    }

    /**
     * Open the chest GUI for {@code viewer}, painting and applying actions as {@code subject}.
     * Staff inspect uses this so they see the target's gates/bugs and can edit settings.
     */
    public void openAs(Player viewer, Player subject, String page) {
        if (viewer == null) {
            return;
        }
        if (subject == null || !subject.isOnline()) {
            AdminInspectSessions.clear(viewer.getUniqueId());
            subject = viewer;
        }
        // Pull live DMZ level + unlock/title data before painting slots.
        ForgeBridge.prepareDifficultyGui(subject);
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        Inventory inv = switch (p) {
            case "tiers", "tier", "buy", "purchase", "unlock",
                 "adjust", "change", "set", "lower" -> tiers(viewer, subject);
            case "titles", "title" -> titles(viewer, subject);
            case "team", "teams" -> teams(viewer, subject);
            case "stats", "statistics", "details" ->
                    ForgeBridge.isStaff(viewer) ? stats(viewer, subject) : main(viewer, subject);
            default -> main(viewer, subject);
        };
        GuiFeedback.openChest(viewer, inv);
    }

    private static boolean inspecting(Player viewer, Player subject) {
        return viewer != null && subject != null
                && !viewer.getUniqueId().equals(subject.getUniqueId());
    }

    private static String titleFor(Player viewer, Player subject, String base) {
        if (inspecting(viewer, subject)) {
            return color("&8" + base + " · &c" + subject.getName());
        }
        return color("&8" + base);
    }

    private static Holder holderFor(Player viewer, Player subject, String page) {
        if (inspecting(viewer, subject)) {
            return new Holder(page, subject.getUniqueId(), subject.getName());
        }
        return new Holder(page, null, null);
    }

    private static List<String> inspectBanner(Player subject) {
        return List.of(
                "",
                "&cInspecting &f" + subject.getName(),
                "&7You see their live Adaptive Difficulty state.",
                "&8Clicks edit &ftheir &8settings / tiers / titles."
        );
    }

    private Inventory main(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.placeholders(subject);
        Holder holder = holderFor(viewer, subject, "main");
        Inventory inv = Bukkit.createInventory(holder, 36, titleFor(viewer, subject, "Adaptive Difficulty"));
        holder.bind(inv);
        frame(inv, 36);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        boolean allowed = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("player_allowed", "false"));
        if (!bridgeOk || !systemOn || !allowed) {
            String title = !bridgeOk ? "&c&lUNAVAILABLE"
                    : !systemOn ? "&c&lSYSTEM DISABLED" : "&e&lWHITELIST ONLY";
            put(holder, inv, 13, item(Material.BEACON, title, unavailableLore(viewer, subject, systemOn, bridgeOk)));
            put(holder, inv, 27, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
            put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        boolean personalOn = "true".equalsIgnoreCase(ph.getOrDefault("personal_enabled", "false"));
        String stateColor = personalOn ? ph.getOrDefault("state_color", "f") : "c";
        List<String> status = statusLore(ph, stateColor, ForgeBridge.isStaff(viewer), personalOn, subject.getName(), inspecting(viewer, subject));
        if (inspecting(viewer, subject)) {
            List<String> withBanner = new ArrayList<>(inspectBanner(subject));
            withBanner.addAll(status);
            status = withBanner;
        }
        put(holder, inv, 13, item(Material.BEACON,
                personalOn ? "&a&lAdaptive Difficulty" : "&c&lDIFFICULTY OFF",
                status));
        // Primary actions — Tiers · Teams · Titles
        put(holder, inv, 19, tipBtn(viewer, "difficulty.main.teams", Material.SHIELD, "&bRival Teams",
                List.of("&7Mutual rivals extend your tier ceiling",
                        "&8Opt-in · online · nearby for full bonus")),
                SlotAction.page("team"));
        put(holder, inv, 21, tipBtn(viewer, "difficulty.main.tiers", Material.GOLD_INGOT, "&eTiers",
                List.of("&7Buy higher · lower unlocked · reset", "&8Ancient Coins · pay-up OK · change returned")),
                SlotAction.page("tiers"));
        put(holder, inv, 23, tipBtn(viewer, "difficulty.main.titles", Material.NAME_TAG, "&dTitles",
                List.of("&7Equip difficulty titles", "&8Earned from tiers and combat")),
                SlotAction.page("titles"));

        // End Dragon summon — T4–T7 AD, 3× Ancient Netherite, summoner-only damage.
        int activeTier = 0;
        try {
            activeTier = Integer.parseInt(ph.getOrDefault("active_tier", "0"));
        } catch (NumberFormatException ignored) {
        }
        boolean canSummon = personalOn && activeTier >= 4 && activeTier <= 7;
        String dragonStatus;
        if (!personalOn) {
            dragonStatus = "&cDifficulty is OFF";
        } else if (activeTier < 4 || activeTier > 7) {
            dragonStatus = "&cNeed active T4–T7 (you: T" + activeTier + ")";
        } else {
            dragonStatus = "&aReady · click to summon";
        }
        Map<String, String> dragonVars = Map.of("status", dragonStatus, "active_tier", String.valueOf(activeTier));
        List<String> dragonDefaults = List.of(
                "&7Summon the End Dragon scaled to",
                "&7your Adaptive Difficulty (T4–T7).",
                "&8Cost: &f3 Ancient Netherite",
                "&8Must be &fin The End",
                "&8Only &fyou &8can damage it.",
                "",
                dragonStatus);
        put(holder, inv, 15, tipBtn(viewer, "difficulty.main.summon_dragon",
                canSummon ? Material.DRAGON_EGG : Material.GRAY_DYE,
                canSummon ? "&5&lSummon End Dragon" : "&8Summon End Dragon",
                dragonDefaults, dragonVars),
                SlotAction.act("summon_end_dragon", "0", "main"));

        boolean coinChatOn = "true".equalsIgnoreCase(ph.getOrDefault("coin_drop_chat", "false"));
        put(holder, inv, 29, tipBtn(viewer,
                personalOn ? "difficulty.main.personal_on" : "difficulty.main.personal_off",
                personalOn ? Material.LIME_DYE : Material.GRAY_DYE,
                personalOn ? "&aDifficulty ON" : "&cDifficulty OFF",
                List.of(
                        personalOn
                                ? "&7Click to turn OFF for you only"
                                : "&7Click to turn ON for you only",
                        personalOn
                                ? "&8OFF disables scaling, kill coins,"
                                : "&8ON restores scaling, kill coins,",
                        personalOn
                                ? "&8AI pressure, and tier buys"
                                : "&8AI pressure, and tier buys",
                        "&cWarning: &7Scaled mobs can attack other players as well"
                )), SlotAction.act("toggle_personal", "0", "main"));
        put(holder, inv, 31, tipBtn(viewer,
                coinChatOn ? "difficulty.main.coin_chat_on" : "difficulty.main.coin_chat_off",
                coinChatOn ? Material.BELL : Material.GRAY_DYE,
                coinChatOn ? "&aCoin Chat ON" : "&8Coin Chat OFF",
                List.of(
                        coinChatOn
                                ? "&7Click to mute drop messages"
                                : "&7Click to show drop messages",
                        "&8Only affects Ancient Coin kill chat"
                )), SlotAction.act("toggle_coin_chat", "0", "main"));
        if (ForgeBridge.isStaff(viewer)) {
            put(holder, inv, 33, tipBtn(viewer, "difficulty.main.details", Material.SPYGLASS, "&8Details",
                    List.of("&7Staff breakdown", "&8CR · prestige · kit gates")),
                    SlotAction.page("stats"));
        }
        put(holder, inv, 27, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory tiers(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.placeholders(subject);
        Holder holder = holderFor(viewer, subject, "tiers");
        Inventory inv = Bukkit.createInventory(holder, 45, titleFor(viewer, subject, "Difficulty Tiers"));
        holder.bind(inv);
        frame(inv, 45);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        boolean allowed = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("player_allowed", "false"));
        if (!bridgeOk || !systemOn || !allowed) {
            put(holder, inv, 4, item(Material.BARRIER, "&c&lTiers Locked", unavailableLore(viewer, subject, systemOn, bridgeOk)));
            put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }

        boolean personalOn = "true".equalsIgnoreCase(ph.getOrDefault("personal_enabled", "false"));
        List<String> info = new ArrayList<>();
        info.add("");
        if (!personalOn) {
            info.add("&cPersonal difficulty is OFF");
            info.add("&7Turn it ON on the main menu to change tiers.");
            put(holder, inv, 4, item(Material.BARRIER, "&c&lTiers Locked", info));
            put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
            put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
            return inv;
        }
        info.add("&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"));
        info.add("&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
        info.add("&7DMZ Level &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        info.add("");
        info.addAll(GuiTooltips.lore("difficulty.tiers.info",
                List.of("&7Click a higher unlocked tier to buy",
                        "&7Click a lower unlocked tier to step down (free)",
                        "&8Unlock with DMZ level or Prestige"), null));
        info.addAll(GuiBoardHelper.tips(viewer,
                "&eUnlock with DMZ level &7OR &ePrestige",
                "&8Either one qualifies — prestige is not required",
                "&cCR / Battle Power does NOT unlock tiers",
                "&8If stuck transformed: drop to base form once to sync"));
        info.add("");
        info.addAll(coinLore(ph));
        if (ForgeBridge.isStaff(viewer)) {
            info.add("");
            info.add("&8Costs scale with your DMZ level");
            info.add("&8Pay-up OK (e.g. Copper instead of Iron) — change returned");
        }
        put(holder, inv, 4, item(Material.GOLD_INGOT, "&e&lDifficulty Tiers", info));
        put(holder, inv, 8, tipBtn(viewer, "difficulty.tiers.reset", Material.RED_DYE, "&cReset to None",
                List.of("&7Clear active tier", "&8Unlocks & coins kept", "&8Always free")),
                SlotAction.act("lower_tier", "0", "tiers"));

        placeTierItems(holder, inv, ph, viewer);
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory titles(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.placeholders(subject);
        Holder holder = holderFor(viewer, subject, "titles");
        Inventory inv = Bukkit.createInventory(holder, 54, titleFor(viewer, subject, "Titles"));
        holder.bind(inv);
        frame(inv, 54);

        String equippedName = blankAsNone(ph.getOrDefault("active_title", ""));
        String perk = ph.getOrDefault("title_perk", "");
        List<String> titlesHeader = new ArrayList<>();
        titlesHeader.add("");
        titlesHeader.add("&7Equipped &e" + equippedName);
        titlesHeader.add(perk.isBlank() ? "&8No equipped perk" : "&7Perk &f" + perk);
        titlesHeader.add("");
        titlesHeader.add("&7Title Score &6" + ph.getOrDefault("title_score", "0"));
        titlesHeader.add("&7Unlocked &f" + ph.getOrDefault("titles_unlocked", "0")
                + " &8/ &f" + ph.getOrDefault("titles_total", "0"));
        titlesHeader.add("&7Elites &f" + ph.getOrDefault("elites_killed", "0")
                + "  &7Bosses &f" + ph.getOrDefault("bosses_killed", "0"));
        titlesHeader.add("&7Nearby Elites &f" + ph.getOrDefault("nearby_elites", "0"));
        titlesHeader.add("");
        titlesHeader.addAll(GuiTooltips.lore("difficulty.titles.header",
                List.of("&7Unlock via DMZ level or Prestige — keeps after lowering tier",
                        "&7Swap freely among unlocked · rarity adds landing/TP/AD presence"), null));
        titlesHeader.addAll(GuiBoardHelper.tips(viewer,
                "&7Unlock via DMZ/Prestige — never regresses on lower tier",
                "&8Rarity presence: softer landings · AD dmg · TP while tier on"));
        put(holder, inv, 4, item(Material.NAME_TAG, "&d&lTitles", titlesHeader));
        boolean senseOn = "true".equalsIgnoreCase(ph.getOrDefault("title_sense", "true"));
        put(holder, inv, 7, tipBtn(viewer, senseOn ? "difficulty.titles.sense_on" : "difficulty.titles.sense_off",
                senseOn ? Material.BELL : Material.NOTE_BLOCK,
                senseOn ? "&aTitle Sense ON" : "&8Title Sense OFF",
                List.of("&7Elite / Boss recognition chat", "&8Click to toggle")),
                SlotAction.act("toggle_title_sense", "0", "titles"));
        put(holder, inv, 8, tipBtn(viewer, "difficulty.titles.clear", Material.NAME_TAG, "&cClear Title",
                List.of("&7Unequip your title")), SlotAction.act("clear_title", "0", "titles"));

        // Tier titles row
        putTitleRow(viewer, holder, inv, ph, new String[]{
                "t1_awakened", "t2_enhanced", "t3_elite", "t4_advanced",
                "t5_master", "t6_legendary", "t7_god"
        }, new Material[]{
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
                Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR
        }, new int[]{19, 20, 21, 22, 23, 24, 25});

        // Combat + challenge titles
        putTitleRow(viewer, holder, inv, ph, new String[]{
                "elite_hunter", "boss_slayer", "ascendant",
                "mutation_hunter", "untouchable", "immortal",
                "coin_lord", "survivor", "worldbreaker"
        }, new Material[]{
                Material.DRAGON_HEAD, Material.WITHER_SKELETON_SKULL, Material.ENCHANTED_GOLDEN_APPLE,
                Material.AMETHYST_SHARD, Material.SHIELD, Material.TOTEM_OF_UNDYING,
                Material.GOLD_BLOCK, Material.CLOCK, Material.END_CRYSTAL
        }, new int[]{28, 29, 30, 31, 32, 33, 37, 39, 41});

        put(holder, inv, 45, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 53, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private void putTitleRow(
            Player viewer, Holder holder, Inventory inv, Map<String, String> ph,
            String[] ids, Material[] mats, int[] slots
    ) {
        boolean staff = ForgeBridge.isStaff(viewer);
        String equipped = ph.getOrDefault("active_title_id", "");
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            boolean earned = "true".equalsIgnoreCase(ph.getOrDefault("title_" + id + "_earned", "false"));
            String name = ph.getOrDefault("title_" + id + "_name", id);
            String tip = ph.getOrDefault("title_" + id + "_req", "");
            String perk = ph.getOrDefault("title_" + id + "_perk", "");
            String rarity = ph.getOrDefault("title_" + id + "_rarity", "");
            String rarityColor = ph.getOrDefault("title_" + id + "_rarity_color", "7");
            boolean isEquipped = id.equalsIgnoreCase(equipped);
            String title = isEquipped ? "&a● " + name : earned ? "&e" + name : "&8" + name;
            String state = isEquipped ? "equipped" : earned ? "unlocked" : "locked";
            String rarityLine = rarity.isBlank() ? "" : ("&" + rarityColor + rarity);
            String perkLine = perk.isBlank() ? "" : ("&f" + perk);
            Map<String, String> vars = Map.of(
                    "name", name,
                    "rarity", rarity,
                    "rarity_line", rarityLine,
                    "req", tip == null ? "" : tip,
                    "perk", perk == null ? "" : perk,
                    "perk_line", perkLine,
                    "state", state);

            List<String> defaults = new ArrayList<>();
            if (!rarityLine.isBlank()) {
                defaults.add("{rarity_line}");
            }
            defaults.add("&7{req}");
            if (!perkLine.isBlank()) {
                defaults.add("{perk_line}");
            }
            List<String> tipLore = new ArrayList<>();
            for (String line : GuiTooltips.lore("difficulty.titles.item", defaults, vars)) {
                if (line != null && !line.isBlank()) {
                    tipLore.add(line);
                }
            }
            if (isEquipped) {
                tipLore.addAll(GuiTooltips.lore("difficulty.titles.state_equipped",
                        List.of(staff ? "&aCurrently equipped &8· click to unequip" : "&aCurrently equipped"),
                        vars));
            } else if (earned) {
                tipLore.addAll(GuiTooltips.lore("difficulty.titles.state_unlocked",
                        List.of(staff ? "&aUnlocked &8· click to equip" : "&aUnlocked"), vars));
            } else {
                tipLore.addAll(GuiTooltips.lore("difficulty.titles.state_locked", List.of("&cLocked"), vars));
            }
            if (earned) {
                put(holder, inv, slots[i], tipBtn(viewer, mats[i], title, tipLore),
                        SlotAction.act("equip_title", id, "titles"));
            } else {
                put(holder, inv, slots[i], item(mats[i], title, prependBlank(tipLore)));
            }
        }
    }

    private Inventory teams(Player viewer, Player subject) {
        Map<String, String> ph = ForgeBridge.placeholders(subject);
        Holder holder = holderFor(viewer, subject, "team");
        Inventory inv = Bukkit.createInventory(holder, 45, titleFor(viewer, subject, "Rival Teams"));
        holder.bind(inv);
        frame(inv, 45);

        List<String> header = new ArrayList<>();
        header.add("");
        header.addAll(toAmp(ForgeBridge.diffTeamLines(subject)));
        put(holder, inv, 4, item(Material.SHIELD,
                GuiTooltips.name("difficulty.team.header", "&b&lRival Teams"),
                header));

        String mode = ph.getOrDefault("team_mode", "personal_only");
        put(holder, inv, 20, tipBtn(viewer, "difficulty.team.mode_personal", Material.GRAY_DYE, "&7Personal",
                List.of("&7Only your tier ceiling",
                        mode.equals("personal_only") ? "&aCurrent mode" : "&eClick to select")),
                SlotAction.act("team", "personal", "team"));
        put(holder, inv, 22, tipBtn(viewer, "difficulty.team.mode_threshold", Material.LIME_DYE, "&aThreshold",
                List.of("&7+" + ph.getOrDefault("bonus_percent", "10") + "% max per opted-in rival online",
                        mode.equals("threshold_bonus_only") ? "&aCurrent mode" : "&eClick to select")),
                SlotAction.act("team", "threshold", "team"));
        put(holder, inv, 24, tipBtn(viewer, "difficulty.team.mode_full", Material.EMERALD, "&2Full",
                List.of("&7Threshold + nearby spare tier room",
                        "&8Within " + ph.getOrDefault("proximity_blocks", "48") + " blocks",
                        mode.equals("full_team_scaling") ? "&aCurrent mode" : "&eClick to select")),
                SlotAction.act("team", "full", "team"));

        List<GuiBoardHelper.TeamRivalCard> cards =
                GuiBoardHelper.parseTeamRivalCards(ForgeBridge.diffTeamMutualCards(subject));
        int[] slots = GuiBoardHelper.centeredSlots(cards.size());
        for (int i = 0; i < cards.size() && i < slots.length; i++) {
            put(holder, inv, slots[i], GuiBoardHelper.teamRivalHead(cards.get(i)));
        }
        if (cards.isEmpty()) {
            put(holder, inv, 28, item(Material.BARRIER, "&7No mutual rivals",
                    List.of("", "&7Use /rival to declare and accept",
                            "&8Both players must accept for Mutual")));
        }

        put(holder, inv, 31, tipBtn(viewer, "difficulty.team.open_rival", Material.DIAMOND_SWORD, "&6Open Rival",
                List.of("&7Manage declares and mutual slots")),
                SlotAction.cmd("lmdo lm open rival"));
        put(holder, inv, 36, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"),
                SlotAction.page("main"));
        put(holder, inv, 40, hubBtn(), SlotAction.cmd("lmdo lm open hub"));
        put(holder, inv, 44, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private Inventory stats(Player viewer, Player subject) {
        if (!ForgeBridge.isStaff(viewer)) {
            return main(viewer, subject);
        }
        Map<String, String> ph = ForgeBridge.placeholders(subject);
        Holder holder = holderFor(viewer, subject, "stats");
        Inventory inv = Bukkit.createInventory(holder, 36, titleFor(viewer, subject, "Details (staff)"));
        holder.bind(inv);
        frame(inv, 36);
        String stateColor = ph.getOrDefault("state_color", "f");

        put(holder, inv, 11, item(Material.NETHER_STAR, "&f&lProgression", List.of(
                "",
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Combat CR   &f" + ph.getOrDefault("combat_rating", "?"),
                "&7State       &" + stateColor + ph.getOrDefault("state", "?"),
                "",
                "&7DMZ &f" + ph.getOrDefault("level", "?")
                        + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "&7Title &e" + blankAsNone(ph.getOrDefault("active_title", ""))
        )));
        put(holder, inv, 13, item(Material.IRON_SWORD, "&c&lCounters", List.of(
                "",
                "&7Class &f" + blankAsNone(ph.getOrDefault("fighting_class", "")),
                "&7Style &f" + ph.getOrDefault("fighting_style", "HYBRID"),
                "",
                "&7Top stats &f" + ph.getOrDefault("top_stats", "—"),
                "",
                "&8Mobs counter class and top stat"
        )));
        List<String> coins = new ArrayList<>();
        coins.add("");
        coins.addAll(coinLore(ph));
        coins.add("");
        coins.add("&8Tier purchases: pay-up OK, change returned");
        put(holder, inv, 15, item(Material.GOLD_INGOT, "&f&lAncient Coins", coins));
        put(holder, inv, 27, pageBtn(viewer, "common.back", Material.ARROW, "&7Back", "&7Return"), SlotAction.page("main"));
        put(holder, inv, 35, closeBtn(), SlotAction.dismiss());
        return inv;
    }

    private static void placeTierItems(
            Holder holder, Inventory inv, Map<String, String> ph, Player viewer) {
        boolean staff = ForgeBridge.isStaff(viewer);
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        int activeTier = (int) parseLong(ph.getOrDefault("active_tier", "0"));
        String key = "difficulty.tiers.tier";
        for (int t = 1; t <= 7; t++) {
            String cost = ph.getOrDefault("tier_" + t + "_cost", "?");
            String name = ph.getOrDefault("tier_" + t + "_name", "T" + t);
            boolean unlocked = "true".equalsIgnoreCase(ph.getOrDefault("tier_" + t + "_unlocked", "false"));
            boolean active = activeTier == t;
            boolean canLower = unlocked && !active && t < activeTier;
            String reqLevel = ph.getOrDefault("tier_" + t + "_req_level", "?");
            String reqPrestige = ph.getOrDefault("tier_" + t + "_req_prestige", String.valueOf(t));
            String reqTip = ph.getOrDefault("tier_" + t + "_req",
                    "DMZ " + reqLevel + " or Prestige " + reqPrestige);
            Map<String, String> tierVars = Map.of(
                    "tier", String.valueOf(t),
                    "cost", cost,
                    "name", name,
                    "req_level", reqLevel,
                    "req_prestige", reqPrestige,
                    "level", ph.getOrDefault("level", "?"),
                    "prestige", ph.getOrDefault("prestige", "?"),
                    "req", reqTip);

            List<String> tip = new ArrayList<>();
            tip.add("&7{name}");
            if (active) {
                tip.add("&aCurrently active");
            } else if (canLower) {
                tip.add("&aOwned &8· free to lower here");
            } else if (unlocked) {
                tip.add("&7Cost &e{cost}");
                if (staff) {
                    tip.add("&8Scaled for your DMZ level");
                }
                tip.add(staff ? "&aUnlocked &8· click to purchase" : "&aUnlocked");
                if (staff) {
                    tip.add("&8Pay-up OK · change returned");
                }
            } else {
                tip.add("&cLocked");
                tip.add("&7Need &fDMZ {req_level} &7or &fPrestige {req_prestige}");
                if (staff) {
                    tip.add("&8{req}");
                    tip.add("&8You: DMZ {level} · Prestige {prestige}");
                    tip.add("&8CR/BP ignored — use DMZ level or Prestige");
                }
            }

            String title;
            if (active) {
                title = "&a● T" + t + " Active";
            } else if (canLower) {
                title = "&fLower to T" + t;
            } else if (unlocked) {
                title = "&eBuy T" + t;
            } else {
                title = "&8Locked T" + t;
            }

            ItemStack stack = tipBtn(viewer, key, mats[t - 1], title, tip, tierVars);
            if (unlocked && !active) {
                // activate: paid raise, free lower (handled in Forge setTier)
                put(holder, inv, TIER_SLOTS[t - 1], stack,
                        SlotAction.act("activate", String.valueOf(t), "tiers"));
            } else {
                put(holder, inv, TIER_SLOTS[t - 1], stack);
            }
        }
    }

    private static List<String> prependBlank(List<String> tip) {
        List<String> out = new ArrayList<>();
        out.add("");
        out.addAll(tip);
        return out;
    }

    /** Clean copy for players; command tips for staff / inspect. */
    private static List<String> unavailableLore(
            Player viewer, Player subject, boolean systemOn, boolean bridgeOk) {
        boolean staff = ForgeBridge.isStaff(viewer);
        boolean inspect = inspecting(viewer, subject);
        if (!bridgeOk) {
            if (staff || inspect) {
                return List.of("", "&cForge Adaptive Difficulty mod unreachable",
                        "&7Check mods/ for AdaptiveDifficulty-*.jar",
                        "&8GUI actions are disabled until the mod loads");
            }
            return List.of("", "&cAdaptive Difficulty is unavailable", "&7Please try again later");
        }
        if (!systemOn) {
            if (staff || inspect) {
                return List.of("", "&cAdaptive Difficulty is off",
                        "&7No scaling, coins, or purchases",
                        "&8Re-enable: &f/difficulty admin on");
            }
            return List.of("", "&cAdaptive Difficulty is off", "&7Please try again later");
        }
        if (staff || inspect) {
            return List.of("", "&eTesting whitelist is on",
                    "&7" + (inspect ? subject.getName() + " is" : "You are") + " not on the whitelist",
                    "&8Add: &f/difficulty admin whitelist add "
                            + (inspect ? subject.getName() : "<you>"));
        }
        return List.of("", "&eNot available right now", "&7Ask an admin if you need access");
    }

    private static List<String> statusLore(
            Map<String, String> ph, String stateColor, boolean staff, boolean personalOn,
            String subjectName, boolean inspect) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (!personalOn) {
            lore.add(inspect
                    ? "&cDifficulty is OFF for &f" + subjectName
                    : "&cDifficulty is OFF for you");
            lore.add("&7No scaling, kill coins, AI, or tier buys");
            lore.add("&8Saved tier &f" + ph.getOrDefault("active_tier_name", "None")
                    + "  &8·  &7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
            lore.add("&7State &cOff");
        } else {
            lore.add("&7Tier &f" + ph.getOrDefault("active_tier_name", "None")
                    + "  &8·  &7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
            lore.add("&7State &" + stateColor + ph.getOrDefault("state", "?"));
        }
        String title = blankAsNone(ph.getOrDefault("active_title", ""));
        if (!"None".equals(title)) {
            lore.add("&7Title &e" + title);
        }
        lore.add("");
        lore.add("&cWarning: &7Scaled mobs can attack other players as well");
        lore.addAll(coinLore(ph));
        if (staff) {
            lore.add("");
            lore.add("&8CR &f" + ph.getOrDefault("combat_rating", "?")
                    + "  &8DMZ &f" + ph.getOrDefault("level", "?")
                    + "  &8Prestige &f" + ph.getOrDefault("prestige", "?"));
        }
        if (staff) {
            lore.add("");
            lore.add(personalOn
                    ? "&8Open Tiers to buy higher or lower"
                    : "&8Turn Difficulty ON below to resume");
        }
        return lore;
    }

    private static List<String> coinLore(Map<String, String> ph) {
        return List.of(
                "&6Ancient Coins",
                "&eCopper &f" + ph.getOrDefault("coins_copper", "0")
                        + "  &eIron &f" + ph.getOrDefault("coins_iron", "0")
                        + "  &eGold &f" + ph.getOrDefault("coins_gold", "0"),
                "&eEmerald &f" + ph.getOrDefault("coins_emerald", "0")
                        + "  &eDiamond &f" + ph.getOrDefault("coins_diamond", "0")
                        + "  &eNetherite &f" + ph.getOrDefault("coins_netherite", "0"),
                "&6Total &f" + ph.getOrDefault("ancient_coins", "0") + " AC"
        );
    }

    private static String blankAsNone(String value) {
        return value == null || value.isBlank() ? "None" : value;
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
        if (!(event.getWhoClicked() instanceof Player viewer)) {
            return;
        }
        // Only top inventory slots we registered — ignore player inv / lore spoofing.
        if (event.getClickedInventory() == null
                || event.getClickedInventory() != event.getView().getTopInventory()) {
            return;
        }
        SlotAction slotAction = holder.actionAt(event.getSlot());
        if (slotAction == null) {
            return;
        }
        Player subject = resolveHolderSubject(viewer, holder);
        if (subject == null) {
            viewer.sendMessage("§cInspect target is offline — closed.");
            AdminInspectSessions.clear(viewer.getUniqueId());
            viewer.closeInventory();
            return;
        }
        if (slotAction.shouldClose) {
            viewer.closeInventory();
            return;
        }
        if (slotAction.page != null) {
            final String targetPage = slotAction.page;
            final Player subjectFinal = subject;
            Bukkit.getScheduler().runTask(plugin, () -> openAs(viewer, subjectFinal, targetPage));
            return;
        }
        if (slotAction.rawCommand != null && !slotAction.rawCommand.isBlank()) {
            // Hub / cross-menu commands always apply to the viewer (not inspect subject).
            final String cmd = slotAction.rawCommand;
            Bukkit.getScheduler().runTask(plugin, () -> {
                viewer.closeInventory();
                viewer.performCommand(cmd);
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
        final Player subjectFinal = subject;
        final boolean inspect = inspecting(viewer, subject);
        Bukkit.getScheduler().runTask(plugin, () -> {
            // Always ForgeBridge — never route GUI clicks through Mohist /difficulty do.
            String reopen = ForgeBridge.resolveReturnPage(action, arg, ret);
            ForgeBridge.ActionResult result =
                    ForgeBridge.handleActionResult(subjectFinal, action, arg, reopen);
            if (result.message() != null && !result.message().isBlank()) {
                String msg = result.message();
                if (!msg.startsWith("§")) {
                    msg = (result.ok() ? "§a" : "§c") + msg;
                }
                if (inspect) {
                    GuiChat.send(viewer, GuiChat.tagged(subjectFinal.getName(), msg));
                } else {
                    GuiChat.sendResult(viewer, msg);
                }
            }
            if (reopen != null && !reopen.isBlank()) {
                openAs(viewer, subjectFinal, reopen);
            }
        });
    }

    private static Player resolveHolderSubject(Player viewer, Holder holder) {
        if (holder.subjectId == null) {
            return viewer;
        }
        Player subject = Bukkit.getPlayer(holder.subjectId);
        if (subject == null || !subject.isOnline()) {
            return null;
        }
        return subject;
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof Holder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        AdminInspectSessions.clearAllInvolving(event.getPlayer().getUniqueId());
        GuiFeedback.clear(event.getPlayer());
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

    private static long parseLong(String raw) {
        try {
            return Long.parseLong(raw);
        } catch (Exception e) {
            return 0L;
        }
    }

    private static String color(String input) {
        return input == null ? "" : input.replace('&', '§');
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
        /** Null when the viewer is editing themselves. */
        final UUID subjectId;
        final String subjectName;
        final Map<Integer, SlotAction> actions = new HashMap<>();
        Inventory inventory;

        Holder(String page) {
            this(page, null, null);
        }

        Holder(String page, UUID subjectId, String subjectName) {
            this.page = page;
            this.subjectId = subjectId;
            this.subjectName = subjectName;
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
