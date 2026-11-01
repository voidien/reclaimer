#!/usr/bin/env bash
# Merges a pull request that has passed tools/verify.sh and moves the primary checkout to the new main. CI runs on main after the
# merge, not before it: read it with `gh run list --branch main --limit 1`. Usage: tools/land.sh 181
#
# Exit status is 0 once merged. A draft or a PR that cannot merge exits non-zero and merges nothing.
set -u
cd "$(dirname "$0")/.."
pr=${1:?usage: tools/land.sh PR_NUMBER}

[ "$(gh pr view "$pr" --json isDraft -q .isDraft)" = "false" ] || { echo "PR $pr is a draft" >&2; exit 2; }

gh pr merge "$pr" --merge --delete-branch || { echo "PR $pr did not merge" >&2; exit 1; }

# Claude Code starts in the first worktree and loads its CLAUDE.md: a stale one means old rules.
primary=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
git fetch -q origin
if git -C "$primary" diff --quiet HEAD; then
    git -C "$primary" checkout -q --detach origin/main && echo "primary checkout at $(git -C "$primary" rev-parse --short HEAD)"
else
    echo "primary checkout has local changes, left where it is" >&2
fi
