"""Converts a Blockbench project (.bbmodel, e.g. from Retro Diffusion's blocky 3D export) into a GeckoLib model
(.geo.json, Bedrock geometry 1.12.0) that keeps our skeleton.

The body is assembled in code from separate part models that attach to each other by bone name and pivot (the
socket contract, see docs/art/ASSETS.md). So the new model never invents its own skeleton:

* If the part already has a model, its bones (names, parents, pivots, rotations) are kept exactly and only their
  geometry is replaced. RD's geometry goes into the part's `<part>_frame` bone (or `<part>_mount` for modules), and
  `bone_map` can send an RD group to any other bone (e.g. RD's "blade" group into `rotor_spin`, so it turns).
* A module without a model gets the module skeleton: `<part>` (root) and `<part>_mount`, geometry under the mount.

RD builds models at 16+ texels, while our parts are 2-66 pixels, so the geometry is scaled uniformly to the old model's
size and centered on it (new modules: `fit` pixels on the longest side, standing on the mount). UVs are unchanged: the
texture is the RD atlas, mapped by GeckoLib through `texture_width`/`texture_height`.

The output matches Blockbench's own Bedrock exporter (js/formats/bedrock/bedrock.js, compileCube/compileGroup), which
GeckoLib is built to read: X is mirrored (cube origin x = -to.x, pivots x negated, rotations around X and Y negated),
and per-face UVs of the up/down faces are flipped.

Rotated face UVs are baked into the texture instead of being written as `uv_rotation`: that field needs geometry format
1.21.0, which GeckoLib 4.9.3 only loads with the warning "Supported versions: 1.12.0. Remove any rotated face UVs and
re-export the model". Each rotated face's region is copied, already mirrored and turned clockwise (Blockbench's own
direction, see Cube.updateUV in js/outliner/types/cube.js), into a strip added below the atlas, and the face points at
the copy with a plain UV. The model stays at format 1.12.0.
"""
import base64
import json

from pngutil import png_read, png_write

FACES = ('north', 'east', 'south', 'west', 'up', 'down')


class ConvertError(Exception):
    pass


def slug(name):
    out = ''.join(c if c.isalnum() else '_' for c in str(name)).strip('_').lower()
    while '__' in out:
        out = out.replace('__', '_')
    return out or 'part'


# ---------------------------------------------------------------------------------------------------------------------
# Reading the .bbmodel

def read_bbmodel(data):
    """Returns (tree, elements_by_uuid, texture_png_bytes_or_None, (uv_width, uv_height))."""
    model = json.loads(data) if isinstance(data, (str, bytes)) else data
    elements = {e['uuid']: e for e in model.get('elements', [])}
    groups = {g['uuid']: g for g in model.get('groups', []) if isinstance(g, dict) and 'uuid' in g}

    def node(item):
        if isinstance(item, str):
            if item in elements:
                return ('element', elements[item])
            if item in groups:  # Blockbench 5 may reference groups by uuid
                return node({'uuid': item, 'children': groups[item].get('children', [])})
            return None
        # A group: inline (Blockbench 4: name/origin/rotation in the outliner) or a reference into "groups" (5).
        g = dict(groups.get(item.get('uuid'), {}))
        g.update({k: v for k, v in item.items() if v is not None})
        if g.get('export') is False:
            return None
        children = [n for n in (node(c) for c in item.get('children', g.get('children', []))) if n]
        return ('group', {'name': g.get('name', 'group'), 'uuid': g.get('uuid'), 'origin': g.get('origin', [0, 0, 0]),
                          'rotation': g.get('rotation', [0, 0, 0]), 'children': children})

    outliner = model.get('outliner')
    if outliner is None:
        outliner = list(elements)
    tree = [n for n in (node(i) for i in outliner) if n]

    res = model.get('resolution', {})
    textures = model.get('textures', [])
    png, uv = None, (res.get('width', 16), res.get('height', 16))
    if textures:
        t = textures[0]
        uv = (t.get('uv_width') or uv[0], t.get('uv_height') or uv[1])
        source = t.get('source', '')
        if source.startswith('data:image/png;base64,'):
            png = base64.b64decode(source.split(',', 1)[1])
        used = {f.get('texture') for e in elements.values() for f in (e.get('faces') or {}).values()}
        used.discard(None)
        if len(used) > 1:
            raise ConvertError('the model uses %d textures; GeckoLib parts use one atlas' % len(used))
    for e in elements.values():
        if e.get('type', 'cube') not in ('cube', 'locator', 'null_object'):
            raise ConvertError('element %r is a %s; only boxes convert (use the Blocky style)'
                               % (e.get('name'), e.get('type')))
    return tree, elements, png, uv, bool(model.get('meta', {}).get('box_uv'))


