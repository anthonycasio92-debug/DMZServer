# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.154** (live) + **KaiPath 1.3.13** (live / recommended). Official KaiPath updates are owned by the KaiPath mod developer.

**At a glance — KaiPath next:** (1) honor `footer:true` + advertise `fixedFooter`, (2) advertise `portraitFrame`, (3) read § Pitfalls before release. JLDK one-pager: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).

Pulled live jar: `mods/kaipath-1.3.13.jar`. Advertise set:

```text
style, activeTab, titleAlign, playerModel, textInput, headerBottom, accent,
palette, sound, buttonSkin, frame, pagedBook, combined.forms.v1
```

## Open bridge gaps (KaiPath next)

| Gap | Owner | LM status | KaiPath 1.3.13 | Notes |
|-----|-------|-----------|-----------------|-------|
| **`button footer:true` / `fixedFooter`** | **JLDK** | **Emits** (4.6.152+) | **Missing** | Pin Back + Main menu (or contextual Back) in fixed chrome so they do not scroll with body. Today client `contentBottom = panelY + panelH - 28` and ignores `footer`. Advertise: `fixedFooter`. Spec: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md). |
| **Advertise `portraitFrame`** | **JLDK** | **Emits** when `playerModel` present | Honors but **not advertised** | Guitest/diagnostics treat missing advertise as unsupported even though paint works. Add `"portraitFrame"` to GuiFeatures. |
| **Title vertical centering** | **JLDK-only** | N/A (no field) | Partial | 1.3.13 already centers vertically in the header band via `(headerBottom - 2 - 9) / 2`. Confirm gear-icon band alignment on all themes. |

`/lm admin guitest` on KaiPath **1.3.13** + LM **4.6.154**:

```text
missingShipped=none pendingNext=[fixedFooter]
```

## KaiPath handoff checklist (do these)

1. **Honor `footer:true`** — buttons with `"footer": true` stay pinned at the panel bottom; scrollable body/`contentBottom` ends above that band. Advertise `fixedFooter`.
2. **Advertise `portraitFrame`** alongside existing chrome features (already painted when present).
3. Keep advertise camelCase **`pagedBook`**; wire widget type stays snake_case **`paged_book`**.
4. When implementing fixedFooter, set `contentBottom` from footer band height (not hard-coded `panelH - 28`).

### Advertise when fixedFooter + portraitFrame ship

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\",\"textInput\","
        + "\"headerBottom\",\"accent\",\"palette\",\"sound\",\"buttonSkin\","
        + "\"frame\",\"pagedBook\",\"portraitFrame\",\"fixedFooter\",\"combined.forms.v1\"]");
