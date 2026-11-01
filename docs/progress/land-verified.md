## `land.sh` asks whether a branch passed
- CI no longer gates a merge, so `tools/verify.sh` records each commit that passes on a clean tree and `tools/land.sh` refuses a code PR with work that has not passed, here or on a stack above it (`tools/verified.sh`, tested by `tools/test_verified.sh`). Docs-only PRs need nothing.
