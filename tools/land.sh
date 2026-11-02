#!/usr/bin/env bash
# Merges a pull request that has passed tools/verify.sh and moves the primary checkout to the new main. CI runs on main after the
# merge, not before it: read it with `gh run list --branch main --limit 1`. Usage: tools/land.sh 181
#
# Exit status is 0 once merged. A draft, a PR that has not passed tools/verify.sh or one that cannot merge exits non-zero and merges
# nothing.
set -u
cd "$(dirname "$0")/.."
pr=${1:?usage: tools/land.sh PR_NUMBER}

[ "$(gh pr view "$pr" --json isDraft -q .isDraft)" = "false" ] || { echo "PR $pr is a draft" >&2; exit 2; }

# CI no longer stands between a branch and main, so this does. A docs-only PR has nothing to check.
git fetch -q origin "pull/$pr/head"
if [ -n "$(gh pr diff "$pr" --name-only | grep -Ev '\.md$|^docs/|^art/' || true)" ] && ! tools/verified.sh "$(gh pr view "$pr" --json headRefOid -q .headRefOid)"; then
    echo "PR $pr has not passed tools/verify.sh here: run it on this branch, or on a stack above it" >&2
    exit 3
fi

# Not --delete-branch: that also removes any clean worktree on the branch, with its worlds and build, even one somebody is using.
branch=$(gh pr view "$pr" --json headRefName -q .headRefName)
gh pr merge "$pr" --merge || { echo "PR $pr did not merge" >&2; exit 1; }
git push -q origin --delete "$branch" 2>/dev/null || echo "branch $branch is still on origin" >&2

# Claude Code starts in the first worktree and loads its CLAUDE.md: a stale one means old rules.
primary=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
git fetch -q origin
if git -C "$primary" diff --quiet HEAD; then
    git -C "$primary" checkout -q --detach origin/main && echo "primary checkout at $(git -C "$primary" rev-parse --short HEAD)"
else
    echo "primary checkout has local changes, left where it is" >&2
fi
