# Head Parts Shop — cross-race catalog scan

Generated from **live** `config/dragonminez/races/*/character.json` (2026-09-17) and `dragonminez-2.1.3.jar` string table.

Legacy Mechanics builds the shop from the **union** of every race’s `headBones[]` (`CosmeticHeadBoneCatalog`).  
`legacymechanics.json` → `characterServices.headBoneShop.excludeBones` on live: **none** (empty).

## Shop-eligible parts today (12)

| Bone id | Native races (free with `nativeRaceBonesFree`) | Other races unlock via shop |
|---------|-----------------------------------------------|-----------------------------|
| `hair` | ancient_saiyan, human, monkey, saiyan, sento_saiyan, viltrumite | bioandroid, frostdemon, majin, namekian |
| `ears1` | namekian, monkey | all other 8 races |
| `ears2` | namekian, monkey | all other 8 races |
| `ears3` | namekian, monkey | all other 8 races |
| `horns1` | frostdemon, monkey | all other 8 races |
| `horns2` | frostdemon, monkey | all other 8 races |
| `horns3` | frostdemon, monkey | all other 8 races |
| `horns4` | frostdemon, monkey | all other 8 races |
| `horns5` | frostdemon, monkey | all other 8 races |
| `majin1` | majin | all other 9 races |
| `majin2` | majin | all other 9 races |
| `majin3` | majin | all other 9 races |

## Per-race `headBones[]` on live

| Race | headBones | hasSaiyanTail |
|------|-----------|---------------|
| ancient_saiyan | hair | true |
| bioandroid | *(empty)* | false |
| frostdemon | horns1–horns5 | false |
| human | hair | false |
| majin | majin1–majin3 | false |
| monkey | hair, ears1–3, horns1–5 | true |
| namekian | ears1–3 | false |
| saiyan | hair | true |
| sento_saiyan | hair | true |
| viltrumite | hair | false |

## Not head-bone shop items

| Feature | How it works |
|---------|----------------|
| **Saiyan tail** | `hasSaiyanTail` on character race config — not `activeHeadBone` |
| **Frost / bio tails in models** | Geo bones `tail1`–`tail9` exist on some models but are **not** registered as `headBones` ids in DMZ 2.1.3 |
| **Reskin editor** | Hair style, eyes, mouth, body type — separate from head-bone shop (`/lm` reskin → DMZ recustomize) |

## DMZ mod: known `activeHeadBone` ids (jar)

From `dragonminez-2.1.3.jar` embedded ids (same 12 as live config):

`hair`, `ears1`–`ears3`, `horns1`–`horns5`, `majin1`–`majin3`

No additional head-bone ids were found in the mod binary beyond this set.

## How to add more shop parts

1. Confirm the bone id exists on the DMZ model (must be a valid `activeHeadBone` for that race mesh).
2. Add the id to `headBones` for at least one race in `config/dragonminez/races/<race>/character.json`.
3. Optional: `characterServices.headBoneShop.displayNames` / `boneCosts` / `excludeBones` in `legacymechanics.json`.
4. `/lm admin reload` — catalog refreshes from disk.

**Candidate experiments (verify in-game before selling):** add `tail1`–`tail5` to frostdemon or bioandroid `headBones[]` if DMZ accepts them as `activeHeadBone` (models include tail bones; shop/LM do not block unknown ids if catalog lists them).
