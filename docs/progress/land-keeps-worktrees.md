## Tools: landing keeps worktrees
- `tools/land.sh` deletes the merged branch itself. `gh pr merge --delete-branch` also removed any clean worktree on that branch, which took a whole checkout with its worlds and build.
