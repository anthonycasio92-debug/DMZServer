# Ultra GUI protocol (KaiPath)

**Audience:** any Forge mod that wants KaiPath to render server-driven menus — not only
LegacyMechanics (LM).

| | |
|--|--|
| Channel convention | `<modid>:gui` (e.g. `legacymechanics:gui`, `mymod:gui`) |
| Forge protocol string | `"1"` (`UltraGuiProtocol.PROTOCOL_VERSION`) |
| Message 0 | GuiOpenPacket (server → client) |
| Message 1 | GuiActionPacket (client → server) |
| Message 2 | GuiFeaturesPacket (client → server, **optional**) |
| Client renderer | KaiPath (dynamic — renders the packet, no hardcoded menus) |

**Principle:** the client renders what the server sends. Packet codecs must not embed a
channel namespace or mod id. New menus are added on the **server mod** side only. Optional
fields (`style`, `activeTab`) are **data**, not new hardcoded screens.

**KaiPath 1.3.9** (live / recommended): decodes GuiOpen fields 1–11, honors `style`,
`titleAlign`, `activeTab`, `playerModel`, and `textInput`, and advertises those five via
`GuiFeaturesPacket` on each registered `{modId}:gui` channel. **Still missing on 1.3.9:**
`headerBottom` + `accent` (plus title vertical centering) — reference kit
(`tools/ultra-gui-kit/kaipath-client/`) has the merge for **all** partner mods, not only LM.
Server mods pull `tools/ultra-gui-kit/server/` and own their own channel namespace.

Implement-yourself kit: [`tools/ultra-gui-kit/`](../tools/ultra-gui-kit/).
JLDK handoff: [`docs/KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).
DMZUltra asset reuse (client-only; bridge fields only):
[`docs/DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md).

**Mod-agnostic rule:** new field names, enum values, defaults, and examples must not
hardcode any single partner mod. Partners map their domains to generic values
(`palette: "purple"`, `sound: "dmzultra:ui_confirm"`, …).

---

## Client conformance contract (JLDK)

The client **must** render what the server sends (no hardcoded screen/tab/button lists).

| Capability | Required since | **1.3.9** released | Reference kit |
|------------|----------------|--------------------|---------------|
| Decode GuiOpen fields 1–10 | 1.3.1 | yes | yes |
| Dynamic title / tabs / lines / buttons | 1.3.1 | yes | yes |
| Verbatim GuiAction echo | 1.3.1 | yes | yes |
| Any `screenId` (no whitelist) | 1.3.1 | yes | yes |
| Honor **`layoutJson.titleAlign`** | 1.3.1+ | **yes** | yes |
| Honor **`activeTab`** highlight | 1.3.1+ | **yes** | yes |
| Honor button **`style`** | 1.3.7 | **yes** | yes |
| Honor **`layoutJson.playerModel`** (+ optional `parts`) | 1.3.7 | **yes** (entity) | yes (+ parts hook) |
| Honor **`inputsJson`** text fields (field 11) | 1.3.9 | **yes** | yes |
| Honor **`layoutJson.headerBottom`** | next | **no** | **yes** |
| Honor **`palette` / letter `accent`** | next | **no** | accent **yes**; palette proposed |
| Honor **`sound` / `buttonSkin` / `frame` / `paged_book`** | later | **no** | proposed — see § Proposed chrome |
| Decode GuiOpen strings as **UTF-8** | 1.3.1 | MC `readUtf` | **yes** (explicit) |
| Send **GuiFeaturesPacket** | 1.3.7 | **yes** (5 features) | **yes** (+ `headerBottom`/`accent`) |
| `inputsJson` `slider` / `toggle` | later | — | reserved — see [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) |

### Button JSON schema

Every button object in `buttonsJson`:

