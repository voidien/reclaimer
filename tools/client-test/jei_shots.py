#!/usr/bin/env python3
"""Screenshots JEI's recipe view for every item our machine recipes make, in the headless test client (see HANDOFF.md).

Start the client at 1920x1080 on a 1920x1080 Xvfb :99 (HANDOFF: "In-game client checks"), join a world, then:

    python3 tools/client-test/jei_shots.py [--out build/jei-shots] [--only carbon_paste,...]

It opens the inventory (E), searches each item in JEI's search box, clicks the first result (JEI shows the recipes that make it),
saves one PNG per item and writes index.html with all of them, so the recipes can be looked at one by one. A recipe that needs
several pages shows only its first. The coordinates are for 1920x1080 and a GUI scale of 4; change them with the options.
"""
import argparse
import html
import json
import os
import subprocess
import sys
import time

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
XT = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'xt.py')
RECIPES = os.path.join(REPO, 'src', 'generated', 'resources', 'data', 'void_reclaimer', 'recipe')
LANG = os.path.join(REPO, 'src', 'generated', 'resources', 'assets', 'void_reclaimer', 'lang', 'en_us.json')


def xt(*args):
    subprocess.run([sys.executable, '-I', XT, *map(str, args)], check=True)


def shot(path):
    subprocess.run(['import', '-window', 'root', path], check=True, env=dict(os.environ, DISPLAY=':99'))


def outputs():
    """item name (path) -> the recipes that make it, from the machine recipe files (not the Replicator's)."""
    found = {}
    for folder in ('create', 'freezing'):
        root = os.path.join(RECIPES, folder)
        for base, _, files in os.walk(root):
            for name in sorted(files):
                if not name.endswith('.json'):
                    continue
                with open(os.path.join(base, name), encoding='utf-8') as f:
                    recipe = json.load(f)
                for result in recipe.get('results', []):
                    if isinstance(result, dict) and 'amount' not in result and str(result.get('id', '')).startswith('void_reclaimer:'):
                        item = result['id'].split(':', 1)[1]
                        found.setdefault(item, []).append(os.path.relpath(os.path.join(base, name), root).replace('\\', '/'))
    return found


def main():
    p = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    p.add_argument('--out', default=os.path.join(REPO, 'build', 'jei-shots'))
    p.add_argument('--only', help='comma-separated item names (carbon_paste,...)')
    p.add_argument('--search', default='1745,1015', help='x,y of JEI\'s search box')
    p.add_argument('--first', default='1715,147', help='x,y of the first search result')
    p.add_argument('--center', default='960,540', help='x,y to click once to focus the game window')
    a = p.parse_args()
    with open(LANG, encoding='utf-8') as f:
        lang = json.load(f)
    items = outputs()
    if a.only:
        items = {k: v for k, v in items.items() if k in set(a.only.split(','))}
    os.makedirs(a.out, exist_ok=True)
    sx, sy = map(int, a.search.split(','))
    fx, fy = map(int, a.first.split(','))
    cx, cy = map(int, a.center.split(','))
    xt('click', cx, cy)
    time.sleep(1)
    xt('key', 'e')
    time.sleep(3)
    rows = []
    for item in sorted(items):
        name = lang.get('item.void_reclaimer.' + item, item.replace('_', ' '))
        xt('click', sx, sy)
        time.sleep(0.8)
        xt('chord', 'ctrl+a')
        time.sleep(0.3)
        xt('key', 'BackSpace')
        time.sleep(0.8)
        xt('type', name.lower(), 0.3)
        time.sleep(2.5)
        xt('click', fx, fy)
        time.sleep(2.5)
        path = os.path.join(a.out, item + '.png')
        shot(path)
        rows.append((item, name, items[item]))
        print('%s: %s' % (item, ', '.join(items[item])))
        xt('key', 'Escape')  # back to the inventory screen
        time.sleep(1.5)
    with open(os.path.join(a.out, 'index.html'), 'w', encoding='utf-8') as f:
        f.write('<!doctype html><meta charset="utf-8"><title>JEI recipe screenshots</title>'
                '<style>body{background:#111;color:#ddd;font:14px sans-serif}figure{margin:16px 0}img{max-width:100%}</style>')
        for item, name, recipes in rows:
            f.write('<figure><figcaption><b>%s</b> (%s): %s</figcaption><img src="%s.png"></figure>'
                    % (html.escape(name), item, html.escape(', '.join(recipes)), item))
    print('%d screenshots in %s' % (len(rows), a.out))


if __name__ == '__main__':
    main()
