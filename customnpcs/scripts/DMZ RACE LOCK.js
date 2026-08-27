// ============================================================
// Restricted DMZ Race Unlock System
// CustomNPCs 1.20.1 Global Player Script
//
// Preview vs select:
// - DMZ race carousel already lists every loaded race (preview OK).
// - While browsing (character not created yet), this script shows a
//   one-time tip: locked races need Prestige unlock.
// - After they finish create/select without the unlock skill, they
//   are reset and told to unlock the race via Prestiging.
//
// Unlock check = Fabled skill level >= 1 (Prestige skill tree purchase).
// Prestige N on the GUI is the token path (prestiginge N times), NOT an
// ongoing "must currently be Prestige N" requirement.
// Ancient Saiyan also needs LP fabled.skill.ancient-saiyan to buy
// the skill (unlockrace / Ancient Rights item grants that).
// After the skill is owned, race Select stays unlocked forever.
//
// Reset command: dmzstats reset <player> 0 false
// ============================================================


// ============================================================
// CONFIGURATION
// ============================================================

// Number of player tick events between checks.
//
// This uses the same tick-counter structure as the working
// Fabled attribute script.
var TICK_INTERVAL = 1;


// Number of completed checks to wait before retrying the reset
// command if the player somehow remains in the restricted race.
//
// With the default interval, this provides a reasonable delay
// without permanently stopping the script.
var RESET_RETRY_CHECKS = 5;


// Exact internal DMZ race IDs that require an unlock.
//
// Capitalization is ignored.
//
// Each entry must line up with the corresponding entry in
// REQUIRED_FABLED_SKILLS.
var RESTRICTED_RACE_IDS = [
    "ancient_saiyan",
    "sento_saiyan"
];


// Exact Fabled skill key OR displayed skill name required for
// each restricted race. Must line up with RESTRICTED_RACE_IDS.
//
// Example for later:
//
// var RESTRICTED_RACE_IDS = [
//     "ancient_saiyan",
//     "sento_saiyan",
//     "viltrumite",
//     "yardrat"
// ];
//
// var REQUIRED_FABLED_SKILLS = [
//     "Ancient Saiyan",
//     "Sento Saiyan",
//     "race_unlock_viltrumite",
//     "race_unlock_yardrat"
// ];
var REQUIRED_FABLED_SKILLS = [
    "Ancient Saiyan",
    "Sento Saiyan"
];


// Friendly race names used in player messages.
// Each position must match RESTRICTED_RACE_IDS.
var RESTRICTED_RACE_DISPLAY_NAMES = [
    "Ancient Saiyan",
    "Sento Saiyan"
];


// Optional LuckPerms / Bukkit permission nodes for clearer tips ONLY.
// Empty string = skill-only unlock (Sento Saiyan).
// Unlock gate itself is always Fabled skill level >= 1.
// Ancient Saiyan also needs fabled.skill.ancient-saiyan to buy its skill.
var REQUIRED_PERMISSIONS = [
    "fabled.skill.ancient-saiyan",
    ""
];


// Short how-to lines shown if a locked race somehow gets past the GUI padlock.
// Each position must match RESTRICTED_RACE_IDS.
// Prestige N = token cost path only. Unlock is permanent after the Fabled skill is bought.
var UNLOCK_VIA_PRESTIGE_HINTS = [
    "Ancient Saiyan: buy with Prestige tokens (from prestiginging 10 times). Once unlocked, permanent — no active Prestige 10 needed.",
    "Sento Saiyan: buy with Prestige tokens (from prestiginging 1 time). Once unlocked, permanent."
];


// Player-facing race lock messages stay on.
// Verbose [Race Lock Debug] spam stays off.
var DEBUG = false;

/*
 * ============================================================
 * SAGA DIFFICULTY HELPERS
 * ============================================================
 *
 * DMZ SetStoryDifficultyC2S ignores clicks when
 * PlayerQuestData.difficultyChosen is already true.
 * dmzstats reset does NOT clear that flag.
 *
 * Extra blockers from DMZ itself:
 * - Party non-leaders cannot select difficulty at all
 * - Joining a party forces difficultyChosen = true
 *
 * Unlock runs from Tick (no Bukkit needed) and from chat:
 *   !unlockdifficulty
 */

var SAGA_UNLOCK_RETRY_TICKS = 40;

function syncProgression(mcPlayer) {
    if (mcPlayer == null) {
        return false;
    }

    try {
        var ProgressionSyncS2C = Java.type(
            "com.dragonminez.common.network.S2C.ProgressionSyncS2C"
        );
        var NetworkHandler = Java.type(
            "com.dragonminez.common.network.NetworkHandler"
        );

        NetworkHandler.sendToPlayer(
            new ProgressionSyncS2C(mcPlayer),
            mcPlayer
        );
        return true;
    } catch (err1) {}

    try {
        var StatsSyncS2C = Java.type(
            "com.dragonminez.common.network.S2C.StatsSyncS2C"
        );
        var NetworkHandler2 = Java.type(
            "com.dragonminez.common.network.NetworkHandler"
        );

        NetworkHandler2.sendToTrackingEntityAndSelf(
            new StatsSyncS2C(mcPlayer),
            mcPlayer
        );
        return true;
    } catch (err2) {}

    return false;
}

