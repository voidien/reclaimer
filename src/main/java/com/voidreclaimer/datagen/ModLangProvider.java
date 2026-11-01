package com.voidreclaimer.datagen;

import java.util.List;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Milestones;
import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.common.data.LanguageProvider;

public final class ModLangProvider extends LanguageProvider {
    public ModLangProvider(PackOutput output) {
        super(output, VoidReclaimer.MOD_ID, "en_us");
    }

    @Override
    protected void addTranslations() {
        add("biome.void_reclaimer.deep_space", "Deep Space");

        add("hud.void_reclaimer.hover", "HOVER");
        add("hud.void_reclaimer.too_heavy", "TOO HEAVY: SINKING");
        add("hud.void_reclaimer.falling", "FALLING: DAMPENER OFF");
        add("message.void_reclaimer.dimension_locked", "Navigation error: no viable route to that destination.");
        add("message.void_reclaimer.no_planetary_anchor", "No planetary anchor detected. This device needs gravity to function.");
        add("message.void_reclaimer.low_power", "LOW POWER: thrusters, tools and replicator offline.");
        add("message.void_reclaimer.power_restored", "Power restored. Systems online.");

        add("message.void_reclaimer.dampener_on", "Inertial dampener: ON");
        add("message.void_reclaimer.dampener_off", "Inertial dampener: OFF. Momentum will carry you.");
        add("message.void_reclaimer.creative_thrusters_on", "Creative: thruster flight");
        add("message.void_reclaimer.creative_thrusters_off", "Creative: vanilla flight");
        add("message.void_reclaimer.creative_only", "Thruster flight mode can only be toggled in creative mode.");

        add("key.categories.void_reclaimer", "Void Reclaimer");
        add("key.void_reclaimer.toggle_dampener", "Toggle Inertial Dampener");
        add("key.void_reclaimer.toggle_creative_thrusters", "Toggle Thruster Flight (Creative)");

        add("hud.void_reclaimer.speed", "%s m/s");
        add("hud.void_reclaimer.dampener_on", "Dampener ON");
        add("hud.void_reclaimer.dampener_off", "Dampener OFF");
        add("hud.void_reclaimer.thrusters_offline", "THRUSTERS OFFLINE");

        add("itemGroup.void_reclaimer", "Void Reclaimer");
        add(ModItems.DUST_SENSOR.get(), "Dust Sensor");
        add(ModItems.CLOUD_COLLECTOR.get(), "Cloud Collector");
        add("item.void_reclaimer.cloud_collector.tooltip", "Tool-mount module: while you fly through a dust cloud it also makes raw items of the cloud's kind, one per dust gathered. Uses energy; stops in low power.");
        add("item.void_reclaimer.dust_sensor.tooltip", "Sensor module: cloud readout, pulse (B), hum and scanner view (N). Install it in a sensor slot.");
        add("dust.void_reclaimer.metallic", "Metallic");
        add("dust.void_reclaimer.silicate", "Silicate");
        add("dust.void_reclaimer.ice", "Ice");
        add("dust.void_reclaimer.carbon", "Carbon");

        add("key.void_reclaimer.sensor_pulse", "Dust Sensor Pulse");
        add("key.void_reclaimer.orbit_camera", "Orbit Camera (hold, third person)");
        add("key.void_reclaimer.assembly", "Body Assembly");
        add("key.void_reclaimer.photo_mode", "Photo Mode (drag look, WASD/Space/Shift move, scroll FOV, Q/E roll, G debug, L labels, O overlay, P freeze, period step, comma slow, T chat, Enter shot)");
        add("screen.void_reclaimer.photo_mode", "Photo mode");
        add("key.void_reclaimer.cloud_scanner", "Toggle Cloud Scanner");
        add("message.void_reclaimer.scanner_on", "Cloud scanner: ON");
        add("message.void_reclaimer.scanner_off", "Cloud scanner: OFF");
        add("hud.void_reclaimer.sensor.cloud", "%s cloud: %s%% density");
        add("hud.void_reclaimer.sensor.core", "CORE %sm");
        add("hud.void_reclaimer.sensor.edge", "EDGE %sm");
        add("hud.void_reclaimer.sensor.clear", "No dust detected");
        add("hud.void_reclaimer.sensor.signal", "Signal");
        add("hud.void_reclaimer.sensor.echo.none", "Pulse: no echo");
        add("hud.void_reclaimer.sensor.echo.faint", "~ faint echo");
        add("hud.void_reclaimer.sensor.echo.moderate", "~ echo");
        add("hud.void_reclaimer.sensor.echo.strong", "~ strong echo");
        add("message.void_reclaimer.sensor_missing", "No Dust Sensor installed.");
        add("message.void_reclaimer.sensor_cooldown", "Sensor recharging.");
        add("message.void_reclaimer.sensor_low_power", "Not enough power for a sensor pulse.");

        add("commands.void_reclaimer.dust.get", "%s dust: %s");
        add("commands.void_reclaimer.dust.amount", "%s %s");
        add("commands.void_reclaimer.dust.set", "Set %s's %s dust to %s");
        add("commands.void_reclaimer.dust.unknown_kind", "Unknown dust kind: %s");
        add("commands.void_reclaimer.dust.locate", "Nearest %s cloud: center %s, radius %s, edge %s blocks away");
        add("commands.void_reclaimer.dust.locate.none", "No dust cloud within %s blocks");
        add("commands.void_reclaimer.dust.no_field", "There are no dust clouds here");

        add(ModItems.SCAFFOLD.get(), "Scaffold");
        add(ModItems.SALVAGE_TOOL.get(), "Salvage Tool");

        add(ModItems.NANITE_SWARM.get(), "Nanite Swarm");
        add(ModItems.MECHANICAL_DRILL.get(), "Mechanical Drill");
        add(ModItems.MATTER_HARVESTER.get(), "Matter Harvester");
        add(ModItems.ASTEROID_SILICATE.get(), "Silicate Asteroid Rock");
        add(ModItems.ASTEROID_ICE.get(), "Ice Asteroid Rock");
        add(ModItems.ASHEN_SLAG.get(), "Ashen Slag");
        add(ModItems.PORTAL_FRAME.get(), "Atmospheric Portal Frame");
        add(ModItems.PORTAL_IGNITER.get(), "Portal Igniter");
        add(ModItems.CHEMICAL_THRUSTER.get(), "Chemical Thruster");
        add(ModItems.ION_THRUSTER.get(), "Ion Thruster");
        add(ModItems.HELM.get(), "Helm");
        add("entity.void_reclaimer.helm_seat", "Helm Seat");
        add(ModItems.STATION_CORE.get(), "Station Core");
        add(ModItems.HOLO_PROJECTOR.get(), "Holo Projector");
        add(ModItems.DOCKING_CLAMP.get(), "Docking Clamp");
        add("message.void_reclaimer.ship_portal.crossed", "The ship came through the portal.");
        add("message.void_reclaimer.ship_portal.too_big", "The ship does not fit through the gate.");
        add("message.void_reclaimer.ship_portal.blocked", "The far side of the portal is not clear for the ship.");
        add("message.void_reclaimer.ship_portal.no_way_back", "The portal leads nowhere for a ship.");
        add("message.void_reclaimer.clamp.docked", "Docked.");
        add("message.void_reclaimer.clamp.released", "Released.");
        add("message.void_reclaimer.clamp.not_on_ship", "The clamp must be built into a ship or station.");
        add("message.void_reclaimer.clamp.no_visitor", "No ship is touching the clamp.");
        add("message.void_reclaimer.clamp.misaligned", "The visiting ship must be level and square to the station.");
        add("message.void_reclaimer.clamp.blocked", "The visiting ship would overlap the station.");
        add("message.void_reclaimer.clamp.empty", "Nothing is docked.");
        add("message.void_reclaimer.clamp.failed", "The clamp could not do that.");
        add("message.void_reclaimer.clamp.idle", "The clamp is idle.");
        add("replicator.void_reclaimer.description.docking_clamp", "Build it into a station with its face outward. A ship that touches the face, level and square to the station, is docked while the clamp is powered (or on sneak-use): its blocks join the station and move with it. Power off, or sneak-use again, lets it go as its own ship.");
        add("container.void_reclaimer.station", "Station");
        add("hud.void_reclaimer.station_marker", "%s %sm");
        add("message.void_reclaimer.holo.source", "Showing: %s");
        add("commands.void_reclaimer.holo.not_a_projector", "No Holo Projector there.");
        add("holo_readout.void_reclaimer.clock", "Mission clock");
        add("holo_readout.void_reclaimer.helm", "Helm");
        add("holo_readout.void_reclaimer.thruster", "Thruster");
        add("holo_readout.void_reclaimer.siphon", "Cloud Siphon");
        add("holo_readout.void_reclaimer.electrolyzer", "Electrolyzer");
        add("holo_readout.void_reclaimer.spinner", "Spinner");
        add("holo_readout.void_reclaimer.launch_complex", "Launch Complex");
        add("holo_readout.void_reclaimer.dyson", "Dyson Sphere");
        add("holo_readout.void_reclaimer.spire", "Terraforming Spire");
        add("holo_readout.void_reclaimer.backup_core", "Backup Core");
        add("holo_readout.void_reclaimer.player", "Nearest body");
        add("holo_readout.void_reclaimer.cloud", "Dust cloud");
        add("holo_readout.void_reclaimer.device", "Device beside it");
        add("message.void_reclaimer.station.assembled", "Station %s assembled.");
        add("message.void_reclaimer.station.disassembled", "Station %s set down.");
        add("message.void_reclaimer.station.already_assembled", "This is already a station.");
        add("message.void_reclaimer.station.not_assembled", "This core is not part of a station.");
        add("message.void_reclaimer.station.too_big", "Too many connected blocks (the limit is %s).");
        add("message.void_reclaimer.station.nothing", "Nothing is connected to the core.");
        add("message.void_reclaimer.station.not_level", "The station must be level to set it down.");
        add("message.void_reclaimer.station.blocked", "The station cannot be set down here: something is in the way.");
        add("message.void_reclaimer.station.failed", "The station could not be assembled here.");
        add("message.void_reclaimer.launch.assembled", "Launch complex assembled.");
        add("message.void_reclaimer.launch.disassembled", "Launch complex set down.");
        add("message.void_reclaimer.launch.already_assembled", "The launch complex is already assembled.");
        add("message.void_reclaimer.launch.not_assembled", "The launch complex is not assembled.");
        add("message.void_reclaimer.launch.ring_incomplete", "The ring is incomplete: 15 x 15 of frame blocks, with the controller in the middle of one side.");
        add("message.void_reclaimer.launch.blocked", "Something is on the pad.");
        add("message.void_reclaimer.launch.failed", "The launch complex could not be assembled.");
        add("replicator.void_reclaimer.description.station_core", "The heart of a station. Build it into a hull and sneak-use it: everything connected becomes one floating station, named after the core. Right-click opens the station's chest. The Replicator works faster nearby, and a Backup Core aboard respawns you wherever the station has moved to.");
        add("key.void_reclaimer.toggle_rcs_lock", "Toggle RCS Lock (ship)");
        add("message.void_reclaimer.rcs_lock_on", "RCS lock: ON. The ship holds its heading.");
        add("message.void_reclaimer.rcs_lock_off", "RCS lock: OFF. The ship keeps spinning until you stop it.");
        add("key.void_reclaimer.roll_left", "Ship: Roll Left");
        add("key.void_reclaimer.roll_right", "Ship: Roll Right");
        add("key.void_reclaimer.pitch_up", "Ship: Nose Up");
        add("key.void_reclaimer.pitch_down", "Ship: Nose Down");
        add("key.void_reclaimer.yaw_left", "Ship: Yaw Left");
        add("key.void_reclaimer.yaw_right", "Ship: Yaw Right");
        add("hud.void_reclaimer.pilot.no_ship", "NOT ON A SHIP");
        add("hud.void_reclaimer.pilot.no_thrusters", "NO THRUSTERS");
        add("hud.void_reclaimer.pilot.rcs_on", "RCS lock ON");
        add("hud.void_reclaimer.pilot.rcs_off", "RCS lock OFF");
        add("hud.void_reclaimer.pilot.fuel", "FUEL %s/%s mB");
        add("hud.void_reclaimer.pilot.energy", "ENERGY %s/%s FE");
        add("hud.void_reclaimer.pilot.thrusters", "THR %s");
        add("hud.void_reclaimer.pilot.heading", "HDG %s");
        add("hud.void_reclaimer.pilot.tilt", "PIT %s ROL %s");
        add(ModItems.TERRAFORMING_SPIRE.get(), "Terraforming Spire");
        add("message.void_reclaimer.spire.started", "The tower is charging for a Genesis Pulse.");
        add("message.void_reclaimer.spire.no_catalyst", "The Spire needs an Atmospheric Catalyst Core.");
        add("message.void_reclaimer.spire.no_water", "The Spire's tank needs more Purified Water.");
        add("message.void_reclaimer.spire.no_slurry", "The Spire's tank needs more Bio-Nutrient Slurry.");
        add("message.void_reclaimer.spire.no_rotation", "The Spire's shaft is not turning fast enough to charge.");
        add("message.void_reclaimer.spire.busy", "A Genesis Pulse is already sweeping out from this Spire.");
        add("message.void_reclaimer.spire.wrong_world", "The Spire needs ruined ground to work on. Take it through the Atmospheric Portal.");
        add("message.void_reclaimer.spire.no_profile", "No terraform profile is loaded; is the datapack disabled?");
        add("message.void_reclaimer.spire.off_planet", "The tower stands outside the planet's sectors: there is no ground here to restore.");
        add("message.void_reclaimer.spire.sector_restored", "This sector is already restored. Raise a tower in another one.");
        add("message.void_reclaimer.spire.not_assembled", "The tower is not assembled. Sneak-use the Spire once its frame stands.");
        add("message.void_reclaimer.spire.assembled", "Spire tower assembled.");
        add("message.void_reclaimer.spire.disassembled", "Spire tower set down.");
        add("message.void_reclaimer.spire.already_assembled", "The tower is already assembled.");
        add("message.void_reclaimer.spire.frame_incomplete", "The frame is incomplete: a 5 x 5 base of frame blocks with the Spire in the middle of one side, and four corner pillars 9 blocks tall.");
        add("message.void_reclaimer.spire.blocked", "Something is inside the tower's frame.");
        add("message.void_reclaimer.spire.failed", "The tower could not be assembled.");
        add(ModBlocks.SPIRE_COLUMN.get(), "Spire Column");
        add(ModBlocks.ATMOSPHERIC_PORTAL.get(), "Atmospheric Portal");
        add(ModItems.ASTEROID_CARBON.get(), "Carbon Asteroid Rock");
        add(ModItems.ASTEROID_METALLIC.get(), "Metallic Asteroid Rock");
        add("replicator.void_reclaimer.tab.materials", "Raw");
        add(ModItems.ELECTROLYZER.get(), "Electrolyzer");
        add(ModItems.SPINNER.get(), "Spinner");
        add(ModItems.CLOUD_SIPHON.get(), "Cloud Siphon");
        add(ModItems.KINETIC_DRIVE_BASE.get(), "Kinetic Drive Base");
        add(ModItems.PAYLOAD_CHAMBER.get(), "Payload Chamber");
        add(ModItems.MAGNETIC_BARREL_NOZZLE.get(), "Magnetic Barrel Nozzle");
        add(ModItems.LAUNCH_CONTROLLER.get(), "Launch Controller");
        add(ModItems.HELPER_DRONE.get(), "Helper Drone");
        add(com.voidreclaimer.registry.ModEntities.HELPER_DRONE.get(), "Helper Drone");
        add(com.voidreclaimer.registry.ModEntities.BODY_HOST.get(), "Remote Shell");
        add("replicator.void_reclaimer.description.helper_drone", "A small flying worker. Sneak-use it on the inventory to take from (or on asteroid rock to mine near), then on the inventory to bring to, and release it: it ferries items 16 at a time, or digs the rock block by block and brings it. Sneak-use it in the air to set a collect point: released, it gathers the cloud's dust there and brings it to you. Sneak-use the drone with an empty hand to take it back.");
        add("tooltip.void_reclaimer.helper.source", "From: %s");
        add("tooltip.void_reclaimer.helper.destination", "To: %s");
        add("tooltip.void_reclaimer.helper.mine", "Mine near: %s");
        add("tooltip.void_reclaimer.helper.collect", "Collect dust at: %s (brought to you)");
        add("message.void_reclaimer.helper.collect_set", "Collect point set here: %s %s %s. Release the drone; it brings the cloud's dust to you.");
        add("message.void_reclaimer.helper.dust", "%s dust");
        add("message.void_reclaimer.helper.status.to_point", "Flying to its collect point. Holding %s.");
        add("message.void_reclaimer.helper.status.gathering", "Gathering dust. Holding %s.");
        add("message.void_reclaimer.helper.status.to_owner", "Bringing dust to its owner. Holding %s.");
        add("message.void_reclaimer.helper.mine_set", "Mine point set: %s %s %s. Sneak-use the inventory to bring the rock to.");
        add("message.void_reclaimer.helper.status.to_rock", "Flying to asteroid rock. Carrying %s.");
        add("message.void_reclaimer.helper.status.digging", "Digging. Carrying %s.");
        add("tooltip.void_reclaimer.helper.none", "not set (sneak-use on an inventory)");
        add("message.void_reclaimer.helper.source_set", "Source set: %s %s %s. Sneak-use the destination next.");
        add("message.void_reclaimer.helper.destination_set", "Destination set: %s %s %s. Release the drone.");
        add("message.void_reclaimer.helper.not_a_container", "That block holds no items and is not asteroid rock.");
        add("message.void_reclaimer.helper.picked_up", "Helper Drone taken back.");
        add("message.void_reclaimer.helper.empty", "nothing");
        add("message.void_reclaimer.helper.carrying", "%s x %s");
        add("message.void_reclaimer.helper.status.idle", "Idle: set both links and release it again. Carrying %s.");
        add("message.void_reclaimer.helper.status.to_source", "Flying to the source. Carrying %s.");
        add("message.void_reclaimer.helper.status.to_destination", "Flying to the destination. Carrying %s.");
        add("message.void_reclaimer.helper.status.waiting", "Waiting: nothing to take or no room to give. Carrying %s.");
        add(com.voidreclaimer.registry.ModBlocks.LAUNCH_PAD.get(), "Launch Pad");
        add(ModItems.QUANTUM_RECEIVER.get(), "Sub-Light Quantum Receiver");
        for (var material : com.voidreclaimer.registry.ModMaterials.ALL) {
            add(material.item().get(), material.name());
        }
        for (var incomplete : com.voidreclaimer.registry.ModMaterials.INCOMPLETE) {
            add(incomplete.item().get(), incomplete.name());
        }
        for (var fluid : com.voidreclaimer.registry.ModFluids.ALL) {
            add("fluid_type.void_reclaimer." + fluid.id(), fluid.name());
            add(fluid.bucket().get(), fluid.name() + " Bucket");
        }
        add("message.void_reclaimer.drill.needed", "This rock needs a powered Mechanical Drill or Matter Harvester.");
        add("item.void_reclaimer.nanite_swarm.tooltip", "Tool module: with an empty hand, right-click builds the offhand block (or Scaffold) in empty space; sneak + right-click builds against a block. Sneak + scroll: distance.");
        add("message.void_reclaimer.nanite.no_power", "Nanites offline: low power.");
        add("message.void_reclaimer.nanite.no_blocks", "No material: hold a block in the offhand or carry Scaffold.");
        add("message.void_reclaimer.nanite.blocked", "Something is in the way.");
        add("message.void_reclaimer.nanite.range", "Nanite build distance: %s");
        add("subtitles.void_reclaimer.nanite_build", "Nanites assemble");
        add("subtitles.void_reclaimer.mass_driver_launch", "Mass Driver fires");
        add("subtitles.void_reclaimer.acid_drizzle", "Acid Drizzle falls");

        add(ModBlocks.TETHER_ANCHOR.get(), "Tether Anchor");
        add("key.void_reclaimer.recall", "Tether Recall (hold)");
        add("message.void_reclaimer.tether.clipped", "Tether clipped on.");
        add("message.void_reclaimer.tether.unclipped", "Tether released. Anchor kept as recall point.");
        add("message.void_reclaimer.tether.lost", "Tether anchor lost.");
        add("message.void_reclaimer.tether.no_anchor", "No tether anchor registered.");
        add("message.void_reclaimer.tether.other_dimension", "Tether anchor is out of reach in another dimension.");
        add("message.void_reclaimer.tether.no_power", "Recall offline: not enough power.");
        add("message.void_reclaimer.tether.cooldown", "Recall teleport recharging: %s s.");
        add("message.void_reclaimer.tether.blocked", "No room to arrive at the anchor.");
        add("message.void_reclaimer.tether.teleported", "Recalled to anchor.");
        add("hud.void_reclaimer.tether.length", "TETHER %s/%sm");
        add("hud.void_reclaimer.tether.recalling", "RECALLING %sm");
        add("hud.void_reclaimer.tether.hold_recall", "RECALL %s%%");
        add("hud.void_reclaimer.tether.hold_teleport", "TELEPORT %s%%");

        add(ModBlocks.BACKUP_CORE.get(), "Backup Core");
        add("block.void_reclaimer.backup_core.default_name", "Core %s");
        add("message.void_reclaimer.core.limit", "Core limit reached (%s). Salvage one first.");
        add("message.void_reclaimer.core.not_owner", "This core belongs to another consciousness.");
        add("message.void_reclaimer.core.registered", "Backup Core registered.");
        add("message.void_reclaimer.core.full", "Core is fully charged.");
        add("message.void_reclaimer.core.unavailable", "Backup Core unavailable. Restoring at the deep space spawn.");
        add("screen.void_reclaimer.core.name", "Core name");
        add("screen.void_reclaimer.core.rename", "Rename");
        add("screen.void_reclaimer.core.charge", "Charge (M%s S%s)");
        add("screen.void_reclaimer.core.charges", "Charges");
        add("screen.void_reclaimer.core.sunlight", "Sunlight");
        add("screen.void_reclaimer.respawn.title", "Restore at:");
        add("screen.void_reclaimer.respawn.spawn", "Deep space spawn (free)");
        add("screen.void_reclaimer.respawn.core", "%s (%s)");
        add("screen.void_reclaimer.respawn.spawn.detail", "Always available");
        add("screen.void_reclaimer.respawn.distance", "%s m from here");
        add("screen.void_reclaimer.respawn.ai_line", "[Placeholder, lore text pending.] Signal lost. Restoring from the last backup.");
        add("hud.void_reclaimer.death_marker", "DROPS %sm");

        add("commands.void_reclaimer.sky.era.get", "Ruined Earth sky era: %s");
        add("commands.void_reclaimer.sky.era.set", "Ruined Earth sky era set to %s");
        add("commands.void_reclaimer.sky.era.unknown", "Unknown sky era '%s' (ruined, healing or restored)");
        add("commands.void_reclaimer.sky.sectors", "Ruined Earth: %s of 16 sectors restored");
        add("commands.void_reclaimer.sky.era.no_dimension", "The ruined_earth dimension is not loaded");
        add("commands.void_reclaimer.dyson.get", "Dyson Sphere: %s satellites, %s truss modules (shell complete at %s), skybox tier %s");
        add("commands.void_reclaimer.dyson.set", "Dyson Sphere: %s satellites, %s truss modules, skybox tier %s");
        add("commands.void_reclaimer.asteroid.locate", "Nearest asteroid: %s, %s, at %s %s %s, about %s blocks wide, %s blocks away");
        add("commands.void_reclaimer.asteroid.locate.none", "No asteroid within %s blocks");
        add("commands.void_reclaimer.asteroid.no_field", "Asteroids only exist in deep space");
        add("commands.void_reclaimer.asteroid.unknown_size", "Unknown size: %s (small, large or giant)");
        add("commands.void_reclaimer.milestone.unknown", "Unknown milestone: %s");
        add("commands.void_reclaimer.body.reset", "Reset %s to the starter drone");
        add("commands.void_reclaimer.body.get", "%s: %s, mass %s / %s, %s inventory slots, top speed %s m/s");
        add("commands.void_reclaimer.milestone.granted", "Granted %s to %s");
        add("commands.void_reclaimer.milestone.revoked", "Revoked %s from %s");
        add("commands.void_reclaimer.core.charged", "Filled %s Backup Core(s) of %s");
        add("gamerule.voidReclaimerStarterNebula", "Void Reclaimer: starter nebula");
        add("gamerule.voidReclaimerStarterNebula.description",
                "Places a small cloud of every dust kind near spawn. Only read when the world is created.");
        add("item.void_reclaimer.salvage_tool.tooltip", "Tool module: with an empty hand, quickly dismantles things you built and recovers part of their dust.");

        add("key.void_reclaimer.replicator", "Open Replicator");
        add("replicator.void_reclaimer.title", "Replicator");
        add("recipe.void_reclaimer.drone_kit", "Drone Kit");
        add("recipe.void_reclaimer.mech_core_kit", "Mech Core Kit");
        add("recipe.void_reclaimer.ship_core_kit", "Ship Core Kit");
        add("recipe.void_reclaimer.titan_core_kit", "Titan Core Kit");
        add("recipe.void_reclaimer.shuttle_core_kit", "Shuttle Core Kit");
        add("commands.void_reclaimer.body.preset", "%s now has a complete %s body");
        add("milestone.void_reclaimer.starship", "Starship");
        add("milestone.void_reclaimer.starship.hint", "Make a Coolant Frame Plate, a Carbon Composite Plate, a High-Efficiency Solar Cell and a Micro-Alloy Sheet");
        add("log.void_reclaimer.starship.title", "Log: The Starship");
        add("log.void_reclaimer.starship.text", "[Placeholder, lore text pending.] Ice, carbon, silicon and metal: one finished part from each. "
                + "The drafts for a ship-sized body are complete.");
        add("replicator.void_reclaimer.tab.tools", "Tools");
        add("replicator.void_reclaimer.tab.modules", "Modules");
        add("replicator.void_reclaimer.tab.blocks", "Blocks");
        add("replicator.void_reclaimer.tab.logs", "Logs");
        add("replicator.void_reclaimer.replicate", "Replicate");
        add("replicator.void_reclaimer.collect", "Collect output");
        add("commands.void_reclaimer.link.census", "Carried: %s items. Lying nearby: %s items.");
        add("commands.void_reclaimer.link.census.changed", "Changed since the last count: %s");
        add("commands.void_reclaimer.link.census.same", "Nothing changed since the last count.");
        add("commands.void_reclaimer.link.spawned", "Spawned a %s shell");
        add("commands.void_reclaimer.link.unknown", "No body called %s");
        add("commands.void_reclaimer.link.not_a_shell", "That is not a shell");
        add("commands.void_reclaimer.link.result", "Link: %s");
        add("commands.void_reclaimer.link.status.home", "In the main body");
        add("commands.void_reclaimer.link.status.linked", "In shell %s, link %s; main body in %s at %s");
        add("commands.void_reclaimer.scene.built", "Built test scene %s");
        add("commands.void_reclaimer.scene.unknown", "No test scene called %s");
        add("replicator.void_reclaimer.assembly", "3D view");
        add("assembly.void_reclaimer.title", "Body assembly");
        add("assembly.void_reclaimer.back", "Back");
        add("body.void_reclaimer.pair_hint", "Shift-click: both sides");
        add("body.void_reclaimer.can_queue", "Queue in the Replicator:");
        add("body.void_reclaimer.click_queue", "Click to queue in the Replicator");
        add("assembly.void_reclaimer.stat.mass", "Mass");
        add("assembly.void_reclaimer.stat.budget", "Mass budget");
        add("assembly.void_reclaimer.stat.acceleration", "Acceleration");
        add("assembly.void_reclaimer.stat.top_speed", "Top speed");
        add("assembly.void_reclaimer.stat.energy", "Energy");
        add("assembly.void_reclaimer.stat.solar", "Sun charging");
        add("assembly.void_reclaimer.stat.health", "Hearts");
        add("assembly.void_reclaimer.stat.reach", "Reach");
        add("assembly.void_reclaimer.stat.intake", "Dust intake");
        add("assembly.void_reclaimer.stat.dust", "Dust storage");
        add("assembly.void_reclaimer.stat.rows", "Inventory rows");
        add("assembly.void_reclaimer.stat.stability", "Stability");
        add("assembly.void_reclaimer.stat.turn_rate", "Turn rate");
        add("assembly.void_reclaimer.explode", "Exploded");
        add("assembly.void_reclaimer.grid", "Grid: %s blocks");
        add("assembly.void_reclaimer.grid_one", "Grid: 1 block");
        add("replicator.void_reclaimer.queue", "Queue %s/%s");
        add("replicator.void_reclaimer.output", "Output");
        add("replicator.void_reclaimer.paused", "PAUSED: low power");
        add("replicator.void_reclaimer.empty_tab", "No designs in this category yet.");
        add("replicator.void_reclaimer.no_logs", "No log entries.");
        add("replicator.void_reclaimer.drag_hint", "drag to reorder");
        add("menu.void_reclaimer.inventory", "Inventory");
        add("menu.void_reclaimer.inventory.dust", "Dust reserves");
        add("menu.void_reclaimer.inventory.locked", "Locked: install storage to unlock this row");
        add("replicator.void_reclaimer.tab.replicator", "Replicator");
        add("replicator.void_reclaimer.cost_tooltip", "%s: %s held, %s needed");
        add("replicator.void_reclaimer.select_hint", "Select a recipe to read what it does.");
        addReplicatorDescriptions();
        add("message.void_reclaimer.replicator.done", "Replicated: %s");
        add("message.void_reclaimer.replicator.locked", "That design is still locked.");
        add("message.void_reclaimer.replicator.queue_full", "Replicator queue is full.");
        add("message.void_reclaimer.replicator.not_enough_dust", "Not enough dust.");
        add("message.void_reclaimer.replicator.unknown_recipe", "Unknown design.");
        add("toast.void_reclaimer.log", "New log entry");
        add("jei.void_reclaimer.time", "%s s");
        add("jei.void_reclaimer.unlock", "Unlocked by: %s");

        add("milestone.void_reclaimer.awakening", "Awakening");
        add("milestone.void_reclaimer.awakening.hint", "Available from the start");
        add("log.void_reclaimer.awakening.title", "Log 0001: Boot");
        add("log.void_reclaimer.awakening.text", "Consciousness restored. Chassis: one micro-drone, no memory of how I got here. "
                + "Starlight on my cells, nothing within sensor range. The replicator core still answers. "
                + "If I can find matter out here, I can build. I will start small. "
                + "A little dust is still in my reserves: press R to open the replicator and build a Dust Sensor.");
        add("milestone.void_reclaimer.first_dust", "First Dust");
        add("milestone.void_reclaimer.first_dust.hint", "Collect any cosmic dust");
        add("log.void_reclaimer.first_dust.title", "Log 0002: Matter");
        add("log.void_reclaimer.first_dust.text", "Resistance against my hull, then grains in the intake. Dust. "
                + "Not much, but it is matter, and matter is everything. "
                + "Drafted a salvage tool so nothing I build is ever wasted, and a nanite swarm to build where nothing stands.");
        add("milestone.void_reclaimer.first_structure", "First Structure");
        add("milestone.void_reclaimer.first_structure.hint", "Place " + Milestones.STRUCTURE_BLOCKS + " blocks");
        add("log.void_reclaimer.first_structure.title", "Log 0003: Foothold");
        add("log.void_reclaimer.first_structure.text", "Ten blocks, bolted to nothing. It is the first thing out here that did not drift. "
                + "I could tie myself to it. A tether would let me work without losing my way back.");
        add("milestone.void_reclaimer.diversified", "Diversified");
        add("milestone.void_reclaimer.diversified.hint", "Hold three kinds of dust at once");
        add("log.void_reclaimer.diversified.title", "Log 0004: Redundancy");
        add("log.void_reclaimer.diversified.text", "Metal, glass, ice, carbon: enough variety to copy myself. "
                + "If this chassis fails, a backup core can hold my pattern and rebuild me where I left it.");

        add("milestone.void_reclaimer.earthfall", "Earthfall");
        add("milestone.void_reclaimer.earthfall.hint", "Step through the Atmospheric Portal onto ruined Earth");
        add("log.void_reclaimer.earthfall.title", "Log: Earthfall");
        add("log.void_reclaimer.earthfall.text", "[Placeholder, lore text pending.] Air. Weight. A horizon. The old world is under the ash, "
                + "and the air itself can carry a hull: propellers, envelopes and burners are in the drafts now.");
        add("replicator.void_reclaimer.description.aeronautics_wooden_propeller", "A small propeller for a shaft. Spins up in air and pushes the ship along its axis; useless in vacuum.");
        add("replicator.void_reclaimer.description.aeronautics_andesite_propeller", "A sturdier small propeller: more push per turn than wood.");
        add("replicator.void_reclaimer.description.aeronautics_smart_propeller", "A small propeller whose pitch follows a signal, for finer control.");
        add("replicator.void_reclaimer.description.aeronautics_propeller_bearing", "A bearing that assembles the blocks in front of it into a large propeller and spins them.");
        add("replicator.void_reclaimer.description.aeronautics_gyroscopic_propeller_bearing", "A propeller bearing that can tilt its propeller, for steering.");
        add("replicator.void_reclaimer.description.aeronautics_steam_vent", "Vents steam into an envelope above it for lift.");
        add("replicator.void_reclaimer.description.aeronautics_adjustable_burner", "Heats the air in an envelope above it; the signal sets how hard.");
        add("replicator.void_reclaimer.description.aeronautics_aviators_goggles", "Goggles for the pilot. They show what a hull carries and how it flies; mostly, they look the part.");
        add("replicator.void_reclaimer.description.aeronautics_end_stone_powder", "Ground end stone, the dust Aeronautics blends into levitite.");
        add("replicator.void_reclaimer.description.aeronautics_levitite", "A block that lifts: build it into a hull for buoyancy without an envelope.");
        add("replicator.void_reclaimer.description.aeronautics_pearlescent_levitite", "Refined levitite: more lift per block.");
        add("replicator.void_reclaimer.description.aeronautics_mounted_potato_cannon", "A potato cannon on a mount. There is nothing to shoot; it is a conversation piece.");
        for (String colour : List.of("white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
                "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black")) {
            add("replicator.void_reclaimer.description.aeronautics_" + colour + "_envelope", "Envelope fabric. Build a closed hull of it over a burner or steam vent and the hot air lifts the ship.");
            add("replicator.void_reclaimer.description.aeronautics_" + colour + "_envelope_encased_shaft", "A shaft through the envelope, so a motor inside can drive propellers outside.");
        }
        add("milestone.void_reclaimer.dyson_tier_1", "First Launches");
        add("milestone.void_reclaimer.dyson_tier_1.hint", "Raise the Dyson Sphere to sky tier 1");
        add("log.void_reclaimer.dyson_tier_1.title", "Dyson Sphere: First Launches");
        add("log.void_reclaimer.dyson_tier_1.text", "[Placeholder, lore text pending.] Dyson Sphere sky tier 1 reached.");
        add("milestone.void_reclaimer.dyson_tier_2", "Dense Swarm");
        add("milestone.void_reclaimer.dyson_tier_2.hint", "Raise the Dyson Sphere to sky tier 2");
        add("log.void_reclaimer.dyson_tier_2.title", "Dyson Sphere: Dense Swarm");
        add("log.void_reclaimer.dyson_tier_2.text", "[Placeholder, lore text pending.] Dyson Sphere sky tier 2 reached.");
        add("milestone.void_reclaimer.dyson_tier_3", "Structural Lattice");
        add("milestone.void_reclaimer.dyson_tier_3.hint", "Raise the Dyson Sphere to sky tier 3");
        add("log.void_reclaimer.dyson_tier_3.title", "Dyson Sphere: Structural Lattice");
        add("log.void_reclaimer.dyson_tier_3.text", "[Placeholder, lore text pending.] Dyson Sphere sky tier 3 reached.");
        add("milestone.void_reclaimer.dyson_tier_4", "Complete Shell");
        add("milestone.void_reclaimer.dyson_tier_4.hint", "Raise the Dyson Sphere to sky tier 4");
        add("log.void_reclaimer.dyson_tier_4.title", "Dyson Sphere: Complete Shell");
        add("log.void_reclaimer.dyson_tier_4.text", "[Placeholder, lore text pending.] Dyson Sphere sky tier 4 reached.");
        add("milestone.void_reclaimer.expansion", "Expansion");
        add("milestone.void_reclaimer.expansion.hint", "Fill one kind of dust to capacity");
        add("log.void_reclaimer.expansion.title", "Log 0005: Outgrown");
        add("log.void_reclaimer.expansion.text", "My reserves are full and the cloud keeps giving. This chassis is too small for what I am becoming. "
                + "A stronger core, heavier thrusters, a frame with room to spare: the next body is already drafted.");

        add("replicator.void_reclaimer.tab.parts", "Parts");
        add("replicator.void_reclaimer.tab.body", "Body");
        // ---- modules ----
        add(ModItems.CORE_MK1.get(), "Core Mk I");
        add(ModItems.CORE_MK2.get(), "Core Mk II");
        add(ModItems.CORE_MK3.get(), "Core Mk III");
        add(ModItems.CORE_MK4.get(), "Core Mk IV");
        add(ModItems.THRUSTER_MK1.get(), "Thruster Mk I");
        add(ModItems.THRUSTER_MK2.get(), "Thruster Mk II");
        add(ModItems.STORAGE_MK1.get(), "Storage Pod Mk I");
        add(ModItems.STORAGE_MK2.get(), "Storage Pod Mk II");
        add(ModItems.BRASS_TRIM.get(), "Brass Trim");
        add(ModItems.COGWHEEL_CROWN.get(), "Cogwheel Crown");
        add("item.void_reclaimer.brass_trim.tooltip", "Cosmetic. Shown on the body once body models arrive.");
        add("item.void_reclaimer.cogwheel_crown.tooltip", "Cosmetic. Shown on the body once body models arrive.");
        add("tooltip.void_reclaimer.module", "%s module · Tier %s · Mass %s");
        add("tooltip.void_reclaimer.module.core", "Energy %s · solar ×%s");
        add("tooltip.void_reclaimer.module.thruster", "Thrust %s · top speed %s m/s");
        add("tooltip.void_reclaimer.module.storage", "+%s inventory rows · +%s dust per kind");
        add("tooltip.void_reclaimer.module.min_size", "Needs a %s body or larger");
        add("body.void_reclaimer.kind.core", "Core");
        add("body.void_reclaimer.kind.thruster", "Thruster");
        add("body.void_reclaimer.kind.storage", "Storage");
        add("body.void_reclaimer.kind.sensor", "Sensor");
        add("body.void_reclaimer.kind.tool", "Tool");
        add("body.void_reclaimer.kind.cosmetic", "Cosmetic");

        // ---- sections ----
        add(ModItems.DRONE_HULL.get(), "Drone Hull");
        add(ModItems.SENSOR_DOME.get(), "Sensor Dome");
        add(ModItems.ROTOR_ARM.get(), "Rotor Arm");
        add(ModItems.ROTOR_ARM_MK2.get(), "Rotor Arm Mk II");
        add(ModItems.TOOL_CLAW.get(), "Tool Claw");
        add(ModItems.MECH_TORSO.get(), "Mech Torso");
        add(ModItems.MECH_PELVIS.get(), "Mech Pelvis");
        add(ModItems.MECH_HEAD.get(), "Mech Head");
        add(ModItems.MECH_UPPER_ARM.get(), "Mech Upper Arm");
        add(ModItems.MECH_FOREARM.get(), "Mech Forearm");
        add(ModItems.MECH_FOREARM_MK2.get(), "Mech Forearm Mk II");
        add(ModItems.MECH_HAND.get(), "Mech Hand");
        add(ModItems.MECH_THIGH.get(), "Mech Thigh");
        add(ModItems.MECH_SHIN.get(), "Mech Shin");
        add(ModItems.MECH_SHIN_MK2.get(), "Mech Shin Mk II");
        add(ModItems.MECH_FOOT.get(), "Mech Foot");
        add(ModItems.SHIP_KEEL.get(), "Ship Keel");
        add(ModItems.SHIP_BOW.get(), "Ship Bow");
        add(ModItems.SHIP_BRIDGE.get(), "Ship Bridge");
        add(ModItems.SHIP_HULL_SEGMENT.get(), "Hull Segment");
        add(ModItems.SHIP_HULL_SEGMENT_MK2.get(), "Hull Segment Mk II");
        add(ModItems.SHIP_WING.get(), "Ship Wing");
        add(ModItems.SHIP_ENGINE_NACELLE.get(), "Engine Nacelle");
        add(ModItems.SHIP_ENGINE_NACELLE_MK2.get(), "Engine Nacelle Mk II");
        add(ModItems.SHIP_HARDPOINT.get(), "Hardpoint");
        add(ModItems.SHIP_CARGO_BAY.get(), "Cargo Bay");
        add(ModItems.SHIP_SENSOR_MAST.get(), "Sensor Mast");
        add(ModItems.SHIP_RCS_CLUSTER.get(), "RCS Cluster");
        add(ModItems.SHIP_DORSAL_SPINE.get(), "Dorsal Spine");
        add(ModItems.SHIP_VENTRAL_FIN.get(), "Ventral Fin");
        add(ModItems.SHUTTLE_HULL.get(), "Shuttle Hull");
        add(ModItems.SHUTTLE_NOSE.get(), "Shuttle Nose");
        add(ModItems.SHUTTLE_WINDSHIELD.get(), "Shuttle Windshield");
        add(ModItems.SHUTTLE_DOME.get(), "Shuttle Dome");
        add(ModItems.SHUTTLE_RIDGE.get(), "Shuttle Ridge");
        add(ModItems.SHUTTLE_AFT.get(), "Shuttle Aft Section");
        add(ModItems.SHUTTLE_STRUT.get(), "Shuttle Strut");
        add(ModItems.SHUTTLE_NACELLE.get(), "Shuttle Nacelle");
        add("item.void_reclaimer.shuttle_hull.tooltip", "Core section of a Shuttle, a second Ship layout. Needs a Core Mk III.");
        add(ModItems.TITAN_SAUCER.get(), "Titan Saucer");
        add(ModItems.TITAN_NECK.get(), "Titan Neck");
        add(ModItems.TITAN_BRIDGE.get(), "Titan Bridge");
        add(ModItems.TITAN_SENSOR_ARRAY.get(), "Titan Sensor Array");
        add(ModItems.TITAN_DEFLECTOR.get(), "Titan Deflector");
        add(ModItems.TITAN_BAY_DOOR.get(), "Titan Bay Door");
        add(ModItems.TITAN_PYLON.get(), "Titan Pylon");
        add(ModItems.TITAN_NACELLE.get(), "Titan Nacelle");
        add("item.void_reclaimer.titan_saucer.tooltip", "Core section of a Titan, a ship over 60 blocks long. Needs a Core Mk IV.");
        add("item.void_reclaimer.drone_hull.tooltip", "Core section: installing it makes the body a Drone.");
        add("item.void_reclaimer.mech_torso.tooltip", "Core section: installing it makes the body a Mech.");
        add("item.void_reclaimer.ship_keel.tooltip", "Core section of a Ship. Needs a Core Mk III.");
        add("tooltip.void_reclaimer.section", "%s section · Tier %s · Mass %s");
        add("tooltip.void_reclaimer.section.core", "%s (core section) · Tier %s · Mass %s");
        add("tooltip.void_reclaimer.section.budget", "budget +%s");
        add("tooltip.void_reclaimer.section.health", "+%s hearts");
        add("tooltip.void_reclaimer.section.reach", "reach +%s");
        add("tooltip.void_reclaimer.section.intake", "intake +%s");
        add("tooltip.void_reclaimer.section.rows", "+%s rows");
        add("tooltip.void_reclaimer.section.stability", "stability +%s");
        add("tooltip.void_reclaimer.section.turn", "turn +%s");
        add("tooltip.void_reclaimer.section.wear", "wear resistance %s");
        add("tooltip.void_reclaimer.section.mounts", "Mounts:");
        add("tooltip.void_reclaimer.section.mount", "%s %s");
        for (var type : java.util.Map.ofEntries(
                java.util.Map.entry("drone_hull", "Drone hull"), java.util.Map.entry("sensor_dome", "Sensor dome"),
                java.util.Map.entry("rotor_arm", "Rotor arm"), java.util.Map.entry("tool_claw", "Tool claw"),
                java.util.Map.entry("mech_torso", "Mech torso"), java.util.Map.entry("pelvis", "Pelvis"), java.util.Map.entry("head", "Head"),
                java.util.Map.entry("upper_arm", "Upper arm"), java.util.Map.entry("forearm", "Forearm"), java.util.Map.entry("hand", "Hand"),
                java.util.Map.entry("thigh", "Thigh"), java.util.Map.entry("shin", "Shin"), java.util.Map.entry("foot", "Foot"),
                java.util.Map.entry("ship_keel", "Ship keel"), java.util.Map.entry("bow", "Bow"), java.util.Map.entry("bridge", "Bridge"),
                java.util.Map.entry("hull_segment", "Hull segment"), java.util.Map.entry("wing", "Wing"),
                java.util.Map.entry("engine_nacelle", "Engine nacelle"), java.util.Map.entry("hardpoint", "Hardpoint"),
                java.util.Map.entry("cargo_bay", "Cargo bay"), java.util.Map.entry("sensor_mast", "Sensor mast"),
                java.util.Map.entry("rcs_cluster", "RCS cluster"), java.util.Map.entry("dorsal_spine", "Dorsal spine"),
                java.util.Map.entry("ventral_fin", "Ventral fin")).entrySet()) {
            add("socket.void_reclaimer." + type.getKey(), type.getValue());
        }
        add("body.void_reclaimer.chassis.drone", "Drone");
        add("body.void_reclaimer.chassis.mech", "Mech");
        add("body.void_reclaimer.chassis.ship", "Ship");
        add("body.void_reclaimer.chassis.titan", "Titan");
        add("body.void_reclaimer.chassis.shuttle", "Shuttle");
        add("body.void_reclaimer.area.all", "All");
        add("body.void_reclaimer.area.core", "Core");
        add("body.void_reclaimer.area.sensors", "Sensors");
        add("body.void_reclaimer.area.propulsion", "Propulsion");
        add("body.void_reclaimer.area.tools", "Tools");
        add("body.void_reclaimer.area.head", "Head");
        add("body.void_reclaimer.area.back", "Back");
        add("body.void_reclaimer.area.arms", "Arms");
        add("body.void_reclaimer.area.legs", "Legs");
        add("body.void_reclaimer.area.hull", "Hull");
        add("body.void_reclaimer.area.control", "Control");
        add("body.void_reclaimer.area.weapons", "Hardpoints");
        add("body.void_reclaimer.area.cargo", "Cargo");
        for (String socket : java.util.List.of("hull", "sensor_dome", "tool_claw", "torso", "head", "pelvis", "back", "keel", "bow",
                "bridge", "wing_port", "wing_starboard", "mast_port", "mast_starboard", "rcs_fore", "rcs_aft", "dorsal_spine", "ventral_fin")) {
            add("body.void_reclaimer.socket." + socket, readable(socket));
        }
        for (String corner : java.util.List.of("front_left", "front_right", "back_left", "back_right")) {
            add("body.void_reclaimer.socket.rotor_" + corner, "Rotor, " + readable(corner).toLowerCase(java.util.Locale.ROOT));
        }
        for (String side : java.util.List.of("left", "right")) {
            for (String limb : java.util.List.of("upper_arm", "forearm", "hand", "thigh", "shin", "foot")) {
                add("body.void_reclaimer.socket." + side + "_" + limb, readable(side) + " " + readable(limb).toLowerCase(java.util.Locale.ROOT));
            }
        }
        for (int i = 1; i <= 6; i++) {
            add("body.void_reclaimer.socket.hull_" + i, "Hull segment " + i);
            add("body.void_reclaimer.socket.hardpoint_" + i, "Hardpoint " + i);
        }
        for (int i = 1; i <= 4; i++) {
            add("body.void_reclaimer.socket.nacelle_" + i, "Engine nacelle " + i);
            add("body.void_reclaimer.socket.cargo_" + i, "Cargo bay " + i);
        }
        add("body.void_reclaimer.size.drone", "Drone");
        add("body.void_reclaimer.size.mech", "Mech");
        add("body.void_reclaimer.size.ship", "Ship");
        add("body.void_reclaimer.size.shuttle", "Shuttle");
        add("body.void_reclaimer.size.titan", "Titan");
        add("body.void_reclaimer.casing.andesite", "Andesite");
        add("body.void_reclaimer.casing.brass", "Brass");
        add("body.void_reclaimer.casing.copper", "Copper");
        add("body.void_reclaimer.trail.none", "None");
        add("body.void_reclaimer.trail.standard", "Standard");
        add("body.void_reclaimer.trail.ember", "Ember");
        add("body.void_reclaimer.trail.plasma", "Plasma");
        add("body.void_reclaimer.trail.accent", "Accent");
        add("body.void_reclaimer.casing", "Casing: %s");
        add("body.void_reclaimer.trail", "Trail: %s");
        add("body.void_reclaimer.accent", "Accent");
        add("body.void_reclaimer.stats", "Body");
        add("body.void_reclaimer.stat.size", "%s (scale %s)");
        add("body.void_reclaimer.stat.mass", "Mass %s / %s");
        add("body.void_reclaimer.stat.flight", "Accel %s · %s m/s");
        add("body.void_reclaimer.stat.energy", "Energy %s · sun ×%s");
        add("body.void_reclaimer.stat.health", "%s hearts · reach +%s");
        add("body.void_reclaimer.stat.dust", "Dust %s · intake ×%s");
        add("body.void_reclaimer.stat.inventory", "Inventory %s/36");
        add("body.void_reclaimer.stat.tier", "Core tier %s");
        add("body.void_reclaimer.overloaded", "Over the mass budget: slower and hungrier.");
        add("body.void_reclaimer.empty_socket", "No section installed");
        add("body.void_reclaimer.empty_mount", "%s: empty");
        add("body.void_reclaimer.remove", "[Remove]");
        add("body.void_reclaimer.remove_module", "[Remove module]");
        add("body.void_reclaimer.install_section", "Install section:%s");
        add("body.void_reclaimer.install_module", "Install %s module:");
        add("body.void_reclaimer.no_parts", "Nothing matching in your inventory.");
        add("body.void_reclaimer.click_install", "Click to install");
        add("body.void_reclaimer.missing", "%s sections missing");
        add("body.void_reclaimer.complete", "All sections installed");
        add("message.void_reclaimer.body.not_a_section", "That isn't a body section.");
        add("message.void_reclaimer.body.not_a_module", "That isn't a module.");
        add("message.void_reclaimer.body.no_such_socket", "This body has no such socket.");
        add("message.void_reclaimer.body.wrong_socket", "That section doesn't fit this socket.");
        add("message.void_reclaimer.body.no_section", "Install a section there first.");
        add("message.void_reclaimer.body.no_such_mount", "That section has no such mount.");
        add("message.void_reclaimer.body.wrong_mount", "That module goes on a different mount.");
        add("message.void_reclaimer.body.tier_too_high", "The core can't run a part of that tier.");
        add("message.void_reclaimer.body.too_small", "That module needs a bigger body.");
        add("message.void_reclaimer.body.core_too_low", "A weaker core couldn't run the installed parts.");
        add("message.void_reclaimer.body.required", "The core section and core module can only be replaced, not removed.");
        add("message.void_reclaimer.body.empty", "Nothing there.");
        add("message.void_reclaimer.body.unknown_chassis", "That core section's layout isn't loaded.");
        add("message.void_reclaimer.body.inventory_full", "No free inventory slot for the parts that would come off.");
        add("message.void_reclaimer.body.returned", "Parts that no longer fit your body were returned to your inventory.");
        add("body.void_reclaimer.condition", "Condition %s%%");
        add("body.void_reclaimer.repair", "[Repair: %s]");
        add("body.void_reclaimer.paint", "Casing: %s");
        add("body.void_reclaimer.paint.default", "Body default");
        add("body.void_reclaimer.blueprint.save", "Save");
        add("body.void_reclaimer.blueprint.apply", "Apply");
        add("body.void_reclaimer.blueprint.queue", "Queue");
        add("message.void_reclaimer.body.not_worn", "That section isn't worn.");
        add("message.void_reclaimer.body.repair_dust", "Not enough dust to repair it.");
        add(ModItems.BODY_BLUEPRINT.get(), "Body Blueprint");
        add(ModItems.TEST_STARSHIP.get(), "Test Starship");
        add(com.voidreclaimer.registry.ModEntities.TEST_STARSHIP.get(), "Test Starship");
        add("commands.void_reclaimer.starship.spawned", "Spawned a Test Starship at %s");
        add("item.void_reclaimer.body_blueprint.empty", "Empty. Use it to save your current body.");
        add("item.void_reclaimer.body_blueprint.saved", "%s design · %s sections · %s modules");
        add("item.void_reclaimer.body_blueprint.usage", "Use to build it from your inventory; sneak-use to save over it.");
        add("message.void_reclaimer.blueprint.saved", "Body saved to the blueprint.");
        add("message.void_reclaimer.blueprint.applied", "Blueprint built: nothing missing.");
        add("message.void_reclaimer.blueprint.missing", "Blueprint built where possible. Missing: %s");
        add("message.void_reclaimer.blueprint.count", "%s× %s");
        add("message.void_reclaimer.blueprint.queued", "Queued %s Replicator jobs for missing parts.");
        add("key.void_reclaimer.module_wheel", "Module Wheel (hold)");
        add("screen.void_reclaimer.module_wheel", "Tool modules");

        add("subtitles.void_reclaimer.thruster", "Thrusters fire");
        add("subtitles.void_reclaimer.dampener", "Dampener hums");
        add("subtitles.void_reclaimer.sensor_pulse", "Sensor pulse");
        add("subtitles.void_reclaimer.sensor_echo", "Sensor echo");
        add("subtitles.void_reclaimer.sensor_hum", "Sensor tone");

        add("commands.void_reclaimer.energy.get", "%s: %s / %s energy (%s)");
        add("commands.void_reclaimer.energy.set", "Set energy of %s to %s");
        add("commands.void_reclaimer.energy.low_power", "low power");
        add("commands.void_reclaimer.energy.online", "online");
    }

