# Extension points

Every new extension point is added here in the same change.

| Extension point | Kind | Notes |
|---|---|---|
| `void_reclaimer:dust_kind` | Synced datapack registry (`api.dust.DustKind`) | `color`, `cloud_weight` (0 = never in clouds), `name`, `order` (built-ins 0–3), optional `icon` (a 9x9 GUI sprite id; without one a square in `color` is drawn) |
| `void_reclaimer:milestone` | Synced datapack registry (`api.milestone.Milestone`) | `criterion`, `order`, optional `title`/`hint`/`log_title`/`log_text` |
| `void_reclaimer:milestone_criterion_type` | Code registry of `MapCodec<? extends MilestoneCriterion>` | Built-in: `always`, `dust_collected`, `dust_kinds_held` (both count dust *gathered* from clouds and drilling, `MilestoneContext.collected`, not the reserve), `blocks_placed`, `dust_full`, `dyson_tier`, `items_made` (a list of item ids the player must have held) |
| `void_reclaimer:replicator_category` | Synced datapack registry (`api.replicator.ReplicatorCategory`) | Replicator tabs; `order`, `title` |
| `void_reclaimer:casing` | Synced datapack registry (`api.body.Casing`) | `order`, `name`, `texture`, `requires` (milestone) |
| `void_reclaimer:trail` | Synced datapack registry (`api.body.Trail`) | `color` (none = accent), `particles`, `order`, `name`, `requires` |
| `void_reclaimer:replicator` recipes | Recipe type | `result` or `results` (kits), optional `title`, `description` and `inputs` (a list of `ingredient` and `count`, which defaults to 1, by item or tag: taken with the dust from the main inventory, then the offhand, then armor, and given back on a cancel); `cost` is a map of dust kind id → amount; `category` and `milestone` are registry ids |
| `void_reclaimer:size_class` | Synced datapack registry (`api.body.SizeClass`) | `scale` (`generic.scale`), `rank` (for module `min_size`), `name`, `storage_factor` (what the same storage pods hold on this size, 1 by default), `stays_in_orbit` (too big for a portal down to Earth, false by default), `speed_factor` (times the best thruster's top speed), `max_speed` (blocks a second, default 40) |
| `void_reclaimer:chassis` | Synced datapack registry (`api.body.Chassis`) | Body layout: `size_class`, `core_socket`, `sockets` (`id`, `accepts` socket types, `required`, `parent`, `pivot`, `area`, optional `mirror`: draw the section flipped left to right, for the far side of a pair built from one lopsided model; optional `boxes`: a list of `at` and `size`, a big body's small boxes, in blocks at scale 1, `at` being right, up, forward from the feet and `size` width, height, length); socket id = GeckoLib bone (5b); optional `cockpit` (`forward`, `up`, `right` in blocks at scale 1): first-person camera offset, and the hand is hidden |
| `void_reclaimer:section` | Data component (`api.body.SectionStats`) | On section items: `socket_type`, tier, stats, `mounts`; core sections name a `chassis` |
| `void_reclaimer:module` | Data component (`api.body.ModuleStats`) | On module items: `kind` (fixed `ModuleKind`), tier, mass, `min_size`, core/thruster/storage specs |
| `void_reclaimer:module` `collector` spec | Part of the module data component (`api.body.ModuleStats.Collector`) | On module items: `yield` (items per dust gathered), `energy_per_second`, `outputs` (dust kind id → item id). Run by `dust.Collectors` from `DustTicker` |
| `void_reclaimer:tool_module_type` | Code registry (`api.body.ToolModuleType`) | What a tool module does (`use`, `breakSpeed`); items name one with the `void_reclaimer:tool_module` component |
| `void_reclaimer:holo_readout` | Code registry (`api.holo.HoloReadout`) | A readout a Holo Projector can show: a sampler (world to snapshot, server side) and a pure formatter. The lang key `holo_readout.<namespace>.<path>` names it; lines past 8 or 48 characters are cut |
| `void_reclaimer:holo_projector` | Create registry entry (`create:display_target`) | `compat.create.HoloDisplayTarget`: a Display Link writes to a Holo Projector |
| CogScript `holo` module | Mixin into Advanced Electricity's `ComputerOS` (`compat.act.HoloScript`) | Programs write to Holo Projectors: `docs/ADVANCED_COMPUTER_TECHNIC_PATCH.md` |
| `void_reclaimer:tool_module_type` built-ins `drill`, `harvester` | Code registry entries | Mechanical Drill and Matter Harvester: `breakSpeed` on `#void_reclaimer:asteroid_blocks`; the break rules are in `asteroid.Drilling` |
| `CollectorPowerEvent` | Event, mutable power | About once a second for each Cloud Siphon: `power` starts as the shaft's (1.0 at 64 RPM, 0 below the minimum speed); an addon (electricity) raises it to run the block without rotation |
| `void_reclaimer:freezing` recipes | Recipe type (`machine.FreezingRecipe`) | `ingredient`, `results`; run by the Encased Fan type below |
| `void_reclaimer:freezing` fan type | Create registry entry (`create:fan_processing_type`) | `machine.FreezingFanType`, priority 450, active in powder snow; registered through `MachineSetup.FAN_TYPES` |
| Spout behaviors | Create `BlockSpoutingBehaviour.BY_BLOCK` | `machine.SpoutCasting` on the Depot; add more casting by registering for another block |
| `void_reclaimer:config` recipe condition | NeoForge condition (`machine.ConfigCondition`) | `key` names a toggle in `Config.TOGGLES`; gates any recipe on a config switch |
| `void_reclaimer:thruster_fuel` | Data map on fluids (`api.ship.ThrusterFuel`: `thrust` multiplier, `burn` mB per tick at full throttle) | What a fluid is worth in a Chemical Thruster; only listed fluids enter its tank. `data/<ns>/data_maps/fluid/thruster_fuel.json` |
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
| `void_reclaimer:config_choice` | Recipe condition (`machine.ConfigChoiceCondition`) | `key`, `value`: the recipe loads only at that setting of a config choice (`shellPrices`) |
| `BodyCrashEvent` | Event | A body flew into something, on the server, before any harm. Cancel it, change the damage, or say whether blocks break |
| `void_reclaimer:crash_proof` | Block tag | Blocks no crash breaks |
| `void_reclaimer:space_weather` | Synced datapack registry | A kind of bad weather: where, how often, how long, what shelters a body and what it does |
| `void_reclaimer:weather_exposure_type` | Code registry | What shelters a body: `everywhere`, `open_sky`, `sun_line` built in |
| `void_reclaimer:weather_effect_type` | Code registry | What weather does each second: `wear`, `damage`, `energy` built in |
| `SpaceWeatherEvent` | Event | `Warning` (cancellable), `Started`, `Ended`, and `Exposed` (cancellable, with a share a part can reduce) |
| `ShipCrashEvent` | Event | A ship's hit is about to break a block, on the server, once for each block. Cancel it to keep the block |
| `RockFractureEvent` | Event | Too much drilling is about to split a rock, with the blocks that would come away. Cancel it to keep them |
| `MainBodyWreckedEvent` | Event | A main body was destroyed and left as a wreck, with the wreck's id. The player already wears a new starter body |
| `BodyCollisionEvent` | Event | A body hit something in flight: a big body by its hit boxes, or a smaller one against a big body. One crash is one event, with the speed it had, the speed it kept and the part lost. Fires on the client that flies the body |
| `ShellLinkEvent.Pre` | Event, cancellable | Before a link changes anything; cancelled, the link ends `CANCELLED`. A listener that throws counts as a refusal |
| `ShellLinkEvent.Started`, `.Ended` | Event | After a link is made, or ended with its `Reason`, and saved. An end cannot be stopped |
| `SectorRestoredEvent` | Event | After a sector of ruined Earth first counts as restored, with its number and the new count |
| `void_reclaimer:planet_sectors` | Data component (Integer) | One bit per restored sector; any item carrying it is kept up to date only by `PlanetaryGridItem`, and the map layer shows it in either hand |
| `#void_reclaimer:dyson/satellite_payloads` | Item tag | Items the Mass Driver launches as one Dyson Swarm Satellite each |
| `#void_reclaimer:dyson/truss_payloads` | Item tag | Items the Mass Driver launches as one Dyson Shell Truss Module each |
| `#void_reclaimer:launch_frame` | Block tag | Blocks that may form the Launch Complex's ring (Scaffold, Portal Frame, Create's industrial iron block and andesite casing) |
| `#void_reclaimer:spire_frame` | Block tag | Blocks that may form a Spire tower's frame; by default it includes `#void_reclaimer:launch_frame` |
| `#void_reclaimer:spire/catalysts` | Item tag | What a Spire tower burns per pulse |
| `#void_reclaimer:spire/water`, `#void_reclaimer:spire/slurry` | Fluid tags | What fills a Spire tower's two tanks |