def bake_rotated_faces(elements, png, uv_w, uv_h):
    """Copies every face whose UV is rotated into a new strip under the atlas, turned so it maps without rotation.

    A face's UV (u1, v1, u2, v2) may run backwards (u2 < u1 mirrors the texture), and `rotation` (90/180/270) turns the
    texture clockwise on the face. The copy has the mirror and the turn applied, so the face gets a plain forward UV.
    Returns (png_bytes, new_uv_height, faces_baked)."""
    pixels = png_read(png)
    img_w, img_h = len(pixels[0]), len(pixels)
    k = img_w / float(uv_w)  # image pixels per UV unit
    jobs = []
    for e in elements.values():
        for f in (e.get('faces') or {}).values():
            r = int(f.get('rotation') or 0) % 360
            if not r or f.get('texture') is None:
                continue
            u1, v1, u2, v2 = [int(round(x * k)) for x in f['uv']]
            region = [list(row[min(u1, u2):max(u1, u2)]) for row in pixels[min(v1, v2):max(v1, v2)]]
            if not region or not region[0]:
                f.pop('rotation', None)
                continue
            if u2 < u1:
                region = [row[::-1] for row in region]
            if v2 < v1:
                region = region[::-1]
            for _ in range(r // 90):  # one clockwise quarter turn: new[y][x] = old[h-1-x][y]
                region = [list(row) for row in zip(*region[::-1])]
            jobs.append((f, region))
    if not jobs:
        return png, uv_h, 0
    # Shelf packing, left to right, below the existing atlas.
    x = y = shelf = 0
    placed = []
    for f, region in jobs:
        w, h = len(region[0]), len(region)
        if x + w > img_w:
            x, y, shelf = 0, y + shelf, 0
        placed.append((f, region, x, img_h + y))
        x += w
        shelf = max(shelf, h)
    unit = max(1, int(round(k)))
    extra = -(-(y + shelf) // unit) * unit  # whole UV units
    out = [list(row) for row in pixels] + [[(0, 0, 0, 0)] * img_w for _ in range(extra)]
    for f, region, px, py in placed:
        for dy, row in enumerate(region):
            out[py + dy][px:px + len(row)] = row
        w, h = len(region[0]), len(region)
        f['uv'] = [px / k, py / k, (px + w) / k, (py + h) / k]
        f.pop('rotation', None)
    return png_write(out), uv_h + extra / k, len(placed)


# ---------------------------------------------------------------------------------------------------------------------
# Bedrock pieces (unscaled, Bedrock coordinates)

def to_cube(e, box_uv_project):
    frm, to = e['from'], e['to']
    size = [to[i] - frm[i] for i in range(3)]
    cube = {'origin': [-to[0], frm[1], frm[2]], 'size': size}
    if e.get('inflate'):
        cube['inflate'] = e['inflate']
    rot = e.get('rotation') or [0, 0, 0]
    if any(rot):
        o = e.get('origin', [0, 0, 0])
        cube['pivot'] = [-o[0], o[1], o[2]]
        cube['rotation'] = [-rot[0], -rot[1], rot[2]]
    if e.get('box_uv', box_uv_project) and 'uv_offset' in e:
        cube['uv'] = list(e['uv_offset'])
        if e.get('mirror_uv'):
            cube['mirror'] = True
        return cube
    uv = {}
    for face in FACES:
        f = (e.get('faces') or {}).get(face)
        if not f or f.get('texture') is None:
            continue
        u1, v1, u2, v2 = f['uv']
        tag = {'uv': [u1, v1], 'uv_size': [u2 - u1, v2 - v1]}
        if face in ('up', 'down'):
            tag['uv'] = [u1 + (u2 - u1), v1 + (v2 - v1)]
            tag['uv_size'] = [-(u2 - u1), -(v2 - v1)]
        uv[face] = tag
    cube['uv'] = uv
    return cube


def bone_pivot(origin):
    return [-origin[0], origin[1], origin[2]]


def bone_rotation(rot):
    rot = rot or [0, 0, 0]
    return [-rot[0], -rot[1], rot[2]] if any(rot) else None


def cubes_bbox(bones):
    lo, hi = [float('inf')] * 3, [float('-inf')] * 3
    for b in bones:
        for c in b.get('cubes', []):
            for i in range(3):
                lo[i] = min(lo[i], c['origin'][i])
                hi[i] = max(hi[i], c['origin'][i] + c['size'][i])
    return None if lo[0] == float('inf') else (lo, hi)


# ---------------------------------------------------------------------------------------------------------------------
# Building the model

def select_groups(tree, names):
    """The subtrees of the named RD groups (each with everything inside it), wherever they sit in the outliner."""
    found = []

    def walk(nodes):
        for kind, item in nodes:
            if kind == 'group':
                if item['name'] in names:
                    found.append((kind, item))
                else:
                    walk(item['children'])
    walk(tree)
    missing = [n for n in names if n not in [i['name'] for _, i in found]]
    if missing:
        all_names = []

        def collect(nodes):
            for kind, item in nodes:
                if kind == 'group':
                    all_names.append(item['name'])
                    collect(item['children'])
        collect(tree)
        raise ConvertError('--group names groups that are not in the model: %s (groups: %s)'
                           % (', '.join(missing), ', '.join(all_names) or 'none'))
    return found


def convert(bbmodel, part, old_geo=None, bone_map=None, fit=4.0, scale=True, identifier=None, texture=None,
            groups=None, scale_factor=None, standalone=False, out=None):
    """Builds the .geo.json dict and returns (geo, png_bytes_or_None, report_lines). `texture` is the atlas PNG to
    use when the .bbmodel has none embedded.

    `groups` keeps only those RD groups (to cut one part out of a whole model), `scale_factor` replaces the fit with a
    fixed scale (the same for every part cut from one model), and `standalone` builds a model of its own: a root bone
    named `part` with RD's groups under their own names (for entities and blocks, not body parts). `out`, if given,
    receives `bones_by_uuid` (RD group uuid -> our bone name) and `scale`, for convert_animations."""
    tree, elements, png, (tw, th), box_uv = read_bbmodel(bbmodel)
    if groups:
        tree = select_groups(tree, groups)
        keep = set()

        def mark(nodes):
            for kind, item in nodes:
                if kind == 'element':
                    keep.add(item['uuid'])
                else:
                    mark(item['children'])
        mark(tree)
        elements = {k: v for k, v in elements.items() if k in keep}
    png = png or texture
    baked = 0
    if png:
        png, th, baked = bake_rotated_faces(elements, png, tw, th)
    elif any((f or {}).get('rotation') for e in elements.values() for f in (e.get('faces') or {}).values()):
        raise ConvertError('the model has rotated face UVs but no texture to bake them into')
    bone_map = dict(bone_map or {})
    report = []

    if standalone:
        skeleton = [{'name': part, 'pivot': [0, 0, 0]}]
        names = [part]
        target, old_box = part, None
        description = {'identifier': identifier or 'geometry.void_reclaimer.' + part,
                       'visible_bounds_width': 4, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 1.5, 0]}
    elif old_geo:
        old = old_geo['minecraft:geometry'][0]
        skeleton = [{k: v for k, v in b.items() if k != 'cubes'} for b in old['bones']]
        names = [b['name'] for b in skeleton]
        target = next((n for n in (part + '_frame',) if n in names), None) \
            or next((n for n in names if n.endswith('_frame')), None) \
            or next((n for n in (part + '_mount',) if n in names), None) or names[0]
        old_box = cubes_bbox(old['bones'])
        description = dict(old['description'])
    else:
        skeleton = [{'name': part, 'pivot': [0, 0, 0]}, {'name': part + '_mount', 'parent': part, 'pivot': [0, 0, 0]}]
        names = [b['name'] for b in skeleton]
        target, old_box = part + '_mount', None
        description = {'identifier': identifier or 'geometry.void_reclaimer.module.' + part,
                       'visible_bounds_width': 4, 'visible_bounds_height': 3, 'visible_bounds_offset': [0, 0.5, 0]}
    for rd_group, bone in bone_map.items():
        if bone not in names:
            raise ConvertError('--bone %s=%s: the model has no bone %r (bones: %s)'
                               % (rd_group, bone, bone, ', '.join(names)))
    description['texture_width'] = int(tw) if tw == int(tw) else tw
    description['texture_height'] = int(th) if th == int(th) else th

    new_bones, used = [], set(names)
    by_name = {b['name']: b for b in skeleton}

    def unique(name):
        base, n = name, 2
        while name in used:
            name, n = '%s_%d' % (base, n), n + 1
        used.add(name)
        return name

    seen_groups = []
    bones_by_uuid = {}

    def walk(nodes, parent):
        for kind, item in nodes:
            if kind == 'element':
                if item.get('type', 'cube') != 'cube' or item.get('export') is False:
                    continue
                holder = by_name.get(parent)
                holder.setdefault('cubes', []).append(to_cube(item, box_uv))
                continue
            seen_groups.append(item['name'])
            mapped = bone_map.get(item['name'])
            bone = {'name': unique(slug(item['name']) if standalone else part + '_' + slug(item['name'])),
                    'parent': mapped or parent, 'pivot': bone_pivot(item['origin'])}
            if item.get('uuid'):
                bones_by_uuid[item['uuid']] = bone['name']
            rot = bone_rotation(item['rotation'])
            if rot:
                bone['rotation'] = rot
            new_bones.append(bone)
            by_name[bone['name']] = bone
            walk(item['children'], bone['name'])

    walk(tree, target)
    missing = [g for g in bone_map if g not in seen_groups]
    if missing:
        raise ConvertError('--bone names RD groups that are not in the model: %s (groups: %s)'
                           % (', '.join(missing), ', '.join(seen_groups) or 'none'))
    bones = skeleton + new_bones
    added = [b for b in bones if b.get('cubes')]
    if not added:
        raise ConvertError('the .bbmodel has no boxes')

    # Fit: uniform scale to the old model's longest side (new modules: `fit` pixels), centered on it; new modules stand
    # on the mount (bottom at y = 0). Only RD's geometry and RD's bones move; our skeleton stays exactly in place.
    lo, hi = cubes_bbox(added)
    dims = [hi[i] - lo[i] for i in range(3)]
    s = 1.0
    if scale or scale_factor:
        if old_box:
            olo, ohi = old_box
            s = scale_factor or max(ohi[i] - olo[i] for i in range(3)) / max(dims)
            anchor_new = [(lo[i] + hi[i]) / 2 for i in range(3)]
            anchor_old = [(olo[i] + ohi[i]) / 2 for i in range(3)]
        elif standalone:
            s = scale_factor or fit / max(dims)
            anchor_new = anchor_old = [0.0, 0.0, 0.0]  # keep RD's origin: the model's feet stay where they were
        else:
            s = scale_factor or fit / max(dims)
            anchor_new = [(lo[0] + hi[0]) / 2, lo[1], (lo[2] + hi[2]) / 2]
            anchor_old = [0.0, 0.0, 0.0]

        def move(p):
            return [round((p[i] - anchor_new[i]) * s + anchor_old[i], 4) for i in range(3)]
        for b in bones:
            for c in b.get('cubes', []):
                c['origin'] = move(c['origin'])
                c['size'] = [round(x * s, 4) for x in c['size']]
                if 'pivot' in c:
                    c['pivot'] = move(c['pivot'])
                if 'inflate' in c:
                    c['inflate'] = round(c['inflate'] * s, 4)
        for b in new_bones:
            b['pivot'] = move(b['pivot'])
        report.append('scaled by %.3f (RD model %s px -> %s px on its longest side)'
                      % (s, round(max(dims), 2), round(max(dims) * s, 2)))
    if baked:
        report.append('baked %d rotated face UV(s) into a strip below the texture (now %sx%s UV units)'
                      % (baked, description['texture_width'], description['texture_height']))
    report.append('%d boxes in %d RD groups, under %s%s'
                  % (sum(len(b.get('cubes', [])) for b in bones), len(new_bones), target,
                     ''.join('; %s -> %s' % kv for kv in bone_map.items())))
    if old_geo:
        empty = [b['name'] for b in skeleton if not b.get('cubes') and b['name'] != target and b['name'] in
                 [ob['name'] for ob in old_geo['minecraft:geometry'][0]['bones'] if ob.get('cubes')]]
        if empty:
            report.append('kept as empty bones (their old boxes used the old texture): ' + ', '.join(empty))

    for b in bones:
        if not b.get('parent'):
            b.pop('parent', None)
    if out is not None:
        out['bones_by_uuid'], out['scale'] = bones_by_uuid, s
    geo = {'format_version': '1.12.0', 'minecraft:geometry': [{'description': description, 'bones': bones}]}
    return geo, png, report


def check_contract(geo, bone_names):
    """Returns the contract bones missing from the model (empty when it keeps the socket contract)."""
    have = {b['name'] for b in geo['minecraft:geometry'][0]['bones']}
    return [n for n in bone_names if n not in have]


# ---------------------------------------------------------------------------------------------------------------------
# Animations

LOOP = {'loop': True, 'hold': 'hold_on_last_frame', 'once': False}


def _number(v):
    try:
        return float(v)
    except (TypeError, ValueError):
        return None


def _flip(v, flip):
    """Negates a keyframe value (a number, or a Molang expression GeckoLib evaluates)."""
    if not flip:
        n = _number(v)
        return n if n is not None else v
    n = _number(v)
    if n is not None:
        return -n if n else 0.0
    return '-(%s)' % v


def _times(t):
    return ('%.4f' % t).rstrip('0').rstrip('.') if t else '0.0'


def convert_animations(bbmodel, bones_by_uuid, scale=1.0, prefix='animation.void_reclaimer.model'):
    """Turns a .bbmodel's animations into a GeckoLib .animation.json dict, for the bones of a model converted with
    convert(..., out=...). Follows Blockbench's own Bedrock export (Keyframe.compileBedrockKeyframe): rotation X and Y
    and position X are negated. Positions are scaled with the model. GeckoLib reads only a keyframe's `post` value,
    so a step keyframe is written as a hold: its value is repeated just before the next keyframe, then jumps.
    Returns (animations_dict, report_lines)."""
    model = json.loads(bbmodel) if isinstance(bbmodel, (str, bytes)) else bbmodel
    out, report = {}, []
    for anim in model.get('animations', []):
        bones = {}
        for uuid, animator in (anim.get('animators') or {}).items():
            bone = bones_by_uuid.get(uuid)
            if not bone or animator.get('type', 'bone') != 'bone':
                continue
            channels = {}
            for channel in ('rotation', 'position', 'scale'):
                keys = sorted((k for k in animator.get('keyframes', []) if k.get('channel') == channel),
                              key=lambda k: float(k['time']))
                if not keys:
                    continue
                frames = {}
                for i, k in enumerate(keys):
                    dp = (k.get('data_points') or [{}])[-1]
                    raw = [dp.get(axis, 0) for axis in ('x', 'y', 'z')]
                    if channel == 'rotation':
                        vec = [_flip(raw[0], True), _flip(raw[1], True), _flip(raw[2], False)]
                    elif channel == 'position':
                        vec = [_flip(raw[0], True), _flip(raw[1], False), _flip(raw[2], False)]
                        vec = [v * scale if isinstance(v, float) else '(%s)*%s' % (v, scale) for v in vec]
                    else:
                        vec = [_flip(v, False) for v in raw]
                    vec = [round(v, 5) if isinstance(v, float) else v for v in vec]
                    entry = {'vector': vec}
                    if k.get('interpolation') == 'catmullrom':
                        entry['lerp_mode'] = 'catmullrom'
                    frames[_times(float(k['time']))] = entry
                    if k.get('interpolation') == 'step' and i + 1 < len(keys):
                        hold = float(keys[i + 1]['time']) - 0.0001
                        if hold > float(k['time']):
                            frames[_times(hold)] = {'vector': list(vec)}
                rest = [1.0, 1.0, 1.0] if channel == 'scale' else [0.0, 0.0, 0.0]
                if all(f['vector'] == rest for f in frames.values()):
                    continue  # the bone never moves on this channel
                channels[channel] = frames
            if channels:
                bones[bone] = channels
        name = '%s.%s' % (prefix, slug(anim.get('name', 'animation')))
        entry = {'loop': LOOP.get(anim.get('loop', 'once'), False), 'animation_length': float(anim.get('length') or 0)}
        if entry['loop'] is False:
            del entry['loop']
        entry['bones'] = bones
        out[name] = entry
        report.append('animation %s: %d moving bones, %.2f s, %s' % (name, len(bones), entry['animation_length'],
                                                                   anim.get('loop', 'once')))
    return {'format_version': '1.8.0', 'animations': out}, report


# ---------------------------------------------------------------------------------------------------------------------
# Preview: what the converted model looks like, to compare with RD's render before the game can draw it

# For each view: the face seen, and how screen right / screen down / toward the viewer map to Blockbench axes
# (Minecraft's face UV conventions: u runs to the viewer's right, v runs down; up: u = +X, v = +Z).
VIEWS = (('south', (0, 1), (1, -1), (2, 1)),   # front: right = +X, down = -Y, near = +Z
         ('east', (2, -1), (1, -1), (0, 1)),   # right side: right = -Z, near = +X
         ('north', (0, -1), (1, -1), (2, -1)),  # back: right = -X, near = -Z
         ('west', (2, 1), (1, -1), (0, -1)),   # left side: right = +Z, near = -X
         ('up', (0, 1), (2, 1), (1, 1)))       # top: right = +X, down = +Z, near = +Y


def preview_png(geo, png, zoom=8):
    """Orthographic views of the model's boxes (box and bone rotations are ignored), side by side, as PNG bytes."""
    tex = png_read(png)
    desc = geo['minecraft:geometry'][0]['description']
    tw, th = float(desc['texture_width']), float(desc['texture_height'])
    kx, ky = len(tex[0]) / tw, len(tex) / th
    boxes = []
    for b in geo['minecraft:geometry'][0]['bones']:
        for c in b.get('cubes', []):
            o, s = c['origin'], c['size']
            frm = [-(o[0] + s[0]), o[1], o[2]]  # back to Blockbench coordinates
            to = [frm[0] + s[0], o[1] + s[1], o[2] + s[2]]
            faces = {}
            for face, f in (c.get('uv') or {}).items() if isinstance(c.get('uv'), dict) else ():
                (u, v), (w, h) = f['uv'], f['uv_size']
                if face in ('up', 'down'):  # undo the Bedrock flip
                    faces[face] = (u + w, v + h, u, v)
                else:
                    faces[face] = (u, v, u + w, v + h)
            boxes.append((frm, to, faces))
    if not boxes:
        raise ConvertError('nothing to draw')
    lo = [min(bx[0][i] for bx in boxes) for i in range(3)]
    hi = [max(bx[1][i] for bx in boxes) for i in range(3)]
    span = max(hi[i] - lo[i] for i in range(3)) or 1.0
    cell = 96
    scale = (cell - 8) / span
    out = [[(30, 32, 40, 255)] * (cell * len(VIEWS)) for _ in range(cell)]
    for vi, (face, (rx, rs), (dx, ds), (nx, ns)) in enumerate(VIEWS):
        order = sorted(boxes, key=lambda bx: (bx[1][nx] if ns > 0 else -bx[0][nx]))
        for frm, to, faces in order:
            if face not in faces:
                continue
            u1, v1, u2, v2 = faces[face]
            a0, a1 = sorted(((frm[rx] - lo[rx]) * rs, (to[rx] - lo[rx]) * rs))
            b0, b1 = sorted(((frm[dx] - lo[dx]) * ds, (to[dx] - lo[dx]) * ds))
            off_a = -min(0, (hi[rx] - lo[rx]) * rs)
            off_b = -min(0, (hi[dx] - lo[dx]) * ds)
            for py in range(int((b0 + off_b) * scale), int((b1 + off_b) * scale)):
                for px in range(int((a0 + off_a) * scale), int((a1 + off_a) * scale)):
                    fs = ((px + 0.5) / scale - off_a - a0) / ((a1 - a0) or 1)
                    ft = ((py + 0.5) / scale - off_b - b0) / ((b1 - b0) or 1)
                    tu = (u1 + (u2 - u1) * fs) * kx
                    tv = (v1 + (v2 - v1) * ft) * ky
                    col = tex[min(len(tex) - 1, max(0, int(tv)))][min(len(tex[0]) - 1, max(0, int(tu)))]
                    yy, xx = py + 4, px + 4 + vi * cell
                    if col[3] > 0 and 0 <= yy < cell and 0 <= xx < cell * len(VIEWS):
                        out[yy][xx] = col
    big = [[px for px in row for _ in range(zoom // 2)] for row in out for _ in range(zoom // 2)]
    return png_write(big)
