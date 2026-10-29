# Art asset list

Everything the mod draws, as of `main` on 2026-10-29 (after Phase 8 and art drop 1), plus what the plan still needs. The list is for generating art with Retro Diffusion (RD); `assets.csv` next to this file has every row with its full prompt, ready for batch work.

## At a glance
| What | Count | How |
|---|---|---|
| Placeholder textures (programmer art, made by us) | 51 | RD image: 43 item icons + 8 block textures |
| Art drop 1 icons, block faces, HUD sprites (redo for one consistent style) | 49 | RD image |
| Art drop 1 3D models: 3 body rigs, 3 first-person rigs, 30 sections, 4 modules | 40 | RD 3D (blocky), then fitted to our bone names |
| Art drop 1 body animations | 16 | RD 3D animate, then renamed and retargeted |
| Art drop 1 body skins (80: a base texture and an accent mask per model) | 80 | Base comes with the RD 3D model; accent masks painted in Blockbench |
| Module models missing for the body (5b) | 9 | RD 3D (blocky) |
| New optional HUD/GUI/sky sprites (need a small code change to show) | 7 | RD image |
| Future-phase assets (Phases 7b, 9, 11–14, later tracks) | 20 | Mixed, per row |

## Retro Diffusion's modes used here
- **RD image:** pixel images. Item icons, tileable block textures (its tiling option is best at 16x16 or 32x32), block faces, HUD/GUI and sky sprites.
- **RD image, VFX/animation:** sprite-sheet animations. Used for the Nanite Swarm icon, the portal surface and the power beam. Minecraft wants the frames stacked in one vertical strip.
- **RD 3D, Blocky or Blocky Lite** (`rd_lowpoly__blocky`, `rd_lowpoly__blocky_lite`): boxes only, with pixel-art textures. Lite is $0.10 a model with simpler builds, good for drafts; Blocky costs $0.25–3.00 by size. The Detailed styles add cylinders and spheres, which GeckoLib and Minecraft can't use. It exports to Blockbench (`.bbmodel`, with animations) and as a Minecraft Java resource pack.
  - Static blocks (Space Thruster, Helm, Electrolyzer, Spinner, Monolith): use the **Java** export.
  - Anything rigged or animated (body parts, Mass Driver, Spire, drop pod, helper drone): use **`.bbmodel`**, then convert to GeckoLib.
- **RD 3D animate:** animations from a prompt. The first one rigs the model automatically. They come along in the `.bbmodel` export.

Costs from RD's API page: about $0.25–3.00 per model, $0.20–1.50 per revision and $0.15–1.00 per animation. Its estimate endpoint is free.

## 3D workflow (GeckoLib parts and rigs)
The body is built in code from separate part models. Each part attaches to its parent by bone name, so a model only works if it keeps the **socket contract**. The art drop already meets it; a regenerated part must too.
1. **Generate** in RD 3D, blocky style, with the row's prompt. Up to 4 reference images help: the current item icon, or the old model's turntable.
2. **Export `.bbmodel`** and open it in Blockbench. Convert it to a GeckoLib model with the GeckoLib plugin.
3. **Rename bones** to the row's "Bones to keep" list. Line up their pivots with the old model's. Easiest: open the old `.geo.json` beside the new one and copy the pivots.
   - The pattern is `<part>` (root), `<part>_casing`, `<part>_frame`, `cog_internal`, `cap_<socket>` and `mount_<kind>_<n>`.
   - Whole rigs name their sockets after the chassis JSON (`hull`, `rotor_front_left`, ...).
