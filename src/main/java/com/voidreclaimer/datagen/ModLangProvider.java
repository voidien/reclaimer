package com.voidreclaimer.datagen;

import com.voidreclaimer.VoidReclaimer;
import com.voidreclaimer.dust.DustKind;
import com.voidreclaimer.registry.ModBlocks;
import com.voidreclaimer.registry.ModItems;
import com.voidreclaimer.replicator.Milestone;
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
        add("item.void_reclaimer.dust_sensor.tooltip", "Reads cosmic dust density and shows the current cloud's core and edge.");
        add("item.void_reclaimer.dust_sensor.tooltip.pulse", "Pulse (B) for a rough heading to the nearest cloud; listen for the hum.");
        for (DustKind kind : DustKind.values()) {
            add("dust.void_reclaimer." + kind.getSerializedName(), switch (kind) {
                case METALLIC -> "Metallic";
                case SILICATE -> "Silicate";
                case ICE -> "Ice";
                case CARBON -> "Carbon";
            });
        }

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

        add("commands.void_reclaimer.dust.get", "%s dust: metallic %s, silicate %s, ice %s, carbon %s");
        add("commands.void_reclaimer.dust.set", "Set %s's %s dust to %s");
        add("commands.void_reclaimer.dust.unknown_kind", "Unknown dust kind: %s");
        add("commands.void_reclaimer.dust.locate", "Nearest %s cloud: center %s, radius %s, edge %s blocks away");
        add("commands.void_reclaimer.dust.locate.none", "No dust cloud within %s blocks");
        add("commands.void_reclaimer.dust.no_field", "There are no dust clouds here");

        add(ModItems.SCAFFOLD.get(), "Scaffold");
        add(ModItems.SALVAGE_TOOL.get(), "Salvage Tool");

        add(ModItems.NANITE_SWARM.get(), "Nanite Swarm");
        add("item.void_reclaimer.nanite_swarm.tooltip", "Builds the offhand block (or Scaffold) in empty space ahead. Sneak + scroll: distance.");
        add("item.void_reclaimer.nanite_swarm.tooltip.range", "Build distance: %s blocks");
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
        add("commands.void_reclaimer.milestone.granted", "Granted %s to %s");
        add("commands.void_reclaimer.milestone.revoked", "Revoked %s from %s");
        add("commands.void_reclaimer.core.charged", "Filled %s Backup Core(s) of %s");
        add("gamerule.voidReclaimerStarterNebula", "Void Reclaimer: starter nebula");
        add("gamerule.voidReclaimerStarterNebula.description",
                "Places a small cloud of every dust kind near spawn. Only read when the world is created.");
        add("item.void_reclaimer.salvage_tool.tooltip", "Quickly dismantles things you built and recovers part of their dust. Can't cut natural rock.");

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
        add("milestone.void_reclaimer.first_structure.hint", "Place " + Milestone.STRUCTURE_BLOCKS + " blocks");
        add("log.void_reclaimer.first_structure.title", "Log 0003: Foothold");
        add("log.void_reclaimer.first_structure.text", "Ten blocks, bolted to nothing. It is the first thing out here that did not drift. "
                + "I could tie myself to it. A tether would let me work without losing my way back.");
        add("milestone.void_reclaimer.diversified", "Diversified");
        add("milestone.void_reclaimer.diversified.hint", "Hold three kinds of dust at once");
        add("log.void_reclaimer.diversified.title", "Log 0004: Redundancy");
        add("log.void_reclaimer.diversified.text", "Metal, glass, ice, carbon: enough variety to copy myself. "
                + "If this chassis fails, a backup core can hold my pattern and rebuild me where I left it.");

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
