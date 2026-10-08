# DMZ Mohist Melee Fix

**v2.12.24** — the priceless skill-purchase mixins are not registered. They called `PricelessPurchaseGuard` from Dragon Mine Z classes, and that mod's classloader cannot see this mod. Melee, stat reset, and the other Mohist fixes stay on. Grab still waits until `GrabService` exists as a class file.

Jar: `mods/dmz_mohist_melee_fix-2.12.24.jar`

**Client note:** Old Kai Precision skip is a **client** mixin. Put this jar in the client/modpack `mods/` folder too (not only the server).

Build: `./tools/dmz-mohist-melee-fix/build.sh`
