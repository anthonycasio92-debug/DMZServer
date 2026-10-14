# Ultra bridge gap matrix (any partner mod ↔ KaiPath)

**Protocol is multi-mod** (`{modId}:gui`). Any Forge mod can pull
[`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/) and register the same wire
format. Official KaiPath updates are owned by the KaiPath mod developer.

DMZUltra asset reuse (client-only): [`DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md).

## Open bridge gaps — near-term (kit / next KaiPath)

| Gap | Owner | Server emit | KaiPath 1.3.9 | Notes |
|-----|-------|-------------|----------------|-------|
| **`layoutJson.headerBottom`** | Cursor + JLDK | **Yes** (partners) | **Missing** | Title + yellow divider strictly above this Y. Default **28** with under-title tabs, **42** without. |
| **Letter `accent`** | Cursor + JLDK | **Yes** (partners) | **Missing** | Compat letter codes. Prefer generic **`palette`** for new work. |
| **Title vertical centering** | **JLDK-only** | N/A | **Missing** | Center title in header band. No protocol field. |
| **ASCII `fitLabel`** | JLDK | N/A | **Broken** (mojibake `…`) | Kit patches overlay + documents source fix. |

On KaiPath **1.3.9**, partner guitest typically reports:

```text
missingShipped=none pendingNext=[headerBottom, accent]
```

## Proposed gaps — DMZUltra chrome reuse (mod-agnostic)

These fields are **specified** in [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md)
§ Proposed chrome. No partner may hardcode system names into the protocol.

| Gap | Wire field | Example values | Advertise id |
|-----|------------|----------------|--------------|
| Generic palette names | `layoutJson.palette` + button/tab `palette` | `gold`, `purple`, `dark_red`, … | `palette` |
| Click / tick sounds | button/layout/input `sound` | `dmzultra:ui_confirm`, `dmzultra:scene_tick` | `sound` |
| Button texture skins | button `buttonSkin` | `dmzultra:arena` or state map | `buttonSkin` |
| Panel chrome | `layoutJson.frame` | `dmzultra:card` or map | `frame` |
| Portrait chrome | `playerModel.portraitFrame` | `dmzultra:portrait` | (under `playerModel`) |
| Paged book widget | `inputsJson` `type=paged_book` | pages[] with title/lines | `pagedBook` |

**Open question for JLDK:** which `UltraUi$Theme` / `$Palette` values are hardcoded vs
data-driven? Answer before expanding the palette name list.

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
| Emit letter `accent` / document `palette` | accent **Done**; palette **spec only** |
| Spec `sound` / `buttonSkin` / `frame` / `paged_book` | **Done** (docs + fixture) |
| Emit chrome fields from production partners | **Not yet** — wait for KaiPath advertise |
| `inputsJson` `slider` / `toggle` | **Deferred** |

## JLDK / KaiPath checklist

Vendored / live jar: `mods/kaipath-1.3.9.jar`. Reference kit: [`tools/ultra-gui-kit/`](../tools/ultra-gui-kit/).

| Item | 1.3.9 | Next / later |
|------|-------|--------------|
| Honor `headerBottom` | Missing | **Done in kit** |
| Honor letter `accent` | Missing | **Done in kit** |
| Title vertical center | Missing | **Done in kit** |
| ASCII `fitLabel` | Broken | **Patched in kit overlay** |
| Honor `palette` / `sound` / `buttonSkin` / `frame` / `paged_book` | — | **Implement from protocol** |
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
