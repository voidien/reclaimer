#!/usr/bin/env python3
"""Generate the art in docs/art/assets.csv with the Retro Diffusion API.

Reads the asset list, turns each row into Retro Diffusion jobs (pixel images, blocky 3D models, animations),
prices the batch with RD's free estimates, asks before spending, then polls the jobs and saves the results in a
staging folder (art/rd/) with a contact sheet (art/rd/index.html). Picked variants are copied into the mod with
--promote. Nothing is ever written into the mod's assets without --promote.

Standard library only (Python 3.9+). The API key comes from the RD_API_KEY environment variable and is never
written anywhere.

Examples:
  python3 tools/art/rd_generate.py --status placeholder --dry-run          # plan + offline price, no key needed
  python3 tools/art/rd_generate.py --status placeholder                    # price with RD, ask, generate
  python3 tools/art/rd_generate.py --asset 'mech_*' --method 3d                 # asks: Blocky or Blocky Lite
  python3 tools/art/rd_generate.py --asset 'mech_*' --method 3d --model-style blocky_lite --yes
  python3 tools/art/rd_generate.py --promote quartz_wafer=2                # copy variant 2 into the mod
  python3 tools/art/rd_generate.py --balance

See docs/art/ASSETS.md for the workflow and tools/art/README.md for the options.
"""
import argparse
import base64
import csv
import fnmatch
import hashlib
import html
import json
import os
import shutil
import struct
import sys
import time
import urllib.error
import urllib.request
import zlib

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pngutil import png_read, png_write  # noqa: E402
import style_guide  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
CSV_PATH = os.path.join(REPO, 'docs', 'art', 'assets.csv')
STAGING = os.path.join(REPO, 'art', 'rd')
STATE_PATH = os.path.join(STAGING, 'state.json')
API = os.environ.get('RD_API_BASE', 'https://api.retrodiffusion.ai/v2').rstrip('/')
TEXTURES = 'src/main/resources/assets/void_reclaimer/textures/'

# The CSV prompts start with a style prefix meant for people typing into RD's UI. The API's styles already do the
# pixel-art rendering, and RD asks for subject-only prompts, so these are stripped before sending.
PREFIXES = (
    'Minecraft item icon, 16x16 pixel art, transparent background, light from top-left, dark 1px outline, '
    'Create mod industrial style; ',
    'seamless tileable 16x16 Minecraft block texture, pixel art, Create mod style; ',
    '16x16 Minecraft block face texture, pixel art, Create mod style, not tiled; ',
)

# Size limits per style (from RD's llms.txt, 2026-10-29). With a key, the live limits from /styles/selector win.
STYLE_LIMITS = {
    'rd_plus__mc_item': (16, 128, 16), 'rd_plus__mc_texture': (16, 128, 16),
    'rd_fast__mc_item': (16, 128, 16), 'rd_fast__mc_texture': (16, 128, 16),
    'rd_plus__ui_element': (64, 384, 16), 'rd_plus__default': (64, 384, 16), 'rd_plus__environment': (64, 384, 16),
    'rd_fast__ui': (64, 384, 16), 'rd_fast__default': (64, 384, 15),
    'rd_pro__simple': (12, 256, 16), 'rd_pro__scifi': (12, 256, 16),
    'rd_animation__vfx': (24, 96, 1),
}
IMAGE_PRICE = {'rd_fast': 0.03, 'rd_plus': 0.06, 'rd_mini': 0.06, 'rd_pro': 0.18, 'rd_animation': 0.25,
               'user': 0.18}  # 'user' = custom styles, which run on RD Pro
LOWPOLY_PRICE = {16: 0.25, 32: 0.50, 64: 0.85, 128: 1.20, 256: 3.00}
LOWPOLY_ANIMATE_PRICE = {16: 0.15, 32: 0.25, 64: 0.35, 128: 0.50, 256: 1.00}
LITE_GENERATE, LITE_OTHER = 0.10, 0.08
# RD's box-only 3D styles: the only ones whose models fit GeckoLib and Minecraft (Detailed adds cylinders and spheres).
MODEL_STYLES = {'blocky': 'rd_lowpoly__blocky', 'blocky_lite': 'rd_lowpoly__blocky_lite'}

# Rows whose CSV size is '-' or that need a fixed size.
SIZE_OVERRIDES = {
    'power_beam': (16, 64), 'dyson_swarm_sprites': (64, 64), 'ruined_earth sky': (256, 128),
    'acid_drizzle': (64, 256), 'planetary_grid_hud': (256, 256), 'restoration_hud': (256, 256),
    'atmospheric_portal': (16, 16), 'bio_nutrient_slurry': (16, 16), 'cloud_collector': (16, 16),
}
# 3D sizes in texels (the model's largest dimension). Anything else uses "auto".
MODEL_SIZE = {'drone (body)': 64, 'mech (body)': 64, 'ship (body)': 128,
              'drone (first person)': 32, 'mech (first person)': 32, 'ship (first person)': 64}


# ---------------------------------------------------------------------------------------------------------------------
# PNG (enough for RD's outputs and our textures: 8-bit gray/RGB/palette/gray+alpha/RGBA, not interlaced)

def downscale(pixels, tw, th):
    """Pixel-art downscale: each target pixel takes the most common color of its source block (transparent wins
    when most of the block is transparent). Works for any size; exact for integer factors."""
    sh, sw = len(pixels), len(pixels[0])
    if (sw, sh) == (tw, th):
        return pixels
    out = []
    for ty in range(th):
        y0, y1 = ty * sh // th, max(ty * sh // th + 1, (ty + 1) * sh // th)
        row = []
        for tx in range(tw):
            x0, x1 = tx * sw // tw, max(tx * sw // tw + 1, (tx + 1) * sw // tw)
            counts, clear = {}, 0
            for y in range(y0, y1):
                for x in range(x0, x1):
                    px = pixels[y][x]
                    if px[3] < 128:
                        clear += 1
                    else:
                        counts[px] = counts.get(px, 0) + 1
            total = (y1 - y0) * (x1 - x0)
            row.append((0, 0, 0, 0) if clear * 2 > total or not counts else max(counts, key=counts.get))
        out.append(row)
    return out


def upscale(pixels, k):
    return [[px for px in row for _ in range(k)] for row in pixels for _ in range(k)]


def frames_from_sheet(pixels, frame, count):
    """Cuts `count` square frames of size `frame` out of a sprite sheet, row by row."""
    sh, sw = len(pixels), len(pixels[0])
    out = []
    for fy in range(0, sh - frame + 1, frame):
        for fx in range(0, sw - frame + 1, frame):
            if len(out) < count:
                out.append([row[fx:fx + frame] for row in pixels[fy:fy + frame]])
    return out


