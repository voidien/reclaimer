#!/usr/bin/env bash
# Runs a GameTest that has to span a server restart: <name>BeforeRestart in one server, <name>AfterRestart in the next, on the
# same world. The first test either ends normally, so the server stops and saves, or calls RestartTests.crash(), which ends the
# JVM on the spot with nothing saved. These tests are never part of a normal run (GameTestFilter).
#
#   tools/restart-test.sh main aCrashKeepsOnlyWhatWasSaved
#   tools/restart-test.sh aero aHeldShipLoadsItself
set -u
cd "$(dirname "$0")/.."
suite=${1:?main or aero}
name=${2:?the test name, without BeforeRestart}
log=build/gametest-$suite.log

# Each run starts from a fresh world: what a failed run leaves behind, such as forced chunks and entities, would decide the next
# one. Takes the world's lock first, so a run in progress keeps its world.
case "$suite" in
  main) dir=run ;;
  aero) dir=run-aero ;;
  *) echo "unknown suite $suite (main or aero)" >&2; exit 2 ;;
esac
mkdir -p build
(flock 9; rm -rf "$dir/world") 9>"build/gametest-$suite.lock"

echo "== ${name}BeforeRestart"
tools/gametest.sh "$suite" "${name}BeforeRestart"
first=$?
if grep -aq "VR_RESTART_CRASHED" "$log"; then
    echo "the server crashed on purpose"
elif [ "$first" -ne 0 ]; then
    echo "${name}BeforeRestart failed, so there is nothing to check after a restart" >&2
    exit 1
fi
echo "== ${name}AfterRestart"
tools/gametest.sh "$suite" "${name}AfterRestart"
