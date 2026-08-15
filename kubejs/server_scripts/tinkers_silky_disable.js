/*
 * DBZ Legacy Reborn - Disable Tinkers' Construct Silky (silk touch).
 *
 * Pure ASCII so KubeJS UTF-8 reader never hits MalformedInputException.
 *
 * 1) Removes the Silky ability + salvage recipes (cannot apply / crystal).
 * 2) Datapack override (kubejs/data + highPriorityData) zeros the modifier
 *    modules so existing tools no longer grant silk touch.
 *
 * Existing tools may still list Silky, but it will no longer silk-touch.
 * Reload: /reload  or  /kubejs reload server_scripts then /reload
 */

console.info(
    "[Tinkers Silky] Disabling Silky silk-touch modifier..."
);

ServerEvents.recipes(function (event) {
    var ids = [
        "tconstruct:tools/modifiers/ability/silky",
        "tconstruct:tools/modifiers/salvage/ability/silky"
    ];
    var removed = 0;
    for (var i = 0; i < ids.length; i++) {
        try {
            event.remove({ id: ids[i] });
            removed++;
            console.info("[Tinkers Silky] Removed recipe " + ids[i]);
        } catch (eRem) {
            console.info(
                "[Tinkers Silky] Could not remove " + ids[i] + ": " + eRem
            );
        }
    }

    /* Catch any other recipe that results in tconstruct:silky. */
    try {
        event.forEachRecipe({ type: "tconstruct:modifier" }, function (recipe) {
            var keep = true;
            try {
                var rid = String(recipe.getId()).toLowerCase();
                if (rid.indexOf("silky") >= 0) keep = false;
            } catch (eId) {}
            try {
                var json = recipe.json;
                var result = null;
                try {
                    result = json.get("result");
                } catch (e1) {
                    try {
                        result = json.result;
                    } catch (e2) {}
                }
                if (result != null && String(result).indexOf("silky") >= 0) {
                    keep = false;
                }
            } catch (eJ) {}
            if (!keep) {
                try {
                    event.remove({ id: recipe.getId() });
                    removed++;
                    console.info(
                        "[Tinkers Silky] Removed modifier recipe " +
                            recipe.getId()
                    );
                } catch (eR2) {}
            }
        });
    } catch (eForEach) {}

    console.info(
        "[DBZ Legacy Reborn] Tinkers Silky recipes removed (" +
            removed +
            "). Modifier datapack zeroed."
    );
});

/*
 * highPriorityData mirrors kubejs/data overrides so /reload applies even if
 * the data folder copy is missing on the server.
 */
ServerEvents.highPriorityData(function (event) {
    event.addJson("tconstruct:tinkering/modifiers/silky", {
        level_display: "tconstruct:no_levels",
        modules: [],
        tooltip_display: "never"
    });

    console.info(
        "[Tinkers Silky] highPriorityData applied (modifier empty)."
    );
});