# ---------------------------------------------------------------------------------------------------------------------
# HTTP

class ApiError(Exception):
    def __init__(self, status, body):
        super().__init__('HTTP %s: %s' % (status, body[:300]))
        self.status, self.body = status, body


def request(method, path, key, body=None, idempotency=None, absolute=False):
    url = path if absolute else API + path
    data = json.dumps(body).encode() if body is not None else None
    req = urllib.request.Request(url, data=data, method=method)
    if key and not absolute:
        req.add_header('X-RD-Token', key)
    if data is not None:
        req.add_header('Content-Type', 'application/json')
    if idempotency:
        req.add_header('Idempotency-Key', idempotency)
    for attempt in range(6):
        try:
            with urllib.request.urlopen(req, timeout=120) as resp:
                raw = resp.read()
                return raw if absolute else json.loads(raw or b'{}')
        except urllib.error.HTTPError as e:
            text = e.read().decode('utf-8', 'replace')
            if e.code in (429, 502, 503, 504) and attempt < 5:
                wait = int(e.headers.get('Retry-After') or 10 * (attempt + 1))
                print('  RD is busy (%s), retrying in %ss' % (e.code, wait))
                time.sleep(wait)
                continue
            raise ApiError(e.code, text)
        except urllib.error.URLError as e:
            if attempt < 5:
                time.sleep(5 * (attempt + 1))
                continue
            raise ApiError(0, str(e))


def download(url):
    return request('GET', url, None, absolute=True)


# ---------------------------------------------------------------------------------------------------------------------
# Rows -> jobs

def slug(name):
    out = ''.join(c if c.isalnum() or c in '_-.' else '_' for c in name).strip('_').lower()
    while '__' in out:
        out = out.replace('__', '_')
    return out


def write_file(path, data):
    with open(path, 'wb') as f:
        f.write(data)


def load_rows():
    with open(CSV_PATH, newline='') as f:
        rows = list(csv.DictReader(f))
    names = {}
    for r in rows:
        names[r['asset']] = names.get(r['asset'], 0) + 1
    for r in rows:
        base = r['asset']
        if names[r['asset']] > 1 and not r['method'].startswith('RD image'):
            base += '_model'  # e.g. nanite_swarm (icon) and nanite_swarm_model (its 3D model)
        r['key'] = slug(base)
    return rows


def subject(prompt):
    for p in PREFIXES:
        if prompt.startswith(p):
            return prompt[len(p):]
    return prompt


def target_size(row):
    if row['asset'] in SIZE_OVERRIDES:
        return SIZE_OVERRIDES[row['asset']]
    size = row['size'].split(' ')[0]
    if 'x' in size:
        w, h = size.split('x')
        return int(w), int(h)
    return 64, 64


def image_style(row):
    """(style, tile, remove_bg, frames) for an image row."""
    method, group, f = row['method'], row['group'], row['file']
    tw, th = target_size(row)
    # Animated textures are square frames stacked into a strip (Nanite Swarm, portal surface); a tall effect
    # texture such as the power beam is a plain image.
    frames = 4 if ('animation' in method.lower() or row['asset'] == 'nanite_swarm') and tw == th else 0
    if frames:
        return 'rd_animation__vfx', False, True, frames
    if '/item/' in f or group in ('Tools & modules', 'Body sections', 'Materials', 'Unfinished (Sequenced Assembly)',
                                  'Fluid buckets') or row['asset'] in ('dust_meter_icons', 'milestone_toast_icon'):
        return 'rd_plus__mc_item', False, True, 0
    if '/block/' in f:
        return 'rd_plus__mc_texture', row['tileable'] == 'yes', False, 0
    if row['asset'].startswith('energy_'):
        return 'rd_pro__simple', False, True, 0
    if '/gui/' in f:
        return 'rd_plus__ui_element', False, True, 0
    if '/environment/' in f or row['asset'] == 'sun':
        return 'rd_plus__environment' if 'sky' in row['asset'] else 'rd_plus__default', False, True, 0
    return 'rd_plus__default', False, False, 0


def fit(style, tw, th, limits):
    """Smallest integer upscale of the target that the style accepts. Returns (w, h, factor)."""
    lo, hi, _ = limits.get(style, (12, 256, 16) if style.startswith('user__') else (16, 384, 16))
    for k in range(1, 65):
        w, h = tw * k, th * k
        if w >= lo and h >= lo:
            if w <= hi and h <= hi:
                return w, h, k
            break
    # Aspect ratio too extreme for the style: clamp each side (the downscale handles the stretch).
    return max(lo, min(hi, tw)), max(lo, min(hi, th)), None


def jobs_for(row, args, limits):
    method = row['method']
    jobs = []
    if method.startswith('RD image'):
        style, tile, remove_bg, frames = image_style(row)
        if args.image_tier != 'plus' and style.startswith('rd_plus__mc_'):
            style = style.replace('rd_plus__', 'rd_%s__' % args.image_tier)
        tw, th = target_size(row)
        if frames:
            side = max(24, min(96, max(tw, th) * 4))
            w = h = side
        else:
            w, h, _ = fit(style, tw, th, limits)
        category = category_for(row, 'image')
        custom = None if args.no_style or frames else custom_styles().get(category)
        if custom:
            style = custom['prompt_style']
            w, h, _ = fit(style, tw, th, limits)
        jobs.append(dict(kind='image', style=style, prompt=styled(subject(row['prompt']), category, args, 1000),
                         width=w, height=h, target=(tw, th), tile=tile, remove_bg=remove_bg, frames=frames,
                         variants=1 if frames else args.variants, palette=None if args.no_style else category))
    if '3D blocky' in method:
        size = args.model_size or MODEL_SIZE.get(row['asset'], 'auto')
        category = category_for(row, 'model')
        jobs.append(dict(kind='model', style=MODEL_STYLES[args.model_style or 'blocky'],
                         prompt=styled(subject(row['prompt']), category, args, 250), size=size,
                         variants=args.model_variants, palette=None if args.no_style else category))
    if method == 'RD 3D animate':
        rig = os.path.basename(row['model']).split('.')[0]
        jobs.append(dict(kind='animate', rig=rig + ' (body)', prompt=row['prompt'][:250],
                         name=row['asset'].split('.')[-1], variants=1))
    return jobs


