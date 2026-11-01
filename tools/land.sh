#!/usr/bin/env bash
# Waits for a pull request's CI, merges it when green and moves the primary checkout to the new main. Run it in the
# background and keep working: tools/land.sh 181
#
# Exit status is 0 once merged. A failed check, a draft or a PR that cannot merge exits non-zero and merges nothing.
set -u
cd "$(dirname "$0")/.."
pr=${1:?usage: tools/land.sh PR_NUMBER}

[ "$(gh pr view "$pr" --json isDraft -q .isDraft)" = "false" ] || { echo "PR $pr is a draft" >&2; exit 2; }

# The workflow starts a few seconds after a push, and a PR that only touches its paths-ignore list never starts it.
checks=""
for _ in $(seq 1 12); do
    checks=$(gh pr checks "$pr" --json name -q 'length' 2>/dev/null)
    [ "${checks:-0}" -gt 0 ] && break
    sleep 5
done
if [ "${checks:-0}" -gt 0 ]; then
    gh pr checks "$pr" --watch --fail-fast --interval 20 > /dev/null 2>&1
    gh pr checks "$pr" | cut -f1,2,3 | column -t -s $'\t'
    failed=$(gh pr checks "$pr" --json bucket -q '[.[] | select(.bucket == "fail" or .bucket == "cancel" or .bucket == "pending")] | length')
    [ "$failed" = "0" ] || { echo "PR $pr is not green" >&2; exit 1; }
else
    code=$(gh pr diff "$pr" --name-only | grep -Ev '\.md$|^docs/|^art/' || true)
    [ -z "$code" ] || { echo "PR $pr has no checks, but changes code: $code" >&2; exit 1; }
    echo "PR $pr changes only docs, so it has no checks"
fi

gh pr merge "$pr" --merge --delete-branch || { echo "PR $pr did not merge" >&2; exit 1; }

# Claude Code starts in the first worktree and loads its CLAUDE.md: a stale one means old rules.
primary=$(git worktree list --porcelain | sed -n '1s/^worktree //p')
git fetch -q origin
if git -C "$primary" diff --quiet HEAD; then
    git -C "$primary" checkout -q --detach origin/main && echo "primary checkout at $(git -C "$primary" rev-parse --short HEAD)"
else
    echo "primary checkout has local changes, left where it is" >&2
fi
