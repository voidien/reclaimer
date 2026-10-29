# Art asset list

Everything the mod draws, as of `main` on 2026-10-29 (after Phase 8 and art drop 1), plus what the plan still needs. The list is for generating art with Retro Diffusion (RD); `assets.csv` next to this file has every row with its full prompt, ready for batch work.

## At a glance
| What | Count | Retro Diffusion? |
|---|---|---|
| Placeholder textures (programmer art, made by us) | 51 | Yes: 43 item icons + 8 block textures |
| Art drop 1 textures (redo if you want one consistent style) | 129 | Icons, block faces and HUD: yes. The 80 body skins: Blockbench |
| Module models missing for the body (5b) | 9 | No: Blockbench (RD for concept art) |
| New optional HUD/GUI/sky sprites (need a small code change to show) | 7 | Yes |
| Future-phase assets (Phases 7b, 9, 11–14, later tracks) | 20 | Textures yes, models Blockbench |

**What RD can and can't do here:** RD makes flat pixel images. Item icons, tileable block textures, block faces, HUD/GUI sprites and sky images are a good fit. 3D shapes are not: the GeckoLib rigs (`geo/*.geo.json`), block models and their UV-mapped skins are made in Blockbench. RD can still give concept art and material swatches for them.

## How to use it
1. Pick rows in `assets.csv` (filter by `status` and `method`).
2. Generate with the `prompt` column. For icons use RD's item style, for block textures its tiling option (best at 16x16 or 32x32), at the `size` shown.
3. Save as PNG at exactly the `file` path, at exactly the `size`. Same file name = no code change: the game picks it up.
4. Tell me when a batch is in: I'll check sizes and transparency in game, and drop the files from the placeholder list in `PROGRESS.md`.

Rows marked "code change needed" (GUI skins, the sun, dust meter icons, toast icon, logo) have no slot in the game yet. They need a small change from me before they show up.

## Style guide
- **Resolution (decided):** 16x16 for item icons and blocks, like vanilla and Create. HUD, GUI and sky sprites are larger (sizes per row). The energy bar must stay 90x9.
- **Look:** Create mod's industrial style: andesite grey, brass, copper, cogs, rivets. Cyan for the AI's glow (lights, cores, lenses).
- **Icons:** transparent background, light from the top-left, dark 1-pixel outline, at most about 16 colors.
- **Blocks:** tile seamlessly. The asteroid rocks cover huge surfaces, so they need extra care to avoid visible repeats.
- **Dust kind colors** (used for asteroids, raw materials and HUD; from the datapack):

| Kind | Color |
|---|---|
| Metallic | `#B8C2CC` |
| Silicate (Silicon) | `#D9B38C` |
| Ice | `#9FE2FF` |
| Carbon | `#6E6A80` |

