# JLDK brief — KaiPath Ultra GUI client conformance

**Status:** KaiPath **1.3.1** ships the dynamic Ultra consumer (10-field decode,
`LmDynamicScreen`, multi-mod registry). LM **4.6.105+** still sends button **`style`** and
packet **`activeTab`** that 1.3.1 ignores — implement those next and advertise via
`GuiFeaturesPacket`.

| Kit | Path |
|-----|------|
| Protocol + conformance contract | [`docs/KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md) |
| KaiPath client sources | [`tools/ultra-gui-kit/kaipath-client/`](../tools/ultra-gui-kit/kaipath-client/) |
| Server codecs (any mod) | [`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/) |

---

## Governing rule

The client renders what the server sends — **no hardcoded screen/tab/button/action lists**.
`style` and `activeTab` are **packet data**, not new screens.

## Gap vs LM today (1.3.1)

| Feature | Server sends | 1.3.1 client |
|---------|--------------|--------------|
| Button `style` (`normal`/`confirm`/`danger`/`dim`) | yes | ignored — all buttons identical |
| `activeTab` (GuiOpenPacket field 5) | yes | ignored — no tab highlight |
| `GuiFeaturesPacket` msg 2 | accepted | not sent → treated as baseline |

LM logs a warning naming the player, KaiPath version, missing features, and player-visible
impact. `/lm admin guitest` prints the feature summary.

## Required next work

1. **Button styles** — map `style` to green / red / dim / normal themed buttons.
2. **activeTab** — highlight the tab whose key matches `GuiOpenPacket.activeTab()`.
3. **Advertise** — after channel register (when remote present):

```java
UltraGuiConsumerRegistry.advertiseFeatures(
    "legacymechanics:gui",
    "[\"style\",\"activeTab\"]");
```

Set `UltraGuiConsumerRegistry.ADVERTISED_FEATURES_JSON` accordingly in the kit.

4. Keep encoding/decoding tolerant (defaults; ignore trailing fields).

## Already done in 1.3.1

Decode 10 fields; dynamic UI; verbatim action echo; any `screenId`; multi-mod
`UltraGuiConsumerRegistry`.
