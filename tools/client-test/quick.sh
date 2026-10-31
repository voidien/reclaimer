#!/usr/bin/env bash
# Fast in-game checks: start the headless client once, then push code changes into the running game instead of restarting it.
#
#   tools/client-test/quick.sh start [world]   start Xvfb and the client (default world vrtest), return when the world has loaded
#   tools/client-test/quick.sh swap            compile, then replace the changed classes in the running game
#   tools/client-test/quick.sh shot NAME       screenshot to build/client-shots/NAME.png
#   tools/client-test/quick.sh cmd "/command"  run a chat command
#   tools/client-test/quick.sh photo NAME [d]  four views (see PhotoScript) joined into build/client-shots/NAME.png, facts printed
#   tools/client-test/quick.sh compare A B     how two shots differ: changed pixels, where, and a picture marking them (build/client-shots/compare.png)
#   tools/client-test/quick.sh gui             with a screen open: print its buttons, click regions and overlaps (F6 outlines them on screen)
#   tools/client-test/quick.sh test NAME       run a GameTest in the live client and print its result, to watch it: the method name, any case
#   tools/client-test/quick.sh gclick X Y      left click at GUI coordinates (the window is 1280x720 at GUI scale 3)
#   tools/client-test/quick.sh gsclick X Y     the same with shift held
#   tools/client-test/quick.sh ghover X Y      move the mouse to GUI coordinates
#   tools/client-test/quick.sh gdrag X Y DX DY left drag at GUI coordinates
#   tools/client-test/quick.sh key KEYSYM      press a key
#   tools/client-test/quick.sh scroll N        scroll the wheel, up when positive
#   tools/client-test/quick.sh stop            close the game
#
# `swap` can change what a method does. Adding, removing or renaming a method or field, or changing a mixin, needs
# `stop` and `start`: it says so. Classes the game has not loaded yet need nothing, they load from build/classes.
# Assets and lang need `stop` and `start` too.
set -u
cd "$(dirname "$0")/../.."
ROOT=$PWD
LOG=build/client-test.log
STAMP=build/client-test.stamp
SHOTS=build/client-shots
export DISPLAY=:99
XT="python3 -I $ROOT/tools/client-test/xt.py"
SCALE=3
PORT=5005

# Only this checkout's game: another worktree may be running its own at the same time.
pid() { ps -eo pid,args | grep "java -Dfml.modFolders" | grep -F "$ROOT/build/" | grep -E "clientRunVmArgs" | grep -v grep | awk '{print $1}'; }

# Hashes of the compiled classes: a recompile that changes nothing must not count as a change.
hashes() { find build/classes/java/main -name '*.class' -exec md5sum {} + | LC_ALL=C sort; }

# The white "Loading terrain..." line, as a share of the strip of screen it sits in. The screen is built for a 1280x720 window.
loading_text() { import -window root -crop 260x28+510+207 +repage png:- 2>/dev/null | convert - -threshold 96% -format '%[fx:mean*1000]' info: 2>/dev/null; }

# A picture with many colours and without the loading line: the world is on screen.
wait_world() {
    local deadline=$((SECONDS + 150)) colours text
    until grep -q "joined the game" "$LOG" 2>/dev/null; do
        if [ -z "$(pid)" ] && [ $SECONDS -gt 60 ]; then echo "the client did not start, see $LOG" >&2; return 1; fi
        if grep -q "BUILD FAILED" "$LOG" 2>/dev/null; then tail -20 "$LOG" >&2; return 1; fi
        if grep -aq "Crash report saved to" "$LOG" 2>/dev/null; then
            grep -aE "Exception message|Failure message|Caused by 0" "run/crash-reports/$(ls -t run/crash-reports | head -1)" | head -4 >&2
            echo "the client crashed while starting, see run/crash-reports; run start again if this is the start-up race in HANDOFF.md" >&2
            return 1
        fi
        [ $SECONDS -lt $deadline ] || { echo "no 'joined the game' after 150 s; a first run needs tools/client-test/bootstrap_world.sh" >&2; return 1; }
        sleep 0.5
    done
    until [ $SECONDS -ge $deadline ]; do
        colours=$(import -window root -resize 160x90 png:- 2>/dev/null | identify -format '%k' - 2>/dev/null)
        text=$(loading_text)
        if [ "${colours:-0}" -gt 50 ] && awk "BEGIN { exit !(${text:-999} < 5) }"; then return 0; fi
        sleep 0.5
    done
    echo "the world did not settle in 150 s, going on" >&2
}

case "${1:-}" in
start)
    world=${2:-vrtest}
    [ -z "$(pid)" ] || { echo "already running: $(pid)"; exit 0; }
    pgrep -x Xvfb >/dev/null || { Xvfb :99 -screen 0 1280x720x24 >/dev/null 2>&1 & sleep 1; }
    mkdir -p build "$SHOTS"
    : > "$LOG"
    ./gradlew classes -q || exit 1
    hashes > "$STAMP"
    SECONDS=0
    nohup ./gradlew runClient -PquickPlay="$world" -PclientSize=1280x720 -PhotSwap >> "$LOG" 2>&1 &
    wait_world && echo "in the world after ${SECONDS}s"
    ;;
