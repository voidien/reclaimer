# Handoff notes

How to pick up Void Reclaimer work in a new session. Read `CLAUDE.md` first: it holds the project rules, the settled design decisions and the extension points. Then read `PROGRESS.md` for what is done, the decisions waiting on the owner and the known issues. This file covers everything else: where things stand, the owner's standing instructions, what comes next, and the working tricks that aren't written down elsewhere.

Last updated 2026-10-31, with the 3D assembly view finished.

## Start here
**State (2026-10-31):** `main` is green through the assembly view. Done, in the order it was built (details in `PROGRESS.md`): phases 1 to 10 of the original plan (deep space, zero-G flight, cosmic dust, the Replicator, the modular body with its third-person rendering, asteroids, machines, the Zero-G Frame and payloads, the Mass Driver and the Starship milestone); the extensibility pass and body sections; the Titan size class; the art pipeline and the RD tools; the playtest rounds (start of the game, dust HUD, Replicator and inventory screens, respawn cards, orbit camera, photo mode, first person in big ships); the Act III slice (`ruined_earth`, the three-era sky, the Atmospheric Portal, the Terraforming Spire's pulse); hover flight on Earth; the required addons and CC: Tweaked peripherals for every block; the 7b ships (Chemical, Ion and RCS thrusters, TFMG fuels, the Helm with its holds, HUD and camera); Earth flight (wind, Earthfall, the Aeronautics parts); stations (Station Core, Docking Clamp, ships through the portal); the launch complex (controller, 15 x 15 ring and pad, renderer, CC peripheral); the helper drones (ferry, mining, collecting dust; payloads reach the launcher through the ferry); the debug tools (`CLAUDE.md` "Debug tools"); the attachment point fixes; the 3D assembly view (screen, markers, camera, hover previews, helpers).

Verified on `main`: 207 GameTests in `runGameTestServer`, 32 Aeronautics GameTests, the build (unit tests), `runData`, the Python art tool tests.

**Next, in the owner's order (2026-10-31):** **remote control** (`CLAUDE.md` "Remote control"; bug fixes first, see below), the Spire, holo-displays (`docs/HOLO_DISPLAYS_PLAN.md`) and the airship kit (`CLAUDE.md` "Next four stages"). Wanted but not built, from `CLAUDE.md` "Round two": helper drones that steer round blocks on a small battery, and Earth's air thickening as sectors heal. Still needing an interview after those: phases 13 and 14 (Data Monolith and villagers, Restoration), first-person pieces for the Drone and Mech, the Titan's module models.

**Remote control PR order (nothing player-reachable until PR 14; PRs 8 to 10 through the reviewer agent):** (1) `DustData.add` never lowers a reserve (done); (2) Backup Cores aboard an unloaded station are not forgotten; (3) a Helm finds its pilot after relog; (4) stale thrust after a dimension change; (5) `BodyHost`, debug spawn; (6) renderer widened to `LivingEntity`; (7) Aeronautics spike of the force-load ticket; (8) body snapshot, `ItemCensus`, `TestPlayers.rejoin`; (9) `HomeBody`, the transaction, `/void_reclaimer link status/wake/force/signal`, dust cap from the home body; (10) destroyed path, wreck, shell list; (11) asynchronous link to an unloaded shell; (12) Uplink Seat, mirror, seat UUID, wake chain, charging, station bonus; (13) `RelayGraph`, `SignalMath`, picker, unlink key, static; (14) Remote Shell item and recipes; (15) Relay block, relays on ships, relay overlay; (16) Dyson tiers and the portal bridge; (17) first-link milestone and Starship gating; (18) Helper Drone takeover. The first four are bug fixes that stand alone.

**Remote control PR order (nothing player-reachable until PR 14; PRs 8 to 10 through the reviewer agent):** (1) `DustData.add` never lowers a reserve; (2) Backup Cores aboard an unloaded station are not forgotten (done); (3) a Helm finds its pilot after relog; (4) stale thrust after a dimension change; (5) `BodyHost`, debug spawn; (6) renderer widened to `LivingEntity`; (7) Aeronautics spike of the force-load ticket; (8) body snapshot, `ItemCensus`, `TestPlayers.rejoin`; (9) `HomeBody`, the transaction, `/void_reclaimer link status/wake/force/signal`, dust cap from the home body; (10) destroyed path, wreck, shell list; (11) asynchronous link to an unloaded shell; (12) Uplink Seat, mirror, seat UUID, wake chain, charging, station bonus; (13) `RelayGraph`, `SignalMath`, picker, unlink key, static; (14) Remote Shell item and recipes; (15) Relay block, relays on ships, relay overlay; (16) Dyson tiers and the portal bridge; (17) first-link milestone and Starship gating; (18) Helper Drone takeover. The first four are bug fixes that stand alone.

**Waiting on the owner (do not decide alone):**
- Everything under "Decisions pending (for the owner)" at the top of `PROGRESS.md`: the unattended defaults of hover flight, the Cloud Siphon's power numbers, the RCS block, the Helm, Earth's wind, the stations, the docking clamp, the portal crossing, the launch complex, the renderer and the helper drones.
- Hazards across the whole game (the owner wants to discuss; fall damage on Earth is vanilla for now).
- The owner has no time to playtest: do not wait for feedback, but list what to look at.

**Not seen in a real client yet (list these when the owner asks what to try):** the three-era sky's colours, hover flight beyond the HUD text and the Mech walking, the Helm HUD and the camera riding a ship, stations and docking, ships through the portal, the launch complex renderer (its orientation relative to the controller and the lean and kick), the helper drones' flight, the CC peripherals from a real computer, the cockpit offsets. No RD tool has run against the real service: that needs the owner's key in `$RD_API_KEY`; never ask for it, store it or print it.

## The owner's standing rules
- **New features:** interview with the AskUserQuestion tool (recommended option first), then a file list, then wait for a go. Bug fixes and root-cause fixes of flaky tests go straight to a PR. For an unattended stretch the owner says so; then take the safest reversible default for every open design choice and record it under "Decisions pending (for the owner)" in `PROGRESS.md`.
- **Small PRs:** one change each, a few files, branched from the latest `main`, never pushed to `main`, never stacked. Merge your own PR once CI is green (`gh pr merge N --merge --delete-branch`; the merge permission is in `.claude/settings.json`). Delete the branch after the merge so the remote stays at `main` plus the parked branches below. If the auto-mode classifier blocks something, say so and stop; never work around it.
- **Before every push:** `./gradlew build`, both GameTest suites (`tools/gametest.sh main` and `tools/gametest.sh aero`), `./gradlew runData` when datagen is touched (commit `src/generated`), the RD tests when `tools/art` is touched. Every push reruns CI, so check locally and batch fixes into one push. Keep working while CI runs; check it the next time the owner writes and merge if green (the owner does not want PR subscriptions).
- **Tests:** every feature adds unit tests and GameTests. Mutation-check each new test: back the file up, break the feature, see the test fail, restore. A flaky test gets a root-cause fix, never a skip or a rerun.
- **Every new block or item with state or actions gets a CC: Tweaked peripheral** in the same PR (`CLAUDE.md` "CC: Tweaked peripherals").
- **Docs:** keep `CLAUDE.md`, `PROGRESS.md` and this file current in the same PR. Two PRs that both add a block before `## Phases` in `PROGRESS.md` conflict: merge `main` into the second and keep both blocks. No model names in commits, PRs or code. Name the phase in the commit message.
- **Speak up (`CLAUDE.md` rule 10):** the owner wants a collaborator. Bring up debug tools and recommendations whenever you see them, for your own work and through any agent you spawn. In an unattended stretch, collect them under "Ideas for the owner" in `PROGRESS.md` and raise them at the end.
- **Agents:** the Opus reviewer agent (`.claude/agents/reviewer.md`) only for risky gameplay, saved data, networking or extension-point changes; other agents only when they save real time (a council on a stuck bug, parallel independent work).

## This machine (the owner's PC, Arch-based, zsh)
- Java 21 (Temurin) is the default `java`; Gradle 9.2.1 does not run on the also-installed Java 27. `gh` is logged in as `voidien`.
- Git has no global identity: commit with `git -c user.name=voidien -c user.email=99926166+voidien@users.noreply.github.com commit ...` (the identity the history uses).
- **Worktrees:** `/home/void/projects/reclaimer` is the primary checkout, detached at `origin/main`; Claude Code starts there and loads its `CLAUDE.md`, so keep it on a current `main` and do not build there. `/home/void/projects/reclaimer-main` is the working checkout: branch from `origin/main` there, build and test there (`run/` holds the main GameTest world, `run-aero/` the Aeronautics one). Never checkout, stash or rebase in `reclaimer-main` while a background Gradle chain runs there: it corrupts that run. Open a PR for a branch without checking it out with `gh pr create --head <branch> --base main`.
- **Branches on GitHub (cleaned 2026-10-31):** `main` is the default branch. Parked, never merged: `art-staging` (the owner's art courier, see "Art workflow"), `claude/aeronautics-spike` (the first Sable spike; its results are only there), `spike-addon-compat` (the addon compatibility harness: `-PspikeMods=a,b,c` loads candidate addons, CurseForge ones through cursemaven, and its `build.gradle` has the client quickplay argument; check it out in a new worktree when needed).
- **Run GameTests with `tools/gametest.sh [main|aero] [filter]`.** It prints the summary and the failures, exits non-zero on failure, and kills a JVM that outlives the summary (the server used to keep `run/world/session.lock`; `GameTestStopMixin` now bounds the stop loop, and the script warns if the exit still takes over 40 s). A `grep gameTest` kill pattern in a shell command matches and kills that shell: kill by PID.
- **Open PRs with `tools/pr.sh "title" "body"`** (pushes the branch, targets `main`). Some foreground commands exceed 10 minutes: run long Gradle tasks with `timeout` or in the background.
- Xvfb is installed for the headless client (`tools/client-test/`, see "In-game client checks"). Call `xt.py` with an absolute path. A fresh client run shows a Narrator screen, "Failed to Quick Play" and the experimental-settings screen: create a world through the GUI with commands allowed.
- Modrinth maven resolves version *numbers* across loaders (CC: Tweaked's number gave the Fabric jar): pin multi-loader mods by Modrinth version id and check the jar has `META-INF/neoforge.mods.toml`.
- **Start-up race:** a client start can die with `IllegalStateException: Found unused register callbacks` from Registrate (`AbstractRegistrate.onRegister`), then a crash about `neoforge:swim_speed` while NeoForge rolls back. It is upstream and intermittent, not our code: run it again. `quick.sh start` reports it.
- **JVM crash:** a client start can also die with `guarantee(def != nullptr) failed` in `PhaseChaitin::post_allocate_copy_removal`, a bug in the JVM's C2 compiler. It is not our code either: run it again.
- `runData` can hang on the upstream Aeronautics start-up deadlock: `timeout 300 ./gradlew runData`, then `./gradlew --stop` and run it again (CI retries it the same way).

## Create Aeronautics spike (branch `claude/aeronautics-spike`, never merge)
- Full results are in `AERONAUTICS_SPIKE.md` on that branch. Aeronautics and Sable are required dependencies since the Foundations phase; ships came in 7b.
- Sable needs a `data/<ns>/dimension_physics/*.json` with `base_gravity` 0 for zero-G; propellers and balloons need `base_pressure` > 0, so they give no thrust in deep space (our thrusters push through Sable's force path instead). Players can ride a moving ship. Sable removes sub-levels outside `sub_level_remove_min` / `sub_level_remove_max`. Useful commands: `/sable spawn platform`, `/sable assemble connected x y z`, `/sable info @n`.
- **sable64bit** (Chelovechic/sable64bit-1.21.1, AGPL) gives f64 physics and held X = 20,000,000.5 exactly where f32 lost half a block. Recommend it to players; never depend on it and never commit its jar. The spike branch loads it with `-PspikeF64=true`.

## Working tricks
### Commands
- `./gradlew build` (includes unit tests).
- `./gradlew runGameTestServer` (or `tools/gametest.sh main`): look for `All N required tests passed`. `-PgametestFilter=ZeroGMining` runs only the tests whose class or method name contains the text (comma-separated alternatives, ignoring case): about 30 s for one class. `-PgametestHeap=8G` runs the server in another heap (default 6G).
- `./gradlew runGameTestAeronautics` (or `tools/gametest.sh aero`): the Aeronautics suite, separate world in `run-aero/`, own CI job.
- `./gradlew runData`: commit the output in `src/generated/resources`. CI fails if it's stale.
- For a GameTest failure, grep the log for `failed at` and the message.

### GameTests
- **Kinetic block entities:** Create resets the speed of a block with no source, so a test sets the speed before every tick (`MachineGameTests.tick`). Call `tick()` once first so the block settles onto the network.
- **Coordinates:** give each test its own X. Existing tests use 12,000 to 101,000, mostly in steps of 1,000 (`grep survivalInDeepSpace(helper,`). Dust tests pick the cloud nearest a fixed far-off point so they always use the same one; two tests must not share a cloud (they discard each other's entities when they clean up).
- **Chunks:** `NaniteGameTests.inLoadedArea` for block tests (forces the chunk, waits up to 100 ticks for it to tick); `inEntityTickingArea` for tests that spawn or look up entities (forces the 3 x 3 and waits until entities tick: a lone forced chunk never reaches entity ticking, and entities in a chunk that does not tick entities neither move nor appear in lookups). Forced chunks stay for the whole run on purpose; see `PROGRESS.md` "Test infrastructure: the GameTest server's memory" before changing that.
- **Blocks with contents:** the GameTest world persists between local runs, and setting a chest over an old chest keeps the old contents: set AIR first.
- **Asteroids and fixed test coordinates:** a fresh world puts asteroids in random places; tests that need guaranteed empty space clear their area first (`AeronauticsGameTests.clearArea`).
- **Generators:** the GameTest world saves each dimension's generator. After changing a generator or dimension JSON, delete `run/world` before the run (CI is always fresh).
- Test-only content lives in `src/main/resources/gametest_pack` (namespace `void_reclaimer_test`), loaded only on the GameTest server. `TestEventListeners` reacts to tagged test players (for example `NO_DUST`), and sets the GameTest server's view and simulation distance to 3.
- **Don't start a second `helper.startSequence()` from inside a running sequence step**; chain steps on one sequence.
- **Long jobs:** to skip a long Replicator job, rewrite the job's progress with `Replicator.store` (see `ReplicatorGameTests.kitsDeliverEveryPartAndKeepSalvagePrices`).
- Timeouts are at most 900 ticks; a test that needs more splits its work.

### Photo mode as a diagnostic
F8 (photo mode), then G: exact facts about whatever the camera looks at, read from the server. Enter saves `run/screenshots/photo_<time>.png` and a `.txt` beside it with the full facts: read the text file instead of guessing from pixels.
`tools/client-test/quick.sh photo NAME [distance]` shoots four views of the body in about 5 s and prints the facts; `/vrphoto focus x y z` first to look at something else. `/vrphoto labels true` names every socket and mount and lists their positions in the text file. `/vrphoto debug true` shows the hit box, which is the fastest way to compare a model with its real size. For a clean background and a scale to read, build the arena first: `/void_reclaimer scene arena`. Other scenes: `launch`, `drones`, `station`. Give the world a few seconds after joining before the first command: commands typed straight away can be lost.

### Test client helper commands
`tools/client-test/xt.py` has `chord ctrl+f` (modifiers plus a key), `type "text" 0.3` (a gap between keys, for boxes that refilter on every key) and `mdrag DX DY [steps [shot.png]]` (hold the middle mouse button and move; the optional PNG is taken while the button is held, which is how the orbit camera was checked).

### JEI recipe screenshots
`python3 tools/client-test/jei_shots.py [--out build/jei-shots] [--only item,item]` screenshots JEI's recipe view for every item our machine recipes make and writes `index.html` beside the PNGs. It needs the headless client at **1920x1080 on a 1920x1080 Xvfb** (at 1280 wide JEI shrinks its list): start `Xvfb :99 -screen 0 1920x1080x24`, run `DISPLAY=:99 ./gradlew runClient -PquickPlay='New World' -PclientSize=1920x1080`, wait for "joined the game", then run the script. JEI drops keys typed too fast, the `r` key typed into a focused search box instead of showing recipes (click the result), and a recipe with several pages shows only its first.

### In-game client checks (headless)
**Use `tools/client-test/quick.sh`: start the client once and swap code into it.** Measured: 40 to 50 s from nothing to a settled world, 3 s from a saved edit to seeing it in the running game.
```
tools/client-test/quick.sh start        # Xvfb, client, world vrtest; returns when the world is on screen
tools/client-test/quick.sh swap         # compile and replace the changed classes in the running game
tools/client-test/quick.sh shot NAME    # build/client-shots/NAME.png
tools/client-test/quick.sh cmd "/gamemode survival"
tools/client-test/quick.sh photo NAME   # four views joined into build/client-shots/NAME.png, with the facts
tools/client-test/quick.sh compare A B  # what changed between two shots: pixels, where, and a marked picture
tools/client-test/quick.sh gui          # with a screen open: its buttons, click regions and overlaps as text; F6 outlines them on screen
tools/client-test/quick.sh test NAME    # run a GameTest in the live client, to watch it; only when a log is not enough, it is the slow way
tools/client-test/quick.sh gclick 160 12   # GUI coordinates (window 1280x720, GUI scale 3); also ghover, gdrag, key, scroll
tools/client-test/quick.sh stop
```
- `swap` changes what methods do. A new, removed or renamed method or field, or a mixin change, needs `stop` and `start`: it says so. Classes the game has not loaded yet need nothing. Assets and lang need a restart.
- So while tuning, keep helper methods and fields added in the first version, and iterate on method bodies.
- The Gradle properties behind it replace editing `build.gradle`: `-PquickPlay=<world>`, `-PclientSize=<w>x<h>`, `-PhotSwap` (debug port 5005). Do not edit `build.gradle` for a client run any more.

Helpers underneath are in `tools/client-test/` (`client.sh`, `xt.py`). With a real display, `./gradlew runClient` opens the game with the four performance mods and the playtest checklist (`docs/PLAYTEST.md`) can be played directly.
1. **Launch:** see above.
2. **First load:** without the `vrtest` world, quick play fails; run `tools/client-test/bootstrap_world.sh` once to create it through the menus (creative, commands on). An "Experimental Settings" screen may show: click "I know what I'm doing" at (878, 464).
3. **Set up the player** with `run "/command"`: `/void_reclaimer dust set @s void_reclaimer:metallic 200`, `/void_reclaimer milestone grant @s void_reclaimer:expansion`, `/void_reclaimer body reset @s`, `/gamemode survival`.
4. **Screen coordinates** (1280 x 720, GUI scale 3): the Replicator (key R) panel spans (160, 61) to (1120, 661); tabs at y 137: Tools 250, Modules 400, Parts 553, Blocks 706, Logs 860, Body 1013; recipe rows start at y about 205, 66 px apart; Replicate at (311, 613). Scroll with `xt scroll -N` while hovering the list.
5. **JEI:** open the inventory (E), hover an item with `xt hover X Y`, then press R for its recipes.
6. **Screenshots:** `shot file.png`, then view the PNG. **Stopping:** `kill $(clientpid)`. There is no sound in the headless client.
- **Seeing a body:** `/void_reclaimer scene arena`, then `/void_reclaimer body preset @s mech full` (or `drone`, `ship`, `shuttle`, `titan`) and `quick.sh photo NAME`. Wait about 45 s after "joined the game" before typing commands. Zoom screenshots with ImageMagick `convert shot.png -crop WxH+X+Y -filter point -resize 300% out.png`.

### Placeholder art
- **Test Starship** (`void_reclaimer:test_starship`): a placeholder entity for checking RD models and animations in game. To replace its model, see `PROGRESS.md` ("Test Starship").
- `docs/art/ASSETS.md` + `assets.csv` list every asset with Retro Diffusion prompts (regenerate with `python3 tools/art/asset_list.py`). `--promote <asset>` converts an RD `.bbmodel` into a part's GeckoLib model (`tools/art/bbgeo.py`): it keeps our skeleton, scales to fit and bakes rotated face UVs (GeckoLib 4.9.3 only supports geometry 1.12.0 cleanly). `GeoModelLoadTest` loads every `.geo.json` and animation through GeckoLib's own loader and checks the bones. `--from-group`/`--scale-like` cut Ship sections out of one whole-ship model; `--promote-as entity/NAME` writes a standalone model plus converted animations (GeckoLib reads only `post`, so step keys become holds). The style guide lives in `docs/art/style.json` (regenerate with `tools/art/style_guide.py`; a test checks it is current).
- Remaining placeholder textures are 16 x 16 programmer art, written by small Python PNG scripts (zlib + struct, no PIL). Every placeholder must be listed in `PROGRESS.md`.

## Art workflow (the owner works on a phone in Termux and on a PC)
- **Generate and pick:** the owner runs `tools/art/rd_generate.py` with their own Retro Diffusion key (never ask for it, store it or commit it; it lives only in their environment). Results stage in `art/rd/` (gitignored). `art/rd/index.html` is a single self-contained page: tap a variant to promote it, or mark Keep current or Redo, add notes, and copy the commands it builds. With the RD MCP server attached, show the free estimate and ask before every paid call.
- **Promote:** `--promote asset=N` copies a picked variant into the mod, size-checked, and records it in `docs/art/promoted.txt`. `asset_list.py` reads that file, so regenerating `docs/art/ASSETS.md` and `assets.csv` marks those files as `RD art (promoted)`. `--status placeholder` then skips them.
- **Getting their work to you:** `art/rd/` is gitignored, so the owner pushes it on a throwaway branch with `git add -f art/rd` (see `tools/art/README.md`, "Moving staging between devices"). The branch `art-staging` on GitHub is only a courier: never merge it, and never commit `art/rd` to `main`. To use what is on it, `git fetch origin art-staging`, then `git checkout origin/art-staging -- src` for the promoted files (and `-- art/rd` plus `git reset -q art/rd` to look at the variants). Then make a normal branch and PR.
- **Models:** `--promote <asset>` (keeps our skeleton), `--from-group` / `--scale-like` (cut Ship sections out of one whole model), `--promote-as entity/NAME` (standalone model plus converted animations, used by the Test Starship). The owner chose RD's Blocky or Blocky Lite 3D styles only.
- **Style:** `docs/art/style.json` is the source for the style guide and the per-category palettes (`python3 tools/art/style_guide.py`; a test checks they are current). `--setup-styles` makes RD custom styles from 1 to 4 reference images in `docs/art/style/<category>/` (the owner picks those later).
- **Tests:** `python3 tools/art/test_rd_generate.py` (against a fake RD server; Node is used for the picker's command builder; CI runs it), `test_rig_check.py`, `test_anim_slice.py`. Mutation-check new ones like any other test.
- **Old request sheet:** `art-handoff/ART_REQUESTS.md` and its zip predate the RD tools (the Mass Driver blocks it lists are now the launch complex); the asset list is the current source.
