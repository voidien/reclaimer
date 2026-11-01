---
name: verify
description: Run Void Reclaimer's checks before a push, and confirm a change works. Use before every push, and when asked to verify a change.
---

Run `tools/verify.sh` from `/home/void/projects/reclaimer-main`, or `tools/verify.sh all` for everything. It picks the build, GameTests, datagen and tool tests from what the branch changed, by the same rules as CI, and prints one line per check. Run it in the background: it takes about 2 minutes for a gameplay change.

If a check fails, read the log it names and fix the cause. A flaky test gets a root-cause fix, never a rerun. If `runData` changed `src/generated`, commit that.

For a change a player sees, tests are not enough: `tools/client-test/quick.sh start`, then `swap` and `photo NAME` to look at it. See "In-game client checks" in `HANDOFF.md`.
