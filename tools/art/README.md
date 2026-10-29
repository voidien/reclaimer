# Art tools

- `asset_list.py` writes `docs/art/ASSETS.md` and `docs/art/assets.csv` (every texture, model and animation, with prompts). Rerun it when assets change.
- `rd_generate.py` generates the CSV's rows with the [Retro Diffusion API](https://github.com/Retro-Diffusion/api-examples), so prompts never have to be typed by hand.
- `test_rd_generate.py` tests it offline against a fake RD server (CI runs it): `python3 tools/art/test_rd_generate.py`.

## Setup
- Python 3.9 or newer. Standard library only; nothing to install.
- A Retro Diffusion API key (`rdpk-...`, from your account at retrodiffusion.ai), in an environment variable:
  - macOS/Linux: `export RD_API_KEY=rdpk-...`
  - Windows PowerShell: `$env:RD_API_KEY = "rdpk-..."`

  The key is only sent to RD. It is never written to a file, so never paste it into the repo.
- Run from the repo root on your own computer.

## Spending
- `--dry-run` sends nothing and needs no key. It prints the jobs and a price from RD's published table (marked `*`).
- Without `--dry-run`, the tool:
  1. prices every job with RD's free estimates;
  2. checks your balance and stops if it is too low;
  3. asks `Spend about $X? [y/N]`. Only `y` sends anything; `--yes` skips the question.
- **Re-running is safe.**
  - Assets that are already done are skipped.
  - Jobs still running are polled, not resubmitted.
  - A crashed run that sends the same job again uses the same `Idempotency-Key`, so RD returns the old job without charging twice.
  - `--again` makes genuinely new variants.

Rough prices (RD, 2026-10-29):

| Job | Price |
|---|---|
| RD Plus Minecraft image (`rd_plus__mc_item`, `rd_plus__mc_texture`) | $0.06 each, so 4 variants = $0.24 |
| RD Fast or Mini image (`--image-tier fast`) | $0.03 each |
| Low-poly blocky model | $0.25 (16 texels) / $0.50 (32) / $0.85 (64) / $1.20 (128) / $3.00 (256) |
| Low-poly blocky lite model (`--model-style blocky_lite`) | $0.10 flat |
| Animation | $0.15–1.00 by model size (lite $0.08) |

The 51 placeholders at 4 variants each come to about $12.

## Choosing rows
Filters combine. Each can be given more than once:

| Option | Example |
|---|---|
| `--status` | `placeholder`, `"art drop"`, `missing`, `future` |
| `--group` | `Materials`, `Blocks`, `"Body models"`, `"Missing module models"` |
| `--asset` | `quartz_wafer`, `"mech_*"`, `"drone (body)"` |
| `--method` | `image`, `3d`, `anim` |
| `--phase` | `now`, `9`, `11`, `later` |
| `--limit` | `5` |

Rows that are hand work are never sent: the `_accent` masks, and the skins that come with a model.

## Examples
```
python3 tools/art/rd_generate.py --status placeholder --dry-run
python3 tools/art/rd_generate.py --group Materials --variants 2
python3 tools/art/rd_generate.py --group "Missing module models"                 # asks: Blocky or Blocky Lite
python3 tools/art/rd_generate.py --group "Missing module models" --model-style blocky_lite
python3 tools/art/rd_generate.py --asset "drone (body)" --asset "animation.void_reclaimer.drone.*"
python3 tools/art/rd_generate.py --resume          # poll jobs from an interrupted run
python3 tools/art/rd_generate.py --promote quartz_wafer=2
python3 tools/art/rd_generate.py --balance
```

## What it does with each row

**Item icons**
- Style: `rd_plus__mc_item` at 16x16, with background removal.
- Variants: 4 per asset.

**Block textures**
- Style: `rd_plus__mc_texture` at 16x16.
- Tiling (`tile_x`/`tile_y`) is on when the row says tileable.

**HUD, GUI and sky sprites**
- Styles: `rd_plus__ui_element` (GUI), `rd_plus__environment` and `rd_plus__default` (sky), `rd_pro__simple` (the energy bar).
- Sizes below a style's minimum are generated at a whole multiple, then reduced to the exact size with a pixel-art-safe downscale. For example, the 90x9 energy bar is generated at 180x18.

**Animated textures (Nanite Swarm, portal, power beam)**
- Style: `rd_animation__vfx`, returned as a sprite sheet.
- The tool cuts the sheet into 4 frames, scales each to 16x16 and stacks them into the vertical strip Minecraft's `.mcmeta` expects.

**3D models**
- Style: RD's **Blocky** (`rd_lowpoly__blocky`, $0.25–3.00 by size) or **Blocky Lite** (`rd_lowpoly__blocky_lite`, $0.10 flat, simpler builds, good for drafts). Both are box-only, so they fit GeckoLib and Minecraft. Detailed is not offered: it adds cylinders and spheres.
- Pick one with `--model-style blocky` or `--model-style blocky_lite`. Without the flag, a run that makes models prices both and asks `Model style: [b]locky $X, blocky [l]ite $Y, or [n]othing`. The answer is the go-ahead. With `--yes` the flag is required.
- Animations follow the rig's style (Lite animations cost $0.08).
- The asset's current item icon is sent as a reference image, so model and icon match (`--no-refs` turns this off).
- Saved files: `model.bbmodel` (with every animation), `atlas.png`, `render.png` and `turntable.png`.
- RD's `minecraft` export is not used: it needs Minecraft 1.21.11+ for tilted boxes, and the mod is on 1.21.1.

**Animations**
- Run on the rig's model once that model exists, then the rig's `.bbmodel` is re-exported.
- Bone names and animation names still need the Blockbench step in `docs/art/ASSETS.md`.

## Output
Everything goes to `art/rd/`, which is gitignored:
- `art/rd/<asset>/v1.png`... are the ready-size variants; the originals are in `raw/`.
- `state.json` remembers jobs, RD asset ids and costs.
- `index.html` is a contact sheet: each asset's current texture next to the new variants and model renders.

`--promote asset=n` copies one variant into the mod after checking its size. Assets without a single target file yet (future blocks named with `*` or `{...}`) are copied by hand once their block exists.
