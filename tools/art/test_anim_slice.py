"""Tests for anim_slice.py (no network, no cost).

Run: python3 tools/art/test_anim_slice.py
"""
import json
import os
import sys
import tempfile
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import anim_slice as s  # noqa: E402


def frame(*v):
    return {'vector': list(v)}


def sample():
    return {'loop': True, 'animation_length': 2.0, 'bones': {
        'arm_l': {'rotation': {'0.0': frame(0, 0, 0), '1.0': frame(10, 20, 30), '2.0': frame(0, 0, 0)}},
        'arm_r': {'rotation': {'0.0': frame(0, 0, 0)}, 'position': {'0.0': frame(1, 2, 3), '2.0': frame(1, 2, 3)}},
        'fx_flame_l_core': {'scale': {'0.0': frame(1, 1, 1)}},
        'head': {'rotation': {'0.0': frame(5, 0, 0)}}}}


class Slice(unittest.TestCase):
    def test_cut_interpolates_the_edges_and_restarts_at_zero(self):
        out = s.slice_time(sample(), 0.5, 1.5)
        self.assertEqual(1.0, out['animation_length'])
        keys = out['bones']['arm_l']['rotation']
        self.assertEqual(['0.0', '0.5', '1.0'], list(keys))
        self.assertEqual([5.0, 10.0, 15.0], keys['0.0']['vector'])
        self.assertEqual([10, 20, 30], keys['0.5']['vector'])
        self.assertEqual([5.0, 10.0, 15.0], keys['1.0']['vector'])

    def test_a_bad_range_is_rejected(self):
        with self.assertRaises(ValueError):
            s.slice_time(sample(), 1.0, 1.0)


class Bones(unittest.TestCase):
    def test_keep_and_exclude(self):
        self.assertEqual({'arm_l', 'head'}, set(s.keep_bones(sample(), {'arm_l', 'head'})['bones']))
        self.assertEqual({'arm_l', 'arm_r', 'fx_flame_l_core'}, set(s.keep_bones(sample(), {'head'}, exclude=True)['bones']))

    def test_descendants_come_from_the_model(self):
        geo = {'minecraft:geometry': [{'bones': [{'name': 'root'}, {'name': 'arm', 'parent': 'root'}, {'name': 'elbow', 'parent': 'arm'},
                                                 {'name': 'hand', 'parent': 'elbow'}, {'name': 'leg', 'parent': 'root'}]}]}
        self.assertEqual({'arm', 'elbow', 'hand'}, s.descendants(geo, {'arm'}))


class Mirror(unittest.TestCase):
    def test_names_swap_sides(self):
        self.assertEqual('arm_r', s.mirror_name('arm_l'))
        self.assertEqual('fx_flame_r_core', s.mirror_name('fx_flame_l_core'))
        self.assertEqual('right_hand', s.mirror_name('left_hand'))
        self.assertEqual('head', s.mirror_name('head'))
        self.assertEqual('klaxon', s.mirror_name('klaxon'), 'letters inside words are not sides')

    def test_axes_flip_and_bones_swap(self):
        out = s.mirror(sample())
        self.assertIn('arm_r', out['bones'])
        self.assertEqual([10, -20, -30], out['bones']['arm_r']['rotation']['1.0']['vector'])
        self.assertEqual([-1, 2, 3], out['bones']['arm_l']['position']['0.0']['vector'])
        self.assertEqual([5, 0, 0], out['bones']['head']['rotation']['0.0']['vector'], 'a centre bone only flips Y and Z')

    def test_mirroring_twice_is_the_original(self):
        original = sample()
        twice = s.mirror(s.mirror(original))
        for bone, channels in original['bones'].items():
            for channel, keys in channels.items():
                for t, f in keys.items():
                    self.assertEqual(f['vector'], twice['bones'][bone][channel][t]['vector'])


class Retime(unittest.TestCase):
    def test_speed_scales_times_and_length(self):
        out = s.retime(sample(), 2.0)
        self.assertEqual(1.0, out['animation_length'])
        self.assertEqual(['0.0', '0.5', '1.0'], list(out['bones']['arm_l']['rotation']))
        with self.assertRaises(ValueError):
            s.retime(sample(), 0)


class Cli(unittest.TestCase):
    def test_writes_a_new_animation_beside_the_source(self):
        with tempfile.TemporaryDirectory() as tmp:
            path = os.path.join(tmp, 'x.animation.json')
            with open(path, 'w') as f:
                json.dump({'format_version': '1.8.0', 'animations': {'animation.void_reclaimer.m.swing': sample()}}, f)
            s.main([path, 'swing', 'swing_left', '--bones', 'arm_l', '--mirror', '--speed', '2'])
            with open(path) as f:
                out = json.load(f)['animations']
            self.assertIn('animation.void_reclaimer.m.swing', out, 'the source is kept')
            made = out['animation.void_reclaimer.m.swing_left']
            self.assertEqual(['arm_r'], list(made['bones']))
            self.assertEqual(1.0, made['animation_length'])


if __name__ == '__main__':
    unittest.main()
