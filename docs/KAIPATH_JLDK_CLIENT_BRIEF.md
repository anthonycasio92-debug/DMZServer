# JLDK brief — KaiPath Ultra GUI client (multi-mod)

**Status:** implement from source in this repo — **no patched KaiPath jar is shipped.**

| Kit | Path |
|-----|------|
| KaiPath client sources (merge into KaiPath) | [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/) |
| Server codecs for any mod author | [`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/) |
| Kit README | [`tools/ultra-gui-kit/README.md`](../tools/ultra-gui-kit/README.md) |
| Wire contract | [`docs/KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) |

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

```java
UltraGuiConsumerRegistry.registerClientChannel("legacymechanics");
UltraGuiConsumerRegistry.registerClientChannel("mymod");
```

Reference: `tools/ultra-gui-kit/kaipath-client/.../UltraGuiConsumerRegistry.java`.

### 9. Reference implementation (server mods)

Copy from `tools/ultra-gui-kit/server/` (also mirrored in `docs/ultra-gui-protocol/reference/`):

- `UltraGuiChannel.java`
- `UltraGuiOpenPacket.java`
- `UltraGuiActionPacket.java`
- `HelloWorldServerSketch.java` (comments only)

### 10. Third-party docs

`docs/KAIPATH_GUI_PROTOCOL.md` — **For other mod developers** (channel, fields, buttons, verbs, hello-world).

---

## How to merge (KaiPath)

1. Copy `tools/ultra-gui-kit/kaipath-client/com/dmzlegacy/kaipath/ultra/consumers/` into the KaiPath sources.
2. Ensure client bootstrap still calls `LmUltraConsumer.bootstrap()` (or equivalent).
3. For each partner mod: `UltraGuiConsumerRegistry.registerClientChannel("<modid>")`.
4. Ship an official KaiPath release — players should **not** need a DMZServer overlay jar.
