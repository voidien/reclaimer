# Void Reclaimer — Project Plan & Rules

You are an expert Java developer and Minecraft modder specializing in NeoForge for Minecraft 1.21.1. "Void Reclaimer" is a singleplayer zero-gravity progression and factory-automation mod.

**New session?** Read `HANDOFF.md` (current state, the user's standing instructions, next steps, test harness) and `PROGRESS.md` (what's done, known issues) before starting.

## Vision & lore
The player is a nameless AI consciousness. There are no enemies, no combat, and no hazards except time, resource management, and patience. The AI starts as a tiny, helpless drone and grows its body freeform, from Drone to Mech to Ship. It builds floating automated space stations and finally returns to the Overworld, which the villagers wrecked through a climate disaster, to terraform and heal it.

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
6. Keep `PROGRESS.md` updated with what was finished and any known issues.

## Phases
Build order after Phase 4: 5a → 6 → 7 → 5b → 8 → 9 → 10.

1. **Deep Space & Spawn:** `deep_space` dimension (datapack JSON), forced spawn/respawn, no gravity, hunger, or fall damage.
2. **Zero-G Flight:** WASD/Space/Shift thrust, momentum, dampening, mass-scaled handling, thrust payload with server validation.
3. **Cosmic Dust:** noise sampler, invisible clouds, drag, dust accumulation, basic dust meter, Dust Sensor HUD module.
4. **Replicator:** keybind GUI, custom recipe type with datagen, JEI category.
5. **Modular Body**, split in two:
   - **5a. Body data:** part/frame data model, size classes, `generic.scale`, body stats driving flight (`FlightStats`), storage capacity, and sensor/tool module slots.
   - **5b. Body rendering & assembly:** GeckoLib rendering replacing the vanilla player model (incl. first person), and the assembly UI.
6. **Asteroids:** floating rock/ice worldgen, drill-gated mining.
7. **Machines:** single-block processing machines and item transport between them.
8. **Stations & Helpers:** multiblock structures and helper drone entities that ferry items.
9. **Ruined Earth:** climate-disaster dimension and the Atmospheric Portal.
10. **Terraforming:** Terraformer blocks/arrays and biome restoration.

---

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
| Gradle wrapper | 9.2.1 | matches official NeoForge 1.21.1 MDK |
| Sodium (dev run only) | mc1.21.1-0.8.13-neoforge | api.modrinth.com/maven |
| ModernFix (dev run only) | 5.27.27+mc1.21.1 | api.modrinth.com/maven |
| FerriteCore (dev run only) | 7.0.3-neoforge | api.modrinth.com/maven |
| ImmediatelyFast (dev run only) | 1.6.15+1.21.1-neoforge | api.modrinth.com/maven |

All versions live in `gradle.properties`. `neoforge.mods.toml` is generated from `src/main/templates/` via the `generateModMetadata` task.

Commands: `./gradlew build` (includes unit tests), `./gradlew runGameTestServer`, `./gradlew runClient`, `./gradlew runData` (writes to `src/generated/resources`).