function getMcPlayer(player) {
    if (player == null) {
        return null;
    }
    try {
        return player.getMCEntity
            ? player.getMCEntity()
            : null;
    } catch (err) {
        return null;
    }
}

function loadDmzData(player) {
    try {
        var StatsProvider = Java.type(
            "com.dragonminez.common.stats.StatsProvider"
        );
        var StatsCapability = Java.type(
            "com.dragonminez.common.stats.StatsCapability"
        );
        var mcPlayer = getMcPlayer(player);
        if (mcPlayer == null) {
            return null;
        }
        var lazy = StatsProvider.get(
            StatsCapability.INSTANCE,
            mcPlayer
        );
        if (lazy == null) {
            return null;
        }
        return lazy.orElse(null);
    } catch (err) {
        return null;
    }
}

function isDifficultyChosen(questData) {
    if (questData == null) {
        return false;
    }
    try {
        return questData.isDifficultyChosen() === true;
    } catch (err) {
        return false;
    }
}

function hasCreatedCharacter(status) {
    if (status == null) {
        return false;
    }
    try {
        return status.isHasCreatedCharacter() === true;
    } catch (err) {
        return false;
    }
}

function isInDmzParty(questData) {
    if (questData == null) {
        return false;
    }
    try {
        return questData.isInParty() === true;
    } catch (err) {
        return false;
    }
}

function isDmzPartyLeader(questData, mcPlayer) {
    if (questData == null || mcPlayer == null) {
        return false;
    }
    try {
        return questData.isPartyLeader(
            mcPlayer.m_20148_()
        ) === true;
    } catch (err1) {
        try {
            return questData.isPartyLeader(
                mcPlayer.getUUID()
            ) === true;
        } catch (err2) {
            return false;
        }
    }
}

function leaveDmzParty(mcPlayer) {
    if (mcPlayer == null) {
        return false;
    }

    /*
     * PartyManager.leaveParty no-ops when PartySavedData has no party,
     * but PlayerQuestData can still hold a ghost activePartyId (V-menu).
     * Always clear quest party state + sync after the leave attempt.
     */
    try {
        var PartyManager = Java.type(
            "com.dragonminez.common.quest.PartyManager"
        );
        PartyManager.leaveParty(mcPlayer);
    } catch (err1) {}

    var cleared = false;
    try {
        var StatsProvider = Java.type(
            "com.dragonminez.common.stats.StatsProvider"
        );
        var StatsCapability = Java.type(
            "com.dragonminez.common.stats.StatsCapability"
        );
        var dmzData = StatsProvider
            .get(StatsCapability.INSTANCE, mcPlayer)
            .orElse(null);
        if (dmzData != null) {
            var questData = dmzData.getPlayerQuestData();
            if (questData != null && questData.isInParty() === true) {
                questData.clearPartyState();
                cleared = true;
            }
        }
    } catch (err2) {}

    try {
        syncProgression(mcPlayer);
    } catch (syncErr) {}

    return cleared;
}

function clearStuckSagaDifficulty(player, dmzData, notify) {
    if (dmzData == null) {
        return false;
    }

    try {
        var questData =
            dmzData.getPlayerQuestData();

        if (questData == null) {
            return false;
        }

        var mcPlayer = getMcPlayer(player);
        var wasChosen = isDifficultyChosen(questData);
        var wasInParty = isInDmzParty(questData);
        var wasLeader = isDmzPartyLeader(
            questData,
            mcPlayer
        );

        /*
         * Non-leaders never get a working picker even after
         * unlocking difficultyChosen. Leave party so solo
         * selection works again.
         */
        if (wasInParty && !wasLeader) {
            leaveDmzParty(mcPlayer);
            try {
                questData =
                    dmzData.getPlayerQuestData();
            } catch (refreshErr) {}
        }

        try {
            questData.requestDifficultyReselect();
        } catch (reselectErr) {
            try {
                questData.setDifficultyChosen(false);
            } catch (setErr) {
                if (notify && player != null) {
                    player.message(
                        "\u00A7c[Race Lock] Could not clear difficultyChosen: " +
                        setErr
                    );
                }
                return false;
            }
        }

        try {
            questData.setDifficultyChosen(false);
        } catch (forceErr) {}

        var synced = syncProgression(mcPlayer);
        var stillChosen = isDifficultyChosen(questData);

        if (notify && player != null) {
            if (!stillChosen) {
                player.message(
                    "\u00A75[Race Lock] \u00A7aSaga difficulty unlocked."
                );
                player.message(
                    "\u00A77Close and reopen the Saga / Quest Tree, then choose Easy, Normal, or Hard."
                );
            } else {
                player.message(
                    "\u00A7c[Race Lock] Unlock ran but difficultyChosen is still true."
                );
            }

            if (!synced) {
                player.message(
                    "\u00A7c[Race Lock] Client sync failed — relog after unlock."
                );
            }

            if (wasInParty && !wasLeader) {
                player.message(
                    "\u00A7e[Race Lock] Left DMZ party so difficulty selection is allowed."
                );
            }

        } else if (DEBUG && wasChosen && player != null) {
            player.message(
                "\u00A76[Race Lock Debug] \u00A77Cleared stuck saga difficultyChosen so the picker can open again."
            );
        }

        return !stillChosen;
    } catch (err) {
        if (notify && player != null) {
            player.message(
                "\u00A7c[Race Lock] Difficulty unlock error: " +
                err
            );
        }
        return false;
    }
}

