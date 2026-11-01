#!/usr/bin/env bash
# Runs a command with a time limit and, just before the limit, prints a thread dump of each JVM matching PATTERN: a hang is a bug to
# read, not a run to try again (rule 8). Exits with the command's status, 124 on a hang. Usage: tools/timed.sh SECONDS PATTERN command...
limit=$1
pattern=$2
shift 2
( sleep $((limit > 15 ? limit - 15 : 1))
  ps -eo pid,args | awk -v want="$pattern" '$2 ~ /\/bin\/java$/ && $0 ~ want {print $1}' | while read -r pid; do
      echo "::group::Thread dump of the hung run"; jstack -l "$pid"; echo "::endgroup::"
  done ) &
dump=$!
status=0
timeout "$limit" "$@" || status=$?
# The dump may be over already, and a CI step runs with -e: a failed kill must not hide the result.
kill "$dump" 2>/dev/null || true
exit "$status"