## Testing
- **Unit tests** (`src/test/java`, JUnit 5 through MDG's `unitTest`, game classes on the classpath): pure logic only. Keep simulation math in config-free functions (`FlightPhysics`, `EnergyTicker.simulate` with `EnergyRules`) so it can be tested here.
- **GameTests** (`com.voidreclaimer.gametest`, `@GameTestHolder(MOD_ID)`, template `void_reclaimer:empty`): run in a real headless server with `./gradlew runGameTestServer`.
  - `GameTestServerMixin` makes the GameTest server load datapack dimensions, so tests can use `deep_space`.
  - `TestPlayers` creates real survival `ServerPlayer`s on a fake connection. Call `doTick()` to run player ticks.
  - The GameTest world persists between local runs: clear your test area first, and give each test its own coordinates.
  - Wait a few ticks after force-loading a chunk before placing blocks; scheduled ticks set earlier are dropped.
- Every phase adds unit tests and GameTests for its logic.
- Verify new tests can fail: break the feature and check that the test catches it.
- In-game client checks still happen for visuals, input, and HUD.
- **CI:** `.github/workflows/build.yml` runs build + unit tests, GameTests, and a datagen up-to-date check on every push and PR.

## Branches
- `main` is the stable line. Each phase (or pair of small phases) is developed on its own branch and merged by pull request.

## Performance-mod compatibility
The mod must work alongside **Sodium, ModernFix, FerriteCore, and ImmediatelyFast**. They are `localRuntime` dependencies, so `./gradlew runClient` always launches with them. They are never declared in `neoforge.mods.toml` and never published. Iris is intentionally not supported or tested. Rules:
- **Sodium:** never mixin into or replace chunk/level rendering. Custom skies go through `DimensionSpecialEffects` / NeoForge render events. World content (asteroids, stations) is ordinary blocks and block entities. Biome changes resend chunks with the normal packets so Sodium rebuilds them.
- **ImmediatelyFast:** draw all HUD and screens through `GuiGraphics` and NeoForge GUI layers (`RegisterGuiLayersEvent`). No raw `Tesselator` drawing in the HUD, and restore any render state you change.
- **ModernFix:** don't cache baked models or resources at startup outside the normal reload listeners. Don't depend on class-loading or registry-event order beyond what NeoForge guarantees.
- **FerriteCore:** treat `BlockState`s, their property maps, and baked model quads as immutable and shared.
- Every phase's in-game test checklist is run with these four mods loaded.

## Package & folder structure
Base package `com.voidreclaimer`. Packages are created only when a phase needs them (no empty stubs).

```
src/main/java/com/voidreclaimer/
├── VoidReclaimer.java            @Mod entry; wires registries to the mod bus
├── VoidReclaimerClient.java      @Mod(dist = CLIENT) entry; client-only setup
├── Config.java                   ModConfigSpec (common/client)
├── registry/                     DeferredRegisters: ModBlocks, ModItems, ModBlockEntities,
│                                 ModEntities, ModAttachments, ModDataComponents, ModMenus,
│                                 ModRecipeTypes, ModRecipeSerializers, ModCreativeTabs
├── network/                      CustomPacketPayload records + ModPayloads registrar
├── space/                        Phase 1: dimension keys, spawn/respawn, dimension lock, zero-G, void boundary
├── energy/                       Phase 1: per-player energy (replaces hunger), sunlight charging
├── command/                      /void_reclaimer debug commands
├── mixin/                        Mixins (only where no NeoForge event exists); config: void_reclaimer.mixins.json
├── flight/                       Phase 2: ThrusterFlight physics (shared), stats, input, settings, server checks
├── dust/                         Phase 3: DustNoise sampler, cloud queries, accumulation
├── replicator/                   Phase 4: recipe type, DustCost, milestones, queue logic, ReplicatorData
├── salvage/                      Phase 4: Salvage Tool and dust refunds
├── nanite/                       Phase 4: Nanite Swarm (build in empty space)
├── tether/                       Phase 4: Tether Anchor leash and recall
├── core/                         Phase 4: Backup Cores, respawn choice, death marker
├── api/                          public API: registry keys, registry element types, extension interfaces, events
├── body/                         Phase 5: BodyData attachment (sections per socket, modules per mount), BodyView,
│   │                             BodyStats, BodyRules, BodyMigration, BuiltinBodies, Bodies (server),
│   │                             ToolModules, Modules, InventoryLock, Cosmetics
│   ├── part/                     SectionItem, ModuleItem
│   └── assembly/                 (5b) 3D assembly view logic
├── starship/                     Test Starship placeholder entity (RD model/animation check), StarshipDrift
├── asteroid/                     Phase 6: AsteroidField/AsteroidBody (pure), AsteroidChunkGenerator, AsteroidBlock, Drilling
├── machine/                      Phase 7: machine blocks, block entities, menus, transport
├── station/                      Phase 8: multiblock validation & controllers
├── helper/                       Phase 8: helper drone entity, steering, task logic
├── earth/                        Phase 9: ruined_earth keys, Atmospheric Portal
├── terraform/                    Phase 10: Terraformer blocks, biome rewrite + chunk resend
├── client/                       client-only (never referenced from common code)
│   ├── input/                    keybinds, thrust input sampling
│   ├── hud/                      dust meter, Dust Sensor overlay
│   ├── screen/                   replicator, assembly, machine screens
│   └── render/                   GeckoLib body/part/drone renderers, player model replacement
├── compat/jei/                   @JeiPlugin + categories (only loaded when JEI present)
├── gametest/                     GameTests (./gradlew runGameTestServer) + TestPlayers helper
└── datagen/                      GatherDataEvent providers (recipes, loot, tags, lang, models)

src/main/resources/
├── data/void_reclaimer/dimension/          deep_space.json, ruined_earth.json
├── data/void_reclaimer/dimension_type/     dimension types
├── data/void_reclaimer/worldgen/           noise settings / biomes / features
└── assets/void_reclaimer/
    ├── geo/  animations/                   GeckoLib models & animations
    └── textures/                           textures
src/main/templates/META-INF/neoforge.mods.toml
src/generated/resources/                    datagen output (committed)
```

## Art & audio
**Art drop 1 (2026-10-29):** the project owner supplies art from a private GitLab repo (`cosmicvoiddweller/void-reclaimer-art`, branch `art/tier-c-pilot`), delivered to us as a zip; only its `src/main/resources/assets/void_reclaimer` tree is copied into this repo (its generators, `.bbmodel` sources and previews stay there). It covers the Drone, Mech and Ship GeckoLib rigs, 30 section models, 4 module models, 3 first-person pieces, per-chassis animations, base+accent entity textures, 42 item icons, 3 block textures and the 4 HUD energy sprites. Its sockets and offsets match our chassis JSON exactly (checked against `socket_contract.json`: zero mismatches). The art's own conventions (bones, casing and accent layers, animation names, axes, coordinate system) are in its README, and Phase 5b implements them. Items, fluids, asteroid blocks and machines added after the drop had our placeholders; **RD art pass 1** (2026-10-29, the owner's own Retro Diffusion work) replaced 48 of the 51, and the first RD module model (`core_mk1`) arrived with it. Promoted files are listed in `docs/art/promoted.txt` (`rd_generate.py --promote` writes it; `asset_list.py` reads it). `art/rd/` is gitignored; the owner moves it between devices on a throwaway branch (`tools/art/README.md`), which is never merged. **Asset list:** `docs/art/ASSETS.md` and `docs/art/assets.csv` list every texture and model (placeholder, art drop, missing, future) with Retro Diffusion prompts; regenerate them with `python3 tools/art/asset_list.py` when assets change. `tools/art/rd_generate.py` generates rows through the Retro Diffusion API (the owner runs it locally with their `RD_API_KEY`; results stage in gitignored `art/rd/`, `--promote` copies picks into the mod). Its offline test (`tools/art/test_rd_generate.py`, fake RD server) runs in CI. **Style guide:** `docs/art/style.json` is the source (mood, rules, colour ramps, per-category palettes and prompt templates); `tools/art/style_guide.py` writes `docs/art/STYLE_GUIDE.md` and `docs/art/palettes/`, and `rd_generate.py` sends each job its category's palette (`input_palette`) and template. New art follows it.

Before the drop:
Programmer art for now: simple placeholder textures and models, and vanilla sounds as stand-ins behind our own sound events. Every placeholder is listed in `PROGRESS.md` so real assets can replace it without code changes.

## Settled design decisions
**Phase 1**
- Deep space is an empty void (flat generator, no layers; since Phase 6 the custom asteroid generator on top of that, see the Asteroids block). Height is the vanilla maximum, Y -2032..2031. A soft push-back starts 96 blocks from either limit (config), so nothing reaches void damage.
- The Overworld, Nether, and End are locked for good. Endgame travel goes to `ruined_earth`. Creative and spectator players may bypass the lock (ops can switch modes). Nether portals can't be lit in deep space.
- Light: real darkness, with sky light from a fixed, dim, distant sun (`fixed_time` 12800 gives effective sky light 10/15). Vanilla sky light only travels downward, so the sun is drawn near the zenith. Undersides and enclosed spaces stay dark.
- Zero-G covers everything: players and mobs via a `generic.gravity` modifier, other entities via NoGravity plus a tag, and falling blocks via a mixin. Fluids still flow. Thrown or hit things keep vanilla's light drag. Gravity generators (later) hook into `ZeroGravity.isZeroG(level, pos)`.
- No fall damage in zero-G. Beds and respawn anchors do nothing in deep space and show a lore message (no explosions).
- Respawn always goes to the deep space spawn until the **Backup Core** (built in Phase 4, Replicator recipe) provides a deep space respawn point.
- **Energy** replaces hunger. Vanilla food is pinned full and hidden, and the energy bar takes its HUD slot.
  - Drain: slow idle drain above a 10% reserve, plus thrust (Phase 2), drilling, and the Replicator.
  - Charge: sunlight, proportional to effective sky light (about 2/3 rate in deep space), plus batteries and chargers later. Dust clouds never give energy.
  - Below the reserve, idle drain stops and an always-on emergency trickle charges, even in darkness.
  - At 0: low power mode (thrusters, tools, and the Replicator offline; no damage) until energy recovers to 5%.
**Phase 2**
- Thrust is relative to the view. W/S thrust along the look vector, A/D strafe along the view's right vector, and Space/Shift thrust along the view's up vector. No roll; a roll/6-DOF camera may come later (e.g. the Ship size class).
- No drag in thruster flight. The **inertial dampener** (toggle, default key Z, on by default) brakes to a stop when no thrust key is held. With it off you drift indefinitely.
- Hard top speed from `FlightStats`. Acceleration is thrust / mass. Phase 2 uses config "starter drone" stats; Phase 5 replaces `FlightStats.of(player)` with stats from the body.
- Energy: thrust 2/s, dampener braking 1/s (config). In low power mode the thrusters and dampener are offline and you drift.
- Creative uses vanilla flight; key V switches creative to thruster flight (same handling, no energy cost, no speed check).
- The client simulates movement (`LivingEntityTravelMixin` → `ThrusterFlight.travel`) and sends held keys (`ThrustInputPayload`) on change. The server charges energy and checks displacement over a 1 s window against top speed × tolerance; violations are pulled back to the start of the window.
**Phase 3**
- Sense of motion (client, cosmetic):
  - Space specks on a fixed world lattice give parallax and streak at speed. They are identical everywhere and never hint at clouds.
  - Optional camera sway, off by default.
  - Thruster and dampener loop sounds.
