# TP IS SP Fabled

Jobs skills (Building / Farming / Fishing) cost **10000 SP**, which is **10000 DMZ TP** via this sync.

## Bug fixed
Spend detection treated a temporary Fabled `getPoints()==0` (reload/race) as spending all SP, which wiped DMZ TP and left players unable to buy 10k-cost Jobs skills.

Guards now:
- Ignore SP drops to 0 while TP is still high (desync)
- Cap SP→TP debit per tick at 50,000,000
