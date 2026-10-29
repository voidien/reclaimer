"""Offline tests for rd_generate.py against a fake Retro Diffusion server (no key, no network, no cost).

Run: python3 tools/art/test_rd_generate.py
"""
import base64
import csv
import http.server
import json
import os
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
        if self.path == '/v2/lowpoly/estimate':
            return self.reply({'operation': 'generate', 'size': 32, 'cost': 0.5})
        if self.path == '/v2/lowpoly/generate':
            tid = 't%d' % len(s['tasks'])
            s['tasks'][tid] = {'body': b, 'polls': 0, 'asset_id': 'a' + tid}
            s['charged'] += 1
            return self.reply({'task_id': tid, 'asset_id': 'a' + tid, 'cost': 0.5}, 202)
        if self.path.endswith('/animate'):
            tid = 't%d' % len(s['tasks'])
            s['tasks'][tid] = {'body': b, 'polls': 0, 'animate': True}
            s['charged'] += 1
            return self.reply({'task_id': tid}, 202)
        if self.path.endswith('/export'):
            name = {'blockbench': 'model.bbmodel', 'atlas': 'atlas.png'}[b['target']]
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
        self.assertTrue(os.path.exists(self.staged('index.html')))
        rd.main(['--promote', 'quartz_wafer=3'])
        self.assertTrue(os.path.exists(os.path.join(self.tmp, rd.TEXTURES, 'item', 'quartz_wafer.png')))

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

    def test_model_style_blocky_or_lite(self):
        # Without --model-style a model batch prices both styles and asks; picking Lite is the go-ahead.
        with mock.patch('builtins.input', return_value='l') as ask:
            rd.main(['--asset', 'core_mk1_model'])
        self.assertEqual(ask.call_count, 1, 'one question: the style choice')
        self.assertIn('blocky [l]ite', ask.call_args[0][0])
        gen = [c for c in Fake.state['calls'] if c[1] == '/v2/lowpoly/generate']
        self.assertEqual([c[2]['style'] for c in gen], ['rd_lowpoly__blocky_lite'])
        estimates = [c[2]['style'] for c in Fake.state['calls'] if c[1] == '/v2/lowpoly/estimate']
        self.assertEqual(sorted(set(estimates)), ['rd_lowpoly__blocky', 'rd_lowpoly__blocky_lite'], 'both priced')
        # Declining sends nothing; --yes without a style refuses rather than guessing.
        charged = Fake.state['charged']
        with mock.patch('builtins.input', return_value='n'):
            rd.main(['--asset', 'core_mk2_model'])
        with self.assertRaises(SystemExit):
            rd.main(['--asset', 'core_mk2_model', '--yes'])
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
        self.stage_model('core_mk1_model', bbmodel())
        rd.main(['--promote', 'core_mk1_model'])
        geo = self.geo('module/core_mk1.geo.json')
        bones = {b['name']: b for b in geo['bones']}
        self.assertEqual(list(bones)[:2], ['core_mk1', 'core_mk1_mount'], 'module skeleton')
        self.assertEqual(bones['core_mk1_body']['parent'], 'core_mk1_mount')
        self.assertEqual(bones['core_mk1_blade']['parent'], 'core_mk1_body')
        self.assertEqual(len(bones['core_mk1_mount']['cubes']), 1, 'the loose box sits on the mount')
        lo, hi = bbgeo.cubes_bbox(geo['bones'])
        self.assertAlmostEqual(max(h - l for l, h in zip(lo, hi)), 4.0, 3, 'fitted to 4 px')
        self.assertAlmostEqual(lo[1], 0.0, 3, 'stands on the mount')
        self.assertEqual((geo['description']['texture_width'], geo['description']['texture_height']), (32, 32))
        tex = os.path.join(self.tmp, rd.TEXTURES, 'entity/body')
        self.assertEqual(rd.png_read(self.read(os.path.join(tex, 'core_mk1.png'))), rd.png_read(checker(32, 32, False)))
        accent = rd.png_read(self.read(os.path.join(tex, 'core_mk1_accent.png')))
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
            rd.main(['--asset', 'core_mk1_model'])
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
        ref = os.path.join(self.tmp, 'docs/art/style/items_reference.png')
        os.makedirs(os.path.dirname(ref))
        with open(ref, 'wb') as f:
            f.write(checker(64, 64, False))
        rd.main(['--setup-styles'])
        created = [c[2] for c in Fake.state['calls'] if c[1] == '/v2/styles']
        self.assertEqual(len(created), 1, 'only categories with a reference image get a custom style')
        self.assertEqual(created[0]['user_prompt_template'].count('{prompt}'), 1)
        self.assertEqual(len(created[0]['reference_images']), 1)
        self.assertIn('cyan', created[0]['llm_instructions'])
        # Item jobs now use the custom style (RD Pro limits: 16x16 is fine); block jobs keep the base style.
        with mock.patch('builtins.input', return_value='y'):
            rd.main(['--asset', 'quartz_wafer', '--asset', 'asteroid_ice', '--variants', '1'])
        styles = sorted(b['prompt_style'] for b in self.posted())
        self.assertEqual(styles, ['rd_plus__mc_texture', 'user__void_reclaimer_1'])
        # Running it again updates the same style instead of creating another.
        rd.main(['--setup-styles'])
        self.assertTrue(any(c[0] == 'PATCH' and c[1] == '/v2/styles/st1' for c in Fake.state['calls']))
        self.assertEqual(len([c for c in Fake.state['calls'] if c[1] == '/v2/styles']), 1)

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