```

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
| `sound` / `buttonSkin` / `frame` / `paged_book` | **Done** (client 1.3.13 + LM 4.6.154 emit) |

## Optional chrome (emitted by LM 4.6.154; not tracked as pending)

| Part | KaiPath 1.3.13 | LM 4.6.154 |
|------|----------------|------------|
| `sound` (`minecraft:ui.button.click`) | Honors (missing → silent) | **Emits** on buttons/tabs |
| `buttonSkin` (`legacymechanics:button`) | Honors (RL → boolean paint) | **Emits** on buttons/tabs |
| `layoutJson.frame` (`legacymechanics:panel`) | Honors (RL → boolean border) | **Emits** |
| `inputsJson` `type=paged_book` (advertise `pagedBook`) | Honors (`BookWidget`) | **Emits** (Staff Admin help book) |
| `playerModel.portraitFrame` | Honors (**not advertised** — JLDK) | **Emits** when playerModel present |
| `layoutJson.palette` | Honors | Optional (accent already covers tint) |

## Pitfalls & traps (read before KaiPath release)

### 1. Server jar ≠ client render

`mods/kaipath-*.jar` on the **dedicated server** does nothing for Ultra painting. Players need the KaiPath **client** mod. Do not treat “server has 1.3.13” as “players have 1.3.13.”

### 2. DMZUltra-absent chrome RLs

`sound` / `buttonSkin` / `frame` / `portraitFrame` look like asset ids. On 1.3.13:

- `sound` → registry Optional; missing SoundEvent → **silent no-op** (no crash)
- skin / frame / portraitFrame → `UltraBridgeChrome.id` only validates `ns:path`; result is a **boolean** that enables GuiGraphics tint paint — **no texture bind**

LM 4.6.154 uses `minecraft:ui.button.click` + `legacymechanics:*` enable tokens so KaiPath-only clients stay healthy.

### 3. `pagedBook` vs `paged_book`

| Where | Spelling |
|-------|----------|
| GuiFeatures advertise | `pagedBook` (camelCase) |
| `inputsJson[].type` | `paged_book` (snake_case) |

Mismatched type string → book widget never mounts.

### 4. `contentBottom` hard-code fights dual footer

1.3.13 sets `contentBottom = panelY + panelH - 28` and ignores `footer:true`. LM dual footer (Back + Main menu) scrolls with the body until `fixedFooter` lands. Do **not** invent server `contentTop`/`contentBottom`/`contentScroll` wire fields — fix the client band.

### 5. `titleTint` is not a wire field

Client derives title color from `palette` / `accent`. LM already sends those. Do not add `titleTint` to GuiOpen.

### 6. UTF-8 mojibake / tab ellipsis

LM Ultra copy prefers ASCII. Pre-fit labels with ASCII `...`. Stock KaiPath `fitLabel` on 1.3.13 uses ASCII `...` — keep it that way.

### 7. Pending vs shipped diagnostics

Missing `SHIPPED_ON_RECOMMENDED` → warn (wrong/old jar). Missing `PENDING_NEXT` → info (`pendingNext=`). On **1.3.13** + LM **4.6.154**, only `fixedFooter` is pending.

### 8. Older clients without textInput

Pre-1.3.9 ignores field 11. LM Weights / Config show an explicit danger line when `textInput` is missing.

### 9. Player model custom parts

`playerModel.parts` schema v1 is LM-side; verify part types render on the client preview.

### 10. Slider / toggle deferred

Booleans stay styled buttons; numbers stay text + ± steppers. Not a KaiPath blocker.

## Not bridge gaps (LM-only, already shipped)

| Item | Notes |
|------|-------|
| Hub tile styles | All `normal` + per-system `accent` letter; Staff Admin accent `4` |
| Hub compact floor | `PANEL_COMPACT_MIN_H=150` for tile-only menus |
| Active-tab style | LM sends `style=normal` + `accent` |
| Main hub title gold | Hub omits `layoutJson.accent` (shared chrome) |
| Prestige GoD / Supreme Kai soft warn | Ultra + chat paths (4.6.154) |

## Cursor / LM (server) checklist

| Item | Status |
|------|--------|
| Emit `style` on every button | **Done** |
| Emit `inputsJson` field 11 | **Done** |
| Emit `playerModel.parts` schema 1 | **Done** |
| Emit `layoutJson.headerBottom` | **Done** |
| Emit `layoutJson.accent` / button `accent` | **Done** |
| Emit button `footer:true` | **Done** (4.6.152) |
| Emit sound / buttonSkin / frame / paged_book | **Done** (4.6.154) |
| Chrome defaults safe without DMZUltra | **Done** (4.6.154) |
| Track pending `fixedFooter` only | **Done** |
| Recommend KaiPath **1.3.13** | **Done** |
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
| `sound` / `buttonSkin` / `frame` / `paged_book` | **Done** | — |
| Advertise shipped set | **Done** | Add `fixedFooter`, `portraitFrame` |
| Honor `footer:true` / `fixedFooter` | **Needed** | Pin footer; raise content band |
| Advertise `portraitFrame` | **Needed** | Already paints |
| Title vertical center | Confirm | Gear-band alignment |
| `slider` / `toggle` widgets | N/A | When LM ships types |

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. Prefer extending field 11 when added.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
- Overview: [`KAIPATH_1.3.1_ULTRA.md`](KAIPATH_1.3.1_ULTRA.md)
