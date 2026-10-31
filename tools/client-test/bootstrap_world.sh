#!/usr/bin/env bash
# Creates the headless client's test world "vrtest" (creative, commands on) by clicking through the menus, once.
# Run it after `xstart` and a client launched with `--quickPlaySingleplayer vrtest` (see HANDOFF.md "In-game client
# checks") has reached the "Failed to Quick Play" screen because the world does not exist yet. Coordinates are for
# the 1280x720 Xvfb screen with the game's 853x480 window centred in it.
#
#   source tools/client-test/client.sh; xstart
#   (DISPLAY=:99 nohup ./gradlew runClient > /tmp/client.log 2>&1 &)   # with the quickPlay argument in build.gradle
#   ... wait for the "Failed to Quick Play" screen (about 2 minutes) ...
#   tools/client-test/bootstrap_world.sh
#
# Afterwards the world exists in run/saves/vrtest and quick play joins it directly on every later launch.
set -u
cd "$(dirname "$0")/../.."
source tools/client-test/client.sh
xt click 640 567; sleep 1.5        # a first start shows the accessibility prompt: "Continue" (harmless otherwise)
xt click 640 417; sleep 8          # "Back to World List": with no worlds this opens "Create New World"
xt click 638 236; sleep 0.3        # world name
xt chord ctrl+a; xt type vrtest; sleep 0.3
xt click 638 292; sleep 0.4        # game mode: Survival -> Hardcore
xt click 638 292; sleep 0.6        #            Hardcore -> Creative (turns Allow Commands on)
xt click 480 567; sleep 10         # "Create New World"
xt click 478 415                   # "Yes" to the experimental-settings warning
echo "world creation started; wait for 'joined the game' in the client log"
