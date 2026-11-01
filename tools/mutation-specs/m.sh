#!/usr/bin/env bash
# m.sh WORKTREE NAME SUITE FILTER SPEC.json: applies deliberate faults, runs the filter, prints which tests failed, restores with git.
W=$1; N=$2; SUITE=$3; F=$4; SPEC=$5
cd "$W" || exit 9
R=$W/build/audit-mut.txt
python3 - "$SPEC" <<'P' || { echo "--- $N: spec did not apply" >> "$R"; git checkout -- src; exit 8; }
import sys,json
for p,a,b in json.load(open(sys.argv[1])):
    t=open(p).read(); assert a in t,(p,a[:60]); open(p,'w').write(t.replace(a,b))
P
tools/gametest.sh "$SUITE" "$F" > build/audit-$N.out 2>&1; rc=$?
{ echo "--- $N rc=$rc"; grep -aE 'failed at|required tests|BUILD FAILED|error:' build/audit-$N.out | sed -E 's/ failed at [-0-9, ]+!/:/' | cut -c1-120; } >> "$R"
git checkout -- src
