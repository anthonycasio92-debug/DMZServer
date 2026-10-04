/**
 * DBZ Legacy Reborn — disable Dragon Block Noea experimental grab (Z grab/throw).
 * Runs on full server load; safe to /kubejs reload server_scripts (loaded fires again).
 *
 * Manual (console): /noea experimental grab off
 * Legacy builds:     /noea cheat grab off
 */
ServerEvents.loaded(function (event) {
    var server = event.server;
    if (!server) return;

    var commands = [
        'noea experimental grab off',
        'noea cheat grab off',
    ];

    commands.forEach(function (cmd) {
        try {
            var code = server.runCommandSilent(cmd);
            console.info('[Noea Grab] ' + cmd + ' -> ' + code);
        } catch (err) {
            console.warn('[Noea Grab] ' + cmd + ' failed: ' + err);
        }
    });
});
