# Ultra bridge gap matrix (LM ↔ KaiPath)

**LM 4.6.155** (live) + **KaiPath 1.3.17** (recommended). Official KaiPath updates are owned by the KaiPath mod developer.

**At a glance:** Bridge chrome gaps closed on **1.3.17** — `fixedFooter` pins `footer:true`; `portraitFrame` is advertised. LM wire already matched (`footer:true`); guitest tracks `FEATURE_FIXED_FOOTER` as shipped. JLDK one-pager: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).

Advertise set (1.3.17):

```text
style, activeTab, titleAlign, playerModel, textInput, headerBottom, accent,
palette, sound, buttonSkin, frame, pagedBook, portraitFrame, fixedFooter, combined.forms.v1
```

## Open bridge gaps (KaiPath next)

| Gap | Owner | LM status | KaiPath 1.3.17 | Notes |
|-----|-------|-----------|---------------|-------|
| **Title vertical centering** | **JLDK-only** | N/A (no field) | Confirm | Centers via `(headerBottom - 2 - 9) / 2`. Confirm gear-icon band alignment on all themes. No protocol field. |

`/lm admin guitest` on KaiPath **1.3.17**:

```text
missingShipped=none pendingNext=none
```

## Closed on KaiPath 1.3.17

| Gap | Notes |
|-----|-------|
| **`fixedFooter`** | Client pins `footer:true` buttons; auto spacing. LM emission unchanged. |
| **Advertise `portraitFrame`** | Was painted but not advertised on 1.3.13 — now advertised. |

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
| GuiFeaturesPacket advertise | **Done** on 1.3.17 (incl. `portraitFrame` + `fixedFooter`) |
| `sound` / `buttonSkin` / `frame` / `paged_book` | Client **Done** (1.3.13+). LM emits `frame` / `paged_book`; **does not** emit button `sound`/`buttonSkin` (skinned path = flat chrome) |
| `footer:true` / `fixedFooter` | **Done** (client 1.3.17 + LM emit 4.6.152+) |
| `playerModel.portraitFrame` advertise | **Done** (1.3.17) |

## Optional chrome (LM emit policy; not tracked as pending)

| Part | KaiPath 1.3.17 | LM |
|------|---------------|----|
| `sound` (`minecraft:ui.button.click`) | Honors (missing → silent) | **Not emitted** on buttons/tabs (default click) |
| `buttonSkin` (`legacymechanics:button`) | Honors (RL → boolean skinned paint) | **Not emitted** — skinned path draws flat squares |
| `layoutJson.frame` (`legacymechanics:panel`) | Honors (RL → boolean border) | **Emits** |
| `inputsJson` `type=paged_book` (advertise `pagedBook`) | Honors (`BookWidget`) | **Emits** (Staff Admin help book) |
| `playerModel.portraitFrame` | Honors + **advertised** | **Emits** when playerModel present |
| `layoutJson.palette` | Honors | Optional (accent already covers tint) |

## Pitfalls & traps (read before KaiPath release)

### 1. Server jar ≠ client render

`mods/kaipath-*.jar` on the **dedicated server** does nothing for Ultra painting. Players need the KaiPath **client** mod. Do not treat “server has 1.3.17” as “players have 1.3.17.”

### 2. DMZUltra-absent chrome RLs

`sound` / `buttonSkin` / `frame` / `portraitFrame` look like asset ids. On 1.3.13+:

- `sound` → registry Optional; missing SoundEvent → **silent no-op** (no crash)
- skin / frame / portraitFrame → `UltraBridgeChrome.id` only validates `ns:path`; result is a **boolean** that enables GuiGraphics tint paint — **no texture bind**

LM keeps `layoutJson.frame` / `portraitFrame` as `legacymechanics:*` enable tokens. Buttons omit `sound`/`buttonSkin` so KaiPath uses default chrome (buttonSkin would force the flat skinned path).

### 3. `pagedBook` vs `paged_book`

| Where | Spelling |
|-------|----------|
| GuiFeatures advertise | `pagedBook` (camelCase) |
| `inputsJson[].type` | `paged_book` (snake_case) |

Mismatched type string → book widget never mounts.

### 4. `contentBottom` / fixedFooter

Pre-1.3.17 hard-coded `contentBottom = panelY + panelH - 28` and ignored `footer:true`. **1.3.17** pins footer buttons and auto-calculates spacing. Do **not** invent server `contentTop`/`contentBottom`/`contentScroll` wire fields.

### 5. `titleTint` is not a wire field

Client derives title color from `palette` / `accent`. LM already sends those. Do not add `titleTint` to GuiOpen.

### 6. UTF-8 mojibake / tab ellipsis

LM Ultra copy prefers ASCII. Pre-fit labels with ASCII `...`. Stock KaiPath `fitLabel` uses ASCII `...` — keep it that way.

### 7. Pending vs shipped diagnostics

Missing `SHIPPED_ON_RECOMMENDED` → warn (wrong/old jar). Missing `PENDING_NEXT` → info (`pendingNext=`). On **1.3.17**, `pendingNext=none` (`FEATURE_FIXED_FOOTER` is shipped).

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
| Prestige GoD / Supreme Kai soft warn | Ultra + chat paths (4.6.155) |

## Cursor / LM (server) checklist

| Item | Status |
|------|--------|
| Emit `style` on every button | **Done** |
| Emit `inputsJson` field 11 | **Done** |
| Emit `playerModel.parts` schema 1 | **Done** |
| Emit `layoutJson.headerBottom` | **Done** |
| Emit `layoutJson.accent` / button `accent` | **Done** |
| Emit button `footer:true` | **Done** (4.6.152) — matches 1.3.17 |
| Emit `frame` / `paged_book` (not button `sound`/`buttonSkin`) | **Done** — omit buttonSkin (flat skinned chrome) |
| Chrome defaults safe without DMZUltra | **Done** (default button chrome; frame/portrait tokens) |
| Track `FEATURE_FIXED_FOOTER` as shipped on 1.3.17 | **Done** |
| Recommend KaiPath **1.3.17** | **Done** |
| `inputsJson` `slider` / `toggle` | **Deferred** |

## JLDK / KaiPath (client) checklist

| Item | 1.3.17 |
|------|--------|
| Button `style` | **Done** |
| `activeTab` | **Done** (accent tint) |
| `titleAlign` | **Done** |
| `playerModel` | **Done** |
| `textInput` | **Done** |
| `headerBottom` | **Done** |
| `accent` / palette chrome | **Done** |
| `sound` / `buttonSkin` / `frame` / `paged_book` | **Done** |
| Advertise `portraitFrame` + `fixedFooter` | **Done** |
| Honor `footer:true` / `fixedFooter` | **Done** |
| Title vertical center | Confirm gear-band alignment |
| `slider` / `toggle` widgets | When LM ships types |

## Reserved (later): `inputsJson` slider / toggle

Not implemented on either side. Prefer extending field 11 when added.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
- Overview: [`KAIPATH_1.3.1_ULTRA.md`](KAIPATH_1.3.1_ULTRA.md)