/*
 * Keep retrying while character creation is incomplete and
 * difficulty/party state blocks the picker. No Bukkit needed.
 */
function maybeAutoUnlockStuckDifficulty(player, dmzData, temp) {
    if (player == null || dmzData == null || temp == null) {
        return false;
    }

    var questData = null;
    var status = null;
    try {
        questData = dmzData.getPlayerQuestData();
    } catch (qErr) {}
    try {
        status = dmzData.getStatus();
    } catch (sErr) {}

    var created = hasCreatedCharacter(status);
    var chosen = isDifficultyChosen(questData);
    var inParty = isInDmzParty(questData);
    var leader = isDmzPartyLeader(
        questData,
        getMcPlayer(player)
    );

    /*
     * Finished characters keep their chosen difficulty.
     * Use !unlockdifficulty to force reselect.
     */
    if (created) {
        return false;
    }

    var stuck = chosen || (inParty && !leader);
    if (!stuck) {
        return false;
    }

    var coolKey = "race_lock_saga_diff_cooldown";
    var cool = 0;
    try {
        cool = parseInt("" + temp.get(coolKey), 10);
        if (isNaN(cool)) {
            cool = 0;
        }
    } catch (coolErr) {
        cool = 0;
    }

    if (cool > 0) {
        temp.put(coolKey, "" + (cool - 1));
        return false;
    }

    temp.put(coolKey, "" + SAGA_UNLOCK_RETRY_TICKS);
    return clearStuckSagaDifficulty(
        player,
        dmzData,
        true
    );
}

var SAGA_TRIGGER_ID = 120;

function resolveScriptPlayer(event) {
    if (event == null) {
        return null;
    }

    /*
     * Player events (tick/login/chat) expose event.player.
     * ScriptTriggerEvent exposes event.entity + event.arguments.
     * Using only event.player makes /noppes script trigger do nothing.
     */
    try {
        if (event.player != null) {
            return event.player;
        }
    } catch (playerErr) {}

    try {
        if (event.entity != null) {
            var ent = event.entity;
            try {
                if (ent.getMCEntity && ent.getMCEntity() != null) {
                    var mc = ent.getMCEntity();
                    if (
                        mc != null &&
                        (
                            "" + mc.getClass().getName()
                        ).indexOf("Player") >= 0
                    ) {
                        return ent;
                    }
                }
            } catch (entTypeErr) {}

            try {
                if (ent.getName && ent.getUUID) {
                    return ent;
                }
            } catch (entErr) {}
        }
    } catch (entityErr) {}

    try {
        if (
            event.arguments != null &&
            event.arguments.length > 0 &&
            event.arguments[0] != null &&
            ("" + event.arguments[0]).length > 0
        ) {
            var name = ("" + event.arguments[0]).trim();
            var world = null;

            try {
                if (event.player != null) {
                    world = event.player.getWorld();
                }
            } catch (w1) {}

            try {
                if (world == null && event.entity != null) {
                    world = event.entity.getWorld();
                }
            } catch (w2) {}

            try {
                if (world == null && event.level != null) {
                    /* some CNPC builds expose level/world on WorldEvent */
                    var players = event.level.getAllPlayers
                        ? event.level.getAllPlayers()
                        : null;
                    if (players != null) {
                        for (var i = 0; i < players.length; i++) {
                            if (
                                ("" + players[i].getName())
                                    .toLowerCase() ===
                                name.toLowerCase()
                            ) {
                                return players[i];
                            }
                        }
                    }
                }
            } catch (w3) {}

            if (world != null) {
                try {
                    var found = world.getPlayer(name);
                    if (found != null) {
                        return found;
                    }
                } catch (getErr) {}

                try {
                    var all = world.getAllPlayers();
                    for (var j = 0; j < all.length; j++) {
                        if (
                            ("" + all[j].getName())
                                .toLowerCase() ===
                            name.toLowerCase()
                        ) {
                            return all[j];
                        }
                    }
                } catch (scanErr) {}
            }

            /* Bukkit fallback used by other scripts on this server */
            try {
                var Bukkit = Java.type("org.bukkit.Bukkit");
                var bp = Bukkit.getPlayer(name);
                if (bp == null) {
                    bp = Bukkit.getPlayerExact(name);
                }
                if (bp != null) {
                    var NPCAPI = Java.type(
                        "noppes.npcs.api.NpcAPI"
                    ).Instance();
                    var mcBp = bp.getPlayer
                        ? bp.getPlayer()
                        : null;
                    /* CraftBukkit player -> MC -> IPlayer */
                    try {
                        var handle = bp.getClass()
                            .getMethod("getHandle")
                            .invoke(bp);
                        return NPCAPI.getIEntity(handle);
                    } catch (handleErr) {}
                }
            } catch (bukkitErr) {}
        }
    } catch (argErr) {}

    return null;
}

