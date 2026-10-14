# DMZUltra asset reuse via the KaiPath Ultra bridge

**Audience:** Cursor (server partners) + JLDK (KaiPath client).  
**Reference client assets:** live `mods/dmzultra-0.2.9.jar` (client-side only; not required in
server CI). Asset ids below match that jar’s `assets/dmzultra/...` layout.  
**Protocol home:** KaiPath (`docs/KAIPATH_GUI_PROTOCOL.md`) — **not** DMZUltra.

## Governing rules

1. **All DMZUltra assets are client-side.** Server mods must never reference
   `assets/dmzultra/...` paths, never bundle those files, and never subclass DMZUltra
   `Screen` classes (Arena, Travel, Divine, Tournament, Journal, … are monolithic features).
2. **The only clean integration point is the KaiPath bridge** — channel
   `{modId}:gui`, wire format in [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md).
3. **Every reuse = protocol field + JLDK render.** Cursor adds a mod-agnostic field;
   JLDK maps it onto DMZUltra / KaiPath widgets and asset ids.
4. **Bridge PRs must work for any mod.** If a field name, default, or doc example only
   makes sense for one partner mod, it is wrong. Partners map their own domains to
   generic values (see `palette` below).
5. **DMZUltra 0.2.9 contains zero `{modId}:gui` protocol code.** Do not look there for
   codecs or consumers. Ignore its 57 Screen classes, entity/block textures, combat
   animations, and VFX shaders when designing menu chrome.

## Already working (do not rebuild)

| Client capability | Notes |
|-------------------|-------|
| UltraButton / UltraList / UltraEditBox | Via KaiPath `UltraUiAccess` / reflect helpers |
| Russo One font | `assets/dmzultra/font/russo_one.ttf` — client-owned |
| `titleAlign`, `activeTab` (via button style), `playerModel` + `parts` schema 1 | Shipped on KaiPath 1.3.9 |
| `inputsJson` field 11 (`type=text`) | Shipped on KaiPath 1.3.9 |
| `headerBottom` + letter `accent` | Emitted by partners; kit honors — merge next KaiPath |

## Reusable assets → protocol fields

| DMZUltra asset (client) | Protocol field (mod-agnostic) | Status |
|-------------------------|-------------------------------|--------|
| `UltraUi$Palette` / theme accents | `layoutJson.palette` + button/tab `palette` (generic color **names**) | **Proposed** |
| Letter-code tint (compat) | `layoutJson.accent` + button `accent` (`0`-`9`/`a`-`f`) | Shipped emit / pending client |
| `sounds/ui_confirm.ogg`, `scene_tick.ogg`, `travel_tick.ogg` | Button (or layout) `sound` — namespaced id | **Proposed** |
| `textures/gui/arena/button.png` (+ states) | Button `buttonSkin` — namespaced skin id or state map | **Proposed** |
| `card_frame.png`, `panel_tile.png`, `header.png` | `layoutJson.frame` — namespaced frame id or map | **Proposed** |
| `portrait_frame.png`, `portrait_frame_grey.png` | `playerModel.portraitFrame` — namespaced id | **Proposed** |
| Warrior Journal paged UX (`com/dmzultra/client/journal/`) | `inputsJson` `type: "paged_book"` — copy UX, not code | **Proposed** |

Full wire shapes: [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) §4 / §11 (future fields).  
Gap tracking: [`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md).

## Namespaced identifiers (not filesystem paths)

Use Minecraft `namespace:path` ids. **Never** put `assets/` prefixes or absolute paths on the wire.

| Kind | Example id | Client resolves to (illustrative) |
|------|------------|-----------------------------------|
| Sound | `dmzultra:ui_confirm` | `assets/dmzultra/sounds/ui_confirm.ogg` |
| Sound | `dmzultra:scene_tick` | `assets/dmzultra/sounds/scene_tick.ogg` |
| Sound | `dmzultra:travel_tick` | `assets/dmzultra/sounds/travel_tick.ogg` |
| Button skin | `dmzultra:arena` | Arena button texture set (normal/hover/pressed/disabled) |
| Frame | `dmzultra:card` | card_frame + panel_tile + header set |
| Portrait | `dmzultra:portrait` / `dmzultra:portrait_grey` | Portrait frame textures |

Other mods may ship their own namespaces (`myshop:ping`, `myshop:wood_frame`, …). KaiPath
resolves whatever the client has loaded; unknown ids are ignored (no crash).

## Palette names (generic — not partner system names)

Protocol values are **color names**, not feature names:

`red` · `dark_red` · `purple` · `dark_purple` · `gold` · `yellow` · `green` · `aqua` · `blue` · `white` · `gray`

Partners map their own systems locally (example only — **not** protocol enums):

| Partner concept (private) | Wire `palette` |
|---------------------------|----------------|
| shop promo tile | `gold` |
| danger / staff | `dark_red` |
| magic / rivalry | `purple` |

Do **not** put `ACCENT_RIVAL`, `SYSTEM_DIFFICULTY`, or similar into the protocol.

**Ask JLDK before parameterizing further:** which `UltraUi$Theme` / `$Palette` values are
hardcoded vs data-driven in the live client. Until answered, stick to the generic name list
above and let KaiPath map names → palette entries.

## Semantic `style` vs decorative `palette`

| Field | Role |
|-------|------|
| `style` | **Semantic** action chrome: `confirm` / `danger` / `dim` / `normal` |
| `palette` | **Decorative** header / tile / tab / system tint |
| `accent` | Legacy letter-code tint (compat); prefer `palette` for new work |

Do not overload `style` with decorative colors. Do not invent a second RGB color system on
the server — reuse DMZUltra’s palette through KaiPath.

## Pitfalls

- Never bundle DMZUltra assets into a server jar.
- Never subclass DMZUltra screens for menus.
- Never hardcode one partner’s mod id into field defaults or protocol examples for new fields.
- When adding fields, update [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) in the
  **same commit** — JLDK implements from the doc.
- Prefer plain ASCII labels (no section-sign); tint via `palette` / `style` / chrome.

## Reference implementation (zero partner imports)

[`tools/ultra-gui-kit/server/HelloWorldServerSketch.java`](../tools/ultra-gui-kit/server/HelloWorldServerSketch.java)
and [`tools/ultra-gui-kit/server/BridgeFutureFieldsFixture.json`](../tools/ultra-gui-kit/server/BridgeFutureFieldsFixture.json)
exercise every proposed field on a fictional `myshop:gui` channel so JLDK can verify without
any production partner mod.
