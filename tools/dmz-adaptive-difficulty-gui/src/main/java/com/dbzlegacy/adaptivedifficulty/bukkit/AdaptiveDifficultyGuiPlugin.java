package com.dbzlegacy.adaptivedifficulty.bukkit;

import java.util.Locale;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Bukkit-side entrypoint so {@code /difficulty}, {@code /rival}, and {@code /spar}
 * work for all players on Mohist.
 * Prefers CMILib (CMI) inventory GUIs, then a plain chest GUI.
 * <p>
 * On Mohist, this plugin owns {@code /difficulty}, {@code /rival}, and {@code /spar} —
 * admin switches (on/off/whitelist) must be handled here and forwarded into the Forge mod config.
 */
public final class AdaptiveDifficultyGuiPlugin extends JavaPlugin {
    private DifficultyChestGui chestGui;
    private RivalChestGui rivalChestGui;
    private SparChestGui sparChestGui;
    private HubChestGui hubChestGui;
    private ProgressionChestGui progressionChestGui;
    private PrestigeChestGui prestigeChestGui;
    private SkillsChestGui skillsChestGui;

    @Override
    public void onEnable() {
        chestGui = new DifficultyChestGui(this);
        rivalChestGui = new RivalChestGui(this);
        sparChestGui = new SparChestGui(this);
        hubChestGui = new HubChestGui(this);
        progressionChestGui = new ProgressionChestGui(this);
        prestigeChestGui = new PrestigeChestGui(this);
        skillsChestGui = new SkillsChestGui(this);
        getServer().getPluginManager().registerEvents(chestGui, this);
        getServer().getPluginManager().registerEvents(rivalChestGui, this);
        getServer().getPluginManager().registerEvents(sparChestGui, this);
        getServer().getPluginManager().registerEvents(hubChestGui, this);
        getServer().getPluginManager().registerEvents(progressionChestGui, this);
        getServer().getPluginManager().registerEvents(prestigeChestGui, this);
        getServer().getPluginManager().registerEvents(skillsChestGui, this);

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DmzDiffExpansion(this).register();
            getLogger().info("Registered PlaceholderAPI expansion: dmzdiff");
        }

