#!/usr/bin/env bash
# The checks to run before a push, chosen from what this branch changed by the same rules as CI's `changes` job, one suite at a
# time. One line per check; the full log of a failure is in build/verify-<check>.log. Exit status 0 means push.
#
#   tools/verify.sh          what this branch needs
#   tools/verify.sh all      everything, whatever changed
set -u
cd "$(dirname "$0")/.."
# Two runs in one checkout overwrite each other's logs and builds.
mkdir -p build
exec 8>build/verify.lock
flock -n 8 || { echo "another tools/verify.sh is running in this checkout" >&2; exit 2; }
git fetch -q origin 2>/dev/null
base=$(git merge-base origin/main HEAD)
changed=$( { git diff --name-only "$base"; git ls-files --others --exclude-standard; } | sort -u)
mkdir -p build
start=$SECONDS
failed=0

run() { # name command...
    local name=$1 at=$SECONDS
    shift
    if "$@" > "build/verify-$name.log" 2>&1 8>&-; then
        echo "ok    $name $((SECONDS - at))s"
    else
        echo "FAIL  $name $((SECONDS - at))s, see build/verify-$name.log"
        grep -aE "failed at|error:|What went wrong" "build/verify-$name.log" | grep -v "DEBUG\|WARN" | cut -c1-200 | head -5
        if grep -aq "exit value 134\|fatal error has been detected" "build/verify-$name.log"; then echo "      the JVM crashed, not a test: run it again (HANDOFF.md, \"JVM crash\")"; fi
        failed=1
    fi
}

touches() { grep -qE "$1" <<< "$changed"; }

# Mirrors the grep in .github/workflows/build.yml: these paths cannot change server behaviour or generated data.
server=$(grep -Ev '^(src/test/|tools/|docs/|art/|src/main/java/com/voidreclaimer/client/|src/main/resources/assets/|.*\.md$)' <<< "$changed" || true)
code=$(grep -Ev '^(docs/|art/|.*\.md$)' <<< "$changed" || true)

if [ "${1:-}" = "all" ] || [ -n "$code" ]; then
    run build ./gradlew build
fi
if [ "${1:-}" = "all" ] || [ -n "$server" ]; then
    run main tools/gametest.sh main
    run aero tools/gametest.sh aero
else
    echo "skip  GameTests: nothing here reaches the server (CI skips them too, and main runs them after the merge)"
fi
if [ "${1:-}" = "all" ] || touches '^src/main/java/com/voidreclaimer/(datagen|registry)/|^src/generated/'; then
    run data timeout 300 ./gradlew runData
    if ! git diff --quiet -- src/generated; then echo "NOTE  runData changed src/generated: commit it"; fi
fi
if [ "${1:-}" = "all" ] || touches '^tools/art/'; then
    run art bash -c 'python3 tools/art/test_rd_generate.py && python3 tools/art/test_rig_check.py && python3 tools/art/test_anim_slice.py'
fi
if [ "${1:-}" = "all" ] || touches '^tools/progress_fold|^docs/progress/'; then
    run progress python3 tools/test_progress_fold.py
fi
echo "done in $((SECONDS - start))s"
exit $failed
