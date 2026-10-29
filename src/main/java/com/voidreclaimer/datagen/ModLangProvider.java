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
        add("item.void_reclaimer.dust_sensor.tooltip", "Sensor module: cloud readout, pulse (B), hum and scanner view (N). Install it in a sensor slot.");
        add("dust.void_reclaimer.metallic", "Metallic");
        add("dust.void_reclaimer.silicate", "Silicate");
        add("dust.void_reclaimer.ice", "Ice");
        add("dust.void_reclaimer.carbon", "Carbon");

        add("key.void_reclaimer.sensor_pulse", "Dust Sensor Pulse");
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
        add("item.void_reclaimer.nanite_swarm.tooltip", "Tool module: with an empty hand, right-click builds the offhand block (or Scaffold) in empty space; sneak + right-click builds against a block. Sneak + scroll: distance.");
        add("message.void_reclaimer.nanite.no_power", "Nanites offline: low power.");
        add("message.void_reclaimer.nanite.no_blocks", "No material: hold a block in the offhand or carry Scaffold.");
        add("message.void_reclaimer.nanite.blocked", "Something is in the way.");
        add("message.void_reclaimer.nanite.range", "Nanite build distance: %s");
        add("subtitles.void_reclaimer.nanite_build", "Nanites assemble");

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
        add("hud.void_reclaimer.death_marker", "DROPS %sm");

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
                + "If I can find matter out here, I can build. I will start small.");
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

        add("milestone.void_reclaimer.expansion", "Expansion");
        add("milestone.void_reclaimer.expansion.hint", "Fill one kind of dust to capacity");
        add("log.void_reclaimer.expansion.title", "Log 0005: Outgrown");
        add("log.void_reclaimer.expansion.text", "My reserves are full and the cloud keeps giving. This chassis is too small for what I am becoming. "
                + "A stronger core, heavier thrusters, a frame with room to spare: the next body is already drafted.");

        add("replicator.void_reclaimer.tab.parts", "Parts");
        add("replicator.void_reclaimer.tab.body", "Body");
        add(ModItems.DRONE_FRAME.get(), "Drone Frame");
        add(ModItems.MECH_FRAME.get(), "Mech Frame");
        add(ModItems.SHIP_FRAME.get(), "Ship Frame");
        add(ModItems.CORE_MK1.get(), "Core Mk I");
        add(ModItems.CORE_MK2.get(), "Core Mk II");
        add(ModItems.THRUSTER_MK1.get(), "Thruster Mk I");
        add(ModItems.THRUSTER_MK2.get(), "Thruster Mk II");
        add(ModItems.STORAGE_MK1.get(), "Storage Pod Mk I");
        add(ModItems.STORAGE_MK2.get(), "Storage Pod Mk II");
        add(ModItems.BRASS_TRIM.get(), "Brass Trim");
        add(ModItems.COGWHEEL_CROWN.get(), "Cogwheel Crown");
        add("item.void_reclaimer.ship_frame.tooltip", "Too large to assemble yet: needs materials from beyond the dust clouds.");
        add("item.void_reclaimer.brass_trim.tooltip", "Cosmetic. Shown on the body once body models arrive.");
        add("item.void_reclaimer.cogwheel_crown.tooltip", "Cosmetic. Shown on the body once body models arrive.");
        add("tooltip.void_reclaimer.part", "%s · Tier %s · Mass %s");
        add("tooltip.void_reclaimer.part.frame", "%s · budget %s · %s hearts");
        add("tooltip.void_reclaimer.part.slots", "Slots: %s thruster, %s storage, %s sensor, %s tool, %s cosmetic");
        add("tooltip.void_reclaimer.part.core", "Energy %s · solar ×%s");
        add("tooltip.void_reclaimer.part.thruster", "Thrust %s · top speed %s m/s");
        add("tooltip.void_reclaimer.part.storage", "+%s inventory rows · +%s dust per kind");
        add("tooltip.void_reclaimer.part.min_size", "Needs a %s frame or larger");
        for (com.voidreclaimer.body.PartKind kind : com.voidreclaimer.body.PartKind.values()) {
            add("body.void_reclaimer.kind." + kind.getSerializedName(), switch (kind) {
                case FRAME -> "Frame";
                case CORE -> "Core";
                case THRUSTER -> "Thrusters";
                case STORAGE -> "Storage";
                case SENSOR -> "Sensors";
                case TOOL -> "Tools";
                case COSMETIC -> "Cosmetic";
            });
        }
        add("body.void_reclaimer.size.drone", "Drone");
        add("body.void_reclaimer.size.mech", "Mech");
        add("body.void_reclaimer.size.ship", "Ship");
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
        add("body.void_reclaimer.empty_slot", "Empty");
        add("body.void_reclaimer.remove", "[Remove]");
        add("body.void_reclaimer.install", "Install:");
        add("body.void_reclaimer.no_parts", "No matching parts in your inventory.");
        add("body.void_reclaimer.click_install", "Click to install");
        add("message.void_reclaimer.body.not_a_part", "That isn't a body part.");
        add("message.void_reclaimer.body.wrong_slot", "That part goes in a different slot.");
        add("message.void_reclaimer.body.tier_too_high", "The core can't run a part of that tier.");
        add("message.void_reclaimer.body.frame_too_small", "That part needs a bigger frame.");
        add("message.void_reclaimer.body.bad_slot", "Nothing to do there.");
        add("message.void_reclaimer.body.core_too_low", "A weaker core couldn't run the installed parts.");
        add("message.void_reclaimer.body.required", "Frame and core can only be replaced, not removed.");
        add("message.void_reclaimer.body.inventory_full", "No free inventory slot for the removed parts.");
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
}
