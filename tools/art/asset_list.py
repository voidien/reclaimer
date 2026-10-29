# Builds docs/art/assets.csv and docs/art/ASSETS.md (tools/art/assets_header.md + generated tables) from one list.
# Run from anywhere: python3 tools/art/asset_list.py
# 'art drop 1' status comes from the files changed by the art-drop commit 0df8a88; update the lists below when assets change.
import csv, os, struct, subprocess, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..')
A = 'src/main/resources/assets/void_reclaimer'
T = A + '/textures/'
os.chdir(ROOT)

drop = set(p.replace(T, '') for p in subprocess.run(
    ['git', 'diff', '--name-only', '0df8a88^1', '0df8a88', '--', T], capture_output=True, text=True).stdout.split())

def size_of(rel):
    p = T + rel
    if not os.path.exists(p):
        return None
    d = open(p, 'rb').read(24)
    return '%dx%d' % struct.unpack('>II', d[16:24])

def status(rel):
    if rel in drop:
        return 'art drop 1 (redo optional)'
    if os.path.exists(T + rel):
        return 'placeholder'
    return 'missing (future)'

ITEM = ('Minecraft item icon, 16x16 pixel art, transparent background, light from top-left, '
        'dark 1px outline, Create mod industrial style; ')
BLOCK = ('seamless tileable 16x16 Minecraft block texture, pixel art, Create mod style; ')
BLOCK_FACE = ('16x16 Minecraft block face texture, pixel art, Create mod style, not tiled; ')

rows = []

def add(group, asset, rel, size, tile, method, prompt, notes='', phase='now', model=''):
    rows.append(dict(group=group, asset=asset, file=(T + rel) if rel else '', size=size,
                     tileable=tile, method=method, status=status(rel) if rel else 'missing (future)',
                     phase=phase, model=model, prompt=prompt, notes=notes))

def item(group, iid, desc, notes='', phase='now'):
    add(group, iid, 'item/%s.png' % iid, '16x16', 'no', 'Retro Diffusion', ITEM + desc, notes, phase)

# ---------- items: tools and modules ----------
G = 'Tools & modules'
item(G, 'dust_sensor', 'handheld brass sensor with a round cyan glass lens and small antenna')
item(G, 'nanite_swarm', 'swirling cloud of tiny silver-cyan nanite specks around a small emitter',
     'Animated in the art drop (nanite_swarm.png.mcmeta): a 16x64 vertical strip of 4 frames (3 ticks each). Generate 4 frames and stack them top to bottom.')
item(G, 'salvage_tool', 'compact cutting tool with a copper handle and glowing orange cutting edge')
item(G, 'mechanical_drill', 'industrial drill module: andesite casing, brass collar, spiral steel drill bit')
item(G, 'matter_harvester', 'mining module with a wide brass intake funnel and copper pipes, slower gentler cousin of a drill')
item(G, 'body_blueprint', 'rolled blue engineering blueprint of a small robot drone, white line drawing')
item(G, 'core_mk1', 'small round power core: andesite shell, glowing cyan center, cog teeth around the rim')
item(G, 'core_mk2', 'larger power core: brass shell, bright white-cyan glowing center, two rings of cog teeth')
item(G, 'thruster_mk1', 'small copper rocket thruster nozzle with an andesite mount')
item(G, 'thruster_mk2', 'larger brass rocket thruster with a flared nozzle and a faint blue exhaust glow')
item(G, 'storage_mk1', 'small andesite storage pod with a hatch and brass latches')
item(G, 'storage_mk2', 'larger brass storage pod with two hatches and copper bands')
item(G, 'brass_trim', 'decorative curved brass trim plate with rivets (cosmetic part)')
item(G, 'cogwheel_crown', 'ring of small brass cogwheels forming a crown (cosmetic part)')

