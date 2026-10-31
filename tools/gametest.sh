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
  main) task=runGameTestServer ;;
  aero) task=runGameTestAeronautics ;;
  *) echo "unknown suite $suite (main or aero)" >&2; exit 2 ;;
esac
log=build/gametest-$suite.log
mkdir -p build

# The JVM and nothing else: a pattern like `grep gameTest` also matches the shell running this script.
killjvm() { ps -eo pid,args | awk '$2 ~ /\/bin\/java$/ && /moddev\/gameTest/ {print $1}' | xargs -r kill; }

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
killjvm
wait $gradle 2>/dev/null
grep -aq "required tests passed" "$log"
