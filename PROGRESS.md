# Progress

## Setup (workflow step 1): done
- `CLAUDE.md` with plan, rules, toolchain versions, package layout, and settled decisions.
- ModDevGradle build (`build.gradle`, `settings.gradle`, `gradle.properties`, Gradle 9.2.1 wrapper).
- `neoforge.mods.toml` template: requires NeoForge 21.1.265+, Minecraft 1.21.1, GeckoLib 4.9.3+; JEI optional.
- Sodium, ModernFix, FerriteCore, and ImmediatelyFast added as dev-run-only (`localRuntime`) dependencies; compatibility rules in `CLAUDE.md`.

## Phase 1: Deep Space & Spawn: done
- `deep_space` dimension (datapack JSON): empty void, Y -2032..2031, fixed dim sun (`fixed_time` 12800), no mobs or weather, black fog.
- Sky (`DeepSpaceEffects`): full-sphere starfield and a small sun near the zenith; no clouds or rain.
- Spawn (`SpawnHandler`): new players and anyone stranded in a locked dimension are moved to the deep space spawn (config, default 0 64 0). Respawn always goes there (respects a future deep space respawn point).
- Dimension lock (`DimensionLock`): the Overworld, Nether, and End are blocked for all entities (portals and /tp), with a lore message. Creative and spectator players bypass it. Switching back to survival or adventure in a locked dimension sends you back. Nether portals can't be lit in deep space.
- Zero-G (`ZeroGravity`, `FallingBlockMixin`): gravity attribute is 0 for living entities, NoGravity for items, XP, and projectiles, falling blocks stay put. No fall damage. The vanilla "floating too long" kick doesn't trigger, because gravity is 0.
- Void boundary (`VoidBoundary`): soft push-back within 96 blocks of the build limits, client-side for the local player and server-side for everything else.
- Beds and respawn anchors (`SpawnDeviceBlocker`): use is denied with a lore message, no explosion.
- Energy (`EnergyData` attachment, `EnergyTicker`, `EnergyBarLayer`): replaces hunger (food pinned full). Idle drain, sunlight charging, emergency trickle below the 10% reserve, and low power mode at 0 with recovery at 5%. HUD cells replace the food bar and flash red in low power mode. Synced with NeoForge attachment sync.
- Debug command: `/void_reclaimer energy get|set <player> [amount]` (op).
- Datagen: `en_us.json` via `ModLangProvider` (`./gradlew runData`).
- Verified in a dedicated server (RCON) and a real client (Xvfb, with Sodium, ModernFix, FerriteCore, ImmediatelyFast, and JEI loaded): every behavior above was exercised.

### Placeholder assets
- `assets/void_reclaimer/textures/gui/sprites/hud/energy_empty.png`, `energy_empty_warning.png`, `energy_half.png`, `energy_full.png` (9x9 programmer art).
- The sun reuses vanilla `textures/environment/sun.png`.

## Phase 2: Zero-G Flight: done
- `ThrusterFlight` (shared): view-relative thrust (W/S look vector, A/D strafe, Space/Shift view-up), no drag, inertial dampener braking, hard speed cap. Active in zero-G for survival and adventure, and for creative when toggled.
- `LivingEntityTravelMixin`: replaces vanilla travel (and its air drag) for the local player under thruster flight.
- `ClientFlightController`: captures movement keys as `FlightInput`, clears vanilla walk/jump/crouch/sprint input, and sends changes with `ThrustInputPayload`. Handles the key bindings.
- Key bindings (Controls → Void Reclaimer): Toggle Inertial Dampener (Z), Toggle Thruster Flight (Creative) (V); `ToggleFlightSettingPayload`, with the server owning `FlightSettings` (attachment, synced).
- `ServerFlightHandler`: energy cost for thrust (2/s) and braking (1/s), and a 1-second-window speed check with teleport and `EntityTeleportEvent` resets.
- `FlightStats`: starter drone stats from config (mass 1, thrust 0.03, brake 0.04, top speed 10 m/s).
- `Energy` helper (consume / canPowerSystems / update) shared by energy ticking, flight, and commands.
- `FlightHudLayer`: speed and dampener/offline readout right of the hotbar.
- Verified in a real client (Xvfb, all compat mods loaded):
  - Thrust caps at 10.0 m/s.
  - The dampener stops you; with it off you drift about 10 m/s indefinitely.
  - Space/Shift move up and down.
  - At zero energy thrust does nothing, and the HUD shows THRUSTERS OFFLINE.
  - Energy drains while thrusting.
  - The creative V toggle switches between vanilla flight and thrusters.
  - No speed-check corrections during legitimate flight.

### Placeholder assets
- None new. The flight HUD is text only.