| Field | Required | Type | Notes |
|-------|----------|------|--------|
| `id` | yes | string | Action id (echoed verbatim on click) |
| `label` | yes | string | Display text — prefer **plain ASCII** (no section-sign) |
| `style` | no | string | **Semantic** role — see table below (default `normal`) |
| `palette` | no | string | **Decorative** generic color name (`gold`, `purple`, …). Prefer over letter `accent` for new work. |
| `accent` | no | string | Legacy single-letter Minecraft color code (`c`, `5`, …). Ignored when `palette` is set. |
| `sound` | no | string | Namespaced sound id played on click (e.g. `dmzultra:ui_confirm`). **Proposed.** |
| `buttonSkin` | no | string or object | Namespaced skin id or state map. **Proposed.** |
| `x` | yes | number | Panel-relative X |
| `y` | yes | number | Panel-relative Y |
| `w` | yes | number | Width (minimum **80** — never shrink for long labels) |
| `h` | yes | number | Height |
| `pad` | no | number | Horizontal label padding (default **8**) |
| `args` | recommended | object | Opaque JSON echoed to the server |

```json
{
  "id": "@myshop:do:buy",
  "label": "Buy",
  "style": "confirm",
  "palette": "gold",
  "sound": "dmzultra:ui_confirm",
  "buttonSkin": "dmzultra:arena",
  "x": 20,
  "y": 100,
  "w": 150,
  "h": 18,
  "pad": 8,
  "args": { "screen": "shop", "page": "main" }
}
```

#### `style` values (client must implement)

| Value | Meaning | Visual expectation |
|-------|---------|-------------------|
| `normal` | Default button | Neutral / standard themed button |
| `confirm` | Safe / positive action | **Green** (or confirm theme) |
| `danger` | Destructive / irreversible | **Red** (or danger theme) |
| `dim` | Disabled / unavailable look | **Dark gray** / muted (still clickable unless server says otherwise) |

Unknown style values → fall back to `normal`. Missing `style` → `normal`.

`style` stays **semantic**. Decorative tints use `palette` (headers / tiles / tabs), not new
style enums. Do not invent parallel RGB systems — map `palette` names onto the client’s
existing UltraUi palette.

### `activeTab` (GuiOpenPacket field 5)

| | |
|--|--|
| Wire field | #5 `activeTab` (string) |
| Meaning | Id of the **currently open** tab |
| Client must | Visually distinguish that tab from the others (highlight / selected state) |

`tabsJson` entries use `{id,label}` and may include optional `palette` / `accent` /
`sound` like buttons. Match `activeTab` against the tab’s **unqualified** key when the
server sends keys like `home`, or against the full `id` when that is what `activeTab`
contains.
### GuiFeaturesPacket (C→S) — message 2

Optional capability advertisement. **Backward compatible:** a client that never sends it
is treated as supporting **nothing beyond the 1.3.1 baseline** (dynamic UI, but no
`style` / `activeTab`).

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `featuresJson` | string | JSON **array** of feature ids |

Known optional feature ids:

| Id | Means the client honors… |
|----|--------------------------|
| `style` | Button `style` values above |
| `activeTab` | GuiOpenPacket field 5 highlight |
| `titleAlign` | `layoutJson.titleAlign` (`center` / `left`) |
| `playerModel` | `layoutJson.playerModel` entity preview |
| `textInput` | GuiOpenPacket field 11 `inputsJson` text fields + `bindInput` |
| `headerBottom` | `layoutJson.headerBottom` geometry |
| `accent` | Letter-code `accent` tint (compat) |
| `palette` | Generic `palette` color names → UltraUi palette (**proposed**) |
| `sound` | Button/layout `sound` ids (**proposed**) |
| `buttonSkin` | Button skin ids / state maps (**proposed**) |
| `frame` | Panel `frame` chrome (**proposed**) |
| `pagedBook` | `inputsJson` `type=paged_book` (**proposed**) |

Example (kit after `headerBottom` / `accent`):

```json
["style","activeTab","titleAlign","playerModel","textInput","headerBottom","accent"]
```

Send once after channel registration (client setup / first join). Extra ids are ignored.
Extra wire fields after #1 are ignored.

LM logs a **warning** when a player’s feature set lacks expected optionals, naming:

- player
- KaiPath version if known
- missing features
- player-visible impact (e.g. “all buttons render identically…” / “active tab is not highlighted”)

`/lm admin guitest` includes the reported features in staff chat and the open packet body.

---

## For other mod developers

### 1. Channel naming

```java
new ResourceLocation(YOUR_MOD_ID, "gui")  // → "mymod:gui"
```

Protocol `"1"`. Message 0 open, 1 action, 2 features (optional).

