# JLDK brief — KaiPath Ultra GUI client conformance

**Status (LM 4.6.107+):** Server sends full layout hints. KaiPath **1.3.x** baseline decodes
10 GuiOpen fields and renders dynamic UI. Optional render features below still need client
work + `GuiFeaturesPacket` advertisement.

| Kit | Path |
|-----|------|
| Protocol + conformance contract | [`docs/KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) |
| KaiPath client sources (reference) | [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/) |
| Server codecs (any mod) | [`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/) |

---

## Governing rule

The client renders what the server sends — **no hardcoded screen/tab/button/action lists**.
`style`, `activeTab`, `titleAlign`, and `playerModel` are **packet data**, not new screens.

## LM already sends (honor these)

| Feature | Where | LM value / notes |
|---------|-------|------------------|
| `titleAlign` | `layoutJson.titleAlign` | Always `"center"` (also in default layout fallback) |
| Panel size | fields 8–9 + `layoutJson` | Default **350×300**; grows toward **420** when body/buttons need room; `scrollBody` when still overflowing |
| Button `style` | each button object | `normal` / `confirm` / `danger` / `dim` |
| `activeTab` | GuiOpenPacket field 5 | Short tab key (`home`, `tiers`, …) |
| `playerModel` | `layoutJson.playerModel` | Character screens: `{x,y,width,height,scale,rotate}` |

Reference kit `LmDynamicScreen` already parses `titleAlign` / `playerModel` / body metrics —
ship that path on live KaiPath and advertise features when each lands.

## Gap vs LM today (KaiPath 1.3.x live)

| Feature | Server sends | Client today |
|---------|--------------|--------------|
| `layoutJson.titleAlign` = `center` | yes | often left-default if ignored |
| Button `style` | yes | ignored — all buttons identical |
| `activeTab` highlight | yes | decoded, not highlighted |
| `playerModel` preview | yes (Character) | no entity render |
| `GuiFeaturesPacket` | accepted | not sent → treated as baseline |

LM logs a warning naming the player, KaiPath version, missing features, and player-visible
impact. `/lm admin guitest` prints the feature summary.

## Required next work (KaiPath / JLDK)

1. **Title alignment** — read `layoutJson.titleAlign`; when `"center"`, center the title in the panel (do not left-align by default).
2. **Button styles** — map `style`: `confirm`→green, `danger`→red, `dim`→dark-gray, `normal`→default.
3. **activeTab** — highlight the tab whose key matches `GuiOpenPacket.activeTab()`.
4. **playerModel** — render the local player entity in the declared bounds (Character screens).
5. **Advertise** — after channel register (when remote present):

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\",\"titleAlign\",\"playerModel\"]");
```

Set `UltraGuiConsumerRegistry.ADVERTISED_FEATURES_JSON` accordingly in the kit.

6. Keep encoding/decoding tolerant (defaults; ignore trailing fields). Honor `panelHeight`
   growth and `scrollBody` / `bodyMaxHeight` so dense menus (Difficulty) stay readable.

## Already done (baseline)

Decode 10 fields; dynamic UI; verbatim action echo; any `screenId`; multi-mod
`UltraGuiConsumerRegistry`. Kit sources under `tools/ultra-gui-kit/kaipath-client/` include
`titleAlign` centering and `playerModel` layout hooks — wire them through the live jar.