def category_for(row, kind):
    """The style category (docs/art/style.json) of a row's image or model job."""
    f, group = row['file'], row['group']
    if kind == 'model':
        return 'blocks' if '/textures/block/' in f else 'body'
    if '/textures/gui/' in f or group == 'HUD & GUI':
        return 'gui'
    if '/textures/environment/' in f or group == 'Sky':
        return 'sky'
    if '/textures/block/' in f:
        return 'blocks'
    if '/textures/entity/' in f:
        return 'body'
    return 'items'


def styled(text, category, args, limit):
    """Wraps a subject in its category's prompt template, shortening the subject (not the style) to fit `limit`."""
    if args.no_style:
        return text[:limit]
    template = style_guide.load_style()['categories'][category]['prompt_template']
    # The CSV's 3D prompts end with their own style phrase; the template says it better.
    for tail in ('; single body part, boxy Minecraft-style model', '; small module that mounts on a robot body, '
                 'boxy Minecraft-style model', '; boxy Minecraft-style model'):
        if text.endswith(tail):
            text = text[:-len(tail)] + ('; module that mounts on a robot body' if 'module' in tail else '')
    room = limit - len(template.replace('{prompt}', ''))
    if len(text) > room:
        cut = text[:room]
        text = cut[:max(cut.rfind(' '), 20)]  # whole words only
    return template.replace('{prompt}', text.rstrip(' ,;:'))[:limit]


def palette_b64(category):
    style = style_guide.load_style()
    return base64.b64encode(style_guide.palette_png(style_guide.palette(style, category))).decode()


def styles_path():
    """The account's RD custom style ids, written by --setup-styles (in the gitignored staging folder)."""
    return os.path.join(STAGING, 'styles.json')


def custom_styles():
    if os.path.exists(styles_path()):
        with open(styles_path()) as f:
            return json.load(f)
    return {}


def setup_styles(key):
    """Creates (or updates) an RD custom style for every category with reference images (1-4 in its folder)."""
    style = style_guide.load_style()
    registered = custom_styles()
    for cat, c in style['categories'].items():
        refs, extra = style_guide.reference_files(REPO, c)
        if not refs:
            print('  %s: skipped, no reference images in %s yet' % (cat, c['references']))
            continue
        if extra:
            print('  %s: using the first %d references; ignoring %s' % (cat, len(refs), ', '.join(
                os.path.basename(x) for x in extra)))
        images = []
        for ref in refs:
            with open(ref, 'rb') as f:
                data = f.read()
            if data[:8] == b'\x89PNG\r\n\x1a\n':
                try:
                    data = style_guide.reference_png(data)  # RD wants exactly 256x256
                except ValueError as e:
                    sys.exit('%s: %s; RD needs square 256x256 references' % (os.path.relpath(ref, REPO), e))
            images.append(base64.b64encode(data).decode())
        body = {'name': '%s %s' % (style['name'], c['title']), 'reference_images': images,
                'description': c['covers'][:300],
                'llm_instructions': style_guide.instructions(style, cat),
                'user_prompt_template': c['prompt_template'], 'force_palette': False,
                'force_bg_removal': bool(c.get('remove_background'))}
        old = registered.get(cat)

        def send(b):
            if old:
                return request('PATCH', '/styles/%s' % old['id'], key, b)
            return request('POST', '/styles', key, b)
        try:
            res = send(body)
        except ApiError as e:
            # RD's API summary says custom styles take exactly one reference; its app takes up to 4. If the API
            # refuses several, keep the style with the first one rather than failing.
            if len(images) < 2 or e.status not in (400, 422):
                raise
            print('  %s: RD refused %d references (%s); using only %s'
                  % (cat, len(images), e.status, os.path.basename(refs[0])))
            body['reference_images'] = images[:1]
            res = send(body)
        if old:
            registered[cat] = {'id': old['id'], 'prompt_style': res.get('prompt_style', old['prompt_style'])}
            print('  %s: updated %s (%d references)' % (cat, registered[cat]['prompt_style'],
                                                       len(body['reference_images'])))
        else:
            registered[cat] = {'id': res['id'], 'prompt_style': res['prompt_style']}
            print('  %s: created %s (%d references)' % (cat, res['prompt_style'], len(body['reference_images'])))
        os.makedirs(STAGING, exist_ok=True)
        with open(styles_path(), 'w') as f:
            json.dump(registered, f, indent=1)


def selected(rows, args):
    out = []
    for r in rows:
        if args.status and not any(s.lower() in r['status'].lower() for s in args.status):
            continue
        if args.group and not any(g.lower() in r['group'].lower() for g in args.group):
            continue
        if args.asset and not any(fnmatch.fnmatch(r['asset'], a) or fnmatch.fnmatch(r['key'], a) for a in args.asset):
            continue
        if args.phase and r['phase'] not in args.phase:
            continue
        m = r['method']
        kinds = {'image': m.startswith('RD image'), '3d': '3D blocky' in m, 'anim': m == 'RD 3D animate'}
        if args.method and not any(kinds.get(k) for k in args.method):
            continue
        if not any(kinds.values()):
            continue  # hand work (accent masks) or textures that come with a model
        out.append(r)
    return out[:args.limit] if args.limit else out


def offline_price(job):
    if job['kind'] == 'image':
        tier = job['style'].split('__')[0]
        return IMAGE_PRICE.get(tier, 0.06) * job['variants']
    if job['kind'] == 'model':
        if job['style'].endswith('_lite'):
            return LITE_GENERATE * job['variants']
        size = job['size'] if job['size'] != 'auto' else 32
        return LOWPOLY_PRICE.get(size, 0.85) * job['variants']
    return LITE_OTHER if job.get('style', '').endswith('_lite') else LOWPOLY_ANIMATE_PRICE[64]


def image_body(job, row, seed=None):
    body = dict(prompt=job['prompt'], prompt_style=job['style'], width=job['width'], height=job['height'],
                num_images=job['variants'], remove_bg=job['remove_bg'])
    if job['tile']:
        body.update(tile_x=True, tile_y=True)
    if job['frames']:
        body.update(frames_duration=job['frames'], return_spritesheet=True)
    if seed is not None:
        body['seed'] = seed
    if job.get('palette'):
        body['input_palette'] = palette_b64(job['palette'])
    return body


def model_body(job, row, args):
    body = dict(prompt=job['prompt'], size=job['size'], style=job['style'])
    if job.get('palette'):
        body['input_palette'] = 'data:image/png;base64,' + palette_b64(job['palette'])
    ref = reference_image(row) if not args.no_refs else None
    if ref:
        body['reference_images'] = [ref]
    return body