# ---------- items: body sections ----------
G = 'Body sections'
sections = {
    'drone_hull': 'small round drone hull: andesite casing, cyan eye light, cog detail',
    'rotor_arm': 'thin andesite drone arm ending in a small spinning rotor',
    'rotor_arm_mk2': 'reinforced brass drone arm with a larger ducted rotor',
    'sensor_dome': 'small glass sensor dome on an andesite base, cyan glow inside',
    'tool_claw': 'small mechanical claw with three copper fingers',
    'mech_torso': 'robot mech torso: andesite chest plate, brass trims, glowing cyan core window',
    'mech_pelvis': 'robot mech hip piece, andesite with brass joints',
    'mech_head': 'robot mech head with a visor slit glowing cyan',
    'mech_upper_arm': 'robot upper arm segment, andesite with a brass shoulder joint',
    'mech_forearm': 'robot forearm segment, andesite with a cog at the elbow',
    'mech_forearm_mk2': 'heavier robot forearm, brass plating and copper pistons',
    'mech_hand': 'robot hand with three thick fingers, copper and andesite',
    'mech_thigh': 'robot thigh segment, andesite with a brass knee cap',
    'mech_shin': 'robot shin segment, andesite with a piston',
    'mech_shin_mk2': 'heavier robot shin, brass armor and twin pistons',
    'mech_foot': 'flat robot foot with small thruster vents',
    'ship_keel': 'starship keel spine, long andesite beam with brass ribs',
    'ship_bow': 'pointed starship bow section, brass nose with a cyan window strip',
    'ship_bridge': 'starship bridge module with a wide cyan glass canopy',
    'ship_cargo_bay': 'boxy starship cargo bay with large hatch doors',
    'ship_dorsal_spine': 'starship dorsal spine with antennas and pipes',
    'ship_engine_nacelle': 'starship engine nacelle, copper nozzle and blue glow',
    'ship_engine_nacelle_mk2': 'larger brass starship engine nacelle with twin nozzles',
    'ship_hardpoint': 'starship hardpoint mount, square brass socket for tools',
    'ship_hull_segment': 'starship hull segment, andesite plating panel',
    'ship_hull_segment_mk2': 'reinforced starship hull segment, brass plating panel',
    'ship_rcs_cluster': 'cluster of four tiny maneuvering thruster nozzles',
    'ship_sensor_mast': 'tall thin sensor mast with a cyan beacon on top',
    'ship_ventral_fin': 'downward starship fin, andesite with brass edge',
    'ship_wing': 'short starship wing with copper heat fins',
}
for k, v in sections.items():
    item(G, k, v + ' (item icon of a body part)')

# ---------- items: raw materials and intermediates ----------
G = 'Materials'
mats = {
    'raw_ice_shards': 'pile of pale cyan ice shards, sharp crystal edges',
    'carbon_flakes': 'handful of dark grey-violet carbon flakes',
    'silicon_grit': 'pile of sandy tan silicon grit granules',
    'metallic_grains': 'pile of shiny silver-grey metal grains',
    'crushed_cryo_ice': 'crushed frosty blue ice powder in a small heap',
    'coolant_frame_plate': 'square metal plate with frosty blue coolant channels',
    'fine_carbon_powder': 'small heap of fine black carbon powder',
    'carbon_paste': 'blob of thick glossy black paste',
    'carbon_composite_plate': 'woven dark carbon-fiber plate with a sheen',
    'silicon_carbide_slurry': 'blob of grey-green gritty slurry',
    'silicon_carbide_coating': 'thin dark grey-green ceramic coating sheet',
    'thermal_shielding_tile': 'black ceramic heat shield tile with a white edge',
    'high_purity_graphite': 'glossy dark graphite ingot',
    'carbon_thread': 'spool of black carbon thread',
    'pure_silicon_dust': 'small heap of bright silver-white silicon dust',
    'quartz_wafer': 'thin round translucent quartz wafer disk',
    'doped_silicon_ingot': 'silicon ingot with faint blue-violet doping streaks',
    'high_efficiency_solar_cell': 'square dark blue solar cell with a silver grid',
    'advanced_sensor_unit': 'small electronic sensor box with a lens and a green status light',
    'refined_metal_nodule': 'smooth round polished metal nodule',
    'micro_alloy_sheet': 'thin flexible shiny alloy sheet, slightly curled',
    'high_output_solar_sail': 'folded gold-and-blue solar sail panel',
    'zero_g_precision_frame': 'precise square metal frame with graphite struts',
    'dyson_swarm_satellite_payload': 'compact satellite with folded solar panels on a frame',
    'dyson_shell_truss_module': 'heavy triangular truss segment with heat tiles',
    'rcs_thruster': 'small maneuvering thruster block with a nozzle and propellant line',
    'reaction_wheel_gyroscope': 'gyroscope with a spinning brass wheel inside a frame',
    'atmospheric_catalyst_core': 'glowing green-blue crystal core inside a metal cage',
}
for k, v in mats.items():
    item(G, k, v)
