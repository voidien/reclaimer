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

## Phase 8: Zero-G Frame, payloads and machinery recipes: done
- 6 items + 6 transitional items and 6 Create Sequenced Assembly recipes (frame, swarm payload, truss module, RCS Thruster, Reaction Wheel Gyroscope, Atmospheric Catalyst Core), each with the owner's steps and a config toggle (`frame`, `dysonPayloads`, `rcsThruster`, `reactionWheel`, `catalystCore`). See the Phase 8 block in `CLAUDE.md`.
- The provider's Sequenced Assembly code is now one generic builder; the existing solar sail recipe regenerates byte-identical.
- Dyson sky thresholds default to 50 (lattice) and 2000 (shell complete).
- Tests: `frameAndMachineryAreTheOwnersSequencedAssemblies` (checks base, result, transitional item, loops and the type of every step through Create's own recipe class), `defaultsAreTheOwnersTrussCounts` and the extended toggle test. Mutation-checked: a wrong loop count, a swapped step and a wrong result each fail the GameTest; a reverted tier default fails the unit test.
- Placeholder icons for the 12 new items (generated). Art ids to deliver: `zero_g_precision_frame`, `dyson_swarm_satellite_payload`, `dyson_shell_truss_module`, `rcs_thruster`, `reaction_wheel_gyroscope`, `atmospheric_catalyst_core` and their `incomplete_` twins.
- Not here: the Mass Driver (Phase 9), the Quantum Receiver, the Starship milestone (Phase 10), placeable RCS Thruster and Gyroscope (Phase 7b).

## Phase 9: Kinetic Mass Driver, Quantum Receiver, power beams: done
- Mass Driver (drive base, payload chamber, barrel nozzle), launch rate from 20 s at 64 RPM to 3 s at 256 RPM, hand-off to `Dyson.launchSatellites/launchTrusses`, `MassDriverLaunchEvent`, payload item tags, sky streak, Sub-Light Quantum Receiver (output proportional to trusses, 262,144 SU at the full shell), its Sequenced Assembly recipe, and the sky's power beams. Details and assumptions in the Phase 9 block in `CLAUDE.md`.
- Tests: `MassDriverMathTest` (8) and 7 GameTests in `MassDriverGameTests` (launch sequence and counts, rate by speed, below-minimum never launches, incomplete/switched-off driver, chamber rules, event change, event cancel, tag-driven payloads via the GameTest datapack, receiver output). Mutation-checked: swapping truss and satellite hand-off, and a constant receiver fraction, each fail unit and GameTests.
- Placeholder textures (generated): `kinetic_drive_base`, `quantum_receiver` (side/end), `payload_chamber`, `magnetic_barrel_nozzle`, and the item `incomplete_quantum_receiver`. Sound `mass_driver_launch` uses a vanilla stand-in.
- Not checked in a real client: the streak and beam rendering (no visual test run for this phase); verify in game: fire a payload in deep space and look towards the star, and place a running Receiver.

## Phase 10: Starship milestone and Ship recipes: done
- New milestone criterion `items_made`; milestone `starship` (one item from each nebula chain); Replicator recipes for the 14 Ship sections and a Ship Core Kit behind it. See the Phase 10 block in `CLAUDE.md`.
- Tests: `ItemsMadeCriterionTest` (6 unit tests, including old-save loading) and 3 GameTests in `StarshipGameTests` (needs all four chains, even after giving items away; Ship recipes locked then unlocked; a datapack milestone using `items_made`). Mutation-checked: "any one item" instead of "all items" fails the unit test and two GameTests. `DysonGameTests` now resets the sphere first, because the persistent GameTest world can hold leftovers from an earlier failed run.
- Ship section icons are real art from drop 1; prices are placeholders.

## Phase 5b-1: body rendering, third person: done
- Replaces the vanilla player model with the GeckoLib body (composed Drone and Ship rigs, whole-model Mech from the owner's RD model), casing and accent paint, pose animations, camera distance, body sync to other players, `body preset` and `body remove` debug commands. Rules and file layout in the Phase 5b-1 block in `CLAUDE.md`.
- Tests: `BodyLookTest` (pose, bone names, casing painting, camera), `BodyRigSpecTest` (the Mech spec against its model, animations and chassis), and 5 GameTests in `BodyPresetGameTests`. Mutation-checked: reversed strafe and back directions fail the unit tests, and a preset without the jetpack failed its GameTest.
- Checked in a real client (with the performance mods): the Mech composes and animates, limbs disappear when their sections are removed, the Ship and Drone render and the camera clears the Ship.
- Not done (next 5b PR): first-person view (the vanilla hand still shows) and the 3D assembly view in the Body tab.
- New art in the repo: `geo/entity/mech_suit`, `textures/entity/mech_suit.png`, `animations/entity/mech_suit` (the owner's RD model), `body_rigs/mech.json`. The older `geo/body/mech.geo.json` and the Mech section models are unused while the whole-model spec exists.

## Module models: done (art)
- All 8 missing module models arrived through the owner's PRs #34 and #35 (Retro Diffusion, promoted with the default 4 px fit): `core_mk2`, `storage_mk1`, `storage_mk2`, `mechanical_drill`, `matter_harvester`, `salvage_tool`, `nanite_swarm`, `dust_sensor`. Each has its root and `_mount` bones; the longest side is about 4 px for all (the tools were suggested at 7 to 8 px, so they are small; re-promote with `--fit 7` to enlarge).
- The Mech does not show modules (its whole-model rig has none); the Drone and Ship do. In the real client the Drone shows them, but the art's textures are very dark, so they read as silhouettes. `body preset ... full` puts all of them on a body; `fullPresetsCarryEveryModuleAndAllFit` checks that they all fit.

## Launcher model (art only, not used yet)
- The owner's RD "launch mortar" (a launch complex for the Dyson sphere's Kinetic Mass Driver) promoted as `entity/mass_driver` at native size: a square pad about 15 x 15 blocks with a tall gun tower (14 blocks), two dishes and two cranes (groups `gun`, `dish_a`, `dish_b`, `crane`, `crane2`; 81 boxes; no animations). The current Mass Driver is three 1x1 blocks, so how the model is used (scale, footprint, launch animations) is an open question for the owner.

## Shuttle layout, Drone probe skin: done
- Shuttle: a second Ship-class chassis (11 sockets, 8 tier-3 sections, a core kit, recipes under the Starship milestone) drawn by the owner's RD shuttlecraft through `body_rigs/shuttle.json`; Drone: the owner's RD mining probe as a skin through `body_rigs/drone.json`, with placeholder animations (the model had none). Details in the "Shuttle layout" block in `CLAUDE.md`.
- Tests: `BodyRigSpecTest` now covers four specs; GameTests `shuttlePresetIsComplete`, `shuttleSharesTheShipSizeClassButHasItsOwnLayout`, the tier and full-preset tests include the Shuttle, and the Starship recipe test includes the Shuttle's recipes.
- Placeholders: icons for the 8 Shuttle sections; the probe animations; prices.
- Not checked in a real client yet (see the chat).

## Shuttle, station and mining probe models (art)
- Three more RD models from the owner, promoted with `rd_generate.py --promote-as entity/NAME` at native size: `shuttle` (5.8 x 2.3 x 8.5 blocks; bones body, nose, windshield, dome, ridge, aft, struts, two nacelles with flames; animations `engines_firing` loop, `hatch_open` and `hatch_ramp` once), `station` (a ring station, 14 x 13.5 x 14.6 blocks, 203 boxes in 6 groups st, br, sol, py, py_1, py_2; no animations) and `mining_probe` (1.2 x 1.1 x 2.0 blocks, two arms with elbows; no animations).
- Nothing draws them. Their roles (helper drone, vehicle, station structure) are the owner's decision; see the question in the chat.

## Titan size class, Core Mk III and IV, Mech mounts: done
- Titan: size class (scale 8, about 67 blocks), chassis with 10 sockets, 8 tier-4 section items, Titan Core Kit, whole-model rig spec over the owner's RD ship, Replicator recipes at about ten times the Ship's prices (placeholder, Dyson tier 3 unlock). Core Mk III and Mk IV (new modules) because Ship sections could not be installed with a Mk II core at best. Mech module mounts and a 180 degree turn through the rig spec. Details in the "Titan size class" block in `CLAUDE.md`.
- Tests: `BodyRigSpecTest` now covers the Mech and the Titan (sockets, bones, mounts, animations, mount names) and the new spec fields; GameTests `titanPresetIsComplete`, `titanIsTheBiggestSizeClass`, `shipAndTitanSectionsNeedTheirCores` and `titanAndCoreRecipesWaitForTheirMilestones` (113 pass). Mutation-checked: making Core Mk IV tier 3 fails the tier test.
- Checked in a real client: modules draw on the Mech facing forward; the Titan renders whole from 128 blocks.
- Placeholders: icons for `core_mk3`, `core_mk4` and the 8 Titan sections (generated); prices and Titan unlock.
- Found on the way: the camera factor first multiplied the distance by the scale twice (the whole Titan was one gray square 1000 blocks away); fixed and the test says why.

## Titan ship model (art only, not used yet)
- The owner's RD "blocky starship" (a saucer on a neck with two pylons, nacelles with bussard collectors, a hangar door, flame effects) promoted with `rd_generate.py --promote-as entity/titan` at RD's native size (64 x 43 x 134 px, so 4 x 2.7 x 8.4 blocks at scale 1): `geo/entity/titan.geo.json`, `textures/entity/titan.png`, `animations/entity/titan.animation.json`.
- Animations kept: `warp_cruise` (loop, 4 s) and `bay_open` (once, 1.6 s). The owner's longer export also had `body_rotate` and `bridge_spin`; they were left out as unnecessary.
- Nothing draws it yet. Its role (a "Titan" size class bigger than the Ship, a Ship skin, or a separate vehicle) is the owner's decision; see the open question in the chat. `GeoModelLoadTest` loads the model and animations.

## Art drop 1 imported: done
- From the owner's art repo (`void-reclaimer-art`, branch `art/tier-c-pilot`, supplied as a zip): 49 placeholder textures replaced by real art (the 3 original block textures, the 4 HUD energy sprites, 42 item icons including all Ship sections, an animated 16x64 Nanite Swarm icon with its `.mcmeta`), plus 124 new files: 3 body rigs, 30 section models, 4 module models, 3 first-person pieces, 3 animation files and 80 entity textures (base + accent). No code uses the models, animations or entity textures yet: they are for Phase 5b.
- Checked: its socket ids and relative pivots equal our Drone, Mech and Ship chassis JSON (0 mismatches); PNG sizes and modes are as documented. Not run: its Python generators (we don't need them) and Blockbench (the sources there for non-ship pieces are stubs, per its README).
- **Still our placeholders (for the next art request; all replaced by RD art pass 1 below except three items):**
  - 31 item icons: Mechanical Drill, Matter Harvester; the 6 fluid buckets (purified water, oxygen, hydrogen, nebulite propellant, coolant, liquid silicon); and the nebula materials: advanced sensor unit, carbon composite plate, carbon flakes, carbon paste, carbon thread, coolant frame plate, crushed cryo-ice, doped silicon ingot, fine carbon powder, high-efficiency solar cell, high-output solar sail (and its incomplete form), high-purity graphite, metallic grains, micro-alloy sheet, pure silicon dust, quartz wafer, raw ice shards, refined metal nodule, silicon-carbide coating, silicon-carbide slurry, silicon grit, thermal shielding tile.
  - 8 block textures: the four asteroid rocks (silicate, ice, carbon, metallic), and the Electrolyzer and Spinner (side and end each).
  - The fluids themselves reuse vanilla water textures with a tint.

## Silicon-Carbide Coating and planning decisions: done
- New item Silicon-Carbide Coating and a Create basin-compacting recipe (Silicon-Carbide Slurry + Carbon Composite Plate, makes 2), under the `carbonChain` toggle.
- Interview decisions recorded in `CLAUDE.md` ("Decisions after Phase 7"): ships kept as extras, `ruined_earth` stays, one Spire per sector, and a revised phase plan around the design doc's Acts.
- The extension-point table now lists the drill and harvester tool types, the freezing recipe and fan type, spout casting, the config condition, the asteroid generator and the asteroid tag.

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

## Test Starship (placeholder entity): done
- A spawnable **Test Starship** for checking RD models and animations in game. It uses the owner's RD "Create style starship" `.bbmodel`, converted with `rd_generate.py --promote-as entity/starship --scale-like body/ship`. That makes it as long as the Ship rig, and it is drawn at the Ship size class's scale (3.0, read from the synced registry), so it is about 12 blocks long.
- `starship.TestStarship` is a plain GeckoLib entity. It has no gravity, no AI and no drops, and breaks with one hit. It drifts round its anchor: a 3-block circle every 60 s, a 1-block bob, one turn every 72 s (`StarshipDrift`, worked out from game time on both sides, so no movement packets). It loops `gear_cycle`, and a right-click plays `ignite_prep` once (48 ticks).
- **Spawning:**
  - the Test Starship item (creative tab, vanilla spawn egg look, no recipe);
  - `/void_reclaimer starship spawn` (12 blocks ahead);
  - `/summon void_reclaimer:test_starship`.
- **Replacing it:** run `--promote-as entity/starship --scale-like body/ship --source NEW.bbmodel`. The files are overwritten in place. The animation names must stay `ignite_prep` and `gear_cycle`, or the entity's names must change too.
- `rd_generate.py --promote-as` now honours `--scale-like`. It was ignored before.
- **Tests:**
  - `StarshipEntityGameTests` (3): drift, turn, anchor and save; right-click ignition ends after 48 ticks and one hit breaks the ship; the spawn command.
  - `StarshipDriftTest` (4).
  - A new case in `test_rd_generate.py`.
  - `GeoModelLoadTest` already loads the model and animations.
  - Mutation-checked: no countdown, no turning, the command spawning at the source, and a doubled radius each fail a test.
- **Checked in a real client** (with the performance mods):
  - the ship renders textured at Ship scale inside its 8.5x5 hitbox;
  - it drifts and turns;
  - a right-click within reach starts `ignite_prep` on both sides.
- The Drone's reach is short, so get within a few blocks of the hitbox to click it.

### Placeholder
- The whole entity is for testing. Its item uses vanilla's spawn egg model (brass and cyan tint). It plays no sounds.

## RD art pass 1: done
- The owner generated and picked art with Retro Diffusion (`tools/art/rd_generate.py`) and promoted it into the mod:
  - 40 item icons (the fluid buckets, the nebula materials and chain intermediates, the Mechanical Drill and Matter Harvester, the Dyson payloads, RCS Thruster, Reaction Wheel Gyroscope, Atmospheric Catalyst Core, ...);
  - 8 block textures (the four asteroid rocks, Electrolyzer and Spinner side and end);
  - the first RD module model, `core_mk1`: `geo/module/core_mk1.geo.json` and `textures/entity/body/core_mk1.png` (the model is not drawn until 5b).
- 48 of the 51 placeholder rows are replaced. The promoted files are listed in `docs/art/promoted.txt`, and `ASSETS.md` and `assets.csv` show them as `RD art (promoted)`.
- Checks: all 48 textures are 16x16; `GeoModelLoadTest` loads `core_mk1` through GeckoLib. **Not yet seen in a real client.**
- The Retro Diffusion tooling around it: a picker page (`art/rd/index.html`: pick variants, keep or redo, notes, and the commands to paste; one self-contained file), `--promote` recording into `promoted.txt`, `--promote-as` honouring `--scale-like`, and the courier-branch workflow in `tools/art/README.md`.

### Still placeholders
- Items: `metallic_grains`, `silicon_carbide_coating`, `micro_alloy_sheet`; the empty accent mask `core_mk1_accent.png` (paint it in Blockbench).
- Everything the Art drop 1 section lists as "still our placeholders" is replaced except these.
- Colour notes for the owner to judge: `asteroid_silicate` is orange (style guide: sand-tan); `carbon_flakes` reads as a dagger; `dyson_shell_truss_module` reads as a hooked tool.

## CI savings: done
- `build.yml`: docs/art-only PRs skip CI; drafts run build + unit tests only; client-only/test/tools/docs/asset PRs skip GameTests, datagen and Aeronautics; PR runs read the Gradle cache and only `main` saves it. Pushes to `main` always run everything. Untested until the PR itself runs. If `main` has required checks, skipped jobs count as passing but a fully skipped workflow (docs-only) does not.
## Config screen: done
- The client and common settings (`ClientConfig`, `Config`) now open from Mods > Void Reclaimer > Config, through NeoForge's `ConfigurationScreen`. Labels and sections use the automatic names and the config comments as tooltips; no custom lang keys yet. Not yet opened in a real client. Recipe toggles still need a restart or `/reload`.
## Body minimum brightness: done
- `BodyLight.atLeast` (pure, `BodyLightTest`) lifts the light the body rig is drawn with to client config `body.minLight` (default 8). Not yet compared in a real client.

## Everything obtainable (test): done
- `ObtainabilityGameTests`: all 61 section and module items are made through the Replicator and installed. It found no missing recipe, unreachable milestone or unaffordable price. Mutation-checked: refusing cosmetic installs fails it and names the two cosmetic modules. Reachability uses a fully satisfied milestone context, not a play-through.

## Cloud Siphon block: done
- `cloud_siphon` block: shaft-driven, in a dense cloud (min density 0.4) it makes the cloud kind's raw item, 0.5 per second at full density and 64 RPM; 9 extract-only output slots; `CollectorPowerEvent` lets an addon supply the power. `dust.RawItems` maps kinds to raw items. Config `machines.siphon*` and the `cloudSiphon` toggle; Replicator Blocks tab.
- Tests: `CloudSiphonMathTest` (4 unit), `CloudSiphonGameTests` (5: output at the expected rate, extract-only, nothing below minimum speed, in empty space or in the thin fringe of a cloud, addon power without rotation, full output pauses). Mutation-checked: dropping the density minimum fails the fringe test, and not posting the event fails the addon test.
- Placeholders: generated block textures `cloud_siphon_side/end.png`. Static model (no rotation animation, like the other machines). Not seen in a real client.
- The module's own raw-item map (`ModItems.CLOUD_COLLECTOR`) and `RawItems` hold the same four pairs; unify them if a third user appears.

## Cloud Collector module: done
- `cloud_collector` module (tool mount) with a data-driven `collector` spec; `dust.Collectors` makes the cloud kind's raw item from dust gathered (yield 1 per dust), uses 1 energy/s, stops in low power, overflows to the output buffer. Config `collector`. Replicator recipe in Modules (Diversified).
- Tests: `CollectorsTest` (5 unit), `CollectorGameTests` (4: yield, no module no items, energy and low power, buffer overflow). Mutation-checked: zero yield and ignoring low power fail them. `ObtainabilityGameTests` covers the new module (62 parts) and now visits chassis in a fixed order (the map order differed between runs, and a Ship-first order hit a legitimate inventory-full refusal).
- Placeholder icon `cloud_collector.png` (generated). Not seen in a real client.
- The block version is the Cloud Siphon (above).

## Fix: energy bar icons: done
- Playtest note "The energy bar icons are not working correctly": the four sprite files are strips of ten battery cells (90x9, as delivered), and `EnergyBarLayer` drew each whole strip into one 9x9 cell, so every cell showed a ten-cell comb. It now draws one cell (the strip's leftmost 9x9) with the sub-region `blitSprite`; the art files are unchanged.
- Checked in a real headless client: at 60% energy the bar shows ten cells (full, half and empty), and the flashing warning cells use the same path. No automated test (pure drawing).

## Fix: punch-break in zero-G: done
- Playtest note "I should be able to just punch and destroy the anchor and scaffold": nothing blocked it, but vanilla's airborne divisor (1/5) applied all the time in zero-G, so a Scaffold took about 6 s and an Anchor about 11 s by hand. `ZeroGMining` undoes the divisor where there is gravity none. Tests: `ZeroGMiningGameTests` (zero-G speed is at least 1.0; airborne where there is gravity still 0.2). Mutation-checked: without the handler the zero-G speed was 0.2 and the test failed.

## Playtest round 1 fixes (2026-10-30): done
- **GameTest filter:** `./gradlew runGameTestServer -PgametestFilter=<terms>` runs only tests whose class or method name matches (`GameTestRegistryMixin`, `GameTestFilter`, test-only). One class takes about 30 s instead of about 3 minutes.
- **Drone bobbing against a floor:** touching a surface fed vanilla's walking view-bob; `ThrusterFlight.calmWalkBob` zeroes it every tick. Cause read from vanilla code, not yet confirmed by eye.
- **Mech animation:** a steady cruise under thrust keeps the thrust pose (`BodyLook.pose` has a `thrusting` input). The owner's re-rigged Mech is imported (flames and tool/watch pieces parented by `--promote-as`, new `idle_ambient` and `idle_impatient`; idle plays `idle_ambient`).
- **Velocity lean:** rig spec `lean` (`BodyLean`); the Mech leans 22 pitch / 16 roll degrees into its motion. Angles are untested by eye.
- **Camera:** bodies above the Mech sit further back in third person (Ship and Shuttle about 40 blocks, Titan about 160). Whether the Ship and Shuttle should also shrink waits until the owner has tried it.
- **Health bar:** one bar replaces the hearts, growing with max health (`HealthBarLayer`, `HealthBarMath`).
- **Art tools:** `rig_check.py` checks an RD model against a rig spec and writes Re-rig text under 250 characters; `--promote-as` re-parents lost `fx_*` groups; `rd_generate.py` writes `rerig.txt` beside each downloaded body-rig model.

## Start of the game: done
- Starter dust (M25 S30 C5) on the first Awakening (`StarterDust`, config `starter`), the Awakening log tells the player to press R for a Dust Sensor, and the directional hum works without a sensor (tone only). Tests: `StarterDustGameTests`. Dust milestones now count dust gathered (`DustLedger`, `dust_collected` attachment), not dust held, so the gift does not unlock First Dust or Diversified. Not seen in game yet: the toast wording and the bare hum's tone.

## Dust HUD: done
- Dust kinds have an optional `icon`; the meter, the Replicator prices and the sensor title draw it (`DustIcons`), the meter dims when idle (`DustMeterFade`), names show while a screen is open, price icons have a hover tooltip. **Placeholder art:** `textures/gui/sprites/hud/dust_{metallic,silicate,ice,carbon}.png` (9x9 programmer art) until the owner's RD icons. Not seen in game yet.

## Replicator screen, part 1: descriptions: done
- A description line per built-in recipe (81), an optional `description` field on Replicator recipes (synced; the sync codec is hand-written because `StreamCodec.composite` stops at six fields), a description box under the list and a hover tooltip on rows; the panel is taller (236). Not seen in game yet.

## Replicator screen, part 3: JEI: done
- JEI gets the screen's bounds and the item under the mouse (`registerGuiHandlers`, `ReplicatorScreen.itemAt`). Checked only that JEI loads the plugin without errors in a headless client; the hover keys are not tried by hand yet.
## Replicator screen, part 2: queue reorder: done
- Drag a waiting job to a new place (`MOVE` action, `ReplicatorLogic.reorder`); the running job stays first. Unit tests and a GameTest. Not seen in game yet.

## Body inventory screen (parts 1 and 2): done
- E opens the body inventory menu and screen (slots only, tab strip to the Replicator, Body and Logs); locked rows darkened. GameTests for the menu, slot rules and shift-click. Part 2 adds the 3D body preview and the dust reserves beside the slots.
## Respawn screen: done
- Card panel for the spawn and charged cores (name, charge pips, distance), plus a placeholder AI line on death. Not seen in game (needs a death in a world with a Backup Core).
## Body inventory screen (part 1): done
- E opens the body inventory menu and screen (slots only, tab strip to the Replicator, Body and Logs); locked rows darkened. GameTests for the menu, slot rules and shift-click. Not seen in game; the body preview and dust display are the next PR.

## Orbit camera: done
- Hold the orbit key (middle mouse) in third person to look round the body, scroll to zoom, release to ease back. Uses `ViewportEvent.ComputeCameraAngles` (no mixin). The feel of the orbit is not tried by hand.

## Photo mode: done
- F8 opens a free camera with the HUD hidden (drag to look, WASD/Space/Shift to move, scroll FOV, Q/E roll, Enter screenshot). `CameraPhotoMixin` is a small client-only mixin. Checked in a headless client (HUD hides and returns, camera moves, no mixin errors); the controls' feel is not tried by hand.

## First person in big ships (5b-2a, first step): done
- Ship, Shuttle and Titan chassis have a `cockpit`: first person has no hand and the camera sits forward of the eyes. The offsets come from the rigs' bridge and windshield pivots; not checked by eye. Still to do: the Drone and Mech first-person pieces (art), and a camera that follows the ship's roll when 6-DOF exists.
## Machine recipe check (GameTest): done
- `CreateRecipeGameTests`: every Create chain, Sequenced Assembly and freezing recipe loads and names real items and fluids (a typo in a recipe fails it). The screenshot tool that opens each recipe in JEI in the headless client is `tools/client-test/jei_shots.py` (1920x1080 client).

## Layered animation poses: done
- Rig specs may have `layers` (up to 3, played by extra GeckoLib controllers over the main one). The Mech swings only its right arm for tool work (`swing_tool_arms`, made with `anim_slice.py`) while the body keeps flying. Unit tests check every layer animation exists. Not seen in game (a swing needs a tool module and a world with cheats).

## Handoff notes, round 2: done
- HANDOFF.md lists what is done, what to try in game and what comes next. `tools/client-test/xt.py` gained `chord ctrl+f`. The JEI recipe screenshot tool is built afterwards (`jei_shots.py`, needs a 1920x1080 client).

## Act III slice, part 1: the ruined_earth dimension: done
- `ruined_earth` (datapack dimension, dimension type and biome; `earth/RuinedEarth` keys): a 1024 x 1024 planet (the 4 x 4 sectors, x and z -512..511) of low hills and craters on a flat landing area at the origin. Normal gravity, so `ZeroGravity.isZeroG` is false there. It is not locked; reach it with `/execute in void_reclaimer:ruined_earth run tp @s 0 100 0` for now (the Atmospheric Portal comes in a later PR).
- `RuinedTerrain` (pure, unit-tested): `surfaceY(seed, x, z)` from value noise plus a jittered crater grid. `RuinedEarthChunkGenerator` (a `FlatLevelSource` subclass like the asteroid one, registered as `void_reclaimer:ruined_earth`) fills stone under 3 layers of **Ashen Slag** (new block) and bedrock at the floor. Everything outside the planet square is void.
- Vanilla 1.21.1 has one world border shared by all dimensions, so the planet edge is void rather than a border. An edge push-back (like `VoidBoundary`) is not built yet.
- The dimension type still uses the vanilla `minecraft:overworld` effects; the three-era sky replaces them in the next PR.
- Tests: `RuinedTerrainTest` (4), `RuinedEarthGameTests` (3: generator wired, terrain on a fresh proto chunk, gravity). The terrain test fills a fresh `ProtoChunk` because the persistent GameTest world returns chunks generated by older runs.

### Placeholder assets
- `textures/block/ashen_slag.png` (16x16 programmer art).

## Act III slice, part 2: the three-era sky: done
- `SkyEra` (ruined, healing, restored) is a saved, synced attachment on the `ruined_earth` level (`ModAttachments.EARTH_SKY_ERA`, like the Dyson counters). `EarthSky.get/set` is the server side; `/void_reclaimer sky era get|set <era>` (op) is the debug command. One era for the whole dimension; blending by restoration percent comes later.
- `SkyLook` (pure, unit-tested) holds each era's colors, fog distance, sun tint and clouds. Ruined: brown smog, 90-block fog, big dim red sun, no clouds. Healing: thinning haze, 220-block fog, pale sun, clouds. Restored: clear blue, 512-block fog, white sun, clouds. All numbers are guesses to tune by eye.
- Client: `RuinedEarthEffects` (a `DimensionSpecialEffects` registered as `void_reclaimer:ruined_earth`, used by the dimension type) draws a gradient dome and a tinted sun through the `renderSky` hook; `RuinedEarthFog` sets fog color and distance from `ViewportEvent`. No chunk rendering is touched (Sodium-safe), no raw Tesselator HUD drawing.
- Tests: `SkyEraTest` (4 unit tests), one GameTest that runs the real command. Mutation-checked.
- **Seen in the headless client (2026-10-30):** all three eras render and look clearly different (brown smog, beige haze, pale blue); the horizon reads flat and the zenith gradient and the sun were not looked at. Every number is still a guess for the owner to tune.

## Act III slice, part 3a: Atmospheric Portal frame and lighting: done
- **Atmospheric Portal Frame** (`portal_frame`) and **Portal Igniter** (`portal_igniter`): Replicator recipes gated by the `starship` milestone (placeholder prices: 8 frames for 40/32/24/16 dust, an igniter for 12/10/8/12).
- `PortalShape` (pure, 7 unit tests): finds the closed flat rectangle of frame blocks around an empty interior, either orientation (width along X or Z). Corners are not needed. Interior 3 x 3 up to 32 x 32, so the Ship (about 12 x 5.6) fits.
- Right-clicking any frame block with the Igniter (`AtmosphericPortals.tryLight`) fills the interior with `atmospheric_portal` panes (thin, no collision, light 11, translucent) and uses the igniter up (not in creative). Breaking any frame block removes the whole portal (`updateShape`, like the Nether portal).
- **Travel is not built yet** (part 3b: entering the pane, the `Portal` interface, the return gate on ruined Earth and the saved link between them). Lit portals are inert for now.
- Tests: `PortalShapeTest` (7), `PortalGameTests` (3: Ship-sized frame lights and breaks together, a gap does not light, the igniter lights and is used up). Mutation-checked.

### Placeholder assets
- `textures/block/portal_frame.png`, `textures/block/atmospheric_portal.png`, `textures/item/portal_igniter.png` (16x16 programmer art, no animation on the pane yet).

## Act III slice, part 3b: portal travel and the return gate: done
- A lit Atmospheric Portal now sends anything that walks or flies into it (it implements vanilla's `Portal`; 5 ticks inside) to **ruined Earth**, and the way back to where the trip began. No screen overlay.
- `PortalTravel`: from any portal outside ruined Earth you arrive at the **return gate**, built on first use on the landing area (centered on x = 0, z = -10, one block above the ground, a level floor under it, at least 12 x 6 so the Ship can always come home; larger if the origin portal is larger). You stand a few blocks off its plane, facing away, on the side you were travelling towards.
- `PortalLinks` (saved on the ruined_earth level, not synced) remembers the gate and the origin portal's floor point and side, so the gate leads back out of the portal the trip began at (any dimension), or to the deep space spawn if none is known. The latest origin wins when several portals are used.
- Note (vanilla): standing inside a portal during its cooldown keeps the cooldown going, so arrivals are placed off the plane.
- Gravity follows the dimension on arrival (checked: on in ruined Earth, off in deep space).
- Tests: `PortalShapeTest` and `PortalLinksTest` (unit), `PortalGameTests.aPlayerTravelsToTheReturnGateAndBack` (a real survival player, a Ship-sized portal, both ways). Mutation-checked: ignoring the saved exit fails it.
- Not built: portal sounds of our own (vanilla's portal sound stands in), flying a body through at speed (only a walking-pace player is tested), a pane animation.

## Act III slice, part 4: the Terraforming Spire and Genesis Pulse: done
- **Terraforming Spire** (`terraforming_spire`, one block for now; the multiblock and its inputs come later): right-click, or a redstone pulse, fires the **Genesis Pulse**, on ruined Earth only (anywhere else it says so). Replicator recipe behind `starship` (placeholder price).
- The pulse is a square wave: rings sweep out from the Spire over `terraform.pulseTicks` (default 300 = 15 s) to `terraform.spireRadius` (default 128, so 16 Spires tile the 4 x 4 sectors). Each ring's columns get their top 5 blocks rewritten as it passes (`Terraforming.swapColumn`), with particles along the ring; when it ends, the whole area's biome is rewritten and its chunks resent the way `/fillbiome` does (`chunkMap.resendBiomesForChunks`), so Sodium rebuilds them from the normal packets. A pulse in progress is saved with the block and continues after a reload.
- Data-driven: the synced-nowhere datapack registry `void_reclaimer:terraform_profile` (`api.terraform.TerraformProfile`: a list of block swaps, each with an optional surface state, and a biome). The Spire uses `void_reclaimer:restored`: Ashen Slag becomes dirt, grass block at the surface, and the area becomes plains. A datapack can replace it.
- Pure logic: `PulseSchedule` (the wave), unit-tested. Tests: `PulseScheduleTest` (3), `SpireGameTests` (3: a real wave on a floating slag platform, caught part way and at the end, blocks and biomes inside and outside the area; refuses in deep space; a running pulse survives a save and load). Mutation-checked both ways (no block rewrite, no biome rewrite). The test resets biomes on its own, not through the code under test, because the GameTest world persists.
- **Not verified by a test:** that the chunk resend packets reach clients (`lastChunksResent` only counts the chunks handed to the resend call). Look at it in game: the grass colour should change when the wave finishes.
- Not built (later Act III phases): the multiblock, Purified Water and Bio-Nutrient Slurry inputs, the Atmospheric Catalyst Core per sector, per-sector restoration tracking, driving the sky era from the restoration percent.

### Placeholder assets
- `textures/block/terraforming_spire.png` (16x16 programmer art; a plain cube, no model).

## Hover flight on Earth, part 1: the physics: done
- `FlightPhysics.step(..., weight)` adds gravity to thruster flight (pure, no caller yet, so no behaviour change): with the dampener on, thrust cancels weight first (`weightSupport`, up to all of it) and only the remainder steers; a body too weak to cover its weight falls at the unsupported weight (partial lift) and does not steer; dampener off or no power means full weight. `hoverShare` is the share of thrust spent on weight, which is what hovering will cost in energy. With weight 0 it is exactly the old step (tested).
- Config `flight.hoverWeightFactor` (default 0.12, see "Hover tuning" below): the weight the thrusters must cancel is the body's gravity times this (vanilla 0.08).
- Tests: 7 new `FlightPhysicsTest` cases; mutation-checked (weight support forced to 0 fails four of them).
- Next: part 2 (always-thruster bodies on Earth: `ThrusterFlight`, server energy, HUD, GameTests), part 3 (Mech walking).

## Hover flight on Earth, part 2: always-thruster bodies on ruined Earth: done
- On ruined Earth every body with thrust now flies on thrusters, like in deep space but with weight (`ThrusterFlight.isActive`, `canHover`, `weight`, and the weight passed into `FlightPhysics.step`). Creative keeps vanilla flight. Zero-G flight is unchanged. The Mech hovers too until part 3 gives it legs.
- Energy (`ServerFlightHandler`): holding altitude costs the usual thrust cost times `hoverShare` (the share of thrust spent on weight) times the body's energy multiplier, on top of braking; thrusting costs the full thrust cost as before. Hovering also wears the thruster sections.
- HUD (`FlightHudLayer`): HOVER, TOO HEAVY: SINKING (thrust below weight) and FALLING: DAMPENER OFF, next to the speed.
- Tests: `HoverGameTests` (3): bodies fly with the right weight and creative does not; the hover cost equals thrust cost x share x energy multiplier (measured against the same minute with the dampener off); controlled landings are safe and free falls hurt. Mutation-checked: no hover charge and no landing rule each fail. The test player is made weightless because the server simulates vanilla gravity on fake players, which would charge braking energy for the fall.
- **Decisions pending (taken unattended 2026-10-30; the owner can overrule):**
  - **Controlled landings take no fall damage.** A body under power with the dampener on lands under control however far it came down (`LivingFallEvent` is cancelled), because vanilla's distance-based damage would hurt every normal hover descent. Free falls (low power, or the dampener off) hurt as in vanilla, which keeps the owner's "vanilla fall damage" for uncontrolled falls.
  - **A body with no thrust walks** like a normal player on Earth (`canHover` is false), instead of being stuck.
  - **The starter numbers:** a test body with the fallback stats (mass 1.0, thrust 0.03) is below the weight 0.048 (0.08 x 0.6) and shows TOO HEAVY; a real Drone's thrust is about 0.075. `flight.hoverWeightFactor` is the knob; look at the real Drone, Mech and Ship before the owner tunes it.
- Not seen in a real client yet (physics runs client-side): hovering, the HUD text, landing.

## Hover flight on Earth, part 3: the Mech walks: done
- `Chassis.walks` (datapack field, optional, default false; `Chassis.withWalks`; the old five-argument constructor stays for addons) marks bodies with legs. Only the built-in Mech has it (`mech.json` gained `"walks": true`). `Bodies.walks(entity)` reads it. It is on `Chassis`, not `BodyStats`, so the `BodyStatsEvent` extension API is untouched.
- On ruined Earth a walking body moves like a normal player (vanilla movement, jump, fall damage) until Space engages its thrusters; then it flies with the same hover model as every other body. Engaging is the new `FlightInput` flag `engaged` (client controller: Space while walking and able to fly; sent with the input payload). It disengages on the ground with no up-thrust held (`FlightInput.shouldDisengage`: after letting go of Space, or on landing). In zero-G a walking body always flies, as before. Drone, Ship and the rest fly all the time.
- Tests: `FlightInputTest` (engaged is a flag and not a key, survives the codec, the disengage rule), `ChassisCockpitTest` (only the Mech walks; the flag round-trips and is omitted when false), `HoverGameTests` (a Mech walks until engaged, Drone and Ship always fly, a Mech always flies in zero-G). Mutation-checked: `walks` forced false fails the Mech test.
- **Not tested by a GameTest, not seen in a client:** the client controller (engaging on Space, disengaging on landing) is client-only, like all input. **Decision pending:** engaging needs Space pressed on the ground, and a Mech hovering still (no keys) stays engaged until it lands; a Mech in creative with thrusters off just walks.

## Hover tuning: the weight factor default, from measured presets: done
- **Found by looking at it in the headless client:** with the first default (0.6, weight 0.048) the HUD showed TOO HEAVY: SINKING for the Drone and the Ship presets. My estimate of a starter body's acceleration (about 0.075) had used the wrong mass. Measured thrust over mass of the presets (`full` = every socket filled): Drone 0.035 / 0.015, Mech 0.035 / 0.023, Ship 0.016 / 0.014, Shuttle 0.015 / 0.014, Titan 0.0044 / 0.0044 blocks/tick². Vanilla gravity is 0.08.
- **Decision taken unattended:** the default is now **0.12** (weight 0.0096): every player body hovers with a little thrust left to steer (the full Drone spends about 63% of its thrust holding up, the Ship about 67%, the Mech about 42%), and the Titan stays too heavy to lift on Earth, which fits a 67-block ship. The owner can change `flight.hoverWeightFactor`; a bigger number means a heavier planet.
- Steering is gentle by design (weight comes first): the full Ship has about 0.0046 blocks/tick² left, so it reaches 10 m/s in about 11 s. If that feels too slow, lower the factor (0.08 gives the Ship twice the spare); if bodies should struggle more, raise it.
- Test: `HoverGameTests.everyPlayerBodyCanHoverAtTheDefaultWeight` ties the real Drone, Mech, Ship and Shuttle presets (empty and full) to the config default, so a future stats change that breaks hovering fails the build.
- Also seen in the real client (headless, with every candidate addon loaded): the hover HUD and `TOO HEAVY: SINKING` text work on ruined Earth; a Mech walks with the vanilla hand until its thrusters engage; a quick tap of Space engages it and it drops straight back to walking, because it is on the ground with no up-thrust held (hold Space to take off).

## Addon compatibility spike: results (2026-10-30)
Done on a never-merged branch (`spike-addon-compat` in a separate worktree; `-PspikeMods=` picked a subset of the candidates as `localRuntime` + `testRuntimeOnly`). **No addon was made required**: the owner decides the required set. Everything below was run against Create 6.0.10, Aeronautics 1.3.2 and Sable 2.0.6 on NeoForge 21.1.265, with Sodium, ModernFix, FerriteCore and ImmediatelyFast.

| Addon (version) | Alone: loads as a mod, our GameTests pass | Notes |
|---|---|---|
| Create Crafts & Additions 1.7.2 (beta) | yes | no log problems |
| Create: New Age 1.2.0 | yes | 25 recipe parse errors in the log (`neoforge:never` condition with an empty body in `pressing/` and `sequenced_assembly/` recipes), not crashes; the recipes are skipped |
| CC: Tweaked 1.120.2 (alpha) | yes | **use the NeoForge file**: Modrinth's maven resolved the Fabric jar for the version *number*, which NeoForge silently ignores (my first CC run "passed" without loading it); pin it by version id `1ewzHZYg` |
| Create: Avionics 0.7.0 (alpha) | yes, with CC: Tweaked | fails without it (`computercraft` >= 1.115 is a hard dependency) |
| Create Propulsion: Simulated 1.1.5 | yes | harmless warnings about fuel overrides for fluids of mods that are not present (tfmg, northstar, mekanism...) |
| Create: Aeroworks 1.5.0 | yes | no log problems |
| Steam 'n' Rails 0.3.0-beta.2 | yes | 276 loot-table parse errors for TerraFirmaCraft track variants (their items do not exist without TFC); harmless |
| Crafts & Additions bridges for Aeronautics and Sable | yes, with Crafts & Additions | no log problems |

- **All nine together:** the whole suite passes (150 GameTests at the time), `runData` succeeds and writes no diff, and the client starts, creates a world, and plays on ruined Earth with no crash and no mixin failure from our mod. Loading everything adds only the noise above. Exit code 124 on the full run is the known GameTest JVM that does not exit, not a failure.
- **Not tested** (so not proven): any actual feature interplay. Nothing here checks that Propulsion thrusters work in deep space under our Sable setup, that Crafts & Additions power can feed our machines, that Avionics sees our blocks, or that Steam 'n' Rails tracks work in the void. Those are the next integration steps, one small PR each, and each needs its own look in a client.
- **Maturity:** Crafts & Additions and Steam 'n' Rails are betas, CC: Tweaked and Avionics are alphas. Required addons should probably be the releases (New Age, plus Propulsion and Aeroworks if the ship track wants them) and Crafts & Additions on the strength of its download count; CC: Tweaked and Avionics are a risk while alpha. This is a recommendation, not a decision.
- **Pitfall for later:** Modrinth maven coordinates resolve by version number across loaders; when an addon has several loaders, use the version id (check the jar for `META-INF/neoforge.mods.toml`).

### Round 2 (2026-10-30): the owner's two CurseForge addons and the themed shortlist
Same method; every Modrinth entry now pinned by version id, the two CurseForge mods through `https://cursemaven.com` (`curse.maven:<slug>-<projectId>:<fileId>`).

| Addon (version, source) | Loads | Notes |
|---|---|---|
| Create: Advanced Electricity / Technology Addon (1.21.1 1.0.0, CurseForge 1743391 file 9362753; mod id `light_bulb_addon_create_crafts_and__addions`) | yes | clean; needs Crafts & Additions; uploaded 2026-10-29, under 900 downloads |
| Create: Advanced Computer Technic (1.21.1 1.0.2, CurseForge 1759784 file 9362807; mod id `advanced_computer_technic`) | yes | clean; needs Crafts & Additions and Advanced Electricity (runs on its 1.0.0 although the page asks for 1.0.5); All Rights Reserved; under 500 downloads |
| Create: The Factory Must Grow 1.2.0 | yes | clean |
| Create Ore Excavation 1.6.8 | yes | clean |
| Create: Power Loader 2.0.5 | yes | clean |
| Create: Hypertubes 0.6.0 | yes | clean |
| Create: Tweaked Controllers 1.2.7 | yes | clean |
| Create: Trading Floor 3.0.16 | yes | clean |
| Create: Numismatics 1.1.0 | yes | clean |
| Create: Electro Energetics 1.1.4 | yes | clean |
| Create: Connected 1.3.3 | yes | 16 loot-table errors for optional Dye Depot blocks; harmless |
| Create: Copycats+ 3.0.9 | yes | Sable ships physics data for `copycats:copycat_catwalk`, which this version does not register; that block would lack ship physics |
| Create Deco 2.1.3 | yes | the placard recipe fails to parse |
| Create: Power Grid 0.6.2 (+ Architectury 13.0.11) | alone yes | native fast solver fails to load (Java fallback); **cannot be installed with TFMG**: its TFMG integration needs TFMG 1.3.x and the newest is 1.2.0, so the game does not start |
| Create: Radars 5.0.3 | **no** | hard-requires Create Big Cannons (weapons), not listed on Modrinth |

- **Everything together** (the first nine plus round 2, without Power Grid and Radars: 22 addons): the game starts and the whole suite passes (151 GameTests on the spike branch); the only log errors are the known harmless ones above (323 lines).
- Still untested: any real feature interplay.


## Electricity bridge: the Cloud Siphon takes Forge Energy: done
- The Cloud Siphon now has a **Forge Energy** input (`Capabilities.EnergyStorage.BLOCK`, receive only; nothing can be taken out). It is the standard NeoForge capability, so Crafts & Additions, Create: New Age and any other energy mod can power it, with no dependency on them.
- Energy **tops up the shaft to full power** while the siphon is actually working (in a cloud at or above the minimum density): `CloudSiphonMath.topUp`. A siphon with no shaft runs on electricity alone at full power for `machines.siphonFePerTick` FE per tick (default 40); a slow shaft only takes the gap; a shaft already at full power or more takes nothing; an idle siphon takes nothing. The buffer is `machines.siphonFeBuffer` (default 4000, 100 ticks of full power) and is saved with the block. `siphonFePerTick = 0` turns the input off. It sits alongside `CollectorPowerEvent`, which stays the hook for addons that want to decide power themselves.
- Power is sampled once a second (as before), so the draw follows a 1-second-old sample.
- Tests: `CloudSiphonEnergyTest` (5 unit tests), three new `CloudSiphonGameTests` (electricity alone makes items and the draw matches the feed, the buffer is bounded and nothing comes back out, a full shaft and an idle siphon draw nothing). Mutation-checked: energy ignored fails the GameTest; draining at full shaft power fails the unit test (the GameTest alone cannot see it, because at exactly power 1.0 the draw is zero either way).
- **Test flake fixed on the way (root cause):** all `CloudSiphonGameTests` had `timeoutTicks = 100`, but `inLoadedArea` waits up to 5 + 100 ticks for the chunk to tick, so any run where the chunk was slow timed out (they failed even on clean `main` when run on their own). They now have 300.
- **Decisions pending (unattended):** the numbers (40 FE per tick, 4000 buffer) are guesses; only the Cloud Siphon takes energy for now; the other machines are Create stress machines, and an electricity answer for them (an electric motor, or Crafts & Additions' own) is a separate step. Whether our body's energy should charge from FE (a charger block) is open.

## Required addons: done
- Five Create addons are now required (owner's choice after the spike): Create Crafts & Additions 1.7.2, Create: Advanced Electricity 1.0.0 and Create: Advanced Computer Technic 1.0.2 (both CurseForge, through `cursemaven.com`), Create Propulsion: Simulated 1.1.5, Create: The Factory Must Grow 1.2.0. Declared like Create and Aeronautics (`gradle.properties`, `localRuntime` + `testRuntimeOnly`, `required` in `neoforge.mods.toml`). See `CLAUDE.md` "Required addons" for the policy, the optional list and the risks.
- The unit-test JVM now gets 2 GB (`maxHeapSize`): with the extra mods it ran out of memory booting the mod at Gradle's default 512 MB.
- Checked: 230 unit tests, 154 GameTests, the Aeronautics GameTests and `runData` (no diff) all pass with the five loaded. Nothing in our code uses them yet.

## CC: Tweaked peripherals, part 1: foundation and factory machines: done
- CC: Tweaked is an optional integration like JEI: `compileOnly` against its API (`maven.squiddev.cc`, 1.120.2), the full mod on `localRuntime`, `optional` in `neoforge.mods.toml`. `compat/cc/CCCompat` registers our generic sources at common setup only when `computercraft` is loaded.
- `MachinePeripheral`: Cloud Siphon (`getDensity`, `getPower`, `getSpeed`, `getEnergy`, `getEnergyCapacity`, `getItems`), Electrolyzer (`getProgress`, `getSpeed`, `getTanks`), Spinner (`getProgress`, `getSpeed`, `getItems`). Read-only. `ElectrolyzerBlockEntity` and `SpinnerBlockEntity` gained `progress()` (share of the cycle, 0 to 1). Lua reference: `docs/CC_PERIPHERALS.md`.
- Standing rule (owner): every new block or item with state or actions gets a peripheral in the same PR (`CLAUDE.md` "CC: Tweaked peripherals").
- Tests: `CCPeripheralGameTests` (2): CC is loaded and our sources registered; CC's own generic peripheral provider (what a computer uses, reached by reflection because it is internal) offers our functions on each machine, and they return the machines' real state. Mutation-checked: removing one `@LuaFunction` fails it.
- Not seen with a real computer in a client yet.

## RD tools, part 1: fixing existing art (`--fix`): done
- `tools/art/rd_edit.py`, used through `rd_generate.py --fix ASSET[=N] --tool ...`: RD canvas edit tools (`palette` onto the style-guide palette, `reduce`, `downscale`, `unbg`, `correct`) and the Pixel Fixer (`pixel`, `pixel-neural`) on the mod's file or a staged variant. The result becomes the next variant, shows on the contact sheet and is promoted as usual. Same spending safety as generation; edit tools run as RD tasks keyed by a `custom_id`, so repeats are free; Pixel Fixer results are remembered in the state file. See `tools/art/README.md` "Fixing existing art".
- Tests: 5 new offline tests in `test_rd_generate.py` against the fake RD server (now with the edit-tool estimate, task and Pixel Fixer routes); 34 pass. Mutation-checked: the wrong palette and forgetting finished fixes each fail.
- Not run against the real RD service yet (needs the owner's key): first real use is `--fix asteroid_silicate --tool palette`.
- Next (agreed): part 2 repaint (`image_edit`, inpainting, outpainting, seam tiling), part 3 restyle placeholders (img2img, tile variation), part 4 low-poly revise and export. Low-poly animate and Blockbench export already exist.

## CC: Tweaked peripherals, part 2: the Dyson launch chain: done
- `DysonPeripheral`: Kinetic Drive Base (`isAssembled`, `getProgress`, `getLaunches`, `getSpeed`, `hasPayload`, `getSphere`), Payload Chamber (`getItems`), Quantum Receiver (`getSpeed`, `getStressCapacity`, `getSphere`); `getSphere` gives the Dyson counters, sky tier, shell progress and sunlight. Lua reference in `docs/CC_PERIPHERALS.md`.
- **Read-only, a change from the plan:** the plan listed a `launch()` function, but the Mass Driver fires on its own once wound up and a player has no way to make it launch, so by the owner's rule (computers do only what players can) there is no control function.
- Tests: `CCPeripheralGameTests.theDysonLaunchChainIsAPeripheral` (CC's own provider offers the functions on a real base, chamber and receiver; values match the blocks and the sphere). Mutation-checked: `hasPayload` forced false fails it.
## Fix: flaky Test Starship spawn GameTest (root cause): done
- `StarshipEntityGameTests.spawnCommandPutsOneShipAheadOfTheSource` failed now and then with "found 2" or "found 0". Two test bugs: old ships were discarded in `prepare()`, before the forced chunks had loaded their entities, so a ship an earlier run left in the persistent GameTest world reappeared and was counted; and the new ship was searched for within 0.5 blocks of its anchor, while a spawned ship sits at its drift position, up to the drift radius away. Old ships are now cleared once the chunks tick entities (in `whenTicking`), and ships are matched by their anchor within the drift radius. The game code was fine.

## CC: Tweaked peripherals, part 3: the Terraforming Spire: done
- `SpirePeripheral`: `isRunning`, `getProgress` (0 to 1), `getPulses`, `getRadius`, and `pulse()`, which goes through the same `SpireBlockEntity.tryStart()` as a right-click or redstone pulse (ruined Earth only, one pulse at a time) and returns `true`, or `false` and `"busy"`, `"wrong_world"` or `"no_profile"`. `SpireBlockEntity` gained `progress()` and `radius()`.
- Tests: `CCPeripheralGameTests.theSpireIsAPeripheralAndPulsesUnderThePlayersRules` (CC offers the functions; `pulse()` is refused in deep space, starts on ruined Earth, and is refused while busy; the block is removed in the same tick so the persistent test world is not terraformed). Mutation-checked: a `pulse()` that skips the player's rules fails it.
## RD tools, part 2: repainting (`--fix --tool edit|inpaint|outpaint|seam`): done
- `rd_edit.py` gains RD's generative edit tools through the same `--fix` command: `edit` (`image_edit`, needs `--prompt`), `inpaint` (`inpainting`, needs `--prompt` and `--mask`), `outpaint` (`--expand L,R,T,B`) and `seam` (`seam_tiling`, `--axes`), with `--seed`. Results are downloaded from RD's URLs and become variants. The mask is checked before anything is paid (size, and not opaque everywhere). See `tools/art/README.md` "Repainting part of an image".
- Tests: 3 new offline tests (37 pass). Mutation-checked: dropping the opaque-mask guard, and not sending the mask, each fail.
- First real uses to try: `carbon_flakes` (reads as a dagger) with `edit` or `inpaint`; `ashen_slag` with `seam`.

## CC: Tweaked peripherals, part 4: spawn and travel blocks: done
- `SpawnTravelPeripherals` (read-only): Backup Core (`getName`, `getOwner`, `getCharges`, `getMaxCharges`, `getSunProgress`; a generic peripheral), Tether Anchor (`getTethered`) and Atmospheric Portal Frame (`getPortal`: nil, or `{lit, width, height, axis}`). The last two have no block entity, so they are `IPeripheral`s on CC's block capability, registered by `CCCompat.registerCapabilities` only when CC is loaded.
- Every existing block with state or actions now has a peripheral (`docs/CC_PERIPHERALS.md`). Item upgrades (turtle, pocket computer) are the next CC step.
- Tests: `CCPeripheralGameTests.theSpawnAndTravelBlocksArePeripherals` (the core through CC's provider, the anchor and frame through CC's capability with their `@LuaFunction`s; a clipped player is listed; a lone frame block, an unlit 3 x 3 frame and a lit one). Mutation-checked: without the frame's capability registration it fails.
## RD tools, part 3: restyling placeholders (`--restyle`): done
- `rd_generate.py --restyle` turns image jobs into image-to-image jobs on the asset's current texture (scaled up to the job size, `--strength`, default 0.6), so programmer art keeps its layout and takes the style; `--tile-variation` sends block textures through `rd_tile__tile_variation`. A restyle always runs (it is new work) and repeating it is not paid twice (the request body, including the image, is in the idempotency key). See `tools/art/README.md` "Restyling placeholders".
- Tests: 4 new offline tests (41 pass). Mutation-checked: skipping a restyle as already generated, and not sending the texture, each fail.
- **Found on the way:** `docs/art/assets.csv` is out of date. It lists `ashen_slag` as missing (its placeholder exists) and does not list the portal frame, portal pane, Portal Igniter or Spire textures at all, so they cannot be restyled until `asset_list.py` knows them. Fixed in a follow-up PR.
## Asset list: the Act III placeholders: done
- `tools/art/asset_list.py` now lists the Act III slice placeholders as ordinary rows, so they show as placeholders and can be generated or restyled: Ashen Slag, the Atmospheric Portal Frame and pane and the Terraforming Spire (Blocks), and the Portal Igniter (Tools & modules). The future rows that remain are the animated portal pane and the Spire multiblock. Only those rows were added to `docs/art/assets.csv` and `ASSETS.md`; the rest of both files is unchanged.
- **Decision pending (found, not changed):** a full run of `asset_list.py` no longer matches the committed list. Since the module models arrived, it lists them as `core_mk2 (module)` and so on under Body models (keys like `core_mk2_module` instead of `core_mk2_model`) and their skins under Body skins, and drops the "Missing module models" group. Regenerating would rename keys the art workflow already uses (staged work in `art/rd/`, promote commands) and breaks four `test_rd_generate.py` tests that use `core_mk2_model`. Until the owner decides, add rows by hand as here, or update the tests and the staged keys together in one PR.

## RD tools, part 4: revising and exporting low-poly models: done
- `tools/art/rd_model.py`, through `rd_generate.py --model-revise ASSET --prompt "..." [--version N]` (a new version of the asset's RD model; paid, priced from RD's table, asks, idempotent; polled and saved like any model job, so `--promote` works) and `--model-export ASSET --target minecraft|blockbench|obj|atlas|turntable|axis_views|blender|web` (free, into `art/rd/<asset>/export/`). Generate, animate and the Blockbench export already existed. See `tools/art/README.md`.
- Tests: 3 new offline tests (44 pass). Mutation-checked: a revision without an Idempotency-Key, and one that skips the question, each fail.
- **The four RD parts agreed on 2026-10-30 are all done** (fix, repaint, restyle, low-poly). None has run against the real RD service yet: that needs the owner's new key in `$RD_API_KEY`.

## 7b ships, part 1: the Chemical Thruster: done
- **Chemical Thruster** (`chemical_thruster`): built into an Aeronautics ship, it pushes the ship away from its nozzle through vacuum. It needs a shaft at the back (full power at 64 RPM, nothing below `minRpm`; the force is capped at full spin), fuel in its tank (pipes reach it) and redstone: signal strength 0-15 is the throttle. Replicator recipe behind `starship` (placeholder price). Config section `ships`: `chemicalThrust` (400), `thrusterStress`, `thrusterTank`, toggle `chemicalThruster`.
- **Fuel:** the data map `void_reclaimer:thruster_fuel` (`api.ship.ThrusterFuel`: thrust multiplier, burn per tick); Nebulite Propellant is thrust 1, 2 mB per tick. Only listed fluids enter the tank. TFMG fuels come in part 4.
- **How it pushes:** `ChemicalThrusterBlockEntity` is a Sable `BlockEntitySubLevelActor`; its physics tick queues a point force at the block on `ForceGroups.PROPULSION`, in the ship's coordinates, the way Sable's propeller does. The force acts at the block, so anything off the centre of mass turns the ship: even a slightly unbalanced test ship tumbles (seen in the velocity log).
- Sable is now a `compileOnly` dependency; `extractCreateLibs` also unpacks its nested Veil and companion jars.
- `ThrusterPeripheral` (read-only: `getThrottle`, `getForce`, `isFiring`, `getSpeed`, `getTanks`; `docs/CC_PERIPHERALS.md`).
- Tests: `ThrusterMathTest` (5), `ThrusterGameTests` (5: tank rules, burn, no throttle, slow shaft, dry tank), `CCPeripheralGameTests.theChemicalThrusterIsAPeripheral`, and three real-ship `AeronauticsGameTests` (a fuelled, powered, turning thruster moves a ship away from its nozzle; without fuel or without redstone it stays exactly still). Mutation-checked: a reversed push, no Sable force, and no fuel burn each fail.
- **Aeronautics GameTests made reliable:** they were colliding with ships left in the local persistent world (a thruster ship is saved still firing and keeps flying). Each test now names its ship (`vr_test_<x>`) and reads and removes it by name, and the first test of a run removes every loaded ship. Sable facts: "all" is `@e` (not `@a`), a bad selector only prints a message (`remove` now throws on it), and `distance=` did not limit `sable remove`, so do not use it there. To clear strays from a local world, delete `run/world`.
- **Numbers are guesses:** at 16 RPM (quarter spin, force 100) the small test ship reaches about 7.7 blocks per second in 1.5 s, and a real ship is heavier. There is no top speed and no drag in deep space yet. Tune with real ships.
- Not seen in a client: the block's look, its orientation when placed, a ship flown for real.

### Placeholder assets
- `textures/block/chemical_thruster_side.png`, `_nozzle.png`, `_back.png` (16x16 programmer art; not yet in `docs/art/assets.csv`).

## 7b ships, part 2: the Ion Thruster: done
- **Ion Thruster** (`ion_thruster`): pushes a ship on Forge Energy from any energy mod (receive-only capability), with no shaft and no fuel. Force 60 at full throttle against the Chemical Thruster's 400, using 80 FE per tick; redstone is the throttle and sets both the draw and the push; a short buffer gives a proportional push. The nozzle points out of the face it is placed on. Replicator recipe behind `starship` (placeholder price). Config section `ships`: `ionThrust`, `ionFePerTick`, `ionFeBuffer` (8000), toggle `ionThruster`.
- `ThrusterPeripheral` gains the Ion Thruster (`getThrottle`, `getForce`, `isFiring`, `getEnergy`, `getEnergyCapacity`). The thrusters' force code moved to `ship/ThrusterPush`.
- Tests: `ThrusterMathTest` (3 more), `ThrusterGameTests` (3 more: buffer rules, draw and push, empty buffer), two real-ship `AeronauticsGameTests` (a charged thruster moves a ship; an empty one leaves it still), the peripheral test. Mutation-checked: never using energy, and pushing on an empty buffer, each fail.
- The numbers are guesses, and a Chemical Thruster's fuel is not weighed against FE cost yet. Not seen in a client.

### Placeholder assets
- `textures/block/ion_thruster_side.png`, `_nozzle.png`, `_back.png` (16x16 programmer art; not yet in `docs/art/assets.csv`).

## 7b ships, part 3: the RCS block: done
- The **RCS Thruster** can now be placed: `rcs_thruster` stays the same item (same id, same Sequenced Assembly recipe, still a material with its flat icon), now a block item. Placed, it is a small jet: force 40, 20 FE per tick, 2000 FE buffer, redstone throttle (config `rcsThrust`, `rcsFePerTick`, `rcsFeBuffer`, toggle `rcsPush`; the older toggle `rcsThruster` is still the recipe). Placed away from the centre of mass it turns the ship: one jet on the end of a 9-block beam spins it at about 1.3 rad/s within 2 s.
- `ship/ElectricThrusterBlockEntity` is now the base of the Ion and RCS thrusters; `ThrusterPeripheral` works on the base, so the RCS Thruster has the same functions.
- Tests: `ThrusterGameTests` (2 more: it is a weaker electric thruster; the item keeps its id and places the block), two real-ship `AeronauticsGameTests` (it pushes a ship; off-centre it turns one, read from `sable info`'s angular velocity), the peripheral test. Mutation-checked: no push from electric thrusters fails all three ship tests.
- **Decisions pending (taken unattended):** the owner chose "an RCS block" without saying what powers it. It runs on Forge Energy like the Ion Thruster, the simplest choice that fits small jets (no shaft or fuel line to route). It is a full cube for now; a small model can replace it.
- Not seen in a client.

### Placeholder assets
- `textures/block/rcs_thruster_side.png`, `_nozzle.png`, `_back.png` (16x16 programmer art; not yet in `docs/art/assets.csv`).

## 7b ships, part 4: TFMG fuels: done
- The Chemical Thruster also burns The Factory Must Grow's fuels, through the `thruster_fuel` data map: `tfmg:kerosene` (thrust 0.8), `tfmg:lpg` (0.7), `tfmg:gasoline` (0.6), `tfmg:diesel` (0.5), all at 3 mB per tick, against Nebulite Propellant at 1.0 and 2 mB per tick, so our own chain stays the best fuel. The numbers are guesses to tune.
- Test: `ThrusterGameTests.tfmgFuelsBurnAtTheirOwnStrengths` loads each real TFMG fluid, so a wrong id fails it; each goes in the tank and pushes at its own strength. Mutation-checked: a fuel missing from the data map fails it.
- **The four approved 7b PRs are done.** Next, and each needs the owner first: the Helm interview (6-DOF controls and camera), then the Create Propulsion vacuum spike.

## Helm, part 1: the seat and the six axes: done
- **Helm** (`helm`): the pilot's chair, from the owner's low-poly cockpit chair model (`geo/block/helm.geo.json`, GeckoLib; the `turn_180` animation plays on sitting). Right-click to sit (an invisible `HelmSeat` entity; sneak to leave). The chair's facing is the ship's nose. Replicator recipe behind `starship` (placeholder price).
- **Flying:** while seated, W/S, A/D, Space/Shift move the ship and Q/E roll, arrow keys pitch and yaw (new keybinds under Void Reclaimer). The mouse only looks. Each tick the Helm finds its ship (Sable sub-level), lists its thrusters, and `ThrustAllocator` sets a throttle per thruster from the six axes: a thruster opens in proportion to how well its force (for the movement keys) or its torque about the centre of mass (for the rotation keys) serves the request, and stays shut if it works against it. No thruster the right way means no movement that way: the ship you build is the ship you fly. A Helm's throttle beats redstone for that tick and the next.
- `HelmPeripheral` (read-only: on-ship, pilot, thruster count, what the ship can do each way, fuel and energy totals), per the standing rule.
- **Tests:** `ThrustAllocatorTest` (10, pure: every axis, angled thrusters, the centre of mass, coverage, the pilot's right for all four facings, the codec), `CCPeripheralGameTests.theHelmIsAPeripheral`, and two real-ship `AeronauticsGameTests`: a seated pilot pressing W moves the ship ahead through its stern thruster, and the arrow key yaws it through a bow jet while no keys means no movement. Mutation-checked: an allocator that opens nothing and a Helm that never commands each fail both pilot tests.
- **Found on the way:** the pilot's right is `forward x up`, not `up x forward` (Minecraft is left-handed); a wrong sign slipped through the ship tests, which cannot tell left from right on a straight push, and is now pinned by unit tests. In the test harness a cross-dimension teleport can leave a stale `ServerPlayer` reference: compare players by UUID. A `DirectionalKineticBlock` takes its shaft from the face opposite its nozzle, which the test ship's motor placement first got wrong.
- **Decisions pending (taken unattended):** without a ship the Helm does nothing (no "walk the block" mode); the seat sits 0.35 blocks up the chair; leaving is vanilla's sneak-to-dismount, and Shift is heave-down only while another axis is held.
- **Not seen in a client:** the chair model, the sit animation, riding a moving ship, the keys. The holds (RCS lock and dampener), the HUD and camera roll are PRs 2 and 3.

### Placeholder assets
- `textures/item/helm.png` (16x16 programmer-art chair icon) and `textures/block/helm_particle.png`. The chair itself is the owner's model.

## Helm, part 2: the holds (RCS lock and dampener): done
- Two holds at the Helm, both on by default and each on its own key: the **inertial dampener** (Z, the body's key) brakes the ship to a stop, the **RCS lock** (X, new) stops its spin and holds its heading. Each works only on the axes the pilot is not using, so a held key always wins. `FlightSettings` gains `rcsLock` (saved; old saves load it as on), toggled with the existing `ToggleFlightSettingPayload`.
- How: each tick the Helm reads the ship's velocity and spin from Sable, turns them into the ship frame, asks `ThrustAllocator` for the opposite (`holdRequest`), and takes the larger of the key throttle and the hold throttle per thruster. A hold is proportional: full throttle at 1 block/s (or 1 rad/s) and easing off below that, stopping inside a 0.05 deadband.
- **Tests:** `ThrustAllocatorTest` (5 more: the dampener's request, a held key owning its axis, the RCS lock and the deadband, holds off, and that a hold request opens the braking thruster), `FlightInputTest` (the settings round-trip, and an old save without the field loads with the lock on), and two real-ship `AeronauticsGameTests`: a ship shoved to 3 blocks/s and spun at 1 rad/s is stopped by the holds within 8 s, and with the holds off it keeps going. Mutation-checked: a Helm that ignores the holds fails the holds-on test.
- **Found on the way:** (1) the allocator scored a thruster by the better of "helps the force" and "helps the torque", so with both holds on it opened a bow jet that braked the sideways drift while feeding the spin, and the spin grew to 5 rad/s; it now scores by the signed sum and such a jet stays shut. (2) Full-throttle holds chattered across the deadband at about one tick's worth of thrust (0.2 rad/s on the test ship); they are proportional below 1 block/s or 1 rad/s now. (3) A tumbling body trades yaw for roll, and a ship with only side jets cannot hold roll, so the test ship carries roll pairs as well; a real ship needs jets on every axis it wants held. (4) The test ship is balanced (a keel block under the Helm) so the thrusters' plane holds the centre of mass; an unbalanced ship's engines also pitch it.
- **Decision pending (unattended):** the hold ramp (1 block/s, 1 rad/s) and deadband (0.05) are constants in `HelmBlockEntity`; the owner may want them in the config or per size class.

## Helm, part 3: the pilot HUD and the camera riding the ship: done
- **HUD** while in the chair, right of the hotbar: speed (or `NOT ON A SHIP` / `NO THRUSTERS`), `Dampener ON/OFF` and `RCS lock ON/OFF`, `FUEL x/y mB` and `ENERGY x/y FE` across the ship's thrusters (only the kinds it carries, red when empty), `THR n` with the twelve axis labels (F B R L U D, RR RL NU ND YL YR) lit where the ship has a thruster for them. Left of the hotbar: an artificial horizon (sky, ground and horizon line roll and slide with the ship, a fixed ship symbol) with `HDG 000 PIT +0 ROL +0` above it. All through `GuiGraphics`.
- **Camera:** the view rolls with the ship so its deck stays level on screen; mouse look, the orbit camera and photo mode keep working (photo mode owns the roll and is left alone). The roll comes from Sable's interpolated render pose, so it is smooth between ticks.
- **Data:** the Helm's readout now carries the ship's velocity and spin in the pilot's terms and its attitude, sent to the pilot each tick; the CC peripheral exposes them too (`getVelocity`, `getSpin`, `getAttitude`).
- **Tests:** `AttitudeTest` (level, headings for all four chairs and a yawed ship, nose up, roll right/left, the readout codec round trip), `ShipCameraMathTest` (no roll on a level ship from any view; roll follows the ship ahead and reverses looking aft; a pitched ship rolls the view across the beam; the degenerate look-along-up keeps the last roll), the Aeronautics GameTest `theHelmReadoutFollowsTheShip` (moving ahead reads as forward speed matching `sable info`, heading north, level) and the Helm peripheral test asks for the three new functions.
- **Found on the way (a real bug from PR 1):** the seat did not ride the ship. It was spawned at the Helm's place in the world and Sable left it there as the ship moved off (Sable carries entities that stand on a ship, not one with no physics), so the Helm lost its pilot within a block or two; the PR 1 and 2 tests passed anyway because a ship coasts in vacuum and the holds test ship barely moved. Tagging the seat `sable:retain_in_sub_level` (Create's seats are) keeps it in the ship's plot, which the GameTests liked but which puts a real pilot's camera in the plot: a black screen (seen in the headless client). The seat now stays in the world and moves itself to the Helm's world position every tick (`HelmSeat.tick`, `worldSpot`). `theHelmReadoutFollowsTheShip` proves it: the Helm only reads its ship while it sees its pilot, and the ship has moved on by then.
- **Found in the headless client:** removing a ship under a seated pilot (`/sable remove`) dragged the player twenty million blocks into the plot and dropped the connection (`Cannot drop chunks in plot`): the Helm's position is in the plots but no ship is there, so the seat's fallback "the Helm's own position" was the raw plot position. `HelmSeat.worldSpot` now returns null for a Helm in the plots without a ship, the seat discards itself, and the pilot is left standing where the ship was (`removingTheShipUnseatsThePilotInTheWorld`). That test checks the end state; in the GameTest server Sable clears the plot in the same tick, so the seat already left by the "no Helm" branch and the test does not catch the raw-position fallback on its own (noted here rather than faked).
- **Found by the ship suite turning red after it had passed five times (same code):** a seat spawned into a chunk that is loaded but not yet *entity-ticking* is kept out of every entity lookup by vanilla until the chunk is (and does not tick), so the Helm's scan for its seat found nothing right after seating and the pilot was "lost"; whether the chunk had been promoted in the sixteen ticks between force-loading and seating was a race the earlier runs won. The Helm now keeps a reference to the seat it spawned (`HelmBlockEntity.seat`, server only), so finding the pilot never waits on the world chunk (the scan is the fallback for a seat restored from a save), and `ship/HelmSeats` drives every live seat's `follow()` once per server tick, because neither the seat's own tick (its chunk may never be entity-ticking) nor the Helm's (Sable drops a removed ship's plot chunk without `setRemoved`) can be relied on. The GameTest assertions now print the chunks' states, which is how both were caught. Also: `Ships.shipAt(Level, pos)` returned a *removed* ship (the plot lingers), which kept a seat transforming through a dead ship's pose; it now returns null like the server overload.
- **Seen in the headless client (deep space, an assembled ship with a Helm, two Ion Thrusters and six RCS jets, unpowered):** sitting works, the pilot sees the world from the chair (first and third person), the HUD column fits beside the hotbar at the 853x480 window (speed, both holds, `ENERGY 0/28k FE` in red, `THR 8`, all twelve axis labels lit) and the attitude box reads `HDG 000 / PIT +0 ROL +0`; `/sable remove` under the pilot leaves them standing in the room. Not seen: motion and roll (the thrusters had no power; there is no command to charge them), so the camera roll and the horizon's direction rest on the unit tests. Also seen: a ship's blocks render unlit inside a lit room (light from the world does not reach a ship's plot; Sable lights ships from their own blocks and the sky), which is Sable's lighting, not ours; ships want their own lamps.
- **For the owner to look at in game:** sit at a Helm on a ship with a roll pair, roll with Q/E and check the horizon in the HUD tilts the opposite way to the view and that the camera roll feels right (if it is backwards, negate `ShipCameraMath.roll`'s result and `PilotHudLayer.attitude`'s rotation; the unit tests pin the current signs). Also that the HUD fits at GUI scale 3 on a small window.

## Tooling: faster GameTest turnarounds: done
- **The GameTest JVM hang, found at the source:** `MinecraftServer.stopServer` ticks the chunk maps `while (hasWork())`, and `hasWork()` includes "has tickets"; Sable keeps tickets on its ships' plot chunks for as long as the ships exist, so a GameTest server with a ship in its world never left that loop (100% CPU on the server thread, the world's `session.lock` held, the next run dying with "already locked"). `mixin/GameTestStopMixin` bounds the loop for the GameTest server only (400 turns, then a warning, save and exit as normal). An Aeronautics run went from 8 to 10 minutes (a timeout and a kill) to 37 seconds, gradle exits cleanly with the test result, and nothing is left behind.
- `tools/gametest.sh [main|aero] [filter]` runs a suite, prints the summary and failures, exits non-zero on a failure and kills a leftover JVM by a pattern that matches only the JVM (a `grep gameTest` in the same shell command matches and kills that shell, which happened twice). `-PgametestFilter` now also works for `runGameTestAeronautics`.
- `tools/pr.sh "title" "body"` pushes and opens the PR against `main` (the repository's default branch is not `main`; a PR against it gets no CI, which cost 40 minutes once).
- `tools/client-test/bootstrap_world.sh` creates the headless client's `vrtest` world through the menus once, so a client check on a fresh machine is a script, not an hour.

## Create Propulsion vacuum spike: done, no bridge needed
- Propulsion's thrusters multiply their thrust by an atmospheric factor from Sable's air pressure: at zero pressure the factor is 1 (full thrust), at sea level 0.2 with the default config (`USE_ATMOSPHERIC_PRESSURE`, `ATMOSPHERIC_PRESSURE_AMOUNT`). Deep space already declares `base_pressure` 0 (`dimension_physics/deep_space.json`), so they push at full strength there with nothing from us.
- **Test:** `aCreatePropulsionThrusterPushesItsShipInVacuum`: a redstone-powered Creative Thruster on a balanced cross moves its ship more than 3 blocks along its axis in 2 s; mutation-checked (no redstone, no push).
- **Not done:** the Helm does not command Propulsion's thrusters (they are not `ThrusterLike`); an adapter is a later, separate step if the owner wants mixed ships.

## Earth flight, part 1: physics and wind: done
- Ruined Earth gets an explicit Sable `dimension_physics` file (gravity 11 down, pressure 1, drag 0.09: Sable's defaults, written down). Thruster ships lift and climb there (a Chemical Thruster is 400 N at full power); Aeronautics propellers push in its air; a free hull drifts with the wind.
- **Wind** (`earth/EarthWind`): a pure horizontal field of sines over position and time, never above `earth.windSpeed` (config, default 1.5 blocks/s, 0 switches it off), swinging round in about a minute, the same on the client and the server (no state, no sync). The flying body is carried toward the air's velocity by `WIND_COUPLING` (0.02) per tick after thrust and braking, so holding a key or the dampener always wins (the starter brake of 0.04 per tick beats the strongest push of 0.0015); with the dampener off the body ends up carried at wind speed. The server's speed check allows the wind on top of top speed. Ships get the same nudge each server tick (`earth/WindDrift`), through the rigid body, because Sable's wind hook is read by nothing.
- **Tests:** `EarthWindTest` (horizontal, capped, zero without a top speed, shifts with place and time, scales), `FlightPhysicsTest` (carried to wind speed unpowered; the dampener holds; no wind is the old step; the drift formula), `RuinedEarthShipGameTests` in the Aeronautics suite (climb, propeller push, hull drift). Deep space is untouched: no wind there, and its platform-stays-put test still runs.
- **Found on the way (with a bytecode-reading agent):** Sable weighs blocks by tag (`#sable:heavy` 2 for stones, `#sable:super_heavy` 4 for storage blocks such as a redstone block, 1 otherwise), so a redstone-block throttle on one side of a thruster puts the centre of mass off the thrust line and the ship tips over and flies sideways; the test hulls are symmetric. A creative motor turns at 16 rpm until scrolled (`ScrollValue` NBT, max 256); our Chemical Thruster reaches its 400 N at 64 rpm, an Aeronautics wooden propeller pushes about its rpm times the air pressure. Sable's air pressure falls with height from the dimension's sea level, and the ruined Earth generator is flat with sea level -63, so at the test height of 200 the pressure is about 0.35 (propellers are weaker up high; thrusters do not care). Nothing in Aeronautics checks for blocks round a propeller's blades.
- **Decisions pending (unattended):** the wind's top speed (1.5 blocks/s), swing period (a minute), cell size (96 blocks) and coupling (0.02) are first guesses; tune in play. Whether ruined Earth's air pressure should be about 1 at ground level (y 64) instead of 0.6 (a `pressure_function` in `dimension_physics/ruined_earth.json`, or a sea level in the generator) is for the owner.
- **Not here:** the Earth-arrival milestone and Aeronautics recipes (part 2); our own airship kit (later, own interview).

## Stations, part 1: the Station Core: done
- **Station Core:** a block with a 27-slot chest (right-click) and a name (rename the item in an anvil, or CC `setName`). Sneak-use assembles everything connected to it into one Sable ship named after it; sneak-use again sets the station down as blocks where it floats, turned to the nearest quarter turn (it must be level within 10 degrees, and the landing blocks must be air or replaceable, else a message says why).
- **Stations list and HUD:** every loaded core is listed once a second in the level's `StationList` (synced): name, id, world position (a core aboard a ship moves with it), assembled or not. The HUD pins the three nearest stations beyond 24 blocks to the screen edge with name and distance.
- **Perks:** the Replicator makes `station.replicatorSpeed` (default 2) ticks of progress per tick within `station.range` (default 8 blocks) of a station; a Backup Core built into a station respawns you where the station has moved to.
- **Tests:** `StationMathTest` (quarter turns, vanilla `yRot` agreement, level check, Sable's block rotation), `StationListTest` (nearest, markers, codec), `ReplicatorLogicTest` (the bonus steps), GameTests `StationCoreGameTests` (a placed core is listed and unlisted; the Replicator bonus near and far), `StationGameTests` in the Aeronautics suite (assemble and name; set down a quarter-turned station and find its arm turned; refuse to set down onto obsidian), and the peripheral in `CCPeripheralGameTests`.
- **Found on the way:** Sable has no disassembly or merge API; Aeronautics' nested "Simulated" jar has both (yaw-only), now compiled against (see `CLAUDE.md`). `BaseContainerBlockEntity`'s custom name is private, so the station keeps its own name field and copies the item's custom name in `applyImplicitComponents`.
- **Decisions pending (unattended):** the station block limit (50,000), range (8) and Replicator speed (2) are config defaults; the Station Core recipe (80/60/40/100 dust, 2 minutes, Starship milestone) is a placeholder.
- **Placeholder art:** `textures/block/station_core_top/side/bottom.png`, `docking_clamp_face/side/back.png` (16x16 programmer art).
## Earth flight, part 2: Earthfall and the Aeronautics parts: done
- **Earthfall** milestone: first setting foot on ruined Earth (new criterion `dimension_visited`; the player's visited dimensions are recorded on every dimension change and login in `ReplicatorData`). Log entry text is a placeholder.
- **Replicator:** every Create Aeronautics block and item has a recipe (`aeronautics_<id>`, Blocks tab; 44 of them, Levitite Blend being a fluid) unlocked by Earthfall, with a short description each; dust costs are placeholders (propellers on carbon, lift parts on ice and metal, levitite on silicon).
- **Create routes, three:** Andesite Propeller (press a Carbon Composite Plate), Steam Vent (press a Micro-Alloy Sheet), Levitite Blend (heated mixing of Pure Silicon Dust and Fine Carbon Powder), each under its chain's config toggle.
- **Tests:** `EarthMilestoneGameTests`: stepping onto ruined Earth reaches Earthfall and it stays reached after leaving; the propeller recipe is LOCKED before and QUEUED after. Mutation-checked.
- **Decisions pending (unattended):** the dust costs and times of the Aeronautics recipes; which parts deserve Create routes (three chosen).

## Stations, part 2: the Docking Clamp: done
- **Docking Clamp:** a block facing any way; while powered it docks the ship whose block sits in the cell in front of its face: the visitor's blocks move into the station's plot (one Sable body; the station's thrusters move both), its name and ship data are remembered. Power off (after docking by power), sneak-use, or CC `release()` assembles the remembered blocks back into their own ship, moving as the station moves, named as before. The visitor must be level and square to the station within 10 degrees (blocks move by quarter turns only) and must not overlap the station; the clamp reports why not (`getStatus`).
- **Tests:** `StationMathTest` (the yaw residual), `StationGameTests.aPoweredClampDocksATouchingShipAndReleasesItWhenUnpowered` in the Aeronautics suite (a three-stone bar slid into the front cell is docked when a redstone block powers the clamp: the visitor ship is gone and its block is part of the station; removing the redstone releases it as a ship named as before with its three stones), the peripheral in `CCPeripheralGameTests`. Mutation-checked (no block move; no re-assembly).
- **Known limits:** a docked ship's Helm seat is given up (the Helm moves plots and the seat finds no Helm); the visitor's own momentum is lost on docking (the station's velocity is kept, as Simulated's glue does); a docked ship that is broken apart while docked comes back as whatever is left.
- **Decisions pending (unattended):** the 10-degree alignment tolerance and the 5-tick look; the recipe (40/30/10/60 dust, Starship milestone).

## Stations, part 3: ships through the Atmospheric Portal: done
- A Sable ship that flies into a lit portal pane is rebuilt on the far side: outward only if its (turned) bounds fit the gate's opening, with the seated pilot and anyone standing on it brought to their spots and the pilot re-seated, keeping its velocity through the matching gate (Earth's gate is made or grown to fit, as for a walking traveller). Too big, or no room beyond the gate, and the ship bumps with a message to its crew and a five-second cooldown. On Earth an arriving ship lands on the ground; flying back out it comes out in deep space still moving.
- How: Sable cannot move a ship between dimensions, so the blocks (with block entity NBT) are copied into the world beyond the far gate, turned to the grid, assembled with Sable's own assembler, then turned to the exact orientation and pushed two ticks later when the new body exists. Panes register themselves in a saved per-level list so ships are only tested against real portals.
- **Tests:** `ShipGateTest` (gate turns, world extents under a pose, the fit check), `ShipPortalGameTests` in the Aeronautics suite: a three-stone ship with a Helm and a seated test pilot crosses to ruined Earth whole with the pilot seated; a ship wider than the gate stays behind; a ship on Earth flying back out keeps its velocity in space. Mutation-checked (no crossing; everything fits).
- **Found on the way:** the new rigid body does not exist until Sable's next physics tick, so velocity must be applied a tick or two after assembly; ruined Earth's chunks take far longer than the void's to load, so tests wait for the chunk rather than a fixed delay. Earth's return gate can predate the pane registry (worlds from before this PR, or a gate built by `ensureGate`), so on Earth the gate from `PortalLinks` counts as a portal too while it is still lit. A ship that spans a plot chunk without a block in it has a chunk holder with no bounding box: `StationAssembly.blocksOf` skips those (it crashed the server before). In the Aeronautics suite the outward crossing writes Earth's exit link, so the return test sets its own link only when it builds its ship, 200 ticks in. CI then showed the main GameTest server passing every test and hanging at "Saving worlds": `GameTestStopMixin` bounded the stop loop by turns (400), but with ship plots in the world each turn ticks the chunk maps' unload work and takes seconds, so the bound was many minutes, past CI's 900 s. The loop is now also bounded by time (15 s), and `tools/gametest.sh` reports when the server does not exit by itself within 40 s of the summary instead of silently killing it.
- **Known limits:** hanging entities and items aboard are not carried; a ship arriving on Earth is set one block above the gate's floor row and drops to the ground; the gate on the way back is the exit's spot with no fit check (nothing to fit through in space).
- **Decisions pending (unattended):** the 2-block clearance beyond the gate, the 5-second cooldown, half a block of fit slack.

## Launch complex, part 1: the controller, the ring and the pad: done
- The three-block Mass Driver is replaced by a **Launch Complex**: a 15 x 15 ring of frame blocks (`#void_reclaimer:launch_frame`: Scaffold, Portal Frame, Create's industrial iron block and andesite casing) with a **Launch Controller** in the middle of one side, facing the pad. Sneak-use the controller to assemble: the ring must be complete and the pad empty; the pad is then filled with invisible solid Launch Pad blocks (walkable, no item, no drop) and the controller is marked assembled. Sneak-use again, or break the controller, to set it down (the pad blocks go). A broken ring sets it down by itself within a second; a pad block someone breaks grows back.
- The controller takes a shaft on its back and payloads from any side (2 slots of 16, payload tags only, nothing out; right-click with a payload to load by hand). Wind-up and firing are the old Mass Driver's (`MassDriverMath`, the `massDriver*` config), only while assembled and with the toggle on; the sound, cloud particles and sky streak come from the muzzle 14.3 blocks above the pad's centre. `MassDriverLaunchEvent` carries the controller's position.
- The old Kinetic Drive Base, Payload Chamber and Magnetic Barrel Nozzle are inert: they stay registered (old worlds load, the blocks can be picked up) but launch nothing and have no Replicator recipe.
- **Tests:** `LaunchPadTest` (ring and interior cells for every facing, centre and muzzle), `LaunchComplexGameTests` (10: a Portal Frame ring assembles and fills the pad, then sets down; a gap, dirt in the ring or a stone on the pad refuses; a broken ring sets the pad down and a missing pad block grows back; breaking the controller sets the pad down and drops the payloads; and the old driver's launch tests moved over: launches into the sphere, speed scaling, unassembled or switched off does nothing, payloads only and nothing out, the launch event's counts and cancel, payload tags), `MassDriverGameTests.theOldMassDriverStackIsInert`. Mutation-checked.
- **Placeholders:** `textures/block/launch_controller_front/side/top.png` (16x16 programmer art); the pad has no visible model yet (PR 2 draws `geo/entity/mass_driver.geo.json` over it).
- **Decisions pending (unattended):** the ring block set; the controller on the pad's edge (so the shaft reaches it) rather than at its centre; one-layer pad with no collision for the gun tower; self-healing pad; the controller's price (90/65/20/70); the old blocks kept registered but inert rather than removed.

## Phases
Original plan, with where each stands (the revised plan for what is left is in `CLAUDE.md`, "Revised phase plan"):
- [x] 1. Deep Space & Spawn
- [x] 2. Zero-G Flight
- [x] 3. Cosmic Dust
- [x] 4a. Replicator (queue, milestones and logs, JEI, Scaffold, Salvage Tool)
- [x] 4b. Nanite Swarm, Tether Anchor, Backup Core (plus death marker and starter nebula)
- [x] 5a. Body data (frames, parts, size classes, scale, stats, storage, modules)
- [x] Extensibility + body sections (datapack registries, events, chassis/sockets, paint, wear, blueprint, kits)
- [x] Foundations: Aeronautics required, Dyson counters and sky
- [x] 6. Asteroids
- [x] 7. Machines (part 1 materials and Create chains, part 2 custom machines)
- [x] Art: RD art pass 1 (48 icons and textures, first RD module model), art tooling and picker, Test Starship placeholder entity
- [ ] 5b. Body rendering (GeckoLib, first person) & assembly UI
- [ ] 7b. Create Aeronautics ships (Space Thruster, Helm)
- [x] 8. Zero-G Frame, payloads and machinery recipes
- [ ] 9-14 (revised): Mass Driver, Starship, Ruined Earth, Spire, Monolith and villagers, Restoration

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
