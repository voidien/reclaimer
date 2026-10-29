# Void Reclaimer: art request sheet (for Retro Diffusion)

All textures are **16x16 RGBA PNG**, transparent background for items, opaque for blocks. Drop files into
`src/main/resources/assets/void_reclaimer/textures/{block,item}/` with the exact file name; no code change needed.
`current/` in `art-handoff/void_reclaimer_current_textures.zip` holds today's files (placeholders and real art) as img2img references.

**Style (matches art drop 1):** Create-mod look. Andesite grey casing, brass and copper trim, riveted panels, dark outlines, 3-4 shade ramps, a
single bright accent colour per machine. Space-industrial, not magical. Items face the upper-left light, 1px dark outline.

## Blocks
Blocks are plain cubes today (no custom 3D model). "Column" blocks use a `_side` texture on the 4 sides and `_end` on the 2 shaft ends (the shaft
sits in the centre of `_end`); "all" blocks use one texture on all 6 faces. Make every texture tile cleanly at the edges.

| File(s) | Type | What it is | Accent / prompt hint |
|---|---|---|---|
| `block/kinetic_drive_base_side`, `_end` | column | Bottom of the Mass Driver: heavy gearbox casing, big shaft in the end face | blue; "heavy riveted andesite gearbox, brass shaft, blue status strip" |
| `block/payload_chamber` | all | Middle of the Mass Driver: sealed cargo chamber with a hatch window | blue; "reinforced airlock hatch with a glowing blue slot window" |
| `block/magnetic_barrel_nozzle` | all | Top of the Mass Driver: coil-wound muzzle | blue; "copper magnetic coil rings around a dark bore" |
| `block/quantum_receiver_side`, `_end` | column | Sub-Light Quantum Receiver, generator fed by the Dyson Sphere | warm yellow-white; "dish receiver casing with gold collector panels, shaft end" |
| `block/electrolyzer_side`, `_end` | column | Water splitter | cyan |
| `block/spinner_side`, `_end` | column | Carbon thread spinner | orange |
| `block/asteroid_silicate` | all | Grey-violet silicate rock, speckled | |
| `block/asteroid_ice` | all | Pale blue-white ice rock, cracks | |
| `block/asteroid_carbon` | all | Near-black carbon rock, graphite sheen | |
| `block/asteroid_metallic` | all | Rust-brown rock with bright metal veins | |

Already real art (redo only if you want to): `scaffold`, `tether_anchor`, `backup_core`.

## Items still on placeholders
Nebula materials: `advanced_sensor_unit`, `carbon_composite_plate`, `carbon_flakes`, `carbon_paste`, `carbon_thread`, `coolant_frame_plate`,
`crushed_cryo_ice`, `doped_silicon_ingot`, `fine_carbon_powder`, `high_efficiency_solar_cell`, `high_output_solar_sail`, `high_purity_graphite`,
`metallic_grains`, `micro_alloy_sheet`, `pure_silicon_dust`, `quartz_wafer`, `raw_ice_shards`, `refined_metal_nodule`, `silicon_carbide_coating`,
`silicon_carbide_slurry`, `silicon_grit`, `thermal_shielding_tile`.
Colour families: Ice = pale cyan/white, Carbon = black/graphite grey, Silicon = blue-violet/silver, Metallic = copper-brown/steel.

Tools: `mechanical_drill`, `matter_harvester`.

Fluid buckets (vanilla bucket silhouette, coloured contents): `purified_water_bucket`, `oxygen_gas_bucket`, `hydrogen_gas_bucket`,
`nebulite_propellant_bucket`, `coolant_fluid_bucket`, `liquid_silicon_bucket`.

Phase 8/9 items (placeholders I generated): `zero_g_precision_frame`, `dyson_swarm_satellite_payload`, `dyson_shell_truss_module`, `rcs_thruster`,
`reaction_wheel_gyroscope`, `atmospheric_catalyst_core`, and the unfinished-assembly twins `incomplete_<same name>` plus `incomplete_quantum_receiver`
(use the finished item with a rough, half-built look: missing panels, exposed frame). Also `incomplete_high_output_solar_sail`.

The Quantum Receiver, Mass Driver blocks and Electrolyzer/Spinner also show as items; their item icon is rendered from the block model, so no
separate icon is needed.

## Real art you may want to redo
Everything else in `textures/item` (body sections, modules, sensor, nanites, salvage tool, cores) came from art drop 1.

## About 3D models
- Blocks: cubes only now. If you want real shapes (Mass Driver as three distinct models, Receiver as a dish), send Blockbench `.json` block models
  and I will wire them in (blockstate + model) with no gameplay change. Say which blocks.
- Body, sections and modules: GeckoLib models/animations are already imported from drop 1; they are not rendered until Phase 5b.
- Fluids use tinted vanilla water textures. `fluid/<name>_still.png` (16x16 x N animated strip + `.mcmeta`) would replace that.
