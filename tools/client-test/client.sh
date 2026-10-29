#!/usr/bin/env bash
# Headless client helpers for in-game checks (see HANDOFF.md). Source this file: `source tools/client-test/client.sh`.
# Needs Xvfb, ImageMagick `import`, libX11 and libXtst.
XT="python3 -I $(dirname "${BASH_SOURCE[0]}")/xt.py"

# Starts Xvfb :99 (1280x720) if it isn't running.
xstart() { pgrep -x Xvfb >/dev/null || (Xvfb :99 -screen 0 1280x720x24 >/dev/null 2>&1 &); sleep 1; }

# Screenshot to the given png path.
shot() { DISPLAY=:99 import -window root "$1"; }

# Runs a chat command in game, e.g. run "/gamemode survival".
run() { $XT key t; sleep 0.7; $XT type "$1"; sleep 0.3; $XT key Return; sleep 1.2; }

# PID of the running game JVM. Kill it by PID only: `pkill -f` patterns also match and kill your own shell.
clientpid() { ps -eo pid,args | grep "java -Dfml.modFolders" | grep -v grep | awk '{print $1}'; }

xt() { $XT "$@"; }