def reference_image(row):
    """The asset's current item icon, enlarged 8x, as a reference for its 3D model (keeps icon and model alike)."""
    name = row['asset'].split(' (')[0]
    path = os.path.join(REPO, TEXTURES, 'item', name + '.png')
    if not os.path.exists(path):
        return None
    try:
        with open(path, 'rb') as f:
            pixels = png_read(f.read())
    except ValueError:
        return None
    return 'data:image/png;base64,' + base64.b64encode(png_write(upscale(pixels[:16], 8))).decode()


def idem(*parts):
    return hashlib.sha256('|'.join(str(p) for p in parts).encode()).hexdigest()[:48]


# ---------------------------------------------------------------------------------------------------------------------
# State

def load_state():
    if os.path.exists(STATE_PATH):
        with open(STATE_PATH) as f:
            return json.load(f)
    return {'assets': {}}


def save_state(state):
    os.makedirs(STAGING, exist_ok=True)
    tmp = STATE_PATH + '.tmp'
    with open(tmp, 'w') as f:
        json.dump(state, f, indent=1)
    os.replace(tmp, STATE_PATH)


# ---------------------------------------------------------------------------------------------------------------------
# Running

def live_limits(key):
    limits = dict(STYLE_LIMITS)
    try:
        data = request('GET', '/styles/selector', key)
    except ApiError as e:
        print('  (could not read the live style list, using built-in limits: %s)' % e)
        return limits
    items = data if isinstance(data, list) else data.get('items') or data.get('styles') or []
    for it in items:
        if isinstance(it, dict) and it.get('prompt_style'):
            limits[it['prompt_style']] = (it.get('min_width') or 16, it.get('max_width') or 384,
                                          it.get('max_number_of_images') or 1)
    return limits


def set_model_style(plan, name):
    for _, job in plan:
        if job['kind'] in ('model', 'animate'):
            job['style'] = MODEL_STYLES[name]


def price(plan, key):
    total = 0.0
    for row, job in plan:
        cost = None
        if key and job['kind'] == 'image':
            body = image_body(job, row)
            body['check_cost'] = True
            try:
                res = request('POST', '/inferences', key, body)
                res = res.get('result', res)
                cost = float(res.get('balance_cost'))
            except (ApiError, TypeError, ValueError) as e:
                print('  estimate failed for %s: %s' % (row['key'], e))
        elif key and job['kind'] == 'model':
            try:
                res = request('POST', '/lowpoly/estimate', key, dict(operation='generate', prompt=job['prompt'],
                                                                     size=job['size'], style=job['style']))
                cost = float(res['cost']) * job['variants']
            except (ApiError, KeyError, ValueError) as e:
                print('  estimate failed for %s: %s' % (row['key'], e))
        job['price'] = cost if cost is not None else offline_price(job)
        job['price_source'] = 'RD' if cost is not None else 'offline'
        total += job['price']
    return total


def account_balance(key):
    try:
        res = request('GET', '/inferences/credits', key)
        return float(res.get('balance'))
    except (ApiError, TypeError, ValueError):
        return None


def print_plan(plan, total):
    print('%-40s %-9s %-26s %-12s %8s' % ('asset', 'kind', 'style', 'size', 'price'))
    for row, job in plan:
        size = '%sx%s' % (job['width'], job['height']) if job['kind'] == 'image' else str(job.get('size', ''))
        if job['kind'] == 'image' and (job['width'], job['height']) != tuple(job['target']):
            size += '->%sx%s' % job['target']
        style = job.get('style') or ('animate ' + job['rig'])
        print('%-40s %-9s %-26s %-12s %7.2f%s' % (row['key'][:40], job['kind'], style[:26], size, job.get('price', 0),
                                                   '' if job.get('price_source') == 'RD' else '*'))
    print('-' * 100)
    print('%d jobs, total about $%.2f%s' % (len(plan), total,
                                             '  (* = offline estimate from the published price table)'
                                             if any(j.get('price_source') != 'RD' for _, j in plan) else ''))


def submit(row, job, key, state, salt):
    a = state['assets'].setdefault(row['key'], {'group': row['group'], 'asset': row['asset'], 'file': row['file'],
                                                'model': row['model'], 'jobs': []})
    if job['kind'] == 'image':
        body = image_body(job, row)
        k = idem('image', row['key'], json.dumps(body, sort_keys=True), salt)
        res = request('POST', '/inferences', key, body, idempotency=k)
        rec = dict(kind='image', task=res['task_id'], target=list(job['target']), frames=job['frames'],
                   style=job['style'], status='submitted', salt=salt)
    elif job['kind'] == 'model':
        body = None
        rec = None
        for v in range(job['variants']):
            body = model_body(job, row, ARGS)
            k = idem('model', row['key'], job['prompt'], job['size'], job['style'], v, salt)
            res = request('POST', '/lowpoly/generate', key, body, idempotency=k)
            rec = dict(kind='model', task=res['task_id'], asset_id=res.get('asset_id'), status='submitted', salt=salt,
                       style=job['style'])
            add_job(a, rec)
        save_state(state)
        return
    else:
        rig = state['assets'].get(slug(job['rig']))
        model = next((j for j in reversed(rig['jobs']) if j['kind'] == 'model' and j['status'] == 'done'), None) \
            if rig else None
        if not model:
            print('  skip %s: generate the %s model first (--asset "%s")' % (row['key'], job['rig'], job['rig']))
            return
        k = idem('animate', row['key'], model['asset_id'], job['prompt'], salt)
        res = request('POST', '/lowpoly/assets/%s/animate' % model['asset_id'], key,
                      dict(prompt=job['prompt']), idempotency=k)
        rec = dict(kind='animate', task=res['task_id'], asset_id=model['asset_id'], rig=slug(job['rig']),
                   name=job['name'], status='submitted', salt=salt)
    add_job(a, rec)
    save_state(state)


def add_job(asset, rec):
    """Adds a job record; a resubmit that RD answered with a task we already know (same Idempotency-Key) reuses it."""
    for old in asset['jobs']:
        if old['task'] == rec['task']:
            if old['status'] != 'done':
                old['status'] = 'submitted'
            return
    asset['jobs'].append(rec)