G = 'Unfinished (Sequenced Assembly)'
for k in ['zero_g_precision_frame', 'dyson_swarm_satellite_payload', 'dyson_shell_truss_module', 'rcs_thruster',
          'reaction_wheel_gyroscope', 'atmospheric_catalyst_core', 'high_output_solar_sail']:
    item(G, 'incomplete_' + k, 'half-built ' + mats[k] + ', parts missing, exposed frame',
         'Create convention: the finished icon with pieces missing. Generate from the finished icon (img2img) to keep them matched.')

G = 'Fluid buckets'
fluids = {
    'purified_water': ('clear light-blue water', '#A8DCFF'),
    'oxygen_gas': ('pale blue gas swirls', '#B4C8FF'),
    'hydrogen_gas': ('pale pink gas swirls', '#FFC8C8'),
    'nebulite_propellant': ('glowing orange propellant', '#FF9638'),
    'coolant_fluid': ('cyan coolant', '#78DCE6'),
    'liquid_silicon': ('glowing silver-white molten silicon', '#D0D8EE'),
}
for k, (d, c) in fluids.items():
    item(G, k + '_bucket', 'vanilla-style iron bucket filled with ' + d,
         'Keep the vanilla bucket shape; only the contents change. Fluid tint in code: ' + c + '.')

# ---------- blocks ----------
G = 'Blocks'
def block(asset, rel, desc, tile='yes', notes='', phase='now', model=''):
    add(G, asset, rel, '16x16', tile, 'Retro Diffusion', (BLOCK if tile == 'yes' else BLOCK_FACE) + desc, notes, phase, model)

for k, (d, c) in {'silicate': ('sandy tan porous space rock', '#D9B38C'),
                  'ice': ('pale cyan frozen space rock with frost cracks', '#9FE2FF'),
                  'carbon': ('dark grey-violet sooty space rock', '#6E6A80'),
                  'metallic': ('silver-grey metallic space rock with shiny flecks', '#B8C2CC')}.items():
    block('asteroid_' + k, 'block/asteroid_%s.png' % k, d,
          notes='Dust kind color ' + c + '. Worlds have huge surfaces of it: it must tile without visible seams or repeats.')
block('scaffold', 'block/scaffold.png', 'light andesite metal scaffold grid with open gaps')
block('backup_core', 'block/backup_core.png', 'brass machine casing with a glowing cyan core window in the center',
      tile='no', notes='All six faces use this one texture now (cube_all).')
block('tether_anchor', 'block/tether_anchor.png', 'heavy andesite anchor block with a brass ring in the middle',
      tile='no', notes='All six faces use this one texture now.')
block('electrolyzer_side', 'block/electrolyzer_side.png', 'machine side: copper tank with glass window showing bubbling water',
      tile='no', model='Wants a real Create-style model (shaft through two faces, tank).')
block('electrolyzer_end', 'block/electrolyzer_end.png', 'machine end face: andesite casing with a shaft hole in the middle', tile='no')
block('spinner_side', 'block/spinner_side.png', 'machine side: brass casing with a spinning spool of black thread visible',
      tile='no', model='Wants a real Create-style model (shaft, visible spool).')
block('spinner_end', 'block/spinner_end.png', 'machine end face: brass casing with a shaft hole in the middle', tile='no')

