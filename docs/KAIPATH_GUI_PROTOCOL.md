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

---

## Client conformance contract (JLDK)

The client **must** render what the server sends (no hardcoded screen/tab/button lists).

| Capability | Required since | **1.3.7** released | Reference kit |
|------------|----------------|--------------------|---------------|
| Decode GuiOpen fields 1–10 | 1.3.1 | yes | yes |
| Dynamic title / tabs / lines / buttons | 1.3.1 | yes | yes |
| Verbatim GuiAction echo | 1.3.1 | yes | yes |
| Any `screenId` (no whitelist) | 1.3.1 | yes | yes |
| Honor **`layoutJson.titleAlign`** | 1.3.1+ | **yes** | yes |
| Honor **`activeTab`** highlight | 1.3.1+ | **yes** | yes |
| Honor button **`style`** | 1.3.7 | **yes** | yes |
| Honor **`layoutJson.playerModel`** (+ optional `parts`) | 1.3.7 | **yes** (entity) | yes (+ parts hook) |
| Honor **`inputsJson`** text fields (field 11) | next | **no** | **yes** |
| Decode GuiOpen strings as **UTF-8** | 1.3.1 | MC `readUtf` | **yes** (explicit) |
| Send **GuiFeaturesPacket** | 1.3.7 | **yes** (4 features) | **yes** (+ `textInput`) |
| `inputsJson` `slider` / `toggle` | later | — | reserved — see [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) |

### Button JSON schema

Every button object in `buttonsJson`:

| Field | Required | Type | Notes |
|-------|----------|------|--------|
| `id` | yes | string | Action id (echoed verbatim on click) |
| `label` | yes | string | Display text (may include `§` colors) |
| `style` | no | string | Visual role — see table below (default `normal`) |
| `x` | yes | number | Panel-relative X |
| `y` | yes | number | Panel-relative Y |
| `w` | yes | number | Width (minimum **80** — never shrink for long labels) |
| `h` | yes | number | Height |
| `pad` | no | number | Horizontal label padding (default **8**) |
| `args` | recommended | object | Opaque JSON echoed to the server |

```json
{
  "id": "@difficulty:do:activate:1",
  "label": "§aActivate T1",
  "style": "confirm",
  "x": 20,
  "y": 100,
  "w": 150,
  "h": 18,
  "pad": 8,
  "args": { "screen": "difficulty", "page": "tiers" }
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

KaiPath **1.3.1** ignores `style`: all buttons render identically (no green/red/dim
distinction).

### `activeTab` (GuiOpenPacket field 5)

| | |
|--|--|
| Wire field | #5 `activeTab` (string) |
| Meaning | Id of the **currently open** tab |
| Client must | Visually distinguish that tab from the others (highlight / selected state) |

`tabsJson` entries use `{id,label}`. Match `activeTab` against the tab’s **unqualified**
key when the server sends keys like `tiers`, or against the full `id` when that is what
`activeTab` contains. LM builders set `activeTab` to the short key (e.g. `home`, `tiers`).

KaiPath **1.3.1** ignores `activeTab`: the active tab is not highlighted.

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

Example (conforming client after implementing all):

```json
["style","activeTab","titleAlign","playerModel","textInput"]
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
| `text` | **Shipped** (LM 4.6.108+) | Config TEXT / NUMBER / ENUM detail editors |
| `toggle` | Reserved | Not on wire yet — BOOLEAN uses styled buttons |
| `slider` | Reserved | Not on wire yet — see [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md) |

Buttons that apply a field include `args.bindInput` set to that input’s `id`. On click (or
Enter in the field), the client must set `args.value` to the typed string before sending
`GuiActionPacket`.

**UTF-8:** All GuiOpen string fields (title, lines, labels, placeholders, …) are encoded and
decoded by LM with explicit `StandardCharsets.UTF_8`. Clients must decode the same way;
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
  "accent": "c",
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
| `titleAlign` | **`center`** (LM default) or `left` — **must** position the title; do not assume left |
| `tabsUnderTitle` | When `true`, LM placed tab controls as regular buttons under the header (not KaiPath's native title-chrome tab strip). Honor `headerBottom`. |
| `headerBottom` | **Panel-relative Y where client header chrome must end.** Title text + yellow divider stay **strictly above** this line. When `tabsUnderTitle` is true, LM places the tab row at this Y (default **28**); when false, default **42** (`bodyTop`). This is the geometry contract that keeps tabs under the yellow divider. |
| `accent` | Optional single-letter Minecraft color code (`c`, `5`, `6`, `b`, `a`, `e`, `4`, …) for **system accent**. Paint the title (and `activeTab` highlight) with this color. Do **not** expect section-sign in the title string — titles stay plain ASCII to avoid mojibake. Omit or ignore when blank. |
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

Buttons also carry per-button `pad` (same default) and optional per-button **`accent`**
(same single-letter codes as `layoutJson.accent`) for hub tiles / staff controls. Labels
stay plain ASCII; the client tints from `accent`. Overlong labels truncate with ASCII
`...`; pages scroll rather than squeezing buttons.

**System accent map (LM):** Difficulty `c` · Rival `5` · Sparring `6` · Prestige `b` ·
Character `a` · Skill Check `e` · Staff Admin `4`. Main hub omits `accent` (shared gold
title chrome).

#### `playerModel` (optional, client-rendered)

| Field | Type | Notes |
|-------|------|-------|
| `x`, `y` | number | Panel-relative origin |
| `width`, `height` | number | Bounds for the preview |
| `scale` | number | Entity scale (default `1.0`) |
| `rotate` | boolean | Slow turntable when `true` |
| `parts` | object | Optional cosmetics snapshot — see below |

**Backward compatible:** clients that do not know `playerModel` ignore it; the rest of the
layout still works. JLDK implements the entity rendering; the server declares position, size,
and (when present) equipped custom parts. Screen builders opt in per screen (LM Character
Services uses it; others default off).

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

### 6. Hello world

```java
String buttons = "["
    + "{\"id\":\"@myshop:do:ping\",\"label\":\"§aPing\","
    + "\"style\":\"confirm\",\"x\":20,\"y\":80,\"w\":150,\"h\":18,"
    + "\"pad\":8,\"args\":{\"screen\":\"hello\",\"page\":\"main\"}}"
    + "]";
GuiOpenPacket open = new GuiOpenPacket(
    "hello", "§6My Shop", "main",
    "[{\"id\":\"home\",\"label\":\"Home\"}]",
    "home",
    "[\"Welcome.\"]",
    buttons, 350, 300,
    "{\"center\":true,\"margin\":10,\"titleAlign\":\"center\",\"lineHeight\":16,\"lineGap\":8,"
        + "\"bodyTop\":42,\"bodyPad\":10,\"bodyMaxHeight\":140,\"scrollBody\":true,"
        + "\"buttonPad\":8,\"buttonGap\":6,\"buttonMinWidth\":80,\"buttonHeight\":18}");
CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), open);
```

Client: `UltraGuiPartners.register("mymod")` (or add to `DEFAULT_PARTNERS`) then the
registry advertises
`["style","activeTab","titleAlign","playerModel","textInput","headerBottom","accent"]`
once those features are implemented.

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