- Clouds are large and rare: 1024-block cells, 32% chance of a cloud each, radius 150–400.
  - Each cloud has one kind: Metallic, Silicate, Ice, or Carbon.
  - Density follows a smooth falloff times noise.
  - Generation is fixed by `DustField` constants (not config) so client and server always agree.
  - The client gets a one-way-derived seed via the synced `DustFieldSeed` level attachment.
- Drag scales with density: up to 4% velocity loss per tick, and up to 60% lower top speed in the core. It applies with the dampener off and in low power too. Thrust can't exceed the cloud top speed; arriving faster bleeds off smoothly.
- Dust is an internal reserve per kind (`DustData` attachment). Gain is density × (trickle + scoop × blocks moved), so flying through a cloud collects far more than sitting still. Capacity is 200 per kind from config; Phase 5a storage parts raise it. Dust never gives energy.
- **Dust Sensor** is an item that works from anywhere in the inventory (creative tab until the Phase 4 Replicator recipe; body module in Phase 5a). It provides:
  - Inside a cloud: kind and density readout, plus screen-edge CORE/EDGE markers with distances.
  - Pulse (key B, 5 energy, 10 s cooldown): after a distance-based delay, a fuzzy echo (6–30° error) gives a rough heading and a FAINT, ECHO, or STRONG strength. No exact distance (not radar).
  - Hum: within 512 blocks outside a cloud, a tone and signal bar grow the more directly you face the nearest cloud.
- Server-to-client payloads are registered in common code with a handler that forwards to a client hook (`SensorEchoPayload.clientHandler`), so common classes never reference client classes.
- Player data that only the owner needs uses Data Attachments with NeoForge's built-in attachment sync (available in 21.1.265). Custom `CustomPacketPayload`s are for client-to-server input (Phase 2+).

**Phase 4a**
- The Replicator is a timed queue (key R). It holds 5 jobs. Dust is paid when a job is queued and refunded on cancel. It uses 1.5 energy/s while working and pauses in low power. Output goes to the inventory, with a 9-slot overflow buffer. The screen never pauses the game.
- Recipes unlock through `Milestone`s. Each milestone adds an AI log entry (toast + Logs tab). New milestones are enum entries with an `isReached` check plus lang keys for their title, hint, and log.
- The recipe type `void_reclaimer:replicator` is datagen-only (`ModRecipeProvider`, ids `replicator/<name>`) and `isSpecial()`, so it stays out of the vanilla recipe book. JEI shows it.
- The Salvage Tool refunds 50% (config) of the cheapest per-item recipe cost for blocks in `#void_reclaimer:salvageable`, with no drop.
**Phase 4b**
- **Nanite Swarm** builds the offhand block (Scaffold from the inventory if the offhand is empty) in empty space 2–6 blocks ahead (sneak + scroll, `nanite_range` component). It costs 1 energy per block, uses vanilla `BlockItem#place`, and places against a looked-at block if there is one. The outline is a plain white box.
- **Tether Anchor**:
  - Leash: clip on with right-click; you drift up to 16 blocks, then get pulled back gently. Client physics in `ThrusterFlight`.
  - Recall (hold G): within 256 blocks a 1 s hold starts a pull at 2× top speed for 3 energy/s, stopped by releasing G. Farther away, a 3 s hold teleports you for 25 energy with a 60 s cooldown. Same dimension only.
  - HUD: text only, "TETHER x/16m".
- **Backup Core**:
  - Up to 3 per player, each with 0–3 charges. A charge costs Metallic 10 + Silicate 10, or sunlight refills it slowly.
  - The core UI renames it. On death the death screen lists charged cores; picking one spends a charge on respawn, with the deep space spawn as the free default.
  - Items drop and float at the death spot; the HUD death marker points there.
- Logic for nanites and the tether lives in plain classes (`Nanites`, `Tether`) so Phase 5a can run them from body modules.
- **Milestones:** First Structure (10 blocks placed) unlocks the Tether Anchor; Diversified (3 dust kinds) unlocks the Backup Core.
- **Starter nebula:** an opt-in `voidReclaimerStarterNebula` game rule (off by default), locked when the world is created; its center reaches the client through `DustFieldSeed`.
- **Saved data:** attachment codecs always give defaults (`optionalFieldOf`), and use `lenientOptionalFieldOf` for lists and complex values, so old or partly broken saves load.

**Create (decided after 4b)**
- Create is a required dependency. Bodies, cosmetics, machines and stations use Create's look: andesite/brass/copper casings, cogwheels and shafts. Our models may reference Create's textures (`create:block/...`) from the player's installed Create; never copy Create's assets into our jar (its `assets/` are All Rights Reserved; its code is MIT).
- Gear animation in Create style: cogs spin with thrust and speed, with module work (Replicator, drill, nanites), with energy level (low power winds down), and idle slowly.
- Body colors: a casing material (Andesite, Brass, Copper, later unlocks) plus a free accent tint.
- Phases 7–8 build on Create's machines and logistics instead of duplicating them.
- **Create Aeronautics: required (decided 2026-10-29; spike results in `AERONAUTICS_SPIKE.md` on branch `claude/aeronautics-spike`, never merged).**
  - Sable works in `deep_space` with zero physics gravity via `data/<ns>/dimension_physics/*.json` (`base_gravity`, `base_pressure`, `universal_drag`). The first spike's "flung away" ships were falling: the zero-G file wasn't in effect. Height and f32 precision were not factors.
  - Aeronautics propellers and balloons need air, so they give no thrust in vacuum. Our own **Space Thruster** block pushes ships through Sable's force API (Create rotation plus dust fuel).
  - The Ship class stays the player's body; a **Helm** block lets the body pilot a block ship with WASD thrust controls. Block ships serve as mobile stations, cargo haulers, mining rigs, and can cross the Atmospheric Portal (air on Ruined Earth makes propellers work).
  - Aeronautics and Sable are required dependencies (`neoforge.mods.toml`); they are `localRuntime` + `testRuntimeOnly`, so our code doesn't compile against them yet. Add them as `implementation` when the first code needs their API.
  - `data/void_reclaimer/dimension_physics/deep_space.json` gives deep space zero physics gravity, pressure and drag. `AeronauticsGameTests` (own namespace `void_reclaimer_aero`, run `./gradlew runGameTestAeronautics`, own CI job) checks a platform stays put and a propeller gives no thrust in vacuum.
  - Upstream bug: an intermittent start-up deadlock between Aeronautics/Simulated/Veil and Create's render classes in client-dist runs (game, `runData`). Required Aeronautics means `runData` loads it too, so CI wraps `runData` in a 5-minute timeout with up to 3 attempts.
  - `sable64bit` (Chelovechic, AGPL, f64 Rapier) works with Sable 2.0.6 and keeps physics exact far from the origin; recommend it, don't depend on it.
