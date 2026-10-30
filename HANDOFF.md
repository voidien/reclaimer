# Handoff notes

How to pick up Void Reclaimer work in a new session. Read `CLAUDE.md` first: it holds the project rules, the settled design decisions and the extension points. Then read `PROGRESS.md` for what is done and the known issues. This file covers everything else: where things stand, the user's standing instructions, what comes next, and the working tricks that aren't written down elsewhere.

Last updated 2026-10-30, after round 2 (PRs #49 to #86). The Act III vertical slice (dimension, sky eras, portal, Spire pulse) is built, PRs #88 to #91 and the Spire PR; see the PROGRESS.md blocks "Act III slice". Next: the owner looks at it in game, then hover flight on Earth, the launch complex, stations and helper drones.

## Start here (written 2026-10-30 at the owner's request)

- **Required addons (2026-10-30):** Crafts & Additions, Advanced Electricity, Advanced Computer Technic, Propulsion, TFMG are required (see `CLAUDE.md` "Required addons"). CC: Tweaked and Avionics are optional; the owner asked whether Computer Technic's computers can script Aeronautics ships like Avionics does: research that before designing it.

- **Update (2026-10-30, unattended stretch):** hover flight on Earth is done (PRs #94 to #97; the weight factor default is 0.12, measured, see PROGRESS.md "Hover tuning") and the addon compatibility spike is done (PROGRESS.md "Addon compatibility spike: results": all nine candidates load together, nothing was made required). The owner decides the required set. The spike worktree is `../reclaimer-spike` (never merge it; it has the `-PspikeMods=` hook and a client quickplay hack). Next in the agreed order: addon policy and an electricity bridge, then 7b ships.

- **Next work (decided 2026-10-30, see `CLAUDE.md` "Hover flight on Earth" and "Create, Aeronautics and addon integration"):** (1) hover flight on Earth in three small PRs (interview done, file list given, **waiting for the owner's go for PR 1**); (2) the addon compatibility spike; then electricity bridge, 7b ships, Earth airships, moving stations, portal-crossing ships. The owner has no time to playtest right now: do not wait for feedback, but keep asking before features. The Act III slice (PRs #88 to #92) is merged; the blue slabs around the Ship were **not reproducible** in the Xvfb client (third person, front, first person, dark and lit room).
**The owner's answers for the next session:**
- **First task (DONE 2026-10-30, see PROGRESS.md): the Act III vertical slice.** It proves the riskiest part early (`CLAUDE.md` "Interview decisions", step 4): the `ruined_earth` dimension, the Atmospheric Portal, the three-era sky and one Terraforming Spire pulse. Interview the owner about the rest of its design (AskUserQuestion), list the files, then split it into small PRs. Their decisions so far:
  - **Portal:** a craftable Atmospheric Portal frame, **big enough for the Ship to fly through** (Ship is about 12 blocks long, 5.6 wide, at scale 3; the Titan is 67 blocks and need not fit yet).
  - **Sky:** all three eras (ruined, healing, restored) selectable by a debug command first, so each can be judged before the Spire drives them. Blending by restoration percent comes later.
  - **Spire pulse:** rewrites biomes **and blocks** in its 128-block area and resends the chunks, with a visible before/after and a GameTest (`CLAUDE.md` Phases 11 and 12: the 4x4 sectors, Purified Water and Bio-Nutrient Slurry inputs, the Atmospheric Catalyst Core, the Genesis Pulse).
- **Ask before features, just do bug fixes.** New features: interview-style questions with the AskUserQuestion tool, a file list, then wait for a go. Bug fixes go straight to a PR. **Merge your own PRs once CI is green** (if the auto-mode classifier blocks `gh pr merge`, say so and stop; do not work around it, the owner then says "merge" or adds a permission rule).
- **Standing rules:** small PRs, one change each, branched from the latest `main`, never pushed to `main`; keep working (start the next bug or look-around) while CI or tests run; the Opus reviewer agent (`.claude/agents/reviewer.md`) only when absolutely necessary (risky gameplay logic, saved data, networking, extension points); keep `CLAUDE.md`, `PROGRESS.md` and this file current in the same PR; no model names in commits, PRs or code.
- **Warnings:** (1) **The owner has not played any of round 2 yet.** Expect feedback on the cockpit offsets (`BuiltinBodies`), the orbit camera sensitivity, the Mech lean, the starter dust, the panel sizes, and the new screens. Ask for it first and fix what comes back. The checklist is `docs/PLAYTEST.md` section 9b. (2) **Unexplained blue slabs around the Ship preset** (see "Seen in the headless client" below): possibly a real rendering bug, possibly what the owner called "too large"; look before building more on the Ship. (3) **Ship and Shuttle size is undecided** (scale 3.0 now; the owner may want about 2.0): do not change it until they decide. (4) The classifier note above.

## Where things stand
- **`main`** has Phases 1 to 10 done, plus everything around them. In order:
  - Phases 1 to 4: deep space and spawn, zero-G flight, cosmic dust, the Replicator (4a), Nanite Swarm, Tether Anchor and Backup Core (4b).
  - Phase 5a body data, then the extensibility pass and body sections (datapack registries, events, chassis and sockets, paint, wear, Body Blueprints, kits).
  - Foundations: Create, Aeronautics and Sable required; Dyson Sphere counters and sky.
  - Phase 6 asteroids; Phase 7 machines (nebula materials, Create chains, Electrolyzer, Spinner, fan freezing, spout casting).
  - Phase 8 Zero-G Frame and payloads; Phase 9 Kinetic Mass Driver, Quantum Receiver and power beams; Phase 10 the Starship milestone and the Ship recipes.
  - Phase 5b-1 body rendering in third person (the GeckoLib body replaces the player model; see the Phase 5b-1 block in `CLAUDE.md`).
  - The Titan size class (a 67-block ship body from the owner's RD model), Core Mk III and IV, and module mounts on whole-model rigs (see the Titan block in `CLAUDE.md`). The Titan unlocks at Dyson tier 3 (decided; one constant, `TITAN_UNLOCK`). Open: its module models.
  - The art pipeline (`tools/art/`, see "Art workflow" below), art drop 1, RD art pass 1, and the Test Starship placeholder entity.
- **Since the last update (2026-10-29 to 2026-10-30), all merged:**
  - Housekeeping: body minimum brightness (`body.minLight`), the "everything is obtainable" GameTest (`ObtainabilityGameTests`), the playtest checklist (`docs/PLAYTEST.md`), the settings screen under Mods > Void Reclaimer > Config.
  - CI trimming (`changes` job in `build.yml`): docs-only PRs skip CI, drafts run build and unit tests only, client-only PRs skip GameTests, datagen and Aeronautics.
  - Raw-item collection: the Cloud Collector module (tool mount, data-driven spec) and the Cloud Siphon block (Create rotation, dense clouds, `CollectorPowerEvent` for addon power). See their blocks in `CLAUDE.md`.
  - Playtest fixes: hand mining in zero-G no longer 5x slower; the energy bar draws one battery cell at a time. PR #47 (energy bar) may still be open: check `gh pr list`.
- **Last verified on `main` (2026-10-30, after PR #81):** 191 unit tests, 134 GameTests (`runGameTestServer`), the Python art tool tests (29 + 10 + 9), datagen up to date, CI green.
- **Older note:** build with 112 unit tests; 103 GameTests (`runGameTestServer`); the Aeronautics GameTests; CI green. Two GameTests are flaky, see "Open items".
- **Not done yet** (revised plan in `CLAUDE.md`):
  - **5b-2:** first-person body rendering (the vanilla hand still shows) and the 3D assembly view in the Body tab. Third person is done (`client/render/body/`).
  - **7b:** Aeronautics ships (Space Thruster, Helm). Stations and helper drones.
  - **Act III, phases 11 to 14:** Ruined Earth, the Terraforming Spire, the Data Monolith and villagers, Restoration.
- **Order decided (2026-10-29, `CLAUDE.md` "Interview decisions"):** housekeeping PRs → playtest → first person → Act III vertical slice (full three-era sky) → launch complex → stations and helper drones → rest of Act III → 3D assembly view → 7b. Titan unlock is Dyson tier 3 (decided). Still ask first (`CLAUDE.md` workflow rule 2): clarifying questions, the file list, then wait for the go-ahead; the launch complex has no go yet.
- **Design doc:** the user's "Nebulae to Planetary Genesis" doc extends the plan (see `CLAUDE.md`, "Design doc" and "Dyson Sphere" blocks). Lore and recipes come only from that doc; ask when it's unclear.
  - https://claude.ai/artifact/YD1avpfCtfdKcZotESDR1n (a Claude Docs project, at revision 12).
  - It does **not** yet describe the body sections, paint, wear, blueprint and kit work, nor the later phases. Update it from the blocks in `CLAUDE.md` if the owner asks.
- **Aeronautics GameTests:** `./gradlew runGameTestAeronautics` (separate from `runGameTestServer`, own CI job). The local GameTest world persists, so old test platforms stay put there; CI is fresh.

## The user's standing instructions
- **Clarifying questions:** ask them before each phase (their preference, and `CLAUDE.md` workflow rule 2). Wait for a go-ahead on the file list.
- **Merging:** merge your own PRs once CI is green. (The owner once said to wait for five open PRs, then said "open and merge the PRs"; the latter is what has been practised since. Ask if they want batching.) Each PR branches from the latest `main` on its own, never stacked; `PROGRESS.md` and `CLAUDE.md` conflict when two PRs both add a section before `## Phases`, so merge `main` into the second and keep both sides.
- **"Stop listening to pull requests unless they are from me":**
  - Don't subscribe to PR activity for PRs you open.
  - Check CI the next time the user writes, and merge if it's green.
- **Small PRs (2026-10-29):** one change per PR, a few files, reviewable in minutes; split a phase into several PRs (see `CLAUDE.md` workflow rules 7 and 8). Never push to `main` directly. Restart the branch from the latest `main` after its PR merges.
- **CI cost:** every push to a PR branch reruns CI, so check locally first and batch fixes into one push.
- **Commits:** name the phase in the message. Keep model names out of commits, PRs and code.
- **Testing:** every phase adds unit tests and GameTests.
  - Mutation-check each new test: break the feature, run the test, see it fail, restore.
  - Back up the file first (e.g. `cp X /tmp/x.bak`) and restore from it.

## Open items
- **Done in round 1 (see PROGRESS.md):** GameTest filter, drone floor bob, Mech cruise animation and re-rigged Mech, velocity lean (Mech), camera distance, health bar, art tools (rig check, reparent, rerig.txt). **Still to look at in game:** the lean angles, the health bar's look, the camera, the flames following the feet.
- **Done in round 2 (2026-10-30, PRs #49 to #76, see PROGRESS.md):** start of the game (starter dust M25 S30 C5, bare-drone hum, dust milestones now count dust *gathered*), dust HUD (icons, fading meter, icon prices), Replicator screen (81 descriptions, drag-to-reorder, JEI lookups), body inventory (E, tabs, 3D preview, dust), respawn cards, orbit camera and photo mode (F8), machine recipe GameTest, `anim_slice.py` and layered poses (the Mech's tool arm), first person in big ships (cockpit camera, no hand). **Not seen in game yet, please try:** every one of these; the numbers most likely to need tuning are the cockpit offsets (`BuiltinBodies`), the orbit sensitivity (`OrbitState.SENSITIVITY`), the lean angles (`body_rigs/mech.json`) and the starter dust (config `starter`).
- **Seen in the headless client (2026-10-30), for the owner to judge:** the Replicator and inventory screens fit a 720p window; the 3D preview, dust icons, health bar, orbit camera and photo mode work; the Ship preset (`body preset @s ship`) shows no hand in first person, but its third-person and first-person views show large dark-blue slabs around the camera at 0 m/s. They are not the Dyson lattice (that world's counters are 0) and not the space specks (they stay with `specks = false` in the client config). The Ship's own model sizes are normal (12 blocks long at scale 3). They look like a sky element: the pattern does not change when the photo-mode camera moves 20 blocks away from the body, they show with the Ship preset in third person but not with the Drone, and they radiate from the centre like beams. Not investigated further; look at `client/render/DeepSpaceEffects` and `DysonSky` (what they draw when the Dyson counters are 0 and when the body is large) and at the cosmetic trail. The cockpit offsets are unchecked by eye.
- **Waiting on the owner:** whether Ship and Shuttle should shrink (scale 3.0 to about 2.0) after trying the camera. The owner's longer-term goal is ships that travel and face freely in every direction (roll, 6-DOF); only the Mech lean and the photo-mode roll exist.
- **Next:** (1) look at the Create recipe screenshots (`python3 tools/client-test/jei_shots.py`, see "In-game client checks"; run it again after recipe changes); (2) first-person pieces for the Drone and Mech (art), then the Act III slice and the launch complex, which need an explicit go and design questions (`CLAUDE.md`); (3) the cockpit offsets and Ship/Shuttle size after the owner's test.
- **Working notes:** the owner wants questions asked interview style (AskUserQuestion), wants work to continue while CI or tests run, and in the 2026-10-30 night window said to merge each PR when green without asking. A GameTest JVM can outlive its Gradle run and hold `run/world/session.lock`; kill it (`ps -eo pid,args | grep gameTestServerRun`) before the next run. Merge conflicts in `CLAUDE.md`/`PROGRESS.md` come from every PR adding a block before `## Phases`: merge `main` into the branch and keep both sides. In-game checks with the headless client need the `quickPlaySingleplayer` hack in `build.gradle` (restore it right after launch; the world 'New World' has cheats off, so `/void_reclaimer` commands are not available).
- **Playtest feedback, round 1 (owner, 2026-10-30, up to stage 5 of `docs/PLAYTEST.md`).** Work through these as small PRs; decisions already made are in brackets.
  - Bugs: punch-break (fixed in its PR); drone bobbing in a 1x1 tunnel when touching the ground; Mech plays its flying animation once and then idle; Mech idle animation and the blue foot flames floating mid-air; energy bar drawn as a comb (the art strips are 90x9 and were squeezed into 9x9; fix = draw one cell with the sub-region `blitSprite`); Ship, Shuttle and Titan are "too large" in F5 (camera distance/scale, not invisible); 70 hearts of health shown for a big body.
  - Start of the game: [starter dust + guided first craft of the Dust Sensor, plus a faint built-in hum for the bare Drone]; early dust collection is boring when clouds are far apart (open).
  - Dust HUD: M/S/I/C text is unclear; wants labelled coloured icons with bars.
  - Replicator: a short "what is this and why spend dust on it" description per recipe; reorder queued jobs; JEI on the Replicator screen.
  - Inventory: [our tabbed screen replaces the inventory on E: Inventory, Replicator, Body, Logs; no armor slots or crafting grid for the Drone and Mech; R jumps to the Replicator tab].
  - Respawn menu: layout and feel are off (redesign with the inventory work).
  - Crafting: [Replicator for our early items, Create machines for the chain items, no vanilla table recipes for ours; every item having a Replicator recipe is a testing convenience only].
  - Cameras: [an orbit camera and a photo mode]; scroll zoom and free rotation of the model. Check NeoForge's camera events before building (research in chat: Free Camera and Unbound Third Person show the two styles).
  - Verification: wants the Create recipes checked by image: plan is a headless-client tool that opens every recipe in JEI and saves screenshots, plus a GameTest that every ingredient and result exists.
- **Playtest checklist:** `docs/PLAYTEST.md` (first dust to first launch, with skip commands). The owner plays it and reports back by stage; fix what comes back.
- **Flaky GameTests (2026-10-30: timing races fixed, watch CI):** the sunlight-charging test waited a fixed 41 ticks for a core to find sky light, and tests that force-load a chunk waited a fixed 5 ticks before it ticked. `NaniteGameTests.inLoadedArea` now waits until the chunk ticks entities (up to 100 ticks) and the sunlight test waits for the charge (up to 400 ticks). The salvage test already waited for the chunk. If one still fails, run it alone with `-PgametestFilter=<name>` first (alone it passed 4 of 4 here), then look for shared state with the other tests.
- **RD art pass 1 has not been looked at in a real client yet.** Unit tests load every model, the texture sizes are checked, and a comparison sheet looked right. In a client with Sodium and the other performance mods, check the asteroid rocks, the Electrolyzer and Spinner faces, and a few icons in the Replicator. Then update the placeholder list in `PROGRESS.md`.
  - Known colour issue: `asteroid_silicate` came out orange; the style guide says sand-tan. `carbon_flakes` reads as a dagger and `dyson_shell_truss_module` as a hooked tool. The owner decides whether to redo them (Redo in the picker).
  - Still placeholders: `metallic_grains`, `silicon_carbide_coating`, `micro_alloy_sheet`, and the empty `core_mk1_accent` mask (paint it in Blockbench).
- **Kit-queue glitch:** once, in one client session, clicking Replicate on a kit did nothing: no job, no dust taken, no message. It didn't reproduce after a restart, and logging showed the request arriving and queueing correctly. It's listed in `PROGRESS.md` known issues. If it comes back, log both sides:
  - client: `ReplicatorScreen` replicate press;
  - server: `Replicator.queue`.
- **Phase 7b planning details** are still open: Space Thruster stats, the Helm block, docking. The overall 7b design is settled in `CLAUDE.md`.

## Create Aeronautics spike (branch `claude/aeronautics-spike`, never merge)
- Full results are in `AERONAUTICS_SPIKE.md` on that branch. The decision is in `CLAUDE.md`: superseded: Aeronautics is a required dependency since the Foundations phase (2026-10-29); ships come in Phase 7b.
- **Key facts:**
  - **Gravity:** Sable needs a `data/<ns>/dimension_physics/*.json` with `base_gravity` 0 for zero-G. Without it, ships fall; that was the first spike's "flung away" bug.
  - **No thrust in vacuum:** propellers and balloons need `base_pressure` > 0, so they give no thrust in deep space.
  - **Riding works:** players can ride a moving ship.
  - **Removal range:** Sable removes sub-levels outside the config `sub_level_remove_min` (-10,000) / `sub_level_remove_max` (100,000).
  - **Useful commands:** `/sable spawn platform`, `/sable assemble connected x y z`, `/sable info @n`.
  - **Startup deadlock:** there is an intermittent deadlock between Aeronautics/Simulated/Veil and Create's render classes in client-dist runs (the game and `runData`). Aeronautics is required now, so `runData` loads it (and the GameTest runs can stall the same way: one CI run sat 18 minutes in GameTests, and CI now retries a GameTest run that exceeds 15 minutes): CI retries `runData` (5-minute timeout, 3 attempts), and locally `timeout 300 ./gradlew runData` and run it again if it hangs.
- **sable64bit** (Chelovechic/sable64bit-1.21.1, AGPL) gives f64 physics.
  - At X = 20,000,000.5 it held the position exactly; f32 lost half a block.
  - Recommend it to players; never depend on it.
  - **Never commit its jar** (licence). `libs/` is gitignored on the spike branch.
  - The jar is built locally with `cargo build`. Copy the native library into `build/generated/nativeResources`, then run `./gradlew jar -x copyAllSable64Natives -x buildAllSable64Natives`.
  - Load it on the spike branch with `-PspikeF64=true`.

## Taking over on a PC
The owner wants to run Claude Code on their own PC. A new session there starts from these files, so keep them current.
- **Read first:** `CLAUDE.md`, this file, `PROGRESS.md`. The first task is the to-do and the playtest feedback list under "Open items".
- **Needs:** Java 21 (Temurin), Git, the GitHub CLI `gh` logged in, Python 3. Gradle comes from the wrapper (`./gradlew`; on Windows `gradlew.bat`, or use Git Bash/WSL so the shell snippets here work). The first build downloads Minecraft, NeoForge, Create, Aeronautics, Sable, JEI and the performance mods (see `gradle.properties`); later builds are much faster because the Gradle caches stay warm.
- **Faster there:** warm caches and a local GPU client. `./gradlew runClient` opens the real game (Sodium, ModernFix, FerriteCore and ImmediatelyFast load with it); with a real display you can play the playtest checklist yourself and screenshot what you see instead of the headless harness in `tools/client-test/` (Linux, Xvfb).
- **Not faster:** CI (same wherever you push from), and the upstream Aeronautics start-up deadlock that makes `runData` hang now and then (CI and local both retry; run `./gradlew --stop` and try again).
- **Commands that matter:** `./gradlew build` (unit tests), `./gradlew runGameTestServer` (look for `All N required tests passed`), `./gradlew runData` (commit `src/generated`), `./gradlew runClient`.
- **Check before every push** (CLAUDE.md workflow rule 8): build, GameTests, and `runData` if datagen changed. A flaky test gets a root-cause fix, not a re-run.
- **Branches:** one small PR per branch from the latest `main`; never push to `main`. The cloud sessions used branch names like `fix-...` and the one the harness assigned; use any clear name on the PC.

## Working tricks
### Commands
- `./gradlew build` (includes unit tests).
- `./gradlew runGameTestServer`: look for `All N required tests passed`. Add `-PgametestFilter=ZeroGMining` to run only the tests whose class or method name contains the text (comma-separated alternatives, ignoring case); about 30 s for one class instead of about 3 minutes. Only `runGameTestServer` passes the property on.
- `./gradlew runData`: commit the output in `src/generated/resources`. CI fails if it's stale.
- For a GameTest failure, grep the log for `failed at` and the message.

### GameTests
- **Kinetic block entities in a GameTest:** Create resets the speed of a block with no source, so a test sets the speed before every tick (`MachineGameTests.tick`). Call `tick()` once first so the block settles onto the network.
- **Asteroids and fixed test coordinates:** a fresh world puts asteroids in random places; the chance one overlaps a given test position is tiny (rock fills well under 0.1% of space), but tests that spawn sub-levels clear their area first (`AeronauticsGameTests.clearArea`). Do the same in a new test that needs guaranteed empty space.
- **The GameTest world persists and saves each dimension's generator.** After changing a generator or dimension JSON, delete `run/world` before `runGameTestServer`, or the old one is used (CI is always fresh).
See `CLAUDE.md` → Testing.
- Give each test its own X coordinate. Existing tests use 12,000–73,000, mostly in steps of 1,000; grep `survivalInDeepSpace(helper,` and pick a free one.
- Test-only content lives in `src/main/resources/gametest_pack` (namespace `void_reclaimer_test`). It is only loaded on the GameTest server.
- `TestEventListeners` reacts to tagged test players.
- **Entities that move:** force-load every chunk they can reach, not only the start chunk. An entity in a chunk that isn't loaded doesn't tick. Wait for `isPositionEntityTicking` in a sequence before spawning, not a fixed delay. Release the chunks at the end; runs that left many forced chunks behind once hung the server while it saved on shutdown. See `StarshipEntityGameTests`.
- **Don't start a second `helper.startSequence()` from inside a running sequence step** (the sequence list is iterated while it ticks). Chain steps on one sequence instead.
- **Long jobs:** to skip a long Replicator job, rewrite the job's progress with `Replicator.store` (see `ReplicatorGameTests.kitsDeliverEveryPartAndKeepSalvagePrices`).

### Test client helper commands
`tools/client-test/xt.py` also has `chord ctrl+f` (modifiers plus a key), `type "text" 0.3` (a gap between keys, for boxes that refilter on every key) and `mdrag DX DY [steps [shot.png]]` (hold the middle mouse button and move; the optional PNG is taken while the button is held, which is how the orbit camera was checked: the camera swings round the body and eases back after release).

### JEI recipe screenshots (done 2026-10-30)
`python3 tools/client-test/jei_shots.py [--out build/jei-shots] [--only item,item]` screenshots JEI's recipe view for every item our machine recipes make and writes `index.html` beside the PNGs. It needs the headless client at **1920x1080 on a 1920x1080 Xvfb** (at 1280 wide JEI shrinks its list and the search box is too small to use): temporarily add `programArguments.addAll '--quickPlaySingleplayer', 'New World', '--width', '1920', '--height', '1080'` to the `client` run in `build.gradle` (never commit it), start `Xvfb :99 -screen 0 1920x1080x24`, run `DISPLAY=:99 ./gradlew runClient`, wait for "joined the game", then run the script. Gotchas found while building it: JEI drops keys typed too fast (`xt type "text" 0.3` takes a gap), the `r` key typed into a focused search box instead of showing recipes (click the result to show recipes), and a recipe with several pages shows only its first.

### In-game client checks (headless)
Helpers are in `tools/client-test/` (`client.sh`, `xt.py`).
1. **Launch:** temporarily add `programArguments.addAll '--quickPlaySingleplayer', 'vrtest'` to the `client` run in `build.gradle`. **Never commit this**: back up `build.gradle` and restore it afterwards. Then:
   - `source tools/client-test/client.sh; xstart`
   - `(DISPLAY=:99 nohup ./gradlew runClient > /tmp/client.log 2>&1 &)`
   - Wait for `joined the game` in the log, about 2 minutes.
2. **First load:** the first load shows the "Experimental Settings" screen. Click "I know what I'm doing" at (878, 464).
3. **Set up the player** with `run "/command"`. Useful commands:
   - `/void_reclaimer dust set @s void_reclaimer:metallic 200`
   - `/void_reclaimer milestone grant @s void_reclaimer:expansion`
   - `/void_reclaimer body reset @s`
   - `/gamemode survival`
4. **Screen coordinates** (1280×720, GUI scale 3):
   - The Replicator (key R) panel spans (160, 61) to (1120, 661).
   - Tabs are at y 137: Tools 250, Modules 400, Parts 553, Blocks 706, Logs 860, Body 1013.
   - Recipe rows start at y ≈ 205, 66 px apart. Replicate is at (311, 613).
   - Scroll with `xt scroll -N` while hovering the list.
5. **JEI:** open the inventory (E), hover an item with `xt hover X Y`, then press R for its recipes.
6. **Screenshots:** `shot file.png`, then view the PNG.
7. **Stopping:** `kill $(clientpid)`. Rebuild by relaunching. There is no sound in the container, so check sound changes on a real machine.

- **Seeing a body in the headless client:** the art's textures are near-black, so stand in a lit room first: `run "/fill ~-15 ~-15 ~-15 ~15 ~15 ~15 minecraft:sea_lantern hollow"`, then `run "/void_reclaimer body preset @s mech"` (or `drone`, `ship`) and F5 for third person (F5 cycles first person, back, front). Wait about 45 s after "joined the game" before typing commands (the world is still loading). Zoom screenshots with ImageMagick `convert shot.png -crop WxH+X+Y -filter point -resize 300% out.png`.
- **A hung GameTest or client JVM:** kill it by PID (`ps -eo pid,args | grep gameTestServerRunProgramArgs`). `salvageToolDismantlesForDustWithoutDrops` is flaky locally too, also on a fresh world (see "Open items"). A fresh-world local run can also run out of heap near the end.

### Placeholder art
- **Test Starship** (`void_reclaimer:test_starship`): a placeholder entity for checking RD models and animations in game. To replace its model, see `PROGRESS.md` ("Test Starship").
- **Art pipeline:** see "Art workflow" below. `docs/art/ASSETS.md` + `assets.csv` list every asset with Retro Diffusion prompts. The owner generates them with `tools/art/rd_generate.py` on their own computer (their key; never ask for it or store it). When they say a batch is promoted, check it in game and update the placeholder list in `PROGRESS.md`. `--promote <asset>` converts an RD `.bbmodel` into a part's GeckoLib model (`tools/art/bbgeo.py`): it keeps our skeleton, scales to fit and bakes rotated face UVs (GeckoLib 4.9.3 only supports geometry 1.12.0 cleanly). `GeoModelLoadTest` (Gradle unit test) loads every `.geo.json` through GeckoLib's own loader. `--from-group`/`--scale-like` cut Ship sections out of one whole-ship model; `--promote-as entity/NAME` writes a standalone model plus converted animations (Blockbench's Bedrock export rules; GeckoLib reads only `post`, so step keys become holds). `GeoModelLoadTest` also loads every animation and checks its bones (body animations may move section/module bones). Body animations from RD still need Blockbench. The style guide lives in `docs/art/style.json` (regenerate with `tools/art/style_guide.py`; a test checks it is current). The owner chose to pick custom-style reference images later (1-4 images in `docs/art/style/<category>/`, then `--setup-styles`).
Remaining placeholder textures are 16×16 programmer art, written by small Python PNG scripts (zlib + struct, no PIL). Every placeholder must be listed in `PROGRESS.md`.

## Art workflow (the owner works on a phone in Termux and on a PC)
- **Generate and pick:** the owner runs `tools/art/rd_generate.py` with their own Retro Diffusion key (never ask for it, store it or commit it; it lives only in their environment). Results stage in `art/rd/` (gitignored). `art/rd/index.html` is a single self-contained page: tap a variant to promote it, or mark Keep current or Redo, add notes, and copy the commands it builds.
- **Promote:** `--promote asset=N` copies a picked variant into the mod, size-checked, and records it in `docs/art/promoted.txt`. `asset_list.py` reads that file, so regenerating `docs/art/ASSETS.md` and `assets.csv` (`python3 tools/art/asset_list.py`) marks those files as `RD art (promoted)`. `--status placeholder` then skips them.
- **Getting their work to you:** `art/rd/` is gitignored, so the owner pushes it on a throwaway branch with `git add -f art/rd` (see `tools/art/README.md`, "Moving staging between devices"). The branch `art-staging` on GitHub is only a courier: never merge it, and never commit `art/rd` to `main`. To use what is on it, `git fetch origin art-staging`, then `git checkout origin/art-staging -- src` for the promoted files (and `-- art/rd` plus `git reset -q art/rd` to look at the variants). Then make a normal branch and PR.
- **Models:** `--promote <asset>` (keeps our skeleton), `--from-group` / `--scale-like` (cut Ship sections out of one whole model), `--promote-as entity/NAME` (standalone model plus converted animations, used by the Test Starship). The owner also chose RD's Blocky or Blocky Lite 3D styles only.
- **Style:** `docs/art/style.json` is the source for the style guide and the per-category palettes (`python3 tools/art/style_guide.py`; a test checks they are current). `--setup-styles` makes RD custom styles from 1 to 4 reference images in `docs/art/style/<category>/` (the owner picks those later).
- **Tests:** `python3 tools/art/test_rd_generate.py` (27 tests against a fake RD server; Node is used for the picker's command builder; CI runs it). Mutation-check new ones like any other test.
