#!/usr/bin/env bash
# Runs a GameTest suite, prints its summary and failures, and kills the server JVM, which usually hangs after the
# tests and would keep the world's session.lock for the next run.
#
#   tools/gametest.sh                      the main suite (runGameTestServer)
#   tools/gametest.sh aero                 the Aeronautics suite (runGameTestAeronautics)
#   tools/gametest.sh aero theHoldsStop    only tests whose class or name contains the term (comma-separated terms)
#   tools/gametest.sh main "" 600          third argument: how wide to print failure lines (default 300)
#
# Exit status is 0 when every test passed. The full log is kept in build/gametest-<suite>.log.
set -u
cd "$(dirname "$0")/.."
suite=${1:-main}
filter=${2:-}
width=${3:-300}
case "$suite" in
  main) task=runGameTestServer; run=gameTestServer ;;
  aero) task=runGameTestAeronautics; run=gameTestAeronautics ;;
  *) echo "unknown suite $suite (main or aero)" >&2; exit 2 ;;
esac
log=build/gametest-$suite.log
mkdir -p build

# The JVM and nothing else: a pattern like `grep gameTest` also matches the shell running this script.
# Only this checkout's and this suite's: another worktree or the other suite may be running at the same time.
killjvm() { ps -eo pid,args | awk -v root="$PWD/build/moddev/$run" '$2 ~ /\/bin\/java$/ && index($0, root) {print $1}' | xargs -r kill; }

killjvm
: > "$log"
if [ -n "$filter" ]; then
  ./gradlew "$task" -PgametestFilter="$filter" > "$log" 2>&1 &
else
  ./gradlew "$task" > "$log" 2>&1 &
fi
gradle=$!
for _ in $(seq 1 450); do
  grep -aq "required tests\|BUILD FAILED\|error:" "$log" && break
  sleep 2
done
sleep 2
grep -aE "required tests|failed at|error:|What went wrong" "$log" | grep -v "DEBUG\|WARN" | cut -c1-"$width" | head -12
# CI has no killer: the server must exit by itself after the summary (GameTestStopMixin bounds the stop loop). Give it
# 40 s and say so when it does not, so a shutdown hang is seen here and not first on CI.
for _ in $(seq 1 20); do kill -0 $gradle 2>/dev/null || break; sleep 2; done
if kill -0 $gradle 2>/dev/null; then echo "WARNING: the server did not exit within 40 s of the summary (killed); CI would hang"; fi
killjvm
wait $gradle 2>/dev/null
grep -aq "required tests passed" "$log"
