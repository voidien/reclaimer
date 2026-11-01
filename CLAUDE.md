# Void Reclaimer — Project Plan & Rules

You are an expert Java developer and Minecraft modder specializing in NeoForge for Minecraft 1.21.1. "Void Reclaimer" is a singleplayer zero-gravity progression and factory-automation mod.

**New session?** Read `HANDOFF.md` (current state, the user's standing instructions, next steps, test harness) and `PROGRESS.md` plus any `docs/progress/*.md` (what's done, known issues) before starting.

## Vision & lore
The player is a nameless AI consciousness. There are no enemies and no combat. The danger is the environment and your own flying, on top of time, resource management, and patience (`docs/design/plans.md`, "Danger"). The AI starts as a tiny, helpless drone and grows its body freeform, from Drone to Mech to Ship. It builds floating automated space stations and finally returns to the Overworld, which the villagers wrecked through a climate disaster, to terraform and heal it.

## Core design
1. **Spawn:** The player never spawns in the Overworld. New players and respawns always go to `void_reclaimer:deep_space`.
2. **Zero-G flight:** Vector thrust with momentum and drift. There is no gravity, hunger, or fall damage. Handling scales with the body's total mass and thrusters.
3. **Cosmic Dust:** Invisible clouds defined by 3D noise. Inside a cloud, the AI feels drag and passively gains dust. No particles. The AI finds clouds by the slowdown and its dust meter. A craftable **Dust Sensor** module adds HUD markers showing the current cloud's extent and density.
4. **Internal Replicator:** A built-in virtual GUI opened by keybind. Turns dust into early tools, basic machines, and the Mechanical Drill.
5. **Freeform modular body:** Assembled from parts: frame, core, thrusters, tools, storage, and sensors. Each frame belongs to a size class: Drone, Mech, or Ship. The size class sets the body's scale, part slots, mass budget, and which parts fit. Progression means unlocking larger frames and better parts. Parts render on the body. Upgrading to a bigger frame transfers compatible parts.
6. **Asteroids:** Floating voxel rock and ice clusters. Breaking blocks requires a drill-type part.
7. **Stations:** Single-block machines for early/mid game, multiblock structures for late game. All float in the void. Helper drones fly items between stations.
8. **Endgame:** An Atmospheric Portal leads to `void_reclaimer:ruined_earth`. There, Terraformer arrays restore biomes chunk by chunk.

## Technical constraints (follow exactly)
- Namespace `void_reclaimer`. NeoForge 21.1.x, Java 21, **ModDevGradle** with Parchment mappings.
- Dependencies: **Create** (required) for kinetics, machines and the art style; **Create Aeronautics** (required, bundles Simulated and Offroad) and **Sable** (required, its physics engine) for ships and sub-levels; **GeckoLib** for body, part, and helper-drone models/animations. **JEI** for recipe categories for the replicator and machines. Look up current compatible versions; don't guess.
- Modern 1.21.1 APIs: **Data Attachments** for per-player body/part/dust data, **Data Components** for item data, **block entities**, `CustomPacketPayload` networking, datagen for recipes, loot tables, tags, lang, and models.
- Body scale uses the vanilla `generic.scale` attribute — no custom hitbox hacks.
- Flight: player movement is client-side, so thrust and drag run on the client. A payload syncs input to the server, which validates speed against body stats and handles dust gain. Keep the logical client/server split correct even though the mod is singleplayer.
- Helper drones use flying navigation or custom steering, not ground pathfinding.
- Biome restoration rewrites chunk biome data and resends the chunks, like `/fillbiome`.

