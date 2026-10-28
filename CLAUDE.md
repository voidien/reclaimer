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
- Dependencies: **GeckoLib** for body, part, and helper-drone models/animations. **JEI** for recipe categories for the replicator and machines. Look up current compatible versions; don't guess.
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
├── replicator/                   Phase 4: recipe type, menu, virtual inventory
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
- Player data that only the owner needs uses Data Attachments with NeoForge's built-in attachment sync (available in 21.1.265). Custom `CustomPacketPayload`s are for client-to-server input (Phase 2+).

## Conventions
- Use `VoidReclaimer.id(path)` for every `ResourceLocation` in our namespace.
- Client-only classes stay under `client/` and are reached only from `VoidReclaimerClient` or `@EventBusSubscriber(value = Dist.CLIENT)`.
- Prefer datagen over hand-written JSON for recipes, loot tables, tags, lang, block states, and item models. Dimension/worldgen JSON can be hand-written datapack files.
