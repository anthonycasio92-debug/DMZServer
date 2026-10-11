# KaiPath Ultra GUI client overlay (multi-mod)

Patches KaiPath **1.3.0** so the client decodes the full 10-field `GuiOpenPacket`, renders
menus dynamically, and registers consumers **by channel** (`<modid>:gui`).

See `docs/KAIPATH_JLDK_CLIENT_BRIEF.md`, `docs/KAIPATH_GUI_PROTOCOL.md`, and
`docs/ultra-gui-protocol/reference/`.

## Build

```bash
# Place stock kaipath-1.3.0.jar here (or set KAIPATH_BASE_JAR):
#   tools/kaipath-lm-ultra-client/base/kaipath-1.3.0.jar
bash tools/kaipath-lm-ultra-client/build.sh
```

Output: `mods/kaipath-1.3.1-lm-ultra.jar`

## What changes

| Class | Change |
|-------|--------|
| `UltraGuiConsumerRegistry` | Multi-mod registry keyed by `<modid>:gui` |
| `GuiOpenPacket` | Decode all 10 fields; defaults; ignore bytes after #10 |
| `LmDynamicScreen` | Dynamic UI; clicks echo on the bound channel |
| `LmScreens` | No screenId whitelist; `open(packet, channelId)` |
| `LmGuiNetwork` | Thin LM binding → registry (`legacymechanics`) |
| `LmUltraConsumer` | Registers the LM channel via the registry |

Other mods (upstream KaiPath): `UltraGuiConsumerRegistry.registerClientChannel("mymod")`.

## Deploy

Replace the client `kaipath-*.jar` with `kaipath-1.3.1-lm-ultra.jar`. Keep LM **4.6.104+**.
