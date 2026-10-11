# KaiPath LM Ultra client overlay

Patches KaiPath **1.3.0** so the client decodes the full 10-field `GuiOpenPacket` and
renders LM menus dynamically (no hardcoded hub / screen whitelist).

See `docs/KAIPATH_JLDK_CLIENT_BRIEF.md` and `docs/KAIPATH_GUI_PROTOCOL.md`.

## Build

```bash
# Place stock kaipath-1.3.0.jar here (or set KAIPATH_BASE_JAR):
#   tools/kaipath-lm-ultra-client/base/kaipath-1.3.0.jar
bash tools/kaipath-lm-ultra-client/build.sh
```

Output: `mods/kaipath-1.3.1-lm-ultra.jar`

The build runs an offline encode/decode round-trip test (10 fields, future-field ignore,
`GuiActionPacket` verbatim echo, protocol accept-on-mismatch).

## What changes

| Class | Change |
|-------|--------|
| `GuiOpenPacket` | Decode all 10 fields; defaults; ignore bytes after #10 |
| `LmDynamicScreen` | Title / tabs / lines / buttons from packet JSON |
| `LmScreens` | No screenId whitelist; unknown id → dynamic screen |
| `LmGuiNetwork` | Accept any remote protocol (warn); `sendAction` verbatim |
| `LmHubScreen` | Thin subclass of `LmDynamicScreen` (compat) |
| `LmUltraConsumer` | Always register client channel (LM is server-only) |

## Deploy

Replace the client `kaipath-*.jar` with `kaipath-1.3.1-lm-ultra.jar`. Keep LM **4.6.104+**.
