# Deep space, flight, energy and dust

Settled decisions, moved here from `CLAUDE.md`. Keep them current in the same PR as the change.

**Phase 1**
- Deep space is an empty void (flat generator, no layers; since Phase 6 the custom asteroid generator on top of that, see the Asteroids block). Height is the vanilla maximum, Y -2032..2031. A soft push-back starts 96 blocks from either limit (config), so nothing reaches void damage.
- The Overworld, Nether, and End are locked for good. Endgame travel goes to `ruined_earth`. Creative and spectator players may bypass the lock (ops can switch modes). Nether portals can't be lit in deep space.
- Light: real darkness, with sky light from a fixed, dim, distant sun (`fixed_time` 12800 gives effective sky light 10/15). Vanilla sky light only travels downward, so the sun is drawn near the zenith. Undersides and enclosed spaces stay dark.
- Zero-G covers everything: players and mobs via a `generic.gravity` modifier, other entities via NoGravity plus a tag, and falling blocks via a mixin. Fluids still flow. Thrown or hit things keep vanilla's light drag. Gravity generators (later) hook into `ZeroGravity.isZeroG(level, pos)`.
- **Mining in zero-G:** vanilla divides mining speed by 5 for a player not on the ground, and in zero-G nobody is, so `space.ZeroGMining` multiplies it back (high priority, so tool modules and the asteroid rules see the corrected speed). Hand-mining a Scaffold or Tether Anchor is as fast as on the ground.
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

**Cloud scanner (after 4b)**
- With a Dust Sensor, key N toggles (client config `cloudScanner`, default on) a wire mesh with a faint tint, in the kind's color, for every cloud whose edge is within 512 blocks.
- The mesh follows the true lumpy edge (`CloudShape`): rays on a 16×32 latitude/longitude grid find the outermost point at density 0.05. Computed once per cloud on a background thread and cached.
- Drawn unfogged (fog start restored afterwards) through `RenderLevelStageEvent`; this bends the Phase 3 "clouds are invisible" rule only for sensor owners who switch it on.

**Cloud Siphon block (2026-10-30, owner's choices)**
- `cloud_siphon` (`machine/CloudSiphonBlockEntity`, a Create kinetic block with a shaft axis) sits in a **dense** dust cloud and condenses it into the cloud kind's raw item (`dust.RawItems`: Metallic Grains, Silicon Grit, Raw Ice Shards, Carbon Flakes). Items per second = density x power x `machines.siphonRate` (0.5), and nothing below `machines.siphonMinDensity` (0.4). Config also has `siphonStress` (4 SU per RPM) and the `cloudSiphon` toggle.
- Power is rotation for now (1.0 at 64 RPM, proportional, 0 below `minRpm`), behind `CollectorPowerEvent` so an electricity addon can supply it. Density and power are sampled once a second.
- Nine output slots; Create funnels, belts and hoppers can only take from them. A full output pauses it; nothing is lost. Replicator Blocks tab (Diversified), 35/25/20/25 dust (placeholder). Named "Cloud Siphon" because "Cloud Collector" is the module's item.

**Cloud Collector module (2026-10-30, owner's choices)**
- A module with a `collector` spec on a **tool mount** (it is not a wheel tool: if selected on the wheel it does nothing). While installed and powered, dust gathered in a cloud also produces raw items of the cloud's kind: `yield` items per dust (default 1.0, twice the Replicator's 0.5), so the rate follows density, distance flown and the body's dust intake. `dust.Collectors.collect` is called by `DustTicker` after `DustGainEvent`, so a cancelled gain also cancels the items.
- Costs `energy_per_second` (default 1) while in a cloud; stops in low power. Items go to the inventory, overflow to the Replicator output buffer; with both full it pauses. Fractions are carried per player (lost on logout, under one item).
- Config `collector.enabled` and `collector.yield` (a global multiplier). Replicator Modules tab, Diversified milestone, 30/25/15/20 dust (placeholder).
- The Cloud Siphon block (above) is the base-machine version of this module.

**Start of the game (2026-10-30, owner's choices)**
- **Starter dust:** a new body gets Metallic 25, Silicate 30, Carbon 5, Ice 0 (config `starter`, toggle `enabled`) when it first reaches the Awakening milestone in play (`replicator/StarterDust`, from `MilestoneReachedEvent`). That pays for the Dust Sensor, Thruster Mk I, Scaffold and Rotor Arm. Worlds that already passed Awakening get nothing.
- **Guided first craft:** the Awakening toast and AI log line (Log 0001) tell the player to press R and build a Dust Sensor. Toast only; no forced screen.
- **Built-in hum:** the directional hum works without a Dust Sensor (same range, tone only). The sensor adds the signal bar, markers, readout and pulse.
- **Gathered, not held:** dust milestones (First Dust, Diversified, datapack `dust_collected` / `dust_kinds_held`) count dust actually gathered, kept per player in the `dust_collected` attachment (`dust/DustLedger`, recorded by cloud gains and drilling). The starter gift, refunds and the Replicator never reach them. `/void_reclaimer dust set` raises the ledger too, so test setups still unlock milestones. Saves keep milestones already reached.
- **Not changed:** clouds can still be far from spawn (the starter nebula game rule stays opt-in); revisit after the next playtest.

**Dust HUD (2026-10-30, owner's choices)**
- **Icons:** a dust kind has an optional `icon` (a 9x9 GUI sprite id, built-ins `hud/dust_<kind>`); without one a square in its colour is drawn (`client/hud/DustIcons`). The built-in icons are placeholders (diamond, block, cross, disc) until the owner's RD icons replace the four PNGs. They are listed in `docs/art/ASSETS.md`.
- **Meter:** icon, bar and number per kind, with the full names beside the bars while any screen is open. It dims to 35% after about 10 seconds without a change, unless the player is in a cloud or a screen is open (`DustMeterFade`).
- **Replicator prices** are icon plus amount (red when unaffordable); hovering an icon shows the name and the amount held. The Dust Sensor's cloud title has the kind's icon. JEI's price text is unchanged.