swap)
    SECONDS=0
    ./gradlew classes -q || exit 1
    hashes > "$STAMP.new"
    changed=$(LC_ALL=C comm -13 "$STAMP" "$STAMP.new" | awk '{print $2}')
    [ -n "$changed" ] || { echo "nothing changed"; exit 0; }
    script=$(mktemp)
    while IFS= read -r file; do
        class=${file#build/classes/java/main/}
        class=${class%.class}
        printf 'redefine %s %s\n' "${class//\//.}" "$ROOT/$file" >> "$script"
    done <<< "$changed"
    # A class the game has not loaded yet answers with an error here: harmless, it loads from build/classes when first used.
    out=$( { sleep 0.7; cat "$script"; sleep 0.8; echo quit; } | jdb -attach 127.0.0.1:$PORT 2>&1 )
    rm -f "$script"
    if grep -qiE 'not implemented|hierarchy|verify|format error|unsupported' <<< "$out"; then
        grep -iE 'not implemented|hierarchy|verify|format error|unsupported' <<< "$out" | head -5
        echo "this change cannot be swapped in: use stop and start" >&2
        exit 1
    fi
    if grep -q 'Unable to attach\|Connection refused' <<< "$out"; then echo "no game to attach to: use start" >&2; exit 1; fi
    mv "$STAMP.new" "$STAMP"
    echo "swapped $(wc -l <<< "$changed") classes in ${SECONDS}s"
    ;;
shot)
    mkdir -p "$SHOTS"
    import -window root "$SHOTS/$2.png" && echo "$ROOT/$SHOTS/$2.png"
    ;;
cmd)
    $XT key t; sleep 0.4; $XT type "$2"; sleep 0.2; $XT key Return; sleep 0.6
    ;;
photo)
    shots=run/screenshots
    mkdir -p "$SHOTS"
    # A sheet is fresh when its last file is newer than this moment; an older sheet of the same name is overwritten.
    touch build/client-photo.stamp
    sleep 0.05
    $XT key t; sleep 0.4; $XT type "/vrphoto sheet $2 ${3:-0}"; sleep 0.1; $XT key Return
    for _ in $(seq 1 100); do [ "$shots/$2_top.txt" -nt build/client-photo.stamp ] && break; sleep 0.2; done
    [ "$shots/$2_top.txt" -nt build/client-photo.stamp ] || { echo "no sheet appeared" >&2; exit 1; }
    sleep 0.4
    montage "$shots/$2_front.png" "$shots/$2_side.png" "$shots/$2_three_quarter.png" "$shots/$2_top.png" -tile 2x2 -geometry 640x360+1+1 "$SHOTS/$2.png"
    echo "$ROOT/$SHOTS/$2.png"
    cut -c1-400 "$shots/$2_front.txt"
    ;;
compare)
    # Names are shots in build/client-shots or run/screenshots, or paths. A little fuzz ignores dithering and sky specks.
    find_shot() { for f in "$1" "$SHOTS/$1.png" "run/screenshots/$1.png"; do [ -f "$f" ] && { echo "$f"; return; }; done; echo "no shot called $1" >&2; exit 2; }
    a=$(find_shot "$2") || exit 2
    b=$(find_shot "$3") || exit 2
    mkdir -p "$SHOTS"
    pixels=$(magick compare -metric AE -fuzz "${4:-3}%" -highlight-color red -lowlight-color '#00000040' "$a" "$b" "$SHOTS/compare.png" 2>&1 | awk '{printf "%d", $1}')
    if [ "${pixels:-0}" = "0" ]; then echo "identical (within ${4:-3}% colour difference)"; exit 0; fi
    size=$(magick identify -format '%wx%h' "$a")
    area=$(magick "$a" "$b" -compose difference -composite -colorspace gray -threshold "${4:-3}%" -trim -format '%wx%h%O' info: 2>/dev/null)
    echo "$pixels pixels differ, of a $size picture, inside $area"
    echo "$ROOT/$SHOTS/compare.png"
    ;;
gui)
    # F6 twice leaves the outlines as they were and writes the list for the screen open now, whichever way round they started.
    report=run/debug/gui.txt
    $XT key F6; sleep 0.15; $XT key F6; sleep 0.3
    cat "$report"
    ;;
test)
    # Slower than tools/gametest.sh and for the odd look only. Tests at far coordinates need the camera there: /vrphoto focus x y z.
    name=$(tr '[:upper:]' '[:lower:]' <<< "$2")
    lines=$(wc -l < "$LOG")
    $XT key t; sleep 0.4; $XT type "/test run $name"; sleep 0.1; $XT key Return
    for _ in $(seq 1 ${3:-150}); do
        tail -n +"$lines" "$LOG" | grep -aqE "CHAT\] .*($name|No such test)|$name failed at" && break
        sleep 0.2
    done
    tail -n +"$lines" "$LOG" | grep -aE "CHAT\] .*(test|Test)|failed at" | sed -E 's/^.*(CHAT\] |LogTestReporter\/\]: )//' | head -6 | cut -c1-200
    ;;
gclick) $XT click $(($2 * SCALE)) $(($3 * SCALE)) ;;
gsclick) $XT sclick $(($2 * SCALE)) $(($3 * SCALE)) ;;
ghover) $XT hover $(($2 * SCALE)) $(($3 * SCALE)) ;;
gdrag) $XT drag $(($2 * SCALE)) $(($3 * SCALE)) $(($4 * SCALE)) $(($5 * SCALE)) ;;
key) $XT key "$2" ;;
scroll) $XT scroll "$2" ;;
stop)
    p=$(pid)
    [ -z "$p" ] || kill "$p"
    ;;
*)
    sed -n '2,21p' "$0" | sed 's/^# \?//'
    exit 2
    ;;
esac
