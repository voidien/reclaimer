## CI runs on main, not on pull requests
- The owner's rule: a PR merges once `tools/verify.sh` passes, with no wait. CI runs the full set after the merge, on a clean machine with real saves. `tools/land.sh` merges at once. The `changes` job and the draft and partial runs have gone with the pull request trigger. `gh workflow run build.yml --ref <branch>` still runs it by hand on a risky branch.
