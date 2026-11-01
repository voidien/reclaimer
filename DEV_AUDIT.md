# Dev environment audit

First audit: 2026-10-31, on `main` at 081141a. The owner approved all findings on 2026-10-31, on condition that each measurably helps; item status is under "Status".

## Context (reuse on the next audit)
- **Machine:** Ryzen 7 5700U (8 cores, 16 threads), 30 GB RAM, NVMe with btrfs (zstd, noatime), CachyOS Linux 7.3. Temurin 21.0.13, also Java 27 (Gradle 9.2.1 does not run on it). No IDE found on the PATH; work runs through the terminal and Claude Code.
- **Build:** Gradle 9.2.1, ModDevGradle 2.0.148, NeoForge 21.1.265, Parchment 2024.11.17. 412 main classes (about 44k lines, 55 of them GameTest files), 67 unit test classes, 26 lines of mixin config.
- **CI:** GitHub Actions, public repo (free runners), `main` not protected, auto-merge off.
- **Your answers:** slowest are local verify before push and waiting on CI. You playtest from checklists. You restart the client 6+ times a session. For approved items, the normal rules apply: small PRs, and I merge my own on green CI.

## Status
- F1: done (`tools/land.sh`).
- F2: done (`docs/progress/`, `tools/progress_fold.py`). Pays off as fewer merges of `main`: check the count on the next audit (34 for 115 PRs before).
- Everything else: in progress.

## Top 3 changes by payoff
1. **Land PRs without waiting (F1).** A small script watches CI, merges on green and refreshes the primary checkout. Today a green PR waits until you next write, and the primary checkout, which sets the `CLAUDE.md` a session starts with, is 67 commits behind.
2. **Stop the docs conflicts (F2).** 34 merges of `main` into branches for 115 PRs since 10-29: about 3 in 10 PRs. `PROGRESS.md`, `CLAUDE.md` and `HANDOFF.md` are the three most-touched files. Each merge means another full CI run.
3. **Cut full CI from 7.8 to about 4.5 minutes (F5, F6, F7).** Find the slow GameTest batch, then split the build job. The same tests run; they just run side by side.

## Where a typical hour goes (estimate)
Assumes a busy hour of three PRs. CI really did run about 8 times an hour across the 12.6-hour window of the last 100 runs. CI time mostly overlaps with other work, so it delays merges more than it blocks typing.

| Activity | Minutes |
|---|---|
| Local full verify (3 at 3.5 min) | 10 |
| Filtered GameTests while iterating (10 at 30 s) | 5 |
| Unit test runs (10 at 20 s) | 3 |
| Conflict repair and repeat verify (about 1 in 3 PRs) | 4 |
| Waiting for CI, elapsed and overlapping (3 at 8 min) | 24 |
| Client restarts, yours (about 3 at 1 min) | 3 |
| Reading, writing and thinking | the rest |

In the last 100 runs, 185 of 575 run-minutes (32%) went to failed or cancelled runs. That has improved: the last 30 runs had 1 failure and 4 cancels.

## Findings
Ranked by minutes saved ÷ effort. Minutes saved are estimates for a busy hour; evidence is measured. 