# ---------- HUD and GUI (larger, mixed sizes) ----------
G = 'HUD & GUI'
for k, d in {'energy_full': 'full segment', 'energy_half': 'half-filled segment', 'energy_empty': 'empty segment',
             'energy_empty_warning': 'empty segment flashing red warning'}.items():
    add(G, k, 'gui/sprites/hud/%s.png' % k, '90x9', 'no', 'Retro Diffusion',
        'pixel art HUD energy bar, 90x9, ten cyan battery cells in a row, ' + d + ', transparent background',
        'Replaces the hunger bar slot. Must stay exactly 90x9.')
gui = [
    ('replicator_panel', 'gui/replicator_panel.png', '256x256',
     'pixel art sci-fi machine GUI panel background, dark blue-grey with andesite metal frame and brass corners, empty inner areas',
     'Not wired up yet: the Replicator and Body tab are drawn with flat fills. Needs a small code change to use a sprite (9-slice).'),
    ('replicator_tabs', 'gui/sprites/replicator/tab.png', '48x20',
     'pixel art GUI tab button, andesite metal with brass edge, plus a highlighted variant', 'Same: code change needed.'),
    ('module_wheel', 'gui/sprites/hud/module_wheel.png', '128x128',
     'pixel art radial selection wheel, six segments, dark translucent metal with cyan highlight', 'Same: code change needed.'),
    ('dust_meter_icons', 'gui/sprites/hud/dust_<kind>.png', '9x9',
     'tiny pixel art icon of a dust grain cloud, one each for metallic, silicate, ice, carbon',
     'Four files. Dust meter uses colored squares now; code change needed.'),
    ('milestone_toast_icon', 'gui/sprites/toast/milestone.png', '16x16',
     'pixel art icon of a glowing AI core with a data spark', 'Toast shows no icon now; code change needed.'),
    ('mod_logo', 'logo.png', '256x256',
     'pixel art game logo: "Void Reclaimer", a small robot drone silhouetted against a dim distant sun in black space',
     'For neoforge.mods.toml logoFile (not set yet).'),
]
for asset, rel, size, prompt, notes in gui:
    add(G, asset, rel, size, 'no', 'Retro Diffusion', prompt, notes, 'optional')

G = 'Sky'
add(G, 'sun', 'environment/sun.png', '64x64', 'no', 'Retro Diffusion',
    'pixel art distant pale star on transparent background, soft glow, small bright core',
    'Deep space draws vanilla\'s sun texture now (DeepSpaceEffects). A custom one needs a one-line code change.', 'optional')

# ---------- entity skins (GeckoLib) ----------
G = 'Body skins (GeckoLib)'
for f in sorted(os.listdir(T + 'entity/body')):
    rel = 'entity/body/' + f
    name = f[:-4]
    accent = name.endswith('_accent')
    add(G, name, rel, size_of(rel), 'no', 'Blockbench (UV-mapped)',
        ('accent mask: white where the player\'s accent color shows, transparent elsewhere' if accent else
         'Create-style casing material for this part: andesite metal, brass trim, cyan lights (concept/swatch only)'),
        'UV-mapped to its .geo.json. Paint in Blockbench over the UV template; RD only for material swatches or concept art.'
        + (' The accent layer is tinted in code, so keep it greyscale/white.' if accent else ''))

# ---------- models that exist / are missing ----------
G = 'Models (Blockbench)'
def model(asset, file, prompt, notes, phase='now'):
    add(G, asset, '', '-', 'no', 'Blockbench', prompt, notes, phase, model=file)
    rows[-1]['status'] = 'missing' if 'missing' in notes.lower() or phase != 'now' else 'art drop 1'
for m in ['dust_sensor', 'nanite_swarm', 'salvage_tool', 'mechanical_drill', 'matter_harvester', 'core_mk1', 'core_mk2',
          'storage_mk1', 'storage_mk2']:
    model(m + ' (on-body model)', A + '/geo/module/%s.geo.json' % m,
          'concept art: ' + (dict((k, v) for k, v in [(r['asset'], r['prompt'].replace(ITEM, '')) for r in rows]).get(m, m)) + ', mounted on a small robot',
          'Missing: only thruster_mk1/2, brass_trim and cogwheel_crown have module models. Needed in 5b if the module shows on the body (cores and storage may stay hidden).')

