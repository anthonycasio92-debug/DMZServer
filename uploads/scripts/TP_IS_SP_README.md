# TP IS SP Fabled

Mirrors DMZ Training Points into Fabled skill points (capped at 2,147,483,647 for display).

## Spend detection
TP is only removed when `getInvestedSkillPoints()` increases (a real skill purchase).
Raw SP drops without invested-cost change are treated as reload glitches and remirrored — they do **not** wipe TP.

This matters for Prestige skills costing up to 2,000,000,000 SP and Jobs skills costing 10,000 SP.

## Jobs
Building / Farming / Fishing: `type: Jobs`, first unlock cost 10000 SP (= 10000 TP).
