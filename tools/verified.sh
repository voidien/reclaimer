#!/usr/bin/env bash
# Whether commit $1 has passed tools/verify.sh: every commit of its own that main lacks is in a commit that passed. So a branch low in
# a stack counts once the top of the stack has passed, and merging main into a branch afterwards does not undo it. Exit status 0 if so.
#
#   tools/verified.sh COMMIT [BASE]     BASE is what counts as already checked, origin/main by default
set -u
cd "$(dirname "$0")/.."
head=${1:?usage: tools/verified.sh COMMIT [BASE]}
base=${2:-origin/main}
list="$(git rev-parse --git-common-dir)/void-reclaimer-verified"
[ -f "$list" ] || exit 1
while read -r sha; do
    git cat-file -e "$sha" 2>/dev/null || continue
    [ -z "$(git rev-list --no-merges "$head" "^$sha" "^$base")" ] && exit 0
done < <(tail -n 100 "$list")
exit 1
