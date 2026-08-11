# DMZ Legacy Reborn — Client Modpack Reference

Source: [testing-dmzlegacy on CurseForge](https://www.curseforge.com/minecraft/modpacks/testing-dmzlegacy)

| Field | Value |
|-------|-------|
| Project ID | `1499152` |
| Latest file | `8472846` — DMZ Legacy Reborn 2.1.3 v1.0 V |
| Minecraft | `1.20.1` |
| Loader | `forge-47.4.10` |
| Manifest entries | 111 |
| Server IP (from pack desc) | `mc.dbzlegacyreborn.com` |

Local copy of the pack zip: `modpack/DMZ-Legacy-Reborn-2.1.3.zip`

## How this relates to the server repo

This CurseForge pack is the **player/client** install. The live server also runs:

- Hybrid Bukkit layer (for CMI + Fabled) — not in the CF pack
- `plugins/` jars you uploaded (CMI, Fabled + their deps)
- CustomNPCs global/player scripts in `customnpcs/scripts/`
- Server-tuned `config/dragonminez/` pulled from this pack's overrides

KubeJS **is** in the pack (KubeJS + Rhino + Architectury). The pack only ships default example scripts; your gameplay JS is CustomNPCs.

## DMZ-related content in the pack

- DragonMineZ (`1136088:8469416` = 2.1.3)
- DragonMine Z: Super
- Shurui's DMZ Utilities / Dungeons / Tournaments / Raid-Bosses
- CustomNPCs-Unofficial + CNPC-Gecko-Integration
- Apotheosis + Apothic Attributes (matches KiWeapons/Piercing scripts)
- Pam's HarvestCraft 2 (matches Farming TP Skill crop IDs)
- Capsule, Create stack, FTB Quests/Chunks/Teams/Ultimine, Lightman's Currency, etc.

## Full mod list

- [Shurui's DMZ Utilities (by Shurui)](https://www.curseforge.com/minecraft/mc-mods/shuruis-dmz-utilities)
- [Starlight with Create Fix (by Project8gbDeRam)](https://www.curseforge.com/minecraft/mc-mods/starlight-with-create-fix)
- [GeckoLib (by Gecko)](https://www.curseforge.com/minecraft/mc-mods/geckolib)
- [Corpse (by henkelmax)](https://www.curseforge.com/minecraft/mc-mods/corpse)
- [Xaero's World Map (by xaero96)](https://www.curseforge.com/minecraft/mc-mods/xaeros-world-map)
- [BetterItem (by DuoFabriTeam)](https://www.curseforge.com/minecraft/mc-mods/betteritem)
- [Tooltips TXF (by jahirtrap)](https://www.curseforge.com/minecraft/mc-mods/tooltips-txf)
- [Embeddium (by FiniteReality)](https://www.curseforge.com/minecraft/mc-mods/embeddium)
- [Apotheosis (by Shadows_of_Fire)](https://www.curseforge.com/minecraft/mc-mods/apotheosis)
- [Macaw's Paths and Pavings (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-paths-and-pavings)
- [Tinkers' Advanced-Materials (by EtSH_C2H6S)](https://www.curseforge.com/minecraft/mc-mods/tinkers-advanced-materials)
- [Tinkers Tool Leveling 2 (by RedFoxGaming67)](https://www.curseforge.com/minecraft/mc-mods/tinkers-tool-leveling-2)
- [Silent Lib (silentlib) (by SilentChaos512)](https://www.curseforge.com/minecraft/mc-mods/silent-lib)
- [Macaw's Bridges (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-bridges)
- [Dis-Enchanting Table (by Lupin)](https://www.curseforge.com/minecraft/mc-mods/dis-enchanting-table)
- [[Java] Purpurite (by thest1076)](https://www.curseforge.com/minecraft/texture-packs/java-purpurite)
- [Create: Bigger Storage Updated to Create 6 (by LandscapesReimagined)](https://www.curseforge.com/minecraft/mc-mods/create-bigger-storage-updated-to-create-6)
- [Shurui's DMZ Raid-Bosses (by Shurui)](https://www.curseforge.com/minecraft/mc-mods/shuruis-dmz-raid-bosses)
- [Furnitury - Vanilla Styled Furniture (by Glythex)](https://www.curseforge.com/minecraft/mc-mods/reeves-furniture)
- [Effortless Building (by Requioss)](https://www.curseforge.com/minecraft/mc-mods/effortless-building)
- [Mouse Tweaks (by YaLTeR)](https://www.curseforge.com/minecraft/mc-mods/mouse-tweaks)
- [Custom Starter Gear (by brandon3055)](https://www.curseforge.com/minecraft/mc-mods/custom-starter-gear)
- [Create: Extra Gauges (by LiukRast)](https://www.curseforge.com/minecraft/mc-mods/create-extra-gauges)
- [Macaw's Fences and Walls (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-fences-and-walls)
- [Patchouli (by Vazkii)](https://www.curseforge.com/minecraft/mc-mods/patchouli)
- [Architectury API (by shedaniel)](https://www.curseforge.com/minecraft/mc-mods/architectury-api)
- [Absent by Design (by Lothrazar)](https://www.curseforge.com/minecraft/mc-mods/absent-by-design)
- [EtST Lib (by EtSH_C2H6S)](https://www.curseforge.com/minecraft/mc-mods/etst-lib)
- [Create: Tinker's Compat (by danniitv_)](https://www.curseforge.com/minecraft/mc-mods/create-tinkers-compat)
- [Sophisticated Core (by P3pp3rF1y)](https://www.curseforge.com/minecraft/mc-mods/sophisticated-core)
- [Tinkers' Katanas (by Xenon372)](https://www.curseforge.com/minecraft/mc-mods/tinkers-katanas)
- [CustomNPCs-Unofficial (by Goodbird)](https://www.curseforge.com/minecraft/mc-mods/customnpcs-unofficial)
- [DragonMine Z - Dragon Ball in Minecraft! (by ezShokkoh)](https://www.curseforge.com/minecraft/mc-mods/dragonminez)
- [Create (by simibubi)](https://www.curseforge.com/minecraft/mc-mods/create)
- [Shurui's DMZ Dungeons (by Shurui)](https://www.curseforge.com/minecraft/mc-mods/shuruis-dmz-dungeons)
- [FTB Ultimine (NeoForge) (by FTB)](https://www.curseforge.com/minecraft/mc-mods/ftb-ultimine-forge)
- [DGLib (by MsDogGirl)](https://www.curseforge.com/minecraft/mc-mods/dglib)
- [Tinkers Construct (by mDiyo)](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct)
- [Explorer's Compass (by Chaosyr)](https://www.curseforge.com/minecraft/mc-mods/explorers-compass)
- [ExtraLib (by Vecoo)](https://www.curseforge.com/minecraft/mc-mods/extralib)
- [Corpse x Cosmetic Armor Reworked Compat (by Project8gbDeRam)](https://www.curseforge.com/minecraft/mc-mods/corpse-x-cosmetic-armor-reworked-compat)
- [Xaero's Minimap (by xaero96)](https://www.curseforge.com/minecraft/mc-mods/xaeros-minimap)
- [Create: Bigger Storage (by Luna)](https://www.curseforge.com/minecraft/mc-mods/create-bigger-storage)
- [Searchables (by Jaredlll08)](https://www.curseforge.com/minecraft/mc-mods/searchables)
- [IBE Editor (by skyecodes)](https://www.curseforge.com/minecraft/mc-mods/ibe-editor)
- [Lodestone (by sammysemicolon)](https://www.curseforge.com/minecraft/mc-mods/lodestone)
- [ModernFix (by embeddedt)](https://www.curseforge.com/minecraft/mc-mods/modernfix)
- [KubeJS (by Lat)](https://www.curseforge.com/minecraft/mc-mods/kubejs)
- [Rhino (by Lat)](https://www.curseforge.com/minecraft/mc-mods/rhino)
- [CNPC-Gecko-Integration (by Goodbird)](https://www.curseforge.com/minecraft/mc-mods/cnpc-gecko-addon)
- [FTB Chunks (NeoForge) (by FTB)](https://www.curseforge.com/minecraft/mc-mods/ftb-chunks-forge)
- [Tinker's Construct Crafting Station Fix (by Saereth)](https://www.curseforge.com/minecraft/mc-mods/tinkers-construct-crafting-station-fix)
- [Tinkers' Advanced-Core (by EtSH_C2H6S)](https://www.curseforge.com/minecraft/mc-mods/tinkers-advanced-core)
- [Fusion (Connected Textures) (by SuperMartijn642)](https://www.curseforge.com/minecraft/mc-mods/fusion-connected-textures)
- [In-game NBTEdit Reborn (by InfinRain)](https://www.curseforge.com/minecraft/mc-mods/nbtedit-reborn)
- [Polymorph (Fabric/Forge/Quilt) (by TheIllusiveC4)](https://www.curseforge.com/minecraft/mc-mods/polymorph)
- [Pam's HarvestCraft 2 - Trees (by pamharvestcraft)](https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-trees)
- [Configured (by MrCrayfish)](https://www.curseforge.com/minecraft/mc-mods/configured)
- [AllTheLeaks (Memory Leak Fix) (by Uncandango)](https://www.curseforge.com/minecraft/mc-mods/alltheleaks)
- [GeckolibBetterFPS (by moepus)](https://www.curseforge.com/minecraft/mc-mods/geckolibbetterfps)
- [Shurui's DMZ Tournaments (by Shurui)](https://www.curseforge.com/minecraft/mc-mods/shuruis-dmz-tournaments)
- [Controlling (by Jaredlll08)](https://www.curseforge.com/minecraft/mc-mods/controlling)
- [Abyssal Decor (by starrysock)](https://www.curseforge.com/minecraft/mc-mods/abyssal-decor)
- [Tinkers Integrations and Tweaks (by wendall911)](https://www.curseforge.com/minecraft/mc-mods/tcintegrations)
- [Cosmetic Armor Reworked (by LainMI)](https://www.curseforge.com/minecraft/mc-mods/cosmetic-armor-reworked)
- [Macaw's Doors (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-doors)
- [Apothic Attributes (by Shadows_of_Fire)](https://www.curseforge.com/minecraft/mc-mods/apothic-attributes)
- [FerriteCore ((Neo)Forge) (by malte0811)](https://www.curseforge.com/minecraft/mc-mods/ferritecore)
- [Macaw's Furniture (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-furniture)
- [Macaw's Roofs (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-roofs)
- [Smooth Boot (Reloaded) (by AbdElAziz333)](https://www.curseforge.com/minecraft/mc-mods/smooth-boot-reloaded)
- [FLIB (by Lothrazar)](https://www.curseforge.com/minecraft/mc-mods/flib)
- [FTB Quests (NeoForge) (by FTB)](https://www.curseforge.com/minecraft/mc-mods/ftb-quests-forge)
- [Placebo (by Shadows_of_Fire)](https://www.curseforge.com/minecraft/mc-mods/placebo)
- [Json Things (by gigaherz)](https://www.curseforge.com/minecraft/mc-mods/json-things)
- [DragonMine Z HD Texturepack (by ZoneMC)](https://www.curseforge.com/minecraft/texture-packs/dmz-hd-texturepack)
- [Pam's HarvestCraft 2 - Food Core (by pamharvestcraft)](https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-food-core)
- [Mantle (by mDiyo)](https://www.curseforge.com/minecraft/mc-mods/mantle)
- [Kotlin for Forge (by thedarkcolour)](https://www.curseforge.com/minecraft/mc-mods/kotlin-for-forge)
- [Quests Additions (Forge) (by NaturaSpell)](https://www.curseforge.com/minecraft/mc-mods/quests-additions)
- [Just Enough Items (JEI) (by mezz)](https://www.curseforge.com/minecraft/mc-mods/jei)
- [Corpse x Curios API Compat (by Project8gbDeRam)](https://www.curseforge.com/minecraft/mc-mods/corpse-x-curios-api-compat)
- [TerraBlender (Forge) (by TheAdubbz)](https://www.curseforge.com/minecraft/mc-mods/terrablender)
- [Tinkers Reforged (by Mrthomas20121)](https://www.curseforge.com/minecraft/mc-mods/tinkers-reforged)
- [Macaw's Trapdoors (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-trapdoors)
- [Torohealth Damage Indicator Reformed (by ALFEECLARE)](https://www.curseforge.com/minecraft/mc-mods/torohealth-damage-indicator-reformed)
- [MonoLib (by jason13official)](https://www.curseforge.com/minecraft/mc-mods/monolib)
- [DragonMine Z: Super (by facub8)](https://www.curseforge.com/minecraft/mc-mods/dragonmine-z-super)
- [FTB Library (NeoForge) (by FTB)](https://www.curseforge.com/minecraft/mc-mods/ftb-library-forge)
- [Pam's HarvestCraft 2 - Food Extended (by pamharvestcraft)](https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-food-extended)
- [Macaw's Windows (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-windows)
- [Moonlight Lib (by MehVahdJukaar)](https://www.curseforge.com/minecraft/mc-mods/selene)
- [Custom FoV (by TheIllusiveC4)](https://www.curseforge.com/minecraft/mc-mods/custom-fov)
- [Capsule (by Lythom)](https://www.curseforge.com/minecraft/mc-mods/capsule)
- [Waystones (by BlayTheNinth)](https://www.curseforge.com/minecraft/mc-mods/waystones)
- [Lootr (Forge & NeoForge) (by Noobanidus)](https://www.curseforge.com/minecraft/mc-mods/lootr)
- [Balm (by BlayTheNinth)](https://www.curseforge.com/minecraft/mc-mods/balm)
- [Sophisticated Backpacks (by P3pp3rF1y)](https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks)
- [Jade 🔍 (by Snownee)](https://www.curseforge.com/minecraft/mc-mods/jade)
- [Packet Fixer (by TonimatasDEV)](https://www.curseforge.com/minecraft/mc-mods/packet-fixer)
- [Curios API (Forge/NeoForge) (by TheIllusiveC4)](https://www.curseforge.com/minecraft/mc-mods/curios)
- [Macaw's Lights and Lamps (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-lights-and-lamps)
- [ExtraQuests [FTB Quests] (by Vecoo)](https://www.curseforge.com/minecraft/mc-mods/extraquests)
- [Pam's HarvestCraft 2 - Crops (by pamharvestcraft)](https://www.curseforge.com/minecraft/mc-mods/pams-harvestcraft-2-crops)
- [FTB Teams (NeoForge) (by FTB)](https://www.curseforge.com/minecraft/mc-mods/ftb-teams-forge)
- [Darker Diamonds (by Le1f)](https://www.curseforge.com/minecraft/texture-packs/darker-diamonds)
- [Lightman's Currency (by Lightman314)](https://www.curseforge.com/minecraft/mc-mods/lightmans-currency)
- [Macaw's Stairs (by sketch_macaw)](https://www.curseforge.com/minecraft/mc-mods/macaws-stairs)
- [BaguetteLib (by Project8gbDeRam)](https://www.curseforge.com/minecraft/mc-mods/baguettelib)
- [Framework (by MrCrayfish)](https://www.curseforge.com/minecraft/mc-mods/framework)
- [Sophisticated Storage (by P3pp3rF1y)](https://www.curseforge.com/minecraft/mc-mods/sophisticated-storage)