- **Ships and stations without Aeronautics:**
  - Drone, Mech and Ship are all the player's body (GeckoLib, Create style). The Ship is the 3.0-scale class: a mobile base with more storage, module slots and a faster Replicator; it docks at stations and is the class that survives the Atmospheric Portal (Phase 9).
  - Stations are fixed block structures: Create machines and belts inside, Create contraptions (bearings, pistons, gantries) for moving parts such as rotating rings, our controller blocks for multiblock bonuses (Phase 8). Between stations: helper drones and Create trains on tracks laid through space.
  - Keep a block-built Ship and movable stations possible as optional Aeronautics support later: don't design station controllers or a Ship Core in ways that rule it out.

**Phase 5a**
- **Parts:** items with a default `part_stats` component. The body (`BodyData` attachment) holds a frame, a core, and per-kind slot lists sized by the frame. Stats are computed purely in `BodyStats`.
- **Size classes:** Drone 0.4 / Mech 1.2 / Ship 3.0 (`generic.scale`). The frame also sets max health, reach, dust intake, base inventory rows, mass budget and slot counts.
- **Mass is a soft limit** (handling and energy penalty over budget). **Tier gate:** part tier ≤ core tier.
- **Inventory:** hotbar + frame base rows + storage rows (max 36). Locked slots take nothing new but can be emptied; creative is exempt.
- **Modules:** sensor and tool modules work only when installed. Tools are used with an empty main hand; the module wheel (hold Left Alt) picks the active one. Nanite: right-click space, or sneak + right-click a block.
- **Install UI:** the Body tab in the Replicator (5b adds the 3D assembly view).
- **Cosmetics:** casing material + accent tint + thruster trail (drawn now as particles), plus cosmetic part slots (drawn in 5b).
- **Mech frame and Mk II parts** unlock with the Expansion milestone (any dust kind at capacity). The Ship frame waits for Phase 6 materials.

**Body sections (after 5a, replaces frames and part slots)**
- A body is a tree of **sections** in **sockets**. The core section names a `chassis` (datapack) that lists the sockets: accepted socket types, `required`, `parent`, `pivot`, `area`. Only the core is required; a missing section empties its child sockets.
- Sections offer **mounts** for modules (core, thruster, storage, sensor, tool, cosmetic). Size class, scale, health, reach and mass budget come from the chassis and its sections.
- Kits for the starting set of a chassis (Drone Kit; Mech Core Kit = torso + pelvis + head); limbs are crafted singly. Mk II sections replace single sockets.
- Old frame/part saves migrate on load; the body is kept on death. Body logic takes a `LivingEntity` where it can (entity-agnostic for helper drones later).
- **Paint:** body default casing + accent, with optional per-section overrides.
- **Wear:** light and repairable. Below 50% condition, efficiency drops up to 25%. Repair costs about 20% of the recipe dust. Never breaks; no wear in creative.
- **Body Blueprint:** save the body, apply it from the inventory, queue Replicator jobs for missing parts.
- **Replicator recipes** may have several `results` and a `title`; a job waits until the buffer fits all of its items. Salvage prices single-item recipes first.

**Cloud scanner (after 4b)**
- With a Dust Sensor, key N toggles (client config `cloudScanner`, default on) a wire mesh with a faint tint, in the kind's color, for every cloud whose edge is within 512 blocks.
- The mesh follows the true lumpy edge (`CloudShape`): rays on a 16×32 latitude/longitude grid find the outermost point at density 0.05. Computed once per cloud on a background thread and cached.
- Drawn unfogged (fog start restored afterwards) through `RenderLevelStageEvent`; this bends the Phase 3 "clouds are invisible" rule only for sensor owners who switch it on.

**Phase order (decided after 4b, revised 2026-10-29):** 5a Body data → extensibility + body sections → **Foundations (Aeronautics + Dyson Sphere counters and sky)** → 6 Asteroids → 7 Machines → 7b Aeronautics ships (Space Thruster, Helm; Aeronautics is required now) → 5b Body rendering → 8 → 9 → 10. Gameplay comes before the GeckoLib rendering phase.

**Test Starship (placeholder entity, 2026-10-29)**
- `void_reclaimer:test_starship` is for checking RD models and animations in game; it is not gameplay. Its model is the owner's RD starship, promoted with `--promote-as entity/starship --scale-like body/ship`. It is drawn at the Ship size class's scale.
- It drifts and turns round its anchor (`StarshipDrift`, from game time, the same on both sides), loops `gear_cycle`, and plays `ignite_prep` on right-click.
- To replace it, promote a new RD `.bbmodel` to the same target. Spawn it from the creative item, with `/void_reclaimer starship spawn`, or with `/summon`.

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

