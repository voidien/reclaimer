"""Tests for rig_check.py (no network, no cost).

Run: python3 tools/art/test_rig_check.py
"""
import os
import sys
import unittest

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import rig_check as rc  # noqa: E402


def group(name, origin, children=()):
    return {'name': name, 'uuid': 'u-' + name, 'origin': list(origin), 'rotation': [0, 0, 0], 'children': list(children)}


def model(tree, animations=None):
    """A bbmodel with an inline (Blockbench 4 style) outliner and no cubes."""
    return {'meta': {'box_uv': False}, 'elements': [], 'outliner': tree, 'textures': [],
            'animations': animations if animations is not None else []}


def animation(name, *bones):
    return {'name': name, 'animators': {'a%d' % i: {'name': b} for i, b in enumerate(bones)}}


SPEC = {'sockets': {'head': ['neck'], 'left_foot': ['foot_l']},
        'mounts': {'mount_core_1': {'bone': 'chest'}}, 'socket_parts': {}, 'poses': {'idle': 'hover'}}


def good_tree():
    return [group('root', (0, 32, 0), [
        group('chest', (0, 40, 0), [group('neck', (0, 54, 0))]),
        group('foot_l', (7, 4, 2), [group('fx_boot_l', (7, 1, 3))])])]


class RigCheck(unittest.TestCase):
    def test_clean_model_has_no_problems(self):
        problems, fixes = rc.check(model(good_tree(), [animation('hover', 'chest')]), SPEC)
        self.assertEqual([], problems)
        self.assertEqual([], fixes)

    def test_missing_bone_is_reported_with_a_fix(self):
        tree = [group('root', (0, 32, 0), [group('chest', (0, 40, 0)), group('foot_l', (7, 4, 2))])]
        problems, fixes = rc.check(model(tree, [animation('hover')]), SPEC)
        self.assertTrue(any("missing bone 'neck'" in p for p in problems))
        self.assertIn('Add a bone named neck', fixes)

    def test_duplicate_names_are_reported(self):
        tree = good_tree()
        tree[0]['children'].append(group('chest', (0, 0, 0)))
        problems, _ = rc.check(model(tree, [animation('hover')]), SPEC)
        self.assertTrue(any("duplicate bone name 'chest'" in p for p in problems))

    def test_a_flame_left_at_the_top_level_goes_under_its_effect_bone(self):
        tree = good_tree() + [group('fx_flame_boot_l', (7, -2, 3)), group('fx_flame_boot_l_core', (7, -1, 3))]
        problems, fixes = rc.check(model(tree, [animation('hover')]), SPEC)
        self.assertEqual(2, sum('is not under any bone' in p for p in problems))
        self.assertEqual(['Parent fx_flame_boot_l, fx_flame_boot_l_core under fx_boot_l'], fixes)

    def test_an_unhinted_orphan_goes_under_the_closest_bone(self):
        tree = good_tree() + [group('antenna', (0, 55, 0))]
        _, fixes = rc.check(model(tree, [animation('hover')]), SPEC)
        self.assertEqual(['Parent antenna under neck'], fixes)

    def test_missing_pose_animation_and_animators_on_unknown_bones(self):
        problems, _ = rc.check(model(good_tree(), [animation('other', 'ghost')]), SPEC)
        self.assertTrue(any("pose 'idle' wants animation 'hover'" in p for p in problems))
        self.assertTrue(any("keys 'ghost'" in p for p in problems))

    def test_pack_respects_the_250_character_box_and_keeps_order(self):
        fixes = ['Parent %s under foot_l' % ('x' * 40) for _ in range(12)]
        packed = rc.pack(fixes)
        self.assertGreater(len(packed), 1)
        self.assertTrue(all(len(p) <= 250 for p in packed))
        self.assertEqual(12, sum(p.count('Parent') for p in packed))

    def test_the_real_mech_spec_is_readable(self):
        import json
        with open(rc.DEFAULT_SPEC, encoding='utf-8') as f:
            need = rc.required_bones(json.load(f))
        self.assertIn('foot_l', need)
        self.assertIn('chest', need)


class Report(unittest.TestCase):
    def test_spec_for_matches_a_word_of_the_asset_name(self):
        self.assertTrue(rc.spec_for('mech (body)').endswith('mech.json'))
        self.assertTrue(rc.spec_for('drone_body').endswith('drone.json'))
        self.assertIsNone(rc.spec_for('drone_hull_section'), 'a part of the drone is not the rig')
        self.assertIsNone(rc.spec_for('quartz_wafer'))
        self.assertIsNone(rc.spec_for('mechanical_drill'), 'a word, not a prefix')

    def test_report_writes_instructions_for_a_bad_model_and_removes_the_file_for_a_good_one(self):
        import json
        import tempfile
        with tempfile.TemporaryDirectory() as tmp:
            spec_path = os.path.join(tmp, 'spec.json')
            with open(spec_path, 'w') as f:
                json.dump(SPEC, f)
            bad = os.path.join(tmp, 'bad.bbmodel')
            tree = good_tree() + [group('fx_flame_boot_l', (7, -2, 3))]
            with open(bad, 'w') as f:
                json.dump(model(tree, [animation('hover')]), f)
            out = os.path.join(tmp, 'rerig.txt')
            self.assertEqual(1, rc.report(bad, spec_path, out))
            with open(out) as f:
                text = f.read()
            self.assertIn('Parent fx_flame_boot_l under fx_boot_l', text)
            good = os.path.join(tmp, 'good.bbmodel')
            with open(good, 'w') as f:
                json.dump(model(good_tree(), [animation('hover')]), f)
            self.assertEqual(0, rc.report(good, spec_path, out))
            self.assertFalse(os.path.exists(out), 'a clean model leaves no stale rerig.txt')


if __name__ == '__main__':
    unittest.main()
