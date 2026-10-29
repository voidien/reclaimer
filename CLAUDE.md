# Void Reclaimer — Project Plan & Rules

You are an expert Java developer and Minecraft modder specializing in NeoForge for Minecraft 1.21.1. "Void Reclaimer" is a singleplayer zero-gravity progression and factory-automation mod.

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
- Dependencies: **Create** (required) for kinetics, machines and the art style; **GeckoLib** for body, part, and helper-drone models/animations. **JEI** for recipe categories for the replicator and machines. Look up current compatible versions; don't guess.
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
├── body/                         Phase 5: SizeClass, Frame/Part definitions, BodyData attachment,
│   ├── part/                     part types & stats
│   └── assembly/                 assembly menu/logic
├── asteroid/                     Phase 6: worldgen features, drill gating
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
Programmer art for now: simple placeholder textures and models, and vanilla sounds as stand-ins behind our own sound events. Every placeholder is listed in `PROGRESS.md` so real assets can replace it without code changes.

## Settled design decisions
**Phase 1**
- Deep space is an empty void (flat generator, no layers). Height is the vanilla maximum, Y -2032..2031. A soft push-back starts 96 blocks from either limit (config), so nothing reaches void damage.
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
- **Create Aeronautics / Sable: not a dependency (spike, 2026-10-28).** Sable gravity can be zeroed per dimension (`data/<ns>/dimension_physics/*.json`: `base_gravity`, `base_pressure`, `universal_drag`), but in `deep_space` every sub-level was flung away or removed on its first physics tick (cause unknown; our tall dimension or zero-G systems are suspects). Revisit before Phase 8; branch `claude/aeronautics-spike` keeps the test setup.
- **Ships and stations without Aeronautics:**
  - Drone, Mech and Ship are all the player's body (GeckoLib, Create style). The Ship is the 3.0-scale class: a mobile base with more storage, module slots and a faster Replicator; it docks at stations and is the class that survives the Atmospheric Portal (Phase 9).
  - Stations are fixed block structures: Create machines and belts inside, Create contraptions (bearings, pistons, gantries) for moving parts such as rotating rings, our controller blocks for multiblock bonuses (Phase 8). Between stations: helper drones and Create trains on tracks laid through space.
  - Keep a block-built Ship and movable stations possible as optional Aeronautics support later: don't design station controllers or a Ship Core in ways that rule it out.

**Cloud scanner (after 4b)**
- With a Dust Sensor, key N toggles (client config `cloudScanner`, default on) a wire mesh with a faint tint, in the kind's color, for every cloud whose edge is within 512 blocks.
- The mesh follows the true lumpy edge (`CloudShape`): rays on a 16×32 latitude/longitude grid find the outermost point at density 0.05. Computed once per cloud on a background thread and cached.
- Drawn unfogged (fog start restored afterwards) through `RenderLevelStageEvent`; this bends the Phase 3 "clouds are invisible" rule only for sensor owners who switch it on.

**Phase order (decided after 4b):** 5a Body data → 6 Asteroids → 7 Machines → 5b Body rendering → 8 → 9 → 10. Gameplay comes before the GeckoLib rendering phase.

## Conventions
- Use `VoidReclaimer.id(path)` for every `ResourceLocation` in our namespace.
- Client-only classes stay under `client/` and are reached only from `VoidReclaimerClient` or `@EventBusSubscriber(value = Dist.CLIENT)`.
- Prefer datagen over hand-written JSON for recipes, loot tables, tags, lang, block states, and item models. Dimension/worldgen JSON can be hand-written datapack files.
