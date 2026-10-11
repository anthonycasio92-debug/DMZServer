# JLDK brief — KaiPath Ultra GUI client (multi-mod)

**Status:** DMZServer ships a drop-in patched jar:
`mods/kaipath-1.3.1-lm-ultra.jar` (`tools/kaipath-lm-ultra-client/`).
Merge into upstream KaiPath so official releases no longer need the overlay.

**Protocol:** `docs/KAIPATH_GUI_PROTOCOL.md` · reference codecs:
`docs/ultra-gui-protocol/reference/` (zero LM imports).

---

## Problem

KaiPath **1.3.0** only decodes **1 of 10** `GuiOpenPacket` fields and renders a hardcoded
hub. Submenus cannot display. The consumer is also LM-only (`legacymechanics:gui`) with no
registry for other mods.

## Required work

### 1–5. Dynamic packet UI (critical path)

1. Decode all **10** `GuiOpenPacket` fields (defaults; ignore bytes after #10).
2. Render title / `tabsJson` / `linesJson` / `buttonsJson` dynamically; panel center + margin.
3. On click, send `GuiActionPacket` with **verbatim** `screenId` / `actionId` / `argsJson`.
4. **No screenId whitelist** — any id → dynamic screen; `MissingScreen` only for malformed packets.
5. Future-proof: dynamic tabs; never filter action ids; accept mismatched channel protocol with a warning.

### 7. Namespace-agnostic protocol

- Packet encode/decode must **not** contain hardcoded `legacymechanics` (or any mod id).
- Channel namespace is a **registration parameter** (`<modid>:gui`).
- Screen ids, verbs, and button schemas are opaque content — work for any mod.

### 8. Shared channel convention + multi-mod registry

```
channel = <modid>:gui
```

KaiPath must key consumers by channel id. Suggested API:

```java
UltraGuiConsumerRegistry.registerClientChannel("legacymechanics");
UltraGuiConsumerRegistry.registerClientChannel("mymod");
// clicks on a screen echo on the same channel that delivered GuiOpenPacket
```

Overlay reference: `com.dmzlegacy.kaipath.ultra.consumers.UltraGuiConsumerRegistry`.

### 9. Reference implementation

Copy-paste codecs (FriendlyByteBuf only, no LM imports):

- `docs/ultra-gui-protocol/reference/UltraGuiChannel.java`
- `docs/ultra-gui-protocol/reference/UltraGuiOpenPacket.java`
- `docs/ultra-gui-protocol/reference/UltraGuiActionPacket.java`

### 10. Third-party docs

`docs/KAIPATH_GUI_PROTOCOL.md` includes a **For other mod developers** section with channel
convention, field tables, button schema, verb contract, and a hello-world example.

---

## Overlay map

| Path | Role |
|------|------|
| `UltraGuiConsumerRegistry` | Multi-mod channel registry |
| `legacymechanics/*` | LM partner binding + dynamic screen |
| `docs/KAIPATH_GUI_PROTOCOL.md` | Full wire contract |
| `docs/ultra-gui-protocol/reference/` | Drop-in codecs for any mod |

Build: `bash tools/kaipath-lm-ultra-client/build.sh`  
Deploy: clients use `kaipath-1.3.1-lm-ultra.jar`; server mods keep their own `<modid>:gui`.
