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
from pngutil import png_read, png_write  # noqa: E402

REPO = os.path.abspath(os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', '..'))
STYLE_PATH = os.path.join(REPO, 'docs', 'art', 'style.json')
MAX_REFERENCES = 4  # RD custom styles take up to 4 reference images
REFERENCE_SIZE = 256  # ...each exactly 256x256 (RD's app: square, 256 at native resolution)
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


def instructions(style, category):
    """The custom style's "Style Guidelines" (the API's llm_instructions): how RD should rewrite each prompt."""
    c = style['categories'][category]
    return ' '.join([style['mood'], c['background']] + c['rules'] + style['rules'])


def reference_png(data):
    """A reference image as RD wants it: square and exactly 256x256. Smaller squares that divide 256 (16x16 icons,
    64x64 skins) are enlarged without smoothing; exact multiples (512, 1024) are reduced. Raises ValueError otherwise."""
    pixels = png_read(data)
    w, h = len(pixels[0]), len(pixels)
    if w != h:
        raise ValueError('%dx%d is not square' % (w, h))
    if w == REFERENCE_SIZE:
        return data
    if w < REFERENCE_SIZE and REFERENCE_SIZE % w == 0:
        k = REFERENCE_SIZE // w
        return png_write([[px for px in row for _ in range(k)] for row in pixels for _ in range(k)])
    if w > REFERENCE_SIZE and w % REFERENCE_SIZE == 0:
        k = w // REFERENCE_SIZE
        return png_write([row[::k] for row in pixels[::k]])
    raise ValueError('%dx%d is neither a divisor nor a multiple of %d' % (w, h, REFERENCE_SIZE))


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


def form_markdown(style, key):
    """Copy-paste values for RD's "Create New Style" form (Based on RD Pro)."""
    c = style['categories'][key]
    yes = lambda b: 'tick' if b else 'leave unticked'  # noqa: E731
    return [
        '#### Make it in RD\'s app (My Styles, Create New Style, based on RD Pro)',
        '| Field | Value |',
        '|---|---|',
        '| Style Name | `%s %s` |' % (style['name'], c['title']),
        '| Description | %s |' % c['covers'],
        '| Style Icon | `%s` (or any) |' % c['icon'],
        '| Reference Images | 1 to %d square images, exactly %dx%d (`python3 tools/art/style_guide.py --reference IMAGE` '
        'makes one from an icon or a render). Use the images in `%s`. |' % (MAX_REFERENCES, REFERENCE_SIZE,
                                                                          REFERENCE_SIZE, c['references']),
        '| AI Prompt Enhancement | tick |',
        '| Style Guidelines | (the text below) |',
        '| User Prompt Template | `%s` |' % c['prompt_template'],
        '| Force color palette | tick, and upload `docs/art/palettes/%s.png` |' % key,
        '| Force Background Removal | %s |' % yes(c['remove_background']),
        '| Force width and height | leave unticked (sizes differ per asset) |',
        '',
        'Style Guidelines:',
        '```',
        instructions(style, key),
        '```',
        '',
    ]

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
        lines += form_markdown(style, key)
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


def main(argv=None):
    argv = sys.argv[1:] if argv is None else argv
    if argv[:1] == ['--reference']:
        # Turn icons or renders into RD reference images: square, exactly 256x256, next to the originals.
        for path in argv[1:]:
            with open(path, 'rb') as f:
                data = reference_png(f.read())
            out = os.path.splitext(path)[0] + '_ref256.png'
            with open(out, 'wb') as f:
                f.write(data)
            print('wrote', out)
        return
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