## Test & CI infrastructure: done
- JUnit 5 unit tests (19): `FlightPhysicsTest`, `FlightInputTest`, `EnergySimulationTest`. Pure logic was extracted into `FlightPhysics` and `EnergyRules` (behavior unchanged).
- GameTests (13), all passing, in `SpaceGameTests` and `FlightGameTests`:
  - zero-G entities and blocks; overworld gravity unaffected; void boundary push;
  - survival login, gamemode-switch, and respawn redirection; creative stays put; dimension lock;
  - sunlight charge, darkness drain, thrust cost, low power;
  - legal flight never corrected; speed hack pulled back. This closes the old "correction path untested" issue.
- Mutation-checked: disabling the falling-block mixin or the speed check makes the matching GameTest fail.
- `GameTestServerMixin`: the GameTest server now loads datapack dimensions (test-only effect).
- GitHub Actions `build.yml` runs build, unit tests, GameTests, and a datagen up-to-date check. Dependabot keeps the actions current.

## Phase 3: Cosmic Dust: done
- Sense of motion: `SpaceSpecksRenderer` (specks and speed streaks), `CameraSway` (off by default), `FlightSounds` and `ThrusterSoundInstance` (thruster and dampener loops), `ClientConfig`.
- `DustField` (deterministic cells, clouds, density, nearest-cloud search), `DustCloud`, `DustKind`, `DustFieldSeed` (synced level attachment, derived from the world seed).
- Cloud drag in `FlightPhysics` (`CloudDrag`), applied in `ThrusterFlight` on both sides.
- Dust collection: `DustTicker`, the `DustData` attachment (synced), and `FlightTracker.lastSpeed` from the server flight handler.
- Dust Sensor: the `DustSensorItem` item and creative tab; `DustSensorLayer` with readout, core and edge markers, signal bar, and echo; `DustMeterLayer`; `ClientSensorController` for pulse key B, echo, and hum; `SensorPulse` server logic; `SensorPulsePayload` and `SensorEchoPayload`.
- Commands: `/void_reclaimer dust get|set|locate`.
- Datagen: lang, `sounds.json` (vanilla placeholders), and the sensor item model.
- Tests:
  - 32 unit tests (+13): field determinism, rarity and size, density falloff, sampling, nearest search, echo accuracy and strength, blur bound, gain formula, capacity, drag, and the cloud top speed.
  - 21 GameTests (+8): seed derivation (the world seed doesn't leak), collecting the right kind, scoop beating trickle, no gain outside clouds, the capacity clamp, and pulse sensor, cost, cooldown, low-power refusal, and echo delivery.
  - Mutation-checked: removing scooping or the pulse cooldown fails the matching tests.
  - A unit test found that thrust could hold speed above the cloud's top speed; fixed.
- Verified in a real client:
  - Specks and streaks give clear parallax while thrusting.
  - The dust meter works.
  - The hum signal appears only when facing the cloud.
  - A pulse gives a "strong echo" marker toward a cloud 343 blocks away.
  - Inside an Ice cloud the readout shows 42% density with CORE and EDGE markers.
  - Speed held at 7.6 m/s in the cloud (expected about 7.5).
  - Ice dust collected and persisted across a restart.
  - No errors and no speed-check corrections.

### Placeholder assets
- `textures/item/dust_sensor.png` (16x16 programmer art).
- Sounds: `thruster` → `item.elytra.flying`, `dampener` → `block.beacon.ambient`, `sensor_pulse` → `block.beacon.power_select`, `sensor_echo` → `block.amethyst_block.chime`, `sensor_hum` → `block.note_block.bit` (vanilla events in `sounds.json`).

## Phase 4a: Replicator: done
- Replicator (key R, `ReplicatorScreen`): Tools / Modules / Blocks recipe tabs and a Logs tab. Locked recipes show "???" and a milestone hint. Costs turn red when you can't afford them. The game doesn't pause.
- Timed queue (`ReplicatorLogic`, `Replicator`, `ReplicatorData` attachment, synced):
  - Up to 5 jobs, with dust paid up front and refunded on cancel.
  - Uses 1.5 energy/s while working (config) and pauses in low power mode.
  - Output goes to the inventory. Overflow goes to a 9-slot output buffer, emptied with "Collect output". A finished job waits while the buffer is full.
- Custom recipe type `void_reclaimer:replicator` (`ReplicatorRecipe`: result, `DustCost` per kind, ticks, category, milestone), generated by `ModRecipeProvider`. Kept out of the vanilla recipe book. `ReplicatorActionPayload` carries queue, cancel, and collect.
- Milestones (`Milestone`): Awakening (on first login) and First Dust. Each unlocks recipes and adds an AI log entry, announced by a toast (`MilestoneToast`).
- Recipes:
  - Dust Sensor: M10 S15 C5, 20 s.
  - Scaffold ×8: S8, 5 s.
  - Salvage Tool: M15 S5, 30 s, unlocked by First Dust.
- Scaffold block (`ModBlocks`), tagged `void_reclaimer:salvageable`.
- Salvage Tool (`SalvageToolItem`, `Salvage`): breaks salvageable blocks fast, drops nothing, and refunds 50% (config) of the per-item recipe cost as dust.
- JEI: Replicator category (`VoidReclaimerJeiPlugin`, `ReplicatorJeiCategory`) showing dust cost, time, and the unlocking milestone.
- Tests:
  - 41 unit tests (+9, `ReplicatorLogicTest`).
  - 28 GameTests (+7, `ReplicatorGameTests`): unlocks, timed delivery, low-power pause, cancel refund, buffer and collect, salvage refund without drops.
  - Mutation-checked: ignoring low power, dropping the buffer, or removing the salvage refund each fail the matching test.
- Verified in a real client (all compat mods loaded):
  - The R screen and tabs work.
  - Queued 2 × Scaffold: 16 delivered, and silicate went from 50 to 34.
  - The Logs tab shows both entries.
  - JEI's Replicator category shows the Scaffold recipe. JEI's own R key (show recipe, in GUIs) doesn't conflict with ours (in game only).

### Placeholder assets
- `textures/block/scaffold.png`, `textures/item/salvage_tool.png` (16x16 programmer art).
- Milestone toast reuses vanilla's `toast/advancement` sprite. The Replicator screen is drawn with flat fills.

## Phase 4b: Nanite Swarm, Tether Anchor, Backup Core: done
- **Nanite Swarm** (`nanite/`: `NaniteSwarmItem`, `Nanites`, `NaniteTargeting`; `ClientNaniteController`):
  - Right-click builds the offhand block, or Scaffold from the inventory, in empty space 2–6 blocks ahead. The distance is stored in the item's `nanite_range` component; sneak + scroll changes it.
  - Looking at a block within reach builds against its face instead.
  - A white outline shows where the block will go.
  - Costs 1 energy per block (config) with a 0.5 s cooldown. Offline in low power.
  - Unlocked by First Dust (M20 S10 C10, 40 s).
- **Tether Anchor** (`tether/`: `TetherAnchorBlock`, `Tether`, `TetherData`, `TetherPhysics`; `ClientTetherController`, `TetherHudLayer`):
  - Right-click to clip on or off. While clipped, a leash stops you drifting past 16 blocks (config) and gently pulls you back.
  - Hold G to recall to the last anchor:
    - within 256 blocks, a 1 s hold starts a pull at 2× top speed costing 3 energy/s;
    - farther away, a 3 s hold teleports you for 25 energy, with a 60 s cooldown.
  - Recall works in the same dimension only and not in low power. While the pull runs, the server speed check steps aside.
  - The HUD shows "TETHER 12/16m" (orange when taut), the hold progress, and "RECALLING".
  - Breaking the anchor clears the tether.
  - Unlocked by First Structure (M15 S5, 20 s).
- **Backup Core** (`core/`: `BackupCoreBlock`, `BackupCoreBlockEntity`, `BackupCores`, `BackupCoreList`, `CoreCharging`; `BackupCoreScreen`, `DeathScreenCores`):
  - Up to 3 per player, each holding 0–3 charges.
  - Core UI: rename, charge instantly for Metallic 10 + Silicate 10, and a sunlight progress bar. Sunlight adds one charge per 12,000 lit ticks; deep space's sun gives 2/3 of that rate.
  - The death screen lists charged cores plus "Deep space spawn (free)". The chosen core spends a charge on respawn; if it's gone or empty, you go to the free spawn.
  - Unlocked by Diversified (M30 S20 I10 C10, 60 s).
- **Death marker** (`DeathMarker`, `DeathMarkerLayer`): "DROPS 123m" marker to where you died (when items drop), cleared within 8 blocks.
- **Milestones**:
  - First Structure: place 10 blocks, by hand or with nanites.
  - Diversified: hold 3 dust kinds.
  - Each adds a new AI log.
  - Criteria now take a `MilestoneContext`, and `ReplicatorData` counts placed blocks.
- **Starter nebula** (`StarterNebula`):
  - The `voidReclaimerStarterNebula` game rule (off by default) is read once when deep space first loads, saved on the level, and never re-read.
  - When on, four small pockets, one per dust kind, sit 450 blocks from spawn. The center reaches clients in `DustFieldSeed`.
- **Commands:** `/void_reclaimer milestone grant|revoke <player> <milestone>` and `/void_reclaimer core charge <player>`.
- **Save-data safety:** every saved attachment field has a default. Lists use lenient decoding, so one bad entry resets that field instead of failing the load.
- **Salvage:** Tether Anchor and Backup Core are salvageable.
- **Tests:**
  - 52 unit tests (+11): nanite targeting, leash and winch physics, core charging and core list, nebula placement, new milestones, and lenient decoding.
  - 40 GameTests (+12): nanite build, offhand, material and power; First Structure; clip, leash, winch energy and arrival, teleport and cooldown; core limit, ownership, sunlight, dust charging, rename distance, respawn at a core and fallback; death marker; nebula lock.
  - Mutation-checked: disabling core respawn, nanite energy, or the leash each fails the matching test.
  - The fake test client now acknowledges dimension changes (`hasChangedDimension`), so test players can take damage.
- Verified in a real client (all compat mods loaded):
  - The nanite outline shows, and building in empty space works (Scaffold 16 → 15).
  - Clipping an anchor shows "TETHER 1/16m". Thrusting away with the dampener off stops at 16/16m (orange).
  - Holding G shows "RECALL 60%", then pulls back to 1m with no speed-check corrections.
  - The core UI renames ("Home") and charges (M50/S34 → M40/S24).
  - After /kill, the death screen lists "Home (1)". Choosing it respawns at the core, and "DROPS 9m" points back.
  - Revoking a milestone shows the toast again.
- Bugs found in the client and fixed:
  - Right-clicking an anchor or core while holding a block toggled the block's own action instead of placing against it. Building tools now skip it.
  - A milestone reached while offline was granted before the client's first data sync, so no toast appeared. Milestone checks now wait 2 s after login.

### Placeholder assets
- `textures/item/nanite_swarm.png`, `textures/block/tether_anchor.png`, `textures/block/backup_core.png` (16x16 programmer art). Tether Anchor and Backup Core are plain cubes (`cube_all`).
- Sound: `nanite_build` → `block.amethyst_cluster.place`.
- The core screen and the death screen list are drawn with flat fills and vanilla buttons.

## Fix: stuck tutorial hint
- Vanilla's "Move with WASD" tutorial toast never cleared, because thruster flight replaces walking input. `ClientFlightController` now ends the vanilla tutorial once thruster flight is active.

## Create is now required
- Create 6.0.10 is a required dependency (`neoforge.mods.toml` range `[6.0.10,6.1)`, compiled against the Modrinth release jar).
- Verified: build, all 40 GameTests, and the client with Sodium, ModernFix, FerriteCore, ImmediatelyFast and JEI. A creative motor spins cogwheels in deep space, with no errors.
- `TestPlayers` declares every registered play payload on its fake connection, because Create sends its own packets on login.
- Known: on software OpenGL (the headless test client) Flywheel logs a GLSL 4.60 error and falls back; on real GPUs this shouldn't appear.

## Cloud scanner: done
- `CloudShape` (pure): the true cloud edge on a 16×32 latitude/longitude ray grid, at density 0.05, refined by bisection.
- `CloudScannerRenderer`: with a Dust Sensor, key N toggles a wire mesh with a faint tint in the kind's color for clouds within 512 blocks. Meshes are computed in the background and cached. Drawn unfogged; fog state is restored afterwards.
- Client config `cloudScanner` (default on) is saved when toggled.
- Tests: 54 unit tests (+2, `CloudShapeTest`: no dust beyond the edge, dust at the edge, lumpy, deterministic, closed seam). Mutation-checked: returning the outer bisection bound fails the test.
- Verified in a real client: an Ice cloud seen from inside (grid around you) and from outside (lumpy globe); N turns it off and on, and the setting persists.

## Create Aeronautics spike (superseded: Aeronautics is now required, see Foundations)
- Retested on branch `claude/aeronautics-spike` (never merged; results in `AERONAUTICS_SPIKE.md` there).
- The first spike's "flung away" ships were falling: the zero-G `dimension_physics` file wasn't in effect. With it, platforms rest in `deep_space`, players can ride moving ships, and height and f32 precision were not factors.
- Propellers and balloons need air, so they give no thrust in vacuum. Phase 7b adds our own Space Thruster and a Helm block.
- `sable64bit` (f64 physics) keeps far-out positions exact; recommended, not a dependency.
- Upstream: an intermittent start-up deadlock between Aeronautics/Simulated/Veil and Create render classes in client-dist runs; keep Aeronautics out of CI datagen.

## Phase 5a: Body data: done
- **Body model** (`body/`):
  - `SizeClass` (Drone 0.4, Mech 1.2, Ship 3.0 via `generic.scale`) and `PartKind` (frame, core, thruster, storage, sensor, tool, cosmetic).
  - `PartStats` is a default data component on part items. `BodyData` is the synced, persisted attachment and defaults to the starter drone.
  - `BodyStats` holds the pure stat math; `BodyRules` holds the pure install rules.
- **Stats from parts:**
  - Flight (`FlightStats.of`), energy capacity and solar rate (core), max health, reach and dust intake (frame), inventory rows and dust capacity (storage).
  - Scale, health and reach are applied as transient attribute modifiers on login, respawn and every change.
  - The starter Drone (frame, Core Mk I, Thruster Mk I) has exactly the Phase 2 handling.
- **Soft mass limit:** past the frame's budget, acceleration, braking and top speed fall and thruster energy use rises.
- **Tier gate:** parts need a core of at least their tier; a weaker core is refused while higher-tier parts are installed; frames need the core tier too.
- **Frame swap:** parts move into the new frame's slots in order; parts that don't fit (slot count or size class) and the old frame go to the inventory. Frame and core can only be replaced.
- **Inventory rows** (`InventoryLock`, `InventoryLockMixin`, `SlotLockMixin`):
  - The drone has the hotbar plus 1 row; frame base rows and storage parts unlock up to 36 slots.
  - Locked slots never take new items (pickup, `/give`, menus, shift-click) but can still be emptied, so nothing is trapped.
  - Creative and spectator are exempt. Locked slots are greyed out in container screens.
- **Modules:**
  - The Dust Sensor works only when installed in a sensor slot.
  - The Nanite Swarm and Salvage Tool are tool modules, used with an empty main hand. Nanite: right-click in empty space; sneak + right-click on a block builds against it; sneak + scroll sets the range, stored on the installed module.
  - Carried module items do nothing. The module wheel (hold Left Alt) picks the active tool; the HUD shows it above the flight readout.
- **Body tab** (6th Replicator tab, `BodyPanel`): slots by kind, the stats panel, the selected slot's part with [Remove], installable parts from the inventory (invalid ones dimmed with the reason), casing material, thruster trail and accent swatches.
- **Cosmetics:** casing (Andesite, Brass, Copper), accent tint and thruster trail are stored now; trails are drawn as dust particles while thrusting. Models come in 5b.
- **Content:**
  - Drone, Mech and Ship frames (Ship defined but not craftable until Phase 6); Core Mk I/II; Thruster Mk I/II (Mk II needs a Mech); Storage Pod Mk I/II; Brass Trim and Cogwheel Crown cosmetics.
  - A new **Parts** recipe tab, and the **Expansion** milestone (any dust kind at capacity), which unlocks Mk II parts and the Mech frame.
- **Command:** `/void_reclaimer body get|reset <player>`.
- **Tests:**
  - 60 unit tests (+6, `BodyStatsTest`).
  - 46 GameTests (+6, `BodyGameTests`: starter stats on join, install/remove item movement and rules, Mk II core and Mech frame swap with transfer and scale, locked slots, Nanite module with an empty hand, Expansion). Sensor and salvage tests now install modules.
  - Mutation-checked: disabling the lock or the tier gate fails the matching tests.
- Verified in a real client:
  - Drone body (6 hearts, scale 0.4); the Body tab shows the starter stats.
  - Installed the Dust Sensor (HUD and scanner return) and the Nanite Swarm from the inventory.
  - Empty-hand build in space (Scaffold 15 → 14); module HUD; locked rows greyed in the inventory; module wheel; thruster trail particles; no speed-check corrections.

### Placeholder assets
- Item textures: `drone_frame`, `mech_frame`, `ship_frame`, `core_mk1`, `core_mk2`, `thruster_mk1`, `thruster_mk2`, `storage_mk1`, `storage_mk2`, `brass_trim`, `cogwheel_crown` (16x16 programmer art in Create-like colors).
- The body itself is still the vanilla player model, scaled. Casing, accent and cosmetic parts aren't visible until 5b.
- Thruster trails use vanilla dust particles.

## Extensibility + body sections: done
- **Extensibility** (`api/`):
  - Datapack registries for dust kinds, milestones (with code-registered criterion types), Replicator categories, casings, trails, size classes and chassis.
  - Code registry for tool module types; data components for sections and modules; the `#void_reclaimer:modules/dust_sensor` tag.
  - Events: `DustGainEvent`, `MilestoneReachedEvent`, `ReplicatorJobFinishedEvent`, `BodyStatsEvent`, `BodyChangedEvent`, `ModuleUseEvent`.
  - The full list is in CLAUDE.md under "Extension points". A GameTest-only datapack (`void_reclaimer_test`) and tagged test listeners cover each one.
- **Body sections** (replaces frames and part slots):
  - A core section names a chassis: a tree of sockets, each with accepted socket types, a parent and a pivot. Sections offer module mounts.
  - Only the core is required; missing sections leave their sockets (and children) empty.
  - Built-in: Drone (hull, rotor arms, sensor dome, tool claw) and Mech (torso, pelvis, head, arms, legs; Mk II forearm and shin). The Ship keel is defined but not craftable.
  - Old frame/part saves migrate on load (frames become their chassis's basic sections). Old frame items are aliased to the new core sections. The body is kept on death.
- **Body tab:** socket tree with area filters, the selected socket's section and mounts, candidates from the inventory, condition and repair, per-section paint, and the Blueprint row.
- **Paint:** the body default plus per-section casing and accent overrides.
- **Wear:** sections and modules lose condition with thrust time and tool use (config). Below 50% condition, efficiency falls up to 25%. Repair costs about 20% of the recipe dust. Wear never breaks a part, and there is none in creative.
- **Body Blueprint** item: save the current body, apply it from the inventory, and queue Replicator jobs for missing parts.
- **Replicator recipes with several outputs:** `results` list and optional `title`. A job waits until the output buffer has room for all of its items. Kits: **Drone Kit** (hull, dome, claw, 4 rotor arms) and **Mech Core Kit** (torso, pelvis, head).
  - Salvage prices a part by single-item recipes first, kits only as a fallback. Blueprint queueing picks the cheapest recipe per wanted part.
- **Tests:**
  - 67 unit tests.
  - 63 GameTests (new: `ExtensionGameTests`, `SectionCareGameTests`, kit delivery; `BodyGameTests` rewritten).
  - Mutation-checked: each new test fails with its feature broken.
- Verified in a real client (with the performance mods):
  - Old save migrates; the Body tab shows the socket tree, stats, looks and the Blueprint row (Save, Apply, Queue).
  - Kit rows show "+N" for extra parts. A Drone Kit job took 40/20 dust and delivered all 7 parts. JEI shows kit parts in a row under the text.
  - Fixed during the check: the trail chip, Blueprint chips and locked-recipe "???" overflowed their areas.

### Placeholder assets
- Item textures for all section items (`drone_hull`, `rotor_arm`, `rotor_arm_mk2`, `sensor_dome`, `tool_claw`, `mech_*`, `ship_keel`) and `body_blueprint`. They are 16x16 programmer art.

## Foundations: Aeronautics required, Dyson Sphere counters and sky: done
- Create Aeronautics 1.3.2 and Sable 2.0.6 are required (`neoforge.mods.toml`, `gradle.properties`); runtime and test-runtime only.
- `dimension_physics/deep_space.json`: zero physics gravity, pressure and drag, so Sable sub-levels don't fall.
- `dyson` package: `DysonSphereData` attachment (saved, synced), `DysonTiers` (pure math), `Dyson` (launch entry point), config `dyson.latticeStart` / `dyson.shellTotal` (placeholders, the doc leaves them open).
- Milestone criterion `dyson_tier` and milestones `dyson_tier_1..4` for the AI log; `MilestoneContext` gained `dysonTier`.
- Command `/void_reclaimer dyson get|set satellites|trusses <n>`.
- Sky (`DysonSky`): swarm points, lattice, shell, corona and conduits, sun tint and dimming.
- Tests: unit (`DysonTiersTest`, `DysonTierCriterionTest`), GameTests (`DysonGameTests`, 3), separate Aeronautics GameTests (`AeronauticsGameTests`, 2, `./gradlew runGameTestAeronautics`, own CI job). Mutation-checked: removing the physics file makes both Aeronautics tests fail; removing the milestone check makes two Dyson tests fail.
- CI: `runData` now runs with a timeout and up to 3 attempts because required Aeronautics brings the upstream start-up deadlock into it.
- Merged the unmerged `claude/handoff-notes` commit (`HANDOFF.md`, `tools/client-test/`).

### Placeholder assets and texts
- The Dyson sky uses untextured coloured quads (no texture files).
- Log texts and titles for `dyson_tier_1..4` in lang are placeholders awaiting the user's lore.
- `dyson.latticeStart` = 1 and `dyson.shellTotal` = 1000 are stand-in numbers.

### Not done yet
- Power beams to the Quantum Receiver Dish (needs that block).
- Solar charging is not reduced by the Dyson tier (see `CLAUDE.md`).
- Display strings still say "Ship", not "Starship".

## Audit fixes: done
From an outside audit, each checked against the code first (several claims were stale or wrong).
- **Nanite Swarm against blocks in flight:** thruster flight clears the vanilla sneak flag, so the server's `isShiftKeyDown()` was always false and block-targeted builds never worked in deep space. `BodyActionPayload` now carries the client's sneak key; `Modules.use(player, sneak)` and a new `ToolModuleType.use(..., sneak)` overload (defaults to the old method) use it.
- **Removing a storage module:** room is now counted under the new body's slot limit, so a removal that would leave no room is refused instead of dropping the item into space.
- **Max health on relog:** the body's max health modifier is permanent, so it is saved and vanilla doesn't clamp a bigger body's health on login.
- **Tether recall:** the teleport checks the anchor block still exists (the chunk may have been unloaded when it was removed) and clears a stale anchor. The client's held-key flag resets on disconnect. Not covered by a test: it needs an unloaded chunk.
- **Build:** `exclude('.cache/**')` (the old pattern never matched, so the datagen cache shipped in the jar).
- Tests: three GameTests and a payload codec unit test, mutation-checked (reverting each fix fails its test).
- Not done: shift-click onto a partial stack in a locked slot, Create's nested libraries on the compile classpath (do it with the first code that imports Registrate, Ponder or Flywheel), the GitHub default branch (a repo setting).

## Phase 6: Asteroids: done
- `AsteroidChunkGenerator` (`void_reclaimer:asteroids`, replaces `minecraft:flat` in `deep_space.json`), `AsteroidField` and `AsteroidBody` (pure, unit-tested): sparse clusters on a 1024-block grid, small rock fields, large bodies, rare giants up to about 300 blocks wide, spawn kept clear, kind from the nearest dust cloud.
- Blocks `asteroid_silicate`, `asteroid_ice`, `asteroid_carbon`, `asteroid_metallic`, tag `#void_reclaimer:asteroid_blocks`.
- **Mechanical Drill** (dust refund, no drop) and **Matter Harvester** (block drops whole): tool modules, Replicator recipes in the Tools tab, unlocked by First Dust. Asteroid rock cannot be broken without one, and neither works in low power mode. Config `asteroids.drillEnergy`, `asteroids.drillDustYield`.
- Tests: `AsteroidFieldTest` (8 unit tests) and `AsteroidGameTests` (6 GameTests: real chunk generation, clear spawn, no break without a drill, drill refund, harvester drop, low power). Mutation-checked from a fresh world: the flat generator fails the generation test, a disabled speed gate and a drill that doesn't cancel fail the drill tests. The unit tests also caught a real cache-key collision while writing them.
- Fixed a flaky existing test: `salvageToolDismantlesForDustWithoutDrops` now loads its chunk before waiting (it failed in about half of fresh worlds).
- Generation cost: about 65 ms per chunk column through the middle of a 195-wide giant.
- Worlds made before this phase keep their flat deep space (the generator is saved with the world). Delete `run/world` locally before running GameTests after changing the generator, or the old one is used.

### Placeholder assets
- Block textures `asteroid_silicate.png`, `asteroid_ice.png`, `asteroid_carbon.png`, `asteroid_metallic.png` (noisy 16x16 programmer art) and item textures `mechanical_drill.png`, `matter_harvester.png`.
- Recipe prices for the drill and harvester are guesses.

### Not done yet
- Ship frame unlock (waits on the Starship upgrade order), ore veins inside asteroids, faster interior fill for giants.

## Phase 7, part 1: nebula materials and Create chains: done
- 22 items, six fluids with buckets, a "Raw" Replicator tab (8 dust of a kind to 4 raw items), and 20 Create recipes covering the design doc's Act I chains that need no new machine code: crushing, milling, mixing in basins, pressing, fan washing, fan blasting, deploying and the solar sail's Sequenced Assembly. See the "Machines, part 1" block in `CLAUDE.md` for the full list, the assumptions, and what the doc's open questions still block.
- Config toggles (`materials`): `replicatorRawItems`, `iceChain`, `carbonChain`, `siliconChain`, `metallicChain`, `solarSail`, through the recipe condition `void_reclaimer:config`.
- Phase 6 items (drill, harvester, asteroid blocks) were missing from the creative tab; added.
- Tests: `MaterialsGameTests` (5: every chain recipe loads as its Create type, item results, fluids and buckets, toggles, Replicator raw items) and `ConfigConditionTest`. Mutation-checked: a corrupted recipe type, a wrong result item and a condition that ignores its toggle each fail their test.
- Checked in a real client: the Raw tab lists the four recipes with icons.

### Placeholder assets and numbers
- Item textures for the 22 items and six buckets (small generated programmer art, one silhouette per kind of item); fluids reuse vanilla water textures with a tint.
- Fluid amounts, times and the 8-dust-to-4-items Replicator ratio are placeholders.

### Not done yet (part 2 and later)
- Custom machines: Electrolyzer (water to oxygen and hydrogen), Spinner (graphite to carbon thread), Encased Fan freezing (packed ice), Spout casting onto a Depot (Quartz Wafer). Until then Quartz Wafer, Carbon Thread, Oxygen and Hydrogen can't be made, so the solar cell, the propellant and the solar sail recipes are present but not yet reachable.
- Collector part for raw items out of a cloud (the second source of raw items, besides the Replicator).
- Everything blocked on the doc's open questions (see `CLAUDE.md`).

## Phase 7, part 2: custom Create machines: done
- **Electrolyzer** (water to oxygen and hydrogen) and **Spinner** (graphite to carbon thread): SU-driven kinetic blocks with a shaft axis, tanks and inventories that pipes, funnels and belts use, Replicator recipes (Blocks tab).
- **Encased Fan freezing** (powder snow, Water Bucket to Packed Ice and an empty Bucket) and **spout casting** (Liquid Silicon into a Quartz Wafer on an empty Depot).
- These close the gaps from part 1: Quartz Wafer, Carbon Thread, Oxygen and Hydrogen can now be made, so the solar cell, the propellant and the solar sail are reachable.
- Config toggles and values under `machines` (see the "Machines, part 2" block in `CLAUDE.md`).
- `build.gradle` gained `extractCreateLibs` so code can compile against Create's nested Flywheel, Ponder and Registrate.
- Tests: `MachineGameTests` (8: Electrolyzer, Spinner, stress impact, fan type and its registry, spout casting, recipes) and `MachineMathTest` (3). Mutation-checked: no stress registration, a fan that accepts air, casting onto an occupied depot and ignoring the minimum speed each fail a test. A real Create motor drives the machines in a real client (see below).

### Placeholder assets and numbers
- Block textures `electrolyzer_side/end.png`, `spinner_side/end.png` (generated programmer art; the end shows the shaft). Static models: no rotation animation yet.
- Fluid amounts, 60-tick cycles, 16 RPM minimum, 4 and 2 SU per RPM, 2 threads per graphite and the Replicator prices are placeholders.

### Assumption to confirm
- The doc's freezing input is just "Water": it is a Water Bucket here (returning the empty bucket).

### Not done yet
- Goggle tooltips, rotation animation, a JEI category for freezing, Ponder scenes, the collector part for raw items from a cloud, and the items blocked on the doc's open questions (see part 1).

## Phases
- [x] 1. Deep Space & Spawn
- [x] 2. Zero-G Flight
- [x] 3. Cosmic Dust
- [x] 4a. Replicator (queue, milestones and logs, JEI, Scaffold, Salvage Tool)
- [x] 4b. Nanite Swarm, Tether Anchor, Backup Core (plus death marker and starter nebula)
- [x] 5a. Body data (frames, parts, size classes, scale, stats, storage, modules)
- [x] Extensibility + body sections (datapack registries, events, chassis/sockets, paint, wear, blueprint, kits)
- [ ] 5b. Body rendering (GeckoLib, first person) & assembly UI
- [ ] 6. Asteroids
- [ ] 7. Machines
- [x] Foundations: Aeronautics required, Dyson counters and sky
- [ ] 7b. Create Aeronautics ships (Space Thruster, Helm)
- [ ] 8. Stations & Helpers
- [ ] 9. Ruined Earth
- [ ] 10. Terraforming

## Known issues
- Opening a world shows vanilla's "Worlds using Experimental Settings" warning, because the mod adds a datapack dimension. NeoForge usually suppresses it after the first confirmation, but it came back after the test world was opened by a dedicated server. It's harmless; it could be removed with a mixin if wanted.
- Sounds were not heard during testing (the headless test client has no audio device). The sound events, `sounds.json`, and the triggering logic are in place, but volume and pitch need a listen on a real machine.
- Dust Sensor markers use the FOV setting, not the dynamic FOV (sprint or speed effects), so markers can be slightly off-position while the FOV is changing.
- A cross-dimension `/tp` that the lock cancels still prints vanilla's "Teleported ..." chat line. The player does not move, and the lock message shows on the action bar.
- The flight HUD sits right of the hotbar and overlaps the first-person hand area slightly. It will be revisited when Phase 5 replaces the player model.
- Pointed dripstone (and modded falling blocks that don't extend `FallingBlock`) still detach. Their falling entity floats in zero-G and drops as an item after vanilla's 30-second timeout.
- Gravity is set when an entity joins a level. When gravity generators arrive, this has to become position-based.
- Without the starter nebula (the default), a world can spawn far from any Silicate cloud. You can't make Scaffold without silicate, so the start may mean a long flight. Revisit after play-testing.
- Sneak + scroll for the Nanite range also thrusts down while Shift is held (Shift is the "down" thruster key). With the dampener on the drift is small.
- Every player is now a 0.4-scale drone, so the vanilla player model looks tiny until 5b replaces it.
- With an active Nanite module, a plain right-click on a block uses the block; sneak + right-click builds against it.
- Thruster trails are only drawn for the local player.
- The death screen list shows core charges as of the moment of death; sunlight charges gained while the screen is open appear only after respawn.
- Respawn at a core always uses the spot above the core (or the first free side). It doesn't check whether that spot is dark or enclosed.
- In one client session, queueing a kit did nothing (no job, no dust taken, no message) while single items queued fine. It did not happen again after a restart, and logging showed the request arriving and queueing correctly. Watch for it.
- The milestone toast was not caught in a screenshot (it fires on first login, during "Loading terrain"). Its trigger is covered by tests.
