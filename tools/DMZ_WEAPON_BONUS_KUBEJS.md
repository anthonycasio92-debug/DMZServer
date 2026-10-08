# DMZ Weapon Bonus Scale → KubeJS

Port of `dmzweaponbonusscale.js` (CNPC Global Player) to:

`kubejs/server_scripts/dmz_weapon_bonus_scale.js`

## Install
1. Deploy the KubeJS file
2. `/kubejs reload server_scripts` (or restart)
3. **Disable** the CNPC player-tab script `dmzweaponbonusscale.js` (avoid double apply)

## Behavior
- Multiplicative (`*`) DMZ weapon bonuses only (main / off / hotbar slot 9)
- Recalc on login + once per second (player age divisible by 20)
- Same Simply Swords / Irons / DMZ / Tinkers tables as V9

## Why KubeJS (not mixin)
Logic is inventory tick + BonusStats API — no bytecode hooks required.
