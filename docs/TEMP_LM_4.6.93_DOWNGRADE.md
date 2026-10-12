# Temporary LegacyMechanics pin: 4.6.93

**Why:** Stock KaiPath does not yet render Ultra GUI packets (10-field `GuiOpenPacket` /
dynamic buttons). LM **4.6.94+** defaults toward Ultra and needs a KaiPath update.

**What this branch is:** the released **4.6.93** hot-path pin (`guiBackend=cnpc`) so the
server works with current KaiPath until JLDK merges the Ultra client kit.

**When KaiPath is ready:** drop this pin; resume **4.6.105+** / Ultra from
`cursor/kaipath-lm-ultra-bridge-fac2` (or main after merge). Client kit:
`tools/ultra-gui-kit/` on that branch.

**Deploy:** `LM_DEPLOY_VERSION=4.6.93 LM_SKIP_GUI=1 DEPLOY_LIVE_CONFIRM=LIVE bash scripts/deploy-lm-live.sh`
(only with explicit LIVE confirm).