def poll(key, state):
    """Polls every unfinished job until all are done or failed, saving results as they arrive."""
    while True:
        open_jobs = [(k, j) for k, a in state['assets'].items() for j in a['jobs']
                     if j['status'] in ('submitted', 'pending', 'running')]
        if not open_jobs:
            return
        for akey, job in open_jobs:
            path = ('/inferences/tasks/%s' if job['kind'] == 'image' else '/lowpoly/tasks/%s') % job['task']
            try:
                res = request('GET', path, key)
            except ApiError as e:
                print('  %s: poll failed (%s), will retry' % (akey, e))
                continue
            status = res.get('status')
            if status == 'succeeded':
                try:
                    collect(akey, job, res, key, state)
                    job['status'] = 'done'
                except Exception as e:  # keep going; the job can be re-collected with --collect
                    job['status'], job['error'] = 'collect_failed', str(e)
                    print('  %s: could not save the result: %s' % (akey, e))
                else:
                    print('  %s: done' % akey)
            elif status == 'failed':
                job['status'], job['error'] = 'failed', json.dumps(res.get('error'))
                print('  %s: FAILED %s' % (akey, job['error']))
            else:
                job['status'] = status or job['status']
            save_state(state)
        still = sum(1 for _, a in state['assets'].items() for j in a['jobs'] if j['status'] in ('submitted', 'pending', 'running'))
        if still:
            print('  %d job(s) still running...' % still)
            time.sleep(POLL_SECONDS)


def collect(akey, job, res, key, state):
    folder = os.path.join(STAGING, akey)
    os.makedirs(os.path.join(folder, 'raw'), exist_ok=True)
    if job['kind'] == 'image':
        result = res.get('result') or {}
        blobs = [base64.b64decode(b) for b in (result.get('base64_images') or [])]
        if not blobs:
            blobs = [download(u) for u in (result.get('output_urls') or [])]
        if not blobs:
            raise ValueError('no images in the result')
        # Variants are numbered across jobs (v1-v4, then v5-v8 from an --again run); collecting the same job again
        # overwrites its own numbers.
        if 'first' not in job:
            job['first'] = 1 + sum(len(j.get('variants') or []) for j in state['assets'][akey]['jobs']
                                   if j is not job and j['kind'] == 'image')
        tw, th = job['target']
        job['variants'] = []
        for i, blob in enumerate(blobs):
            n = job['first'] + i
            write_file(os.path.join(folder, 'raw', 'v%d.png' % n), blob)
            if blob[:3] == b'GIF':
                job['variants'].append({'n': n, 'note': 'GIF returned; ask for a sprite sheet'})
                continue
            pixels = png_read(blob)
            if job['frames']:
                side = min(len(pixels), len(pixels[0]))
                frames = frames_from_sheet(pixels, side if len(pixels) == len(pixels[0]) else
                                           min(len(pixels), len(pixels[0])), job['frames'])
                if len(frames) < job['frames']:
                    side = max(len(pixels), len(pixels[0])) // job['frames']
                    frames = frames_from_sheet(pixels, side, job['frames'])
                pixels = [row for fr in frames for row in downscale(fr, tw, th)]
            else:
                pixels = downscale(pixels, tw, th)
            write_file(os.path.join(folder, 'v%d.png' % n), png_write(pixels))
            opaque = all(px[3] == 255 for row in pixels for px in row)
            job['variants'].append({'n': n, 'size': [len(pixels[0]), len(pixels)], 'opaque': opaque})
        job['cost'] = result.get('balance_cost')
    elif job['kind'] == 'model':
        result = res.get('result') or {}
        job['asset_id'] = result.get('asset_id', job.get('asset_id'))
        versions = result.get('versions') or []
        if versions:
            v = versions[-1]
            for name, url in (('render.png', v.get('render_url')), ('turntable.png', v.get('turntable_url'))):
                if url:
                    write_file(os.path.join(folder, name), download(url))
        export_model(folder, job['asset_id'], key)
    else:
        job['animation'] = res.get('animation') or job['name']
        rig_folder = os.path.join(STAGING, job['rig'])
        os.makedirs(rig_folder, exist_ok=True)
        export_model(rig_folder, job['asset_id'], key)  # the .bbmodel carries every animation


def export_model(folder, asset_id, key):
    for target, name in (('blockbench', 'model.bbmodel'), ('atlas', 'atlas.png')):
        res = request('POST', '/lowpoly/assets/%s/export' % asset_id, key, {'target': target})
        write_file(os.path.join(folder, name), download(res['url']))


# ---------------------------------------------------------------------------------------------------------------------
# Contact sheet and promotion

MEDIA_TYPES = ((b'\x89PNG', 'image/png'), (b'GIF8', 'image/gif'), (b'\xff\xd8', 'image/jpeg'), (b'RIFF', 'image/webp'))


def embedded(path):
    """A file as a data: URI, so the contact sheet is one self-contained file: it opens anywhere (a phone, a file
    preview, a browser that blocks local files) and can be sent on. The type comes from the bytes, not the name."""
    with open(path, 'rb') as f:
        data = f.read()
    kind = next((t for magic, t in MEDIA_TYPES if data.startswith(magic)), None)
    if kind is None and data[4:8] == b'ftyp':
        kind = 'video/mp4'  # a turntable may come as a video
    return kind or 'application/octet-stream', 'data:%s;base64,%s' % (kind or 'application/octet-stream',
                                                                       base64.b64encode(data).decode())


def media_tag(path, title, size):
    kind, uri = embedded(path)
    if kind.startswith('video/'):
        return '<video src="%s" %s autoplay loop muted playsinline title="%s"></video>' % (uri, size, html.escape(title))
    return '<img src="%s" %s title="%s">' % (uri, size, html.escape(title))


def write_index(state):
    parts = ['<!doctype html><meta charset="utf-8"><title>RD staging</title><style>'
             'body{font:14px system-ui;background:#1b1d22;color:#ddd;margin:16px}'
             'img,video{image-rendering:pixelated;background:repeating-conic-gradient(#333 0 25%,#444 0 50%) 0 0/16px 16px;'
             'margin:4px;vertical-align:top}.a{border-top:1px solid #333;padding:8px 0}code{color:#9fe2ff}'
             '.f{color:#f88}</style><h1>Retro Diffusion staging</h1>'
             '<p>Promote a variant with <code>python3 tools/art/rd_generate.py --promote &lt;asset&gt;=&lt;n&gt;</code>.</p>']
    for akey in sorted(state['assets']):
        a = state['assets'][akey]
        folder = os.path.join(STAGING, akey)
        parts.append('<div class="a"><b>%s</b> <small>%s</small><br>' % (html.escape(akey), html.escape(a['group'])))
        current = os.path.join(REPO, a['file']) if a.get('file') else ''
        if current and os.path.exists(current):
            parts.append(media_tag(current, 'current', 'width="96"'))
        for f in sorted(os.listdir(folder)) if os.path.isdir(folder) else []:
            if f.startswith('v') and f.endswith('.png'):
                parts.append(media_tag(os.path.join(folder, f), f, 'width="96"'))
            elif f in ('render.png', 'turntable.png'):
                parts.append(media_tag(os.path.join(folder, f), f, 'height="160"'))
            elif f == 'model.bbmodel':
                parts.append('<a href="%s/%s">model.bbmodel</a> ' % (html.escape(akey), f))
        for j in a['jobs']:
            if j['status'] not in ('done',):
                parts.append('<div class="f">%s: %s %s</div>' % (j['kind'], j['status'], html.escape(j.get('error', ''))))
        parts.append('</div>')
    os.makedirs(STAGING, exist_ok=True)
    with open(os.path.join(STAGING, 'index.html'), 'w') as f:
        f.write('\n'.join(parts))


