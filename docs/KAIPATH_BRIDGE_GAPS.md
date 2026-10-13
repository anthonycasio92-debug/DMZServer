# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.123** + **KaiPath 1.3.9** (live / recommended). Official KaiPath updates are owned by the KaiPath mod developer.

## Still missing on KaiPath 1.3.9

None for the Ultra Weights / Config typed-entry path. Stock **1.3.9** advertises and honors:

| Part | Status |
|------|--------|
| GuiOpen **field 11** `inputsJson` | **Done** |
| EditBox / `type=text` + `bindInput` → `args.value` | **Done** |
| `textInput` in GuiFeaturesPacket advertise | **Done** |
| `style` / `activeTab` / `titleAlign` / `playerModel` | **Done** (since 1.3.7) |

LM 4.6.123+ treats those five as `SHIPPED_ON_RECOMMENDED`. `/lm admin guitest` should report `missingShipped=none pendingNext=none` on 1.3.9.

Deferred (neither side): `inputsJson` `slider` / `toggle`.

## Cursor / LM (server protocol) — status

| Gap | Status | Notes |
|-----|--------|-------|
| Button `style` on every button | **Done** | Wire always emits `style`. Roles: `confirm` / `danger` / `dim` / `normal`. |
| Text input (`inputsJson` field 11) | **Done** | `type=text` + `args.bindInput` → `args.value`. Config + Difficulty Weights shop. |
| `playerModel.parts` (cosmetics) | **Done** (4.6.111) | Schema 1: `headBones` / `form` / `extras`. See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md). |
| Slider / toggle as input types | **Deferred** | BOOLEAN stays styled buttons; NUMBER uses text + ±. Reserved below. |

## JLDK / KaiPath (client) — 1.3.9

Vendored / live jar: `mods/kaipath-1.3.9.jar`. Reference kit remains at [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/) for protocol parity.

| Gap | KaiPath **1.3.9** | Notes |
|-----|-------------------|-------|
| Button `style` colors | **Done** | |
| `activeTab` highlight | **Done** | |
| `titleAlign` | **Done** | |
| `playerModel` draw | **Done** | |
| Text field widgets (field 11) | **Done** | Unblocks Weight shop |
| GuiFeaturesPacket advertise | **Full** | `["style","activeTab","titleAlign","playerModel","textInput"]` |
| Slider / toggle widgets | N/A | Reserved until LM ships types |

## Practical pitfalls (watch-outs)

### 1. Older clients without textInput

Pre-1.3.9 KaiPath ignores field 11. LM Weights / Config pages show an explicit danger line when `textInput` is missing — do not leave a silent dead field.

### 2. UTF-8 mojibake

Fancy punctuation can still show as `â€` on some clients. LM Ultra copy prefers ASCII dashes.

### 3. Player model custom parts

`playerModel.parts` schema v1 is LM-side; verify all part types render on the client preview.

### 4. Limited graceful degradation

`GuiFeaturesPacket` drives guitest / gap logs and Weights/Config copy; LM does not yet hide every optional chrome feature-by-feature.

### 5. Slider / toggle types are deferred

Booleans stay styled buttons; numbers stay text + ± steppers.

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. When added, prefer extending field 11 rather than a new packet field.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
