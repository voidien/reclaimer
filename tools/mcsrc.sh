#!/usr/bin/env bash
# Searches the decompiled Minecraft and NeoForge sources for a real signature instead of guessing one. They are unpacked
# once into build/mc-sources from the jar ModDevGradle already made.
#
#   tools/mcsrc.sh 'waitUntilNextTick'              grep every source file (a basic regex)
#   tools/mcsrc.sh -f GameTestServer                list the files with that name
set -eu
cd "$(dirname "$0")/.."
jar=build/moddev/artifacts/neoforge-21.1.265-sources.jar
src=build/mc-sources
[ -f "$jar" ] || ./gradlew -q createMinecraftArtifacts
[ -d "$src" ] || { mkdir -p "$src"; unzip -q "$jar" -d "$src"; }
if [ "${1:-}" = "-f" ]; then
    find "$src" -name "*$2*"
else
    grep -rn --include='*.java' -e "${1:?usage: tools/mcsrc.sh PATTERN}" "$src" | cut -c1-200
fi
