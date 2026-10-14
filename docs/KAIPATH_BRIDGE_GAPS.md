# Ultra bridge gap matrix (any partner mod ↔ KaiPath)

**Protocol is multi-mod** (`{modId}:gui`). Any Forge mod can pull
[`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/) and register the same wire
format. Official KaiPath updates are owned by the KaiPath mod developer.

DMZUltra asset reuse (client-only): [`DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md).

## Open bridge gaps — near-term (kit / next KaiPath)

| Gap | Owner | Server emit | KaiPath 1.3.9 | Kit | Notes |
|-----|-------|-------------|----------------|-----|-------|
| **`layoutJson.headerBottom`** | Cursor + JLDK | **Yes** | **Missing** | **Done** | Title + yellow divider strictly above this Y. |
| **Letter `accent`** | Cursor + JLDK | **Yes** | **Missing** | **Done** | Compat letter codes. Prefer **`palette`**. |
| **`palette`** | Cursor + JLDK | **Yes** (LM 4.6.152) | **Missing** | **Done** | Generic color names on layout/buttons/tabs. |
| **Title vertical centering** | **JLDK-only** | N/A | **Missing** | **Done** | Center title in header band. No protocol field. |
| **ASCII `fitLabel`** | JLDK | N/A | **Broken** | **Patched** | Overlay + source docs. |

On stock KaiPath **1.3.9**, partner guitest typically reports:

```text
missingShipped=none pendingNext=[headerBottom, accent, palette]
```

After merging this kit into the next official KaiPath jar: `pendingNext=none` for those three.

## Chrome gaps — kit-honored, texture blit still JLDK

Wire fields from [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) § Proposed chrome.
Kit advertises + paints chrome rects / plays sounds; full DMZUltra texture mapping stays
KaiPath-owned.

| Gap | Wire field | Advertise id | Kit status |
|-----|------------|--------------|------------|
| Generic palette names | `layoutJson.palette` + button/tab `palette` | `palette` | **Honored** (ARGB map) |
| Click / tick sounds | button/input `sound` | `sound` | **Honored** (reflection play) |
| Button texture skins | button `buttonSkin` | `buttonSkin` | Chrome cue (texture blit TBD) |
| Panel chrome | `layoutJson.frame` | `frame` | Edge rects (texture blit TBD) |
| Portrait chrome | `playerModel.portraitFrame` | (under `playerModel`) | Edge rects (texture blit TBD) |
| Paged book widget | `inputsJson` `type=paged_book` | `pagedBook` | **Honored** (Prev/Next) |

**Answered from live `dmzultra-0.2.9`:** `UltraUi$Palette` = data `(accent,second,tint)`;
`UltraUi$Theme` = hardcoded feature enum. Keep wire `palette` as generic color names;
do not require Theme enum names for other mods. JLDK still chooses the name→RGB map.

**Reference fixture (not a production partner):**
[`tools/ultra-gui-kit/server/BridgeFutureFieldsFixture.json`](../tools/ultra-gui-kit/server/BridgeFutureFieldsFixture.json).

## Closed on both sides (not gaps)

| Part | Status |
|------|--------|
| GuiOpen fields 1–11 (`inputsJson`) | **Done** |
| Button `style` (`confirm` / `danger` / `dim` / `normal`) | **Done** (1.3.7+) |
| `activeTab` highlight | **Done** (shared yellow until `palette`/`accent` ships) |
| `layoutJson.titleAlign` | **Done** |
| `layoutJson.playerModel` (+ `parts` schema 1) | **Done** |
| `textInput` / EditBox / `bindInput` → `args.value` | **Done** (1.3.9) |
| GuiFeaturesPacket advertise (5 features) | **Done** on 1.3.9 |
| Multi-mod `{modId}:gui` + `UltraGuiPartners` | **Done** in kit |

## Cursor / server checklist

| Item | Status |
|------|--------|
| Emit `style` on every button | **Done** (partners) |
| Emit `inputsJson` field 11 | **Done** |
| Emit `playerModel.parts` schema 1 | **Done** |
| Emit `layoutJson.headerBottom` | **Done** |
| Emit letter `accent` / `palette` | **Done** (LM 4.6.152) |
| Spec `sound` / `buttonSkin` / `frame` / `paged_book` | **Done** (docs + fixture) |
| Emit `portraitFrame` on playerModel | **Done** (LM 4.6.152) |
| Emit `sound` / `buttonSkin` / `frame` / `paged_book` from production | **Not yet** — safe after KaiPath advertise |
| `inputsJson` `slider` / `toggle` | **Deferred** |

## JLDK / KaiPath checklist

Vendored / live jar: `mods/kaipath-1.3.9.jar`. Reference kit: [`tools/ultra-gui-kit/`](../tools/ultra-gui-kit/).

| Item | 1.3.9 | Kit / next |
|------|-------|------------|
| Honor `headerBottom` | Missing | **Done in kit** |
| Honor letter `accent` | Missing | **Done in kit** |
| Title vertical center | Missing | **Done in kit** |
| ASCII `fitLabel` | Broken | **Patched in kit overlay** |
| Honor `palette` / `sound` / `buttonSkin` / `frame` / `paged_book` | — | **Done in kit** (texture blit TBD) |
| Register all `UltraGuiPartners` | LM-oriented | **Multi-mod in kit** |

## Practical pitfalls

### 1. Older clients without textInput

Pre-1.3.9 KaiPath ignores field 11. Partners should degrade gracefully when `textInput` is
missing from the feature advertise set.

### 2. UTF-8 mojibake / tab ellipsis (weird A/E glyphs)

Stock `UltraWidgetReflect.fitLabel` appends mojibaked `â€¦`. Section-sign label prefixes
cause the same glitch. Fix: ASCII `...` + plain labels. See
[`FITLABEL_ASCII.md`](../tools/ultra-gui-kit/kaipath-client/com/dmzlegacy/kaipath/ultra/internal/FITLABEL_ASCII.md).

### 3. Never pull DMZUltra assets into a server jar

Assets stay client-side. Bridge fields carry namespaced ids only. Do not subclass DMZUltra
screens. Protocol lives in KaiPath, not in `dmzultra-0.2.9.jar`.

### 4. Pending vs shipped diagnostics

Missing shipped features → warn. Missing proposed chrome features → info until advertised.
Do not treat 1.3.9 as broken for `palette`/`sound`/… until a newer KaiPath advertises them.

### 5. Slider / toggle types are deferred

Booleans stay styled buttons; numbers stay text + ± steppers.

## Docs

- Protocol: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
- Asset reuse: [`DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md)
- JLDK handoff: [`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md)
- Overview: [`KAIPATH_1.3.1_ULTRA.md`](KAIPATH_1.3.1_ULTRA.md)