# ---------- future phases ----------
G = 'Future phases'
fut = [
    ('7b', 'space_thruster', 'block + model', 'block/space_thruster_*.png',
     'Create-style space thruster block: andesite casing, copper bell nozzle on one face, shaft input on the back',
     'Kinetic block (shaft in), pushes Aeronautics ships. Model in Blockbench; textures 16x16.'),
    ('7b', 'helm', 'block + model', 'block/helm_*.png',
     'ship helm console: brass control panel with levers and a small cyan screen',
     'Player sits/stands here to steer a block ship.'),
    ('9', 'kinetic_mass_driver', 'multiblock (1x1x3) + model', 'block/mass_driver_{base,barrel,muzzle}.png',
     'long electromagnetic launch rail: base with copper coils, barrel with brass rings, muzzle with a cyan glow',
     'Launch sequence and animations planned: likely GeckoLib block entity (geo + animation).'),
    ('9', 'quantum_receiver_dish', 'block + model', 'block/quantum_receiver_*.png',
     'large receiver dish on a brass mount, cyan energy focus at the center', 'Big dish model; receives the sky power beams.'),
    ('9', 'power_beam', 'sky/effect texture', 'environment/power_beam.png',
     'vertical beam of soft cyan-white light, fading at the edges, 16x64 strip', 'Drawn from the Dyson swarm to the receiver.'),
    ('9', 'dyson_swarm_sprites', 'sky texture', 'environment/dyson_{swarm,lattice,shell}.png',
     'tiny satellites / lattice segments / shell panels around a sun, pixel art, transparent',
     'DysonSky draws these procedurally now; textures optional.'),
    ('11', 'ruined_earth sky', 'sky textures', 'environment/ruined_*.png',
     'smoggy brown-orange sky, three eras: choked, clearing, healed', 'Three-era sky.'),
    ('11', 'atmospheric_portal', 'block + portal surface', 'block/atmospheric_portal_{frame,surface}.png',
     'portal frame of brass and andesite; swirling pale-blue atmosphere surface',
     'Surface likely animated (vertical strip of 16x16 frames, like the nether portal).'),
    ('11', 'ashen_slag', 'block', 'block/ashen_slag.png', 'grey-black ashen slag ground with cinders', 'Tileable 16x16.'),
    ('11', 'acid_drizzle', 'weather texture', 'environment/acid_rain.png',
     'sickly yellow-green rain streaks, vanilla rain layout 64x256', 'Weather replacement texture.'),
    ('11', 'drop_pod', 'entity model + texture', 'entity/drop_pod.png',
     'scorched landing pod, brass ribs and heat shield underside', 'GeckoLib entity; texture is UV-mapped (Blockbench).'),
    ('11', 'industrial_mech', 'body/entity', 'entity/industrial_mech.png',
     'heavy industrial mech, copper and andesite, hazard stripes', 'Defined in the design doc; GeckoLib, UV-mapped.'),
    ('11', 'planetary_grid_hud', 'GUI', 'gui/planetary_grid.png',
     'pixel art 4x4 planetary sector map, dark screen with cyan grid lines', '256x256 GUI sprite.'),
    ('12', 'terraforming_spire', 'multiblock + model', 'block/spire_{base,column,emitter}.png',
     'tall terraforming spire: heavy base, ribbed column, glowing green emitter top', 'Multiblock; the emitter likely animated.'),
    ('12', 'bio_nutrient_slurry', 'fluid + bucket', 'item/bio_nutrient_slurry_bucket.png',
     'bucket of thick green bio-nutrient slurry', 'Fluid can reuse tinted water like the others.'),
    ('13', 'data_monolith', 'block/multiblock + model', 'block/data_monolith_*.png',
     'tall black monolith with faint cyan circuit lines and an output drawer', 'Design not settled in the doc yet.'),
    ('14', 'restoration_hud', 'GUI', 'gui/restoration.png',
     'pixel art planet restoration progress panel, green and blue', '256x256 GUI sprite.'),
    ('later', 'helper_drone', 'entity model + texture', 'entity/helper_drone.png',
     'small cargo drone with a claw and rotors, Create style', 'Stations track; GeckoLib, UV-mapped.'),
    ('later', 'asteroid ores', 'blocks', 'block/asteroid_<kind>_ore.png',
     'asteroid rock with embedded crystal veins, one per dust kind', 'Polish track: ore veins in asteroids. Four tileable textures.'),
    ('later', 'cloud_collector', 'module item + model', 'item/cloud_collector.png',
     'module with a wide scoop that pulls particles from a dust cloud', 'Raw-item collector part (polish track).'),
]
for phase, asset, kind, rel, prompt, notes in fut:
    rows.append(dict(group=G, asset=asset, file=T + rel, size='16x16' if kind.startswith('block') else '-',
                     tileable='yes' if asset in ('ashen_slag', 'asteroid ores') else 'no',
                     method='Blockbench + Retro Diffusion' if 'model' in kind or 'entity' in kind else 'Retro Diffusion',
                     status='missing (future)', phase=phase, model=kind, prompt=prompt, notes=notes))