### Extension points
| Extension point | Kind | Notes |
|---|---|---|
| `void_reclaimer:dust_kind` | Synced datapack registry (`api.dust.DustKind`) | `color`, `cloud_weight` (0 = never in clouds), `name`, `order` (built-ins 0–3) |
| `void_reclaimer:milestone` | Synced datapack registry (`api.milestone.Milestone`) | `criterion`, `order`, optional `title`/`hint`/`log_title`/`log_text` |
| `void_reclaimer:milestone_criterion_type` | Code registry of `MapCodec<? extends MilestoneCriterion>` | Built-in: `always`, `dust_collected`, `dust_kinds_held`, `blocks_placed`, `dust_full`, `dyson_tier`, `items_made` (a list of item ids the player must have held) |
| `void_reclaimer:replicator_category` | Synced datapack registry (`api.replicator.ReplicatorCategory`) | Replicator tabs; `order`, `title` |
| `void_reclaimer:casing` | Synced datapack registry (`api.body.Casing`) | `order`, `name`, `texture`, `requires` (milestone) |
| `void_reclaimer:trail` | Synced datapack registry (`api.body.Trail`) | `color` (none = accent), `particles`, `order`, `name`, `requires` |
| `void_reclaimer:replicator` recipes | Recipe type | `result` or `results` (kits), optional `title`; `cost` is a map of dust kind id → amount; `category` and `milestone` are registry ids |
| `void_reclaimer:size_class` | Synced datapack registry (`api.body.SizeClass`) | `scale` (`generic.scale`), `rank` (for module `min_size`), `name` |
| `void_reclaimer:chassis` | Synced datapack registry (`api.body.Chassis`) | Body layout: `size_class`, `core_socket`, `sockets` (`id`, `accepts` socket types, `required`, `parent`, `pivot`, `area`); socket id = GeckoLib bone (5b) |
| `void_reclaimer:section` | Data component (`api.body.SectionStats`) | On section items: `socket_type`, tier, stats, `mounts`; core sections name a `chassis` |
| `void_reclaimer:module` | Data component (`api.body.ModuleStats`) | On module items: `kind` (fixed `ModuleKind`), tier, mass, `min_size`, core/thruster/storage specs |
| `void_reclaimer:tool_module_type` | Code registry (`api.body.ToolModuleType`) | What a tool module does (`use`, `breakSpeed`); items name one with the `void_reclaimer:tool_module` component |
| `void_reclaimer:tool_module_type` built-ins `drill`, `harvester` | Code registry entries | Mechanical Drill and Matter Harvester: `breakSpeed` on `#void_reclaimer:asteroid_blocks`; the break rules are in `asteroid.Drilling` |
| `void_reclaimer:freezing` recipes | Recipe type (`machine.FreezingRecipe`) | `ingredient`, `results`; run by the Encased Fan type below |
| `void_reclaimer:freezing` fan type | Create registry entry (`create:fan_processing_type`) | `machine.FreezingFanType`, priority 450, active in powder snow; registered through `MachineSetup.FAN_TYPES` |
| Spout behaviors | Create `BlockSpoutingBehaviour.BY_BLOCK` | `machine.SpoutCasting` on the Depot; add more casting by registering for another block |
| `void_reclaimer:config` recipe condition | NeoForge condition (`machine.ConfigCondition`) | `key` names a toggle in `Config.TOGGLES`; gates any recipe on a config switch |
| `void_reclaimer:asteroids` | Chunk generator type (`asteroid.AsteroidChunkGenerator`) | A flat-generator subclass; a dimension can use it with the flat `settings` object |
| `#void_reclaimer:asteroid_blocks` | Block tag | Blocks only the drill and harvester can break |
| `#void_reclaimer:modules/dust_sensor` | Item tag | Items that work as a Dust Sensor in a sensor slot |
| `#void_reclaimer:salvageable` | Block tag | Blocks the Salvage Tool refunds |
| `DustGainEvent` | Event, modifiable + cancellable | Before cloud dust is added |
| `MilestoneReachedEvent` | Event | After a milestone is reached in play |
| `ReplicatorJobFinishedEvent` | Event, mutable outputs | Before a finished job's items are delivered |
| `BodyStatsEvent` | Event, replace stats | When body stats are computed (cached until the body changes), both sides |
| `BodyChangedEvent` | Event | After a body changed (sections, modules, looks), server side |
| `ModuleUseEvent` | Event, cancellable | Before the active tool module is used |
| `MassDriverLaunchEvent` | Event, cancellable, mutable counts | When a Mass Driver fires a payload, before the Dyson counters change; counts start from the payload's tag |
| `#void_reclaimer:dyson/satellite_payloads` | Item tag | Items the Mass Driver launches as one Dyson Swarm Satellite each |
| `#void_reclaimer:dyson/truss_payloads` | Item tag | Items the Mass Driver launches as one Dyson Shell Truss Module each |

### Fixed sets (enums by design)
- `ModuleKind` (`api.body`): the mount kinds sections offer (core, thruster, storage, sensor, tool, cosmetic). Each has its own rule in `BodyStats`; new behaviour comes from new modules of these kinds (tools: `ToolModuleType`). Sections themselves are open (socket types are free ids).

## Conventions
- Use `VoidReclaimer.id(path)` for every `ResourceLocation` in our namespace.
- Client-only classes stay under `client/` and are reached only from `VoidReclaimerClient` or `@EventBusSubscriber(value = Dist.CLIENT)`.
- Prefer datagen over hand-written JSON for recipes, loot tables, tags, lang, block states, and item models. Dimension/worldgen JSON can be hand-written datapack files.

