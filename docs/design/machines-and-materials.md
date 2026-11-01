# Machines, materials, the Dyson Sphere

Settled decisions, moved here from `CLAUDE.md`. Keep them current in the same PR as the change.

**Design doc "Nebulae to Planetary Genesis" (decided 2026-10-29)**
- The doc extends this plan; it is an idea framework, not a bible. The repo's systems stay (Replicator from the start, body, energy, dust). Where they disagree, ask before changing anything.
- Nebulae are the existing dust clouds: Ice, Carbon, Silicon (`silicate` in code), Metallic. Raw items (Ice Shards, Carbon Flakes, Silicon Grit, Metallic Grains) come from (a) the Replicator turning stored dust into raw items and (b) a collector part that pulls items out of a cloud. (a) is built (Replicator "Raw" tab); (b) is built as the Cloud Collector, see below.
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