    private static String readable(String id) {
        String text = id.replace('_', ' ');
        return Character.toUpperCase(text.charAt(0)) + text.substring(1);
    }

    /** One short gameplay hint per built-in Replicator recipe (not lore); a recipe's own {@code description} overrides it. */
    private void addReplicatorDescriptions() {
        add("replicator.void_reclaimer.description.portal_frame", "Frame blocks for an Atmospheric Portal. Build a flat rectangle at least 3 by 3 inside (the Ship needs about 12 by 6), then light it with a Portal Igniter.");
        add("replicator.void_reclaimer.description.holo_projector", "Shows up to eight lines of text in the air above itself. Write to it from a computer or a Create Display Link, or sneak-use it with an empty hand to pick a built-in readout.");
        add("replicator.void_reclaimer.description.portal_igniter", "Right-click a built Atmospheric Portal frame to light it. Used up when it works.");
        add("replicator.void_reclaimer.description.terraforming_spire", "The heart of a Spire tower. Build a 5 x 5 base of Scaffold or Portal Frame with this block in the middle of one side, with four corner pillars 9 tall, then sneak-use it. Feed it a Catalyst Core, Purified Water, Bio-Nutrient Slurry and rotation, and it restores one sector of ruined Earth.");
        add("replicator.void_reclaimer.description.chemical_thruster", "Pushes a block ship. Build it into the ship with the nozzle pointing away from where you want to go, turn it with a shaft, pipe in Nebulite Propellant, and open the throttle with redstone.");
        add("replicator.void_reclaimer.description.ion_thruster", "Pushes a block ship on Forge Energy. Weaker than a Chemical Thruster, but it needs no fuel and no shaft. Place it on a hull with the nozzle facing out, feed it electricity, and open the throttle with redstone.");
        add("replicator.void_reclaimer.description.helm", "The pilot's chair of a block ship. Build it in, sit down, and the movement keys fly the ship through its thrusters: WASD and Space/Shift move it, Q/E roll, the arrow keys pitch and yaw. The ship's nose is the way the chair faces.");
        add("replicator.void_reclaimer.description.backup_core", "A respawn point. Place it in deep space; after a death you can come back to it, spending one charge.");
        add("replicator.void_reclaimer.description.body_blueprint", "Saves your body's layout. Apply it later to queue the Replicator jobs for any parts you are missing.");
        add("replicator.void_reclaimer.description.brass_trim", "Cosmetic brass plating for a cosmetic mount. It changes how your body looks, not how it flies.");
        add("replicator.void_reclaimer.description.carbon_flakes", "Turns 8 carbon dust into 4 Carbon Flakes, a raw material for the Carbon chain in Create machines.");
        add("replicator.void_reclaimer.description.cloud_collector", "A tool-mount module that also gathers raw items while you fly through a dust cloud. Uses a little energy.");
        add("replicator.void_reclaimer.description.cloud_siphon", "A Create machine. Turned by a shaft inside a dense dust cloud, it condenses the cloud into raw items.");
        add("replicator.void_reclaimer.description.cogwheel_crown", "Cosmetic cogwheel crown for a cosmetic mount. It changes how your body looks, not how it flies.");
        add("replicator.void_reclaimer.description.core_mk1", "Your power core: stores energy and charges from sunlight. Every body needs one.");
        add("replicator.void_reclaimer.description.core_mk2", "A larger core with more energy and faster solar charging. Lets you install tier 2 parts.");
        add("replicator.void_reclaimer.description.core_mk3", "A core with far more energy. Required before the tier 3 Ship and Shuttle parts can be installed.");
        add("replicator.void_reclaimer.description.core_mk4", "The best core. Required for the tier 4 Titan parts.");
        add("replicator.void_reclaimer.description.drone_hull", "The hull of a Drone. Every other Drone part attaches to it, and it holds your core and storage.");
        add("replicator.void_reclaimer.description.drone_kit", "A whole starting Drone: hull, sensor dome, claw and four rotor arms, cheaper than buying them one by one.");
        add("replicator.void_reclaimer.description.dust_sensor", "Shows the dust cloud you are in and sends pulses that help you find clouds. Install it in a sensor slot.");
        add("replicator.void_reclaimer.description.electrolyzer", "A Create machine that splits Purified Water into Oxygen and Hydrogen gas when a shaft turns it.");
        add("replicator.void_reclaimer.description.kinetic_drive_base", "The base of the old three-block Mass Driver. Inert: the Launch Complex launches payloads now.");
        add("replicator.void_reclaimer.description.launch_controller", "The heart of the Launch Complex. Build a 15 x 15 ring of Scaffold or Portal Frame with this block in the middle of one side, facing the pad, and sneak-use it to assemble. A turning shaft on its back winds it up to launch Dyson payloads.");
        add("replicator.void_reclaimer.description.magnetic_barrel_nozzle", "The top of the old three-block Mass Driver. Inert: the Launch Complex launches payloads now.");
        add("replicator.void_reclaimer.description.matter_harvester", "A tool module that mines asteroid rock slowly, but lets the blocks drop as items.");
        add("replicator.void_reclaimer.description.mech_core_kit", "The three main Mech pieces, torso, pelvis and head, cheaper as a set.");
        add("replicator.void_reclaimer.description.mech_foot", "A Mech foot. It can carry a thruster module.");
        add("replicator.void_reclaimer.description.mech_forearm", "A Mech forearm. Attach it between the upper arm and the hand.");
        add("replicator.void_reclaimer.description.mech_forearm_mk2", "A sturdier Mech forearm with a tool mount of its own.");
        add("replicator.void_reclaimer.description.mech_hand", "A Mech hand. It carries a tool module.");
        add("replicator.void_reclaimer.description.mech_head", "A Mech head with two sensor mounts.");
        add("replicator.void_reclaimer.description.mech_pelvis", "The Mech's pelvis. The legs attach to it.");
        add("replicator.void_reclaimer.description.mech_shin", "A Mech shin. It can carry a thruster module.");
        add("replicator.void_reclaimer.description.mech_shin_mk2", "A sturdier Mech shin that also carries a thruster module.");
        add("replicator.void_reclaimer.description.mech_thigh", "A Mech thigh. Attach it between the pelvis and the shin.");
        add("replicator.void_reclaimer.description.mech_torso", "The core of a Mech body. It holds your power core and storage, and every other Mech piece attaches to it.");
        add("replicator.void_reclaimer.description.mech_upper_arm", "A Mech upper arm. Attach it to the torso, then add a forearm.");
        add("replicator.void_reclaimer.description.mechanical_drill", "A tool module that breaks asteroid rock quickly and gives back its dust. You need it to mine asteroids.");
        add("replicator.void_reclaimer.description.metallic_grains", "Turns 8 metallic dust into 4 Metallic Grains, a raw material for the Metallic chain in Create machines.");
        add("replicator.void_reclaimer.description.nanite_swarm", "Builds blocks for you in empty space in front of you, for a little energy each.");
        add("replicator.void_reclaimer.description.payload_chamber", "The middle of the old three-block Mass Driver. Inert: load payloads into the Launch Controller instead.");
        add("replicator.void_reclaimer.description.raw_ice_shards", "Turns 8 ice dust into 4 Raw Ice Shards, a raw material for the Ice chain in Create machines.");
        add("replicator.void_reclaimer.description.rotor_arm", "A Drone arm that carries a thruster module. Attach it to the hull.");
        add("replicator.void_reclaimer.description.rotor_arm_mk2", "A better Drone rotor arm: lighter, tougher and turns the Drone faster.");
        add("replicator.void_reclaimer.description.salvage_tool", "Breaks salvageable blocks without drops and gives back half of their dust.");
        add("replicator.void_reclaimer.description.scaffold", "Eight cheap building blocks. Place them by hand or build with them using the Nanite Swarm.");
        add("replicator.void_reclaimer.description.sensor_dome", "A dome on the Drone with a mount for one sensor module, such as the Dust Sensor.");
        add("replicator.void_reclaimer.description.ship_bow", "The front of a Ship. Adds a little hull strength and dust intake.");
        add("replicator.void_reclaimer.description.ship_bridge", "The Ship's bridge. It extends your reach and has a sensor mount.");
        add("replicator.void_reclaimer.description.ship_cargo_bay", "A Ship cargo bay with a storage mount for more room.");
        add("replicator.void_reclaimer.description.ship_core_kit", "The three main Ship pieces: keel, bridge and bow, cheaper as a set.");
        add("replicator.void_reclaimer.description.ship_dorsal_spine", "A spine along the top of the Ship. It adds hull strength and a cosmetic mount.");
        add("replicator.void_reclaimer.description.ship_engine_nacelle", "A Ship engine pod with two thruster mounts.");
        add("replicator.void_reclaimer.description.ship_engine_nacelle_mk2", "A lighter, tougher engine pod with two thruster mounts.");
        add("replicator.void_reclaimer.description.ship_hardpoint", "A Ship hardpoint with a tool mount.");
        add("replicator.void_reclaimer.description.ship_hull_segment", "A hull segment. It adds mass budget and strength to the Ship.");
        add("replicator.void_reclaimer.description.ship_hull_segment_mk2", "A better hull segment with more strength and a storage mount.");
        add("replicator.void_reclaimer.description.ship_keel", "The core of a Ship body. It holds your power core and storage; every other Ship piece attaches to it.");
        add("replicator.void_reclaimer.description.ship_rcs_cluster", "Small thrusters that help the Ship turn faster.");
        add("replicator.void_reclaimer.description.ship_sensor_mast", "A mast with a sensor mount that also extends your reach.");
        add("replicator.void_reclaimer.description.ship_ventral_fin", "A fin under the Ship. It adds stability and strength, and has a cosmetic mount.");
        add("replicator.void_reclaimer.description.ship_wing", "A Ship wing. It helps the Ship turn and has a cosmetic mount.");
        add("replicator.void_reclaimer.description.shuttle_aft", "The rear of a Shuttle, with a storage mount.");
        add("replicator.void_reclaimer.description.shuttle_core_kit", "The three main Shuttle pieces: hull, windshield and nose, cheaper as a set.");
        add("replicator.void_reclaimer.description.shuttle_dome", "A dome on the Shuttle with a sensor mount.");
        add("replicator.void_reclaimer.description.shuttle_hull", "The core of a Shuttle body. It holds your power core and storage; every other Shuttle piece attaches to it.");
        add("replicator.void_reclaimer.description.shuttle_nacelle", "A Shuttle engine pod with two thruster mounts.");
        add("replicator.void_reclaimer.description.shuttle_nose", "The nose of a Shuttle.");
        add("replicator.void_reclaimer.description.shuttle_ridge", "A ridge along the top of the Shuttle, with a cosmetic mount.");
        add("replicator.void_reclaimer.description.shuttle_strut", "A strut that joins a Shuttle engine pod to the hull.");
        add("replicator.void_reclaimer.description.shuttle_windshield", "The Shuttle's windshield, with a sensor mount.");
        add("replicator.void_reclaimer.description.silicon_grit", "Turns 8 silicate dust into 4 Silicon Grit, a raw material for the Silicon chain in Create machines.");
        add("replicator.void_reclaimer.description.spinner", "A Create machine that spins High-Purity Graphite into Carbon Thread when a shaft turns it.");
        add("replicator.void_reclaimer.description.storage_mk1", "A storage pod. It adds inventory rows and dust capacity.");
        add("replicator.void_reclaimer.description.storage_mk2", "A larger storage pod with more inventory rows and dust capacity.");
        add("replicator.void_reclaimer.description.tether_anchor", "A block you can clip onto. It keeps you from drifting far away, and holding G recalls you to it.");
        add("replicator.void_reclaimer.description.thruster_mk1", "The basic thruster module. It gives your body thrust to fly.");
        add("replicator.void_reclaimer.description.thruster_mk2", "A stronger thruster module with more thrust.");
        add("replicator.void_reclaimer.description.titan_bay_door", "A Titan bay door with two storage mounts.");
        add("replicator.void_reclaimer.description.titan_bridge", "The Titan's bridge, with a sensor mount.");
        add("replicator.void_reclaimer.description.titan_core_kit", "The three main Titan pieces: saucer, neck and bridge, cheaper as a set.");
        add("replicator.void_reclaimer.description.titan_deflector", "The Titan's front deflector.");
        add("replicator.void_reclaimer.description.titan_nacelle", "A Titan engine pod with three thruster mounts.");
        add("replicator.void_reclaimer.description.titan_neck", "The neck that joins the Titan's bridge to its saucer.");
        add("replicator.void_reclaimer.description.titan_pylon", "A pylon that carries a Titan engine pod.");
        add("replicator.void_reclaimer.description.titan_saucer", "The core of a Titan body. It holds your power core and storage; every other Titan piece attaches to it.");
        add("replicator.void_reclaimer.description.titan_sensor_array", "A Titan sensor array with two sensor mounts.");
        add("replicator.void_reclaimer.description.tool_claw", "A claw for the Drone with two tool mounts.");
    }
}