def promote(spec, rows):
    akey, _, n = spec.partition('=')
    row = next((r for r in rows if r['key'] == akey), None)
    if not row:
        sys.exit('unknown asset %r (use the name shown in art/rd/index.html)' % akey)
    if '3D' in row['method'] and not row['method'].startswith('RD image'):
        return promote_model(row, ARGS)
    src = os.path.join(STAGING, akey, 'v%s.png' % (n or '1'))
    if not os.path.exists(src):
        sys.exit('no variant %s for %s' % (n or '1', akey))
    if not row['file'] or '*' in row['file'] or '<' in row['file'] or '{' in row['file']:
        sys.exit('%s has no single target file yet (%s): copy it by hand' % (akey, row['file'] or 'model'))
    dst = os.path.join(REPO, row['file'])
    want = target_size(row)
    with open(src, 'rb') as f:
        pixels = png_read(f.read())
    if row['asset'] != 'nanite_swarm' and (len(pixels[0]), len(pixels)) != tuple(want):
        sys.exit('%s is %dx%d, expected %dx%d' % (src, len(pixels[0]), len(pixels), want[0], want[1]))
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copyfile(src, dst)
    print('copied %s -> %s' % (os.path.relpath(src, REPO), row['file']))


def promote_model(row, args):
    """Converts the staged .bbmodel into the part's GeckoLib model and texture (see bbgeo.py), backing up what it
    replaces into art/rd/<asset>/replaced/."""
    import bbgeo
    akey = row['key']
    model_rel = row['model']
    if not model_rel.endswith('.geo.json'):
        sys.exit('%s has no model file yet (%s): its block or entity is not built, so copy it by hand later'
                 % (akey, model_rel))
    src = args.source or os.path.join(STAGING, akey, 'model.bbmodel')
    if not os.path.exists(src):
        sys.exit('no model for %s (expected %s); generate it first, or name one with --source'
                 % (akey, os.path.relpath(src, REPO)))
    part = os.path.basename(model_rel)[:-len('.geo.json')]
    folder = os.path.basename(os.path.dirname(model_rel))
    tex_name = ('first_person_' if folder == 'first_person' else '') + part
    tex_rel = TEXTURES + 'entity/body/%s.png' % tex_name
    accent_rel = TEXTURES + 'entity/body/%s_accent.png' % tex_name
    geo_path = os.path.join(REPO, model_rel)
    old_geo = None
    if os.path.exists(geo_path):
        with open(geo_path) as f:
            old_geo = json.load(f)
    bone_map = {}
    for spec in args.bone or []:
        rd_group, _, bone = spec.partition('=')
        if not bone:
            sys.exit('--bone takes RD_GROUP=OUR_BONE, got %r' % spec)
        bone_map[rd_group] = bone
    with open(src, 'rb') as f:
        data = f.read()
    atlas = os.path.join(os.path.dirname(src), 'atlas.png')
    fallback = None
    if os.path.exists(atlas):
        with open(atlas, 'rb') as f:
            fallback = f.read()
    scale_factor = args.scale
    if args.scale_like and not scale_factor:
        scale_factor = whole_model_scale(data, args.scale_like)
    try:
        geo, png, report = bbgeo.convert(data, part, old_geo, bone_map, fit=args.fit, scale=not args.no_fit,
                                         identifier='geometry.void_reclaimer.%s.%s' % (folder, part), texture=fallback,
                                         groups=args.from_group, scale_factor=scale_factor)
    except (bbgeo.ConvertError, ValueError, KeyError) as e:
        sys.exit('%s: cannot convert %s: %s' % (akey, os.path.relpath(src, REPO), e))
    if png is None:
        sys.exit('%s: the .bbmodel has no embedded texture and there is no atlas.png' % akey)
    pixels = png_read(png)

    contract = [part, part + '_mount'] if not old_geo else \
        [b.strip() for b in row['notes'].split('Bones: ', 1)[1].split(',')] if 'Bones: ' in row['notes'] else \
        [b['name'] for b in old_geo['minecraft:geometry'][0]['bones']]
    missing = bbgeo.check_contract(geo, contract)
    if missing:
        sys.exit('%s: the converted model lost contract bones: %s' % (akey, ', '.join(missing)))

    backup = os.path.join(STAGING, akey, 'replaced')
    for rel in (model_rel, tex_rel, accent_rel):
        path = os.path.join(REPO, rel)
        if os.path.exists(path):
            os.makedirs(backup, exist_ok=True)
            shutil.copyfile(path, os.path.join(backup, os.path.basename(rel)))
    os.makedirs(os.path.dirname(geo_path), exist_ok=True)
    with open(geo_path, 'w') as f:
        json.dump(geo, f, indent='\t')
        f.write('\n')
    os.makedirs(os.path.dirname(os.path.join(REPO, tex_rel)), exist_ok=True)
    write_file(os.path.join(REPO, tex_rel), png)
    # The old accent mask matched the old texture layout, so it is replaced by an empty one (no accent tint) to paint.
    write_file(os.path.join(REPO, accent_rel), png_write([[(0, 0, 0, 0)] * len(pixels[0]) for _ in pixels]))
    preview = os.path.join(STAGING, akey, 'promoted_preview.png')
    try:
        write_file(preview, bbgeo.preview_png(geo, png))
    except bbgeo.ConvertError:
        preview = None
    print('promoted %s:' % akey)
    print('  model   -> %s' % model_rel)
    print('  texture -> %s (%dx%d)' % (tex_rel, len(pixels[0]), len(pixels)))
    print('  accent  -> %s (empty: paint it in Blockbench to get the accent tint)' % accent_rel)
    for line in report:
        print('  ' + line)
    if os.path.isdir(backup):
        print('  the old files are in %s' % os.path.relpath(backup, REPO))
    if preview:
        print('  preview: %s (front, right, back, left, top; compare with render.png)' % os.path.relpath(preview, REPO))
    print('  The game draws these from Phase 5b (body rendering) on.')