4. **Texture size:** keep 64x64 (128x128 for the Ship and ship parts), or tell me the new size.
   - RD's `size` setting (16–256 texels) controls how fine the texture is. Aim for Minecraft's density of 16 texels per block.
   - Then paint the `_accent` mask on top (white = tinted with the player's accent color).
5. **Animations** keep their exact names: `animation.void_reclaimer.<chassis>.<idle|thrust|brake|low_power|module_work|rotor_spin>`.
   - RD rigs its own bones, so an RD animation has to be retargeted to our bones in Blockbench.
   - For the existing rigs it is often quicker to keyframe there directly. For new models (Mass Driver, drop pod, helper drone) RD's rig can simply become the rig.
6. **Export** `.geo.json` and `.animation.json` to the paths in the row. Same names = no code change.

**Java block models** (the RD Java export) have Minecraft's limits:
- each box rotates on only one axis, in 22.5° steps;
- the model must fit within -16 to 32 pixels (3 blocks).

RD's blocky style "tilts where needed", so check tilted boxes in Blockbench; a block that needs free angles should be a GeckoLib block entity instead. Kinetic blocks (Space Thruster, Electrolyzer, Spinner) leave a hole for the shaft, because Create draws the turning shaft itself.

## How to use it
**Automatically (recommended):** `tools/art/rd_generate.py` sends the rows to Retro Diffusion's API, so no prompt is typed by hand. Setup and options are in `tools/art/README.md`. In short:
1. `export RD_API_KEY=rdpk-...` (your key; it is never written to a file).
2. `python3 tools/art/rd_generate.py --status placeholder --dry-run` shows the jobs and an offline price. Without `--dry-run` it prices them with RD's free estimates, checks your balance and asks before spending.
3. Results land in `art/rd/` (not committed): 4 variants per image, 1 model per 3D asset, and a contact sheet `art/rd/index.html`.
4. `python3 tools/art/rd_generate.py --promote quartz_wafer=2` copies a picked variant into the mod (size-checked). 3D models still go through Blockbench (workflow above).
5. Tell me when a batch is promoted. I'll check it in game and update `PROGRESS.md`.

**By hand:** take the `prompt` column into RD's web app, in the mode the `method` column names, and save the result at exactly the `file` (or `model`) path and size.

Rows marked "code change needed" (GUI skins, the sun, dust meter icons, toast icon, logo) have no slot in the game yet. They need a small change from me before they show up. The same goes for new models from future phases: their blocks and entities don't exist yet.

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
- **Animated textures:** a vertical strip of 16x16 frames plus a `.png.mcmeta`. The Nanite Swarm already works this way (4 frames). RD's animation output is a sprite sheet: restack its frames into one column.

## About the art drop
Art drop 1 came from your private art repo (`cosmicvoiddweller/void-reclaimer-art`, branch `art/tier-c-pilot`). If you redo any of those files, change them there too, or the next drop will overwrite the new ones. The art repo's README defines the conventions the 3D workflow above relies on (bones, base and accent layers, animation names, axes). Its `socket_contract.json` is the reference for bone pivots. The `_accent` file is a mask that the game tints with the player's accent color.

## The list

### Tools & modules (14)

| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |
|---|---|---|---|---|---|---|
| dust_sensor | `item/dust_sensor.png` | 16x16 | no | art drop 1 (redo optional) | handheld brass sensor with a round cyan glass lens and small antenna |  |
| nanite_swarm | `item/nanite_swarm.png` | 16x16 | no | art drop 1 (redo optional) | swirling cloud of tiny silver-cyan nanite specks around a small emitter | Animated (nanite_swarm.png.mcmeta): a 16x64 vertical strip of 4 frames (3 ticks each). RD VFX animation as a sprite sheet, then stack 4 frames top to bottom. |
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
| electrolyzer_side | `block/electrolyzer_side.png` | 16x16 | no | placeholder | machine side: copper tank with glass window showing bubbling water | Wants a real model: RD 3D blocky, Minecraft Java export. Leave the shaft hole on both ends (Create draws the turning shaft). |
| electrolyzer_end | `block/electrolyzer_end.png` | 16x16 | no | placeholder | machine end face: andesite casing with a shaft hole in the middle |  |
| spinner_side | `block/spinner_side.png` | 16x16 | no | placeholder | machine side: brass casing with a spinning spool of black thread visible | Wants a real model: RD 3D blocky, Minecraft Java export. Leave the shaft hole on both ends; the spool could spin later (GeckoLib). |
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

### Body models (GeckoLib) (40)

| Model | File | Texture | Status | Prompt | Bones to keep |
|---|---|---|---|---|---|
| drone (body) | `geo/body/drone.geo.json` | 64x64 tex | art drop 1 (redo optional) | complete small hovering robot drone: round andesite hull, four rotor arms, sensor dome on top, small claw underneath; boxy Minecraft-style model | root_body, hull, hull_casing, hull_frame, cog_internal, cap_sensor_dome, cap_rotor_fl, cap_rotor_fr, cap_rotor_bl, cap_rotor_br, cap_tool_claw, mount_core_1, mount_storage_1, mount_cosmetic_1, sensor_dome, rotor_front_left, rotor_front_right, rotor_back_left, rotor_back_right, tool_claw |
| mech (body) | `geo/body/mech.geo.json` | 64x64 tex | art drop 1 (redo optional) | complete humanoid robot mech: andesite and brass plating, cyan visor, piston limbs, Create mod style; boxy Minecraft-style model | root_body, torso, torso_casing, torso_frame, cog_internal, head, pelvis, back, left_upper_arm, left_forearm, left_hand, right_upper_arm, right_forearm, right_hand, left_thigh, left_shin, left_foot, right_thigh, right_shin, right_foot, cap_head, cap_pelvis, cap_back, cap_left_upper_arm, cap_right_upper_arm, cap_left_thigh, cap_right_thigh, mount_core_1, mount_storage_1, mount_storage_2, mount_cosmetic_1, head__mount_sensor_1, head__mount_sensor_2, pelvis__mount_cosmetic_1, left_forearm__mount_tool_1, left_hand__mount_tool_1, left_shin__mount_thruster_1, left_foot__mount_thruster_1, right_forearm__mount_tool_1, right_hand__mount_tool_1, right_shin__mount_thruster_1, right_foot__mount_thruster_1, back__mount_thruster_1 |
| ship (body) | `geo/body/ship.geo.json` | 128x128 tex | art drop 1 (redo optional) | complete small starship: keel spine, pointed bow, bridge canopy, engine nacelles, short wings, Create mod style; boxy Minecraft-style model | root_body, keel, keel_casing, keel_frame, cog_internal, bow, bridge, hull_1, hull_2, hull_3, hull_4, hull_5, hull_6, wing_port, wing_starboard, nacelle_1, nacelle_2, nacelle_3, nacelle_4, hardpoint_1, hardpoint_2, hardpoint_3, hardpoint_4, hardpoint_5, hardpoint_6, cargo_1, cargo_2, cargo_3, cargo_4, mast_port, mast_starboard, rcs_fore, rcs_aft, dorsal_spine, ventral_fin, cap_bow, cap_bridge, cap_hull_1, cap_hull_2, cap_hull_3, cap_hull_4, cap_hull_5, cap_hull_6, cap_wing_port, cap_wing_starboard, cap_nacelle_1, cap_nacelle_2, cap_nacelle_3, cap_nacelle_4, cap_hardpoint_1, cap_hardpoint_2, cap_hardpoint_3, cap_hardpoint_4, cap_hardpoint_5, cap_hardpoint_6, cap_cargo_1, cap_cargo_2, cap_cargo_3, cap_cargo_4, cap_mast_port, cap_mast_starboard, cap_rcs_fore, cap_rcs_aft, cap_dorsal_spine, cap_ventral_fin, mount_core_1, mount_cosmetic_1, bridge__mount_sensor_1, hull_1__mount_storage_1, hardpoint_1__mount_tool_1, hull_2__mount_storage_1, hardpoint_2__mount_tool_1, hull_3__mount_storage_1, hardpoint_3__mount_tool_1, hull_4__mount_storage_1, hardpoint_4__mount_tool_1, hull_5__mount_storage_1, hardpoint_5__mount_tool_1, hull_6__mount_storage_1, hardpoint_6__mount_tool_1, wing_port__mount_cosmetic_1, wing_starboard__mount_cosmetic_1, nacelle_1__mount_thruster_1, nacelle_1__mount_thruster_2, cargo_1__mount_storage_1, nacelle_2__mount_thruster_1, nacelle_2__mount_thruster_2, cargo_2__mount_storage_1, nacelle_3__mount_thruster_1, nacelle_3__mount_thruster_2, cargo_3__mount_storage_1, nacelle_4__mount_thruster_1, nacelle_4__mount_thruster_2, cargo_4__mount_storage_1, mast_port__mount_sensor_1, mast_starboard__mount_sensor_1, dorsal_spine__mount_cosmetic_1, ventral_fin__mount_cosmetic_1 |
| drone (first person) | `geo/first_person/drone.geo.json` | 64x64 tex | art drop 1 (redo optional) | first-person view of the drone's arms/front: complete small hovering robot drone: round andesite hull, four rotor arms, sensor dome on top, small claw underneath; boxy Minecraft-style model | fp_root, hull_rim, tool_claw, claw_left, claw_right |
| mech (first person) | `geo/first_person/mech.geo.json` | 64x64 tex | art drop 1 (redo optional) | first-person view of the mech's arms/front: complete humanoid robot mech: andesite and brass plating, cyan visor, piston limbs, Create mod style; boxy Minecraft-style model | fp_root, right_arm, right_hand, right_finger_front, right_finger_back, left_arm, left_hand, left_finger_front, left_finger_back |
| ship (first person) | `geo/first_person/ship.geo.json` | 64x64 tex | art drop 1 (redo optional) | first-person view of the ship's arms/front: complete small starship: keel spine, pointed bow, bridge canopy, engine nacelles, short wings, Create mod style; boxy Minecraft-style model | fp_root, bridge_frame, bridge_glass |
| drone_hull (section) | `geo/section/drone_hull.geo.json` | 64x64 tex | art drop 1 (redo optional) | small round drone hull: andesite casing, cyan eye light, cog detail; single body part, boxy Minecraft-style model | drone_hull, drone_hull_casing, drone_hull_frame, cog_internal, cap_sensor_dome, cap_rotor_fl, cap_rotor_fr, cap_rotor_bl, cap_rotor_br, cap_tool_claw, mount_core_1, mount_storage_1, mount_cosmetic_1 |
| mech_foot (section) | `geo/section/mech_foot.geo.json` | 64x64 tex | art drop 1 (redo optional) | flat robot foot with small thruster vents; single body part, boxy Minecraft-style model | mech_foot, mech_foot_body, mech_foot_casing, mech_foot_frame, mount_thruster_1 |
| mech_forearm (section) | `geo/section/mech_forearm.geo.json` | 64x64 tex | art drop 1 (redo optional) | robot forearm segment, andesite with a cog at the elbow; single body part, boxy Minecraft-style model | mech_forearm, mech_forearm_body, mech_forearm_casing, mech_forearm_frame, cap_hand |
| mech_forearm_mk2 (section) | `geo/section/mech_forearm_mk2.geo.json` | 64x64 tex | art drop 1 (redo optional) | heavier robot forearm, brass plating and copper pistons; single body part, boxy Minecraft-style model | mech_forearm_mk2, mech_forearm_mk2_body, mech_forearm_mk2_casing, mech_forearm_mk2_frame, cap_hand, mount_tool_1 |
| mech_hand (section) | `geo/section/mech_hand.geo.json` | 64x64 tex | art drop 1 (redo optional) | robot hand with three thick fingers, copper and andesite; single body part, boxy Minecraft-style model | mech_hand, mech_hand_body, mech_hand_casing, mech_hand_frame, finger_front, finger_back, thumb, mount_tool_1 |
| mech_head (section) | `geo/section/mech_head.geo.json` | 64x64 tex | art drop 1 (redo optional) | robot mech head with a visor slit glowing cyan; single body part, boxy Minecraft-style model | mech_head, mech_head_body, mech_head_casing, mech_head_frame, mech_head_visor, mount_sensor_1, mount_sensor_2 |
| mech_pelvis (section) | `geo/section/mech_pelvis.geo.json` | 64x64 tex | art drop 1 (redo optional) | robot mech hip piece, andesite with brass joints; single body part, boxy Minecraft-style model | mech_pelvis, mech_pelvis_casing, mech_pelvis_frame, cap_left_thigh, cap_right_thigh, mount_cosmetic_1 |
| mech_shin (section) | `geo/section/mech_shin.geo.json` | 64x64 tex | art drop 1 (redo optional) | robot shin segment, andesite with a piston; single body part, boxy Minecraft-style model | mech_shin, mech_shin_body, mech_shin_casing, mech_shin_frame, nozzle_flap_top, nozzle_flap_bottom, cap_foot, mount_thruster_1 |
| mech_shin_mk2 (section) | `geo/section/mech_shin_mk2.geo.json` | 64x64 tex | art drop 1 (redo optional) | heavier robot shin, brass armor and twin pistons; single body part, boxy Minecraft-style model | mech_shin_mk2, mech_shin_mk2_body, mech_shin_mk2_casing, mech_shin_mk2_frame, nozzle_flap_top, nozzle_flap_bottom, cap_foot, mount_thruster_1 |
| mech_thigh (section) | `geo/section/mech_thigh.geo.json` | 64x64 tex | art drop 1 (redo optional) | robot thigh segment, andesite with a brass knee cap; single body part, boxy Minecraft-style model | mech_thigh, mech_thigh_body, mech_thigh_casing, mech_thigh_frame, cap_shin |
| mech_torso (section) | `geo/section/mech_torso.geo.json` | 64x64 tex | art drop 1 (redo optional) | robot mech torso: andesite chest plate, brass trims, glowing cyan core window; single body part, boxy Minecraft-style model | mech_torso, mech_torso_casing, mech_torso_frame, cog_internal, cap_head, cap_pelvis, cap_back, cap_left_upper_arm, cap_right_upper_arm, mount_core_1, mount_storage_1, mount_storage_2, mount_cosmetic_1 |
| mech_upper_arm (section) | `geo/section/mech_upper_arm.geo.json` | 64x64 tex | art drop 1 (redo optional) | robot upper arm segment, andesite with a brass shoulder joint; single body part, boxy Minecraft-style model | mech_upper_arm, mech_upper_arm_body, mech_upper_arm_casing, mech_upper_arm_frame, cap_forearm |
| rotor_arm (section) | `geo/section/rotor_arm.geo.json` | 64x64 tex | art drop 1 (redo optional) | thin andesite drone arm ending in a small spinning rotor; single body part, boxy Minecraft-style model | rotor_arm, rotor_arm_casing, rotor_arm_frame, rotor_spin, nozzle_flap_top, nozzle_flap_bottom, mount_thruster_1 |
| rotor_arm_mk2 (section) | `geo/section/rotor_arm_mk2.geo.json` | 64x64 tex | art drop 1 (redo optional) | reinforced brass drone arm with a larger ducted rotor; single body part, boxy Minecraft-style model | rotor_arm_mk2, rotor_arm_mk2_casing, rotor_arm_mk2_frame, rotor_spin, nozzle_flap_top, nozzle_flap_bottom, mount_thruster_1 |
| sensor_dome (section) | `geo/section/sensor_dome.geo.json` | 64x64 tex | art drop 1 (redo optional) | small glass sensor dome on an andesite base, cyan glow inside; single body part, boxy Minecraft-style model | sensor_dome, sensor_dome_casing, sensor_dome_frame, sensor_lens, mount_sensor_1 |
| ship_bow (section) | `geo/section/ship_bow.geo.json` | 128x128 tex | art drop 1 (redo optional) | pointed starship bow section, brass nose with a cyan window strip; single body part, boxy Minecraft-style model | ship_bow, ship_bow_casing, ship_bow_frame |
| ship_bridge (section) | `geo/section/ship_bridge.geo.json` | 128x128 tex | art drop 1 (redo optional) | starship bridge module with a wide cyan glass canopy; single body part, boxy Minecraft-style model | ship_bridge, ship_bridge_casing, ship_bridge_frame, mount_sensor_1 |
| ship_cargo_bay (section) | `geo/section/ship_cargo_bay.geo.json` | 128x128 tex | art drop 1 (redo optional) | boxy starship cargo bay with large hatch doors; single body part, boxy Minecraft-style model | ship_cargo_bay, ship_cargo_bay_casing, ship_cargo_bay_frame, mount_storage_1 |
| ship_dorsal_spine (section) | `geo/section/ship_dorsal_spine.geo.json` | 128x128 tex | art drop 1 (redo optional) | starship dorsal spine with antennas and pipes; single body part, boxy Minecraft-style model | ship_dorsal_spine, ship_dorsal_spine_casing, ship_dorsal_spine_frame, mount_cosmetic_1 |
| ship_engine_nacelle (section) | `geo/section/ship_engine_nacelle.geo.json` | 128x128 tex | art drop 1 (redo optional) | starship engine nacelle, copper nozzle and blue glow; single body part, boxy Minecraft-style model | ship_engine_nacelle, ship_engine_nacelle_casing, ship_engine_nacelle_frame, nacelle_flap_top, nacelle_flap_bottom, cog_nacelle, mount_thruster_1, mount_thruster_2 |
| ship_engine_nacelle_mk2 (section) | `geo/section/ship_engine_nacelle_mk2.geo.json` | 128x128 tex | art drop 1 (redo optional) | larger brass starship engine nacelle with twin nozzles; single body part, boxy Minecraft-style model | ship_engine_nacelle_mk2, ship_engine_nacelle_mk2_casing, ship_engine_nacelle_mk2_frame, nacelle_flap_top, nacelle_flap_bottom, cog_nacelle, mount_thruster_1, mount_thruster_2 |
| ship_hardpoint (section) | `geo/section/ship_hardpoint.geo.json` | 128x128 tex | art drop 1 (redo optional) | starship hardpoint mount, square brass socket for tools; single body part, boxy Minecraft-style model | ship_hardpoint, ship_hardpoint_casing, ship_hardpoint_frame, hardpoint_jaw_left, hardpoint_jaw_right, mount_tool_1 |
| ship_hull_segment (section) | `geo/section/ship_hull_segment.geo.json` | 128x128 tex | art drop 1 (redo optional) | starship hull segment, andesite plating panel; single body part, boxy Minecraft-style model | ship_hull_segment, ship_hull_segment_casing, ship_hull_segment_frame, cap_hardpoint |
| ship_hull_segment_mk2 (section) | `geo/section/ship_hull_segment_mk2.geo.json` | 128x128 tex | art drop 1 (redo optional) | reinforced starship hull segment, brass plating panel; single body part, boxy Minecraft-style model | ship_hull_segment_mk2, ship_hull_segment_mk2_casing, ship_hull_segment_mk2_frame, cap_hardpoint, mount_storage_1 |
| ship_keel (section) | `geo/section/ship_keel.geo.json` | 128x128 tex | art drop 1 (redo optional) | starship keel spine, long andesite beam with brass ribs; single body part, boxy Minecraft-style model | ship_keel, ship_keel_casing, ship_keel_frame, cog_internal, cap_bow, cap_bridge, cap_hull_1, cap_hull_2, cap_hull_3, cap_hull_4, cap_hull_5, cap_hull_6, cap_wing_port, cap_wing_starboard, cap_nacelle_1, cap_nacelle_2, cap_nacelle_3, cap_nacelle_4, cap_cargo_1, cap_cargo_2, cap_cargo_3, cap_cargo_4, cap_mast_port, cap_mast_starboard, cap_rcs_fore, cap_rcs_aft, cap_dorsal_spine, cap_ventral_fin, mount_core_1, mount_cosmetic_1 |
| ship_rcs_cluster (section) | `geo/section/ship_rcs_cluster.geo.json` | 128x128 tex | art drop 1 (redo optional) | cluster of four tiny maneuvering thruster nozzles; single body part, boxy Minecraft-style model | ship_rcs_cluster, ship_rcs_cluster_casing, ship_rcs_cluster_frame |
| ship_sensor_mast (section) | `geo/section/ship_sensor_mast.geo.json` | 128x128 tex | art drop 1 (redo optional) | tall thin sensor mast with a cyan beacon on top; single body part, boxy Minecraft-style model | ship_sensor_mast, ship_sensor_mast_casing, ship_sensor_mast_frame, mast_dish, mount_sensor_1 |
| ship_ventral_fin (section) | `geo/section/ship_ventral_fin.geo.json` | 128x128 tex | art drop 1 (redo optional) | downward starship fin, andesite with brass edge; single body part, boxy Minecraft-style model | ship_ventral_fin, ship_ventral_fin_casing, ship_ventral_fin_frame, mount_cosmetic_1 |
| ship_wing (section) | `geo/section/ship_wing.geo.json` | 128x128 tex | art drop 1 (redo optional) | short starship wing with copper heat fins; single body part, boxy Minecraft-style model | ship_wing, ship_wing_casing, ship_wing_frame, wing_flap, mount_cosmetic_1 |
| tool_claw (section) | `geo/section/tool_claw.geo.json` | 64x64 tex | art drop 1 (redo optional) | small mechanical claw with three copper fingers; single body part, boxy Minecraft-style model | tool_claw, tool_claw_casing, tool_claw_frame, claw_left, claw_right, mount_tool_1, mount_tool_2 |
| brass_trim (module) | `geo/module/brass_trim.geo.json` | 64x64 tex | art drop 1 (redo optional) | decorative curved brass trim plate with rivets (cosmetic part); single body part, boxy Minecraft-style model | brass_trim, brass_trim_drone, brass_trim_mech, brass_trim_ship |
| cogwheel_crown (module) | `geo/module/cogwheel_crown.geo.json` | 64x64 tex | art drop 1 (redo optional) | ring of small brass cogwheels forming a crown (cosmetic part); single body part, boxy Minecraft-style model | cogwheel_crown, cogwheel_crown_base, crown_cog |
| thruster_mk1 (module) | `geo/module/thruster_mk1.geo.json` | 64x64 tex | art drop 1 (redo optional) | small copper rocket thruster nozzle with an andesite mount; single body part, boxy Minecraft-style model | thruster_mk1, thruster_mk1_mount, thruster_nozzle |
| thruster_mk2 (module) | `geo/module/thruster_mk2.geo.json` | 64x64 tex | art drop 1 (redo optional) | larger brass rocket thruster with a flared nozzle and a faint blue exhaust glow; single body part, boxy Minecraft-style model | thruster_mk2, thruster_mk2_mount, thruster_nozzle, nozzle_flap_top, nozzle_flap_bottom |

### Body animations (16)

| Animation (exact name) | File | Prompt |
|---|---|---|
| `animation.void_reclaimer.drone.idle` | `animations/body/drone.animation.json` | drone: gentle hover bob, cogs turning slowly |
| `animation.void_reclaimer.drone.thrust` | `animations/body/drone.animation.json` | drone: leaning into thrust, cogs spinning fast |
| `animation.void_reclaimer.drone.brake` | `animations/body/drone.animation.json` | drone: pitching back to brake |
| `animation.void_reclaimer.drone.low_power` | `animations/body/drone.animation.json` | drone: drooping, cogs winding down |
| `animation.void_reclaimer.drone.module_work` | `animations/body/drone.animation.json` | drone: tool arm working, cogs spinning |
| `animation.void_reclaimer.drone.rotor_spin` | `animations/body/drone.animation.json` | drone: rotors spinning |
| `animation.void_reclaimer.mech.idle` | `animations/body/mech.animation.json` | mech: gentle hover bob, cogs turning slowly |
| `animation.void_reclaimer.mech.thrust` | `animations/body/mech.animation.json` | mech: leaning into thrust, cogs spinning fast |
| `animation.void_reclaimer.mech.brake` | `animations/body/mech.animation.json` | mech: pitching back to brake |
| `animation.void_reclaimer.mech.low_power` | `animations/body/mech.animation.json` | mech: drooping, cogs winding down |
| `animation.void_reclaimer.mech.module_work` | `animations/body/mech.animation.json` | mech: tool arm working, cogs spinning |
| `animation.void_reclaimer.ship.idle` | `animations/body/ship.animation.json` | ship: gentle hover bob, cogs turning slowly |
| `animation.void_reclaimer.ship.thrust` | `animations/body/ship.animation.json` | ship: leaning into thrust, cogs spinning fast |
| `animation.void_reclaimer.ship.brake` | `animations/body/ship.animation.json` | ship: pitching back to brake |
| `animation.void_reclaimer.ship.low_power` | `animations/body/ship.animation.json` | ship: drooping, cogs winding down |
| `animation.void_reclaimer.ship.module_work` | `animations/body/ship.animation.json` | ship: tool arm working, cogs spinning |

### Body skins (GeckoLib) (80)

All 80 come from art drop 1 and are UV-mapped to their models. If you regenerate a model with RD 3D, its base texture comes with it. The `_accent` masks are painted by hand in Blockbench (white where the accent color shows). Sizes: 128x128 for the Ship rig and ship parts, 64x64 for the rest. Full list in `assets.csv`.

### Missing module models (9)

| Model | File | Texture | Status | Prompt | Bones to keep |
|---|---|---|---|---|---|
| dust_sensor | `geo/module/dust_sensor.geo.json` | 64x64 tex | missing | handheld brass sensor with a round cyan glass lens and small antenna; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |
| nanite_swarm | `geo/module/nanite_swarm.geo.json` | 64x64 tex | missing | swirling cloud of tiny silver-cyan nanite specks around a small emitter; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |
| salvage_tool | `geo/module/salvage_tool.geo.json` | 64x64 tex | missing | compact cutting tool with a copper handle and glowing orange cutting edge; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |
| mechanical_drill | `geo/module/mechanical_drill.geo.json` | 64x64 tex | missing | industrial drill module: andesite casing, brass collar, spiral steel drill bit; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |
| matter_harvester | `geo/module/matter_harvester.geo.json` | 64x64 tex | missing | mining module with a wide brass intake funnel and copper pipes, slower gentler cousin of a drill; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |
| core_mk1 | `geo/module/core_mk1.geo.json` | 64x64 tex | missing | small round power core: andesite shell, glowing cyan center, cog teeth around the rim; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |
| core_mk2 | `geo/module/core_mk2.geo.json` | 64x64 tex | missing | larger power core: brass shell, bright white-cyan glowing center, two rings of cog teeth; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |
| storage_mk1 | `geo/module/storage_mk1.geo.json` | 64x64 tex | missing | small andesite storage pod with a hatch and brass latches; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |
| storage_mk2 | `geo/module/storage_mk2.geo.json` | 64x64 tex | missing | larger brass storage pod with two hatches and copper bands; small module that mounts on a robot body, boxy Minecraft-style model | `<id>`, `<id>_mount`, own parts |

### Future phases (20)

| Phase | Asset | What | Method | Prompt idea | Notes |
|---|---|---|---|---|---|
| 7b | space_thruster | block model | RD 3D blocky -> Minecraft Java | Create-style space thruster block: andesite casing, copper bell nozzle on one face, shaft input on the back | Kinetic block: Create draws the turning shaft itself, so leave a shaft hole. Static block model: Java export. |
| 7b | helm | block model | RD 3D blocky -> Minecraft Java | ship helm console: brass control panel with levers and a small cyan screen | Steers a block ship. |
| 9 | kinetic_mass_driver | animated multiblock (1x1x3) | RD 3D blocky + animate (GeckoLib) | long electromagnetic launch rail: base with copper coils, barrel with brass rings, muzzle with a cyan glow | Animations: charge, launch, cool down. GeckoLib block entity, so export .bbmodel, not Java. |
| 9 | quantum_receiver_dish | animated block | RD 3D blocky + animate (GeckoLib) | large receiver dish on a brass mount, cyan energy focus at the center | Turns to track the beam; bigger than one block. |
| 9 | power_beam | effect texture | RD image (VFX animation) | vertical beam of soft cyan-white light, fading at the edges, 16x64 strip | Drawn from the Dyson swarm to the receiver. |
| 9 | dyson_swarm_sprites | sky textures | RD image | tiny satellites / lattice segments / shell panels around a sun, pixel art, transparent | DysonSky draws these procedurally now; textures optional. |
| 11 | ruined_earth sky | sky textures | RD image | smoggy brown-orange sky, three eras: choked, clearing, healed | Three-era sky. |
| 11 | atmospheric_portal | block + animated surface | RD image (VFX animation) | portal frame of brass and andesite; swirling pale-blue atmosphere surface | Surface is a vertical strip of 16x16 frames, like the nether portal: RD sprite-sheet output, restacked. |
| 11 | ashen_slag | block | RD image | grey-black ashen slag ground with cinders | Tileable 16x16. |
| 11 | acid_drizzle | weather texture | RD image | sickly yellow-green rain streaks, vanilla rain layout 64x256 | Weather replacement texture. |
| 11 | drop_pod | animated entity | RD 3D blocky + animate (GeckoLib) | scorched landing pod, brass ribs and heat shield underside | Animations: fall, land, open. |
| 11 | industrial_mech | body or entity | RD 3D blocky + animate (GeckoLib) | heavy industrial mech, copper and andesite, hazard stripes | Defined in the design doc; if it is a body chassis it follows the socket contract. |
| 11 | planetary_grid_hud | GUI | RD image | pixel art 4x4 planetary sector map, dark screen with cyan grid lines | 256x256 GUI sprite. |
| 12 | terraforming_spire | animated multiblock | RD 3D blocky + animate (GeckoLib) | tall terraforming spire: heavy base, ribbed column, glowing green emitter top | Genesis Pulse animation on the emitter. |
| 12 | bio_nutrient_slurry | fluid + bucket | RD image | bucket of thick green bio-nutrient slurry | Fluid can reuse tinted water like the others. |
| 13 | data_monolith | block model | RD 3D blocky | tall black monolith with faint cyan circuit lines and an output drawer | Design not settled in the doc yet. |
| 14 | restoration_hud | GUI | RD image | pixel art planet restoration progress panel, green and blue | 256x256 GUI sprite. |
| later | helper_drone | animated entity | RD 3D blocky + animate (GeckoLib) | small cargo drone with a claw and rotors, Create style | Animations: fly, pick up, drop off. |
| later | asteroid ores | blocks | RD image | asteroid rock with embedded crystal veins, one per dust kind | Polish track: ore veins in asteroids. Four tileable textures. |
| later | cloud_collector | module item + model | RD image + RD 3D blocky | module with a wide scoop that pulls particles from a dust cloud | Raw-item collector part (polish track). |

