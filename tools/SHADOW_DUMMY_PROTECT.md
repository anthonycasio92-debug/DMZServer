# Shadow Dummy spawn protect (KubeJS)

Port of `ShadowDummyForgeProtect.js` (CustomNPCs Global Forge tab) to:

`kubejs/startup_scripts/shadow_dummy_protect_hook.js`

## Why KubeJS (not a mixin)

This logic is pure Forge event handling (join + cancel hurt/damage). A mixin is unnecessary unless KubeJS event order fails in combat. Prefer KubeJS first — same stack as your other DMZ hooks.

## Install

1. Place `shadow_dummy_protect_hook.js` in live `kubejs/startup_scripts/`
2. **Full server restart** (startup Forge hooks only bind on boot)
3. In CustomNPCs, **disable/remove** the Global Forge tab for `ShadowDummyForgeProtect` (avoid double protect)
4. Keep `ShadowDummyLimiter.js` on the **Player** tab

## Behavior (unchanged)

- On player shadow dummy join: 3s invuln + no AI + heal + NBT lock tags
- During protect / limiter lock: cancel `LivingHurtEvent` + `LivingDamageEvent` and re-apply protect state

## Verify

Boot log should show:

`[ShadowDummyProtect] EntityJoinLevel + LivingHurt/Damage registered`
