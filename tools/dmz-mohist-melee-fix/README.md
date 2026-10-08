# DMZ Mohist Melee Fix

**v2.12.23** — hard-disables Dragon Block Noea experimental grab when `noeabosses` is present. The grab mixin looks up `GrabService` as a class file and does not define that class during mixin config.

Jar: `mods/dmz_mohist_melee_fix-2.12.23.jar`

**Client note:** Old Kai Precision skip is a **client** mixin. Put this jar in the client/modpack `mods/` folder too (not only the server).

Build: `./tools/dmz-mohist-melee-fix/build.sh`
