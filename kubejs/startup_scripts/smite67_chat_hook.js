/*
 * Smite chat hook (startup) - Forge ServerChatEvent.
 * Pure ASCII. ForgeEvents only register from startup_scripts on this KubeJS build.
 *
 * Companion: kubejs/server_scripts/random_kick_player.js (toggle /smite67)
 * Uses global.smite67Enabled (default true if unset).
 *
 * Requires a FULL server restart once (startup hooks).
 */

console.info("[smite67] startup chat hook evaluating...");

function contains67(msg) {
    if (!msg) return false;
    var s = String(msg).toLowerCase();
    if (s.indexOf("sixtyseven") >= 0) return true;
    if (/sixty[\s\-_]*seven/.test(s)) return true;
    if (/\blxvii\b/.test(s)) return true;
    if (/(^|[^0-9])6[\s\-_./\\]*7([^0-9]|$)/.test(s)) return true;
    if (/\b6even\b/.test(s)) return true;
    return false;
}

function isEnabled() {
    try {
        if (typeof global !== "undefined" && global.smite67Enabled === false) {
            return false;
        }
    } catch (e1) {}
    return true;
}

function playerName(player) {
    try {
        return String(player.getGameProfile().getName());
    } catch (e1) {}
    try {
        return String(player.getName().getString());
    } catch (e2) {}
    return "?";
}

function smiteNow(player) {
    var name = playerName(player);
    try {
        var server = player.getServer();
        if (server == null) return;
        var stack = server.createCommandSourceStack().withSuppressedOutput();
        server
            .getCommands()
            .performPrefixedCommand(
                stack,
                "execute as " +
                    name +
                    " at @s run summon minecraft:lightning_bolt ~ ~ ~"
            );
        try {
            player.setSecondsOnFire(3);
        } catch (eFire) {}
        console.info("[smite67] smote " + name + " (Forge chat)");
    } catch (err) {
        console.error("[smite67] smite failed: " + err);
    }
}

function onServerChat(event) {
    try {
        if (!isEnabled()) return;
        var raw = "";
        try {
            if (event.getRawText) raw = String(event.getRawText());
        } catch (e1) {}
        try {
            if (!raw && event.getMessage) {
                var m = event.getMessage();
                if (m.getString) raw = String(m.getString());
                else raw = String(m);
            }
        } catch (e2) {}
        try {
            if (!raw && event.message != null) raw = String(event.message);
        } catch (e3) {}
        if (!contains67(raw)) return;

        var player = null;
        try {
            player = event.getPlayer();
        } catch (e4) {}
        try {
            if (player == null) player = event.player;
        } catch (e5) {}
        if (player == null) return;

        console.info(
            "[smite67] " + playerName(player) + " said (forge): " + raw
        );

        try {
            var server = player.getServer();
            if (server != null && server.execute) {
                server.execute(function () {
                    smiteNow(player);
                });
                return;
            }
        } catch (e6) {}
        smiteNow(player);
    } catch (err) {
        console.error("[smite67] ServerChatEvent error: " + err);
    }
}

try {
    ForgeEvents.onEvent(
        "net.minecraftforge.event.ServerChatEvent",
        onServerChat
    );
    console.info(
        "[smite67] startup: ServerChatEvent registered (restart required once)."
    );
} catch (err) {
    console.error("[smite67] startup register failed: " + err);
}