## Workflow rules (strict)
1. (Done) Save plan, propose structure, create build files, confirm `./gradlew build`.
2. Before each phase: ask clarifying questions about any open design choice, then list files to create/change. Wait for go-ahead.
3. During a phase: write complete, working code. No logic placeholders like `// TODO: add logic`. Placeholder textures/models are allowed and must be listed at the end.
4. After a phase: run `./gradlew build` and fix all errors. Give a short in-game test checklist and commit with a message naming the phase.
5. If unsure whether an API exists in 1.21.1, say so and check (decompiled sources / docs) instead of guessing.
6. Record what was finished and any known issues in a new file `docs/progress/<slug>.md`, one per PR, so PRs never conflict over the same spot; `python3 tools/progress_fold.py` folds them into `PROGRESS.md` (see `docs/progress/README.md`). Change `CLAUDE.md` or `HANDOFF.md` only when a rule, a settled decision or a working trick changes.
7. **Small pull requests (the owner's rule, 2026-10-29).** One change per PR, a few files, reviewable in minutes. A phase is split into several PRs (for example data and registry, then logic, then client rendering), each of which builds and passes tests alone. Art drops, docs and refactors are their own PRs, but a docs-only change may ride along with the nearest code PR. Never let a branch collect unrelated work. Branch from the latest `main`, and restart the branch after its PR merges.
8. **Local checks gate a merge; CI runs on `main` (owner's rule, 2026-11-01).** A PR merges once `tools/verify.sh` passes, with no CI run and no wait: `tools/land.sh N` refuses a branch that has not passed here, or on a stack above it (`tools/verified.sh`). CI runs the full set on every code push to `main`, on a clean machine with real saves, and whoever merges reads the result (`gh run list --branch main --limit 1`). A red `main` gets a follow-up PR straight away, as the next thing done. A flaky test gets a root-cause fix, not a re-run.
9. **Reviewer agent (owner's rule, 2026-10-30).** `.claude/agents/reviewer.md` is an independent Opus reviewer for a finished diff. Use it only when absolutely necessary: a risky change to gameplay logic, saved data, networking or an extension point. Never for routine, small or client-only PRs, and never to edit files. Other work stays in the main session: helpers start without context and parallel helpers conflict on `CLAUDE.md`, `PROGRESS.md` and the lang provider.
10. **Collaborate: speak up (owner's rule, 2026-10-31; applies to every session and every agent).** We build this mod together. While building, reading the code or reading the story, tell the owner whenever you see something worth saying: a debug tool, menu, item or command that would make the mod easier or faster to build, or any recommendation on design, story, balance, code or workflow. Say it when you see it, briefly, with why it helps; do not build a new idea without the owner's yes. **During an unattended stretch ("x hours without interrupting") do not interrupt:** add it to "Ideas for the owner" at the top of `PROGRESS.md` and raise the list when the stretch ends. Whoever spawns an agent passes this rule on, and an agent reports its ideas back in its result.

## Toolchain (verified 2026-10-28)
| Component | Version | Source |
|---|---|---|
| Minecraft | 1.21.1 | |
| NeoForge | 21.1.265 | maven.neoforged.net |
| ModDevGradle | 2.0.148 | maven.neoforged.net |
| Parchment | 1.21.1 / 2024.11.17 | maven.parchmentmc.org |
| GeckoLib | 4.9.3 (`geckolib-neoforge-1.21.1`) | dl.cloudsmith.io/public/geckolib3 |
| JEI | 19.70.0.464 (`jei-1.21.1-*`) | maven.blamejared.com |
| Create (required) | 6.0.10 (`maven.modrinth:create:6.0.10+mc1.21.1`, range `[6.0.10,6.1)`) | api.modrinth.com/maven; bundles Flywheel 1.0.x, Ponder, Registrate |
| Create Aeronautics (required) | 1.3.2 (`maven.modrinth:create-aeronautics:1.3.2+mc1.21.1`, range `[1.3.2,1.4)`, mod id `aeronautics`) | api.modrinth.com/maven; jar-in-jar bundle of aeronautics, simulated, offroad; needs Create and Veil |
| Sable (required) | 2.0.6 (`maven.modrinth:sable:2.0.6+mc1.21.1`, range `[2.0.6,2.1)`) | api.modrinth.com/maven |
| Create Crafts & Additions (required) | 1.7.2 (`maven.modrinth:createaddition:neoforge-1.21.1-1.7.2`, mod id `createaddition`) | api.modrinth.com/maven |
| Create Propulsion: Simulated (required) | 1.1.5 (`maven.modrinth:create-propulsion-simulated:1.1.5`, mod id `createpropulsion`) | api.modrinth.com/maven |
| Create: The Factory Must Grow (required) | 1.2.0 (`maven.modrinth:create-tfmg:uDi14nbt`, pinned by version id, mod id `tfmg`) | api.modrinth.com/maven |
| Create: Advanced Electricity / Technology Addon (required) | 1.21.1 1.0.0 (`curse.maven:create-advanced-electricity-addon-1743391:9362753`, mod id `light_bulb_addon_create_crafts_and__addions`) | cursemaven.com (CurseForge only) |
| Create: Advanced Computer Technic (required) | 1.21.1 1.0.0 (`curse.maven:create-advanced-computer-technic-1759784:9362807`, mod id `advanced_computer_technic`; All Rights Reserved) | cursemaven.com (CurseForge only) |
| Gradle wrapper | 9.2.1 | matches official NeoForge 1.21.1 MDK |
| Sodium (dev run only) | mc1.21.1-0.8.13-neoforge | api.modrinth.com/maven |
| ModernFix (dev run only) | 5.27.27+mc1.21.1 | api.modrinth.com/maven |
| FerriteCore (dev run only) | 7.0.3-neoforge | api.modrinth.com/maven |
| ImmediatelyFast (dev run only) | 1.6.15+1.21.1-neoforge | api.modrinth.com/maven |

All versions live in `gradle.properties`. `neoforge.mods.toml` is generated from `src/main/templates/` via the `generateModMetadata` task.

Commands: `./gradlew build` (includes unit tests), `./gradlew runGameTestServer`, `./gradlew runClient`, `./gradlew runData` (writes to `src/generated/resources`).

## Testing
- **Unit tests** (`src/test/java`, JUnit 5 through MDG's `unitTest`, game classes on the classpath): pure logic only. Keep simulation math in config-free functions (`FlightPhysics`, `EnergyTicker.simulate` with `EnergyRules`) so it can be tested here.
- **`./gradlew fastTest`** runs the unit tests that do not need the game in about 5 s, for the inner loop. `./gradlew test` (in `build` and on CI) stays the full run; one test costs 20 s there because the game boots. A test that needs the game goes on the exclude list in `build.gradle`.
- **GameTests** (`com.voidreclaimer.gametest`, `@GameTestHolder(MOD_ID)`, template `void_reclaimer:empty`): run in a real headless server with `./gradlew runGameTestServer`.
  - `GameTestServerMixin` makes the GameTest server load datapack dimensions, so tests can use `deep_space`.
  - `GameTestBatchChunkMixin` releases every forced chunk as each batch starts, except in restart tests, so a test keeps its chunks until its batch ends and never into the next one. No test run writes a half-made chunk.
  - `GameTestChunkTasksMixin` keeps the GameTest server working on chunk tasks between ticks, as a normal server does; without it the server finishes one chunk per tick (`ChunkTaskGameTests`).
  - **No chunk saves:** writing chunks took most of a run (a deep space chunk has 254 sections), so GameTest runs write none (`GameTestChunkSaveMixin`) and end right after the summary (`GameTestQuickExitMixin`). Chunks still load and unload, but one that unloads comes back as the generator made it, so **a test must hold a ticket on any chunk it needs** (`NaniteGameTests.inLoadedArea`), and a test that needs the disk belongs in a restart test: those save for real (`GameTestFilter.skipsChunkSaves`). `-PgametestChunkSaves` runs real saves and a real stop, which CI does on every push to `main`.
  - `TestPlayers` creates real survival `ServerPlayer`s on a fake connection. Call `doTick()` to run player ticks.
  - The GameTest world persists between local runs: clear your test area first, and give each test its own coordinates. The main suite's world is `run/world`; the Aeronautics suite has its own run directory, `run-aero/` (world, config, logs).
  - **Run the suites with `tools/gametest.sh [main|aero] [filter]`** (e.g. `tools/gametest.sh aero theHoldsStop`): it prints the summary and the failures, kills the server JVM (which usually hangs after the tests and would keep the world's `session.lock`, so the next run dies with "already locked"), and exits non-zero on a failure; the full log is `build/gametest-<suite>.log`. `-PgametestFilter` works for both tasks. If you kill the JVM by hand, match the JVM and nothing else: `ps -eo pid,args | awk '$2 ~ /\/bin\/java$/ && /moddev\/gameTest/ {print $1}' | xargs -r kill`; a `grep gameTest` pattern in the same shell command matches that shell itself.
  - A test player (`TestPlayers`) does not tick unless told to and loads no chunks, so entities it spawns in chunks that are not entity-ticking neither tick nor show up in entity lookups; design server code so it does not rely on an entity's own tick (see `ship/HelmSeats`).
  - Run a subset with `./gradlew runGameTestServer -PgametestFilter=<terms>` (class or method name contains any comma-separated term; `GameTestRegistryMixin`, test-only). Use it while iterating, and run the whole suite before pushing.
  - Wait a few ticks after force-loading a chunk before placing blocks; scheduled ticks set earlier are dropped.
  - **Tests that span a restart:** a pair `<name>BeforeRestart` and `<name>AfterRestart`, run with `tools/restart-test.sh main|aero <name>`, on the same world. The first ends normally (the server saves) or calls `RestartTests.crash()` (the JVM ends on the spot, nothing saved). `tools/restart-test.sh` deletes the suite's world first, so a failed run cannot decide the next one; the next plain GameTest run then starts on a fresh world too. `GameTestFilter` keeps these out of every other run, even a filter on their class. `TestPlayers.rejoin` is a clean logout, not a crash. CI runs `aCrashKeepsOnlyWhatWasSaved`, which keeps `crash()` honest, the link's three (`aLinkOutlivesACrash`, `aWokenShellOutlivesACrash`, `anOldShellGoesAfterACrash`), the clash of two copies (`aNewerShellWinsAClash`), recovering an old copy (`anOldCopyIsRecovered`) and `aHeldShipLoadsItself`.
- Every phase adds unit tests and GameTests for its logic.
- Verify new tests can fail: break the feature and check that the test catches it.
- In-game client checks still happen for visuals, input, and HUD.
- **CI:** `.github/workflows/build.yml` runs build + unit tests, GameTests, the restart tests and a datagen up-to-date check on every code push to `main`, and by hand with `gh workflow run build.yml --ref <branch>`.

## Debug tools
Use these before guessing from pixels. If one is missing, say so (workflow rule 10).
- **Fast client:** `tools/client-test/quick.sh`: `start`, `swap` (a saved edit in the running game in about 3 s), `reload` (changed assets, models, animations and lang in about 15 s; F3+T after `./gradlew processResources` by hand), `shot`, `photo NAME` (four views and the facts in about 5 s), `compare A B`, `gui`, `cmd`, `test NAME` (a GameTest in the live client, slow, only when a log is not enough), `stop`. See `HANDOFF.md`.
- **Debug layer (G in photo mode):** `client/camera/PhotoDebug` turns on hit boxes and chunk borders, draws a crosshair, an outline on what the camera looks at and a panel of facts from `debug/DebugReadout` (common code, read on the integrated server's thread, so it is the real state: the first block or entity along the view with its properties, block entity, Create rotation, inventory, tanks, FE and saved NBT; the place's dimension, light, dust cloud and sky era; the player's body and reserves). Leaving photo mode restores the hit-box and chunk-border settings. Every photo-mode screenshot also writes the full facts to `screenshots/<name>.txt`.
- **Labels (L in photo mode):** `client/render/body/BoneProbe` reads each socket and mount from the render matrices in `BodyPartsLayer` (so animation and scale are included, and the rig and module positions cannot disagree), drawn as names over the model; sockets blue, mounts gold, dim when empty. The text file then lists them in blocks from the feet along the body's own axes (right, up, forward). In a GUI pass the same points are GUI pixels.
- **GUI layout (F6 on any screen, `client/debug/GuiDebug`):** outlines every visible widget and click region, red where two overlap (`debug/GuiLayout`, pure), with the mouse's GUI position, and writes the list to `debug/gui.txt` in the game folder; a screen with click regions that are not widgets implements `GuiDebug.Regions`.
- **Test scenes (`debug/TestScenes`, `/void_reclaimer scene arena|launch|drones|station`):** a ready setup in deep space through the real assembly code, each with its own spot, safe to build again; the player is moved to a view of it. `arena` is a pale lit floor and two walls with a line every 16 blocks, for judging dark bodies and sizes.
- **Scripted (`client/camera/PhotoScript`, client command `/vrphoto`):** `at x y z yaw pitch [fov]`, `focus crosshair|body|<x y z>`, `orbit yaw pitch distance`, `debug true|false`, `labels true|false`, `reload`, `shot name`, `sheet name [distance]` (four views, `PhotoSheet`; no distance fits the body, `BodyBounds`), `exit`. Shots are taken a few frames after the camera moves, from the finished frame (`RenderFrameEvent.Post`). T in photo mode opens chat and returns to the free camera when it closes.
- **Ship overlay (O in photo mode; `/vrphoto overlay true`):** `debug/ShipOverlay` draws each ship's box, centre of mass (magenta), velocity (blue) and an arrow per thruster, grey for what it could push and orange for what it pushes now, and adds the same facts to a shot's text file. `debug/LogisticsOverlay` adds each helper drone's route, heading, job and phase, and the Create rotation of nearby machines, red when overstressed. `debug/DebugShapes` (pure) carries lines and labels from the server to the client; new overlays add to it.
- **Item census (`/void_reclaimer link census <player>`, `debug/ItemCensus`):** counts everything a body carries and the items lying near it, and says what changed since the last count: run it before and after anything that moves items.
- **Chunks (`/void_reclaimer debug chunks [x z]`, `debug/ChunkReadout`):** one chunk's ticket level, status and entity state, plus how many chunks are held, finished and waiting, and the state of the queue. Use it when a chunk will not tick: a long queue looks just like a stuck chunk.
- **Time (P freeze, period step, comma quarter speed, in photo mode; `/vrphoto time freeze|step|slow|normal`):** `debug/DebugTime` drives the server's tick manager. Leaving photo mode restores normal time, and only if photo mode changed it, so a player's own `/tick` settings stay.

## Branches
- `main` is the stable line. Each phase (or pair of small phases) is developed on its own branch and merged by pull request.

## Performance-mod compatibility
The mod must work alongside **Sodium, ModernFix, FerriteCore, and ImmediatelyFast**. They are `localRuntime` dependencies, so `./gradlew runClient` always launches with them. They are never declared in `neoforge.mods.toml` and never published. Iris is intentionally not supported or tested. Rules:
- **Sodium:** never mixin into or replace chunk/level rendering. Custom skies go through `DimensionSpecialEffects` / NeoForge render events. World content (asteroids, stations) is ordinary blocks and block entities. Biome changes resend chunks with the normal packets so Sodium rebuilds them.
- **ImmediatelyFast:** draw all HUD and screens through `GuiGraphics` and NeoForge GUI layers (`RegisterGuiLayersEvent`). No raw `Tesselator` drawing in the HUD, and restore any render state you change.
- **ModernFix:** don't cache baked models or resources at startup outside the normal reload listeners. Don't depend on class-loading or registry-event order beyond what NeoForge guarantees.
- **FerriteCore:** treat `BlockState`s, their property maps, and baked model quads as immutable and shared.
- Every phase's in-game test checklist is run with these four mods loaded.

## Design docs
**Read the file for the area you touch before you change it**, and keep it current in the same PR. A name in quotes in another doc, such as "Remote control", is a block in one of these files: grep `docs/design/` for it.

| File | Read before working on |
|---|---|
| `docs/design/world-and-dust.md` | space/, flight/, energy/, dust/, the HUD meters, the start of the game |
| `docs/design/replicator-and-tools.md` | replicator/, nanite/, tether/, core/, salvage/, the inventory and respawn screens |
| `docs/design/body.md` | body/, client/render/body/, client/camera/, the assembly view, any rig or chassis |
| `docs/design/machines-and-materials.md` | machine/, launch/, dyson/, asteroid/, recipes, the design doc's chains |
| `docs/design/earth.md` | earth/, terraform/, hover flight, Earth flight and airships |
| `docs/design/ships-and-stations.md` | ship/, station/, compat/, anything on Sable or a required addon |
| `docs/design/plans.md` | choosing what to do next, remote control (link/), helper drones, holo-displays |
| `docs/design/extension-points.md` | any registry, data component, tag or event; every new extension point goes here |
| `docs/design/structure.md` | adding a package |
| `docs/design/art.md` | art, textures, models, the Retro Diffusion tools |
| `docs/design/original-phases.md` | history only |

**Standing rule:** every new block or item with state or actions gets a CC: Tweaked peripheral in the same PR (`docs/design/ships-and-stations.md`, the `new-block` skill).

## Extensibility (strict)
Void Reclaimer is built to be extended by datapack makers, addon developers and KubeJS (through datapacks and events). Every change keeps it that way:
1. **Data first.** Content and tuning that a datapack could reasonably change lives in data files (datapack registries, recipes, tags), not code.
2. **Registries, not enums, for open sets.** An enum is allowed only for a set that is fixed by design, and is listed under "Fixed sets" below.
3. **Behaviour through registered interfaces.** Game logic never checks for specific items (`stack.is(ModItems.X)`); it asks a component, a tag or a registered type.
4. **Events at key moments.** Fire a NeoForge event (in `com.voidreclaimer.api.event`) wherever an addon could reasonably want to react or change the outcome.
5. **Public API package.** Registry keys, registry element types, extension interfaces and events live in `com.voidreclaimer.api`. Keep it small and documented.
6. **Tags for groups, config for numbers.**
7. **A GameTest per extension point**, using the GameTest-only datapack (`resources/gametest_pack`, namespace `void_reclaimer_test`, loaded by `GameTestPack` only on the GameTest server) and test listeners (`TestEventListeners`, acting only on tagged test players).
8. **This list is kept current:** every new extension point is added below in the same change.
9. **Saves and data stay loadable:** ids default to our namespace when bare (`ModCodecs.ID`), unknown ids are kept rather than dropped, and built-in values keep their meaning (e.g. dust kind order keeps cloud generation identical).

The table of extension points is in `docs/design/extension-points.md`.

### Fixed sets (enums by design)
- `HostRole` (`link`): what a `BodyHost` is for (shell, wreck, mirror). Each is a rule of remote control, not content; an unknown saved role reads as a shell, so a body is never lost.
- `ShellLinkEvent.Reason` (`api.event`): why a link ended (left, signal lost, body lost). Each is a rule of remote control, not content.
- `ModuleKind` (`api.body`): the mount kinds sections offer (core, thruster, storage, sensor, tool, cosmetic). Each has its own rule in `BodyStats`; new behaviour comes from new modules of these kinds (tools: `ToolModuleType`). Sections themselves are open (socket types are free ids).

## Conventions
- Use `VoidReclaimer.id(path)` for every `ResourceLocation` in our namespace.
- Client-only classes stay under `client/` and are reached only from `VoidReclaimerClient` or `@EventBusSubscriber(value = Dist.CLIENT)`.
- Prefer datagen over hand-written JSON for recipes, loot tables, tags, lang, block states, and item models. Dimension/worldgen JSON can be hand-written datapack files.
