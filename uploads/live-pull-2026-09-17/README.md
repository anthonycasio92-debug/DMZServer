# Live pull — 2026-09-17

Read-only SFTP from production (`config/legacymechanics/`).

| Item | Value |
|------|--------|
| Live jars | `LegacyMechanics-2.4.45.jar`, `LegacyMechanicsGUI-2.4.45.jar` |
| Config | `legacymechanics.json` (root of this folder) |
| Combat hits | `telemetry/hits-*.jsonl` (21 files) |
| System events | `telemetry/systems-*.jsonl` (20 files) |
| Total size | ~74 MB |

## Quick summary

```bash
python3 tools/dmz-adaptive-difficulty/sim/summarize_telemetry.py --dir uploads/live-pull-2026-09-17/telemetry
python3 tools/dmz-adaptive-difficulty/sim/audit_retune_readiness.py --dir uploads/live-pull-2026-09-17/telemetry
```

**62,864** combat hits (Aug 9 – Sep 17, 2026).  
`staffFreeAncientCoinCosts`: **false** on live at pull time.