        boolean forge = ForgeBridge.forgeAvailable();
        getLogger().info("GUI backend: CMILib=" + CmiDifficultyGui.available()
                + " forgeMod=" + forge);
        if (!forge) {
            String err = ForgeBridge.lastError();
            getLogger().severe("LegacyMechanics Forge mod NOT reachable — GUI actions will fail."
                    + (err == null || err.isBlank() ? "" : " (" + err + ")"));
            getLogger().severe("Install mods/LegacyMechanics-*.jar and restart.");
        } else {
            String modVer = ForgeBridge.modVersion();
            String pluginVer = getDescription().getVersion();
            if (modVer == null || modVer.isBlank()) {
                getLogger().severe("VERSION HANDSHAKE FAILED: could not read AdaptiveDifficultyMod.VERSION"
                        + " — GUI may call missing Forge APIs.");
            } else if (pluginVer != null && !modVer.equals(pluginVer)) {
                getLogger().severe("VERSION SKEW: Forge mod=" + modVer + " GUI plugin=" + pluginVer
                        + " — install matching LegacyMechanics + LegacyMechanicsGUI jars (same version).");
            } else {
                getLogger().info("Version handshake OK: " + pluginVer);
            }
        }
        getLogger().info("Registered Bukkit /difficulty /rival /spar /lm /progression /prestige /skills /skillcheck.");
    }

    /**
     * Called by the Forge mod via reflection when {@code guiBackend} is cmi/chest/auto.
     * Always opens an inventory GUI (does not re-read chat preference — Forge already decided).
     */
    public void openMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        AdminInspectSessions.clear(player.getUniqueId());
        openInventory(player, page);
    }

    /**
     * Preferred Forge entry — UUID only, no Player crossing Mohist classloaders.
     * Always runs on the Bukkit primary thread (CMI requires it).
     */
    public void openMenuForUuid(UUID playerId, String page) {
        if (playerId == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        Runnable task = () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                AdminInspectSessions.clear(p.getUniqueId());
                openInventory(p, target);
            } else {
                getLogger().warning("openMenuForUuid: player offline/unresolved " + playerId);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    /** Chest-only open for Forge {@code guiBackend=chest}. */
    public void openChestMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        AdminInspectSessions.clear(player.getUniqueId());
        chestGui.open(player, page);
    }

    /** UUID chest open — Mohist-safe Forge entry for {@code guiBackend=chest}. */
    public void openChestMenuForUuid(UUID playerId, String page) {
        if (playerId == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        Runnable task = () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                AdminInspectSessions.clear(p.getUniqueId());
                chestGui.open(p, target);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    /**
     * Staff inspect: open chest GUI for admin while painting/editing {@code subjectId}.
     * Chest-only so clicks are bound to the subject (CMI buttons would edit the admin).
     */
    public void openInspectForUuid(UUID adminId, UUID subjectId, String page) {
        if (adminId == null || subjectId == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        Runnable task = () -> {
            Player admin = Bukkit.getPlayer(adminId);
            Player subject = Bukkit.getPlayer(subjectId);
            if (admin == null || !admin.isOnline()) {
                getLogger().warning("openInspectForUuid: admin offline " + adminId);
                return;
            }
            if (subject == null || !subject.isOnline()) {
                admin.sendMessage("§cThat player is not online.");
                return;
            }
            if (!ForgeBridge.isStaff(admin)) {
                admin.sendMessage("§cStaff only.");
                return;
            }
            AdminInspectSessions.set(admin.getUniqueId(), subject.getUniqueId());
            admin.sendMessage("§eInspecting §f" + subject.getName()
                    + "§e's LegacyMechanics Difficulty GUI (chest). Edits apply to them.");
            chestGui.openAs(admin, subject, target);
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    /** Player-facing open that honors Forge {@code guiBackend}. */
    public void openMenuRespectingConfig(Player player, String page) {
        if (player == null) {
            return;
        }
        AdminInspectSessions.clear(player.getUniqueId());
        String backend = ForgeBridge.guiBackend();
        if ("chat".equals(backend)) {
            if (!ForgeBridge.openChatMenu(player, page)) {
                player.sendMessage("§cChat difficulty menu unavailable (is the Forge mod loaded?).");
            }
            return;
        }
        if ("chest".equals(backend)) {
            chestGui.open(player, page);
            return;
        }
        // cmi / auto / unknown → CMI then chest
        openInventory(player, page);
    }

    private void openInventory(Player player, String page) {
        if (CmiDifficultyGui.available() && CmiDifficultyGui.open(player, page)) {
            return;
        }
        if (CmiDifficultyGui.available()) {
            getLogger().warning("CMI GUI open failed for " + player.getName()
                    + " — falling back to chest GUI. Check CMILib version.");
        } else {
            getLogger().warning("CMILib/CMI not available — using chest GUI for "
                    + player.getName() + ". Install CMILib + CMI for the CMI inventory UI.");
        }
        chestGui.open(player, page);
    }

    private void openInspect(Player admin, Player subject, String page) {
        AdminInspectSessions.set(admin.getUniqueId(), subject.getUniqueId());
        admin.sendMessage("§eInspecting §f" + subject.getName()
                + "§e's LegacyMechanics Difficulty GUI. Edits apply to them.");
        admin.sendMessage("§8Exit: §f/difficulty §8or §f/difficulty admin gui clear");
        chestGui.openAs(admin, subject, page == null || page.isBlank() ? "main" : page);
    }

    // ── Rival ──────────────────────────────────────────────────────────

    public void openRivalMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        openRivalInventory(player, page);
    }

    public void openRivalMenuForUuid(UUID playerId, String page) {
        if (playerId == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        Runnable task = () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                openRivalInventory(p, target);
            } else {
                getLogger().warning("openRivalMenuForUuid: player offline/unresolved " + playerId);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    public void openRivalChestMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        rivalChestGui.open(player, page);
    }

    public void openRivalChestMenuForUuid(UUID playerId, String page) {
        if (playerId == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        Runnable task = () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                rivalChestGui.open(p, target);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    private void openRivalInventory(Player player, String page) {
        if (CmiRivalGui.available() && CmiRivalGui.open(player, page)) {
            return;
        }
        rivalChestGui.open(player, page);
    }

    private void openRivalRespectingConfig(Player player, String page) {
        if (player == null) {
            return;
        }
        String backend = ForgeBridge.guiBackend();
        if ("chat".equals(backend)) {
            if (!ForgeBridge.openRivalChatMenu(player, page)) {
                player.sendMessage("§cRival chat menu unavailable (is the Forge mod loaded?).");
            }
            return;
        }
        if ("chest".equals(backend)) {
            rivalChestGui.open(player, page);
            return;
        }
        openRivalInventory(player, page);
    }

    // ── Spar ───────────────────────────────────────────────────────────

    public void openSparMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        openSparInventory(player, page);
    }

    public void openSparMenuForUuid(UUID playerId, String page) {
        if (playerId == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        Runnable task = () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                openSparInventory(p, target);
            } else {
                getLogger().warning("openSparMenuForUuid: player offline/unresolved " + playerId);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    public void openSparChestMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        sparChestGui.open(player, page);
    }

    public void openSparChestMenuForUuid(UUID playerId, String page) {
        if (playerId == null) {
            return;
        }
        String target = page == null || page.isBlank() ? "main" : page;
        Runnable task = () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                sparChestGui.open(p, target);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    private void openSparInventory(Player player, String page) {
        if (CmiSparGui.available() && CmiSparGui.open(player, page)) {
            return;
        }
        sparChestGui.open(player, page);
    }

    private void openSparRespectingConfig(Player player, String page) {
        if (player == null) {
            return;
        }
        String backend = ForgeBridge.guiBackend();
        if ("chat".equals(backend)) {
            if (!ForgeBridge.openSparChatMenu(player, page)) {
                player.sendMessage("§cSpar chat menu unavailable (is the Forge mod loaded?).");
            }
            return;
        }
        if ("chest".equals(backend)) {
            sparChestGui.open(player, page);
            return;
        }
        openSparInventory(player, page);
    }

    // ── Hub (/lm) ──────────────────────────────────────────────────────

    public void openHubMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        openHubInventory(player, page);
    }

    public void openHubMenuForUuid(UUID playerId, String page) {
        runForUuid(playerId, page, "main", this::openHubInventory, "openHubMenuForUuid");
    }

    public void openHubChestMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        hubChestGui.open(player, page);
    }

    public void openHubChestMenuForUuid(UUID playerId, String page) {
        runForUuid(playerId, page, "main", hubChestGui::open, null);
    }

    private void openHubInventory(Player player, String page) {
        if (CmiHubGui.available() && CmiHubGui.open(player, page)) {
            return;
        }
        hubChestGui.open(player, page);
    }

    private void openHubRespectingConfig(Player player, String page) {
        if (player == null) {
            return;
        }
        String backend = ForgeBridge.guiBackend();
        if ("chat".equals(backend)) {
            if (!ForgeBridge.openHubChatMenu(player, page)) {
                player.sendMessage("§cHub chat menu unavailable (is the Forge mod loaded?).");
            }
            return;
        }
        if ("chest".equals(backend)) {
            hubChestGui.open(player, page);
            return;
        }
        openHubInventory(player, page);
    }

    // ── Progression ────────────────────────────────────────────────────

    public void openProgressionMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        openProgressionInventory(player, page);
    }

    public void openProgressionMenuForUuid(UUID playerId, String page) {
        runForUuid(playerId, page, "main", this::openProgressionInventory, "openProgressionMenuForUuid");
    }

    public void openProgressionChestMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        progressionChestGui.open(player, page);
    }

    public void openProgressionChestMenuForUuid(UUID playerId, String page) {
        runForUuid(playerId, page, "main", progressionChestGui::open, null);
    }

    private void openProgressionInventory(Player player, String page) {
        if (CmiProgressionGui.available() && CmiProgressionGui.open(player, page)) {
            return;
        }
        progressionChestGui.open(player, page);
    }

    private void openProgressionRespectingConfig(Player player, String page) {
        if (player == null) {
            return;
        }
        String backend = ForgeBridge.guiBackend();
        if ("chat".equals(backend)) {
            if (!ForgeBridge.openProgressionChatMenu(player, page)) {
                player.sendMessage("§cProgression chat menu unavailable (is the Forge mod loaded?).");
            }
            return;
        }
        if ("chest".equals(backend)) {
            progressionChestGui.open(player, page);
            return;
        }
        openProgressionInventory(player, page);
    }

    // ── Prestige ───────────────────────────────────────────────────────

    public void openPrestigeMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        openPrestigeInventory(player, page);
    }

    public void openPrestigeMenuForUuid(UUID playerId, String page) {
        runForUuid(playerId, page, "main", this::openPrestigeInventory, "openPrestigeMenuForUuid");
    }

    public void openPrestigeChestMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        prestigeChestGui.open(player, page);
    }

    public void openPrestigeChestMenuForUuid(UUID playerId, String page) {
        runForUuid(playerId, page, "main", prestigeChestGui::open, null);
    }

    private void openPrestigeInventory(Player player, String page) {
        if (CmiPrestigeGui.available() && CmiPrestigeGui.open(player, page)) {
            return;
        }
        prestigeChestGui.open(player, page);
    }

    private void openPrestigeRespectingConfig(Player player, String page) {
        if (player == null) {
            return;
        }
        String backend = ForgeBridge.guiBackend();
        if ("chat".equals(backend)) {
            if (!ForgeBridge.forwardCommand(player, "prestige")) {
                openPrestigeInventory(player, page);
            }
            return;
        }
        if ("chest".equals(backend)) {
            prestigeChestGui.open(player, page);
            return;
        }
        openPrestigeInventory(player, page);
    }

    // ── Skills ─────────────────────────────────────────────────────────

    public void openSkillsMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        openSkillsInventory(player, page);
    }

    public void openSkillsMenuForUuid(UUID playerId, String page) {
        runForUuid(playerId, page, "core", this::openSkillsInventory, "openSkillsMenuForUuid");
    }

    public void openSkillsChestMenu(Player player, String page) {
        if (player == null) {
            return;
        }
        skillsChestGui.open(player, page);
    }

    public void openSkillsChestMenuForUuid(UUID playerId, String page) {
        runForUuid(playerId, page, "core", skillsChestGui::open, null);
    }

    private void openSkillsInventory(Player player, String page) {
        if (CmiSkillsGui.available() && CmiSkillsGui.open(player, page)) {
            return;
        }
        skillsChestGui.open(player, page);
    }

    private void openSkillsRespectingConfig(Player player, String page) {
        if (player == null) {
            return;
        }
        String backend = ForgeBridge.guiBackend();
        if ("chat".equals(backend)) {
            String p = page == null || page.isBlank() ? "core" : page;
            if (!ForgeBridge.forwardCommand(player, "skills do page " + p)) {
                openSkillsInventory(player, page);
            }
            return;
        }
        if ("chest".equals(backend)) {
            skillsChestGui.open(player, page);
            return;
        }
        openSkillsInventory(player, page);
    }

    private void runForUuid(
            UUID playerId, String page, String defaultPage,
            java.util.function.BiConsumer<Player, String> opener, String warnLabel) {
        if (playerId == null) {
            return;
        }
        String target = page == null || page.isBlank() ? defaultPage : page;
        Runnable task = () -> {
            Player p = Bukkit.getPlayer(playerId);
            if (p != null && p.isOnline()) {
                opener.accept(p, target);
            } else if (warnLabel != null) {
                getLogger().warning(warnLabel + ": player offline/unresolved " + playerId);
            }
        };
        if (Bukkit.isPrimaryThread()) {
            task.run();
        } else {
            Bukkit.getScheduler().runTask(this, task);
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String name = command.getName().toLowerCase();
        if ("dmzdiffgui".equals(name)) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                player.sendMessage("§cNo permission: dmzdiff.gui");
                return true;
            }
            String page = args.length > 0 ? args[0] : "main";
            if (("settings".equalsIgnoreCase(page) || "stats".equalsIgnoreCase(page)
                    || "details".equalsIgnoreCase(page) || "statistics".equalsIgnoreCase(page))
                    && !ForgeBridge.isStaff(player)) {
                player.sendMessage("§cStaff only.");
                page = "main";
            }
            // Force inventory — ignore guiBackend=chat (debug / recovery).
            AdminInspectSessions.clear(player.getUniqueId());
            openInventory(player, page);
            return true;
        }
        if ("rival".equals(name)) {
            return handleRival(sender, args);
        }
        if ("spar".equals(name)) {
            return handleSpar(sender, args);
        }
        if ("lm".equals(name) || "legacymechanics".equals(name)) {
            return handleHub(sender, args);
        }
        if ("progression".equals(name) || "prog".equals(name)) {
            return handleProgression(sender, args);
        }
        if ("prestige".equals(name)) {
            return handlePrestige(sender, args);
        }
        if ("skills".equals(name)) {
            return handleSkills(sender, args);
        }
        if ("skillcheck".equals(name)) {
            return handleSkillCheck(sender, args);
        }
        if (!"difficulty".equals(name)) {
            return false;
        }
        return handleDifficulty(sender, args);
    }

    private boolean handleHub(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            if (args.length > 0 && "admin".equalsIgnoreCase(args[0])) {
                return handleLmAdmin(sender, args);
            }
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
            player.sendMessage("§cNo permission: dmzdiff.gui");
            return true;
        }
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0])) {
            openHubRespectingConfig(player, "main");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("admin".equals(sub)) {
            return handleLmAdmin(sender, args);
        }
        if ("do".equals(sub)) {
            String action = args.length > 1 ? args[1] : "";
            String arg = args.length > 2 ? args[2] : "";
            String returnPage = args.length > 3 ? args[3] : null;
            if ("open".equalsIgnoreCase(action)) {
                openSystemFromHub(player, arg);
                return true;
            }
            String reopen;
            if ("page".equalsIgnoreCase(action) || "refresh".equalsIgnoreCase(action)) {
                reopen = arg == null || arg.isBlank() ? "main" : arg;
            } else {
                reopen = returnPage == null || returnPage.isBlank() ? "main" : returnPage;
                String msg = ForgeBridge.hubHandleDo(player, action, arg, reopen);
                if (msg != null && !msg.isBlank()) {
                    if (!msg.startsWith("§")) {
                        msg = "§a" + msg;
                    }
                    player.sendMessage(msg);
                }
            }
            if ("chat".equals(ForgeBridge.guiBackend())) {
                ForgeBridge.openHubChatMenu(player, reopen);
            } else {
                openHubInventory(player, reopen);
            }
            return true;
        }
        if ("help".equals(sub) || "logs".equals(sub) || "syslog".equals(sub)) {
            openHubRespectingConfig(player, sub);
            return true;
        }
        forwardToForge(player, "lm", args);
        return true;
    }

    /**
     * Open a system inventory from the hub without {@code performCommand} to bare
     * {@code /difficulty}/{@code /rival}/… (keeps guide path on {@code /lm}).
     */
    public void openSystemFromHub(Player player, String system) {
        if (player == null) {
            return;
        }
        String s = system == null ? "" : system.toLowerCase(Locale.ROOT).trim();
        switch (s) {
            case "difficulty", "diff", "ad" -> openMenuRespectingConfig(player, "main");
            case "rival", "rivals", "rivalry" -> openRivalRespectingConfig(player, "main");
            case "spar", "sparring" -> openSparRespectingConfig(player, "main");
            case "skillcheck", "skill_check" -> {
                if (!ForgeBridge.hasSkillCheck(player) && !ForgeBridge.isStaff(player)) {
                    player.sendMessage("§cSkill Check requires donator access.");
                    openHubInventory(player, "main");
                    return;
                }
                ForgeBridge.markSkillCheckSession(player);
                openSkillsRespectingConfig(player, "core");
            }
            case "skills", "skill" -> {
                if (!ForgeBridge.isStaff(player)) {
                    player.sendMessage("§cStaff only. Use Skill Check if you have access.");
                    openHubInventory(player, "main");
                    return;
                }
                openSkillsRespectingConfig(player, "core");
            }
            case "prestige" -> {
                if (!ForgeBridge.isStaff(player)) {
                    player.sendMessage("§cStaff only.");
                    openHubInventory(player, "main");
                    return;
                }
                openPrestigeRespectingConfig(player, "main");
            }
            case "progression", "prog" -> {
                if (!ForgeBridge.isStaff(player)) {
                    player.sendMessage("§cStaff only.");
                    openHubInventory(player, "main");
                    return;
                }
                openProgressionRespectingConfig(player, "main");
            }
            case "admin" -> {
                if (!ForgeBridge.isStaff(player)) {
                    player.sendMessage("§cStaff only.");
                    return;
                }
                sendLmAdminHelp(player);
            }
            case "logs", "syslog" -> {
                if (!ForgeBridge.isStaff(player)) {
                    player.sendMessage("§cStaff only.");
                    return;
                }
                openHubInventory(player, "logs");
            }
            case "help" -> openHubInventory(player, "help");
            default -> {
                player.sendMessage("§cUnknown system: " + s
                        + " §8(difficulty|rival|spar|skillcheck|prestige|progression)");
                openHubInventory(player, "main");
            }
        }
    }

    private boolean handleLmAdmin(CommandSender sender, String[] args) {
        boolean staff = sender instanceof Player p
                ? ForgeBridge.isStaff(p)
                : sender.isOp() || sender.hasPermission(ForgeBridge.adminPermission());
        if (!staff) {
            sender.sendMessage("§cStaff only.");
            return true;
        }
        if (args.length < 2 || "help".equalsIgnoreCase(args[1])) {
            sendLmAdminHelp(sender);
            return true;
        }
        String sub = args[1].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                if (ForgeBridge.reloadConfig()) {
                    sender.sendMessage("§aLegacyMechanics config reloaded.");
                } else {
                    String err = ForgeBridge.lastError();
                    sender.sendMessage("§cReload failed" + (err == null ? "." : ": " + err));
                }
            }
            case "syslog" -> {
                String mode = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "status";
                if (sender instanceof Player player) {
                    String msg = ForgeBridge.hubHandleDo(player, "syslog", mode, "logs");
                    sender.sendMessage(msg == null || msg.isBlank() ? "§7Done." : msg);
                } else {
                    forwardAdminSyslogConsole(sender, mode);
                }
            }
            case "open" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only for open.");
                    return true;
                }
                String system = args.length > 2 ? args[2] : "difficulty";
                openSystemFromHub(player, system);
            }
            default -> {
                sender.sendMessage("§cUnknown: /lm admin " + sub);
                sendLmAdminHelp(sender);
            }
        }
        return true;
    }

    private void forwardAdminSyslogConsole(CommandSender sender, String mode) {
        // Console: reuse difficulty admin syslog via forge forward if possible
        sender.sendMessage("§7Use in-game §f/lm admin syslog " + mode
                + " §7or §f/difficulty admin syslog " + mode);
    }

    private static void sendLmAdminHelp(CommandSender sender) {
        sender.sendMessage("§6§l/lm admin §8— Legacy Mechanics");
        sender.sendMessage("§e/lm admin help §7— this list");
        sender.sendMessage("§e/lm admin reload §7— reload config");
        sender.sendMessage("§e/lm admin syslog on|off|status|flush");
        sender.sendMessage("§e/lm admin open <difficulty|rival|spar|progression|prestige|skills>");
        sender.sendMessage("§8Also: /difficulty admin · /rival admin · /spar admin · /progression admin");
    }

    private boolean handleProgression(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        // Meditation status is player-facing (not staff-gated). Staff-only: meditation next.
        if (args.length > 0 && "meditation".equalsIgnoreCase(args[0])) {
            if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                player.sendMessage("§cNo permission: dmzdiff.gui");
                return true;
            }
            String medSub = args.length > 1 ? args[1].toLowerCase() : "status";
            if ("next".equals(medSub) || "advance".equals(medSub) || "cycle".equals(medSub)) {
                if (!ForgeBridge.isStaff(player)) {
                    player.sendMessage("§cStaff only.");
                    return true;
                }
                sendMultiline(player, ForgeBridge.meditationAdvance(player));
                sendMultiline(player, ForgeBridge.meditationExplain());
                return true;
            }
            sendMultiline(player, ForgeBridge.meditationExplain());
            return true;
        }
        // Android convert — dedicated Forge bridge (Mohist shadows /progression brigadier).
        if (args.length > 0 && "android".equalsIgnoreCase(args[0])) {
            if (!ForgeBridge.isStaff(player)) {
                player.sendMessage("§cStaff only.");
                return true;
            }
            if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                player.sendMessage("§cNo permission: dmzdiff.gui");
                return true;
            }
            String target = args.length > 1 ? args[1] : "";
            sendMultiline(player, ForgeBridge.androidConvert(player, target));
            return true;
        }
        if (!ForgeBridge.isStaff(player)) {
            player.sendMessage("§cStaff only.");
            return true;
        }
        if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
            player.sendMessage("§cNo permission: dmzdiff.gui");
            return true;
        }
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0])) {
            openProgressionRespectingConfig(player, "main");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("do".equals(sub)) {
            String action = args.length > 1 ? args[1] : "";
            String arg = args.length > 2 ? args[2] : "";
            String returnPage = args.length > 3 ? args[3] : null;
            String reopen;
            if ("page".equalsIgnoreCase(action) || "refresh".equalsIgnoreCase(action)) {
                reopen = arg == null || arg.isBlank() ? "main" : arg;
            } else {
                reopen = returnPage == null || returnPage.isBlank() ? "main" : returnPage;
                String msg = ForgeBridge.progressionHandleDo(player, action, arg, reopen);
                sendMultiline(player, msg);
            }
            if ("chat".equals(ForgeBridge.guiBackend())) {
                ForgeBridge.openProgressionChatMenu(player, reopen);
            } else {
                openProgressionInventory(player, reopen);
            }
            return true;
        }
        if ("status".equals(sub) || "help".equals(sub) || "admin".equals(sub)
                || "flags".equals(sub) || "fabled".equals(sub)) {
            openProgressionRespectingConfig(player, sub);
            return true;
        }
        forwardToForge(player, "progression", args);
        return true;
    }

    private boolean handlePrestige(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!ForgeBridge.isStaff(player)) {
            player.sendMessage("§cStaff only.");
            return true;
        }
        if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
            player.sendMessage("§cNo permission: dmzdiff.gui");
            return true;
        }
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0])) {
            openPrestigeRespectingConfig(player, "main");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("do".equals(sub)) {
            String action = args.length > 1 ? args[1] : "";
            String arg = args.length > 2 ? args[2] : "";
            String returnPage = args.length > 3 ? args[3] : null;
            String reopen;
            if ("page".equalsIgnoreCase(action) || "refresh".equalsIgnoreCase(action)) {
                reopen = arg == null || arg.isBlank() ? "main" : arg;
            } else {
                reopen = returnPage == null || returnPage.isBlank() ? "main" : returnPage;
                String msg = ForgeBridge.prestigeHandleDo(player, action, arg, reopen);
                if (msg != null && !msg.isBlank()) {
                    if (!msg.startsWith("§")) {
                        msg = "§a" + msg;
                    }
                    player.sendMessage(msg);
                }
            }
            openPrestigeInventory(player, reopen);
            return true;
        }
        forwardToForge(player, "prestige", args);
        return true;
    }

    private boolean handleSkills(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!ForgeBridge.isStaff(player)) {
            player.sendMessage("§cStaff only.");
            return true;
        }
        if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
            player.sendMessage("§cNo permission: dmzdiff.gui");
            return true;
        }
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0]) || "check".equalsIgnoreCase(args[0])) {
            openSkillsRespectingConfig(player, "core");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("do".equals(sub)) {
            String action = args.length > 1 ? args[1] : "";
            String arg = args.length > 2 ? args[2] : "";
            String returnPage = args.length > 3 ? args[3] : null;
            String reopen;
            if ("page".equalsIgnoreCase(action) || "refresh".equalsIgnoreCase(action)) {
                reopen = arg == null || arg.isBlank() ? "core" : arg;
            } else {
                reopen = returnPage == null || returnPage.isBlank() ? "core" : returnPage;
                String msg = ForgeBridge.skillsHandleDo(player, action, arg, reopen);
                if (msg != null && !msg.isBlank()) {
                    if (!msg.startsWith("§")) {
                        msg = "§a" + msg;
                    }
                    player.sendMessage(msg);
                }
            }
            openSkillsInventory(player, reopen);
            return true;
        }
        if ("core".equals(sub) || "advanced".equals(sub) || "saga".equals(sub) || "help".equals(sub)) {
            openSkillsRespectingConfig(player, sub);
            return true;
        }
        forwardToForge(player, "skills", args);
        return true;
    }

    private boolean handleSkillCheck(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!ForgeBridge.hasSkillCheck(player)) {
            player.sendMessage("§cNo permission: legacymechanics.skillcheck");
            return true;
        }
        if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
            player.sendMessage("§cNo permission: dmzdiff.gui");
            return true;
        }
        ForgeBridge.markSkillCheckSession(player);
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0])) {
            openSkillsRespectingConfig(player, "core");
            return true;
        }
        String sub = args[0].toLowerCase();
        if ("do".equals(sub)) {
            String action = args.length > 1 ? args[1] : "";
            String arg = args.length > 2 ? args[2] : "";
            String reopen;
            if ("page".equalsIgnoreCase(action) || "refresh".equalsIgnoreCase(action)) {
                reopen = arg == null || arg.isBlank() ? "core" : arg;
            } else {
                reopen = "core";
            }
            openSkillsInventory(player, reopen);
            return true;
        }
        if ("core".equals(sub) || "advanced".equals(sub) || "saga".equals(sub) || "help".equals(sub)) {
            openSkillsRespectingConfig(player, sub);
            return true;
        }
        forwardToForge(player, "skillcheck", args);
        return true;
    }

    private boolean handleRival(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
            player.sendMessage("§cNo permission: dmzdiff.gui");
            return true;
        }
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0])) {
            openRivalRespectingConfig(player, "main");
            return true;
        }
        String sub = args[0].toLowerCase();
        // Staff admin: bare → GUI; with args → ForgeBridge handleDo (not brigadier perm gate)
        if ("admin".equals(sub)) {
            if (!ForgeBridge.isStaff(player)) {
                player.sendMessage("§cStaff only.");
                return true;
            }
            if (args.length == 1 || "gui".equalsIgnoreCase(args[1])) {
                openRivalRespectingConfig(player, "admin");
                return true;
            }
            if ("open".equalsIgnoreCase(args[1])) {
                String page = args.length > 2 ? args[2] : "main";
                openRivalRespectingConfig(player, page);
                return true;
            }
            StringBuilder adminArg = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i++) {
                adminArg.append(' ').append(args[i]);
            }
            String msg = ForgeBridge.rivalHandleDo(player, "admin", adminArg.toString(), "admin");
            sendMultiline(player, msg);
            return true;
        }
        if ("do".equals(sub)) {
            String action = args.length > 1 ? args[1] : "";
            String arg = args.length > 2 ? args[2] : "";
            String returnPage = args.length > 3 ? args[3] : null;
            String reopen;
            if ("page".equalsIgnoreCase(action) || "refresh".equalsIgnoreCase(action)) {
                reopen = arg == null || arg.isBlank() ? "main" : arg;
            } else {
                reopen = returnPage == null || returnPage.isBlank() ? "main" : returnPage;
                String msg = ForgeBridge.rivalHandleDo(player, action, arg, reopen);
                sendMultiline(player, msg);
            }
            if ("chat".equals(ForgeBridge.guiBackend())) {
                ForgeBridge.openRivalChatMenu(player, reopen);
            } else {
                openRivalInventory(player, reopen);
            }
            return true;
        }
        // GUI pages (only when no extra forge args — e.g. /rival challenge send X forwards)
        if (args.length == 1 && switch (sub) {
            case "list", "stats", "top", "season", "quests", "achievements", "hof",
                 "journal", "title", "titles", "challenge", "help", "progress",
                 "actions", "history", "pending", "invites",
                 "pick_declare", "pick_accept", "pick_decline", "pick_remove", "pick_challenge",
                 "pick_spectate", "pick_silent" -> true;
            default -> false;
        }) {
            String page = "titles".equals(sub) ? "title" : sub;
            openRivalRespectingConfig(player, page);
            return true;
        }
        // Known forge actions that need typed names — keep forwarding
        if (isRivalForgeAction(sub)) {
            forwardToForge(player, "rival", args);
            return true;
        }
        // Bare /rival <onlinePlayer> → silent rival via Forge
        if (args.length == 1 && Bukkit.getPlayerExact(args[0]) != null) {
            forwardToForge(player, "rival", args);
            return true;
        }
        // Unknown non-admin chatter → open GUI instead of failing typed command
        openRivalRespectingConfig(player, "main");
        return true;
    }

    private static boolean isRivalForgeAction(String sub) {
        return switch (sub.toLowerCase()) {
            case "declare", "accept", "decline", "deny", "remove", "silent",
                 "challenge", "spectate", "tpmsg", "instinct",
                 "refresh", "save" -> true;
            default -> false;
        };
    }

    private boolean handleSpar(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Players only.");
            return true;
        }
        if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
            player.sendMessage("§cNo permission: dmzdiff.gui");
            return true;
        }
        if (args.length == 0 || "gui".equalsIgnoreCase(args[0])) {
            openSparRespectingConfig(player, "main");
            return true;
        }
        String sub = args[0].toLowerCase();
        // Staff admin: bare → GUI; with args → ForgeBridge handleDo (not brigadier perm gate)
        if ("admin".equals(sub)) {
            if (!ForgeBridge.isStaff(player)) {
                player.sendMessage("§cStaff only.");
                return true;
            }
            if (args.length == 1 || "gui".equalsIgnoreCase(args[1])) {
                openSparRespectingConfig(player, "admin");
                return true;
            }
            StringBuilder adminArg = new StringBuilder(args[1]);
            for (int i = 2; i < args.length; i++) {
                adminArg.append(' ').append(args[i]);
            }
            String msg = ForgeBridge.sparHandleDo(player, "admin", adminArg.toString(), "admin");
            sendMultiline(player, msg);
            return true;
        }
        if ("do".equals(sub)) {
            String action = args.length > 1 ? args[1] : "";
            String arg = args.length > 2 ? args[2] : "";
            String returnPage = args.length > 3 ? args[3] : null;
            String reopen;
            if ("page".equalsIgnoreCase(action) || "refresh".equalsIgnoreCase(action)) {
                reopen = arg == null || arg.isBlank() ? "main" : arg;
            } else {
                reopen = returnPage == null || returnPage.isBlank() ? "main" : returnPage;
                String msg = ForgeBridge.sparHandleDo(player, action, arg, reopen);
                sendMultiline(player, msg);
            }
            if ("chat".equals(ForgeBridge.guiBackend())) {
                ForgeBridge.openSparChatMenu(player, reopen);
            } else {
                openSparInventory(player, reopen);
            }
            return true;
        }
        if ("stats".equals(sub) || "help".equals(sub)
                || ("mentor".equals(sub) && args.length == 1)
                || ("top".equals(sub))
                || "pick_apprentice".equals(sub)
                || "pick_mentor".equals(sub)) {
            String page = sub;
            if ("top".equals(sub) && args.length > 1) {
                page = "top_" + args[1].toLowerCase();
            }
            openSparRespectingConfig(player, page);
            return true;
        }
        if ("end".equals(sub)) {
            String msg = ForgeBridge.sparHandleDo(player, "end", "0", "main");
            if (msg != null && !msg.isBlank()) {
                if (!msg.startsWith("§")) {
                    msg = "§a" + msg;
                }
                player.sendMessage(msg);
            }
            openSparRespectingConfig(player, "main");
            return true;
        }
        // mentor <name> / apprentice <name> / admin … → Forge
        if (isSparForgeAction(sub)) {
            forwardToForge(player, "spar", args);
            return true;
        }
        openSparRespectingConfig(player, "main");
        return true;
    }

    private static boolean isSparForgeAction(String sub) {
        return switch (sub.toLowerCase()) {
            case "mentor", "apprentice", "save" -> true;
            default -> false;
        };
    }

    /** Forward unknown subcommands to Forge brigadier via reflection (avoids Bukkit recursion). */
    private static void forwardToForge(Player player, String root, String[] args) {
        StringBuilder sb = new StringBuilder(root);
        for (String a : args) {
            sb.append(' ').append(a);
        }
        if (!ForgeBridge.forwardCommand(player, sb.toString())) {
            player.sendMessage("§cCould not run §f/" + sb + "§c (Forge command bridge failed).");
        }
    }

    private static void sendMultiline(Player player, String msg) {
        if (player == null || msg == null || msg.isBlank()) {
            return;
        }
        for (String line : msg.split("\n")) {
            if (line == null || line.isBlank()) {
                continue;
            }
            player.sendMessage(line.startsWith("§") ? line : "§a" + line);
        }
    }

    private boolean handleDifficulty(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage("Players only. Use /difficulty admin … from console.");
                return true;
            }
            if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                player.sendMessage("§cNo permission: dmzdiff.gui");
                return true;
            }
            openMenuRespectingConfig(player, "main");
            return true;
        }

        String sub = args[0].toLowerCase();
        switch (sub) {
            case "do" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                    player.sendMessage("§cNo permission: dmzdiff.gui");
                    return true;
                }
                String action = args.length > 1 ? args[1] : "";
                String arg = args.length > 2 ? args[2] : null;
                String returnPage = args.length > 3 ? args[3] : null;
                String reopen = ForgeBridge.resolveReturnPage(action, arg, returnPage);
                ForgeBridge.ActionResult result = ForgeBridge.handleActionResult(player, action, arg, reopen);
                if (result.message() != null && !result.message().isBlank()) {
                    String msg = result.message();
                    if (!msg.startsWith("§")) {
                        msg = (result.ok() ? "§a" : "§c") + msg;
                    }
                    player.sendMessage(msg);
                }
                // Bukkit owns reopen: inventory for cmi/chest/auto, chat menu for chat backend.
                if (reopen != null && !reopen.isBlank()) {
                    if ("chat".equals(ForgeBridge.guiBackend())) {
                        ForgeBridge.openChatMenu(player, reopen);
                    } else {
                        openInventory(player, reopen);
                    }
                }
                return true;
            }
            case "admin" -> {
                return handleAdmin(sender, args);
            }
            case "reset", "zero", "clear" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                    player.sendMessage("§cNo permission: dmzdiff.gui");
                    return true;
                }
                ForgeBridge.ActionResult result = ForgeBridge.handleActionResult(player, "reset", "0", "main");
                if (result.message() != null && !result.message().isBlank()) {
                    String msg = result.message();
                    if (!msg.startsWith("§")) {
                        msg = (result.ok() ? "§a" : "§c") + msg;
                    }
                    player.sendMessage(msg);
                }
                if ("chat".equals(ForgeBridge.guiBackend())) {
                    ForgeBridge.openChatMenu(player, "main");
                } else {
                    openInventory(player, "main");
                }
                return true;
            }
            case "buy", "purchase", "unlock", "lower", "adjust", "titles", "title",
                 "team", "teams", "stats", "details" -> {
                if (!(sender instanceof Player player)) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                if (!player.hasPermission("dmzdiff.gui") && !player.isOp()) {
                    player.sendMessage("§cNo permission: dmzdiff.gui");
                    return true;
                }
                String page = switch (sub) {
                    case "purchase", "unlock" -> "buy";
                    case "adjust" -> "lower";
                    case "title" -> "titles";
                    case "teams" -> "team";
                    case "details" -> "stats";
                    default -> sub;
                };
                if (("stats".equals(page) || "details".equals(sub)) && !ForgeBridge.isStaff(player)) {
                    openMenuRespectingConfig(player, "main");
                    return true;
                }
                openMenuRespectingConfig(player, page);
                return true;
            }
            case "hard", "normal", "easy", "peaceful" -> {
                boolean staff = sender instanceof Player p
                        ? ForgeBridge.isStaff(p)
                        : sender.isOp() || sender.hasPermission(ForgeBridge.adminPermission());
                if (!staff) {
                    sender.sendMessage("§cOps only.");
                    return true;
                }
                boolean ok = ForgeBridge.setVanillaDifficulty(sub);
                sender.sendMessage(ok
                        ? "§aVanilla difficulty set to §f" + sub
                        : "§cFailed to set vanilla difficulty.");
                if (ok && "peaceful".equals(sub)) {
                    sender.sendMessage("§cHostile mobs will not spawn — adaptive scaling will not run.");
                }
                return true;
            }
            case "help", "?" -> {
                if (sender instanceof Player player && !ForgeBridge.isStaff(player)) {
                    // Normal players: open the clean GUI — no command dump.
                    openMenuRespectingConfig(player, "main");
                    return true;
                }
                sendStaffHelp(sender);
                return true;
            }
            default -> {
                // Unknown args: players just get the menu; staff get command help.
                if (sender instanceof Player player) {
                    if (ForgeBridge.isStaff(player)) {
                        sendStaffHelp(sender);
                    } else {
                        openMenuRespectingConfig(player, "main");
                    }
                    return true;
                }
                sendStaffHelp(sender);
                return true;
            }
        }
    }

    private boolean handleAdmin(CommandSender sender, String[] args) {
        boolean console = !(sender instanceof Player);
        Player player = console ? null : (Player) sender;

        if (!console && !ForgeBridge.isStaff(player)) {
            // No command-syntax dump for normal players.
            sender.sendMessage("§cStaff only.");
            return true;
        }

        // Bare /difficulty admin shows staff help.
        if (args.length == 1) {
            sendAdminHelp(sender);
            return true;
        }

        String sub = args[1].toLowerCase();

        // Master switch + whitelist.
        if (isDirectStaffSubcommand(sub)) {
            return handleDirectStaff(sender, args, sub);
        }

        switch (sub) {
            case "help" -> {
                sendAdminHelp(sender);
                return true;
            }
            case "resetpurchased" -> {
                if (player == null) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                sender.sendMessage(ForgeBridge.resetPurchased(player));
                return true;
            }
            case "characterreset" -> {
                if (player == null) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                ForgeBridge.ActionResult result =
                        ForgeBridge.handleActionResult(player, "character_reset", "0", "");
                String msg = result.message();
                if (msg == null || msg.isBlank()) {
                    msg = result.ok() ? "Character difficulty reset applied." : "Character reset failed.";
                }
                if (!msg.startsWith("§")) {
                    msg = (result.ok() ? "§a" : "§c") + msg;
                }
                sender.sendMessage(msg);
                return true;
            }
            case "reload" -> {
                if (ForgeBridge.reloadConfig()) {
                    sender.sendMessage("§aAdaptive difficulty config reloaded.");
                } else {
                    String err = ForgeBridge.lastError();
                    sender.sendMessage("§cConfig reload failed"
                            + (err == null || err.isBlank() ? "." : ": " + err));
                }
                return true;
            }
            case "settings" -> {
                if (player == null) {
                    sender.sendMessage("Players only for settings page.");
                    return true;
                }
                if (!ForgeBridge.openChatMenu(player, "settings")) {
                    openMenuRespectingConfig(player, "main");
                }
                sender.sendMessage("§7Use §f/difficulty admin set <key> <value>");
                return true;
            }
            case "area" -> {
                if (player == null) {
                    sender.sendMessage("Players only.");
                    return true;
                }
                sender.sendMessage(ForgeBridge.areaDifficultyText(player));
                return true;
            }
            case "gamedifficulty" -> {
                if (args.length < 3) {
                    sender.sendMessage("§cUsage: /difficulty admin gamedifficulty <peaceful|easy|normal|hard>");
                    return true;
                }
                boolean ok = ForgeBridge.setVanillaDifficulty(args[2]);
                sender.sendMessage(ok
                        ? "§aVanilla difficulty set to §f" + args[2]
                        : "§cFailed to set vanilla difficulty.");
                return true;
            }
            case "gui", "inspect", "view", "playergui" -> {
                return handleAdminGui(sender, args);
            }
            case "set" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /difficulty admin set <key> <value>");
                    return true;
                }
                String key = args[2];
                StringBuilder sb = new StringBuilder(args[3]);
                for (int i = 4; i < args.length; i++) {
                    sb.append(' ').append(args[i]);
                }
                sender.sendMessage(ForgeBridge.adminSet(key, sb.toString()));
                return true;
            }
            default -> {
                sender.sendMessage("§cUnknown admin subcommand. Try §f/difficulty admin help");
                return true;
            }
        }
    }

    private static boolean isDirectStaffSubcommand(String sub) {
        return switch (sub) {
            case "off", "disable", "on", "enable", "toggle", "status",
                 "whitelist", "wl",
                 "telemetry", "tel", "balancelog", "combatlog" -> true;
            default -> false;
        };
    }

    private boolean handleDirectStaff(CommandSender sender, String[] args, String sub) {
        switch (sub) {
            case "off", "disable" -> {
                sender.sendMessage(ForgeBridge.setSystemEnabled(false));
                return true;
            }
            case "on", "enable" -> {
                sender.sendMessage(ForgeBridge.setSystemEnabled(true));
                return true;
            }
            case "toggle" -> {
                sender.sendMessage(ForgeBridge.setSystemEnabled(!ForgeBridge.systemEnabled()));
                return true;
            }
            case "status" -> {
                sender.sendMessage(ForgeBridge.systemStatusText());
                return true;
            }
            case "whitelist", "wl" -> {
                return handleWhitelist(sender, args);
            }
            case "telemetry", "tel", "balancelog", "combatlog" -> {
                return handleTelemetry(sender, args);
            }
            default -> {
                return false;
            }
        }
    }

    private boolean handleWhitelist(CommandSender sender, String[] args) {
        // /difficulty admin whitelist
        if (args.length == 2) {
            sender.sendMessage(ForgeBridge.whitelistStatusText());
            return true;
        }
        String op = args[2].toLowerCase();
        switch (op) {
            case "on", "enable" -> {
                sender.sendMessage(ForgeBridge.setWhitelistEnabled(true));
                return true;
            }
            case "off", "disable" -> {
                sender.sendMessage(ForgeBridge.setWhitelistEnabled(false));
                return true;
            }
            case "toggle" -> {
                sender.sendMessage(ForgeBridge.setWhitelistEnabled(!ForgeBridge.whitelistEnabled()));
                return true;
            }
            case "status" -> {
                sender.sendMessage(ForgeBridge.whitelistStatusText());
                return true;
            }
            case "list" -> {
                sender.sendMessage(ForgeBridge.whitelistListText());
                return true;
            }
            case "add" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /difficulty admin whitelist add <player>");
                    return true;
                }
                sender.sendMessage(ForgeBridge.whitelistAdd(args[3]));
                return true;
            }
            case "remove", "rm", "del" -> {
                if (args.length < 4) {
                    sender.sendMessage("§cUsage: /difficulty admin whitelist remove <player|uuid>");
                    return true;
                }
                StringBuilder name = new StringBuilder(args[3]);
                for (int i = 4; i < args.length; i++) {
                    name.append(' ').append(args[i]);
                }
                sender.sendMessage(ForgeBridge.whitelistRemove(name.toString()));
                return true;
            }
            case "clear" -> {
                sender.sendMessage(ForgeBridge.whitelistClear());
                return true;
            }
            default -> {
                sender.sendMessage("§cUsage: /difficulty admin whitelist on|off|add|remove|list|clear");
                return true;
            }
        }
    }

    private boolean handleTelemetry(CommandSender sender, String[] args) {
        // /difficulty admin telemetry
        if (args.length == 2) {
            sender.sendMessage(ForgeBridge.telemetryStatusText());
            return true;
        }
        String op = args[2].toLowerCase();
        switch (op) {
            case "on", "enable" -> {
                // Auto-list the running staffer so the first fight actually logs.
                if (sender instanceof Player p && !ForgeBridge.isPlayerWhitelisted(p.getName())) {
                    ForgeBridge.whitelistAdd(p.getName());
                }
                sender.sendMessage(ForgeBridge.setTelemetryEnabled(true));
                return true;
            }
            case "off", "disable" -> {
                sender.sendMessage(ForgeBridge.setTelemetryEnabled(false));
                return true;
            }
            case "toggle" -> {
                sender.sendMessage(ForgeBridge.setTelemetryEnabled(!ForgeBridge.telemetryEnabled()));
                return true;
            }
            case "status" -> {
                sender.sendMessage(ForgeBridge.telemetryStatusText());
                return true;
            }
            case "flush" -> {
                sender.sendMessage(ForgeBridge.telemetryFlush());
                return true;
            }
            case "test", "probe" -> {
                String name = sender instanceof Player p ? p.getName() : "console";
                sender.sendMessage(ForgeBridge.telemetryTest(name));
                return true;
            }
            default -> {
                sender.sendMessage("§cUsage: /difficulty admin telemetry on|off|status|flush|test");
                return true;
            }
        }
    }


    private boolean handleAdminGui(CommandSender sender, String[] args) {
        if (!(sender instanceof Player admin)) {
            sender.sendMessage("Players only (open inspect from in-game).");
            return true;
        }
        if (args.length < 3
                || "clear".equalsIgnoreCase(args[2])
                || "self".equalsIgnoreCase(args[2])
                || "me".equalsIgnoreCase(args[2])) {
            AdminInspectSessions.clear(admin.getUniqueId());
            openMenuRespectingConfig(admin, "main");
            admin.sendMessage("§7Inspect closed — showing your own GUI.");
            return true;
        }
        String page = "main";
        String name = args[2];
        if (args.length >= 4
                && ("main".equalsIgnoreCase(args[3])
                || "buy".equalsIgnoreCase(args[3])
                || "lower".equalsIgnoreCase(args[3])
                || "titles".equalsIgnoreCase(args[3])
                || "stats".equalsIgnoreCase(args[3])
                || "details".equalsIgnoreCase(args[3]))) {
            page = "details".equalsIgnoreCase(args[3]) ? "stats" : args[3].toLowerCase();
        } else if (args.length >= 4) {
            StringBuilder sb = new StringBuilder(args[2]);
            int last = args.length - 1;
            String maybePage = args[last].toLowerCase();
            boolean hasPage = switch (maybePage) {
                case "main", "buy", "lower", "titles", "stats", "details" -> true;
                default -> false;
            };
            int end = hasPage ? last : args.length;
            for (int i = 3; i < end; i++) {
                sb.append(' ').append(args[i]);
            }
            name = sb.toString();
            if (hasPage) {
                page = "details".equals(maybePage) ? "stats" : maybePage;
            }
        }
        Player subject = Bukkit.getPlayerExact(name);
        if (subject == null) {
            subject = Bukkit.getPlayer(name);
        }
        if (subject == null || !subject.isOnline()) {
            admin.sendMessage("§cPlayer not online: §f" + name);
            return true;
        }
        openInspect(admin, subject, page);
        return true;
    }

    private static void sendAdminHelp(CommandSender sender) {
        sendStaffHelp(sender);
    }

    /** Full command reference — staff / console only. */
    private static void sendStaffHelp(CommandSender sender) {
        sender.sendMessage("§6Adaptive Difficulty — staff");
        sender.sendMessage("§e/difficulty §7— open GUI");
        sender.sendMessage("§e/difficulty buy|lower|titles|details §7— open those pages");
        sender.sendMessage("§e/difficulty reset §7— clear active tier");
        sender.sendMessage("§e/difficulty admin off|on|toggle|status §7— master system switch");
        sender.sendMessage("§e/difficulty admin whitelist on|off|add|remove|list|clear §7— testing whitelist");
        sender.sendMessage("§e/difficulty admin telemetry on|off|status|flush|test §7— balance hit logs");
        sender.sendMessage("§e/difficulty admin gui|inspect <player> [page] §7— open their GUI (edit/see their state)");
        sender.sendMessage("§e/difficulty admin gui clear §7— stop inspecting");
        sender.sendMessage("§e/difficulty admin reload|settings|area|set §7— config tools");
        sender.sendMessage("§e/difficulty hard|normal|easy|peaceful §7— vanilla difficulty");
        sender.sendMessage("§8Master keys: enabled · whitelistEnabled · balanceTelemetryEnabled");
    }
}
