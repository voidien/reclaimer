"""Offline tests for rd_generate.py against a fake Retro Diffusion server (no key, no network, no cost).

Run: python3 tools/art/test_rd_generate.py
"""
import base64
import csv
import http.server
import json
import os
import re
import shutil
import sys
import tempfile
import threading
import unittest
from unittest import mock

os.environ['NO_PROXY'] = os.environ['no_proxy'] = '127.0.0.1,localhost'
for var in ('HTTP_PROXY', 'HTTPS_PROXY', 'http_proxy', 'https_proxy'):
    os.environ.pop(var, None)
os.environ['RD_POLL_SECONDS'] = '0'
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import rd_generate as rd  # noqa: E402

REAL_REPO = rd.REPO


def checker(w, h, transparent_corner=True):
    px = [[((x // 2 + y // 2) % 2 * 200, 100, 50, 0 if transparent_corner and x < w // 4 and y < h // 4 else 255)
           for x in range(w)] for y in range(h)]
    return rd.png_write(px)


def bbmodel(groups_v5=False, extra_elements=(), textures=1):
    """A small Blockbench project like RD's blocky export: a loose box, a "body" group and a rotated "blade" group."""
    def face(u1, v1, u2, v2):
        return {'uv': [u1, v1, u2, v2], 'texture': 0}
    def box(uuid, frm, to, origin=(0, 0, 0), rotation=(0, 0, 0)):
        return {'uuid': uuid, 'name': uuid, 'type': 'cube', 'from': list(frm), 'to': list(to), 'origin': list(origin),
                'rotation': list(rotation), 'faces': {f: face(0, 0, 8, 8) for f in
                                                      ('north', 'east', 'south', 'west', 'up', 'down')}}
    elements = [box('loose', (2, 0, 0), (6, 2, 2)), box('core', (-4, 0, -4), (4, 8, 4)),
                box('fin', (-1, 8, -1), (1, 16, 1), origin=(0, 8, 0), rotation=(0, 0, 22.5))] + list(extra_elements)
    png = base64.b64encode(checker(32, 32, transparent_corner=False)).decode()
    model = {'meta': {'format_version': '4.10', 'model_format': 'free', 'box_uv': False}, 'name': 'rd',
             'resolution': {'width': 32, 'height': 32}, 'elements': elements,
             'textures': [{'name': 'atlas', 'source': 'data:image/png;base64,' + png, 'uv_width': 32, 'uv_height': 32}]
             * textures}
    blade = {'name': 'blade', 'uuid': 'g2', 'origin': [0, 8, 0], 'rotation': [0, 45, 0], 'children': ['fin']}
    body = {'name': 'body', 'uuid': 'g1', 'origin': [0, 0, 0], 'rotation': [0, 0, 0], 'children': ['core', blade]}
    if groups_v5:
        model['groups'] = [{k: v for k, v in g.items() if k != 'children'} for g in (body, blade)]
        model['outliner'] = ['loose', {'uuid': 'g1', 'children': ['core', {'uuid': 'g2', 'children': ['fin']}]}]
    else:
        model['outliner'] = ['loose', body]
    model['outliner'] += [e['uuid'] for e in extra_elements]
    return json.dumps(model)


class Fake(http.server.BaseHTTPRequestHandler):
    server_version = 'fake-rd'
    state = None

    def log_message(self, *a):
        pass

    def reply(self, obj, code=200, raw=None):
        body = raw if raw is not None else json.dumps(obj).encode()
        self.send_response(code)
        self.send_header('Content-Length', str(len(body)))
        self.end_headers()
        self.wfile.write(body)

    def body(self):
        n = int(self.headers.get('Content-Length') or 0)
        return json.loads(self.rfile.read(n) or b'{}')

    def do_GET(self):
        s = Fake.state
        s['calls'].append(('GET', self.path))
        if self.path.startswith('/files/'):
            return self.reply(None, raw=checker(64, 64) if self.path.endswith('.png') else b'{"meta":{}}')
        if self.path == '/v2/styles/selector':
            return self.reply([{'prompt_style': 'rd_plus__mc_item', 'min_width': 16, 'max_width': 128,
                                'max_number_of_images': 16}])
        if self.path == '/v2/inferences/credits':
            return self.reply({'credits': 0, 'balance': s['balance']})
        if self.path.startswith('/v2/inferences/tasks/'):
            t = s['tasks'][self.path.rsplit('/', 1)[1]]
            t['polls'] += 1
            if t['polls'] < 2:
                return self.reply({'status': 'running'})
            b = t['body']
            if b.get('return_spritesheet'):
                imgs = [checker(b['width'] * 2, b['height'] * 2)]  # 2x2 grid of frames
            else:
                imgs = [checker(b['width'], b['height']) for _ in range(b['num_images'])]
            return self.reply({'status': 'succeeded', 'result': {
                'base64_images': [base64.b64encode(i).decode() for i in imgs], 'balance_cost': 0.06 * b['num_images']}})
        if self.path.startswith('/v2/edit/tasks/'):
            t = s['tasks'][self.path.rsplit('/', 1)[1]]
            t['polls'] += 1
            if t['polls'] < 2:
                return self.reply({'status': 'running'})
            return self.reply({'status': 'succeeded', 'task_id': self.path.rsplit('/', 1)[1], 'result': {
                'tool_id': t['tool'], 'balance_cost': 0.001, 'base64_images': [],
                'output_urls': [self.base() + '/files/fixed.png']}})
        if self.path.startswith('/v2/lowpoly/tasks/'):
            t = s['tasks'][self.path.rsplit('/', 1)[1]]
            t['polls'] += 1
            if t['polls'] < 2:
                return self.reply({'status': 'running'})
            if t.get('animate'):
                return self.reply({'status': 'succeeded', 'animation': 'rd_named_anim', 'result': {}})
            return self.reply({'status': 'succeeded', 'result': {'asset_id': t['asset_id'], 'versions': [
                {'number': 1, 'render_url': self.base() + '/files/render.png',
                 'turntable_url': self.base() + '/files/turn.png'}]}})
        self.reply({'error': {'code': 'not_found'}}, 404)

    def base(self):
        return 'http://127.0.0.1:%d' % self.server.server_port

    def do_PATCH(self):
        b = self.body()
        Fake.state['calls'].append(('PATCH', self.path, b, None))
        if len(b.get('reference_images', [])) > Fake.state.get('max_refs', 4):
            return self.reply({'error': {'code': 'invalid_request', 'message': 'exactly 1 reference image'}}, 400)
        return self.reply({'id': self.path.rsplit('/', 1)[1], 'prompt_style': 'user__void_reclaimer_1', 'type': 'user'})

    def do_POST(self):
        s = Fake.state
        b = self.body()
        s['calls'].append(('POST', self.path, b, self.headers.get('Idempotency-Key')))
        if self.path == '/v2/inferences':
            if b.get('check_cost'):
                return self.reply({'balance_cost': 0.06 * b['num_images'], 'model': 'check_cost', 'base64_images': []})
            key = self.headers.get('Idempotency-Key')
            if key in s['idem']:
                return self.reply({'status': 'accepted', 'task_id': s['idem'][key]})
            tid = 't%d' % len(s['tasks'])
            s['tasks'][tid] = {'body': b, 'polls': 0}
            s['idem'][key] = tid
            s['charged'] += 1
            return self.reply({'status': 'accepted', 'task_id': tid})
        if self.path == '/v2/styles':
            s['styles'] = s.get('styles', 0) + 1
            return self.reply({'id': 'st%d' % s['styles'], 'prompt_style': 'user__void_reclaimer_%d' % s['styles'],
                               'name': b['name'], 'type': 'user'}, 201)
        if self.path.startswith('/v2/edit/tools/') and self.path.endswith('/estimate'):
            return self.reply({'tool_id': self.path.split('/')[4], 'balance_cost': 0.001, 'credit_cost': 1})
        if self.path.startswith('/v2/edit/tools/') and self.path.endswith('/tasks'):
            cid = b.get('custom_id')
            if cid in s['idem']:
                return self.reply({'status': 'accepted', 'task_id': s['idem'][cid]}, 202)
            tid = 't%d' % len(s['tasks'])
            s['tasks'][tid] = {'body': b, 'polls': 0, 'tool': self.path.split('/')[4]}
            s['idem'][cid] = tid
            s['charged'] += 1
            return self.reply({'status': 'accepted', 'task_id': tid}, 202)
        if self.path.startswith('/v2/pixel-fixer/'):
            s['charged'] += 1
            return self.reply({'base64_images': [base64.b64encode(checker(16, 16, transparent_corner=False)).decode()]})
        if self.path == '/v2/lowpoly/estimate':
            return self.reply({'operation': 'generate', 'size': 32, 'cost': 0.5})
        if self.path == '/v2/lowpoly/generate':
            tid = 't%d' % len(s['tasks'])
            s['tasks'][tid] = {'body': b, 'polls': 0, 'asset_id': 'a' + tid}
            s['charged'] += 1
            return self.reply({'task_id': tid, 'asset_id': 'a' + tid, 'cost': 0.5}, 202)
        if self.path.endswith('/revise'):
            key = self.headers.get('Idempotency-Key')
            if key in s['idem']:
                return self.reply({'task_id': s['idem'][key]}, 202)
            tid = 't%d' % len(s['tasks'])
            s['tasks'][tid] = {'body': b, 'polls': 0, 'asset_id': self.path.split('/')[4]}
            s['idem'][key] = tid
            s['charged'] += 1
            return self.reply({'task_id': tid, 'asset_id': self.path.split('/')[4]}, 202)
        if self.path.endswith('/animate'):
            tid = 't%d' % len(s['tasks'])
            s['tasks'][tid] = {'body': b, 'polls': 0, 'animate': True}
            s['charged'] += 1
            return self.reply({'task_id': tid}, 202)
        if self.path.endswith('/export'):
            name = {'blockbench': 'model.bbmodel', 'atlas': 'atlas.png'}.get(b['target'], 'rd-%s.zip' % b['target'])
            return self.reply({'url': self.base() + '/files/' + name, 'filename': name})
        self.reply({'error': {'code': 'not_found'}}, 404)


class RdGenerateTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.httpd = http.server.ThreadingHTTPServer(('127.0.0.1', 0), Fake)
        threading.Thread(target=cls.httpd.serve_forever, daemon=True).start()
        rd.API = 'http://127.0.0.1:%d/v2' % cls.httpd.server_port

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown()
        cls.httpd.server_close()

    def setUp(self):
        Fake.state = {'calls': [], 'tasks': {}, 'idem': {}, 'charged': 0, 'balance': 100.0}
        self.tmp = tempfile.mkdtemp()
        os.makedirs(os.path.join(self.tmp, 'docs', 'art'))
        shutil.copy(os.path.join(rd.REPO, 'docs', 'art', 'assets.csv'), os.path.join(self.tmp, 'docs', 'art'))
        icon = os.path.join(self.tmp, rd.TEXTURES, 'item')
        os.makedirs(icon)
        shutil.copy(os.path.join(rd.REPO, rd.TEXTURES, 'item', 'drone_hull.png'), icon)
        self.patch = mock.patch.multiple(rd, REPO=self.tmp, CSV_PATH=os.path.join(self.tmp, 'docs', 'art', 'assets.csv'),
                                         STAGING=os.path.join(self.tmp, 'art', 'rd'),
                                         STATE_PATH=os.path.join(self.tmp, 'art', 'rd', 'state.json'))
        self.patch.start()
        os.environ['RD_API_KEY'] = 'rdpk-test'

    def tearDown(self):
        self.patch.stop()
        shutil.rmtree(self.tmp)

    @staticmethod
    def read(path):
        with open(path, 'rb') as f:
            return f.read()

    def staged(self, *p):
        return os.path.join(self.tmp, 'art', 'rd', *p)

    def test_png_round_trip_and_downscale(self):
        px = rd.png_read(checker(32, 32))
        self.assertEqual((len(px[0]), len(px)), (32, 32))
        small = rd.downscale(px, 16, 16)
        self.assertEqual((len(small[0]), len(small)), (16, 16))
        self.assertEqual(small[0][0][3], 0, 'transparent corner stays transparent')
        self.assertEqual(rd.png_read(rd.png_write(small)), small)
        self.assertEqual(rd.fit('rd_plus__mc_item', 9, 9, rd.STYLE_LIMITS)[:2], (18, 18), 'below the minimum: 2x')
        self.assertEqual(rd.fit('rd_pro__simple', 90, 9, rd.STYLE_LIMITS)[:2], (180, 18))

    def test_prompts_lose_the_ui_prefix(self):
        rows = {r['asset']: r for r in rd.load_rows()}
        self.assertTrue(rd.subject(rows['quartz_wafer']['prompt']).startswith('thin round translucent'))
        self.assertTrue(all(len(rd.subject(r['prompt'])) <= 250 for r in rows.values()))

    def test_images_priced_asked_generated_downscaled_and_promoted(self):
        current = os.path.join(self.tmp, rd.TEXTURES, 'block', 'asteroid_ice.png')
        os.makedirs(os.path.dirname(current))
        shutil.copy(os.path.join(REAL_REPO, rd.TEXTURES, 'block', 'asteroid_ice.png'), current)
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'quartz_wafer', '--asset', 'asteroid_ice'])
        posts = [c for c in Fake.state['calls'] if c[0] == 'POST' and c[1] == '/v2/inferences']
        self.assertEqual(sum(1 for c in posts if c[2].get('check_cost')), 2, 'free estimate per job first')
        real = [c[2] for c in posts if not c[2].get('check_cost')]
        ice = next(b for b in real if b['prompt_style'] == 'rd_plus__mc_texture')
        self.assertTrue(ice['tile_x'] and ice['tile_y'], 'asteroid rock tiles')
        wafer = next(b for b in real if b['prompt_style'] == 'rd_plus__mc_item')
        self.assertTrue(wafer['remove_bg'])
        self.assertEqual((wafer['width'], wafer['num_images']), (16, 4))
        self.assertNotIn('Minecraft item icon', wafer['prompt'])
        for n in range(1, 5):
            px = rd.png_read(self.read(self.staged('quartz_wafer', 'v%d.png' % n)))
            self.assertEqual((len(px[0]), len(px)), (16, 16))
        page = self.read(self.staged('index.html')).decode()
        sources = re.findall(r'src="([^"]+)"', page)
        self.assertEqual(len(sources), 8 + 1, 'four variants each and the current asteroid_ice texture')
        for src in sources:
            self.assertTrue(src.startswith('data:image/png;base64,'), 'the contact sheet embeds its images: ' + src[:40])
        embedded = [base64.b64decode(s.split(',', 1)[1]) for s in sources]
        self.assertIn(self.read(self.staged('quartz_wafer', 'v2.png')), embedded)
        self.assertIn(self.read(current), embedded, 'the current texture is shown next to the variants')
        rd.main(['--promote', 'quartz_wafer=3'])
        self.assertTrue(os.path.exists(os.path.join(self.tmp, rd.TEXTURES, 'item', 'quartz_wafer.png')))
        listed = os.path.join(self.tmp, 'docs', 'art', 'promoted.txt')
        self.assertEqual(self.read(listed).decode().split(), ['item/quartz_wafer.png'], 'a promotion is recorded for the asset list')
        rd.record_promoted(rd.TEXTURES + 'block/asteroid_ice.png')
        rd.record_promoted(rd.TEXTURES + 'item/quartz_wafer.png')
        self.assertEqual(self.read(listed).decode().split(), ['block/asteroid_ice.png', 'item/quartz_wafer.png'],
                         'sorted, and recording twice adds nothing')

    def put_texture(self, rel):
        dst = os.path.join(self.tmp, rd.TEXTURES, rel)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy(os.path.join(REAL_REPO, rd.TEXTURES, rel), dst)
        return dst

    def test_fix_dry_run_prices_offline_and_sends_nothing(self):
        self.put_texture('block/asteroid_silicate.png')
        del os.environ['RD_API_KEY']
        rd.main(['--fix', 'asteroid_silicate', '--tool', 'palette', '--dry-run'])
        self.assertEqual(Fake.state['calls'], [], 'a dry run sends nothing')
        self.assertFalse(os.path.exists(self.staged('asteroid_silicate')))

    def test_fix_palette_uses_the_style_palette_becomes_a_variant_and_never_pays_twice(self):
        self.put_texture('block/asteroid_silicate.png')
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'palette'])
        estimate = [c for c in Fake.state['calls'] if c[0] == 'POST' and c[1].endswith('/palette_converter/estimate')]
        self.assertEqual(len(estimate), 1, 'a free estimate first')
        task = next(c[2] for c in Fake.state['calls'] if c[0] == 'POST' and c[1] == '/v2/edit/tools/palette_converter/tasks')
        rows = {r['asset']: r for r in rd.load_rows()}
        self.assertEqual(task['input_palette'], rd.palette_b64(rd.category_for(rows['asteroid_silicate'], 'image')),
                         "the asset's own style-guide palette")
        self.assertTrue(task['custom_id'], 'tasks carry a custom_id, so a retry is not charged again')
        self.assertEqual(self.read(self.staged('asteroid_silicate', 'v1.png')), checker(64, 64), 'the result is variant 1')
        self.assertEqual(Fake.state['charged'], 1)
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'palette'])
        self.assertEqual(Fake.state['charged'], 1, 'the same fix again is not paid again')
        self.assertFalse(os.path.exists(self.staged('asteroid_silicate', 'v2.png')))
        self.assertIn('asteroid_silicate', self.read(self.staged('index.html')).decode(), 'the fix shows on the contact sheet')

    def test_fix_asks_first_and_a_no_sends_nothing_paid(self):
        self.put_texture('block/asteroid_silicate.png')
        with mock.patch('builtins.input', return_value='n'):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'reduce', '--colors', '8'])
        self.assertEqual(Fake.state['charged'], 0)
        self.assertFalse(any(c[1].endswith('/tasks') for c in Fake.state['calls'] if c[0] == 'POST'))

    def test_pixel_fixer_on_a_staged_variant_and_remembered(self):
        folder = self.staged('quartz_wafer')
        os.makedirs(folder)
        with open(os.path.join(folder, 'v1.png'), 'wb') as f:
            f.write(checker(16, 16))
        rd.main(['--fix', 'quartz_wafer=1', '--tool', 'pixel', '--yes'])
        call = next(c for c in Fake.state['calls'] if c[0] == 'POST' and c[1] == '/v2/pixel-fixer/standard')
        self.assertEqual(set(call[2]), {'input_image'}, 'the Pixel Fixer rejects unknown fields')
        self.assertIsNone(call[3], 'no Idempotency-Key on Pixel Fixer requests')
        self.assertTrue(os.path.exists(os.path.join(folder, 'v2.png')), 'the fix becomes the next variant')
        rd.main(['--fix', 'quartz_wafer=1', '--tool', 'pixel', '--yes'])
        self.assertEqual(Fake.state['charged'], 1, 'a done Pixel Fixer run is remembered, not repeated')

    def test_fix_downscale_aims_at_the_target_size(self):
        self.put_texture('block/asteroid_silicate.png')
        rd.main(['--fix', 'asteroid_silicate', '--tool', 'downscale', '--yes'])
        task = next(c[2] for c in Fake.state['calls'] if c[0] == 'POST' and c[1].endswith('/k_centroid_downscale/tasks'))
        rows = {r['asset']: r for r in rd.load_rows()}
        self.assertEqual((task['width'], task['height']), tuple(rd.target_size(rows['asteroid_silicate'])))

    def mask_file(self, w, h, opaque_box):
        x0, y0, x1, y1 = opaque_box
        px = [[(255, 0, 0, 255 if x0 <= x < x1 and y0 <= y < y1 else 0) for x in range(w)] for y in range(h)]
        path = os.path.join(self.tmp, 'mask.png')
        with open(path, 'wb') as f:
            f.write(rd.png_write(px))
        return path

    def tool_call(self, tool_id):
        return next(c[2] for c in Fake.state['calls'] if c[0] == 'POST' and c[1] == '/v2/edit/tools/%s/tasks' % tool_id)

    def test_edit_needs_a_prompt_and_sends_it(self):
        self.put_texture('block/asteroid_silicate.png')
        with self.assertRaises(SystemExit):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'edit', '--yes'])
        self.assertEqual(Fake.state['charged'], 0)
        rd.main(['--fix', 'asteroid_silicate', '--tool', 'edit', '--prompt', 'sand-tan rock', '--seed', '7', '--yes'])
        call = self.tool_call('image_edit')
        self.assertEqual((call['prompt'], call['seed']), ('sand-tan rock', 7))
        self.assertTrue(os.path.exists(self.staged('asteroid_silicate', 'v1.png')), 'the URL result is downloaded as a variant')

    def test_inpaint_checks_the_mask_and_sends_it(self):
        self.put_texture('block/asteroid_silicate.png')
        size = rd.png_read(self.read(os.path.join(self.tmp, rd.TEXTURES, 'block', 'asteroid_silicate.png')))
        w, h = len(size[0]), len(size)
        with self.assertRaises(SystemExit):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'inpaint', '--prompt', 'x', '--yes'])  # no mask
        with self.assertRaises(SystemExit):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'inpaint', '--prompt', 'x', '--yes',
                     '--mask', self.mask_file(w + 1, h, (0, 0, 2, 2))])  # wrong size
        with self.assertRaises(SystemExit):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'inpaint', '--prompt', 'x', '--yes',
                     '--mask', self.mask_file(w, h, (0, 0, w, h))])  # opaque everywhere: would repaint it all
        self.assertEqual(Fake.state['charged'], 0, 'nothing paid for a bad mask')
        mask = self.mask_file(w, h, (0, 0, w // 2, h // 2))
        rd.main(['--fix', 'asteroid_silicate', '--tool', 'inpaint', '--prompt', 'rough flakes', '--mask', mask, '--yes'])
        call = self.tool_call('inpainting')
        self.assertEqual(base64.b64decode(call['mask_image']), self.read(mask))
        self.assertEqual(call['prompt'], 'rough flakes')

    def test_outpaint_and_seam_send_their_fields(self):
        self.put_texture('block/asteroid_silicate.png')
        with self.assertRaises(SystemExit):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'outpaint', '--expand', '0,0,0,0', '--yes'])
        rd.main(['--fix', 'asteroid_silicate', '--tool', 'outpaint', '--expand', '0,4,8,0', '--yes'])
        call = self.tool_call('outpainting')
        self.assertEqual((call['expand_left'], call['expand_right'], call['expand_top'], call['expand_bottom']), (0, 4, 8, 0))
        rd.main(['--fix', 'asteroid_silicate', '--tool', 'seam', '--axes', 'x', '--yes'])
        call = self.tool_call('seam_tiling')
        self.assertEqual((call['tile_x'], call['tile_y']), (True, False))
        with self.assertRaises(SystemExit):
            rd.main(['--fix', 'asteroid_silicate', '--tool', 'seam', '--axes', 'z', '--yes'])

    def real_inference(self):
        return [c for c in Fake.state['calls'] if c[0] == 'POST' and c[1] == '/v2/inferences' and not c[2].get('check_cost')]

    def test_restyle_starts_from_the_current_texture(self):
        current = self.put_texture('block/ashen_slag.png')
        rd.main(['--asset', 'ashen_slag', '--restyle', '--strength', '0.5', '--yes'])
        body = self.real_inference()[0][2]
        self.assertEqual(body['strength'], 0.5)
        sent = rd.png_read(base64.b64decode(body['input_image']))
        self.assertEqual((len(sent[0]), len(sent)), (body['width'], body['height']), 'the texture is scaled to the job size')
        k = body['width'] // 16
        self.assertEqual(sent, rd.upscale(rd.png_read(self.read(current)), k), 'nearest-neighbour upscale of our texture')
        rd.main(['--asset', 'ashen_slag', '--restyle', '--strength', '0.5', '--yes'])
        self.assertEqual(Fake.state['charged'], 1, 'the same restyle again is not paid again')

    def test_restyle_runs_even_after_a_normal_generation(self):
        self.put_texture('block/ashen_slag.png')
        rd.main(['--asset', 'ashen_slag', '--yes'])
        rd.main(['--asset', 'ashen_slag', '--restyle', '--yes'])
        self.assertEqual(len(self.real_inference()), 2, 'a restyle is new work, not "already generated"')
        self.assertNotIn('input_image', self.real_inference()[0][2])
        self.assertIn('input_image', self.real_inference()[1][2])

    def test_tile_variation_for_blocks(self):
        self.put_texture('block/ashen_slag.png')
        rd.main(['--asset', 'ashen_slag', '--restyle', '--tile-variation', '--yes'])
        body = self.real_inference()[0][2]
        self.assertEqual(body['prompt_style'], 'rd_tile__tile_variation')
        self.assertNotIn('strength', body)
        self.assertNotIn('tile_x', body, 'the tile style does its own tiling')

    def test_restyle_skips_an_asset_with_no_texture(self):
        rd.main(['--asset', 'quartz_wafer', '--restyle', '--yes'])
        self.assertEqual(self.real_inference(), [], 'nothing to start from, nothing sent')

    def generated_model(self):
        rd.main(['--asset', 'core_mk2_model', '--model-style', 'blocky_lite', '--yes'])
        return Fake.state['charged']

    def test_revise_needs_a_model_and_a_prompt(self):
        with self.assertRaises(SystemExit):
            rd.main(['--model-revise', 'core_mk2_model', '--prompt', 'fins', '--yes'])  # no model yet
        self.generated_model()
        with self.assertRaises(SystemExit):
            rd.main(['--model-revise', 'core_mk2_model', '--yes'])  # no prompt
        self.assertFalse(any(c[1].endswith('/revise') for c in Fake.state['calls'] if c[0] == 'POST'))

    def test_revise_asks_makes_a_new_version_and_never_pays_twice(self):
        before = self.generated_model()
        with mock.patch('builtins.input', return_value='n'):
            rd.main(['--model-revise', 'core_mk2_model', '--prompt', 'add cooling fins'])
        self.assertEqual(Fake.state['charged'], before, 'a no sends nothing')
        rd.main(['--model-revise', 'core_mk2_model', '--prompt', 'add cooling fins', '--version', '1', '--yes'])
        call = next(c for c in Fake.state['calls'] if c[0] == 'POST' and c[1].endswith('/revise'))
        self.assertEqual(call[2], {'prompt': 'add cooling fins', 'version': 1})
        self.assertTrue(call[3], 'a revision carries an Idempotency-Key')
        state = rd.load_state()
        models = [j for j in state['assets']['core_mk2_model']['jobs'] if j['kind'] == 'model']
        self.assertEqual(models[-1].get('revision'), 'add cooling fins')
        self.assertEqual(models[-1]['status'], 'done', 'the revision was polled and saved like a model')
        self.assertTrue(os.path.exists(self.staged('core_mk2_model', 'model.bbmodel')))
        self.assertEqual(Fake.state['charged'], before + 1)
        rd.main(['--model-revise', 'core_mk2_model', '--prompt', 'add cooling fins', '--version', '1', '--yes'])
        self.assertEqual(Fake.state['charged'], before + 1, 'the same revision again is not paid again')

    def test_export_is_free_and_lands_in_the_export_folder(self):
        before = self.generated_model()
        rd.main(['--model-export', 'core_mk2_model', '--target', 'minecraft', '--target', 'obj'])
        exports = [c[2]['target'] for c in Fake.state['calls'] if c[0] == 'POST' and c[1].endswith('/export')]
        self.assertIn('minecraft', exports)
        self.assertIn('obj', exports)
        self.assertTrue(os.path.exists(self.staged('core_mk2_model', 'export', 'rd-minecraft.zip')))
        self.assertTrue(os.path.exists(self.staged('core_mk2_model', 'export', 'rd-obj.zip')))
        self.assertEqual(Fake.state['charged'], before, 'exports are free')
        with self.assertRaises(SystemExit):
            rd.main(['--model-export', 'core_mk2_model', '--target', 'fbx'])

    def test_contact_sheet_embeds_renders_by_their_real_type(self):
        folder = self.staged('box_model')
        os.makedirs(folder)
        gif, mp4 = b'GIF89a' + b'\0' * 20, b'\0\0\0\x18ftypmp42' + b'\0' * 20
        for name, data in (('render.png', gif), ('turntable.png', mp4)):
            with open(os.path.join(folder, name), 'wb') as f:
                f.write(data)
        rd.write_index({'assets': {'box_model': {'group': 'Models', 'jobs': []}}})
        page = self.read(self.staged('index.html')).decode()
        self.assertIn('<img src="data:image/gif;base64,%s"' % base64.b64encode(gif).decode(), page)
        self.assertIn('<video src="data:video/mp4;base64,%s"' % base64.b64encode(mp4).decode(), page,
                      'a turntable that is really a video still plays')
        self.assertNotIn('src="box_model/', page, 'no links to files next to the page')

    def test_contact_sheet_is_a_picker(self):
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'quartz_wafer', '--asset', 'core_mk2_model', '--model-style', 'blocky_lite'])
        page = self.read(self.staged('index.html')).decode()
        wafer = page[page.index('data-key="quartz_wafer"'):page.index('</section>', page.index('data-key="quartz_wafer"'))]
        self.assertEqual(re.findall(r'data-pick="(\w+)"', wafer), ['1', '2', '3', '4', 'keep', 'redo'],
                         'one button per variant, then keep and redo')
        core = page[page.index('data-key="core_mk2_model"'):]
        self.assertIn('data-pick="model"', core[:core.index('</section>')], 'a model asset can be promoted whole')
        self.assertIn('function buildCommands', page, 'the picker script is inlined')
        self.assertNotIn('<script src', page)
        self.assertNotIn('<link', page, 'nothing loads from outside the page')
        rd.main(['--promote', 'quartz_wafer=2'])
        self.assertEqual(rd.load_state()['assets']['quartz_wafer']['promoted'], 'v2')
        self.assertIn('data-key="quartz_wafer" data-promoted="v2"', self.read(self.staged('index.html')).decode(),
                      'the rebuilt page shows the promotion as done')

    @unittest.skipUnless(shutil.which('node'), 'needs Node to run the page script')
    def test_picker_builds_the_commands(self):
        import subprocess
        picks = {'b_icon': {'pick': '3'}, 'a_icon': {'pick': '1', 'note': 'nice'}, 'core_mk2_model': {'pick': 'model'},
                 'ice': {'pick': 'redo'}, 'rock': {'pick': 'redo'}, 'kept': {'pick': 'keep'}, 'empty': {'pick': ''}}
        script = ("const {buildCommands} = require(%s); process.stdout.write(JSON.stringify(buildCommands(%s)))"
                  % (json.dumps(os.path.join(REAL_REPO, 'tools', 'art', 'picker.js')), json.dumps(picks)))
        out = json.loads(subprocess.run(['node', '-e', script], capture_output=True, text=True, check=True).stdout)
        self.assertEqual(out, ['python3 tools/art/rd_generate.py --promote a_icon=1 --promote b_icon=3 --promote core_mk2_model',
                               'python3 tools/art/rd_generate.py --again --asset ice --asset rock'])
        empty = subprocess.run(['node', '-e', script.replace(json.dumps(picks), '{}')], capture_output=True, text=True)
        self.assertEqual(json.loads(empty.stdout), [], 'no picks, no commands')

    def test_no_spend_when_declined_or_over_balance(self):
        with mock.patch('builtins.input', return_value='n'):
            rd.main(['--asset', 'quartz_wafer'])
        Fake.state['balance'] = 0.14
        with mock.patch('builtins.input', return_value='y'):
            self.assertEqual(rd.main(['--asset', 'asteroid_*']), 1)
        self.assertEqual(Fake.state['charged'], 0)

    def test_rerun_skips_done_assets_and_resubmits_use_the_same_key(self):
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'quartz_wafer'])
            rd.main(['--asset', 'quartz_wafer'])
        self.assertEqual(Fake.state['charged'], 1, 'a done asset is not generated again')
        state = rd.load_state()
        state['assets']['quartz_wafer']['jobs'][0]['status'] = 'failed'  # pretend the client crashed mid-way
        rd.save_state(state)
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'quartz_wafer'])
        self.assertEqual(Fake.state['charged'], 1, 'same Idempotency-Key: RD returns the old job, no new charge')
        self.assertEqual(len(rd.load_state()['assets']['quartz_wafer']['jobs']), 1, 'the old job record is reused')
        self.assertFalse(os.path.exists(self.staged('quartz_wafer', 'v5.png')), 'no duplicate variants')

    def test_animated_icon_becomes_a_vertical_strip(self):
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'nanite_swarm', '--method', 'image'])
        body = next(c[2] for c in Fake.state['calls'] if c[0] == 'POST' and c[1] == '/v2/inferences'
                    and not c[2].get('check_cost'))
        self.assertEqual(body['prompt_style'], 'rd_animation__vfx')
        self.assertTrue(body['return_spritesheet'])
        px = rd.png_read(self.read(self.staged('nanite_swarm', 'v1.png')))
        self.assertEqual((len(px[0]), len(px)), (16, 64), '4 frames stacked like the .mcmeta expects')

    def test_models_and_animations(self):
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'drone_hull (section)', '--model-style', 'blocky_lite'])
        gen = next(c for c in Fake.state['calls'] if c[1] == '/v2/lowpoly/generate')
        self.assertEqual(gen[2]['style'], 'rd_lowpoly__blocky_lite')
        self.assertTrue(gen[2]['reference_images'][0].startswith('data:image/png;base64,'), 'icon sent as reference')
        self.assertTrue(gen[3], 'paid call carries an Idempotency-Key')
        for f in ('model.bbmodel', 'atlas.png', 'render.png', 'turntable.png'):
            self.assertTrue(os.path.exists(self.staged('drone_hull_section', f)), f)
        # An animation without its rig is skipped, not charged.
        charged = Fake.state['charged']
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'animation.void_reclaimer.mech.idle', '--model-style', 'blocky'])
        self.assertEqual(Fake.state['charged'], charged)
        # With the rig generated, it animates and re-exports the rig's .bbmodel.
        with mock.patch('builtins.input', return_value='b'):
            rd.main(['--asset', 'drone (body)', '--asset', 'animation.void_reclaimer.drone.idle'])
        self.assertTrue(any(c[1].endswith('/animate') for c in Fake.state['calls']))
        self.assertTrue(os.path.exists(self.staged('drone_body', 'model.bbmodel')))
        self.assertTrue(os.path.exists(self.staged('drone_body', 'rerig.txt')),
                        'a downloaded body-rig model is checked against its rig spec')
        self.assertFalse(os.path.exists(self.staged('drone_hull_section', 'rerig.txt')), 'sections are not rigs')

    def test_model_style_blocky_or_lite(self):
        # Without --model-style a model batch prices both styles and asks; picking Lite is the go-ahead.
        with mock.patch('builtins.input', return_value='l') as ask:
            rd.main(['--asset', 'core_mk2_model'])
        self.assertEqual(ask.call_count, 1, 'one question: the style choice')
        self.assertIn('blocky [l]ite', ask.call_args[0][0])
        gen = [c for c in Fake.state['calls'] if c[1] == '/v2/lowpoly/generate']
        self.assertEqual([c[2]['style'] for c in gen], ['rd_lowpoly__blocky_lite'])
        estimates = [c[2]['style'] for c in Fake.state['calls'] if c[1] == '/v2/lowpoly/estimate']
        self.assertEqual(sorted(set(estimates)), ['rd_lowpoly__blocky', 'rd_lowpoly__blocky_lite'], 'both priced')
        # Declining sends nothing; --yes without a style refuses rather than guessing.
        charged = Fake.state['charged']
        with mock.patch('builtins.input', return_value='n') as decline:
            rd.main(['--asset', 'core_mk2_model', '--again'])  # --again: it was generated above, so plan it afresh
        self.assertEqual(decline.call_count, 1, 'the style question was asked')
        with self.assertRaises(SystemExit):
            rd.main(['--asset', 'core_mk2_model', '--again', '--yes'])
        self.assertEqual(Fake.state['charged'], charged)
        # Blocky and Blocky Lite are the only choices (Detailed builds cylinders and spheres).
        with self.assertRaises(SystemExit), mock.patch('sys.stderr'):
            rd.main(['--asset', 'core_mk2_model', '--model-style', 'detailed'])

    def stage_model(self, key, text):
        os.makedirs(self.staged(key), exist_ok=True)
        with open(self.staged(key, 'model.bbmodel'), 'w') as f:
            f.write(text)

    def geo(self, rel):
        with open(os.path.join(self.tmp, 'src/main/resources/assets/void_reclaimer/geo', rel)) as f:
            return json.load(f)['minecraft:geometry'][0]

    def test_promote_new_module_model(self):
        import bbgeo
        self.stage_model('core_mk2_model', bbmodel())
        rd.main(['--promote', 'core_mk2_model'])
        geo = self.geo('module/core_mk2.geo.json')
        bones = {b['name']: b for b in geo['bones']}
        self.assertEqual(list(bones)[:2], ['core_mk2', 'core_mk2_mount'], 'module skeleton')
        self.assertEqual(bones['core_mk2_body']['parent'], 'core_mk2_mount')
        self.assertEqual(bones['core_mk2_blade']['parent'], 'core_mk2_body')
        self.assertEqual(len(bones['core_mk2_mount']['cubes']), 1, 'the loose box sits on the mount')
        lo, hi = bbgeo.cubes_bbox(geo['bones'])
        self.assertAlmostEqual(max(h - l for l, h in zip(lo, hi)), 4.0, 3, 'fitted to 4 px')
        self.assertAlmostEqual(lo[1], 0.0, 3, 'stands on the mount')
        self.assertEqual((geo['description']['texture_width'], geo['description']['texture_height']), (32, 32))
        tex = os.path.join(self.tmp, rd.TEXTURES, 'entity/body')
        self.assertEqual(rd.png_read(self.read(os.path.join(tex, 'core_mk2.png'))), rd.png_read(checker(32, 32, False)))
        accent = rd.png_read(self.read(os.path.join(tex, 'core_mk2_accent.png')))
        self.assertTrue(all(px[3] == 0 for row in accent for px in row), 'empty accent mask to paint')

    def test_bedrock_coordinates_match_blockbench(self):
        import bbgeo
        geo, _, _ = bbgeo.convert(bbmodel(), 'p', scale=False)
        bones = {b['name']: b for b in geo['minecraft:geometry'][0]['bones']}
        loose = bones['p_mount']['cubes'][0]
        self.assertEqual((loose['origin'], loose['size']), ([-6, 0, 0], [4, 2, 2]), 'X mirrored: origin x = -to.x')
        self.assertEqual(loose['uv']['north'], {'uv': [0, 0], 'uv_size': [8, 8]})
        self.assertEqual(loose['uv']['up'], {'uv': [8, 8], 'uv_size': [-8, -8]}, 'up/down flipped like Blockbench')
        fin = bones['p_blade']['cubes'][0]
        self.assertEqual((fin['pivot'], fin['rotation']), ([0, 8, 0], [0, 0, 22.5]))
        self.assertEqual(bones['p_blade']['rotation'], [0, -45, 0], 'rotation around Y negated')
        # Blockbench 5 keeps groups in a separate list; the result is the same.
        self.assertEqual(bbgeo.convert(bbmodel(groups_v5=True), 'p', scale=False)[0], geo)

    def test_promote_section_keeps_its_skeleton(self):
        import bbgeo
        rel = 'src/main/resources/assets/void_reclaimer/geo/section/rotor_arm.geo.json'
        os.makedirs(os.path.dirname(os.path.join(self.tmp, rel)))
        shutil.copy(os.path.join(REAL_REPO, rel), os.path.join(self.tmp, rel))
        old = self.geo('section/rotor_arm.geo.json')
        self.stage_model('rotor_arm_section', bbmodel())
        with self.assertRaises(SystemExit), mock.patch('sys.stdout'):
            rd.main(['--promote', 'rotor_arm_section', '--bone', 'blade=no_such_bone'])
        rd.main(['--promote', 'rotor_arm_section', '--bone', 'blade=rotor_spin'])
        new = self.geo('section/rotor_arm.geo.json')
        nb = {b['name']: b for b in new['bones']}
        for b in old['bones']:
            self.assertEqual((nb[b['name']].get('parent'), nb[b['name']]['pivot']), (b.get('parent'), b['pivot']),
                             'contract bone %s unchanged' % b['name'])
        self.assertEqual(nb['rotor_arm_blade']['parent'], 'rotor_spin', 'mapped group turns with rotor_spin')
        self.assertEqual(nb['rotor_arm_body']['parent'], 'rotor_arm_frame')
        self.assertFalse(nb['cap_thruster'].get('cubes') if 'cap_thruster' in nb else False)
        olo, ohi = bbgeo.cubes_bbox(old['bones'])
        nlo, nhi = bbgeo.cubes_bbox(new['bones'])
        self.assertAlmostEqual(max(h - l for l, h in zip(nlo, nhi)), max(h - l for l, h in zip(olo, ohi)), 3)
        self.assertEqual(new['description']['identifier'], old['description']['identifier'])
        self.assertTrue(os.path.exists(self.staged('rotor_arm_section', 'replaced', 'rotor_arm.geo.json')), 'backup')

    def test_baked_faces_match_blockbench_rotation(self):
        import bbgeo
        # A texture where every pixel is different, and faces using it rotated and mirrored every way.
        tex = [[(x * 16, y * 16, (x + y) * 8, 255) for x in range(16)] for y in range(16)]
        uvs = [(2, 3, 8, 7), (8, 7, 2, 3), (8, 3, 2, 7), (2, 7, 8, 3)]
        elements, cases = {}, []
        for i, uv in enumerate(uvs):
            for r in (90, 180, 270):
                f = {'uv': list(uv), 'texture': 0, 'rotation': r}
                elements['e%d_%d' % (i, r)] = {'faces': {'north': f}}
                cases.append((uv, r, f))
        png, new_h, n = bbgeo.bake_rotated_faces(elements, rd.png_write(tex), 16, 16)
        self.assertEqual(n, 12)
        baked = rd.png_read(png)

        def blockbench(uv, r, s, t):
            # Literal port of Blockbench's Cube.updateUV: corner UVs TL, TR, BL, BR, then per 90 degrees
            # arr0 = arr2, arr2 = arr3, arr3 = arr1, arr1 = old arr0; the face shows them bilinearly.
            u1, v1, u2, v2 = uv
            arr = [(u1, v1), (u2, v1), (u1, v2), (u2, v2)]
            for _ in range(r // 90):
                a = arr[0]
                arr[0], arr[2], arr[3], arr[1] = arr[2], arr[3], arr[1], a
            u = arr[0][0] * (1 - s) * (1 - t) + arr[1][0] * s * (1 - t) + arr[2][0] * (1 - s) * t + arr[3][0] * s * t
            v = arr[0][1] * (1 - s) * (1 - t) + arr[1][1] * s * (1 - t) + arr[2][1] * (1 - s) * t + arr[3][1] * s * t
            return u, v

        for uv, r, f in cases:
            self.assertNotIn('rotation', f)
            bu1, bv1, bu2, bv2 = f['uv']
            w, h = int(abs(bu2 - bu1)), int(abs(bv2 - bv1))
            for s, t in [((i + 0.5) / w, (j + 0.5) / h) for i in range(w) for j in range(h)]:  # every pixel's center
                    u, v = blockbench(uv, r, s, t)
                    want = tex[int(v)][int(u)]
                    got = baked[int(bv1 + (bv2 - bv1) * t)][int(bu1 + (bu2 - bu1) * s)]
                    self.assertEqual(got, want, 'uv %s rotation %d at (%.1f, %.1f)' % (uv, r, s, t))

    def anim_model(self):
        model = json.loads(bbmodel(groups_v5=True))
        def key(channel, t, x, y, z, interp='linear'):
            return {'channel': channel, 'time': t, 'interpolation': interp, 'data_points': [{'x': x, 'y': y, 'z': z}]}
        model['animations'] = [
            {'name': 'Spin Up', 'loop': 'loop', 'length': 1.0, 'animators': {
                'g2': {'name': 'blade', 'type': 'bone', 'keyframes': [
                    key('rotation', 0.0, 10, 20, 30), key('rotation', 1.0, 0, 360, 0),
                    key('position', 0.0, 2, 3, 4),
                    key('scale', 0.0, 1, 1, 1)]},          # never moves: dropped
                'g1': {'name': 'body', 'type': 'bone', 'keyframes': [
                    key('scale', 0.0, 0, 0, 0, 'step'), key('scale', 0.5, 1, 1, 1)]},
                'gone': {'name': 'deleted_group', 'type': 'bone', 'keyframes': [key('rotation', 0, 5, 0, 0)]},
                'fx': {'name': 'effects', 'type': 'effect', 'keyframes': []}}},
            {'name': 'once', 'loop': 'once', 'length': 2.0, 'animators': {}},
            {'name': 'held', 'loop': 'hold', 'length': 2.0, 'animators': {}}]
        return json.dumps(model)

    def test_animations_follow_blockbench_bedrock_export(self):
        import bbgeo
        info = {}
        data = self.anim_model()
        bbgeo.convert(data, 'ship', standalone=True, scale=False, scale_factor=2.0, out=info)
        anims, report = bbgeo.convert_animations(data, info['bones_by_uuid'], info['scale'], 'animation.void_reclaimer.ship')
        a = anims['animations']
        spin = a['animation.void_reclaimer.ship.spin_up']
        self.assertEqual(spin['loop'], True)
        self.assertEqual(spin['animation_length'], 1.0)
        blade = spin['bones']['blade']
        self.assertEqual(blade['rotation']['0.0']['vector'], [-10.0, -20.0, 30.0], 'rotation X and Y negated')
        self.assertEqual(blade['rotation']['1']['vector'], [0.0, -360.0, 0.0])
        self.assertEqual(blade['position']['0.0']['vector'], [-4.0, 6.0, 8.0], 'position X negated, scaled with the model')
        self.assertNotIn('scale', blade, 'a channel that never moves is left out')
        body = spin['bones']['body']['scale']
        self.assertEqual(body['0.0']['vector'], [0.0, 0.0, 0.0])
        self.assertEqual(body['0.4999']['vector'], [0.0, 0.0, 0.0], 'a step key holds until just before the next')
        self.assertEqual(body['0.5']['vector'], [1.0, 1.0, 1.0])
        self.assertEqual(set(spin['bones']), {'blade', 'body'}, 'animators for missing groups and effects are skipped')
        self.assertNotIn('loop', a['animation.void_reclaimer.ship.once'])
        self.assertEqual(a['animation.void_reclaimer.ship.held']['loop'], 'hold_on_last_frame')

    def test_cut_one_group_out_of_a_whole_model(self):
        import bbgeo
        geo, _, _ = bbgeo.convert(bbmodel(), 'part', groups=['blade'], scale=False, scale_factor=0.5)
        bones = {b['name']: b for b in geo['minecraft:geometry'][0]['bones']}
        self.assertEqual(set(bones), {'part', 'part_mount', 'part_blade'}, 'only the chosen group and the skeleton')
        self.assertEqual(sum(len(b.get('cubes', [])) for b in bones.values()), 1, 'only the boxes inside it')
        self.assertEqual(bones['part_blade']['cubes'][0]['size'], [1.0, 4.0, 1.0], 'fixed scale 0.5')
        with self.assertRaises(bbgeo.ConvertError):
            bbgeo.convert(bbmodel(), 'part', groups=['wing'])

    def test_promote_as_entity_writes_model_texture_and_animations(self):
        src = os.path.join(self.tmp, 'art', 'ship', 'ship.bbmodel')
        os.makedirs(os.path.dirname(src))
        with open(src, 'w') as f:
            f.write(self.anim_model())
        with mock.patch('sys.stdout'):
            rd.main(['--promote-as', 'entity/test_ship', '--source', src])
        assets = os.path.join(self.tmp, 'src/main/resources/assets/void_reclaimer')
        geo = self.geo('entity/test_ship.geo.json')
        self.assertEqual(geo['bones'][0]['name'], 'test_ship')
        self.assertIn('blade', [b['name'] for b in geo['bones']], "RD's group names are kept")
        with open(os.path.join(assets, 'animations/entity/test_ship.animation.json')) as f:
            anims = json.load(f)
        self.assertIn('animation.void_reclaimer.test_ship.spin_up', anims['animations'])
        self.assertTrue(os.path.exists(os.path.join(assets, 'textures/entity/test_ship.png')))
        self.assertTrue(os.path.exists(os.path.join(self.tmp, 'art', 'ship', 'test_ship_preview.png')))
        for bad in (['--promote-as', 'item/x', '--source', src], ['--promote-as', 'entity/Bad Name', '--source', src],
                    ['--promote-as', 'entity/x']):
            with self.assertRaises(SystemExit):
                rd.main(bad)

    def test_standalone_conversion_reparents_effect_groups_that_lost_their_parent(self):
        """RD's Re-rig can leave flames at the model's top level, where no bone moves them."""
        import bbgeo

        def cube(uuid, at):
            faces = {f: {'uv': [0, 0, 4, 4], 'texture': 0} for f in ('north', 'east', 'south', 'west', 'up', 'down')}
            return {'uuid': uuid, 'name': uuid, 'type': 'cube', 'from': at, 'to': [at[0] + 2, at[1] + 2, at[2] + 2],
                    'origin': at, 'rotation': [0, 0, 0], 'faces': faces}

        def grp(name, origin, children=()):
            return {'name': name, 'uuid': 'g-' + name, 'origin': origin, 'rotation': [0, 0, 0], 'children': list(children)}

        png = base64.b64encode(checker(16, 16, transparent_corner=False)).decode()
        model = {'meta': {'format_version': '4.10', 'model_format': 'free', 'box_uv': False}, 'name': 'rd',
                 'resolution': {'width': 16, 'height': 16},
                 'elements': [cube('c1', [0, 0, 0]), cube('c2', [4, 0, 0]), cube('c3', [4, -4, 0])],
                 'textures': [{'name': 'a', 'source': 'data:image/png;base64,' + png, 'uv_width': 16, 'uv_height': 16}],
                 'outliner': [grp('root', [0, 8, 0], ['c1', grp('foot_l', [4, 2, 0], ['c2', grp('fx_boot_l', [4, 0, 0])])]),
                              grp('fx_flame_boot_l', [4, -4, 0], ['c3'])]}
        geo, _, report = bbgeo.convert(json.dumps(model), 'mech', standalone=True, scale=False)
        parents = {b['name']: b.get('parent') for b in geo['minecraft:geometry'][0]['bones']}
        self.assertEqual('fx_boot_l', parents['fx_flame_boot_l'])
        self.assertTrue(any('reparented fx_flame_boot_l under fx_boot_l' in line for line in report))
        # A group with no matching bone is left alone, and a parented model changes nothing.
        model['outliner'].append(grp('antenna', [0, 20, 0]))
        geo, _, _ = bbgeo.convert(json.dumps(model), 'mech', standalone=True, scale=False)
        parents = {b['name']: b.get('parent') for b in geo['minecraft:geometry'][0]['bones']}
        self.assertEqual('mech', parents['antenna'])
        tree = bbgeo.read_bbmodel(model)[0]
        self.assertEqual([('fx_flame_boot_l', 'fx_boot_l')], bbgeo.reparent_effects(tree))
        self.assertEqual([], bbgeo.reparent_effects(tree), 'already moved: nothing left to do')

    def test_a_body_rig_model_gets_a_rig_report_beside_it(self):
        folder = os.path.join(self.tmp, 'mech (body)')
        os.makedirs(folder)
        with open(os.path.join(folder, 'model.bbmodel'), 'w') as f:
            f.write(bbmodel())
        with mock.patch('sys.stdout'):
            rd.rig_report(folder)
        with open(os.path.join(folder, 'rerig.txt')) as f:
            self.assertIn('Add a bone named', f.read())
        other = os.path.join(self.tmp, 'quartz_wafer')
        os.makedirs(other)
        with open(os.path.join(other, 'model.bbmodel'), 'w') as f:
            f.write(bbmodel())
        rd.rig_report(other)
        self.assertFalse(os.path.exists(os.path.join(other, 'rerig.txt')), 'only body rigs are checked')

    def test_scale_like_sizes_the_whole_model_like_ours(self):
        import bbgeo
        geo_dir = os.path.join(self.tmp, 'src/main/resources/assets/void_reclaimer/geo/body')
        os.makedirs(geo_dir)
        with open(os.path.join(geo_dir, 'test.geo.json'), 'w') as f:
            json.dump({'format_version': '1.12.0', 'minecraft:geometry': [{'description': {}, 'bones': [
                {'name': 'root', 'pivot': [0, 0, 0], 'cubes': [{'origin': [0, 0, 0], 'size': [4, 32, 4]}]}]}]}, f)
        whole, _, _ = bbgeo.convert(bbmodel(), 'w', standalone=True, scale=False)
        lo, hi = bbgeo.cubes_bbox(whole['minecraft:geometry'][0]['bones'])
        with mock.patch('sys.stdout'):
            s = rd.whole_model_scale(bbmodel(), 'body/test')
        self.assertAlmostEqual(s, 32 / max(h - l for l, h in zip(lo, hi)))
        with self.assertRaises(SystemExit), mock.patch('sys.stdout'):
            rd.whole_model_scale(bbmodel(), 'body/missing')
        src = os.path.join(self.tmp, 'whole.bbmodel')
        with open(src, 'w') as f:
            f.write(bbmodel())
        with mock.patch('sys.stdout'):
            rd.main(['--promote-as', 'entity/sized', '--source', src, '--scale-like', 'body/test'])
        lo, hi = bbgeo.cubes_bbox(self.geo('entity/sized.geo.json')['bones'])
        self.assertAlmostEqual(max(h - l for l, h in zip(lo, hi)), 32, places=3, msg='--promote-as takes --scale-like')

    def test_model_promote_refuses_what_it_cannot_convert(self):
        mesh = {'uuid': 'm', 'name': 'm', 'type': 'mesh', 'vertices': {}, 'faces': {}}
        for text in (bbmodel(extra_elements=[mesh]), bbmodel(textures=2)):
            two = text if 'mesh' in text else json.dumps(dict(json.loads(text), elements=[
                dict(e, faces={k: dict(v, texture=i % 2) for i, (k, v) in enumerate(e['faces'].items())})
                for e in json.loads(text)['elements']]))
            self.stage_model('core_mk2_model', two)
            with self.assertRaises(SystemExit), mock.patch('sys.stdout'):
                rd.main(['--promote', 'core_mk2_model'])
        self.assertFalse(os.path.exists(os.path.join(self.tmp, 'src/main/resources/assets/void_reclaimer/geo/module/core_mk2.geo.json')))
        with self.assertRaises(SystemExit):
            rd.main(['--promote', 'space_thruster'])  # its block does not exist yet

    def posted(self, path='/v2/inferences'):
        return [c[2] for c in Fake.state['calls'] if c[0] == 'POST' and c[1] == path and not c[2].get('check_cost')]

    def test_style_guide_palette_and_prompt_on_every_job(self):
        import style_guide
        style = style_guide.load_style()
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'quartz_wafer', '--asset', 'asteroid_ice', '--variants', '1'])
        bodies = {b['prompt_style']: b for b in self.posted()}
        for style_id, cat in (('rd_plus__mc_item', 'items'), ('rd_plus__mc_texture', 'blocks')):
            b = bodies[style_id]
            colors = rd.png_read(base64.b64decode(b['input_palette']))[0]
            want = [style_guide.rgb(c) + (255,) for c in style_guide.palette(style, cat)]
            self.assertEqual(colors, want, cat + ' palette sent as input_palette')
            tail = style['categories'][cat]['prompt_template'].replace('{prompt}', '')
            self.assertTrue(b['prompt'].endswith(tail), b['prompt'])
        with mock.patch('builtins.input', return_value='l'):
            rd.main(['--asset', 'core_mk2_model'])
        gen = self.posted('/v2/lowpoly/generate')[0]
        self.assertTrue(gen['input_palette'].startswith('data:image/png;base64,'))
        self.assertEqual(rd.png_read(base64.b64decode(gen['input_palette'].split(',', 1)[1]))[0],
                         [style_guide.rgb(c) + (255,) for c in style_guide.palette(style, 'body')])
        self.assertLessEqual(len(gen['prompt']), 250)
        self.assertIn('blocky Minecraft-style robot part', gen['prompt'])
        self.assertNotIn('boxy Minecraft-style model', gen['prompt'], 'no duplicated style phrase')
        long = rd.styled('word ' * 80, 'body', mock.Mock(no_style=False), 250)
        self.assertLessEqual(len(long), 250)
        self.assertTrue(long.split(', blocky')[0].endswith('word'), 'cut at a word boundary')

    def test_no_style_sends_plain_prompts(self):
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'quartz_wafer', '--variants', '1', '--no-style'])
        b = self.posted()[0]
        self.assertNotIn('input_palette', b)
        self.assertEqual(b['prompt'], 'thin round translucent quartz wafer disk')

    def test_categories(self):
        rows = {r['key']: r for r in rd.load_rows()}
        self.assertEqual(rd.category_for(rows['quartz_wafer'], 'image'), 'items')
        self.assertEqual(rd.category_for(rows['asteroid_ice'], 'image'), 'blocks')
        self.assertEqual(rd.category_for(rows['energy_full'], 'image'), 'gui')
        self.assertEqual(rd.category_for(rows['sun'], 'image'), 'sky')
        self.assertEqual(rd.category_for(rows['mod_logo'], 'image'), 'gui')
        self.assertEqual(rd.category_for(rows['rotor_arm_section'], 'model'), 'body')
        self.assertEqual(rd.category_for(rows['space_thruster'], 'model'), 'blocks')

    def test_setup_styles_and_custom_style_jobs(self):
        folder = os.path.join(self.tmp, 'docs/art/style/items')
        os.makedirs(folder)
        for n in range(5):  # one too many: only the first four (by name) are sent
            with open(os.path.join(folder, 'ref_%d.png' % n), 'wb') as f:
                f.write(checker(64, 64, False))
        with open(os.path.join(folder, 'notes.txt'), 'w') as f:
            f.write('not an image')
        rd.main(['--setup-styles'])
        created = [c[2] for c in Fake.state['calls'] if c[1] == '/v2/styles']
        self.assertEqual(len(created), 1, 'only categories with reference images get a custom style')
        self.assertEqual(created[0]['user_prompt_template'].count('{prompt}'), 1)
        self.assertEqual(len(created[0]['reference_images']), 4)
        ref = rd.png_read(base64.b64decode(created[0]['reference_images'][0]))
        self.assertEqual((len(ref[0]), len(ref)), (256, 256), '64x64 references are enlarged to the 256x256 RD needs')
        import style_guide
        self.assertEqual(created[0]['llm_instructions'], style_guide.instructions(style_guide.load_style(), 'items'))
        self.assertTrue(created[0]['force_bg_removal'], 'item styles remove the background')
        self.assertIn('cyan', created[0]['llm_instructions'])
        # Item jobs now use the custom style (RD Pro limits: 16x16 is fine); block jobs keep the base style.
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'quartz_wafer', '--asset', 'asteroid_ice', '--variants', '1'])
        styles = sorted(b['prompt_style'] for b in self.posted())
        self.assertEqual(styles, ['rd_plus__mc_texture', 'user__void_reclaimer_1'])
        # Running it again updates the same style instead of creating another.
        rd.main(['--setup-styles'])
        self.assertTrue(any(c[0] == 'PATCH' and c[1] == '/v2/styles/st1' for c in Fake.state['calls']))
        # If RD only takes one reference after all, the style is kept with the first one.
        Fake.state['max_refs'] = 1
        rd.main(['--setup-styles'])
        patches = [c[2] for c in Fake.state['calls'] if c[0] == 'PATCH']
        self.assertEqual([len(p['reference_images']) for p in patches[-2:]], [4, 1])
        self.assertEqual(len([c for c in Fake.state['calls'] if c[1] == '/v2/styles']), 1)

    def test_reference_images_are_256_square(self):
        import style_guide
        icon = checker(16, 16)
        big = rd.png_read(style_guide.reference_png(icon))
        self.assertEqual((len(big[0]), len(big)), (256, 256))
        self.assertEqual(big[0:16], [[px for px in rd.png_read(icon)[0] for _ in range(16)]] * 16, 'no smoothing')
        self.assertEqual(len(rd.png_read(style_guide.reference_png(checker(512, 512)))), 256)
        same = checker(256, 256)
        self.assertIs(style_guide.reference_png(same), same)
        for bad in (checker(100, 100), checker(64, 32)):
            with self.assertRaises(ValueError):
                style_guide.reference_png(bad)

    def test_style_guide_files_are_up_to_date(self):
        import style_guide
        style = style_guide.load_style()
        art = os.path.join(REAL_REPO, 'docs', 'art')
        with open(os.path.join(art, 'STYLE_GUIDE.md')) as f:
            self.assertEqual(f.read(), style_guide.guide_markdown(style),
                             'docs/art/STYLE_GUIDE.md is stale: run python3 tools/art/style_guide.py')
        for cat in style['categories']:
            self.assertEqual(self.read(os.path.join(art, 'palettes', cat + '.png')),
                             style_guide.palette_png(style_guide.palette(style, cat)), cat + ' palette is stale')
            self.assertLessEqual(len(style_guide.palette(style, cat)), 256, 'RD palettes hold at most 256 colours')

    def test_dry_run_needs_no_key_and_sends_nothing(self):
        os.environ.pop('RD_API_KEY')
        rd.main(['--status', 'placeholder', '--dry-run'])
        self.assertEqual(Fake.state['calls'], [])


if __name__ == '__main__':
    unittest.main()
