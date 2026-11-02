## Tools: landing checks the commit being merged
- Right after a push GitHub's API can still give the PR's old head, so `tools/land.sh` checked a commit that had passed and merged one that had not, which is how #327 landed on a failed verify. It now reads the head from git and tells GitHub to merge only that commit. Main passed 445 of 445 twice after it, so nothing was broken.
- `respawnAtChosenCoreSpendsACharge` failed once under load because the core wasn't placed, and it didn't check. It now says why if that happens again.