| # | Issue | Evidence | Fix | Est. min/h | Effort | Quality risk |
|---|---|---|---|---|---|---|
| F1 | Green PRs wait for someone to notice; primary checkout goes stale | `HANDOFF.md` says to check CI "the next time the owner writes". `reclaimer` is at #152, 67 commits behind `origin/main` | `tools/land.sh N`, run in the background: `gh pr checks N --watch`, merge on green, then move the primary checkout to `origin/main` | 5 | 15 min | None: the green CI gate is unchanged |
| F2 | Shared docs conflict | 34 merges of `main` for 115 PRs. Since 10-29 `PROGRESS.md` was touched in 109 commits, `CLAUDE.md` in 81, `HANDOFF.md` in 66. GitHub ignores `merge=union` | Each PR adds its own file, `docs/progress/<slug>.md`; a small script folds them into `PROGRESS.md`. `CLAUDE.md` only changes when a rule changes | 5 | 30 min | Low: docs still ship in the same PR |
| F3 | Permission allowlist is one entry | `.claude/settings.json` allows only `gh pr merge` | Allow `./gradlew`, `tools/*.sh` and read-only git. You did not report stalls, so this is cheap insurance | 1 | 5 min | Low |
| F4 | One local verify does everything every time, by hand | Build 21 s + main 110 s + aero 52 s = about 3 min, plus `runData` 18 s. CI already skips GameTests for client, test, tool and docs PRs | `tools/verify.sh` picks the checks from the diff with the same rules as CI's `changes` job, and prints one result line. Expose it as a project `verify` skill | 4 | 30 min | **Check moves:** for client-only PRs, GameTests move from pre-push to the post-merge `main` CI run. The rest stay |
| F5 | One GameTest batch takes about 45% of the suite | Alone: batches 16, 6, **35**, 7 and 13 s. A batch lasts as long as its slowest test. Under load on CI it costs proportionally more | Log per-test time, find the test, fix its wait or give it its own batch. Never loosen it | 3 | 30 min | Low: same tests, same assertions |
| F6 | Restart tests sit in the long CI job | Build job 7.8 min: GameTests 256 s, crash tests 107 s, build 52 s, datagen 26 s | Give the restart tests their own job | 2 | 15 min | None: same steps. Saves about 1.8 min per full run |
| F7 | Main GameTests run in one job | 256 s of the 7.8 min; 246 tests | Two shards via a `-PgametestShard` property in `GameTestRegistryMixin`. Full run about 4.5 min with F5 and F6 | 3 | 1-2 h | Low: every test still runs. Shared-state races may surface, which is useful |
| F8 | Assets are documented as needing a restart | `quick.sh:22`. But `GeckoLibCache.registerReloadListener` exists in the 4.9.3 jar, and our `BodyTextures` and `BodyRigSpec.Loader` are reload listeners (`BodyRenderEvents.java:49-50`) | Test F3+T after `processResources`; add `quick.sh reload`. **Not yet proven in a client** | 3 | 15 min | None |
| F9 | Your own client can't take hot swaps easily | `quick.sh` is built around Xvfb and `start` writes the swap's baseline. `-PhotSwap` exists but the windowed path is undocumented | `quick.sh start --window` so a visible game gets `swap` in about 3 s. Method-body changes stop costing a restart | 4 | 30 min | None; you still check in game |
| F10 | One unit test costs the same as all of them | `test` is 19.9 s of the 21 s build; 369 tests sum to 1.65 s; a single test run is 20 s. Almost all of it is booting the game | Add a `fastTest` task without the FML launcher for tests that don't need the game (27 of 67 classes import game classes; only 1 matched a bootstrap or registry search). **Needs a spike** | 3 | 45 min | None if `test` stays in `build` and CI |
| F11 | No note that decompiled sources can be searched | `build/moddev/artifacts/neoforge-21.1.265-sources.jar` exists; `CLAUDE.md` and `HANDOFF.md` only mention `javap` | Add a line and a one-line grep helper | 1 | 10 min | None |
| F12 | `CLAUDE.md` is 138 KB, about 34k tokens | Official target is under 200 lines; longer files "consume more context and reduce adherence". Ours has 670 very long lines | Keep rules, commands and an index. Move settled design to `docs/design/*.md`, with path-scoped rules where they fit | 3 | 2 h | Medium: a rule could be missed. Move text verbatim, one PR per topic |
| F13 | Repeated new-block work | A new block needs registration, datagen, lang (`ModLangProvider` touched in 42 commits), a CC peripheral, a doc row and tests | A project skill listing the steps and scaffolding the files | 2 | 1 h | Low |
| F14 | `swap` can't add methods or fields | `quick.sh:20`; the standard JVM redefines method bodies only | Spike JetBrains Runtime with `-XX:+AllowEnhancedClassRedefinition`. **Unverified with NeoForge**; found no NeoForge source; needs an install | 3 | 2 h | Low if used for dev runs only |
| F15 | `gametest.sh` can't run both suites at once | `killjvm` matches the prefix `build/moddev/gameTest` (`tools/gametest.sh:26`), so either suite kills the other | Match the exact suite | 0 | 2 min | None |

### Java crashes (for your memtest, not a dev-speed item)
- One new crash in this audit: a main-suite SIGSEGV in a G1 GC thread scanning the heap (`run/hs_err_pid2547477.log`). That fits heap corruption, and it shows the main suite crashes too, not only Aeronautics.
- It happened in 1 of 3 runs with both suites at once. All 5 single-suite full runs passed.
- I searched the logs of the 36 failed CI runs I could list and found no JVM crash. GitHub's machines don't have your RAM, so this leans toward your machine, but Sable's native code or load could still be the cause. It is not proof. Memtest remains the next step.

