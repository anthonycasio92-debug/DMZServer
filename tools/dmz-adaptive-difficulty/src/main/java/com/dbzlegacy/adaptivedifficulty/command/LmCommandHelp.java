package com.dbzlegacy.adaptivedifficulty.command;

/** Central help text for all Legacy Mechanics command trees (same layout as {@code /lm admin help}). */
public final class LmCommandHelp {
    private LmCommandHelp() {}

    public static String lmPlayer() {
        return LmStaffHelp.build(
                "Legacy Mechanics",
                "/lm",
                "§7Your hub for CNPC menus—no chest GUIs. Type §f/lm help §7anytime.",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/lm", "open the main hub"),
                        LmStaffHelp.cmd("/lm open difficulty", "adaptive difficulty"),
                        LmStaffHelp.cmd("/lm open rival", "rival system"),
                        LmStaffHelp.cmd("/lm open spar", "sparring and dojo"),
                        LmStaffHelp.cmd("/lm open prestige", "prestige shop"),
                        LmStaffHelp.cmd("/lm open character", "race, class, and reskin services"),
                        LmStaffHelp.cmd("/lm open skillcheck", "donator skill check (if permitted)")),
                new LmStaffHelp.Section("Staff",
                        LmStaffHelp.cmd("/lm admin help", "staff tools and player resets"),
                        LmStaffHelp.note("Shortcut: none — use /lm")));
    }

    public static String lmAdmin() {
        return LmStaffHelp.build(
                "Legacy Mechanics staff",
                "/lm admin",
                "§7Staff tools and player fixes. Type §f/lm admin help §7anytime—menus stay CNPC-only.",
                new LmStaffHelp.Section("Config and data",
                        LmStaffHelp.cmd("/lm admin reload", "reload Legacy Mechanics config"),
                        LmStaffHelp.cmd("/lm admin migrate-cnpc", "import CNPC rival and spar data"),
                        LmStaffHelp.cmd("/lm admin migrate-cnpc force", "wipe LM stores, then re-import"),
                        LmStaffHelp.note("Backup: config/legacymechanics/cnpc-import-backup/")),
                new LmStaffHelp.Section("Player resets",
                        LmStaffHelp.cmd("/lm admin clear <player>", "clear all LM data for a player"),
                        LmStaffHelp.cmd("/lm admin clear <player> rival", "clear rival data only"),
                        LmStaffHelp.cmd("/lm admin clear <player> spar", "clear spar data only"),
                        LmStaffHelp.cmd("/lm admin clear <player> difficulty", "clear difficulty data only"),
                        LmStaffHelp.cmd("/lm admin clear <player> progression", "clear progression data only"),
                        LmStaffHelp.cmd("/lm admin character cooldown clear <player>", "clear all character service cooldowns"),
                        LmStaffHelp.cmd("/lm admin character cooldown clear <player> race", "clear race change cooldown"),
                        LmStaffHelp.cmd("/lm admin character cooldown clear <player> class", "clear class change cooldown"),
                        LmStaffHelp.cmd("/lm admin character cooldown clear <player> reskin", "clear reskin cooldown")),
                new LmStaffHelp.Section("Staff tools",
                        LmStaffHelp.cmd("/lm admin syslog status", "event log on or off"),
                        LmStaffHelp.cmd("/lm admin syslog on", "turn unified event log on"),
                        LmStaffHelp.cmd("/lm admin syslog off", "turn unified event log off"),
                        LmStaffHelp.cmd("/lm admin syslog flush", "flush logs to disk"),
                        LmStaffHelp.cmd("/lm admin open <system>", "open a CNPC menu as yourself"),
                        LmStaffHelp.cmd("/lm admin inspect <player>", "view menus as that player"),
                        LmStaffHelp.cmd("/lm admin inspect clear", "stop inspect mode"),
                        LmStaffHelp.cmd("/lm admin testgui", "staff CNPC test hub")),
                new LmStaffHelp.Section("Related staff trees",
                        LmStaffHelp.cmd("/difficulty admin help", "adaptive difficulty staff commands"),
                        LmStaffHelp.cmd("/padmin help", "prestige shop staff commands"),
                        LmStaffHelp.note("Difficulty shortcut: /diff")));
    }

    public static String difficultyPlayer() {
        return LmStaffHelp.build(
                "Adaptive difficulty",
                "/difficulty",
                "§7Open your difficulty menu here or from §f/lm§7. Staff: §f/difficulty admin help§7 · shortcut §f/diff",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/difficulty", "open your difficulty menu (CNPC)"),
                        LmStaffHelp.cmd("/difficulty reset", "clear active tier (free)")),
                new LmStaffHelp.Section("World difficulty (ops)",
                        LmStaffHelp.cmd("/difficulty peaceful", "set vanilla world difficulty"),
                        LmStaffHelp.cmd("/difficulty easy", "set vanilla world difficulty"),
                        LmStaffHelp.cmd("/difficulty normal", "set vanilla world difficulty"),
                        LmStaffHelp.cmd("/difficulty hard", "set vanilla world difficulty")));
    }

    public static String difficultyAdmin() {
        return LmStaffHelp.build(
                "Adaptive difficulty staff",
                "/difficulty admin",
                "§7Player hub: §f/lm §7· Shortcut: §f/diff",
                new LmStaffHelp.Section("Players",
                        LmStaffHelp.cmd("/difficulty", "open your difficulty menu (CNPC)"),
                        LmStaffHelp.cmd("/difficulty reset", "clear active tier (free)"),
                        LmStaffHelp.cmd("/difficulty peaceful", "set vanilla world difficulty (ops)"),
                        LmStaffHelp.cmd("/difficulty easy", "set vanilla world difficulty (ops)"),
                        LmStaffHelp.cmd("/difficulty normal", "set vanilla world difficulty (ops)"),
                        LmStaffHelp.cmd("/difficulty hard", "set vanilla world difficulty (ops)")),
                new LmStaffHelp.Section("System switch",
                        LmStaffHelp.cmd("/difficulty admin status", "show master switch state"),
                        LmStaffHelp.cmd("/difficulty admin on", "enable adaptive difficulty"),
                        LmStaffHelp.cmd("/difficulty admin off", "disable adaptive difficulty"),
                        LmStaffHelp.cmd("/difficulty admin toggle", "flip master switch")),
                new LmStaffHelp.Section("Testing whitelist",
                        LmStaffHelp.cmd("/difficulty admin whitelist list", "who can see scaled mobs while testing"),
                        LmStaffHelp.cmd("/difficulty admin whitelist add <player>", "add player to whitelist"),
                        LmStaffHelp.cmd("/difficulty admin whitelist remove <player>", "remove from whitelist"),
                        LmStaffHelp.cmd("/difficulty admin whitelist on", "restrict scaling to whitelist"),
                        LmStaffHelp.cmd("/difficulty admin whitelist off", "scaling for everyone again"),
                        LmStaffHelp.cmd("/difficulty admin whitelist clear", "empty whitelist")),
                new LmStaffHelp.Section("Staff perks and logs",
                        LmStaffHelp.cmd("/difficulty admin stafffree status", "staff skip Ancient Coin costs"),
                        LmStaffHelp.cmd("/difficulty admin stafffree on", "enable staff free coins"),
                        LmStaffHelp.cmd("/difficulty admin stafffree off", "disable staff free coins"),
                        LmStaffHelp.cmd("/difficulty admin telemetry status", "combat balance telemetry"),
                        LmStaffHelp.cmd("/difficulty admin telemetry on", "enable combat telemetry"),
                        LmStaffHelp.cmd("/difficulty admin telemetry off", "disable combat telemetry"),
                        LmStaffHelp.cmd("/difficulty admin syslog status", "unified system event log"),
                        LmStaffHelp.cmd("/difficulty admin syslog on", "enable system event log"),
                        LmStaffHelp.cmd("/difficulty admin syslog off", "disable system event log")),
                new LmStaffHelp.Section("Inspect and fix players",
                        LmStaffHelp.cmd("/difficulty admin inspect <player>", "open their difficulty menu"),
                        LmStaffHelp.cmd("/difficulty admin inspect clear", "stop inspect mode"),
                        LmStaffHelp.cmd("/difficulty admin resynclevel <player>", "fix stuck level sample on GUI"),
                        LmStaffHelp.cmd("/difficulty admin resetpurchased", "reset purchased tiers (staff)"),
                        LmStaffHelp.cmd("/difficulty admin characterreset", "character wipe hook")),
                new LmStaffHelp.Section("Config",
                        LmStaffHelp.cmd("/difficulty admin reload", "reload adaptivedifficulty.json"),
                        LmStaffHelp.cmd("/difficulty admin settings", "open admin settings menu"),
                        LmStaffHelp.cmd("/difficulty admin area", "show area difficulty at you"),
                        LmStaffHelp.cmd("/difficulty admin gamedifficulty <level>", "set vanilla difficulty"),
                        LmStaffHelp.cmd("/difficulty admin set <key> <value>", "change a live config key"),
                        LmStaffHelp.note("Tier keys live in config/adaptivedifficulty.json")));
    }

    public static String rivalPlayer() {
        return LmStaffHelp.build(
                "Rival system",
                "/rival",
                "§7Most rival actions live in the CNPC menu. Type §f/rival help §7when you need the full list.",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/rival", "open rival menu"),
                        LmStaffHelp.cmd("/rival season", "season progress menu"),
                        LmStaffHelp.cmd("/rival quests", "quests menu"),
                        LmStaffHelp.cmd("/rival achievements", "achievements menu"),
                        LmStaffHelp.cmd("/rival hof", "hall of fame"),
                        LmStaffHelp.cmd("/rival journal", "rival journal"),
                        LmStaffHelp.cmd("/rival title", "title picker")),
                new LmStaffHelp.Section("Info",
                        LmStaffHelp.cmd("/rival list", "list your rivals"),
                        LmStaffHelp.cmd("/rival stats", "your rival stats"),
                        LmStaffHelp.cmd("/rival stats <player>", "another player's stats"),
                        LmStaffHelp.cmd("/rival top", "leaderboard (RP)"),
                        LmStaffHelp.cmd("/rival top <category>", "leaderboard by category")),
                new LmStaffHelp.Section("Rivalry actions",
                        LmStaffHelp.cmd("/rival declare <player>", "declare a rival"),
                        LmStaffHelp.cmd("/rival accept <player>", "accept a rival request"),
                        LmStaffHelp.cmd("/rival decline <player>", "decline a rival request"),
                        LmStaffHelp.cmd("/rival remove <player>", "remove a rival")),
                new LmStaffHelp.Section("Duel challenge",
                        LmStaffHelp.cmd("/rival challenge send <player>", "send a duel request"),
                        LmStaffHelp.cmd("/rival challenge send <player> <minutes>", "send with custom window"),
                        LmStaffHelp.cmd("/rival challenge accept", "accept pending duel"),
                        LmStaffHelp.cmd("/rival challenge decline", "decline pending duel"),
                        LmStaffHelp.cmd("/rival challenge cancel", "cancel your outgoing duel")),
                new LmStaffHelp.Section("Spectate",
                        LmStaffHelp.cmd("/rival spectate", "spectate an active duel"),
                        LmStaffHelp.cmd("/rival spectate <player>", "spectate a player's duel"),
                        LmStaffHelp.cmd("/rival spectate stop", "stop spectating")),
                new LmStaffHelp.Section("Teleport messages",
                        LmStaffHelp.cmd("/rival tpmsg", "show TP message toggle state"),
                        LmStaffHelp.cmd("/rival tpmsg on", "enable rival TP messages"),
                        LmStaffHelp.cmd("/rival tpmsg off", "disable rival TP messages")),
                new LmStaffHelp.Section("Staff",
                        LmStaffHelp.cmd("/rival admin help", "save, refresh, status")));
    }

    public static String rivalAdmin() {
        return LmStaffHelp.build(
                "Rival staff",
                "/rival admin",
                "§7Use §f/rival admin help §7anytime.",
                new LmStaffHelp.Section("Data",
                        LmStaffHelp.cmd("/rival admin save", "write rivalry data to disk"),
                        LmStaffHelp.cmd("/rival admin refresh", "reload stores from disk"),
                        LmStaffHelp.cmd("/rival admin status", "enabled flag and file paths")),
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/rival admin open", "open rival CNPC menu"),
                        LmStaffHelp.cmd("/rival admin open <page>", "open a specific rival page")));
    }

    public static String sparPlayer() {
        return LmStaffHelp.build(
                "Sparring",
                "/spar",
                "§7Training, mentors, and dojo wars—mostly in the CNPC menu. Type §f/spar help §7for commands.",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/spar", "open sparring menu")),
                new LmStaffHelp.Section("Session",
                        LmStaffHelp.cmd("/spar stats", "your spar stats"),
                        LmStaffHelp.cmd("/spar stats <player>", "another player's stats"),
                        LmStaffHelp.cmd("/spar end", "end your active spar session"),
                        LmStaffHelp.cmd("/spar top", "leaderboard (training points)"),
                        LmStaffHelp.cmd("/spar top <category>", "leaderboard by category")),
                new LmStaffHelp.Section("Mentor",
                        LmStaffHelp.cmd("/spar mentor", "mentor bond status"),
                        LmStaffHelp.cmd("/spar mentor <player>", "invite a mentor"),
                        LmStaffHelp.cmd("/spar mentor accept", "accept mentor invite"),
                        LmStaffHelp.cmd("/spar mentor decline", "decline mentor invite"),
                        LmStaffHelp.cmd("/spar mentor leave", "leave mentor bond")),
                new LmStaffHelp.Section("Apprentice",
                        LmStaffHelp.cmd("/spar apprentice", "apprentice status"),
                        LmStaffHelp.cmd("/spar apprentice <player>", "invite an apprentice"),
                        LmStaffHelp.cmd("/spar apprentice remove", "release your apprentice"),
                        LmStaffHelp.cmd("/spar apprentice remove <player>", "release one apprentice")),
                new LmStaffHelp.Section("Dojo",
                        LmStaffHelp.cmd("/spar dojo", "your dojo summary"),
                        LmStaffHelp.cmd("/spar dojo accept", "accept dojo war invite"),
                        LmStaffHelp.cmd("/spar dojo decline", "decline dojo war invite"),
                        LmStaffHelp.cmd("/spar dojo challenge <player>", "challenge another dojo"),
                        LmStaffHelp.cmd("/spar dojo members", "list dojo members"),
                        LmStaffHelp.cmd("/spar dojo hof", "dojo hall of fame"),
                        LmStaffHelp.cmd("/spar dojo top", "dojo leaderboard")),
                new LmStaffHelp.Section("Staff",
                        LmStaffHelp.cmd("/spar admin help", "save, status, mentor cooldown reset")));
    }

    public static String sparAdmin() {
        return LmStaffHelp.build(
                "Sparring staff",
                "/spar admin",
                "§7Use §f/spar admin help §7anytime.",
                new LmStaffHelp.Section("Data",
                        LmStaffHelp.cmd("/spar admin save", "write spar data to disk"),
                        LmStaffHelp.cmd("/spar admin status", "enabled flag and file path")),
                new LmStaffHelp.Section("Mentor cooldown",
                        LmStaffHelp.cmd("/spar admin mentor resetcd", "clear your mentor invite cooldown"),
                        LmStaffHelp.cmd("/spar admin mentor resetcd <player>", "clear cooldown for a player")));
    }

    public static String progressionPlayer() {
        return LmStaffHelp.build(
                "Progression",
                "/progression",
                "§7Check your meditation trial here. Prestige and shop live under §f/lm§7.",
                new LmStaffHelp.Section("Meditation",
                        LmStaffHelp.cmd("/progression meditation", "current meditation trial status"),
                        LmStaffHelp.cmd("/progression meditation status", "same as meditation")),
                new LmStaffHelp.Section("Android",
                        LmStaffHelp.cmd("/progression android remove", "remove android (opens hub flow)"),
                        LmStaffHelp.note("Prestige and shop: use /lm open prestige")));
    }

    public static String progressionStaff() {
        return LmStaffHelp.build(
                "Progression staff",
                "/progression",
                "§7Staff progression tools—boosts, flags, and android conversion.",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/progression", "open progression staff menu (CNPC)")),
                new LmStaffHelp.Section("Meditation",
                        LmStaffHelp.cmd("/progression meditation next", "rotate meditation trial (staff)")),
                new LmStaffHelp.Section("Natural boost",
                        LmStaffHelp.cmd("/progression boost status", "show active boost"),
                        LmStaffHelp.cmd("/progression boost end", "end active boost"),
                        LmStaffHelp.cmd("/progression boost start <encoded>", "start boost from encoded id"),
                        LmStaffHelp.cmd("/progression boost start <multiplier> <minutes>", "start timed boost")),
                new LmStaffHelp.Section("Android (staff)",
                        LmStaffHelp.cmd("/progression android <player>", "convert player to android"),
                        LmStaffHelp.cmd("/progression android remove <player>", "remove android from player")),
                new LmStaffHelp.Section("Flags",
                        LmStaffHelp.cmd("/progression admin <flag> <on>", "toggle progression flag"),
                        LmStaffHelp.cmd("/progression admin <flag> <off>", "toggle progression flag off")));
    }

    public static String endDragonStaff() {
        return LmStaffHelp.build(
                "End dragon staff",
                "/enddragon",
                "§7Players summon dragons from the Difficulty menu only.",
                new LmStaffHelp.Section("Maintenance",
                        LmStaffHelp.cmd("/enddragon clear", "remove stray end dragons"),
                        LmStaffHelp.cmd("/enddragon repair", "repair End podium"),
                        LmStaffHelp.cmd("/enddragon spawn", "disabled — use Difficulty menu")));
    }

    public static String skillCheck() {
        return LmStaffHelp.build(
                "Skill check",
                "/skillcheck",
                "§7Donator or staff permission required.",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/skillcheck", "open skill check menu"),
                        LmStaffHelp.cmd("/skillcheck <page>", "open a specific page")),
                new LmStaffHelp.Section("Hub",
                        LmStaffHelp.cmd("/lm open skillcheck", "open from Legacy Mechanics hub")));
    }

    public static String characterStaff() {
        return LmStaffHelp.build(
                "Character services",
                "/character",
                "§7Staff opens CNPC character services. Players normally use §f/lm open character§7.",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/character", "open character services menu"),
                        LmStaffHelp.cmd("/lm open character", "same menu from the hub")));
    }

    public static String androidifyStaff() {
        return LmStaffHelp.build(
                "Androidify",
                "/androidify",
                "§7Staff-only race conversion tool.",
                new LmStaffHelp.Section("Usage",
                        LmStaffHelp.cmd("/androidify", "convert yourself (in-game)"),
                        LmStaffHelp.cmd("/androidify <player>", "convert another online player")));
    }

    public static String skillsStaff() {
        return LmStaffHelp.build(
                "Skills admin",
                "/skills",
                "§7Staff skill-unlock browser (not the donator skill check).",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/skills", "open skills admin menu"),
                        LmStaffHelp.cmd("/skills do page <page>", "scripted page open")));
    }

    public static String prestigeMenuStaff() {
        return LmStaffHelp.build(
                "Prestige menu",
                "/prestige",
                "§7Staff GUI backend for prestige shop scripting. Use §f/prestige help §7anytime.",
                new LmStaffHelp.Section("Menus",
                        LmStaffHelp.cmd("/prestige", "open prestige menu"),
                        LmStaffHelp.cmd("/prestige do confirm", "confirm prestige action"),
                        LmStaffHelp.cmd("/prestige do page <page>", "open menu page")),
                new LmStaffHelp.Section("Prestige adjustments",
                        LmStaffHelp.cmd("/padmin help", "staff prestige wallet and shop edits")));
    }

    public static String prestigeAdmin() {
        return LmStaffHelp.build(
                "Prestige admin",
                "/padmin",
                "§7Staff-only prestige shop adjustments. Use §f/padmin help §7anytime.",
                new LmStaffHelp.Section("Look up",
                        LmStaffHelp.cmd("/padmin info", "your prestige summary"),
                        LmStaffHelp.cmd("/padmin info <player>", "another player's summary"),
                        LmStaffHelp.cmd("/padmin sync <player>", "refresh prestige from disk and mods")),
                new LmStaffHelp.Section("Wallet counts",
                        LmStaffHelp.cmd("/padmin points <player> set <amount>", "set wallet points"),
                        LmStaffHelp.cmd("/padmin points <player> add <amount>", "add wallet points"),
                        LmStaffHelp.cmd("/padmin points <player> remove <amount>", "remove wallet points"),
                        LmStaffHelp.cmd("/padmin held <player> set <amount>", "set held prestige count"),
                        LmStaffHelp.cmd("/padmin held <player> add <amount>", "add held prestige count"),
                        LmStaffHelp.cmd("/padmin held <player> remove <amount>", "remove held prestige count"),
                        LmStaffHelp.cmd("/padmin completed <player> set <amount>", "set completed count"),
                        LmStaffHelp.cmd("/padmin completed <player> add <amount>", "add completed count"),
                        LmStaffHelp.cmd("/padmin completed <player> remove <amount>", "remove completed count"),
                        LmStaffHelp.cmd("/padmin breakthroughs <player> set <0-5>", "set breakthroughs"),
                        LmStaffHelp.cmd("/padmin breakthroughs <player> add <n>", "add breakthroughs"),
                        LmStaffHelp.cmd("/padmin breakthroughs <player> remove <n>", "remove breakthroughs")),
                new LmStaffHelp.Section("Shop unlocks",
                        LmStaffHelp.cmd("/padmin tier <player> set <0-7>", "set highest purchased tier"),
                        LmStaffHelp.cmd("/padmin tier <player> give <1-7>", "grant a tier unlock"),
                        LmStaffHelp.cmd("/padmin tier <player> clear <1-7>", "clear one tier unlock"),
                        LmStaffHelp.cmd("/padmin tier <player> clear all", "clear all tier unlocks"),
                        LmStaffHelp.cmd("/padmin skills <player>", "list invested skill floors"),
                        LmStaffHelp.cmd("/padmin skill <player> <skillId> set <levels>", "set skill floor level"),
                        LmStaffHelp.cmd("/padmin skill <player> <skillId> add <levels>", "add skill floor levels"),
                        LmStaffHelp.cmd("/padmin skill <player> <skillId> remove <levels>", "remove skill floor levels"),
                        LmStaffHelp.note("Skill IDs match the prestige shop (e.g. potentialunlock)")));
    }
}
