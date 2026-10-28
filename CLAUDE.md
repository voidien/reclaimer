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
5. **Modular Body:** part/frame data model, size classes, `generic.scale`, GeckoLib rendering replacing the vanilla player model (incl. first person), assembly UI.
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

All versions live in `gradle.properties`. `neoforge.mods.toml` is generated from `src/main/templates/` via the `generateModMetadata` task.

Commands: `./gradlew build`, `./gradlew runClient`, `./gradlew runData` (writes to `src/generated/resources`).

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
├── space/                        Phase 1: dimension keys, spawn/respawn redirection, zero-G rules
├── flight/                       Phase 2: thrust model, server validation (shared + server logic)
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

## Conventions
- Use `VoidReclaimer.id(path)` for every `ResourceLocation` in our namespace.
- Client-only classes stay under `client/` and are reached only from `VoidReclaimerClient` or `@EventBusSubscriber(value = Dist.CLIENT)`.
- Prefer datagen over hand-written JSON for recipes, loot tables, tags, lang, block states, and item models. Dimension/worldgen JSON can be hand-written datapack files.
