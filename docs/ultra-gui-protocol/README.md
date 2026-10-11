# Ultra GUI protocol — third-party reference

Drop-in wire codec (no LegacyMechanics imports):

| File | Role |
|------|------|
| `reference/UltraGuiChannel.java` | Channel convention `<modid>:gui`, protocol `"1"`, panel defaults |
| `reference/UltraGuiOpenPacket.java` | 10-field S→C open |
| `reference/UltraGuiActionPacket.java` | 3-field C→S click |

Full contract + hello-world: [`../KAIPATH_GUI_PROTOCOL.md`](../KAIPATH_GUI_PROTOCOL.md).

Copy the three Java files into your mod, rename the package, and register a Forge
`SimpleChannel` on `new ResourceLocation(yourModId, "gui")`.
