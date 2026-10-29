#!/usr/bin/env python3
"""Builds the art style guide from docs/art/style.json.

Writes docs/art/STYLE_GUIDE.md, docs/art/palettes/<category>.png (a 1-pixel-high strip with one pixel per colour, the
format Retro Diffusion's `input_palette` takes) and docs/art/palettes/<category>_preview.png (swatches to look at).
rd_generate.py reads the same JSON, so the guide, the palettes and the generated art never disagree.

Run from anywhere: python3 tools/art/style_guide.py
"""
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from pngutil import png_write  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
STYLE_PATH = os.path.join(REPO, 'docs', 'art', 'style.json')
MAX_REFERENCES = 4  # RD custom styles take up to 4 reference images
IMAGE_TYPES = ('.png', '.jpg', '.jpeg', '.webp')


def load_style(path=STYLE_PATH):
    with open(path) as f:
        return json.load(f)


def rgb(hex_color):
    h = hex_color.lstrip('#')
    return int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16)


def reference_files(repo, category_def):
    """The category's reference images, sorted by name, at most MAX_REFERENCES (extra ones are reported)."""
    folder = os.path.join(repo, category_def['references'])
    if not os.path.isdir(folder):
        return [], []
    files = sorted(os.path.join(folder, f) for f in os.listdir(folder) if f.lower().endswith(IMAGE_TYPES))
    return files[:MAX_REFERENCES], files[MAX_REFERENCES:]


def palette(style, category):
    """The category's colours in ramp order, without duplicates."""
    out = []
    for ramp in style['categories'][category]['ramps']:
        for c in style['ramps'][ramp]:
            if c.upper() not in out:
                out.append(c.upper())
    return out


def palette_png(colors):
    return png_write([[rgb(c) + (255,) for c in colors]])


def preview_png(style, category, cell=16):
    rows = [[rgb(c) + (255,) for c in style['ramps'][r]] for r in style['categories'][category]['ramps']]
    width = max(len(r) for r in rows) * cell
    out = []
    for colors in rows:
        line = []
        for c in colors:
            line += [c] * cell
        line += [(30, 32, 40, 255)] * (width - len(line))
        for _ in range(cell):
            out.append(list(line))
        out.append([(30, 32, 40, 255)] * width)
    return png_write(out)


def guide_markdown(style):
    cats = style['categories']
    lines = [
        '# Art style guide',
        '',
        'How Void Reclaimer\'s art looks, for people and for Retro Diffusion. Generated from `docs/art/style.json` by '
        '`tools/art/style_guide.py`: edit the JSON, then rerun the script. `tools/art/rd_generate.py` applies the same '
        'file to every job (see "How the tool applies it").',
        '',
        '## Mood',
        style['mood'],
        '',
        '## Rules for everything',
    ]
    lines += ['- ' + r for r in style['rules']]
    lines += ['', '## Colour ramps', 'Each ramp runs dark to light. Shade an object within one ramp.', '',
              '| Ramp | Colours |', '|---|---|']
    for name, colors in style['ramps'].items():
        lines.append('| %s | %s |' % (name, ' '.join('`%s`' % c for c in colors)))
    lines += ['', '## Categories', 'Every asset belongs to one category. Each category has its own locked palette: '
              'Retro Diffusion snaps every pixel to it.', '']
    for key, c in cats.items():
        colors = palette(style, key)
        lines += ['### %s (`%s`)' % (c['title'], key), c['covers'], '']
        lines += ['- ' + r for r in c['rules']]
        lines += ['',
                  '- **Palette:** %d colours from %s: `docs/art/palettes/%s.png` (swatches: `%s_preview.png`).'
                  % (len(colors), ', '.join(c['ramps']), key, key),
                  '- **Prompt template:** `%s`' % c['prompt_template'],
                  '- **Reference images (for an RD custom style):** up to %d in `%s`' % (MAX_REFERENCES, c['references']),
                  '']
    lines += [
        '## How the tool applies it',
        '`rd_generate.py` puts every row in a category: item icons in `items`, block textures and block models in '
        '`blocks`, body parts and entity models in `body`, HUD and screens in `gui`, sky and effects in `sky`. Then, on '
        'every job:',
        '- **Palette:** the category\'s palette PNG is sent as `input_palette`, so every pixel uses its colours (2D '
        'images and 3D models alike).',
        '- **Prompt:** the subject from `assets.csv` is wrapped in the category\'s prompt template.',
        '- **Custom style:** once a category has reference images (1 to %d in its folder above; PNG, JPEG or WebP) '
        'and you have run '
        '`python3 tools/art/rd_generate.py --setup-styles`, that category\'s 2D jobs use your RD custom style instead '
        'of the base style. A custom style is an RD Pro style ($0.18 an image instead of $0.06). It carries the '
        'reference images, the rules above as instructions, and the prompt template. Choosing references: generate '
        'a few assets, pick the ones that best show the look together, and copy them into the folder.' % MAX_REFERENCES,
        '- `--no-style` turns all of this off for a run.',
        '',
    ]
    return '\n'.join(lines)


def main():
    style = load_style()
    out_dir = os.path.join(REPO, 'docs', 'art', 'palettes')
    os.makedirs(out_dir, exist_ok=True)
    for key in style['categories']:
        with open(os.path.join(out_dir, key + '.png'), 'wb') as f:
            f.write(palette_png(palette(style, key)))
        with open(os.path.join(out_dir, key + '_preview.png'), 'wb') as f:
            f.write(preview_png(style, key))
    with open(os.path.join(REPO, 'docs', 'art', 'STYLE_GUIDE.md'), 'w') as f:
        f.write(guide_markdown(style))
    print('wrote docs/art/STYLE_GUIDE.md and %d palettes' % len(style['categories']))


if __name__ == '__main__':
    main()