def whole_model_scale(data, like):
    """The scale that makes the whole .bbmodel as long (on its longest side) as one of our models, e.g. body/ship,
    so that every part cut out of it with --from-group keeps the same proportions."""
    import bbgeo
    path = os.path.join(REPO, 'src/main/resources/assets/void_reclaimer/geo/%s.geo.json' % like)
    if not os.path.exists(path):
        sys.exit('--scale-like %s: no model at %s' % (like, os.path.relpath(path, REPO)))
    with open(path) as f:
        ours = bbgeo.cubes_bbox(json.load(f)['minecraft:geometry'][0]['bones'])
    whole, _, _ = bbgeo.convert(data, 'whole', standalone=True, scale=False, texture=png_write([[(0, 0, 0, 0)]]))
    theirs = bbgeo.cubes_bbox(whole['minecraft:geometry'][0]['bones'])
    s = max(ours[1][i] - ours[0][i] for i in range(3)) / max(theirs[1][i] - theirs[0][i] for i in range(3))
    print('  scale %.3f: the whole model sized like %s' % (s, like))
    return s


def promote_as(target, args):
    """Converts a whole .bbmodel into a standalone GeckoLib model with its animations, for an entity or a block
    (`entity/NAME` or `block/NAME`): RD's group names become the bones, and nothing is fitted unless --fit/--scale is
    given. Writes geo/<target>.geo.json, animations/<target>.animation.json (when there are animations) and
    textures/<target>.png, backing up what it replaces next to the source."""
    import bbgeo
    folder, _, name = target.partition('/')
    if folder not in ('entity', 'block') or not name or slug(name) != name:
        sys.exit('--promote-as takes entity/NAME or block/NAME with a lowercase name, got %r' % target)
    if not args.source or not os.path.exists(args.source):
        sys.exit('--promote-as needs --source FILE.bbmodel')
    with open(args.source, 'rb') as f:
        data = f.read()
    atlas = os.path.join(os.path.dirname(args.source), 'atlas.png')
    fallback = None
    if os.path.exists(atlas):
        with open(atlas, 'rb') as f:
            fallback = f.read()
    info = {}
    try:
        geo, png, report = bbgeo.convert(data, name, None, fit=args.fit, scale=bool(args.fit_given),
                                         identifier='geometry.void_reclaimer.%s.%s' % (folder, name), texture=fallback,
                                         groups=args.from_group, scale_factor=args.scale, standalone=True, out=info)
        anims, anim_report = bbgeo.convert_animations(data, info['bones_by_uuid'], info['scale'],
                                                      'animation.void_reclaimer.%s' % name)
    except (bbgeo.ConvertError, ValueError, KeyError) as e:
        sys.exit('%s: cannot convert %s: %s' % (target, args.source, e))
    if png is None:
        sys.exit('%s: the .bbmodel has no embedded texture and there is no atlas.png next to it' % target)
    assets = 'src/main/resources/assets/void_reclaimer/'
    files = {assets + 'geo/%s.geo.json' % target: json.dumps(geo, indent='\t') + '\n',
             TEXTURES + '%s.png' % target: png}
    if anims['animations']:
        files[assets + 'animations/%s.animation.json' % target] = json.dumps(anims, indent='\t') + '\n'
    backup = os.path.join(os.path.dirname(os.path.abspath(args.source)), 'replaced')
    for rel, content in files.items():
        path = os.path.join(REPO, rel)
        if os.path.exists(path):
            os.makedirs(backup, exist_ok=True)
            shutil.copyfile(path, os.path.join(backup, os.path.basename(rel)))
        os.makedirs(os.path.dirname(path), exist_ok=True)
        write_file(path, content if isinstance(content, bytes) else content.encode())
    preview = os.path.join(os.path.dirname(os.path.abspath(args.source)), name + '_preview.png')
    write_file(preview, bbgeo.preview_png(geo, png, zoom=4))
    print('promoted %s as %s:' % (args.source, target))
    for rel in files:
        print('  -> %s' % rel)
    for line in report + anim_report:
        print('  ' + line)
    print('  preview: %s' % preview)
    names = list(info['bones_by_uuid'].values())
    print('  Bones keep RD\'s group names (%s%s); the entity or block that draws them comes in its own phase.'
          % (', '.join(names[:8]), ', ...' if len(names) > 8 else ''))


# ---------------------------------------------------------------------------------------------------------------------

POLL_SECONDS = float(os.environ.get('RD_POLL_SECONDS', '12'))
ARGS = None


