# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.153** + **KaiPath 1.3.13** (live / recommended). Official KaiPath updates are owned by the KaiPath mod developer.

Pulled live jar: `mods/kaipath-1.3.13.jar`. Advertise set:

```text
style, activeTab, titleAlign, playerModel, textInput, headerBottom, accent,
palette, sound, buttonSkin, frame, pagedBook, combined.forms.v1
```

## Open bridge gaps (only these)

| Gap | Owner | LM status | KaiPath 1.3.13 | Notes |
|-----|-------|-----------|-----------------|-------|
| **`button footer:true` / `fixedFooter`** | Cursor + JLDK | **Emits** (4.6.152+) | **Missing** | Pin Back + Main menu (or contextual Back) in fixed chrome so they do not scroll with body. Spec: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md). Advertise: `fixedFooter`. |
| **Title vertical centering** | **JLDK-only** | N/A (no field) | **Missing** | Center title text in the header band between the gear icons. No protocol change. |

`/lm admin guitest` on KaiPath **1.3.13** should report:

```text
missingShipped=none pendingNext=[fixedFooter]
```

(`UltraClientFeatures.PENDING_NEXT` tracks `fixedFooter`. Title centering is docs-only.)

## Closed on both sides (not gaps)

| Part | Status |
|------|--------|
| GuiOpen fields 1–11 (`inputsJson`) | **Done** |
| Button `style` (`confirm` / `danger` / `dim` / `normal`) | **Done** (1.3.7+) |
| `activeTab` highlight | **Done** (tint from `accent` on 1.3.13) |
| `layoutJson.titleAlign` | **Done** |
| `layoutJson.playerModel` (+ `parts` schema 1) | **Done** |
| `textInput` / EditBox / `bindInput` → `args.value` | **Done** (1.3.9+) |
| `layoutJson.headerBottom` / `tabsUnderTitle` | **Done** (1.3.13) |
| `layoutJson.accent` + button/tab `accent` | **Done** (1.3.13 via `UltraBridgeChrome`) |
| GuiFeaturesPacket advertise | **Done** on 1.3.13 (12+ tokens incl. chrome extras) |

## Optional chrome (emitted by LM 4.6.153; not tracked as pending)

| Part | KaiPath 1.3.13 | LM 4.6.153 |
|------|----------------|------------|
| `sound` (`dmzultra:ui_confirm`) | Honors | **Emits** on buttons/tabs |
| `buttonSkin` (`dmzultra:button`) | Honors | **Emits** on buttons/tabs |
| `layoutJson.frame` (`dmzultra:panel`) | Honors | **Emits** |
| `inputsJson` `type=paged_book` (advertise `pagedBook`) | Honors (`BookWidget`) | **Emits** (Staff Admin help book) |
| `playerModel.portraitFrame` | Honors (not advertised) | **Emits** when playerModel present |
| `layoutJson.palette` | Honors | Optional (accent already covers tint) |

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
| Emit button `footer:true` | **Done** (4.6.152) |
| Track pending in `UltraClientFeatures.PENDING_NEXT` | **Done** (4.6.153) — `fixedFooter` only |
| Recommend KaiPath **1.3.13** | **Done** (4.6.153) |
| `inputsJson` `slider` / `toggle` | **Deferred** |

## JLDK / KaiPath (client) checklist

Vendored / live jar: `mods/kaipath-1.3.13.jar`. Reference kit: [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/).

| Item | 1.3.13 | Next KaiPath |
|------|--------|---------------|
| Button `style` | **Done** | — |
| `activeTab` | **Done** (accent tint) | — |
| `titleAlign` | **Done** | — |
| `playerModel` | **Done** | — |
| `textInput` | **Done** | — |
| `headerBottom` | **Done** | — |
| `accent` / palette chrome | **Done** | — |
| Advertise shipped set | **Done** | Add `fixedFooter` |
| Honor `footer:true` / `fixedFooter` | **Needed** | Pin footer buttons |
| Title vertical center | **Needed** | No new field |
| `slider` / `toggle` widgets | N/A | When LM ships types |

### Advertise when fixedFooter ships

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\","
        + "\"headerBottom\",\"accent\",\"palette\",\"sound\",\"buttonSkin\","
        + "\"frame\",\"pagedBook\",\"fixedFooter\",\"combined.forms.v1\"]");
```

## Practical pitfalls

### 1. Older clients without textInput

Pre-1.3.9 KaiPath ignores field 11. LM Weights / Config pages show an explicit danger line when `textInput` is missing.

### 2. UTF-8 mojibake / tab ellipsis

LM Ultra copy prefers ASCII. Pre-fit labels with ASCII `...` so stock KaiPath `fitLabel` never appends mojibaked ellipsis. JLDK: change client `fitLabel` to ASCII `...` (present on 1.3.13).

### 3. Player model custom parts

`playerModel.parts` schema v1 is LM-side; verify part types render on the client preview.

### 4. Pending vs shipped diagnostics

Missing `SHIPPED_ON_RECOMMENDED` → warn (wrong/old jar). Missing `PENDING_NEXT` → info (`pendingNext=` in guitest). On **1.3.13**, only `fixedFooter` is pending; pre-1.3.13 clients missing `headerBottom`/`accent` warn as shipped gaps.

### 5. Slider / toggle types are deferred

Booleans stay styled buttons; numbers stay text + ± steppers.

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. Prefer extending field 11 when added.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
- Overview: [`KAIPATH_1.3.1_ULTRA.md`](KAIPATH_1.3.1_ULTRA.md)
