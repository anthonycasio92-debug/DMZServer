# Kai Path — LegacyMechanics Ultra GUI bridge (drop-in)

**Just the bridge.** Not part of LegacyMechanics server jar and not a patch to `dmzultra`.
Copy these sources into the Kai Path project so **kaipath** owns the client channel that
opens Ultra-themed LM menus. Server LM stays server-only.

## What this does

| Piece | Owner |
|-------|--------|
| Gameplay / `/lm` / action handling | Server `LegacyMechanics` |
| Ultra textures / `UltraUi` widgets | Client `dmzultra` (optional soft dep — already in kaipath `mods.toml`) |
| Channel `legacymechanics:gui` + hub screens | **Kai Path** (this package) |

Wire format (do not change without bumping LM server too):

- Channel: `legacymechanics:gui`
- Protocol: `"1"`
- Msg 0 S2C: UTF `screenId` (e.g. `hub`)
- Msg 1 C2S: UTF `screenId`, `actionId`, `argsJson`

Hub actions today: `character`, `difficulty`, `progression`, `prestige`, `spar`, `rival`.

## Copy into Kai Path

Package already matches Kai Path: `com.dmzlegacy.kaipath.lmbridge.*`

```
src/main/java/com/dmzlegacy/kaipath/lmbridge/
  LmGuiBridge.java
  gui/
    UltraWidgetAdapter.java   # reflective soft-dep on com.dmzultra.client.ui.*
    LmScreen.java
    LmHubScreen.java
    LmScreens.java
    LmMissingScreen.java
  net/
    LmGuiNetwork.java
    GuiOpenPacket.java
    GuiActionPacket.java
```

## One-line hook

Call once from Kai Path **client** init (alongside existing Ultra compat):

```java
// In KaiPath constructor (client only), or KaiClient setup:
com.dmzlegacy.kaipath.lmbridge.LmGuiBridge.bootstrap();
```

Example with DistExecutor:

```java
net.minecraftforge.fml.DistExecutor.safeRunWhenOn(
        net.minecraftforge.api.distmarker.Dist.CLIENT,
        () -> com.dmzlegacy.kaipath.lmbridge.LmGuiBridge::bootstrap);
```

## mods.toml

Kai Path already has optional `dmzultra`. No new `[[mods]]` entry — this is not a second mod id.
Keep:

```toml
[[dependencies.kaipath]]
modId="dmzultra"
mandatory=false
versionRange="(,0),[0,)"
ordering="AFTER"
side="BOTH"
```

## Do NOT ship

- `LegacyMechanics-*.jar` on clients
- A separate `LegacyMechanicsUltra` companion (this bridge replaces it)
- Hard compile dependency on `com.dmzultra.*` (use the reflective adapter)

## Server config

```json
"guiBackend": "ultra"
```

`/lm` → server sends open packet → kaipath client opens Ultra hub → clicks return as actions → server opens chat/subsystem menus.

## Conflict note

Only **one** client mod may register `legacymechanics:gui`. If an old `LegacyMechanicsUltra` jar is still in the pack, remove it after kaipath ships this bridge.
