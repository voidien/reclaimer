# Package and folder structure

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
├── link/                         remote control: BodyHost (a body nobody is in), HostRole
├── ship/                         7b: block ship thrusters (Sable actors), ThrusterMath, the Helm (seat, PilotInput, ThrustAllocator, Ships)
├── compat/jei/                   @JeiPlugin + categories (only loaded when JEI present)
├── compat/cc/                    CC: Tweaked generic peripherals (only loaded when CC present)
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