function runManualSagaDifficultyUnlock(player, sourceLabel) {
    if (player == null) {
        return false;
    }

    var dmzData = loadDmzData(player);
    if (dmzData == null) {
        try {
            player.message(
                "\u00A7c[Race Lock] Could not read DMZ data."
            );
        } catch (err2) {}
        return false;
    }

    return clearStuckSagaDifficulty(player, dmzData, true);
}

/*
 * Chat often does nothing on hybrid / plugin chat bridges.
 * Keep it, but prefer Trigger:
 *   /noppes script trigger 120
 *   /noppes script trigger 120 <player>
 */
function chat(event) {
    try {
        var message = "" + event.message;
        if (message == null) {
            return;
        }

        var trimmed = message.trim().toLowerCase();
        if (
            trimmed !== "!unlockdifficulty" &&
            trimmed !== "!sagadifficulty"
        ) {
            return;
        }

        try {
            event.setCanceled(true);
        } catch (cancelErr) {}

        var player = resolveScriptPlayer(event);
        if (player == null) {
            return;
        }
        runManualSagaDifficultyUnlock(player, "chat");
    } catch (err) {
        try {
            var p = resolveScriptPlayer(event);
            if (p != null) {
                p.message(
                    "\u00A7c[Race Lock] Difficulty unlock error: " +
                    err
                );
            }
        } catch (msgErr) {}
    }
}

function trigger(event) {
    try {
        if (
            event.id != null &&
            Number(event.id) !== SAGA_TRIGGER_ID
        ) {
            return;
        }
    } catch (idErr) {}

    try {
        var player = resolveScriptPlayer(event);
        if (player == null) {
            try {
                print(
                    "[Race Lock] trigger " +
                    SAGA_TRIGGER_ID +
                    " could not resolve a player. Use: /noppes script trigger " +
                    SAGA_TRIGGER_ID +
                    " <playerName>"
                );
            } catch (printErr) {}
            return;
        }

        runManualSagaDifficultyUnlock(
            player,
            "trigger " + SAGA_TRIGGER_ID
        );
    } catch (err) {
        try {
            var p2 = resolveScriptPlayer(event);
            if (p2 != null) {
                p2.message(
                    "\u00A7c[Race Lock] Difficulty unlock error: " +
                    err
                );
            }
            print("[Race Lock] trigger error: " + err);
        } catch (msgErr) {}
    }
}

function login(event) {
    try {
        runSessionSagaDifficultyCheck(event.player, true);
    } catch (err) {}
}

/*
 * Once per login session (Tick or Login).
 *
 * Only unlock difficulty when the player is STUCK after a wipe:
 * hasCreatedCharacter == false but difficultyChosen/party still
 * blocks the picker.
 *
 * Do NOT clear difficultyChosen for finished characters. That was
 * forcing every relog to re-pick difficulty (quest progress kept,
 * only rewards scaled) even when Dragon Balls were never used.
 */
function runSessionSagaDifficultyCheck(player, fromLogin) {
    if (player == null) {
        return;
    }

    var temp = player.getTempdata();
    var doneKey = "race_lock_saga_session_check";
    try {
        if (temp.get(doneKey) != null) {
            return;
        }
    } catch (err) {}

    try {
        temp.put(doneKey, "1");
    } catch (putErr) {}

    var dmzData = loadDmzData(player);
    if (dmzData == null) {
        return;
    }

    var questData = null;
    var status = null;
    try {
        questData = dmzData.getPlayerQuestData();
    } catch (qErr) {}
    try {
        status = dmzData.getStatus();
    } catch (sErr) {}

    /*
     * Finished characters already chose difficulty. Leave them alone.
     * Manual unlock: /noppes script trigger 120  or  !unlockdifficulty
     */
    if (hasCreatedCharacter(status)) {
        return;
    }

    var chosen = isDifficultyChosen(questData);
    var inParty = isInDmzParty(questData);
    var leader = isDmzPartyLeader(
        questData,
        getMcPlayer(player)
    );

    if (chosen || (inParty && !leader)) {
        clearStuckSagaDifficulty(player, dmzData, true);
    }
}

