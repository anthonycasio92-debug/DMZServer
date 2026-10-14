# KaiPath Ultra bridge (any Forge mod)

| Side | Role |
|------|------|
| **Any server mod** | Pull [`tools/ultra-gui-kit/server/`](../tools/ultra-gui-kit/server/); own `{modId}:gui` |
| LM **4.6.152** (one partner) | Emits 11-field opens + `headerBottom` / letter `accent` / `palette` / `portraitFrame` |
| KaiPath **1.3.9** (live / recommended) | `style`, `activeTab`, `titleAlign`, `playerModel`, `textInput` (5 GuiFeatures) per partner channel |
| **Pending on KaiPath** | Merge kit: `headerBottom`, `accent`, `palette`, chrome honor, title centering, ASCII `fitLabel`, `UltraGuiPartners` |
| **Chrome in kit** | `palette`, `sound`, `buttonSkin`, `frame`, `portraitFrame`, `paged_book` — see [`DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md) |
| **Kit (handoff)** | [`tools/ultra-gui-kit/`](../tools/ultra-gui-kit/) |

Note: CurseForge jumped **1.3.1 → 1.3.7 → 1.3.9**. Field 11 / `textInput` landed in **1.3.9**.

Guitest on stock 1.3.9: `missingShipped=none pendingNext=[headerBottom, accent, palette]`.
After merging the kit: `pendingNext=none` for those three; texture blit for skins/frames stays JLDK.

See [`KAIPATH_GUI_PROTOCOL.md`](KAIPATH_GUI_PROTOCOL.md),
[`KAIPATH_BRIDGE_GAPS.md`](KAIPATH_BRIDGE_GAPS.md),
[`DMZULTRA_ASSET_REUSE.md`](DMZULTRA_ASSET_REUSE.md), and
[`KAIPATH_JLDK_CLIENT_BRIEF.md`](KAIPATH_JLDK_CLIENT_BRIEF.md).
