#!/usr/bin/env python3
"""Cuts, mirrors, retimes and filters an animation, so one good animation can be reused.

    python3 tools/art/anim_slice.py FILE.animation.json SOURCE NEW [options]

SOURCE is the animation to start from (its full id, e.g. animation.void_reclaimer.mech_suit.swing_tool, or just the last part);
NEW is the name of the result (same prefix is added when it has no dots). The result is written into the same file unless --into
names another. Options, applied in this order:
  --start S --end E     keep only seconds S to E (keys at the edges are interpolated), time restarts at 0
  --bones a,b,c         keep only these bones (--geo MODEL.geo.json --children adds each bone's descendants)
  --without a,b         drop these bones
  --mirror              swap left and right: bones named *_l / *_r (or left/right) swap, rotation Y and Z and position X are negated
  --speed F             play F times faster (2 = half the length)
  --loop / --no-loop    set the loop flag (otherwise kept)

Examples: an arms-only swing for layering (--bones arm_r,elbow_r,hand_r), the other arm for free (--mirror), a calmer version (--speed 0.5).
"""
import argparse
import copy
import json
import re
import sys

CHANNELS = ('rotation', 'position', 'scale')


def parse_time(key):
    return float(key)


def format_time(t):
    return repr(round(t, 4))


def _sorted(keys):
    return sorted(keys.items(), key=lambda kv: parse_time(kv[0]))


def _lerp(a, b, f):
    return [x + (y - x) * f for x, y in zip(a, b)]


def _value_at(keys, t):
    """The vector of a channel at time t (linear between keys, held outside them)."""
    items = _sorted(keys)
    if t <= parse_time(items[0][0]):
        return list(items[0][1]['vector'])
    for (t0, k0), (t1, k1) in zip(items, items[1:]):
        t0f, t1f = parse_time(t0), parse_time(t1)
        if t0f <= t <= t1f:
            span = t1f - t0f
            return _lerp(k0['vector'], k1['vector'], 0.0 if span == 0 else (t - t0f) / span)
    return list(items[-1][1]['vector'])


def slice_time(anim, start, end):
    """Keeps start..end seconds. Edge keys are interpolated, so the cut animation starts and ends on the same pose as the original."""
    if end <= start:
        raise ValueError('--end must be after --start')
    out = copy.deepcopy(anim)
    out['animation_length'] = round(end - start, 4)
    for bone in out.get('bones', {}).values():
        for channel in CHANNELS:
            keys = bone.get(channel)
            if not keys:
                continue
            kept = {}
            for key, frame in _sorted(keys):
                t = parse_time(key)
                if start < t < end:
                    kept[format_time(t - start)] = frame
            kept[format_time(0.0)] = {'vector': _value_at(keys, start)}
            kept[format_time(end - start)] = {'vector': _value_at(keys, end)}
            bone[channel] = dict(_sorted(kept))
    return out


def keep_bones(anim, names, exclude=False):
    out = copy.deepcopy(anim)
    out['bones'] = {b: v for b, v in out.get('bones', {}).items() if (b in names) != exclude}
    return out


def descendants(geo, names):
    """names plus every bone below them in a .geo.json model."""
    bones = geo['minecraft:geometry'][0]['bones']
    children = {}
    for bone in bones:
        children.setdefault(bone.get('parent'), []).append(bone['name'])
    found, stack = set(), list(names)
    while stack:
        name = stack.pop()
        if name not in found:
            found.add(name)
            stack.extend(children.get(name, []))
    return found


def mirror_name(name):
    """Swaps the left/right word in a bone name: arm_l <-> arm_r, fx_flame_l_core <-> fx_flame_r_core, left_hand <-> right_hand."""
    swapped = re.sub(r'(?<![a-z])(l|r)(?![a-z])', lambda m: 'r' if m.group(1) == 'l' else 'l', name)
    swapped = re.sub(r'(left|right)', lambda m: 'right' if m.group(1) == 'left' else 'left', swapped)
    return swapped


def mirror(anim):
    """Mirrors across the body's middle: left and right bones swap and rotation Y and Z and position X change sign."""
    out = copy.deepcopy(anim)
    bones = {}
    for name, bone in out.get('bones', {}).items():
        for channel, signs in (('rotation', (1, -1, -1)), ('position', (-1, 1, 1))):
            for frame in bone.get(channel, {}).values():
                frame['vector'] = [v * s if v else 0.0 for v, s in zip(frame['vector'], signs)]
        bones[mirror_name(name)] = bone
    out['bones'] = bones
    return out


def retime(anim, speed):
    if speed <= 0:
        raise ValueError('--speed must be positive')
    out = copy.deepcopy(anim)
    out['animation_length'] = round(anim.get('animation_length', 0) / speed, 4)
    for bone in out.get('bones', {}).values():
        for channel in CHANNELS:
            if channel in bone:
                bone[channel] = {format_time(parse_time(k) / speed): v for k, v in _sorted(bone[channel])}
    return out


def find_animation(animations, name):
    if name in animations:
        return name
    matches = [k for k in animations if k.endswith('.' + name)]
    if len(matches) != 1:
        raise ValueError('animation %r: %d matches among %s' % (name, len(matches), ', '.join(sorted(animations))))
    return matches[0]


def main(argv=None):
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('file')
    p.add_argument('source')
    p.add_argument('new')
    p.add_argument('--into')
    p.add_argument('--start', type=float)
    p.add_argument('--end', type=float)
    p.add_argument('--bones')
    p.add_argument('--children', action='store_true')
    p.add_argument('--geo')
    p.add_argument('--without')
    p.add_argument('--mirror', action='store_true')
    p.add_argument('--speed', type=float)
    p.add_argument('--loop', dest='loop', action='store_true', default=None)
    p.add_argument('--no-loop', dest='loop', action='store_false')
    a = p.parse_args(argv)
    with open(a.file, encoding='utf-8') as f:
        data = json.load(f)
    animations = data['animations']
    source = find_animation(animations, a.source)
    anim = animations[source]
    if a.start is not None or a.end is not None:
        anim = slice_time(anim, a.start or 0.0, a.end if a.end is not None else anim['animation_length'])
    if a.bones:
        names = set(a.bones.split(','))
        if a.children:
            if not a.geo:
                raise ValueError('--children needs --geo')
            with open(a.geo, encoding='utf-8') as f:
                names = descendants(json.load(f), names)
        anim = keep_bones(anim, names)
    if a.without:
        anim = keep_bones(anim, set(a.without.split(',')), exclude=True)
    if a.mirror:
        anim = mirror(anim)
    if a.speed:
        anim = retime(anim, a.speed)
    if a.loop is not None:
        anim['loop'] = a.loop
    new = a.new if '.' in a.new else source.rsplit('.', 1)[0] + '.' + a.new
    target = a.into or a.file
    if target != a.file:
        with open(target, encoding='utf-8') as f:
            data = json.load(f)
    data['animations'][new] = anim
    with open(target, 'w', encoding='utf-8') as f:
        json.dump(data, f, indent='\t')
        f.write('\n')
    print('%s: %d bones, %.2f s, written to %s' % (new, len(anim.get('bones', {})), anim.get('animation_length', 0), target))
    return 0


if __name__ == '__main__':
    try:
        sys.exit(main())
    except ValueError as e:
        sys.exit(str(e))