function tryEarlySagaDifficultyUnlock(player) {
    try {
        if (player == null) {
            return;
        }

        /* Session check first so Tick-only installs still unlock. */
        runSessionSagaDifficultyCheck(player, false);

        var temp = player.getTempdata();
        var dmzData = loadDmzData(player);
        if (dmzData == null) {
            return;
        }
        maybeAutoUnlockStuckDifficulty(
            player,
            dmzData,
            temp
        );
    } catch (err) {}
}

function tick(event) {
    try {
        var player = event.player;

        if (player == null) {
            return;
        }

        /*
         * Unlock saga difficulty before any Bukkit-dependent
         * race-lock logic. Missing Bukkit must not block this.
         */
        tryEarlySagaDifficultyUnlock(player);

        var temp =
            player.getTempdata();


        // ====================================================
        // CHECK INTERVAL
        // ====================================================

        var tickKey =
            "restricted_race_command_tick";

        var tickCount =
            temp.get(tickKey);

        if (tickCount == null) {
            tickCount = 0;
        }

        tickCount =
            parseInt("" + tickCount) + 1;

        if (isNaN(tickCount)) {
            tickCount = 1;
        }

        if (tickCount < TICK_INTERVAL) {
            temp.put(
                tickKey,
                "" + tickCount
            );

            return;
        }

        temp.put(
            tickKey,
            "0"
        );


        // ====================================================
        // RESET RETRY COOLDOWN
        // ====================================================

        var retryKey =
            "restricted_race_command_retry";

        var retryChecks =
            temp.get(retryKey);

        if (retryChecks == null) {
            retryChecks = 0;
        }

        retryChecks =
            parseInt("" + retryChecks);

        if (isNaN(retryChecks)) {
            retryChecks = 0;
        }

        if (retryChecks > 0) {
            retryChecks =
                retryChecks - 1;

            temp.put(
                retryKey,
                "" + retryChecks
            );

            return;
        }


        // ====================================================
        // VALIDATE CONFIGURATION
        // ====================================================

        if (
            RESTRICTED_RACE_IDS.length !=
            REQUIRED_FABLED_SKILLS.length
        ) {
            throw (
                "RESTRICTED_RACE_IDS and " +
                "REQUIRED_FABLED_SKILLS must contain " +
                "the same number of entries."
            );
        }

        if (
            RESTRICTED_RACE_DISPLAY_NAMES.length !=
            RESTRICTED_RACE_IDS.length
        ) {
            throw (
                "RESTRICTED_RACE_DISPLAY_NAMES and " +
                "RESTRICTED_RACE_IDS must contain " +
                "the same number of entries."
            );
        }

        if (
            typeof REQUIRED_PERMISSIONS !== "undefined" &&
            REQUIRED_PERMISSIONS != null &&
            REQUIRED_PERMISSIONS.length !=
                RESTRICTED_RACE_IDS.length
        ) {
            throw (
                "REQUIRED_PERMISSIONS and " +
                "RESTRICTED_RACE_IDS must contain " +
                "the same number of entries."
            );
        }

        if (
            typeof UNLOCK_VIA_PRESTIGE_HINTS !== "undefined" &&
            UNLOCK_VIA_PRESTIGE_HINTS != null &&
            UNLOCK_VIA_PRESTIGE_HINTS.length !=
                RESTRICTED_RACE_IDS.length
        ) {
            throw (
                "UNLOCK_VIA_PRESTIGE_HINTS and " +
                "RESTRICTED_RACE_IDS must contain " +
                "the same number of entries."
            );
        }


        // ====================================================
        // JAVA CLASSES
        // ====================================================

        var Bukkit = Java.type(
            "org.bukkit.Bukkit"
        );

        var UUID = Java.type(
            "java.util.UUID"
        );

        var StatsProvider = Java.type(
            "com.dragonminez.common.stats.StatsProvider"
        );

        var StatsCapability = Java.type(
            "com.dragonminez.common.stats.StatsCapability"
        );


        // ====================================================
        // GET THE BUKKIT PLAYER
        //
        // This is copied from the working Fabled attribute
        // script's approach.
        // ====================================================

        var bukkitPlayer =
            Bukkit.getPlayer(
                UUID.fromString(
                    "" + player.getUUID()
                )
            );

        if (bukkitPlayer == null) {
            if (DEBUG) {
                player.message(
                    "\u00A7c[Race Lock Debug] Bukkit player was unavailable."
                );
            }

            return;
        }


        // ====================================================
        // GET DMZ DATA
        // ====================================================

        var lazy = StatsProvider.get(
            StatsCapability.INSTANCE,
            player.getMCEntity()
        );

        if (lazy == null) {
            if (DEBUG) {
                player.message(
                    "\u00A7c[Race Lock Debug] DMZ LazyOptional was unavailable."
                );
            }

            return;
        }

        var dmzData =
            lazy.orElse(null);

        if (dmzData == null) {
            if (DEBUG) {
                player.message(
                    "\u00A7c[Race Lock Debug] DMZ player data was unavailable."
                );
            }

            return;
        }

        var status =
            dmzData.getStatus();

        if (status == null) {
            if (DEBUG) {
                player.message(
                    "\u00A7c[Race Lock Debug] DMZ status data was unavailable."
                );
            }

            return;
        }


        // After a successful reset, DMZ marks the character as
        // not created. Stop checking until the player creates
        // another character.
        //
        // Also unlock stuck saga difficulty once — dmzstats
        // reset leaves difficultyChosen true, which blocks the
        // picker until requestDifficultyReselect runs.

        var character =
            dmzData.getCharacter();

        if (character == null) {
            if (DEBUG) {
                player.message(
                    "\u00A7c[Race Lock Debug] DMZ character data was unavailable."
                );
            }

            return;
        }


        // ====================================================
        // READ THE EXACT DMZ RACE ID
        // ====================================================

        var rawRaceId =
            character.getRace();

        if (rawRaceId == null) {
            return;
        }

        var raceId =
            ("" + rawRaceId).trim();

        if (
            raceId == "" ||
            raceId == "null"
        ) {
            return;
        }

        var lowerRaceId =
            raceId.toLowerCase();


        // Character not finished yet: allow full GUI preview.
        // No chat here — players cannot see chat in race selection.
        if (!status.isHasCreatedCharacter()) {
            maybeAutoUnlockStuckDifficulty(
                player,
                dmzData,
                temp
            );

            temp.remove(
                "restricted_race_command_last_state"
            );


            return;
        }


        // ====================================================
        // FIND THE RACE IN THE RESTRICTED LIST
        // ====================================================

        var restrictedIndex = -1;

        var raceIndex;

        for (
            raceIndex = 0;
            raceIndex <
                RESTRICTED_RACE_IDS.length;
            raceIndex++
        ) {
            var configuredRaceId =
                "" +
                RESTRICTED_RACE_IDS[
                    raceIndex
                ];

            configuredRaceId =
                configuredRaceId
                    .trim()
                    .toLowerCase();

            if (
                lowerRaceId ==
                configuredRaceId
            ) {
                restrictedIndex =
                    raceIndex;

                break;
            }
        }


        // The player's current race is not restricted.

        if (restrictedIndex == -1) {
            if (DEBUG) {
                var unrestrictedState =
                    "unrestricted|" +
                    lowerRaceId;

                var oldUnrestrictedState =
                    temp.get(
                        "restricted_race_command_last_state"
                    );

                if (
                    oldUnrestrictedState == null ||
                    ("" + oldUnrestrictedState) !=
                        unrestrictedState
                ) {
                    temp.put(
                        "restricted_race_command_last_state",
                        unrestrictedState
                    );

                    player.message(
                        "\u00A76[Race Lock Debug] \u00A77Actual race ID: \u00A7f[" +
                        raceId +
                        "]"
                    );

                    player.message(
                        "\u00A76[Race Lock Debug] \u00A77This race is not restricted."
                    );
                }
            }

            return;
        }


        // ====================================================
        // READ THE REQUIRED FABLED SKILL
        // ====================================================

        var requiredSkill =
            "" +
            REQUIRED_FABLED_SKILLS[
                restrictedIndex
            ];

        requiredSkill =
            requiredSkill.trim();

        var raceDisplayName =
            "" +
            RESTRICTED_RACE_DISPLAY_NAMES[
                restrictedIndex
            ];

        raceDisplayName =
            raceDisplayName.trim();

        if (raceDisplayName == "") {
            raceDisplayName =
                raceId;
        }


        // ====================================================
        // UNLOCK CHECK
        //   permission ? LuckPerms / Bukkit hasPermission
        //   skill      ? Fabled skill level >= 1
        // ====================================================

        var unlockMode = "skill";
        try {
            if (
                typeof UNLOCK_MODES !== "undefined" &&
                UNLOCK_MODES != null &&
                restrictedIndex < UNLOCK_MODES.length
            ) {
                unlockMode =
                    ("" + UNLOCK_MODES[restrictedIndex])
                        .trim()
                        .toLowerCase();
            }
        } catch (modeErr) {
            unlockMode = "skill";
        }

        if (unlockMode == "permission") {
            var requiredPerm = "";
            try {
                requiredPerm =
                    "" +
                    REQUIRED_PERMISSIONS[restrictedIndex];
                requiredPerm = requiredPerm.trim();
            } catch (permReadErr) {
                requiredPerm = "";
            }

            var hasPerm = false;
            if (requiredPerm != "") {
                try {
                    hasPerm = !!bukkitPlayer.hasPermission(
                        requiredPerm
                    );
                } catch (permCheckErr) {
                    hasPerm = false;
                }
            }

            if (DEBUG) {
                player.message(
                    "\u00A76[Race Lock Debug] \u00A77Unlock mode: permission | node: \u00A7f" +
                    requiredPerm +
                    "\u00A77 | has: \u00A7f" +
                    hasPerm
                );
            }

            if (hasPerm) {
                return;
            }

            player.message(
                "\u00A7c" +
                raceDisplayName +
                "\u00A77 is locked until you have permission \u00A7f" +
                requiredPerm +
                "\u00A77 (Ancient Rights / unlockrace). Prestige level is not checked."
            );
        } else {
            if (requiredSkill == "") {
                throw (
                    "Race " +
                    raceId +
                    " has no required Fabled skill configured."
                );
            }

            // ====================================================
            // GET THE FABLED PLUGIN + SKILL LEVEL
            // ====================================================

            var plugin =
                Bukkit
                    .getPluginManager()
                    .getPlugin("Fabled");

            if (
                plugin == null ||
                !plugin.isEnabled()
            ) {
                if (DEBUG) {
                    player.message(
                        "\u00A7c[Race Lock Debug] Fabled is not loaded or enabled."
                    );
                }

                return;
            }

            var loader =
                plugin
                    .getClass()
                    .getClassLoader();

            var fabledClass =
                loader.loadClass(
                    "studio.magemonkey.fabled.Fabled"
                );

            var getDataMethod = null;

            var methods =
                fabledClass.getMethods();

            var methodIndex;

            for (
                methodIndex = 0;
                methodIndex <
                    methods.length;
                methodIndex++
            ) {
                if (
                    String(
                        methods[
                            methodIndex
                        ].getName()
                    ) == "getData" &&
                    methods[
                        methodIndex
                    ].getParameterTypes().length == 1
                ) {
                    getDataMethod =
                        methods[
                            methodIndex
                        ];

                    break;
                }
            }

            if (getDataMethod == null) {
                if (DEBUG) {
                    player.message(
                        "\u00A7c[Race Lock Debug] Fabled getData method was not found."
                    );
                }

                return;
            }

            var fabledData =
                getDataMethod.invoke(
                    null,
                    bukkitPlayer
                );

            if (fabledData == null) {
                if (DEBUG) {
                    player.message(
                        "\u00A7c[Race Lock Debug] Fabled player data was unavailable."
                    );
                }

                return;
            }

            var skillLevel = 0;

            try {
                skillLevel =
                    Number(
                        fabledData.getSkillLevel(
                            requiredSkill
                        )
                    );

            } catch (skillError) {
                if (DEBUG) {
                    player.message(
                        "\u00A7c[Race Lock Debug] getSkillLevel failed: \u00A7f" +
                        skillError
                    );
                }

                return;
            }

            if (isNaN(skillLevel)) {
                skillLevel = 0;
            }

            if (DEBUG) {
                var restrictedState =
                    "restricted|" +
                    lowerRaceId +
                    "|" +
                    requiredSkill.toLowerCase() +
                    "|" +
                    skillLevel;

                var oldRestrictedState =
                    temp.get(
                        "restricted_race_command_last_state"
                    );

                if (
                    oldRestrictedState == null ||
                    ("" + oldRestrictedState) !=
                        restrictedState
                ) {
                    temp.put(
                        "restricted_race_command_last_state",
                        restrictedState
                    );

                    player.message(
                        "\u00A76[Race Lock Debug] \u00A77Actual race ID: \u00A7f[" +
                        raceId +
                        "]"
                    );

                    player.message(
                        "\u00A76[Race Lock Debug] \u00A77Restricted race matched: \u00A7f" +
                        raceId
                    );

                    player.message(
                        "\u00A76[Race Lock Debug] \u00A77Required Fabled skill: \u00A7f" +
                        requiredSkill
                    );

                    player.message(
                        "\u00A76[Race Lock Debug] \u00A77Current skill level: \u00A7f" +
                        skillLevel
                    );
                }
            }

            if (skillLevel >= 1) {
                return;
            }

            player.message(
                "\u00A7c" +
                raceDisplayName +
                "\u00A77 is locked until you buy its Prestige unlock skill. Once purchased it stays unlocked."
            );
        }


        // ====================================================
        // BUILD THE EXACT DMZ RESET COMMAND
        // ====================================================
        //
        // Verified DMZ syntax:
        //
        // dmzstats reset <targets> <keepPercentage> <keepSkills>
        //
        // For a complete reset:
        //
        // dmzstats reset PlayerName 0 false
        // ====================================================

        var resetCommand =
            "dmzstats reset " +
            player.getName() +
            " 0 false";


        // Prefer GUI padlock (SDU RaceLockClient). Keep one short fallback line.
        // Unlock is the purchased Fabled skill — not current/active prestige level.
        player.message(
            "\u00A7c" +
            raceDisplayName +
            "\u00A77 is locked until you buy its Prestige unlock skill. Once purchased it stays unlocked."
        );

        if (DEBUG) {
            player.message(
                "\u00A76[Race Lock Debug] \u00A77Running command: \u00A7f" +
                resetCommand
            );
        }


        // ====================================================
        // EXECUTE THE COMMAND
        // ====================================================
        //
        // First try Bukkit's console dispatcher. This gives the
        // command full console permissions.
        //
        // If the Bukkit command bridge does not recognize the
        // Forge command, fall back to CustomNPCs' command
        // executor, which runs through Minecraft's dispatcher.
        // ====================================================

        var dispatchedThroughBukkit = false;
        var customNpcCommandOutput = null;

        try {
            dispatchedThroughBukkit =
                Bukkit.dispatchCommand(
                    Bukkit.getConsoleSender(),
                    resetCommand
                );

        } catch (bukkitCommandError) {
            if (DEBUG) {
                player.message(
                    "\u00A7c[Race Lock Debug] Bukkit command error: \u00A7f" +
                    bukkitCommandError
                );
            }

            dispatchedThroughBukkit =
                false;
        }


        if (!dispatchedThroughBukkit) {
            try {
                customNpcCommandOutput =
                    event.API.executeCommand(
                        player.getWorld(),
                        resetCommand
                    );

            } catch (cnpcCommandError) {
                player.message(
                    "\u00A7c[Race Lock] Both command execution methods failed."
                );

                if (DEBUG) {
                    player.message(
                        "\u00A7c[Race Lock Debug] CNPC command error: \u00A7f" +
                        cnpcCommandError
                    );
                }

                temp.put(
                    retryKey,
                    "" + RESET_RETRY_CHECKS
                );

                return;
            }
        }


        // Prevent command spam if the reset did not take effect
        // immediately. The script will retry after the configured
        // number of checks.

        temp.put(
            retryKey,
            "" + RESET_RETRY_CHECKS
        );

        /*
         * Clear stuck saga difficulty as soon as the reset
         * command is issued. Do not wait for the character-
         * created flag to flip — that is what blocks the picker.
         */
        clearStuckSagaDifficulty(
            player,
            dmzData,
            true
        );


        // ====================================================
        // COMMAND RESULT DEBUGGING
        // ====================================================

        if (DEBUG) {
            player.message(
                "\u00A76[Race Lock Debug] \u00A77Bukkit dispatch result: \u00A7f" +
                dispatchedThroughBukkit
            );

            if (customNpcCommandOutput != null) {
                player.message(
                    "\u00A76[Race Lock Debug] \u00A77CNPC command output: \u00A7f" +
                    customNpcCommandOutput
                );
            }
        }


        // ====================================================
        // IMMEDIATE RESET VERIFICATION
        // ====================================================
        //
        // Command execution is normally synchronous, so DMZ's
        // character-created status should already be false.
        //
        // Even if the immediate check has not updated yet, the
        // script will check again after the retry cooldown.
        // ====================================================

        var updatedStatus =
            dmzData.getStatus();

        if (
            updatedStatus != null &&
            !updatedStatus.isHasCreatedCharacter()
        ) {
            /*
             * dmzstats reset clears quest progress but does NOT
             * clear difficultyChosen. If that flag stays true,
             * SetStoryDifficultyC2S rejects every click and the
             * saga difficulty picker never works again.
             */
            clearStuckSagaDifficulty(
                player,
                dmzData,
                true
            );

            player.message(
                "\u00A7a[Race Lock] Your DMZ character was reset."
            );

            player.message(
                "\u00A77Purchase \u00A7f" +
                requiredSkill +
                " \u00A77before selecting \u00A7f" +
                raceDisplayName +
                " \u00A77again."
            );

            temp.remove(
                "restricted_race_command_last_state"
            );

        } else {
            player.message(
                "\u00A7e[Race Lock] The reset command was issued, but DMZ " +
                "still reports the character as created."
            );

            if (DEBUG) {
                player.message(
                    "\u00A7e[Race Lock Debug] The script will retry after " +
                    RESET_RETRY_CHECKS +
                    " checks."
                );
            }
        }

    } catch (error) {
        var playerForError =
            event.player;

        if (playerForError != null) {
            var errorTemp =
                playerForError.getTempdata();

            var errorText =
                "" + error;

            var previousError =
                errorTemp.get(
                    "restricted_race_command_last_error"
                );

            if (
                previousError == null ||
                ("" + previousError) !=
                    errorText
            ) {
                errorTemp.put(
                    "restricted_race_command_last_error",
                    errorText
                );

                playerForError.message(
                    "\u00A7c[Race Lock Error] \u00A7f" +
                    errorText
                );

                print(
                    "[Restricted Race Command] Error for " +
                    playerForError.getName() +
                    ": " +
                    errorText
                );
            }
        }
    }
}