## Quick wins (under 15 minutes)
F1, F3, F6, F8, F11, F15.

## Bigger investments
F2, F4, F5, F9 and F10 (30-45 min), F13 and F7 (1-2 h), F12 and F14 (2 h).

## Proposed loops
**Inner (edit to result):**

| Change | Do | Today | Target |
|---|---|---|---|
| Code, compile only | `./gradlew compileJava` | 1 s | 1 s |
| Pure logic | `fastTest` | 20 s | under 5 s |
| In-world logic | `tools/gametest.sh main <filter>` | 29 s | 29 s |
| Method body, in game | `quick.sh swap` | about 3 s, headless only | about 3 s, windowed too |
| Assets, lang | `processResources`, F3+T | restart, about 1 min | under 10 s |
| Data | `/reload` | seconds | seconds |

**Still needs a restart:** mixins, registry additions, and new methods or fields (unless F14 works).

**Outer (before a push):** `tools/verify.sh`: about 3.5 min for gameplay changes, under 30 s for client, tool or docs changes. Then CI, 7.8 min today, target 4.5 min. `tools/land.sh` merges on green.

## Checked: no change needed
- **Gradle is already well tuned.** Daemon, parallel, build cache and configuration cache are on, and the MDG docs confirm configuration cache support. Warm no-op is 0.74 s. Repositories have content filters. The 2 GB heap shows no sign of pressure: `compileJava` is 0.6 s.
- **Running both suites at once:** 139-151 s against about 162 s in sequence, so 10% faster with one JVM crash in three runs. Not worth it until the crashes are explained.
- **Forcing faster GameTest ticks:** no gain. `GameTestServer.waitUntilNextTick` doesn't wait, so tests are limited by CPU.
- **Skipping JEI on quick runs:** about 2.5-4 s of its own time in idle-machine logs, too small to justify another run mode.
- **Physics and progression tests:** already pure and unit-tested. Client code leaks are limited to `VoidReclaimerClient`, the JEI plugin and one client mixin, and headless GameTests work.
- **Hooks:** not needed; F1 refreshes the checkout without one.
- **Worktrees:** two at a time fit this machine; five took the load to 62.
- **Lean options, antivirus:** render distance 12 is reasonable; Windows scanning does not apply.
- **CI cache:** 12.3 GB against a 10 GB limit, but GitHub evicts oldest-used first and setup takes 11-13 s. Nothing is suffering.

## Upgrades to consider (none applied)
Dependabot #178 is still open. No upgrade would change these numbers.

## Baseline history
| Date | Commit | Cold build | Warm no-op | One-line: compile / build | Unit | GameTest main / aero | Filtered main / aero | Datagen | Restart test main / aero | Client: title / world | CI full |
|---|---|---|---|---|---|---|---|---|---|---|---|
| 2026-10-31 | 081141a | 34 s | 0.74 s | 1 s / 21 s | 21 s | 110 s / 52 s | 29 s / 36 s | 18 s | 94 s / 87 s | 28 s / 29-47 s | 7.8 min |

**How measured:** on an idle machine, with medians of 2-3 runs. Cold means daemon stopped, `build/` cleaned and build cache off, but dependency caches kept; clearing those was ruled out. Cold includes unit tests. Main GameTest uses 2 of 3 timed runs (102 and 117 s), because the third wrote over its own log and has no exact time; its batches took 77 s. Title is 3 headless launches to the sound engine starting; world is from 3 earlier logs, because I did not open a world save in this audit.

## Sources
- [ModDevGradle docs](https://docs.neoforged.net/toolchain/docs/plugins/mdg/)
- [Gradle performance guide](https://docs.gradle.org/current/userguide/performance.html)
- [Claude Code memory docs](https://code.claude.com/docs/en/memory)
- [Claude Code skills docs](https://code.claude.com/docs/en/skills)
- [GitHub Actions limits](https://docs.github.com/en/actions/reference/limits)
- [GitHub dependency caching](https://docs.github.com/en/actions/reference/workflows-and-actions/dependency-caching)
- [GitHub auto-merge](https://docs.github.com/en/pull-requests/collaborating-with-pull-requests/incorporating-changes-from-a-pull-request/automatically-merging-a-pull-request)
- [JetBrains Runtime flag, Vaadin docs](https://vaadin.com/docs/latest/getting-started/run/generic)
