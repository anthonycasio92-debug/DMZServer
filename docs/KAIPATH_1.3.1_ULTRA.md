# KaiPath 1.3.1 + LegacyMechanics 4.6.105

Live clients run **kaipath-1.3.1.jar** with:

- 10-field `GuiOpenPacket` decode
- `LmDynamicScreen` (title / tabs / lines / buttons from JSON)
- `UltraGuiConsumerRegistry` (`legacymechanics:gui`)
- Verbatim `GuiActionPacket` echo

LM **4.6.105** pairs with that client:

- `guiBackend=ultra` (default)
- `UltraScreenIds.KAIPATH_DYNAMIC_ANY_SCREEN_ID=true` → no hub wire shim
- Logical `screenId`s on the wire (`difficulty`, `rival`, …)

Smoke: `/lm admin guitest` → click **Ping server** → server log `GuiActionPacket ←`.
