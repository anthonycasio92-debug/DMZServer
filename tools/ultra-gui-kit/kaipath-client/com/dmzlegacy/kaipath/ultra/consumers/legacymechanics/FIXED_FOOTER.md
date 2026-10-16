# fixedFooter — merge into official KaiPath (1.3.13 → next)

**Gap:** LM emits `"footer": true` on Back / Main menu. Live 1.3.13 sets
`contentBottom = panelY + panelH - 28` and adds **every** button to `scrollWidgets`,
so dual footer scrolls with the body.

**Kit status:** `UltraBridgeFooter` + `LmDynamicScreen` honor `footer:true` and advertise
`fixedFooter`. Copy the helpers into the official scroll-clipped screen.

## Advertise

Add `"fixedFooter"` (and `"portraitFrame"` if still missing) to GuiFeatures:

```json
["style","activeTab","titleAlign","playerModel","textInput","headerBottom","accent","palette","sound","buttonSkin","frame","pagedBook","portraitFrame","fixedFooter","combined.forms.v1","bodyParts","icon"]
```

`bodyParts` attaches `playerModel.parts.bodyParts`. `icon` is the reference kit's extra button/tab id. Stock KaiPath 1.3.17 stops at `combined.forms.v1`.

## Live `LmDynamicScreen.m_7856_` changes

After tabs (where 1.3.13 sets `contentTop` / `contentBottom`):

```java
JsonArray buttons = array(packet.buttonsJson());
int footerReserve = UltraBridgeFooter.footerReservePx(buttons, buttonHeight, margin);
this.contentTop = this.panelY + cursorY;
this.contentBottom = UltraBridgeFooter.contentBottomAbs(this.panelY, this.panelH, footerReserve);
```

When building the button list, **split**:

```java
for (JsonElement el : buttons) {
    JsonObject b = el.getAsJsonObject();
    // ...
    boolean footer = UltraBridgeFooter.isFooter(b);
    int h = Math.max(12, integer(b, "h", buttonHeight));
    int y = footer
            ? UltraBridgeFooter.pinFooterY(this.panelH, footerReserve, h)
            : integer(b, "y", cursorY);
    AbstractWidget btn = makeButton(/* x,y,w,h,... */);
    if (footer) {
        // Pinned chrome — NOT in scrollWidgets
        this.m_142416_(btn);
    } else {
        content.add(btn); // existing scroll path
    }
}
```

Do **not** add wire fields for `contentTop` / `contentBottom` / `contentScroll` /
`titleTint` — those stay client-internal. See `docs/KAIPATH_BRIDGE_GAPS.md` § Pitfalls.

## Pitfalls (same as gap matrix)

1. Server `mods/kaipath-*.jar` does not paint Ultra — players need the client mod.
2. Chrome RLs (`buttonSkin` / `frame` / `portraitFrame`) are enable tokens, not texture binds.
3. Advertise `pagedBook`; widget type is `paged_book`.
4. Keep ASCII `fitLabel` ellipsis (`...`).
