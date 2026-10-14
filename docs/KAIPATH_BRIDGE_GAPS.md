# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.151** + **KaiPath 1.3.9** (live / recommended). Official KaiPath updates are owned by the KaiPath mod developer.

## Open bridge gaps (only these)

| Gap | Owner | LM status | KaiPath 1.3.9 | Notes |
|-----|-------|-----------|----------------|-------|
| **`layoutJson.headerBottom`** | Cursor + JLDK | **Emits** (4.6.149+) | **Missing** | Keep title + yellow divider **strictly above** this panel-relative Y. With `tabsUnderTitle=true`, tab row sits at this Y (default **28**). Spec: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) §4. |
| **`layoutJson.accent` + button/tab `accent`** | Cursor + JLDK | **Emits** (4.6.149+) | **Missing** | Single-letter color codes. Paint title + `activeTab` highlight from `layoutJson.accent`; hub tiles / staff controls from per-button `accent`. Titles/labels stay plain ASCII. |
| **Title vertical centering** | **JLDK-only** | N/A (no field) | **Missing** | Center title text in the header band between the gear icons. No protocol change. |

`/lm admin guitest` on KaiPath **1.3.9** should report:

```text
missingShipped=none pendingNext=[headerBottom, accent]
```

(`UltraClientFeatures.PENDING_NEXT` tracks the two advertise-able gaps. Title centering is docs-only.)

## Closed on both sides (not gaps)

| Part | Status |
|------|--------|
| GuiOpen fields 1–11 (`inputsJson`) | **Done** |
| Button `style` (`confirm` / `danger` / `dim` / `normal`) | **Done** (1.3.7+) |
| `activeTab` highlight | **Done** (shared yellow until `accent` ships) |
| `layoutJson.titleAlign` | **Done** |
| `layoutJson.playerModel` (+ `parts` schema 1) | **Done** |
| `textInput` / EditBox / `bindInput` → `args.value` | **Done** (1.3.9) |
| GuiFeaturesPacket advertise (5 features) | **Done** on 1.3.9 |

## Not bridge gaps (LM-only, already shipped)

| Item | Notes |
|------|-------|
| Hub tile styles | All `normal` + per-system `accent` letter; Staff Admin accent `4` (not `STYLE_DANGER`) |
| Hub compact floor | `PANEL_COMPACT_MIN_H=150` for tile-only menus |
| Active-tab style | LM sends `style=normal` + `accent` (not shared `confirm` yellow) |
| Main hub title gold | Hub omits `layoutJson.accent` (shared chrome) |

## Cursor / LM (server) checklist

| Item | Status |
|------|--------|
| Emit `style` on every button | **Done** |
| Emit `inputsJson` field 11 | **Done** |
| Emit `playerModel.parts` schema 1 | **Done** |
| Emit `layoutJson.headerBottom` | **Done** (4.6.149) |
| Emit `layoutJson.accent` / button `accent` | **Done** (4.6.149) |
| Track pending in `UltraClientFeatures.PENDING_NEXT` | **Done** (4.6.151) — `headerBottom`, `accent` |
| `inputsJson` `slider` / `toggle` | **Deferred** |

## JLDK / KaiPath (client) checklist

Vendored / live jar: `mods/kaipath-1.3.9.jar`. Reference kit: [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/).

| Item | 1.3.9 | Next KaiPath (kit ready) |
|------|-------|---------------|
| Button `style` | **Done** | — |
| `activeTab` | **Done** (yellow) | Tint from `accent` |
| `titleAlign` | **Done** | — |
| `playerModel` | **Done** | — |
| `textInput` | **Done** | — |
| Advertise 5 features | **Done** | Add `headerBottom`, `accent` (**done in kit**) |
| Honor `headerBottom` | **Missing** | **Done in kit** — Title+divider above Y |
| Honor `accent` | **Missing** | **Done in kit** — Title / activeTab / hub tint |
| Title vertical center | **Missing** | **Done in kit** — No new field |
| `slider` / `toggle` widgets | N/A | When LM ships types |

### Advertise when headerBottom + accent ship

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\",\"headerBottom\",\"accent\"]");
```

## Practical pitfalls

### 1. Older clients without textInput

Pre-1.3.9 KaiPath ignores field 11. LM Weights / Config pages show an explicit danger line when `textInput` is missing.

### 2. UTF-8 mojibake / tab ellipsis

LM Ultra copy prefers ASCII. Pre-fit labels with ASCII `...` so stock KaiPath `fitLabel` never appends mojibaked ellipsis. JLDK: change client `fitLabel` to ASCII `...`.

### 3. Player model custom parts

`playerModel.parts` schema v1 is LM-side; verify part types render on the client preview.

### 4. Pending vs shipped diagnostics

Missing `SHIPPED_ON_RECOMMENDED` → warn (wrong/old jar). Missing `PENDING_NEXT` → info (`pendingNext=` in guitest). Do not treat 1.3.9 as broken for headerBottom/accent until a newer KaiPath advertises them.

### 5. Slider / toggle types are deferred

Booleans stay styled buttons; numbers stay text + ± steppers.

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. Prefer extending field 11 when added.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
- Overview: [`KAIPATH_1.3.1_ULTRA.md`](KAIPATH_1.3.1_ULTRA.md)
