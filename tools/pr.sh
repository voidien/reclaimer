#!/usr/bin/env bash
# Opens a pull request for the current branch against main (the repository's default branch is not main, so a bare
# `gh pr create` would target the wrong base). Usage: tools/pr.sh "title" "body"
set -eu
cd "$(dirname "$0")/.."
branch=$(git branch --show-current)
[ "$branch" != "main" ] || { echo "on main; branch first" >&2; exit 2; }
git push -q -u origin "$branch"
gh pr create --base main --title "$1" --body "$2"