def main(argv=None):
    global ARGS
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--status', action='append', help='filter: status contains (placeholder, "art drop", missing, future)')
    p.add_argument('--group', action='append', help='filter: group contains (Materials, Blocks, "Body models", ...)')
    p.add_argument('--asset', action='append', help='filter: asset name or glob (quartz_wafer, "mech_*")')
    p.add_argument('--phase', action='append', help='filter: phase (now, 9, 11, later, ...)')
    p.add_argument('--method', action='append', choices=['image', '3d', 'anim'], help='filter: kind of job')
    p.add_argument('--limit', type=int, help='at most this many rows')
    p.add_argument('--variants', type=int, default=4, help='images per asset (default 4)')
    p.add_argument('--model-variants', type=int, default=1, help='3D models per asset (default 1)')
    p.add_argument('--model-size', type=int, choices=[16, 32, 64, 128, 256], help='3D size in texels (default: auto)')
    p.add_argument('--model-style', choices=sorted(MODEL_STYLES),
                   help='3D style: blocky ($0.25-3.00 by size) or blocky_lite ($0.10 flat, simpler; animations $0.08). '
                        'Without it, a run that makes models shows both prices and asks.')
    p.add_argument('--image-tier', choices=['plus', 'fast', 'mini'], default='plus', help='Minecraft image styles tier')
    p.add_argument('--no-refs', action='store_true', help="don't send the item icon as a 3D reference image")
    p.add_argument('--no-style', action='store_true',
                   help="don't apply docs/art/style.json (palette, prompt template, custom styles) to this run")
    p.add_argument('--setup-styles', action='store_true',
                   help='create or update the RD custom styles for categories with reference images (up to 4 each)')
    p.add_argument('--again', action='store_true', help='new jobs even for assets generated before')
    p.add_argument('--dry-run', action='store_true', help='plan and offline price only; no key needed, nothing sent')
    p.add_argument('--yes', action='store_true', help="don't ask before spending")
    p.add_argument('--resume', action='store_true', help='only poll jobs already submitted')
    p.add_argument('--promote', action='append', help='copy a staged pick into the mod: asset=n for an image, asset for a 3D model')
    p.add_argument('--bone', action='append', metavar='RD_GROUP=OUR_BONE',
                   help='model promote: put an RD group under one of our bones (e.g. blade=rotor_spin)')
    p.add_argument('--fit', type=float, default=4.0,
                   help='model promote, new modules: longest side in pixels (default 4, like the thrusters)')
    p.add_argument('--no-fit', action='store_true', help="model promote: keep RD's size and position")
    p.add_argument('--source', help='model promote: the .bbmodel to use instead of the staged one')
    p.add_argument('--from-group', action='append', metavar='RD_GROUP',
                   help='model promote: use only this RD group (and what is inside it), e.g. to cut a Ship section '
                        'out of a whole-ship model; repeatable')
    p.add_argument('--scale', type=float,
                   help='model promote: a fixed scale instead of fitting, e.g. the same for every part of one ship')
    p.add_argument('--scale-like', metavar='FOLDER/MODEL',
                   help='model promote: scale so the WHOLE source model is as long as this model of ours '
                        '(e.g. body/ship); every part cut from it then shares one scale')
    p.add_argument('--promote-as', metavar='entity/NAME',
                   help='convert --source into a standalone GeckoLib model with its animations (entity/NAME or '
                        'block/NAME), keeping RD\'s group names as bones')
    p.add_argument('--balance', action='store_true', help='show the account balance')
    p.add_argument('--index', action='store_true', help='rewrite art/rd/index.html')
    args = ARGS = p.parse_args(argv)
    rows = load_rows()

    args.fit_given = any(a == '--fit' or a.startswith('--fit=') for a in (argv if argv is not None else sys.argv[1:]))
    if args.promote_as:
        promote_as(args.promote_as, args)
        return 0
    if args.promote:
        for spec in args.promote:
            promote(spec, rows)
        return 0
    if args.index:
        write_index(load_state())
        print('wrote', os.path.relpath(os.path.join(STAGING, 'index.html'), REPO))
        return 0

    key = os.environ.get('RD_API_KEY', '').strip()
    if not key and not args.dry_run:
        sys.exit('Set RD_API_KEY (your Retro Diffusion key, rdpk-...), or use --dry-run.')
    if args.setup_styles:
        setup_styles(key)
        return 0
    if args.balance:
        print(request('GET', '/inferences/credits', key))
        return 0

    state = load_state()
    if args.resume:
        poll(key, state)
        write_index(state)
        return 0

    limits = live_limits(key) if key and not args.dry_run else dict(STYLE_LIMITS)
    plan = []
    for row in selected(rows, args):
        done = state['assets'].get(row['key'], {}).get('jobs', [])
        for job in jobs_for(row, args, limits):
            if not args.again and any(j['kind'] == job['kind'] and j['status'] in ('done', 'submitted', 'pending', 'running')
                                      for j in done):
                continue
            plan.append((row, job))
    if not plan:
        print('Nothing to do (no matching rows, or all generated already; --again makes new ones).')
        return 0
    # 3D jobs can use Blocky or Blocky Lite. Without --model-style, price both and let the user pick.
    has_models = any(j['kind'] in ('model', 'animate') for _, j in plan)
    styles = [args.model_style] if args.model_style or not has_models else ['blocky', 'blocky_lite']
    totals = {}
    for name in styles:
        set_model_style(plan, name)
        totals[name] = price(plan, None if args.dry_run else key)
    print_plan(plan, totals[styles[-1]])
    chosen = styles[0]
    if len(styles) > 1:
        print('3D style: Blocky about $%.2f in total, Blocky Lite about $%.2f (Lite: $0.10 a model, simpler builds).'
              % (totals['blocky'], totals['blocky_lite']))
        if args.dry_run:
            return 0
        if args.yes:
            sys.exit('This batch makes 3D models: choose --model-style blocky or blocky_lite to run it with --yes.')
        answer = input('Model style: [b]locky $%.2f, blocky [l]ite $%.2f, or [n]othing: '
                       % (totals['blocky'], totals['blocky_lite'])).strip().lower()
        if answer not in ('b', 'blocky', 'l', 'lite', 'blocky_lite'):
            print('Nothing sent.')
            return 0
        chosen = 'blocky' if answer.startswith('b') else 'blocky_lite'
        set_model_style(plan, chosen)
        print('Using %s.' % chosen)
        args.yes = True  # picking a priced style is the go-ahead
    total = totals[chosen]
    if args.dry_run:
        return 0
    balance = account_balance(key)
    if balance is not None and balance < total:
        print('Your balance is $%.2f, less than this batch (about $%.2f). Top up at retrodiffusion.ai, '
              'or narrow the batch (--limit, --asset, --variants 1, --model-style blocky_lite).' % (balance, total))
        return 1
    if not args.yes:
        answer = input('Spend about $%.2f on these %d jobs? [y/N] ' % (total, len(plan))).strip().lower()
        if answer not in ('y', 'yes'):
            print('Nothing sent.')
            return 0
    salt = str(time.time()) if args.again else ''
    models = [(r, j) for r, j in plan if j['kind'] != 'image']
    for row, job in [(r, j) for r, j in plan if j['kind'] == 'image'] + [(r, j) for r, j in models if j['kind'] == 'model']:
        try:
            submit(row, job, key, state, salt)
            print('  submitted %s (%s)' % (row['key'], job['kind']))
        except ApiError as e:
            print('  %s: not submitted: %s' % (row['key'], e))
    poll(key, state)
    # Animations need their rig's model, so they go after the models are done.
    for row, job in [(r, j) for r, j in models if j['kind'] == 'animate']:
        try:
            submit(row, job, key, state, salt)
        except ApiError as e:
            print('  %s: not submitted: %s' % (row['key'], e))
    poll(key, state)
    write_index(state)
    failed = [k for k, a in state['assets'].items() for j in a['jobs'] if j['status'] not in ('done',)]
    print('Results: art/rd/index.html' + ('  (%d not finished: see the page)' % len(failed) if failed else ''))
    return 0


if __name__ == '__main__':
    sys.exit(main())