**Design doc "Nebulae to Planetary Genesis" (decided 2026-10-29)**
- The doc extends this plan; it is an idea framework, not a bible. The repo's systems stay (Replicator from the start, body, energy, dust). Where they disagree, ask before changing anything.
- Nebulae are the existing dust clouds: Ice, Carbon, Silicon (`silicate` in code), Metallic. Raw items (Ice Shards, Carbon Flakes, Silicon Grit, Metallic Grains) come from (a) the Replicator turning stored dust into raw items and (b) a collector part that pulls items out of a cloud. Neither is built yet.
- The 33 existing Replicator recipes keep their dust prices until told otherwise.
- Body size classes stay Drone, Mech, Ship; the doc's Starship is the Ship class. Display strings still say "Ship".
- The Overworld lock and the planned `ruined_earth` dimension are unchanged for now (Act II's return home is undecided).
- Lore and recipes come only from the user's doc. Placeholder texts are marked `[Placeholder, lore text pending.]` in lang.

**Dyson Sphere (foundations, 2026-10-29)**
- `dyson.DysonSphereData` (saved, synced attachment on the deep space level): satellites and truss modules launched, plus the server's `latticeStart` and `shellTotal` thresholds so clients can draw the shell. `DysonSphereData.of(level)` works from any dimension on the server.
- `dyson.DysonTiers` (config-free): tier 1 = 1-50 satellites, tier 2 = 51+, tier 3 = `latticeStart` trusses, tier 4 = `shellTotal` trusses. Config `dyson.latticeStart` (default 50) and `dyson.shellTotal` (default 2000) are the owner's numbers (2026-10-29).
- `dyson.Dyson.launchSatellites/launchTrusses/set` is the one entry point for launchers (the Mass Driver later). A tier change re-checks milestones for every player.
- Milestones `dyson_tier_1..4` (criterion `dyson_tier`, order 100-103) write the AI log entries. Their texts are placeholders.
- `/void_reclaimer dyson get|set satellites|trusses <n>` for testing.
- Sky: `client/render/DysonSky` draws the swarm and shell in the sun's frame, and tints/dims the sun. Power beams to the Quantum Receiver Dish wait for that block. The sun's dimming is visual only: solar charging does not use `DysonTiers.sunlight` yet, because a dark star would also cut off the AI's power before the Dyson power beam exists.

**Asteroids (Phase 6, 2026-10-29)**
- **Generation:** `deep_space.json` uses the custom generator `void_reclaimer:asteroids` (`AsteroidChunkGenerator`, a `FlatLevelSource` subclass) instead of `minecraft:flat`. It is not a structure: structures only reach about 128 blocks from their start chunk, and giants are up to about 300 blocks wide. It captures the world seed in `createState`, then each chunk asks `AsteroidField` about its own cell.
- **`AsteroidField` (pure, unit-tested):** space is cut into 1024-block cells (like the dust field). About 35% of cells hold a cluster: a small-rock field (most), a large body (18-40 half-width) with satellites, or a rare giant (4% of clusters, 100-150 half-width, so up to about 300 wide) with satellites. No body ever leaves its cell. The spawn keeps 300 blocks clear from any body's edge.
- **Kinds:** a cluster is made of the dust kind of the nearest cloud within 1500 blocks (85% of the time), otherwise a weighted pick. Blocks: `asteroid_silicate`, `asteroid_ice`, `asteroid_carbon`, `asteroid_metallic` (tag `#void_reclaimer:asteroid_blocks`).
- **Mining:** asteroid rock cannot be broken by hand or by other tools (break speed 0, and the break is cancelled server-side). The **Mechanical Drill** (tool module type `drill`, Replicator, Tools tab) breaks it fast, drops nothing and refunds `asteroids.drillDustYield` dust of the block's kind. The **Matter Harvester** (`harvester`) is slower and lets the block drop as an item. Both cost `asteroids.drillEnergy` per block and go offline in low power mode. Creative breaks anything. Drilling only reads the active tool, so the sneak-flag fix for flight is not involved.
- **Existing worlds:** the generator is saved with the world, so a world made before this phase keeps its flat deep space (and chunks already generated stay empty). New worlds get asteroids.
- **Measured:** about 65 ms per chunk column through the middle of a 195-wide giant (synchronous, in a GameTest). Giants are rare, so this is accepted; the interior could be filled faster if it becomes a problem.
- Not in this phase: the Ship frame stays locked (the Starship upgrade order is open in the design doc); no ores or veins inside asteroids; no rotation of bodies.

**Machines, part 1: nebula materials and Create chains (Phase 7, 2026-10-29)**
- Phase 7 builds on Create's own machines (belts, funnels, arms already do the item transport). Part 1 adds the materials, six fluids and the recipes of the design doc's Act I tables that need no new machine code. Part 2 adds the custom machines the doc says need code (Electrolyzer, Spinner, Encased Fan freezing, Spout casting onto a Depot).
- **Items** (`ModMaterials`, names as in the doc): the four raw items, Crushed Cryo-Ice, Coolant Frame Plate, Fine Carbon Powder, Carbon Paste, Carbon Composite Plate, Silicon-Carbide Slurry, Thermal Shielding Tile, High-Purity Graphite, Carbon Thread, Pure Silicon Dust, Quartz Wafer, Doped Silicon Ingot, High-Efficiency Solar Cell, Advanced Sensor Unit, Refined Metal Nodule, Micro-Alloy Sheet, High-Output Solar Sail (+ its transitional item). **Fluids** (`ModFluids`): Purified Water, Oxygen Gas, Hydrogen Gas, Nebulite Propellant, Coolant Fluid, Liquid Silicon; each with a bucket, no placeable block, tinted water textures.
- **Recipes** are written by `ModCreateRecipeProvider` as plain JSON (20 recipes under `data/void_reclaimer/recipe/create/`): crushing, milling, mixing (basin, heated, superheated), pressing, splashing (fan washing), vanilla blasting (fan blasting), deploying, and the solar sail's Sequenced Assembly. The Replicator turns 8 dust of a kind into 4 raw items of that kind (tab "Raw").
- **Toggles** (config `materials`): `replicatorRawItems`, `iceChain`, `carbonChain`, `siliconChain`, `metallicChain`, `solarSail`. Recipes carry the condition `void_reclaimer:config` (`machine.ConfigCondition`, key = toggle name), checked when recipes load, so a change needs a restart or `/reload`. New features should get a toggle the same way (`Config.toggle`).
- **Assumptions the doc left open (all placeholders; change them in the provider):** fluid amounts (250 mB steps, 2:1 hydrogen to oxygen) and times; "Packed Ice Block" is vanilla Packed Ice; "Electron Tube" and "Precision Mechanism" are Create's items (`create:electron_tube`, `create:precision_mechanism`); "high pressure" pressing is a normal press (Create has one pressure); Silicon-Carbide Slurry is an item (it feeds a press); fan blasting uses vanilla `minecraft:blasting`, so a blast furnace also makes nodules.
- **Blocked on open questions in the doc** (not implemented): the Data Monolith (Act III; its frame step is now known, the rest of its design is not built), and "standard Create crafting" of shafts and gears from Micro-Alloy Sheet (which recipes?). Collector part (b) for raw items from a cloud is also not built.

**Machines, part 2: the custom Create machines (Phase 7, 2026-10-29)**
- Built on Create 6's addon API (checked against the jar, not memory): `RotatedPillarKineticBlock` + `IBE` for blocks, `KineticBlockEntity` for block entities, `BlockStressValues.IMPACTS` for stress, `FanProcessingType` in `CreateBuiltInRegistries.FAN_PROCESSING_TYPE`, `BlockSpoutingBehaviour.BY_BLOCK`. Registration is plain NeoForge `DeferredRegister`s (no Registrate); `machine/MachineSetup` does the hooks that need Create's registries.
- **Compiling against Create:** its Flywheel, Ponder and Registrate are nested jars that javac can't see, so `build.gradle` has `extractCreateLibs`, which unpacks them from the Create jar into `build/create-libs` (compile-only, the exact versions Create runs with).
- **Electrolyzer** (`electrolyzer`, `MachineBlock` + `ElectrolyzerBlockEntity`): a shaft through two opposite faces. Water in through pipes; 250 mB of water per cycle makes 125 mB Oxygen Gas and 250 mB Hydrogen Gas (the 2:1 that the propellant recipe uses). Works from 16 RPM; a cycle is 60 ticks at 64 RPM and faster with speed. Needs 4 SU per RPM. Pipes only fill the water tank and only drain the gases.
- **Spinner** (`spinner`): 1 High-Purity Graphite per cycle makes 2 Carbon Thread. Same speed rules, 2 SU per RPM. Automation inserts graphite and extracts thread only.
- **Fan freezing** (`FreezingFanType`, `FreezingRecipe`, recipe type `void_reclaimer:freezing`): an Encased Fan blowing through powder snow freezes items. The one recipe is Water Bucket to Packed Ice plus an empty Bucket. The design doc only says "Water", so the bucket is an assumption. Priority 450, above washing.
- **Spout casting** (`SpoutCasting`): a Spout over an empty Depot casts 250 mB Liquid Silicon into a Quartz Wafer. It does nothing when the Depot holds an item, so normal spout filling and Sequenced Assembly are unaffected.
- **Replicator:** both machines are Replicator recipes (Blocks tab, Diversified milestone); the doc gives no recipe for them, so the prices are placeholders.
- **Toggles** (config `machines`): `electrolyzer`, `spinner`, `fanFreezing`, `spoutCasting`, plus `electrolyzerStress`, `spinnerStress`, `minRpm`, `cycleTicks`, `electrolyzerTank`, `electrolyzerWater`, `electrolyzerOxygen`, `electrolyzerHydrogen`, `spinnerThreads`, `castingAmount`. Machine behavior reads its toggle live; recipes read theirs when they load.
- **Not built:** goggle tooltips, rotating animations (the blocks are static models), a JEI category for freezing, Ponder scenes.

**Decisions after Phase 7 (interview, 2026-10-29)**
- **Aeronautics:** keep block-built ships as extras (Phase 7b: Space Thruster, Helm; mobile stations, cargo haulers). The Starship stays the player's body (Ship size class). The doc never mentions Aeronautics.
- **Act II setting:** the planet is `ruined_earth`; the Overworld, Nether and End stay locked. The Dyson Sphere work stays in `deep_space`.
- **Sectors:** each of the 16 cells of the 4x4 grid is one Spire's area: 256x256 centered on its 128-block pulse, so 16 Spires cover the planet.
- **Silicon-Carbide Coating:** Silicon-Carbide Slurry + Carbon Composite Plate in a Basin, then a Press (Create basin compacting) makes 2 coatings (`carbonChain` toggle).
- **Answered later the same day:** the Zero-G Precision Frame steps (Graphite, sheet, press; 2 loops), the truss counts (tier 3 at 50, tier 4 at 2000), the Catalyst Core order (sensor base, wafer, propellant, press), and the Starship: one milestone from the nebula chains (the Ship frame unlocks when the AI has made one item from each of the four chains: Coolant Frame Plate, Carbon Composite Plate, High-Efficiency Solar Cell, Micro-Alloy Sheet). Not yet built: the Starship milestone (Phase 10).

**Revised phase plan (2026-10-29, after Phase 7; replaces the order above)**
Done: 1-7 (spawn, flight, dust, replicator, body data and sections, Foundations, asteroids, machines).
Act II, the Dyson Sphere (deep_space):
- **8. Zero-G Frame and payloads (done):** Zero-G Precision Frame, Dyson Swarm Satellite Payload, Dyson Shell Truss Module, RCS Thruster, Reaction Wheel Gyroscope, Atmospheric Catalyst Core, as Create Sequenced Assembly recipes (see the Phase 8 block).
- **9. Kinetic Mass Driver (done, see the Phase 9 block):** the 1x1x3 multiblock, its launch sequence and animations, the handoff to `Dyson.launchSatellites/launchTrusses`, the Sub-Light Quantum Receiver and the sky's power beams. Sky tier thresholds from the truss counts.
- **10. The Starship (done, see the Phase 10 block):** the milestone that unlocks the Ship frame (one item from each nebula chain) and the Ship class's recipes.
Act III, Planetary Genesis (ruined_earth):
- **11. Ruined Earth:** the dimension and its three-era sky, the Atmospheric Portal, the Planetary Grid HUD, the drop pod and Industrial Mech, Ashen Slag, smog, Acid Drizzle weather.
- **12. Terraforming Spire:** the multiblock, Purified Water and Bio-Nutrient Slurry inputs, the Atmospheric Catalyst Core (one per sector), the Genesis Pulse that rewrites biomes and blocks in 128 blocks.
- **13. Data Monolith and villagers:** the Monolith recipe, villagers rebuilding on their own, tributes in its output drawer.
- **14. Restoration:** per-sector tracking, the Planetary Restoration HUD, the 100% victory state and the climate shield.
Parallel tracks, scheduled as they fit:
- **5b. Body rendering and assembly UI** (GeckoLib, first person). Large and independent; a good break between the Acts.
- **7b. Aeronautics ships** (Space Thruster, Helm).
- **Stations and helper drones** (the old Phase 8): the sphere's logistics may need them; revisit after Phase 9.
- **Polish:** goggle tooltips and rotation animation for the machines, a JEI category for freezing, Ponder scenes, the raw-item collector part (cloud to item), ore veins in asteroids, the "Starship" display name.

**Phase 8: Zero-G Frame, payloads and machinery recipes (2026-10-29)**
- Six items (`ModMaterials`, names as in the doc) with a transitional `incomplete_<id>` twin each, all made by Create Sequenced Assembly recipes that `ModCreateRecipeProvider.assembly(...)` writes. Spouts are 250 mB (placeholder).
  - **Zero-G Precision Frame:** Micro-Alloy Sheet base; Deploy High-Purity Graphite, Deploy Micro-Alloy Sheet, Press; 2 loops. Toggle `frame`.
  - **Dyson Swarm Satellite Payload:** Frame base; Deploy Carbon Composite Plate, Deploy High-Efficiency Solar Cell, Spout Liquid Silicon, Press; 2 loops. Toggle `dysonPayloads`.
  - **Dyson Shell Truss Module:** Frame base; Deploy Thermal Shielding Tile, Deploy Micro-Alloy Sheet, Spout Coolant Fluid, Press (Create has one press pressure); 3 loops. Toggle `dysonPayloads`.
  - **RCS Thruster:** Frame base; Deploy Carbon Composite Plate, Spout Nebulite Propellant, Deploy Create's Precision Mechanism, Press. Toggle `rcsThruster`.
  - **Reaction Wheel Gyroscope:** Frame base; Spout Liquid Silicon, Deploy High-Purity Graphite, Deploy Precision Mechanism, Press. Toggle `reactionWheel`.
  - **Atmospheric Catalyst Core:** Advanced Sensor Unit base; Deploy Quartz Wafer, Spout Nebulite Propellant, Press. Toggle `catalystCore`.
- The RCS Thruster and the Gyroscope are plain items for now; placing them and pushing ships with them belongs to Phase 7b. The Catalyst Core is an Act III item; it is here because its recipe was decided.
- The doc's intermediate names (Reinforced Satellite Chassis, ...) are not used: Create shows one transitional item per recipe.
- Dyson sky thresholds now default to 50 and 2000 (`DysonTiers`, config `dyson`).
- Not here: the Mass Driver, the Quantum Receiver, the Starship milestone.


**Phase 9: Kinetic Mass Driver, Quantum Receiver, power beams (2026-10-29, defaults; all numbers are placeholders with config)**
- **Mass Driver** (`machine/MassDriver*`): a vertical stack Kinetic Drive Base (`kinetic_drive_base`, shaft through two opposite faces, the controller), Payload Chamber (`payload_chamber`) and Magnetic Barrel Nozzle (`magnetic_barrel_nozzle`). The base checks the stack above it each tick. Stress 32 SU per RPM (`massDriverStress`).
  - The chamber has 2 slots of up to 16, takes payload items only (tags `dyson/satellite_payloads`, `dyson/truss_payloads`) from belts, funnels, arms, hoppers or a right-click, and gives nothing back to automation; it drops its items when broken.
  - Wind-up runs only while a payload is loaded. `MassDriverMath.launchTicks`: nothing below 64 RPM, 400 ticks (20 s) at 64 RPM falling linearly to 60 ticks (3 s) at 256 RPM (`massDriverMinRpm`, `massDriverFastRpm`, `massDriverSlowTicks`, `massDriverFastTicks`).
  - Firing takes one payload, posts `MassDriverLaunchEvent`, then calls `Dyson.launchSatellites/launchTrusses` (a cancelled launch still uses up the payload). Works in any dimension. Sound `mass_driver_launch` (vanilla stand-in), cloud particles at the nozzle.
  - **Assumption, not built:** no projectile entity, steam ring, wind-up animation or flywheel; the doc's launch animation is replaced by the sound, the particles and the sky streak. The blocks are static cubes.
- **Sky streak:** `LaunchStreakPayload` (server to client) makes the deep space sky draw a blue streak from a direction hashed from the launch position towards the star (`DysonSky.drawStreaks`, 120 ticks; wider for trusses).
- **Sub-Light Quantum Receiver** (`quantum_receiver`, `QuantumReceiverBlockEntity extends GeneratingKineticBlockEntity`): turns at `receiverRpm` (128) once any truss is launched, with output proportional to trusses launched, `receiverMaxStress` (262,144 SU) at `shellTotal`. The server refreshes it once a second. It reads the global `DysonSphereData`, so a Receiver on a ship or on Ruined Earth draws on the same sphere. Made only by Sequenced Assembly (toggle `quantumReceiver`): Micro-Alloy Sheet base, Deploy High-Efficiency Solar Cell, Deploy Carbon Composite Plate, Deploy Create's Precision Mechanism, Spout 250 mB Coolant Fluid; one loop, no press (the doc ends at the coolant).
- **Power beams (sky Layer C):** additive beams from the star to the direction of every loaded, turning Receiver (`QuantumReceiverBlockEntity.CLIENT_RECEIVERS`), brighter with the trusses launched, pulses travelling along them. They are drawn in deep space only (the only dimension with the Dyson sky).
- **Replicator:** the three Mass Driver blocks (Blocks tab, Diversified milestone, toggle `massDriver`) at placeholder prices; the doc names none.
- **Toggles** (config `dysonMachines`): `massDriver`, `quantumReceiver`; the numbers above are in the same section.
- **Not here:** goggle tooltips, rotation animation, Ponder; Receiver tooltip shows Create's per-RPM capacity rather than the live output; the Receiver does not require sky exposure.

**Phase 10: the Starship milestone and the Ship recipes (2026-10-29, defaults)**
- **Milestone `void_reclaimer:starship`** (order 50, criterion `items_made`): reached once the AI has held one of each of Coolant Frame Plate, Carbon Composite Plate, High-Efficiency Solar Cell and Micro-Alloy Sheet (one per nebula chain). "Made" means held at some point: `ReplicatorData.itemsMade` records the ids (only those an unreached milestone asks for) from the inventory, armor, offhand and Replicator buffer, checked once a second with the other milestones. A plate that later goes into a machine still counts; a plate taken out of a Create output by hand counts when it reaches the inventory.
- **`items_made` is a general criterion** (`MilestoneCriteria.ItemsMade`, field `items`, non-empty list of ids): datapacks can ask for any items. `MilestoneContext` carries `itemsMade`. Saves without the new field load with none.
- **Ship recipes:** all 14 Ship sections and a **Ship Core Kit** (keel, bridge, bow) are Replicator recipes in the Parts tab gated by `starship`. The prices are placeholders (a keel is 120/90/60/80 dust, a whole Ship several times a Mech); the doc gives none. The sections were already items (tier 3); only the recipes and the unlock were missing.
- **Not here:** the "Starship" display name (still "Ship", per the earlier decision), Ship-only materials in recipes (Replicator recipes cost dust only), the Ship's GeckoLib rendering (5b).

**Phase 5b-1: the body on the model, third person (2026-10-29, defaults)**
- **What it does:** `RenderPlayerEvent.Pre` is cancelled and the player is drawn by `client/render/body/BodyRenderer` (a GeckoLib `GeoReplacedEntityRenderer<Player>` with one shared `BodyAnimatable`). The body is drawn at `generic.scale`, facing where the player looks (flyers also pitch with the view; the upright Mech only turns). The `body` attachment now syncs to everyone who can see the player, not only the owner. Not here: first person (the vanilla hand still shows) and the 3D assembly view, both the next 5b PR.
- **Composed rigs (Drone, Ship; the art's own contract):**
  - `geo/body/<rig>.geo.json` is the core section plus empty marker bones named after the chassis sockets (socket id = bone name). Each installed section's model (`geo/section/<item>.geo.json`) is drawn at its socket bone's pivot; each module (`geo/module/<item>.geo.json`) at the rig bone `mount_<kind>_<n>` or `<socket>__mount_<kind>_<n>`, or else at the same `mount_*` bone inside its section's model. Items without a model are not drawn.
  - `cap_*` bones plug empty sockets and are hidden once the socket has a section.
  - Paint: the top-left 16x16 of every art texture is the casing swatch; `BodyTextures` paints the chosen casing's Create texture into it (a dynamic texture per art texture and casing, cleared on resource reload). The `_accent` mask is drawn again, tinted with the accent colour at full brightness (so lights and trim glow). Per-section paint overrides apply to that section and its modules.
  - Sections are drawn upright at their socket pivot; a section for a mirrored socket is not mirrored yet (art sections are symmetrical or single).
- **Whole-model rigs (the Mech):** `assets/void_reclaimer/body_rigs/<rig>.json` (`BodyRigSpec`) says a rig is one RD model with its own bones and animations: `model`, `texture`, `animations`, `animation_prefix`, `grounded` (origin at the feet), `sockets` (socket id to bones hidden, with their children, while that socket has no section) and `poses` (pose key to animation name). A rig with a spec is not composed: no section models, modules, casing or accent. Pose keys: `idle`, `thrust`, `brake`, `back`, `strafe_left`, `strafe_right`, `low_power`, `module_work`; a missing key falls back to `thrust` (strafe and back) and then `idle`.
  - The Mech is the owner's RD "blocky mech suit", promoted with `--promote-as entity/mech_suit --scale 0.39` (28.8 px, a 1.8-block body at scale 1). Its animations: `fly` = idle, `zoom_flight` = thrust, `fly_back` = back, `fly_left`/`fly_right` = strafes, `swing_tool` = module work. Its `menace_walk` and `menace_run` are not used (no ground in deep space), and it has no low-power animation (it falls back to idle).
  - To replace it: `python3 tools/art/rd_generate.py --promote-as entity/mech_suit --source NEW.bbmodel --scale 0.39`, and keep the group names in the spec (or edit `body_rigs/mech.json`). `BodyRigSpecTest` checks that every socket, bone and animation the spec names exists.
- **Animation:** `BodyLook.pose` picks the pose from the player's motion each tick (speed change for thrust and brake, the part of the step along and across the facing for back and strafe; low power from the energy state; a pose is held 4 ticks). While the player swings, `module_work` plays. Remote players are animated from their movement alone.
- **Camera:** the third-person camera distance grows with body size (`BodyRenderEvents.cameraDistance`), so the Ship isn't viewed from inside.
- **Debug commands:** `/void_reclaimer body preset <player> drone|mech|ship [full]` (a complete body: every socket filled, Mk I core, thrusters and storage; the Mech gets a rotor arm jetpack on its back; `full` adds a Mk II core, Mk I and Mk II storage pods in turn, Dust Sensors and the four tool modules, so every module model shows) and `/void_reclaimer body remove <player> <socket>`.
- **Known limits:** the art's textures are very dark (black frames with small casing and accent areas), so bodies need light to read; the Drone and Ship look sparse for the same reason. Modules without models (sensors, storage, core, tools) are invisible. The nameplate and held items are not drawn on the body.
