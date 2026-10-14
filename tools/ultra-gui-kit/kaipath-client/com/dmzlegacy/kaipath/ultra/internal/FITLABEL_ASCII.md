# `UltraWidgetReflect.fitLabel` — ASCII ellipsis (fixes weird A/E glyphs)

Stock KaiPath `UltraWidgetReflect.fitLabel` appends a **mojibaked** ellipsis string
(`â€¦`, UTF-8 misread of `…`). On many clients that renders as stray **A/E** glyphs after
truncated tab/button labels.

## Required replacement (merge into official KaiPath)

In `com.dmzlegacy.kaipath.ultra.internal.UltraWidgetReflect`, replace `fitLabel` with:

```java
public static String fitLabel(net.minecraft.client.gui.Font font, String text, int maxWidth) {
    if (maxWidth <= 0) {
        return "";
    }
    if (text == null) {
        text = "";
    }
    if (font.m_92895_(text) <= maxWidth) {
        return text;
    }
    String ellipsis = "..."; // ASCII only — never U+2026 / mojibake
    int ellipsisW = font.m_92895_(ellipsis);
    if (ellipsisW > maxWidth) {
        return "";
    }
    return font.m_92834_(text, maxWidth - ellipsisW) + ellipsis;
}
```

Also ensure button/tab **labels stay plain ASCII** (no section-sign prefixes). Tint via
`style` / `accent` chrome only — `§a` / `§e` prefixes are another source of stray A/E glyphs.

## Dev overlay

`tools/ultra-gui-kit/build-kaipath-overlay.sh` runs `patch_fitlabel_ascii.py` to rewrite the
mojibake constants inside the base jar’s `UltraWidgetReflect.class` to ASCII `...`.