### 2. GuiOpenPacket (S→C) — 11 fields

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Any string (not a whitelist) |
| 2 | `title` | string | May include `§` colors |
| 3 | `pageId` | string | Page within the system |
| 4 | `tabsJson` | string | JSON array of `{id,label}` |
| 5 | `activeTab` | string | **Active tab key — client must highlight** |
| 6 | `linesJson` | string | JSON array of body lines |
| 7 | `buttonsJson` | string | JSON array of button objects (incl. **style**) |
| 8 | `panelWidth` | varint | Default **350** |
| 9 | `panelHeight` | varint | Default **300** (grows toward **420** when needed) |
| 10 | `layoutJson` | string | JSON object (panel + body/button metrics + optional `playerModel`) |
| 11 | `inputsJson` | string | JSON array of input elements (`type=text` today); default `[]` |

Missing trailing fields → defaults. Bytes after the last understood field ignored.

### 3. GuiActionPacket (C→S) — 3 fields

| # | Field | Type | Notes |
|---|--------|------|--------|
| 1 | `screenId` | string | Echo open `screenId` verbatim |
| 2 | `actionId` | string | Echo button/tab/input `id` verbatim |
| 3 | `argsJson` | string | Echo button `args` (+ `value` when submitting a text field) |

#### `inputsJson` (field 11)

```json
[{
  "id": "@config:input:difficulty|some.path",
  "type": "text",
  "x": 16, "y": 200, "w": 318, "h": 18,
  "value": "current",
  "placeholder": "Type a value…",
  "maxLength": 128,
  "args": { "screen": "config", "page": "detail:…", "system": "config" }
}]
```

| `type` | Status | Notes |
|--------|--------|-------|
| `text` | **Shipped** (KaiPath 1.3.9+) | Single-line EditBox; `bindInput` → `args.value` |
| `paged_book` | **Proposed** | Multi-page reader (prev/next). Copy Warrior Journal **UX**, not DMZUltra screen classes. See § Proposed chrome. |
| `toggle` | Reserved | Not on wire yet — booleans use styled buttons |
| `slider` | Reserved | Not on wire yet — see [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) |
Buttons that apply a field include `args.bindInput` set to that input’s `id`. On click (or
Enter in the field), the client must set `args.value` to the typed string before sending
`GuiActionPacket`.

**UTF-8:** All GuiOpen string fields (title, lines, labels, placeholders, …) are encoded and
decoded with explicit `StandardCharsets.UTF_8`. Clients must decode the same way;
platform-default charset causes mojibake on accents (é/à).

### 4. Layout JSON

```json
{
  "panelWidth": 350,
  "panelHeight": 300,
  "width": 350,
  "height": 300,
  "center": true,
  "margin": 10,
  "titleAlign": "center",
  "tabsUnderTitle": true,
  "headerBottom": 28,
  "palette": "gold",
  "frame": "dmzultra:card",
  "lineHeight": 16,
  "lineGap": 8,
  "bodyTop": 42,
  "bodyPad": 10,
  "bodyMaxHeight": 140,
  "scrollBody": true,
  "buttonPad": 8,
  "buttonGap": 6,
  "buttonMinWidth": 80,
  "buttonHeight": 18,
  "playerModel": {
    "x": 248,
    "y": 60,
    "width": 86,
    "height": 120,
    "scale": 1.0,
    "rotate": true,
    "portraitFrame": "dmzultra:portrait",
    "parts": {
      "schema": 1,
      "subject": "local",
      "headBones": {
        "token": "hair+ears1+horns2",
        "pieces": ["hair", "ears1", "horns2"]
      },
      "form": {
        "race": "saiyan",
        "group": "super_saiyan",
        "form": "ssj",
        "stackGroup": "",
        "stackForm": ""
      },
      "extras": []
    }
  }
}
```

Panel at `((screenW - panelWidth) / 2, (screenH - panelHeight) / 2)` when `center` is true;
honor `margin`. Button `x`/`y` are panel-relative.

#### Shared body / button metrics

