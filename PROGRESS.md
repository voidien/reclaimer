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

## Phases
- [x] 1. Deep Space & Spawn
- [x] 2. Zero-G Flight
- [x] 3. Cosmic Dust
- [ ] 4. Replicator (includes the Backup Core respawn block)
- [ ] 5a. Body data (frames, parts, size classes, scale, stats, storage, modules)
- [ ] 5b. Body rendering (GeckoLib, first person) & assembly UI
- [ ] 6. Asteroids
- [ ] 7. Machines
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
