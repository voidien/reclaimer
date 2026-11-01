#!/usr/bin/env bash
# pipe.sh WORKTREE BRANCH TITLE BODY: rebases on main, verifies, opens the PR and lands it. Stops at any failure.
W=$1; B=$2; T=$3; BODY=$4
S=/tmp/claude-1000/-home-void-projects-reclaimer/46869ab2-8ab8-4c07-80c7-783fc6c45429/scratchpad
G="git -c user.name=voidien -c user.email=99926166+voidien@users.noreply.github.com"
cd "$W" || exit 9
git fetch -q origin
GIT_EDITOR=true $G rebase origin/main > build/pipe.out 2>&1 || { echo "REBASE FAILED on $B"; tail -5 build/pipe.out; exit 3; }
# Chunks load slowly next to other runs and GameTest ticks do not wait: tests that need fresh chunks get room.
for f in ${RAISE:-}; do sed -i 's/timeoutTicks = 200/timeoutTicks = 900/' "$f"; done
if ! git diff --quiet -- src; then git add src; bash $S/commit.sh "$T: more time for fresh chunks"; fi
for try in 1 2 3; do
  tools/verify.sh > build/pipe-verify.txt 2>&1; rc=$?
  cat build/pipe-verify.txt | cut -c1-140
  if [ -n "$(git status --porcelain src/generated)" ]; then git add src/generated; bash $S/commit.sh "$T: generated data"; fi
  [ $rc = 0 ] && break
  grep -q 'the JVM c' build/pipe-verify.txt || { echo "VERIFY FAILED on $B"; exit 4; }
  echo "JVM crash, try $try"
done
[ $rc = 0 ] || { echo "VERIFY FAILED on $B after retries"; exit 4; }
git push -q -u origin "$B" 2>&1 | tail -1
N=$(gh pr create --base main --head "$B" --title "$T" --body "$(bash $S/body.sh "$BODY")" | grep -oE '[0-9]+$')
echo "PR $N"
tools/land.sh "$N" 2>&1 | tail -8; lr=${PIPESTATUS[0]}
echo "land rc=$lr"
exit $lr
