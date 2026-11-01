## Tool: one command before a push
- `tools/verify.sh` picks the build, GameTests, datagen and tool tests from what the branch changed, by the same rules as CI's `changes` job, one suite at a time, one line per check. `tools/verify.sh all` runs everything. The `verify` skill (`.claude/skills/verify/`) replaces Claude Code's bundled one.
- **Where the check moved:** a client, test, tool or docs branch skips GameTests before the push. CI already skipped them on such PRs, so they run on `main` after the merge.
