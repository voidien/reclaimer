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


def checker(w, h, transparent_corner=True):
    px = [[((x // 2 + y // 2) % 2 * 200, 100, 50, 0 if transparent_corner and x < w // 4 and y < h // 4 else 255)
           for x in range(w)] for y in range(h)]
    return rd.png_write(px)


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

    def test_dry_run_needs_no_key_and_sends_nothing(self):
        os.environ.pop('RD_API_KEY')
        rd.main(['--status', 'placeholder', '--dry-run'])
        self.assertEqual(Fake.state['calls'], [])


if __name__ == '__main__':
    unittest.main()
