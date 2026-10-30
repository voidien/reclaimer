# Prompt for taking over on a PC

Paste this as the first message of a new Claude Code session in a clone of `voidien/reclaimer` on the owner's PC.

---

You are taking over development of "Void Reclaimer", a NeoForge 1.21.1 Minecraft mod (Java 21, ModDevGradle) in the GitHub repo voidien/reclaimer. It is a singleplayer zero-gravity progression and factory-automation mod. It requires Create, Create Aeronautics, Sable and GeckoLib, and uses JEI. You are now running on the owner's own PC, not in a cloud container: Gradle caches stay warm and `./gradlew runClient` opens the real game with a GPU.

START HERE, in this order, before writing any code:
1. Read CLAUDE.md. Project rules, settled design decisions, extension points, workflow rules.
2. Read HANDOFF.md. Current state, my standing instructions, "Taking over on a PC" (setup and what is and isn't faster), the open items and the playtest feedback list.
3. Read PROGRESS.md. What is done and the known issues.
4. Run `git fetch`, `git log origin/main --oneline -15` and `gh pr list` to see what has merged and what is still open (PR #47, the energy bar fix, may still be open: merge it when CI is green).

SET UP, then check the toolchain works before changing anything: Java 21, `gh auth status`, and `./gradlew build` (the first build downloads a lot). Tell me anything that fails.

HOW WE WORK (strict, details in CLAUDE.md):
- Small pull requests. One change per PR, a few files, reviewable in minutes. Branch from the latest main; never push to main. Merge your own PR once CI is green.
- Check locally before each push, because every push restarts CI: `./gradlew build` (includes unit tests), `./gradlew runGameTestServer` (look for "All N required tests passed"), and `./gradlew runData` if you touch datagen (commit src/generated). `runData` can hang on an upstream Aeronautics start-up deadlock: `./gradlew --stop` and run it again.
- Before each new feature or phase: ask me clarifying questions about open design choices, list the files you will create or change, and wait for my go-ahead. Bug fixes can go straight to a PR.
- Complete, working code, no logic placeholders. Placeholder art is allowed and must be listed in PROGRESS.md.
- Every feature adds unit tests and GameTests. Break the feature once to check the test can fail. A flaky test gets a root-cause fix, not a re-run; never skip or disable a test.
- Keep CLAUDE.md, PROGRESS.md and HANDOFF.md current in the same PR as the change. Do not put model names in commits, PRs or code.

WHERE WE ARE: housekeeping, the settings screen, CI trimming, the raw-item collector (Cloud Collector module and Cloud Siphon block) and the first playtest fixes are done. I playtested up to stage 5 of docs/PLAYTEST.md and my notes are in HANDOFF.md under "Open items". Decisions already made: starter dust plus a guided first craft and a faint built-in hum for the bare Drone; our tabbed screen replaces the inventory on E (no armor slots or crafting grid for the Drone and Mech); Replicator for early items and Create machines for the chain items; an orbit camera and a photo mode.

YOUR FIRST TASKS, in this order:
1. The to-do in HANDOFF.md: a way to run one GameTest class or test instead of all 127, so an iteration takes under a minute. Prove it, document it, small PR.
2. Playtest fixes from group A that are still open, each as its own PR: the drone bobbing in a 1x1 tunnel when it touches the ground; the Mech's flying animation playing once and then idle, and the idle animation and floating blue foot flames; Ship, Shuttle and Titan too large in F5 (camera distance and scale); the very large health bar on big bodies (ask me first how to show it).
3. Then the rest of the feedback list, in the order I confirm: start of the game, dust HUD, Replicator screen (descriptions, queue reorder, JEI), the inventory screen, the respawn menu, the cameras, and an image check of the Create recipes.

Now that you have a real display, look at your own work in the game (`./gradlew runClient`) and tell me what you saw. First task: read the three files, run the toolchain check, then tell me in a few lines what you understood and what you will do first. Then wait for my go-ahead.
