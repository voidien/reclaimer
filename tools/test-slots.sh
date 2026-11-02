#!/usr/bin/env bash
# How many GameTest servers this machine runs at once, across every checkout. The owner's call: the Omen has the room, and every
# other machine runs one at a time. VR_GAMETEST_SLOTS overrides it for a run.
if [ -n "${VR_GAMETEST_SLOTS:-}" ]; then
    echo "$VR_GAMETEST_SLOTS"
    exit
fi
case "$(uname -n)" in
    omen-cachyos) echo 4 ;;
    *) echo 1 ;;
esac