| Field | Meaning |
|-------|---------|
| `titleAlign` | **`center`** (recommended default) or `left` — **must** position the title; do not assume left |
| `tabsUnderTitle` | When `true`, tab controls are regular buttons under the header (not KaiPath's native title-chrome tab strip). Honor `headerBottom`. |
| `headerBottom` | **Panel-relative Y where client header chrome must end.** Title text + yellow divider stay **strictly above** this line. When `tabsUnderTitle` is true, the tab row sits at this Y (default **28**); when false, default **42** (`bodyTop`). This is the geometry contract that keeps tabs under the yellow divider. |
| `palette` | Optional generic color **name** for title / active-tab tint: `red`, `dark_red`, `purple`, `dark_purple`, `gold`, `yellow`, `green`, `aqua`, `blue`, `white`, `gray`. Maps to the client UltraUi palette — **not** partner system names. Prefer this over letter `accent`. When both are set, `palette` wins. |
| `accent` | Optional single-letter Minecraft color code (`c`, `5`, `6`, `b`, `a`, `e`, `4`, …) for compat. Titles/labels stay plain ASCII (no section-sign). Omit when blank. |
| `frame` | Optional panel chrome id (string) or state map — see § Proposed chrome. **Proposed.** |
| `lineHeight` | Body line height in GUI pixels (do **not** squeeze below this) |
| `lineGap` | Extra gap between body lines |
| `bodyTop` | Panel-relative Y where the body/scroll region starts |
| `bodyPad` | Vertical padding inside the body scroll region |
| `bodyMaxHeight` | Vertical room for body text inside the panel (grows with `panelHeight`) |
| `scrollBody` | When `true`, long body content **scrolls** instead of compressing |
| `buttonPad` | Horizontal padding inside buttons (default **8**) |
| `buttonGap` | Vertical gap between button rows |
| `buttonMinWidth` | Minimum button width (**80**) — never shrink for long labels |
| `buttonHeight` | Standard button height |

Buttons also carry per-button `pad` and optional per-button **`palette`** / **`accent`** for
tiles and tabs. Labels stay plain ASCII; the client tints from `palette`/`accent`. Overlong
labels truncate with ASCII `...`; pages scroll rather than squeezing buttons.
#### `playerModel` (optional, client-rendered)

| Field | Type | Notes |
|-------|------|-------|
| `x`, `y` | number | Panel-relative origin |
| `width`, `height` | number | Bounds for the preview |
| `scale` | number | Entity scale (default `1.0`) |
| `rotate` | boolean | Slow turntable when `true` |
| `portraitFrame` | string | Optional namespaced portrait chrome id (`dmzultra:portrait`, `dmzultra:portrait_grey`, …). **Proposed.** |
| `parts` | object | Optional cosmetics snapshot — see below |

**Backward compatible:** clients that do not know `playerModel` ignore it; the rest of the
layout still works. JLDK implements the entity rendering; the server declares position, size,
and (when present) equipped custom parts. Screen builders opt in per screen.
##### `playerModel.parts` (optional, schema 1)

LM sends this so the client can draw **custom body pieces** on top of / instead of a bare
Steve preview — DMZ head bones today, DMZUltra form accessories next. Omit the whole
`parts` object when nothing is equipped / unavailable. Clients that only honor bounds still
work; unknown sub-fields are ignored.

| Field | Type | Required | Notes |
|-------|------|----------|-------|
| `schema` | number | yes | **`1`** for this document. Bump only on breaking shape changes. |
| `subject` | string | yes | Who to draw. LM Character Services always sends **`local`** (the viewing player). Future: `uuid:<player-uuid>` for inspect. |
| `headBones` | object | no | Equipped DMZ `activeHeadBone` cosmetics. Omit when empty. |
| `headBones.token` | string | yes* | Authoritative DMZ token (`+`-joined). Same string DMZ stores on the character. |
| `headBones.pieces` | string[] | yes* | Atomic bone ids split from the token (stable shop order when known). Prefer this list when iterating geo bones. |
| `form` | object | no | Worn transformation identity so the client can attach **DMZUltra FormParts** (client-side catalog keyed by form). Omit when fully base. |
| `form.race` | string | no | Race id (lowercase), e.g. `saiyan`. |
| `form.group` | string | no | Active form group. |
| `form.form` | string | no | Active form name. |
| `form.stackGroup` | string | no | Stack form group (empty when none). |
| `form.stackForm` | string | no | Stack form name (empty when none). |
| `extras` | array | no | Open extension list. Each entry is `{ "kind": string, "id": string, … }`. Unknown `kind` values **must** be ignored. LM currently sends `[]`. |

\* When `headBones` is present, both `token` and `pieces` are required. `pieces` may be empty
only if `token` is empty (do not send `headBones` in that case).

**Client render contract (JLDK):**

1. Draw the subject player entity inside `x/y/width/height` at `scale`, with turntable when
   `rotate` is true.
2. If `parts.headBones` is present, apply those geo bones the same way DMZ applies
   `activeHeadBone` (split `token` on `+`, or iterate `pieces`).
3. If `parts.form` is present, resolve DMZUltra FormParts for that race/group/form (and
   stack, when set) and draw them with the preview.
4. Iterate `parts.extras`; skip unknown `kind`s.
5. If `parts` is missing entirely, draw the local player with whatever the client already
   knows from entity sync (no server cosmetics hint).

**`extras` entry shape (forward-compatible):**

```json
{ "kind": "dmzultra.formPart", "id": "example_accessory", "meta": {} }
```

| Field | Type | Notes |
|-------|------|-------|
| `kind` | string | Namespaced role (`dmz.headBone` is redundant with `headBones`; prefer the typed object). |
| `id` | string | Piece id within that kind. |
| `meta` | object | Optional opaque bag (tint, slot, …). Ignore unknown keys. |

LM server helper: `PlayerModelParts.snapshot(ServerPlayer)` → Gson object attached under
`playerModel.parts`.

### 5. Action verb contract (recommended)

Optional `@<screen>:` prefix, then bare id or `open:` / `do:` / `tab:` / `nav:` / `page:`.
Client never validates action ids — echo verbatim.

LM enforces this at packet **build** time via `UltraScreenIds.requireValidActionId`
(malformed ids never reach the client).

### 6. Hello world (any mod — exercises chrome fields)

Plain ASCII labels. Decorative tint via `palette`. Sounds / skins / frames use namespaced
ids. Full JSON fixture:
[`tools/ultra-gui-kit/server/BridgeFutureFieldsFixture.json`](../tools/ultra-gui-kit/server/BridgeFutureFieldsFixture.json).

```java
String buttons = "["
    + "{\"id\":\"@myshop:do:ping\",\"label\":\"Ping\",\"style\":\"confirm\","
    + "\"palette\":\"gold\",\"sound\":\"dmzultra:ui_confirm\","
    + "\"buttonSkin\":\"dmzultra:arena\",\"x\":20,\"y\":40,\"w\":150,\"h\":18,"
    + "\"pad\":8,\"args\":{\"screen\":\"hello\",\"page\":\"main\"}}"
    + "]";
String inputs = "["
    + "{\"id\":\"@myshop:book:guide\",\"type\":\"paged_book\",\"x\":8,\"y\":70,"
    + "\"w\":220,\"h\":120,\"pages\":["
    + "{\"title\":\"Welcome\",\"lines\":[\"Page one.\"]},"
    + "{\"title\":\"Tips\",\"lines\":[\"Page two.\"]}"
    + "]}"
    + "]";
String layout = "{"
    + "\"center\":true,\"margin\":10,\"titleAlign\":\"center\","
    + "\"headerBottom\":42,\"palette\":\"gold\",\"frame\":\"dmzultra:card\","
    + "\"lineHeight\":16,\"lineGap\":8,\"bodyTop\":48,\"bodyPad\":10,"
    + "\"bodyMaxHeight\":140,\"scrollBody\":true,"
    + "\"buttonPad\":8,\"buttonGap\":6,\"buttonMinWidth\":80,\"buttonHeight\":18,"
    + "\"playerModel\":{\"x\":248,\"y\":60,\"width\":86,\"height\":120,"
    + "\"scale\":1.0,\"rotate\":true,\"portraitFrame\":\"dmzultra:portrait\"}"
    + "}";
UltraGuiOpenPacket open = new UltraGuiOpenPacket(
    "hello", "My Shop", "main", "[]", "", "[\"Welcome.\"]",
    buttons, 350, 300, layout, inputs);
CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), open);
```

Client: `UltraGuiPartners.register("myshop")` then advertise implemented features
(at minimum the shipped set; add `palette` / `sound` / `buttonSkin` / `frame` / `pagedBook`
as each lands).

---

## Proposed chrome fields (asset reuse — next KaiPath)

See also [`DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md). These fields are
**mod-agnostic**. Ids are `namespace:path` (never `assets/...` filesystem paths). Unknown
ids → ignore (no crash). Ask JLDK which `UltraUi$Theme` / `$Palette` entries are hardcoded
vs data-driven before expanding the palette name list.

### `palette` (layout + button/tab)

Generic color **names** only: `red`, `dark_red`, `purple`, `dark_purple`, `gold`, `yellow`,
`green`, `aqua`, `blue`, `white`, `gray`.

Client maps names → UltraUi palette entries. Partners map their own systems → names on the
server. When `palette` and letter `accent` both appear, **`palette` wins**.

### `sound` (button / optional layout)

```json
"sound": "dmzultra:ui_confirm"
```

Play on click (button) or as a light UI tick when the client chooses (layout). Examples:
`dmzultra:ui_confirm`, `dmzultra:scene_tick`, `dmzultra:travel_tick`. Other mods may use
their own namespaces.

### `buttonSkin` (button)

Short id **or** explicit state map:

```json
"buttonSkin": "dmzultra:arena"
```

```json
"buttonSkin": {
  "id": "dmzultra:arena",
  "normal": "dmzultra:gui/arena/button",
  "hover": "dmzultra:gui/arena/button",
  "pressed": "dmzultra:gui/arena/button",
  "disabled": "dmzultra:gui/arena/button"
}
```

Prefer short ids; the client resolves state textures. Do not send flat color RGB as a
substitute for skins.

### `frame` (layout)

```json
"frame": "dmzultra:card"
```

```json
"frame": {
  "id": "dmzultra:card",
  "card": "dmzultra:gui/arena/card_frame",
  "panel": "dmzultra:gui/arena/panel_tile",
  "header": "dmzultra:gui/arena/header"
}
```

### `playerModel.portraitFrame`

```json
"portraitFrame": "dmzultra:portrait"
```

Values such as `dmzultra:portrait` / `dmzultra:portrait_grey`. Draws chrome around the
existing `playerModel` preview bounds.

### `inputsJson` `type: "paged_book"`

```json
{
  "id": "@myshop:book:guide",
  "type": "paged_book",
  "x": 8, "y": 70, "w": 220, "h": 120,
  "page": 0,
  "pages": [
    { "title": "Welcome", "lines": ["Line A.", "Line B."] },
    { "title": "Tips", "lines": ["More text."] }
  ]
}
```

Prev/next controls are client chrome. Page turns may use a `sound` on the input object
(`dmzultra:scene_tick`). Copy the Warrior Journal **interaction pattern** only — do not
subclass DMZUltra journal screens.

Advertise when implemented: `palette`, `sound`, `buttonSkin`, `frame`, `pagedBook`.

---

## Shared channel convention (KaiPath)

| Rule | Detail |
|------|--------|
| Format | `<modid>:gui` |
| Registry key | Full channel id |
| Multi-mod | Clicks return on the channel that opened the screen |
| Protocol | `"1"` |

---

## LegacyMechanics notes

LM channel: `legacymechanics:gui`. Startup aborts on catalog errors or encode/decode
round-trip failure (including **style** / **activeTab** survival and `GuiFeaturesPacket`).

With KaiPath **1.3.1+** dynamic-any screen ids, hub wire shim is off
(`UltraScreenIds.KAIPATH_DYNAMIC_ANY_SCREEN_ID`).

Smoke: `/lm admin guitest` → reports client features → click **Ping server**.

**LM-only letter `accent` map** (not protocol enums — LM maps systems → letters locally):
Difficulty `c` · Rival `5` · Sparring `6` · Prestige `b` · Character `a` · Skill Check `e` ·
Staff Admin `4`. Main hub omits `accent` (shared gold). New work should prefer generic
`palette` names instead.

---

## Packaging checklist (LM jar)

1. Clean compile output before packaging (`build.sh` does this).
2. Scan jar for missing/referenced classes and inner classes
   (`audit_jar_inner_classes.py`).
3. Scan every `@Mixin` class for non-private helper methods
   (`audit_mixin_visibility.py` — helpers must be `private`).
4. Version pins match: `AdaptiveDifficultyMod.VERSION` == `mods.toml` `version`
   (== `plugin.yml` when a GUI plugin is shipped).
5. Fail-closed audits in `build.sh` (Ultra actions, forge GUI backend, ship manifest, …).
