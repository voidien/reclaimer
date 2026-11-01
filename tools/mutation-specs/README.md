# Mutation specs for the remote control stack
Saved from the old PC so they are not lost. Parked: not a tool of the project, and the scripts have that PC's paths in them.

Each JSON file is a list of `[file, old text, new text]` faults. `m.sh WORKTREE NAME SUITE FILTER SPEC` applies them, runs the filter, appends which tests failed to `build/audit-mut.txt`, and restores with `git checkout -- src`. A run counts only if the expected test fails on its own assertion: "Didn't succeed or fail" means it ran out of ticks.

Still to run: `mirrorA` split in two (no `dropMirror`; no rebuild of a removed mirror), `wakeB` to `wakeE` (`wakeC` to `wakeE` in the aero suite), `chargeA`, `chargeB`, `pickC`, `pickD`, and everything on `remote-shell`, which has no spec yet.
