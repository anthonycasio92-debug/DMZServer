# Claim Block Vouchers (Lightman's + GriefPrevention)

Buy claim blocks with Lightman's Currency coins via a **voucher item**, then
right-click to redeem into GriefPrevention bonus claim blocks.

Lightman's built-in `[compat.claim_purchasing]` only supports FTB Chunks /
Cadmus / Flan — **not** GriefPrevention. Do not enable it for this server.

## How it works

1. Staff mint voucher paper: `/claimvoucher give <player> <blocks> [count]`
2. Put that exact stack in a Lightman's **Item Trader** sell slot
3. Set the coin price in the trader UI
4. Players buy the paper, then **right-click** it
5. Server runs `acb <player> <blocks>` (GriefPrevention `adjustbonusclaimblocks`)

Voucher NBT key: `dmzClaimVoucher` (int = claim blocks granted).

## Suggested denominations

| Voucher | Command |
|---------|---------|
| 100 blocks | `/claimvoucher give <you> 100 64` |
| 500 blocks | `/claimvoucher give <you> 500 64` |
| 1000 blocks | `/claimvoucher give <you> 1000 64` |

Prices are set in Lightman's — pick whatever fits your coin economy.

## Lightman's Item Trader setup

1. Craft / place an **Item Trader** (or Network Trader)
2. Open trader settings as owner
3. Add a **SELL** trade:
   - **Sell item**: one claim voucher paper (from `/claimvoucher give`)
   - **Price**: e.g. `coin;N-lightmanscurrency:coin_iron` (use the trader UI picker)
4. Repeat for 100 / 500 / 1000 (separate trades)
5. Stock the trader inventory with vouchers (or use infinite stock if you use that mode)

Exact minted NBT matters — copy from the give command, do not craft plain paper.

## Reload

After uploading `kubejs/server_scripts/claim_vouchers.js`:

```
/kubejs reload server_scripts
```

No full restart required for script-only changes. Restart still needed if you
also swap jars.

## Staff checks

- `/claimvoucher denoms` — print suggested amounts
- `/acb <player> 0` or GP's claim info tools — confirm bonus blocks rose after redeem
- Permission for `acb` is console-only here (script uses `runCommandSilent`)

## Files

- `kubejs/server_scripts/claim_vouchers.js` — redeem + `/claimvoucher`
