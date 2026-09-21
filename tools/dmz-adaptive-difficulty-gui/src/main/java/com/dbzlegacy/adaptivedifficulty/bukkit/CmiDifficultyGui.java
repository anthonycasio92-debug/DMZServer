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

/**
 * CMILib inventory GUI — tier-centric Adaptive Difficulty.
 * Pages: Hub · Tiers · Titles · Rival Teams.
 * Details is staff/ops only.
 */
public final class CmiDifficultyGui {
    private static final Material FILL = Material.BLACK_STAINED_GLASS_PANE;
    private static final Material ACCENT = Material.GRAY_STAINED_GLASS_PANE;
    /** Centered tier row slots in a 5-row chest (columns 1–7 of row 3). */
    private static final int[] TIER_SLOTS = {19, 20, 21, 22, 23, 24, 25};

    private CmiDifficultyGui() {}

    public static boolean available() {
        try {
            Class.forName("net.Zrips.CMILib.GUI.CMIGui");
            Class.forName("net.Zrips.CMILib.CMILib");
            org.bukkit.plugin.Plugin lib = org.bukkit.Bukkit.getPluginManager().getPlugin("CMILib");
            org.bukkit.plugin.Plugin cmi = org.bukkit.Bukkit.getPluginManager().getPlugin("CMI");
            return (lib != null && lib.isEnabled()) || (cmi != null && cmi.isEnabled());
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean open(Player player, String page) {
        if (player == null || !available()) {
            return false;
        }
        // Pull live DMZ level + unlock/title data before painting slots.
        ForgeBridge.prepareDifficultyGui(player);
        String p = page == null || page.isBlank() ? "main" : page.toLowerCase(Locale.ROOT);
        try {
            switch (p) {
                case "tiers", "tier", "buy", "purchase", "unlock",
                     "adjust", "change", "set", "lower" -> openTiers(player);
                case "titles", "title" -> openTitles(player);
                case "team", "teams" -> openTeams(player);
                case "stats", "statistics", "details" -> {
                    if (ForgeBridge.isStaff(player)) {
                        openStats(player);
                    } else {
                        openMain(player);
                    }
                }
                default -> openMain(player);
            }
            return true;
        } catch (Throwable t) {
            player.sendMessage("§cCMI GUI failed: " + t.getMessage());
            return false;
        }
    }

    private static void openMain(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Adaptive Difficulty", 4);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        boolean allowed = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("player_allowed", "false"));
        boolean personalOn = "true".equalsIgnoreCase(ph.getOrDefault("personal_enabled", "false"));
        String stateColor = personalOn ? ph.getOrDefault("state_color", "f") : "c";
        CMIGuiButton status = new CMIGuiButton(4, Material.BEACON,
                !bridgeOk ? "&c&lUNAVAILABLE"
                        : !systemOn ? "&c&lSYSTEM DISABLED"
                        : !allowed ? "&e&lWHITELIST ONLY"
                        : !personalOn ? "&c&lDIFFICULTY OFF"
                        : "&a&lAdaptive Difficulty");
        status.lockField();
        if (!bridgeOk || !systemOn || !allowed) {
            status.addLore(unavailableLore(player, systemOn, bridgeOk));
            gui.addButton(status);
            gui.addButton(hubBtn(27));
            gui.addButton(closeBtn(35));
            fillEmpty(gui, 4);
            GuiFeedback.openCmi(gui);
            return;
        }
        status.addLore(statusLore(ph, stateColor, ForgeBridge.isStaff(player), personalOn));
        gui.addButton(status);

        // Primary actions — Tiers (buy/lower/reset) + Titles
        gui.addButton(pageBtn(player, 19, "difficulty.main.teams", Material.SHIELD, "&bRival Teams", "team",
                "&7Mutual rivals can raise your tier ceiling",
                "&7Team modes boost elite, mutant, and boss spawns"));
        gui.addButton(pageBtn(player, 21, "difficulty.main.tiers", Material.GOLD_INGOT, "&eTiers", "tiers",
                "&7Buy higher · lower unlocked · reset",
                "&8Ancient Coins · pay-up OK · change returned"));
        gui.addButton(pageBtn(player, 23, "difficulty.main.titles", Material.NAME_TAG, "&dTitles", "titles",
                "&7Equip difficulty titles",
                "&8Earned from tiers and combat"));

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
            dragonStatus = "&aReady — tap to summon";
        }
        List<String> dragonDefaults = List.of(
                "&7Summon the End Dragon scaled to",
                "&7your Adaptive Difficulty (T4–T7).",
                "&8Cost: &f3 Ancient Netherite",
                "&8Must be &fin The End",
                "&8Only &fyou &8can damage it.",
                "",
                dragonStatus);
        gui.addButton(actionBtn(player, 15, "difficulty.main.summon_dragon",
                canSummon ? Material.DRAGON_EGG : Material.GRAY_DYE,
                canSummon ? "&5&lSummon End Dragon" : "&8Summon End Dragon",
                "summon_end_dragon", "0", "main",
                dragonDefaults, Map.of("status", dragonStatus)));

        boolean coinChatOn = "true".equalsIgnoreCase(ph.getOrDefault("coin_drop_chat", "false"));
        gui.addButton(actionBtn(player, 29,
                personalOn ? "difficulty.main.personal_on" : "difficulty.main.personal_off",
                personalOn ? Material.LIME_DYE : Material.GRAY_DYE,
                personalOn ? "&aDifficulty ON" : "&cDifficulty OFF",
                "toggle_personal", "0", "main",
                List.of(
                        personalOn
                                ? "&7Turn off just for you"
                                : "&7Turn back on for you",
                        personalOn
                                ? "&8OFF disables scaling, kill coins,"
                                : "&8ON restores scaling, kill coins,",
                        personalOn
                                ? "&8AI pressure, and tier buys"
                                : "&8AI pressure, and tier buys",
                        "&cWarning: &7Scaled mobs can attack other players as well"
                )));
        gui.addButton(actionBtn(player, 31,
                coinChatOn ? "difficulty.main.coin_chat_on" : "difficulty.main.coin_chat_off",
                coinChatOn ? Material.BELL : Material.GRAY_DYE,
                coinChatOn ? "&aCoin Chat ON" : "&8Coin Chat OFF",
                "toggle_coin_chat", "0", "main",
                List.of(
                        coinChatOn
                                ? "&7Hide coin drop messages"
                                : "&7Show coin drop messages again",
                        "&8Only affects Ancient Coin kill chat"
                )));
        if (ForgeBridge.isStaff(player)) {
            gui.addButton(pageBtn(player, 33, "difficulty.main.details", Material.SPYGLASS, "&8Details", "stats",
                    "&7Staff breakdown",
                    "&8CR · prestige · kit gates"));
        }
        gui.addButton(hubBtn(27));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        GuiFeedback.openCmi(gui);
    }

    private static void openTiers(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Difficulty Tiers", 5);

        boolean bridgeOk = "true".equalsIgnoreCase(ph.getOrDefault("bridge_ok", "false"));
        boolean systemOn = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("system_enabled", "false"));
        boolean allowed = bridgeOk && !"false".equalsIgnoreCase(ph.getOrDefault("player_allowed", "false"));
        if (!bridgeOk || !systemOn || !allowed) {
            CMIGuiButton locked = new CMIGuiButton(4, Material.BARRIER, "&c&lTiers Locked");
            locked.lockField();
            locked.addLore(unavailableLore(player, systemOn, bridgeOk));
            gui.addButton(locked);
            gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            GuiFeedback.openCmi(gui);
            return;
        }

        boolean personalOn = "true".equalsIgnoreCase(ph.getOrDefault("personal_enabled", "false"));
        if (!personalOn) {
            CMIGuiButton locked = new CMIGuiButton(4, Material.BARRIER, "&c&lTiers Locked");
            locked.lockField();
            locked.addLore(List.of(
                    "",
                    "&cPersonal difficulty is OFF",
                    "&7Turn it ON on the main menu to change tiers."
            ));
            gui.addButton(locked);
            gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
            gui.addButton(closeBtn(44));
            fillEmpty(gui, 5);
            GuiFeedback.openCmi(gui);
            return;
        }

        CMIGuiButton info = new CMIGuiButton(4, Material.GOLD_INGOT, "&e&lDifficulty Tiers");
        info.lockField();
        List<String> infoLore = new ArrayList<>();
        infoLore.add("");
        infoLore.add("&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"));
        infoLore.add("&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"));
        infoLore.add("&7DMZ Level &f" + ph.getOrDefault("level", "?")
                + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"));
        infoLore.add("");
        infoLore.addAll(GuiTooltips.lore("difficulty.tiers.info",
                List.of("&7Click a higher unlocked tier to buy",
                        "&7Click a lower unlocked tier to step down (free)",
                        "&8Each tier shows DMZ level or Prestige to unlock",
                        "&8Unlock with DMZ level or Prestige"), null));
        infoLore.addAll(GuiBoardHelper.tips(player,
                "&eUnlock with DMZ level &7OR &ePrestige",
                "&8Either one qualifies — prestige is not required",
                "&cCR / Battle Power does NOT unlock tiers",
                "&8If stuck transformed: drop to base form once to sync"));
        infoLore.add("");
        infoLore.addAll(coinLore(ph));
        if (ForgeBridge.isStaff(player)) {
            infoLore.add("");
            infoLore.add("&8Costs scale with your DMZ level");
            infoLore.add("&8Pay-up OK (e.g. Copper instead of Iron) — change returned");
        }
        info.addLore(infoLore);
        gui.addButton(info);

        gui.addButton(actionBtn(player, 8, "difficulty.tiers.reset", Material.RED_DYE, "&cReset to None",
                "lower_tier", "0", "tiers",
                List.of("&7Clear active tier", "&8Unlocks & coins kept", "&8Always free")));

        placeTierButtons(gui, player, ph);
        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openTitles(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Titles", 6);

        String perk = ph.getOrDefault("title_perk", "");
        CMIGuiButton info = new CMIGuiButton(4, Material.NAME_TAG, "&d&lTitles");
        info.lockField();
        List<String> titlesHeader = new ArrayList<>();
        titlesHeader.add("");
        titlesHeader.add("&7Equipped &e" + blankAsNone(ph.getOrDefault("active_title", "")));
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
        titlesHeader.addAll(GuiBoardHelper.tips(player,
                "&7Unlock via DMZ/Prestige — never regresses on lower tier",
                "&8Rarity presence: softer landings · AD dmg · TP while tier on"));
        info.addLore(titlesHeader);
        gui.addButton(info);
        boolean senseOn = "true".equalsIgnoreCase(ph.getOrDefault("title_sense", "true"));
        gui.addButton(actionBtn(player, 7, senseOn ? "difficulty.titles.sense_on" : "difficulty.titles.sense_off",
                senseOn ? Material.BELL : Material.NOTE_BLOCK,
                senseOn ? "&aTitle Sense ON" : "&8Title Sense OFF",
                "toggle_title_sense", "0", "titles",
                List.of("&7Elite / Boss recognition chat", "&8Click to toggle")));
        gui.addButton(actionBtn(player, 8, "difficulty.titles.clear", Material.NAME_TAG, "&cClear Title",
                "clear_title", "0", "titles",
                List.of("&7Unequip your title")));

        String[] ids = {
                "t1_awakened", "t2_enhanced", "t3_elite", "t4_advanced",
                "t5_master", "t6_legendary", "t7_god",
                "elite_hunter", "boss_slayer", "ascendant",
                "mutation_hunter", "untouchable", "immortal",
                "coin_lord", "survivor", "worldbreaker"
        };
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT, Material.DIAMOND,
                Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR,
                Material.DRAGON_HEAD, Material.WITHER_SKELETON_SKULL, Material.ENCHANTED_GOLDEN_APPLE,
                Material.AMETHYST_SHARD, Material.SHIELD, Material.TOTEM_OF_UNDYING,
                Material.GOLD_BLOCK, Material.CLOCK, Material.END_CRYSTAL
        };
        int[] slots = {19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 37, 39, 41};
        String equipped = ph.getOrDefault("active_title_id", "");
        boolean staff = ForgeBridge.isStaff(player);
        for (int i = 0; i < ids.length; i++) {
            String id = ids[i];
            boolean earned = "true".equalsIgnoreCase(ph.getOrDefault("title_" + id + "_earned", "false"));
            String name = ph.getOrDefault("title_" + id + "_name", id);
            String tip = ph.getOrDefault("title_" + id + "_req", "");
            String titlePerk = ph.getOrDefault("title_" + id + "_perk", "");
            String rarity = ph.getOrDefault("title_" + id + "_rarity", "");
            String rarityColor = ph.getOrDefault("title_" + id + "_rarity_color", "7");
            boolean isEquipped = id.equalsIgnoreCase(equipped);
            String title = isEquipped ? "&a● " + name
                    : earned ? "&e" + name
                    : "&8" + name;
            String state = isEquipped ? "equipped" : earned ? "unlocked" : "locked";
            String rarityLine = rarity.isBlank() ? "" : ("&" + rarityColor + rarity);
            String perkLine = titlePerk.isBlank() ? "" : ("&f" + titlePerk);
            Map<String, String> vars = Map.of(
                    "name", name,
                    "rarity", rarity,
                    "rarity_line", rarityLine,
                    "req", tip == null ? "" : tip,
                    "perk", titlePerk == null ? "" : titlePerk,
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
            List<String> lore = new ArrayList<>();
            for (String line : GuiTooltips.lore("difficulty.titles.item", defaults, vars)) {
                if (line != null && !line.isBlank()) {
                    lore.add(line);
                }
            }
            if (isEquipped) {
                lore.addAll(GuiTooltips.lore("difficulty.titles.state_equipped",
                        List.of(staff ? "&aCurrently equipped &8· tap to unequip" : "&aCurrently equipped"),
                        vars));
            } else if (earned) {
                lore.addAll(GuiTooltips.lore("difficulty.titles.state_unlocked",
                        List.of(staff ? "&aUnlocked &8· tap to equip" : "&aUnlocked"), vars));
            } else {
                lore.addAll(GuiTooltips.lore("difficulty.titles.state_locked", List.of("&cLocked"), vars));
            }
            if (earned) {
                gui.addButton(actionBtn(player, slots[i], mats[i], title, "equip_title", id, "titles", lore));
            } else {
                CMIGuiButton locked = new CMIGuiButton(slots[i], mats[i], title);
                locked.lockField();
                locked.addLore(lore);
                gui.addButton(locked);
            }
        }

        gui.addButton(pageBtn(player, 45, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(53));
        fillEmpty(gui, 6);
        GuiFeedback.openCmi(gui);
    }

    private static void openTeams(Player player) {
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Rival Teams", 5);
        CMIGuiButton header = new CMIGuiButton(4, Material.SHIELD,
                GuiTooltips.name("difficulty.team.header", "&b&lRival Teams"));
        header.lockField();
        List<String> headerLore = new ArrayList<>();
        headerLore.add("");
        for (String line : ForgeBridge.diffTeamLines(player)) {
            headerLore.add(line == null ? "" : line.replace('§', '&'));
        }
        headerLore.add("");
        headerLore.addAll(GuiBoardHelper.teamRivalLegendLines());
        header.addLore(headerLore);
        gui.addButton(header);

        String mode = ph.getOrDefault("team_mode", "personal_only");
        boolean personalMode = mode.equals("personal_only");
        boolean thresholdMode = mode.equals("threshold_bonus_only");
        boolean fullMode = mode.equals("full_team_scaling");
        gui.addButton(actionBtn(player, 20, "difficulty.team.mode_personal",
                personalMode ? Material.RED_DYE : Material.GRAY_DYE,
                personalMode ? "&c&lPersonal" : "&7Personal",
                "team", "personal", "team",
                List.of("&7Only your own tier ceiling counts",
                        "&8Rivals on Personal do not boost you",
                        personalMode ? "&c&lYour current mode" : "&eClick to select")));
        gui.addButton(actionBtn(player, 22, "difficulty.team.mode_threshold",
                thresholdMode ? Material.LIME_DYE : Material.GRAY_DYE,
                thresholdMode ? "&a&lThreshold" : "&7Threshold",
                "team", "threshold", "team",
                List.of("&7Extra max when rivals are online",
                        "&7They must also use a team mode",
                        "&7More elites, mutants, and bosses",
                        thresholdMode ? "&a&lYour current mode" : "&eClick to select")));
        gui.addButton(actionBtn(player, 24, "difficulty.team.mode_full",
                fullMode ? Material.GOLD_INGOT : Material.EMERALD,
                fullMode ? "&6&lFull" : "&2Full",
                "team", "full", "team",
                List.of("&7Threshold bonus plus nearby spare room",
                        "&7Best spawn boost when rivals are close",
                        "&8Within " + ph.getOrDefault("proximity_blocks", "48") + " blocks",
                        fullMode ? "&6&lYour current mode" : "&eClick to select")));

        List<GuiBoardHelper.TeamRivalCard> cards =
                GuiBoardHelper.parseTeamRivalCards(ForgeBridge.diffTeamMutualCards(player));
        int[] slots = GuiBoardHelper.teamMutualRivalSlots(cards.size());
        for (int i = 0; i < cards.size() && i < slots.length; i++) {
            CMIGuiButton head = new CMIGuiButton(slots[i], GuiBoardHelper.teamRivalHead(cards.get(i)));
            head.lockField();
            gui.addButton(head);
        }
        if (cards.isEmpty()) {
            CMIGuiButton empty = new CMIGuiButton(28, Material.BARRIER, "&7No mutual rivals");
            empty.lockField();
            empty.addLore(List.of("", "&7Use /rival to declare and accept",
                    "&8Both players must accept for Mutual"));
            gui.addButton(empty);
        }

        CMIGuiButton rival = new CMIGuiButton(31, Material.DIAMOND_SWORD,
                GuiTooltips.name("difficulty.team.open_rival", "&6Open Rival"));
        rival.lockField();
        rival.addLore(GuiTooltips.buttonLore("difficulty.team.open_rival",
                List.of("&7Declare, accept, or manage mutual slots")));
        rival.addCommand("lmdo lm open rival");
        rival.setCloseInv(true);
        gui.addButton(rival);

        gui.addButton(pageBtn(player, 36, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(hubBtn(40));
        gui.addButton(closeBtn(44));
        fillEmpty(gui, 5);
        GuiFeedback.openCmi(gui);
    }

    private static void openStats(Player player) {
        if (!ForgeBridge.isStaff(player)) {
            openMain(player);
            return;
        }
        Map<String, String> ph = ForgeBridge.placeholders(player);
        CMIGui gui = base(player, "&8Details (staff)", 4);
        String stateColor = ph.getOrDefault("state_color", "f");

        CMIGuiButton core = new CMIGuiButton(11, Material.NETHER_STAR, "&f&lProgression");
        core.lockField();
        core.addLore(List.of(
                "",
                "&7Current Tier &f" + ph.getOrDefault("active_tier_name", "None"),
                "&7Combat CR   &f" + ph.getOrDefault("combat_rating", "?"),
                "&7State       &" + stateColor + ph.getOrDefault("state", "?"),
                "",
                "&7DMZ &f" + ph.getOrDefault("level", "?")
                        + "  &7Prestige &f" + ph.getOrDefault("prestige", "?"),
                "&7Unlocked &fT" + ph.getOrDefault("highest_unlocked", "0"),
                "&7Title &e" + blankAsNone(ph.getOrDefault("active_title", "")),
                "",
                "&7Overhaul scale &f" + ph.getOrDefault("overhaul_scale", "x1"),
                "&7Melee &f" + ph.getOrDefault("melee_scaled", "?")
                        + "  &7Strike &f" + ph.getOrDefault("strike_scaled", "?"),
                "&7Ki &f" + ph.getOrDefault("ki_scaled", "?")
                        + "  &7Defense &f" + ph.getOrDefault("defense_scaled", "?")
        ));
        gui.addButton(core);

        CMIGuiButton counters = new CMIGuiButton(13, Material.IRON_SWORD, "&c&lCounters");
        counters.lockField();
        counters.addLore(List.of(
                "",
                "&7Class &f" + blankAsNone(ph.getOrDefault("fighting_class", "")),
                "&7Style &f" + ph.getOrDefault("fighting_style", "HYBRID"),
                "",
                "&7Top stats &f" + ph.getOrDefault("top_stats", "—"),
                "",
                "&8Mobs counter class and top stat"
        ));
        gui.addButton(counters);

        CMIGuiButton account = new CMIGuiButton(15, Material.GOLD_INGOT, "&f&lAncient Coins");
        account.lockField();
        List<String> coin = new ArrayList<>();
        coin.add("");
        coin.addAll(coinLore(ph));
        coin.add("");
        coin.add("&8Tier purchases: pay-up OK, change returned");
        account.addLore(coin);
        gui.addButton(account);

        gui.addButton(pageBtn(player, 27, "common.back", Material.ARROW, "&7Back", "main", "&7Return"));
        gui.addButton(closeBtn(35));
        fillEmpty(gui, 4);
        GuiFeedback.openCmi(gui);
    }

    private static void placeTierButtons(CMIGui gui, Player player, Map<String, String> ph) {
        Material[] mats = {
                Material.COPPER_INGOT, Material.IRON_INGOT, Material.GOLD_INGOT,
                Material.DIAMOND, Material.EMERALD, Material.NETHERITE_INGOT, Material.NETHER_STAR
        };
        boolean staff = ForgeBridge.isStaff(player);
        int activeTier = parseInt(ph.getOrDefault("active_tier", "0"));
        String key = "difficulty.tiers.tier";
        for (int t = 1; t <= 7; t++) {
            int slot = TIER_SLOTS[t - 1];
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

            List<String> lore = GuiBoardHelper.difficultyTierButtonLore(active, canLower, unlocked, staff);

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

            if (unlocked && !active) {
                gui.addButton(actionBtn(player, slot, key, mats[t - 1], title,
                        "activate", String.valueOf(t), "tiers", lore, tierVars));
            } else {
                gui.addButton(tipButton(slot, key, mats[t - 1], title, lore, tierVars));
            }
        }
    }

    /** Clean copy for players; command tips only for staff. */
    private static List<String> unavailableLore(Player player, boolean systemOn, boolean bridgeOk) {
        boolean staff = ForgeBridge.isStaff(player);
        if (!bridgeOk) {
            if (staff) {
                return List.of("", "&cForge Adaptive Difficulty mod unreachable",
                        "&7Check mods/ for AdaptiveDifficulty-*.jar",
                        "&8GUI actions are disabled until the mod loads");
            }
            return List.of("", "&cAdaptive Difficulty is unavailable", "&7Please try again later");
        }
        if (!systemOn) {
            if (staff) {
                return List.of("", "&cAdaptive Difficulty is off",
                        "&7No scaling, coins, or purchases",
                        "&8Re-enable: &f/difficulty admin on");
            }
            return List.of("", "&cAdaptive Difficulty is off", "&7Please try again later");
        }
        if (staff) {
            return List.of("", "&eTesting whitelist is on", "&7You are not on the whitelist",
                    "&8Add: &f/difficulty admin whitelist add <you>");
        }
        return List.of("", "&eNot available right now", "&7Ask an admin if you need access");
    }

    private static List<String> statusLore(
            Map<String, String> ph, String stateColor, boolean staff, boolean personalOn) {
        List<String> lore = new ArrayList<>();
        lore.add("");
        if (!personalOn) {
            lore.add("&cDifficulty is OFF for you");
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
        GuiBoardHelper.addOverhaulCombat(lore, ph);
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

    private static int parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (Exception e) {
            return 0;
        }
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
        // Bukkit-only /lmdo — Mohist may route /difficulty do to Forge's word()-limited tree.
        btn.addCommand("lmdo difficulty " + action + " " + arg + " " + returnPage);
        return btn;
    }

    /** Display-only tile (no command) with wired name/lore — used for locked/non-clickable states. */
    private static CMIGuiButton tipButton(
            int slot, String key, Material mat, String name, List<String> tip, Map<String, String> vars
    ) {
        String display = key == null || key.isBlank() ? name : GuiTooltips.name(key, name, vars);
        CMIGuiButton btn = new CMIGuiButton(slot, mat, display);
        btn.lockField();
        btn.addLore(key == null || key.isBlank()
                ? withBlank(tip)
                : GuiTooltips.buttonLore(key, tip, vars, null));
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
        btn.addCommand("lmdo difficulty page " + page);
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

    private static CMIGuiButton hubBtn(int slot) {
        return GuiNav.cmiHubButton(slot);
    }

    private static CMIGuiButton closeBtn(int slot) {
        return GuiNav.cmiCloseButton(slot);
    }
}