- **Fluid colors** (the fluids tint vanilla water; the buckets should match): Purified Water `#A8DCFF`, Oxygen `#B4C8FF`, Hydrogen `#FFC8C8`, Nebulite Propellant `#FF9638`, Coolant `#78DCE6`, Liquid Silicon `#D0D8EE`.
- **Prompt prefixes** (the CSV's prompts already include them):
  - Icons: `Minecraft item icon, 16x16 pixel art, transparent background, light from top-left, dark 1px outline, Create mod industrial style;`
  - Tiling blocks: `seamless tileable 16x16 Minecraft block texture, pixel art, Create mod style;`
  - Single block faces: `16x16 Minecraft block face texture, pixel art, Create mod style, not tiled;`
- **Unfinished items:** the `incomplete_*` icons are the finished item with parts missing (Create's convention). Make them from the finished icon (image-to-image) so the pair matches.
- **Animated textures:** a vertical strip of 16x16 frames plus a `.png.mcmeta`. The Nanite Swarm already works this way (4 frames).

## About the art drop
Art drop 1 came from your private art repo (`cosmicvoiddweller/void-reclaimer-art`, branch `art/tier-c-pilot`). If you redo any of those files, change them there too, or the next drop will overwrite the new ones. The 80 body skins are painted to their rigs' UV layouts: open the `.geo.json` in Blockbench and paint over the template. The art repo's README defines the base and accent layers: the `_accent` file is a mask that the game tints with the player's accent color.

## The list

### Tools & modules (14)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| dust_sensor | `item/dust_sensor.png` | 16x16 | no | art drop 1 (redo optional) | handheld brass sensor with a round cyan glass lens and small antenna |  |
| nanite_swarm | `item/nanite_swarm.png` | 16x16 | no | art drop 1 (redo optional) | swirling cloud of tiny silver-cyan nanite specks around a small emitter | Animated in the art drop (nanite_swarm.png.mcmeta): a 16x64 vertical strip of 4 frames (3 ticks each). Generate 4 frames and stack them top to bottom. |
| salvage_tool | `item/salvage_tool.png` | 16x16 | no | art drop 1 (redo optional) | compact cutting tool with a copper handle and glowing orange cutting edge |  |
| mechanical_drill | `item/mechanical_drill.png` | 16x16 | no | placeholder | industrial drill module: andesite casing, brass collar, spiral steel drill bit |  |
| matter_harvester | `item/matter_harvester.png` | 16x16 | no | placeholder | mining module with a wide brass intake funnel and copper pipes, slower gentler cousin of a drill |  |
| body_blueprint | `item/body_blueprint.png` | 16x16 | no | art drop 1 (redo optional) | rolled blue engineering blueprint of a small robot drone, white line drawing |  |
| core_mk1 | `item/core_mk1.png` | 16x16 | no | art drop 1 (redo optional) | small round power core: andesite shell, glowing cyan center, cog teeth around the rim |  |
| core_mk2 | `item/core_mk2.png` | 16x16 | no | art drop 1 (redo optional) | larger power core: brass shell, bright white-cyan glowing center, two rings of cog teeth |  |
| thruster_mk1 | `item/thruster_mk1.png` | 16x16 | no | art drop 1 (redo optional) | small copper rocket thruster nozzle with an andesite mount |  |
| thruster_mk2 | `item/thruster_mk2.png` | 16x16 | no | art drop 1 (redo optional) | larger brass rocket thruster with a flared nozzle and a faint blue exhaust glow |  |
| storage_mk1 | `item/storage_mk1.png` | 16x16 | no | art drop 1 (redo optional) | small andesite storage pod with a hatch and brass latches |  |
| storage_mk2 | `item/storage_mk2.png` | 16x16 | no | art drop 1 (redo optional) | larger brass storage pod with two hatches and copper bands |  |
| brass_trim | `item/brass_trim.png` | 16x16 | no | art drop 1 (redo optional) | decorative curved brass trim plate with rivets (cosmetic part) |  |
| cogwheel_crown | `item/cogwheel_crown.png` | 16x16 | no | art drop 1 (redo optional) | ring of small brass cogwheels forming a crown (cosmetic part) |  |

### Body sections (30)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| drone_hull | `item/drone_hull.png` | 16x16 | no | art drop 1 (redo optional) | small round drone hull: andesite casing, cyan eye light, cog detail (item icon of a body part) |  |
| rotor_arm | `item/rotor_arm.png` | 16x16 | no | art drop 1 (redo optional) | thin andesite drone arm ending in a small spinning rotor (item icon of a body part) |  |
| rotor_arm_mk2 | `item/rotor_arm_mk2.png` | 16x16 | no | art drop 1 (redo optional) | reinforced brass drone arm with a larger ducted rotor (item icon of a body part) |  |
| sensor_dome | `item/sensor_dome.png` | 16x16 | no | art drop 1 (redo optional) | small glass sensor dome on an andesite base, cyan glow inside (item icon of a body part) |  |
| tool_claw | `item/tool_claw.png` | 16x16 | no | art drop 1 (redo optional) | small mechanical claw with three copper fingers (item icon of a body part) |  |
| mech_torso | `item/mech_torso.png` | 16x16 | no | art drop 1 (redo optional) | robot mech torso: andesite chest plate, brass trims, glowing cyan core window (item icon of a body part) |  |
| mech_pelvis | `item/mech_pelvis.png` | 16x16 | no | art drop 1 (redo optional) | robot mech hip piece, andesite with brass joints (item icon of a body part) |  |
| mech_head | `item/mech_head.png` | 16x16 | no | art drop 1 (redo optional) | robot mech head with a visor slit glowing cyan (item icon of a body part) |  |
| mech_upper_arm | `item/mech_upper_arm.png` | 16x16 | no | art drop 1 (redo optional) | robot upper arm segment, andesite with a brass shoulder joint (item icon of a body part) |  |
| mech_forearm | `item/mech_forearm.png` | 16x16 | no | art drop 1 (redo optional) | robot forearm segment, andesite with a cog at the elbow (item icon of a body part) |  |
| mech_forearm_mk2 | `item/mech_forearm_mk2.png` | 16x16 | no | art drop 1 (redo optional) | heavier robot forearm, brass plating and copper pistons (item icon of a body part) |  |
| mech_hand | `item/mech_hand.png` | 16x16 | no | art drop 1 (redo optional) | robot hand with three thick fingers, copper and andesite (item icon of a body part) |  |
| mech_thigh | `item/mech_thigh.png` | 16x16 | no | art drop 1 (redo optional) | robot thigh segment, andesite with a brass knee cap (item icon of a body part) |  |
| mech_shin | `item/mech_shin.png` | 16x16 | no | art drop 1 (redo optional) | robot shin segment, andesite with a piston (item icon of a body part) |  |
| mech_shin_mk2 | `item/mech_shin_mk2.png` | 16x16 | no | art drop 1 (redo optional) | heavier robot shin, brass armor and twin pistons (item icon of a body part) |  |
| mech_foot | `item/mech_foot.png` | 16x16 | no | art drop 1 (redo optional) | flat robot foot with small thruster vents (item icon of a body part) |  |
| ship_keel | `item/ship_keel.png` | 16x16 | no | art drop 1 (redo optional) | starship keel spine, long andesite beam with brass ribs (item icon of a body part) |  |
| ship_bow | `item/ship_bow.png` | 16x16 | no | art drop 1 (redo optional) | pointed starship bow section, brass nose with a cyan window strip (item icon of a body part) |  |
| ship_bridge | `item/ship_bridge.png` | 16x16 | no | art drop 1 (redo optional) | starship bridge module with a wide cyan glass canopy (item icon of a body part) |  |
| ship_cargo_bay | `item/ship_cargo_bay.png` | 16x16 | no | art drop 1 (redo optional) | boxy starship cargo bay with large hatch doors (item icon of a body part) |  |
| ship_dorsal_spine | `item/ship_dorsal_spine.png` | 16x16 | no | art drop 1 (redo optional) | starship dorsal spine with antennas and pipes (item icon of a body part) |  |
| ship_engine_nacelle | `item/ship_engine_nacelle.png` | 16x16 | no | art drop 1 (redo optional) | starship engine nacelle, copper nozzle and blue glow (item icon of a body part) |  |
| ship_engine_nacelle_mk2 | `item/ship_engine_nacelle_mk2.png` | 16x16 | no | art drop 1 (redo optional) | larger brass starship engine nacelle with twin nozzles (item icon of a body part) |  |
| ship_hardpoint | `item/ship_hardpoint.png` | 16x16 | no | art drop 1 (redo optional) | starship hardpoint mount, square brass socket for tools (item icon of a body part) |  |
| ship_hull_segment | `item/ship_hull_segment.png` | 16x16 | no | art drop 1 (redo optional) | starship hull segment, andesite plating panel (item icon of a body part) |  |
| ship_hull_segment_mk2 | `item/ship_hull_segment_mk2.png` | 16x16 | no | art drop 1 (redo optional) | reinforced starship hull segment, brass plating panel (item icon of a body part) |  |
| ship_rcs_cluster | `item/ship_rcs_cluster.png` | 16x16 | no | art drop 1 (redo optional) | cluster of four tiny maneuvering thruster nozzles (item icon of a body part) |  |
| ship_sensor_mast | `item/ship_sensor_mast.png` | 16x16 | no | art drop 1 (redo optional) | tall thin sensor mast with a cyan beacon on top (item icon of a body part) |  |
| ship_ventral_fin | `item/ship_ventral_fin.png` | 16x16 | no | art drop 1 (redo optional) | downward starship fin, andesite with brass edge (item icon of a body part) |  |
| ship_wing | `item/ship_wing.png` | 16x16 | no | art drop 1 (redo optional) | short starship wing with copper heat fins (item icon of a body part) |  |

### Materials (28)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| raw_ice_shards | `item/raw_ice_shards.png` | 16x16 | no | placeholder | pile of pale cyan ice shards, sharp crystal edges |  |
| carbon_flakes | `item/carbon_flakes.png` | 16x16 | no | placeholder | handful of dark grey-violet carbon flakes |  |
| silicon_grit | `item/silicon_grit.png` | 16x16 | no | placeholder | pile of sandy tan silicon grit granules |  |
| metallic_grains | `item/metallic_grains.png` | 16x16 | no | placeholder | pile of shiny silver-grey metal grains |  |
| crushed_cryo_ice | `item/crushed_cryo_ice.png` | 16x16 | no | placeholder | crushed frosty blue ice powder in a small heap |  |
| coolant_frame_plate | `item/coolant_frame_plate.png` | 16x16 | no | placeholder | square metal plate with frosty blue coolant channels |  |
| fine_carbon_powder | `item/fine_carbon_powder.png` | 16x16 | no | placeholder | small heap of fine black carbon powder |  |
| carbon_paste | `item/carbon_paste.png` | 16x16 | no | placeholder | blob of thick glossy black paste |  |
| carbon_composite_plate | `item/carbon_composite_plate.png` | 16x16 | no | placeholder | woven dark carbon-fiber plate with a sheen |  |
| silicon_carbide_slurry | `item/silicon_carbide_slurry.png` | 16x16 | no | placeholder | blob of grey-green gritty slurry |  |
| silicon_carbide_coating | `item/silicon_carbide_coating.png` | 16x16 | no | placeholder | thin dark grey-green ceramic coating sheet |  |
| thermal_shielding_tile | `item/thermal_shielding_tile.png` | 16x16 | no | placeholder | black ceramic heat shield tile with a white edge |  |
| high_purity_graphite | `item/high_purity_graphite.png` | 16x16 | no | placeholder | glossy dark graphite ingot |  |
| carbon_thread | `item/carbon_thread.png` | 16x16 | no | placeholder | spool of black carbon thread |  |
| pure_silicon_dust | `item/pure_silicon_dust.png` | 16x16 | no | placeholder | small heap of bright silver-white silicon dust |  |
| quartz_wafer | `item/quartz_wafer.png` | 16x16 | no | placeholder | thin round translucent quartz wafer disk |  |
| doped_silicon_ingot | `item/doped_silicon_ingot.png` | 16x16 | no | placeholder | silicon ingot with faint blue-violet doping streaks |  |
| high_efficiency_solar_cell | `item/high_efficiency_solar_cell.png` | 16x16 | no | placeholder | square dark blue solar cell with a silver grid |  |
| advanced_sensor_unit | `item/advanced_sensor_unit.png` | 16x16 | no | placeholder | small electronic sensor box with a lens and a green status light |  |
| refined_metal_nodule | `item/refined_metal_nodule.png` | 16x16 | no | placeholder | smooth round polished metal nodule |  |
| micro_alloy_sheet | `item/micro_alloy_sheet.png` | 16x16 | no | placeholder | thin flexible shiny alloy sheet, slightly curled |  |
| high_output_solar_sail | `item/high_output_solar_sail.png` | 16x16 | no | placeholder | folded gold-and-blue solar sail panel |  |
| zero_g_precision_frame | `item/zero_g_precision_frame.png` | 16x16 | no | placeholder | precise square metal frame with graphite struts |  |
| dyson_swarm_satellite_payload | `item/dyson_swarm_satellite_payload.png` | 16x16 | no | placeholder | compact satellite with folded solar panels on a frame |  |
| dyson_shell_truss_module | `item/dyson_shell_truss_module.png` | 16x16 | no | placeholder | heavy triangular truss segment with heat tiles |  |
| rcs_thruster | `item/rcs_thruster.png` | 16x16 | no | placeholder | small maneuvering thruster block with a nozzle and propellant line |  |
| reaction_wheel_gyroscope | `item/reaction_wheel_gyroscope.png` | 16x16 | no | placeholder | gyroscope with a spinning brass wheel inside a frame |  |
| atmospheric_catalyst_core | `item/atmospheric_catalyst_core.png` | 16x16 | no | placeholder | glowing green-blue crystal core inside a metal cage |  |

### Unfinished (Sequenced Assembly) (7)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| incomplete_zero_g_precision_frame | `item/incomplete_zero_g_precision_frame.png` | 16x16 | no | placeholder | half-built precise square metal frame with graphite struts, parts missing, exposed frame | Create convention: the finished icon with pieces missing. Generate from the finished icon (img2img) to keep them matched. |
| incomplete_dyson_swarm_satellite_payload | `item/incomplete_dyson_swarm_satellite_payload.png` | 16x16 | no | placeholder | half-built compact satellite with folded solar panels on a frame, parts missing, exposed frame | Create convention: the finished icon with pieces missing. Generate from the finished icon (img2img) to keep them matched. |
| incomplete_dyson_shell_truss_module | `item/incomplete_dyson_shell_truss_module.png` | 16x16 | no | placeholder | half-built heavy triangular truss segment with heat tiles, parts missing, exposed frame | Create convention: the finished icon with pieces missing. Generate from the finished icon (img2img) to keep them matched. |
| incomplete_rcs_thruster | `item/incomplete_rcs_thruster.png` | 16x16 | no | placeholder | half-built small maneuvering thruster block with a nozzle and propellant line, parts missing, exposed frame | Create convention: the finished icon with pieces missing. Generate from the finished icon (img2img) to keep them matched. |
| incomplete_reaction_wheel_gyroscope | `item/incomplete_reaction_wheel_gyroscope.png` | 16x16 | no | placeholder | half-built gyroscope with a spinning brass wheel inside a frame, parts missing, exposed frame | Create convention: the finished icon with pieces missing. Generate from the finished icon (img2img) to keep them matched. |
| incomplete_atmospheric_catalyst_core | `item/incomplete_atmospheric_catalyst_core.png` | 16x16 | no | placeholder | half-built glowing green-blue crystal core inside a metal cage, parts missing, exposed frame | Create convention: the finished icon with pieces missing. Generate from the finished icon (img2img) to keep them matched. |
| incomplete_high_output_solar_sail | `item/incomplete_high_output_solar_sail.png` | 16x16 | no | placeholder | half-built folded gold-and-blue solar sail panel, parts missing, exposed frame | Create convention: the finished icon with pieces missing. Generate from the finished icon (img2img) to keep them matched. |

### Fluid buckets (6)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| purified_water_bucket | `item/purified_water_bucket.png` | 16x16 | no | placeholder | vanilla-style iron bucket filled with clear light-blue water | Keep the vanilla bucket shape; only the contents change. Fluid tint in code: #A8DCFF. |
| oxygen_gas_bucket | `item/oxygen_gas_bucket.png` | 16x16 | no | placeholder | vanilla-style iron bucket filled with pale blue gas swirls | Keep the vanilla bucket shape; only the contents change. Fluid tint in code: #B4C8FF. |
| hydrogen_gas_bucket | `item/hydrogen_gas_bucket.png` | 16x16 | no | placeholder | vanilla-style iron bucket filled with pale pink gas swirls | Keep the vanilla bucket shape; only the contents change. Fluid tint in code: #FFC8C8. |
| nebulite_propellant_bucket | `item/nebulite_propellant_bucket.png` | 16x16 | no | placeholder | vanilla-style iron bucket filled with glowing orange propellant | Keep the vanilla bucket shape; only the contents change. Fluid tint in code: #FF9638. |
| coolant_fluid_bucket | `item/coolant_fluid_bucket.png` | 16x16 | no | placeholder | vanilla-style iron bucket filled with cyan coolant | Keep the vanilla bucket shape; only the contents change. Fluid tint in code: #78DCE6. |
| liquid_silicon_bucket | `item/liquid_silicon_bucket.png` | 16x16 | no | placeholder | vanilla-style iron bucket filled with glowing silver-white molten silicon | Keep the vanilla bucket shape; only the contents change. Fluid tint in code: #D0D8EE. |

### Blocks (11)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| asteroid_silicate | `block/asteroid_silicate.png` | 16x16 | yes | placeholder | sandy tan porous space rock | Dust kind color #D9B38C. Worlds have huge surfaces of it: it must tile without visible seams or repeats. |
| asteroid_ice | `block/asteroid_ice.png` | 16x16 | yes | placeholder | pale cyan frozen space rock with frost cracks | Dust kind color #9FE2FF. Worlds have huge surfaces of it: it must tile without visible seams or repeats. |
| asteroid_carbon | `block/asteroid_carbon.png` | 16x16 | yes | placeholder | dark grey-violet sooty space rock | Dust kind color #6E6A80. Worlds have huge surfaces of it: it must tile without visible seams or repeats. |
| asteroid_metallic | `block/asteroid_metallic.png` | 16x16 | yes | placeholder | silver-grey metallic space rock with shiny flecks | Dust kind color #B8C2CC. Worlds have huge surfaces of it: it must tile without visible seams or repeats. |
| scaffold | `block/scaffold.png` | 16x16 | yes | art drop 1 (redo optional) | light andesite metal scaffold grid with open gaps |  |
| backup_core | `block/backup_core.png` | 16x16 | no | art drop 1 (redo optional) | brass machine casing with a glowing cyan core window in the center | All six faces use this one texture now (cube_all). |
| tether_anchor | `block/tether_anchor.png` | 16x16 | no | art drop 1 (redo optional) | heavy andesite anchor block with a brass ring in the middle | All six faces use this one texture now. |
| electrolyzer_side | `block/electrolyzer_side.png` | 16x16 | no | placeholder | machine side: copper tank with glass window showing bubbling water |  |
| electrolyzer_end | `block/electrolyzer_end.png` | 16x16 | no | placeholder | machine end face: andesite casing with a shaft hole in the middle |  |
| spinner_side | `block/spinner_side.png` | 16x16 | no | placeholder | machine side: brass casing with a spinning spool of black thread visible |  |
| spinner_end | `block/spinner_end.png` | 16x16 | no | placeholder | machine end face: brass casing with a shaft hole in the middle |  |

### HUD & GUI (10)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| energy_full | `gui/sprites/hud/energy_full.png` | 90x9 | no | art drop 1 (redo optional) | pixel art HUD energy bar, 90x9, ten cyan battery cells in a row, full segment, transparent background | Replaces the hunger bar slot. Must stay exactly 90x9. |
| energy_half | `gui/sprites/hud/energy_half.png` | 90x9 | no | art drop 1 (redo optional) | pixel art HUD energy bar, 90x9, ten cyan battery cells in a row, half-filled segment, transparent background | Replaces the hunger bar slot. Must stay exactly 90x9. |
| energy_empty | `gui/sprites/hud/energy_empty.png` | 90x9 | no | art drop 1 (redo optional) | pixel art HUD energy bar, 90x9, ten cyan battery cells in a row, empty segment, transparent background | Replaces the hunger bar slot. Must stay exactly 90x9. |
| energy_empty_warning | `gui/sprites/hud/energy_empty_warning.png` | 90x9 | no | art drop 1 (redo optional) | pixel art HUD energy bar, 90x9, ten cyan battery cells in a row, empty segment flashing red warning, transparent background | Replaces the hunger bar slot. Must stay exactly 90x9. |
| replicator_panel | `gui/replicator_panel.png` | 256x256 | no | missing (future) | pixel art sci-fi machine GUI panel background, dark blue-grey with andesite metal frame and brass corners, empty inner areas | Not wired up yet: the Replicator and Body tab are drawn with flat fills. Needs a small code change to use a sprite (9-slice). |
| replicator_tabs | `gui/sprites/replicator/tab.png` | 48x20 | no | missing (future) | pixel art GUI tab button, andesite metal with brass edge, plus a highlighted variant | Same: code change needed. |
| module_wheel | `gui/sprites/hud/module_wheel.png` | 128x128 | no | missing (future) | pixel art radial selection wheel, six segments, dark translucent metal with cyan highlight | Same: code change needed. |
| dust_meter_icons | `gui/sprites/hud/dust_<kind>.png` | 9x9 | no | missing (future) | tiny pixel art icon of a dust grain cloud, one each for metallic, silicate, ice, carbon | Four files. Dust meter uses colored squares now; code change needed. |
| milestone_toast_icon | `gui/sprites/toast/milestone.png` | 16x16 | no | missing (future) | pixel art icon of a glowing AI core with a data spark | Toast shows no icon now; code change needed. |
| mod_logo | `logo.png` | 256x256 | no | missing (future) | pixel art game logo: "Void Reclaimer", a small robot drone silhouetted against a dim distant sun in black space | For neoforge.mods.toml logoFile (not set yet). |

### Sky (1)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| sun | `environment/sun.png` | 64x64 | no | missing (future) | pixel art distant pale star on transparent background, soft glow, small bright core | Deep space draws vanilla's sun texture now (DeepSpaceEffects). A custom one needs a one-line code change. |

### Body skins (GeckoLib) (80)

All 80 come from art drop 1 and are UV-mapped to their `.geo.json` rigs: paint them in Blockbench, not Retro Diffusion. Each part has a base texture and an `_accent` mask (tinted in code). Sizes: 128x128 for the Ship rig and ship parts, 64x64 for everything else. The full list is in `assets.csv`.

### Models (Blockbench) (9)

| Asset | File | Status | Notes |
|---|---|---|---|
| dust_sensor (on-body model) | `geo/module/dust_sensor.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |
| nanite_swarm (on-body model) | `geo/module/nanite_swarm.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |
| salvage_tool (on-body model) | `geo/module/salvage_tool.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |
| mechanical_drill (on-body model) | `geo/module/mechanical_drill.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |
| matter_harvester (on-body model) | `geo/module/matter_harvester.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |
| core_mk1 (on-body model) | `geo/module/core_mk1.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |
| core_mk2 (on-body model) | `geo/module/core_mk2.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |
| storage_mk1 (on-body model) | `geo/module/storage_mk1.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |
| storage_mk2 (on-body model) | `geo/module/storage_mk2.geo.json` | missing | Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden). |

### Future phases (20)

| Phase | Asset | What | Files (planned) | Prompt idea | Notes |
|---|---|---|---|---|---|
| 7b | space_thruster | block + model | `block/space_thruster_*.png` | Create-style space thruster block: andesite casing, copper bell nozzle on one face, shaft input on the back | Kinetic block (shaft in), pushes Aeronautics ships. Model in Blockbench; textures 16x16. |
| 7b | helm | block + model | `block/helm_*.png` | ship helm console: brass control panel with levers and a small cyan screen | Player sits/stands here to steer a block ship. |
| 9 | kinetic_mass_driver | multiblock (1x1x3) + model | `block/mass_driver_{base,barrel,muzzle}.png` | long electromagnetic launch rail: base with copper coils, barrel with brass rings, muzzle with a cyan glow | Launch sequence and animations planned: likely GeckoLib block entity (geo + animation). |
| 9 | quantum_receiver_dish | block + model | `block/quantum_receiver_*.png` | large receiver dish on a brass mount, cyan energy focus at the center | Big dish model; receives the sky power beams. |
| 9 | power_beam | sky/effect texture | `environment/power_beam.png` | vertical beam of soft cyan-white light, fading at the edges, 16x64 strip | Drawn from the Dyson swarm to the receiver. |
| 9 | dyson_swarm_sprites | sky texture | `environment/dyson_{swarm,lattice,shell}.png` | tiny satellites / lattice segments / shell panels around a sun, pixel art, transparent | DysonSky draws these procedurally now; textures optional. |
| 11 | ruined_earth sky | sky textures | `environment/ruined_*.png` | smoggy brown-orange sky, three eras: choked, clearing, healed | Three-era sky. |
| 11 | atmospheric_portal | block + portal surface | `block/atmospheric_portal_{frame,surface}.png` | portal frame of brass and andesite; swirling pale-blue atmosphere surface | Surface likely animated (vertical strip of 16x16 frames, like the nether portal). |
| 11 | ashen_slag | block | `block/ashen_slag.png` | grey-black ashen slag ground with cinders | Tileable 16x16. |
| 11 | acid_drizzle | weather texture | `environment/acid_rain.png` | sickly yellow-green rain streaks, vanilla rain layout 64x256 | Weather replacement texture. |
| 11 | drop_pod | entity model + texture | `entity/drop_pod.png` | scorched landing pod, brass ribs and heat shield underside | GeckoLib entity; texture is UV-mapped (Blockbench). |
| 11 | industrial_mech | body/entity | `entity/industrial_mech.png` | heavy industrial mech, copper and andesite, hazard stripes | Defined in the design doc; GeckoLib, UV-mapped. |
| 11 | planetary_grid_hud | GUI | `gui/planetary_grid.png` | pixel art 4x4 planetary sector map, dark screen with cyan grid lines | 256x256 GUI sprite. |
| 12 | terraforming_spire | multiblock + model | `block/spire_{base,column,emitter}.png` | tall terraforming spire: heavy base, ribbed column, glowing green emitter top | Multiblock; the emitter likely animated. |
| 12 | bio_nutrient_slurry | fluid + bucket | `item/bio_nutrient_slurry_bucket.png` | bucket of thick green bio-nutrient slurry | Fluid can reuse tinted water like the others. |
| 13 | data_monolith | block/multiblock + model | `block/data_monolith_*.png` | tall black monolith with faint cyan circuit lines and an output drawer | Design not settled in the doc yet. |
| 14 | restoration_hud | GUI | `gui/restoration.png` | pixel art planet restoration progress panel, green and blue | 256x256 GUI sprite. |
| later | helper_drone | entity model + texture | `entity/helper_drone.png` | small cargo drone with a claw and rotors, Create style | Stations track; GeckoLib, UV-mapped. |
| later | asteroid ores | blocks | `block/asteroid_<kind>_ore.png` | asteroid rock with embedded crystal veins, one per dust kind | Polish track: ore veins in asteroids. Four tileable textures. |
| later | cloud_collector | module item + model | `item/cloud_collector.png` | module with a wide scoop that pulls particles from a dust cloud | Raw-item collector part (polish track). |

