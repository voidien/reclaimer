#!/usr/bin/env python3
"""Checks a Retro Diffusion .bbmodel against one of our whole-model rig specs, and writes Re-rig instructions for the gaps.

    python3 tools/art/rig_check.py MODEL.bbmodel [--spec src/main/resources/assets/void_reclaimer/body_rigs/mech.json]

What it checks (a rig spec is `body_rigs/<rig>.json`):
  * every bone the spec names (sockets, socket_parts, mounts) exists, and bone names are unique;
  * every animation the spec's `poses` names exists in the model, and animations only key bones that exist;
  * effect groups (`fx_*`, flames, tool pieces) are parented. A group at the model's top level next to `root` is not
    moved by any body bone, so a flame left there floats where the foot used to be.
Exit code 0 when clean, 1 when there are problems. Each problem comes with a suggested fix, and the fixes are
packed into instructions of at most 250 characters, the limit of RD's Re-rig box, to paste there one at a time.
Re-rig before animating: animations that use removed bones are deleted by RD.
"""
import argparse
import json
import math
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import bbgeo  # noqa: E402

DEFAULT_SPEC = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..', 'src', 'main', 'resources', 'assets',
                            'void_reclaimer', 'body_rigs', 'mech.json')
REAL_LIMIT = 250


def groups_of(model):
    """name -> {'parent': name or None, 'origin': [x, y, z], 'depth': n, 'top': bool}; also the duplicates found."""
    tree = bbgeo.read_bbmodel(model)[0]
    found, duplicates = {}, []

    def walk(node, parent, depth):
        kind, g = node
        if kind != 'group':
            return
        if g['name'] in found:
            duplicates.append(g['name'])
        found[g['name']] = {'parent': parent, 'origin': list(g['origin']), 'depth': depth, 'top': parent is None}
        for child in g['children']:
            walk(child, g['name'], depth + 1)

    for node in tree:
        walk(node, None, 0)
    return found, duplicates


def required_bones(spec):
    need = set()
    for bones in (spec.get('sockets') or {}).values():
        need.update(bones)
    for bones in (spec.get('socket_parts') or {}).values():
        need.update(bones)
    for mount in (spec.get('mounts') or {}).values():
        need.add(mount['bone'])
    return need


def distance(a, b):
    return math.dist(a, b)


def suggest_parent(name, groups, roots):
    """Where an unparented group belongs: by its name's hint, else the closest parented non-effect bone to its pivot."""
    candidate = bbgeo.hint_parent(name)
    if candidate in groups and (not groups[candidate]['top'] or candidate in roots):
        return candidate
    best, best_d = None, None
    for other, info in groups.items():
        if other == name or other.startswith('fx_') or (info['top'] and other not in roots):
            continue
        d = distance(groups[name]['origin'], info['origin'])
        if best_d is None or d < best_d:
            best, best_d = other, d
    return best


def check(model, spec):
    """Returns (problems, fixes): problems are readable lines, fixes are (kind, text) Re-rig sentences."""
    groups, duplicates = groups_of(model)
    problems, fixes = [], []

    for name in sorted(set(duplicates)):
        problems.append('duplicate bone name %r (names are the attach points and must be unique)' % name)
        fixes.append('Rename the duplicate %s so every bone name is unique' % name)

    missing = sorted(required_bones(spec) - set(groups))
    for name in missing:
        problems.append('missing bone %r (the rig spec needs it)' % name)
        fixes.append('Add a bone named %s' % name)

    # The body bone tree is the one rooted at the first top-level group; other top-level groups are detached.
    top = [n for n, g in groups.items() if g['top']]
    roots = {top[0]} if top else set()
    detached = [n for n in top if n not in roots]
    by_parent = {}
    for name in detached:
        parent = suggest_parent(name, groups, roots)
        problems.append('%r is not under any bone (no body bone moves it)%s'
                        % (name, ', put it under %r' % parent if parent else ''))
        if parent:
            by_parent.setdefault(parent, []).append(name)
    for parent, names in sorted(by_parent.items()):
        fixes.append('Parent %s under %s' % (', '.join(names), parent))

    animations = {a.get('name'): a for a in model.get('animations', [])}
    for pose, anim in sorted((spec.get('poses') or {}).items()):
        if anim not in animations:
            problems.append('pose %r wants animation %r, which the model does not have' % (pose, anim))
    for anim in animations.values():
        for animator in anim.get('animators', {}).values():
            if animator.get('name') not in groups:
                problems.append('animation %r keys %r, which is not a bone' % (anim.get('name'), animator.get('name')))
    return problems, fixes


def pack(fixes, limit=REAL_LIMIT):
    """Packs fix sentences into instructions of at most `limit` characters, in order."""
    out, current = [], ''
    for fix in fixes:
        piece = fix[:limit]  # a single fix longer than the box is cut; nothing better to do
        candidate = piece if not current else current + '. ' + piece
        if len(candidate) <= limit:
            current = candidate
        else:
            out.append(current)
            current = piece
    if current:
        out.append(current)
    return out


RIG_DIR = os.path.dirname(os.path.abspath(DEFAULT_SPEC))


def spec_for(asset):
    """The rig spec of a whole-body asset: the one whose name is a word of the asset's name, which must also say "body"
    ("mech (body)" -> mech.json). A section or module of the drone ("drone_hull (section)") is not a rig."""
    words = {w for w in ''.join(c if c.isalnum() else ' ' for c in asset.lower()).split()}
    if 'body' in words and os.path.isdir(RIG_DIR):
        for file in sorted(os.listdir(RIG_DIR)):
            if file.endswith('.json') and file[:-5] in words:
                return os.path.join(RIG_DIR, file)
    return None


def report(model_path, spec_path, out_path):
    """Checks a model and writes the findings and the Re-rig instructions to `out_path` (removed when the model is clean).
    Returns the number of problems."""
    with open(model_path, encoding='utf-8') as f:
        model = json.load(f)
    with open(spec_path, encoding='utf-8') as f:
        spec = json.load(f)
    problems, fixes = check(model, spec)
    if not problems:
        if os.path.exists(out_path):
            os.remove(out_path)
        return 0
    lines = ['Rig check against %s: %d problem(s)' % (os.path.basename(spec_path), len(problems))]
    lines += ['  - ' + p for p in problems]
    lines += ['', 'Re-rig instructions (paste into RD one at a time, then export and check again):']
    lines += ['%d. %s' % (i, text) for i, text in enumerate(pack(fixes), 1)]
    with open(out_path, 'w', encoding='utf-8') as f:
        f.write('\n'.join(lines) + '\n')
    return len(problems)


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('model', help='the RD .bbmodel (or its .json export)')
    parser.add_argument('--spec', default=DEFAULT_SPEC, help='rig spec JSON (default: the Mech)')
    args = parser.parse_args(argv)
    with open(args.model, encoding='utf-8') as f:
        model = json.load(f)
    with open(args.spec, encoding='utf-8') as f:
        spec = json.load(f)
    problems, fixes = check(model, spec)
    if not problems:
        print('OK: the model has every bone and animation the spec needs, and nothing is left unparented.')
        return 0
    print('%d problem(s):' % len(problems))
    for p in problems:
        print('  - ' + p)
    print('\nRe-rig instructions (paste one at a time, then re-run this check):')
    for i, text in enumerate(pack(fixes), 1):
        print('  %d. [%d chars] %s' % (i, len(text), text))
    return 1


if __name__ == '__main__':
    sys.exit(main())
