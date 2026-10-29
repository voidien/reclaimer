# Handoff notes

How to pick up Void Reclaimer work in a new session. Read `CLAUDE.md` first: it holds the project rules, the settled design decisions and the extension points. Then read `PROGRESS.md` for what is done and the known issues. This file covers everything else: where things stand, the user's standing instructions, what comes next, and the working tricks that aren't written down elsewhere.

Last updated 2026-10-29, after PR #14 (extensibility + body sections) was merged into `main`.

## Where things stand
- **`main`** has Phases 1–5a done, plus the extensibility pass and body sections:
  - datapack registries, events, chassis and sockets;
  - paint, wear and repair, Body Blueprints;
  - kits (multi-output Replicator recipes).
- **Last verified on `main`:** build, 67 unit tests and 63 GameTests pass, CI is green. The Body tab, kits and the JEI kit layout were checked in a real client with the performance mods.
- **Design doc:** the user's "Nebulae to Planetary Genesis" doc extends the plan (see `CLAUDE.md`, "Design doc" and "Dyson Sphere" blocks). Lore and recipes come only from that doc; ask when it's unclear.
- **Aeronautics GameTests:** `./gradlew runGameTestAeronautics` (separate from `runGameTestServer`, own CI job). The local GameTest world persists, so old test platforms stay put there; CI is fresh.
- **Next phase:** 5b (GeckoLib rendering) or Act III, phase 11 (Ruined Earth); see the revised plan in `CLAUDE.md`. Phases 6 to 10 are done. 5b (GeckoLib rendering, art imported) and 7b (Aeronautics ships) are parallel tracks.
- **Design doc:** https://claude.ai/artifact/YD1avpfCtfdKcZotESDR1n
  - It's a Claude Docs project, at revision 12.
  - It does **not** yet describe the body sections, paint, wear, blueprint and kit work merged in #14. Update it from the "Body sections" block in `CLAUDE.md`.

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
- **Art pipeline:** `docs/art/ASSETS.md` + `assets.csv` list every asset with Retro Diffusion prompts. The owner generates them with `tools/art/rd_generate.py` on their own computer (their key; never ask for it or store it). When they say a batch is promoted, check it in game and update the placeholder list in `PROGRESS.md`. `--promote <asset>` converts an RD `.bbmodel` into a part's GeckoLib model (`tools/art/bbgeo.py`): it keeps our skeleton, scales to fit and bakes rotated face UVs (GeckoLib 4.9.3 only supports geometry 1.12.0 cleanly). `GeoModelLoadTest` (Gradle unit test) loads every `.geo.json` through GeckoLib's own loader. `--from-group`/`--scale-like` cut Ship sections out of one whole-ship model; `--promote-as entity/NAME` writes a standalone model plus converted animations (Blockbench's Bedrock export rules; GeckoLib reads only `post`, so step keys become holds). `GeoModelLoadTest` also loads every animation and checks its bones (body animations may move section/module bones). Body animations from RD still need Blockbench. The style guide lives in `docs/art/style.json` (regenerate with `tools/art/style_guide.py`; a test checks it is current). The owner chose to pick custom-style reference images later (1-4 images in `docs/art/style/<category>/`, then `--setup-styles`).
Item textures are 16×16 programmer art, written by small Python PNG scripts (zlib + struct, no PIL). Every placeholder must be listed in `PROGRESS.md`.