# ---------- write CSV ----------
os.makedirs('docs/art', exist_ok=True)
cols = ['group', 'asset', 'status', 'phase', 'method', 'size', 'tileable', 'file', 'model', 'prompt', 'notes']
with open('docs/art/assets.csv', 'w', newline='') as f:
    w = csv.DictWriter(f, fieldnames=cols)
    w.writeheader()
    for r in rows:
        w.writerow({c: r.get(c, '') for c in cols})

# ---------- markdown tables ----------
def esc(s):
    return str(s).replace('|', '\\|')
out = []
order = []
for r in rows:
    if r['group'] not in order:
        order.append(r['group'])
for g in order:
    rs = [r for r in rows if r['group'] == g]
    out.append('### %s (%d)\n' % (g, len(rs)))
    if g == 'Body skins (GeckoLib)':
        out.append('All 80 come from art drop 1 and are UV-mapped to their `.geo.json` rigs: paint them in Blockbench, not Retro Diffusion. '
                   'Each part has a base texture and an `_accent` mask (tinted in code). Sizes: 128x128 for the Ship rig and ship parts, '
                   '64x64 for everything else. The full list is in `assets.csv`.\n')
        continue
    if g == 'Future phases':
        out.append('| Phase | Asset | What | Files (planned) | Prompt idea | Notes |\n|---|---|---|---|---|---|')
        for r in rs:
            out.append('| %s | %s | %s | `%s` | %s | %s |' % (r['phase'], r['asset'], r['model'], r['file'].replace(T, ''), esc(r['prompt']), esc(r['notes'])))
    elif g == 'Models (Blockbench)':
        out.append('| Asset | File | Status | Notes |\n|---|---|---|---|')
        for r in rs:
            out.append('| %s | `%s` | %s | %s |' % (r['asset'], r['model'].replace(A + '/', ''), r['status'], esc(r['notes'])))
    else:
        out.append('| Asset | File | Size | Tile | Status | Subject (prompt suffix) | Notes |\n|---|---|---|---|---|---|---|')
        for r in rs:
            p = r['prompt']
            for pre in (ITEM, BLOCK, BLOCK_FACE):
                p = p.replace(pre, '')
            out.append('| %s | `%s` | %s | %s | %s | %s | %s |' % (r['asset'], r['file'].replace(T, ''), r['size'], r['tileable'],
                                                              r['status'], esc(p), esc(r['notes'])))
    out.append('')
header = open('tools/art/assets_header.md').read()
open('docs/art/ASSETS.md', 'w').write(header + '\n' + '\n'.join(out) + '\n')
from collections import Counter
print(len(rows), Counter(r['status'] for r in rows))
