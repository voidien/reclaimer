package com.voidreclaimer.datagen;

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
        add(ModItems.ASTEROID_CARBON.get(), "Carbon Asteroid Rock");
        add(ModItems.ASTEROID_METALLIC.get(), "Metallic Asteroid Rock");
        add("replicator.void_reclaimer.tab.materials", "Raw");
        add(ModItems.ELECTROLYZER.get(), "Electrolyzer");
        add(ModItems.SPINNER.get(), "Spinner");
        add(ModItems.CLOUD_SIPHON.get(), "Cloud Siphon");
        add(ModItems.KINETIC_DRIVE_BASE.get(), "Kinetic Drive Base");
        add(ModItems.PAYLOAD_CHAMBER.get(), "Payload Chamber");
        add(ModItems.MAGNETIC_BARREL_NOZZLE.get(), "Magnetic Barrel Nozzle");
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
        add("replicator.void_reclaimer.description.kinetic_drive_base", "The base of the Mass Driver. A turning shaft winds it up to launch Dyson payloads.");
        add("replicator.void_reclaimer.description.magnetic_barrel_nozzle", "The top of the Mass Driver stack: the barrel that payloads leave through.");
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
        add("replicator.void_reclaimer.description.payload_chamber", "The middle of the Mass Driver stack. It holds Dyson payloads until the driver launches them.");
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
