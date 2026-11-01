#!/usr/bin/env bash
# Tests tools/verified.sh on a throwaway repository: a stack, a merge of main after the check, and new work after it.
set -eu
tool="$(cd "$(dirname "$0")" && pwd)/verified.sh"
repo=$(mktemp -d)
trap 'rm -rf "$repo"' EXIT
cd "$repo"
mkdir tools; cp "$tool" tools/verified.sh
git init -q -b main .
g() { git -c user.name=t -c user.email=t@t "$@"; }
c() { g commit -q --allow-empty -m "$1"; git rev-parse HEAD; }
list=.git/void-reclaimer-verified
check() { # name expected commit
    if tools/verified.sh "$3" main; then got=yes; else got=no; fi
    [ "$got" = "$2" ] || { echo "FAIL $1: wanted $2, got $got"; exit 1; }
}
root=$(c root)
git checkout -q -b low; low=$(c low)
git checkout -q -b top; top=$(c top)
check "nothing has passed" no "$low"
echo "$top" >> "$list"
check "the top of a stack" yes "$top"
check "a branch low in the stack" yes "$low"
git checkout -q main; c more-main > /dev/null
git checkout -q low; g merge -q --no-edit main; merged=$(git rev-parse HEAD)
check "main merged in afterwards" yes "$merged"
extra=$(c new-work)
check "new work after the check" no "$extra"
echo "ok verified.sh"
