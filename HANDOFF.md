# Handoff notes

How to pick up Void Reclaimer work in a new session. Read `CLAUDE.md` first: it holds the project rules, the settled design decisions and the extension points. Then read `PROGRESS.md` for what is done and the known issues. This file covers everything else: where things stand, the user's standing instructions, what comes next, and the working tricks that aren't written down elsewhere.

Last updated 2026-10-29, after the RD art pass 1 PR (promoted icons, textures and the first RD module model) was merged into `main`.

## Where things stand
- **`main`** has Phases 1 to 10 done, plus everything around them. In order:
  - Phases 1 to 4: deep space and spawn, zero-G flight, cosmic dust, the Replicator (4a), Nanite Swarm, Tether Anchor and Backup Core (4b).
  - Phase 5a body data, then the extensibility pass and body sections (datapack registries, events, chassis and sockets, paint, wear, Body Blueprints, kits).
  - Foundations: Create, Aeronautics and Sable required; Dyson Sphere counters and sky.
  - Phase 6 asteroids; Phase 7 machines (nebula materials, Create chains, Electrolyzer, Spinner, fan freezing, spout casting).
  - Phase 8 Zero-G Frame and payloads; Phase 9 Kinetic Mass Driver, Quantum Receiver and power beams; Phase 10 the Starship milestone and the Ship recipes.
  - The art pipeline (`tools/art/`, see "Art workflow" below), art drop 1, RD art pass 1, and the Test Starship placeholder entity.
- **Last verified on `main`:** build with 112 unit tests; 103 GameTests (`runGameTestServer`); the Aeronautics GameTests; CI green. Two GameTests are flaky, see "Open items".
- **Not done yet** (revised plan in `CLAUDE.md`):
  - **5b:** GeckoLib body rendering and the assembly UI. The art is imported, and the Test Starship already proves that a GeckoLib entity (model, texture, animations, renderer) works in this setup, so use `starship/TestStarship` and `client/render/TestStarshipRenderer` as a working example.
  - **7b:** Aeronautics ships (Space Thruster, Helm). Stations and helper drones.
  - **Act III, phases 11 to 14:** Ruined Earth, the Terraforming Spire, the Data Monolith and villagers, Restoration.
- **Next phase:** the owner's choice between 5b and phase 11. Ask first (`CLAUDE.md` workflow rule 2): clarifying questions, the file list, then wait for the go-ahead.
- **Design doc:** the user's "Nebulae to Planetary Genesis" doc extends the plan (see `CLAUDE.md`, "Design doc" and "Dyson Sphere" blocks). Lore and recipes come only from that doc; ask when it's unclear.
  - https://claude.ai/artifact/YD1avpfCtfdKcZotESDR1n (a Claude Docs project, at revision 12).
  - It does **not** yet describe the body sections, paint, wear, blueprint and kit work, nor the later phases. Update it from the blocks in `CLAUDE.md` if the owner asks.
- **Aeronautics GameTests:** `./gradlew runGameTestAeronautics` (separate from `runGameTestServer`, own CI job). The local GameTest world persists, so old test platforms stay put there; CI is fresh.

## The user's standing instructions
- **Clarifying questions:** ask them before each phase (their preference, and `CLAUDE.md` workflow rule 2). Wait for a go-ahead on the file list.
- **"Merge all once CI is green":** merge your PRs yourself once CI passes.
- **"Stop listening to pull requests unless they are from me":**
  - Don't subscribe to PR activity for PRs you open.
  - Check CI the next time the user writes, and merge if it's green.
- **One branch and one PR per phase** (or per pair of small phases). Never push to `main` directly.
- **Commits:** name the phase in the message. Keep model names out of commits, PRs and code.
- **Testing:** every phase adds unit tests and GameTests.
  - Mutation-check each new test: break the feature, run the test, see it fail, restore.
  - Back up the file first (e.g. `cp X /tmp/x.bak`) and restore from it.

## Open items
- **Two flaky GameTests (root cause not found, worth their own PR):**
  - `placingregisterscoresuptothelimitandsunlightcharges` ("sunlight did not charge"; failed on PRs #28 and #30) and `salvagetooldismantlesfordustwithoutdrops` (PR #27).
  - Each failed once in CI and passed on the one allowed re-run of the failed job. The PR-triggered run and the push-triggered run of the same commit disagreed, so it is timing or world state, not the PR's change.
  - If one fails again: re-run the failed job once, comment on the PR, and keep going. Don't skip or disable the test. To fix it, run the test in a loop locally and look at what its fixed coordinates share with other tests.
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

## Working tricks
### Commands
- `./gradlew build` (includes unit tests).
- `./gradlew runGameTestServer`: look for `All N required tests passed`.
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
