# Art tools

- `asset_list.py` writes `docs/art/ASSETS.md` and `docs/art/assets.csv` (every texture, model and animation, with prompts). Rerun it when assets change.
- `rd_generate.py` generates the CSV's rows with the [Retro Diffusion API](https://github.com/Retro-Diffusion/api-examples), so prompts never have to be typed by hand.
- `rd_edit.py` (used through `rd_generate.py --fix`) fixes art we already have with RD's edit tools and Pixel Fixer; see "Fixing existing art" below.
- `test_rd_generate.py` tests it offline against a fake RD server (CI runs it): `python3 tools/art/test_rd_generate.py`.

- `style_guide.py` builds `docs/art/STYLE_GUIDE.md` and the palettes in `docs/art/palettes/` from `docs/art/style.json`.

## Style
Every job follows the art style guide (`docs/art/STYLE_GUIDE.md`, defined in `docs/art/style.json`). Each row belongs to a category:

| Category | Covers |
|---|---|
| `items` | Item icons |
| `blocks` | Block textures and block models |
| `body` | Body parts and entity models |
| `gui` | HUD and screens |
| `sky` | Sky and effects |

To make a style by hand in RD's app instead, `docs/art/STYLE_GUIDE.md` has the form values for each category (name, guidelines, template, palette, checkboxes).

Then, on every job:
- **Palette:** the category's palette goes in as `input_palette`, so every pixel uses its colours. This applies to 3D models too.
- **Prompt:** the subject is wrapped in the category's prompt template.
- **Custom styles:**
  1. Pick 1 to 4 reference images for a category: generated assets that together show the look best.
  2. Copy them into the category's folder, `docs/art/style/<category>/` (PNG, JPEG or WebP). They are used in name order; a fifth and later are ignored. RD needs square images of exactly 256x256: PNGs that divide 256 (16x16 icons) are enlarged and exact multiples (512x512 renders) reduced automatically. `python3 tools/art/style_guide.py --reference IMAGE...` does the same for upload by hand.
  3. Run `python3 tools/art/rd_generate.py --setup-styles`. That creates an RD custom style with the references, the guide's rules as instructions, and the prompt template. Running it again updates the style, so add or swap references any time.

  If RD's API turns down several references, the tool keeps the style with the first one and says so.

  From then on, that category's 2D jobs use the custom style. It runs on RD Pro: $0.18 an image instead of $0.06. The style ids are kept in `art/rd/styles.json`.
- `--no-style` sends plain prompts with no palette, for one run.

To change the style, edit `docs/art/style.json`, then run `python3 tools/art/style_guide.py`. A test fails while the guide or the palettes are out of date.

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

**Checking a rig (`rig_check.py`)**
- `python3 tools/art/rig_check.py MODEL.bbmodel [--spec src/main/resources/assets/void_reclaimer/body_rigs/mech.json]` compares an RD model with a rig spec: every bone the spec names exists and is unique, every animation the poses name exists, animations only key real bones, and effect groups (flames, tool and watch pieces) are parented. A group left at the model's top level is moved by no bone, so a flame there floats in the air.
- It prints the problems and Re-rig instructions of at most 250 characters (the limit of RD's Re-rig box). Paste them one at a time, re-export, and run the check again.
- `rd_generate.py` runs the check by itself on every downloaded body-rig model (`mech (body)`, `drone (body)`, ...): it prints a one-line verdict and writes `rerig.txt` beside the model with the problems and the Re-rig text (the file is removed when the model is clean). Parts and modules are not rigs and are not checked.
- Re-rig before animating: RD deletes animations that use bones a Re-rig removes. Test: `python3 tools/art/test_rig_check.py` (runs in CI).

**Reusing animations (`anim_slice.py`)**
- `python3 tools/art/anim_slice.py FILE.animation.json SOURCE NEW [options]` makes a new animation in the same file from an existing one: `--start/--end` keep a time range (edge keys are interpolated), `--bones a,b` (with `--geo MODEL.geo.json --children`) keep only those bones and their descendants, `--without` drops bones, `--mirror` swaps left and right (`arm_l` and `arm_r`, rotation Y and Z and position X change sign), `--speed` retimes, `--loop/--no-loop`. So one good swing becomes an arms-only layer, its mirror for the other arm and a calmer version. Test: `python3 tools/art/test_anim_slice.py` (runs in CI).

**Animations**
- Run on the rig's model once that model exists, then the rig's `.bbmodel` is re-exported.
- Animation names and bones still need the Blockbench step in `docs/art/ASSETS.md`; `--promote` converts models, not animations.

## Output
Everything goes to `art/rd/`, which is gitignored:
- `art/rd/<asset>/v1.png`... are the ready-size variants; the originals are in `raw/`.
- `state.json` remembers jobs, RD asset ids and costs.
- `index.html` is the **picker**: each asset's current texture next to the new variants and model renders.
  - Tap a variant to promote it (tap again to undo), **Promote model** for a 3D model, or mark the asset **Keep current** or **Redo**. Each asset has a notes box.
  - The bar at the bottom counts what is decided, can hide decided assets, and shows the commands for your picks: one `--promote ...` command and one `--again --asset ...` command for the redos. **Copy commands**, then paste them into a terminal in the repo.
  - Picks and notes stay in that browser (its local storage) across visits and rebuilds. Promoted assets show as done.
  - Everything is inside the one file (images embedded, script `picker.js` and styles `picker.css` inlined), so it opens anywhere, a phone included, and can be sent on.
  - `--index` rebuilds it from what is staged, without sending anything.

## Moving staging between devices (phone, PC, or a session that needs to see it)
`art/rd/` is gitignored, so a normal push carries nothing. Push it on a throwaway branch with `-f`. In the repo root:
```
git checkout -B art-staging
git add -f art/rd
git rm -r -q --cached --ignore-unmatch art/rd/index.html
git add src
git status --short | head -20
git commit -m "RD staging: promoted art and variants"
git push -u origin art-staging
```
- `git add src` picks up what you promoted into the mod; the page `index.html` is left out because `--index` rebuilds it.
- Check the `git status` output: lines starting with `A` or `M` mean something will be committed. An empty list means nothing was added, and the push would carry nothing.
- If the push is rejected, `git push -u --force-with-lease origin art-staging` (only for this branch).

On the other device (or for someone reviewing):
```
git checkout main && git pull
git fetch origin art-staging
git checkout origin/art-staging -- art/rd src
git reset -q art/rd src
python3 tools/art/rd_generate.py --index
```
That leaves the staged variants and the promoted files as plain changes on `main`, and rebuilds the picker. Commit the `src` changes on a normal branch and open a PR. **Never merge `art-staging` and never commit `art/rd`.**

The picker's picks live in one browser's storage, so they do not move with the branch: run the copied commands before switching devices, or pick again.

`--promote` also records each promoted texture in `docs/art/promoted.txt`. Commit that file with the art, then run `python3 tools/art/asset_list.py` so `ASSETS.md` and `assets.csv` show those assets as `RD art (promoted)`.

## Promoting
**Images:** `--promote asset=n` copies variant n into the mod after checking its size. Assets without a single target file yet (future blocks named with `*` or `{...}`) are copied by hand once their block exists.

**3D models:** `--promote asset` (no number) turns `art/rd/<asset>/model.bbmodel` into the part's GeckoLib model (`tools/art/bbgeo.py`):
```
python3 tools/art/rd_generate.py --promote core_mk1_model
python3 tools/art/rd_generate.py --promote rotor_arm_section --bone blade=rotor_spin
```
- **Our skeleton is kept.** A part that already has a model keeps every bone exactly: names, parents and positions, which are the attach points the body is built from. Only the geometry is replaced: RD's boxes go into `<part>_frame`. A module without a model gets the module skeleton: `<part>` with `<part>_mount`, geometry on the mount.
- **`--bone RD_GROUP=OUR_BONE`** puts one of RD's groups under one of our bones, for parts that move. For example, a rotor blade under `rotor_spin` turns with the rotor animation. The promote output lists RD's groups. A wrong name stops the promote and changes nothing.
- **Scaled to fit.** RD builds models at 16+ texels and our parts are 2–66 pixels. The model is scaled to the old model's size and centered on it. A new module is scaled to `--fit` pixels on its longest side (default 4, like the thrusters) and stands on its mount. `--no-fit` keeps RD's size.
- **Rotated face UVs are baked.** RD's models turn some face textures by 90/180/270°. GeckoLib 4.9.3 only supports unrotated face UVs without warnings, so those faces are copied into the texture already turned, in a strip added below the atlas. The model stays in the geometry format GeckoLib supports (1.12.0).
- **Written:**
  - `geo/<folder>/<part>.geo.json`;
  - `textures/entity/body/<part>.png` (RD's texture);
  - an empty `<part>_accent.png`. The old mask matched the old texture, so paint a new one in Blockbench to get the accent tint back.
  - The files it replaces are backed up to `art/rd/<asset>/replaced/`.
- **Check it:** `art/rd/<asset>/promoted_preview.png` shows the converted model from the front, right, back, left and top, to compare with RD's `render.png`. The Gradle unit test `GeoModelLoadTest` loads every model in the mod through GeckoLib's own loader (CI runs it), so a model GeckoLib can't read fails the build.
- **Not converted for body parts:** animations. The body's animations belong to the chassis rigs and use our bone names, so keyframe them in Blockbench. Standalone models (below) do get their animations. Body rigs (`drone (body)`, ...) convert, but RD's groups land in the core section's frame unless you map them with `--bone`.

**Cutting Ship sections out of one whole-ship model:**
```
python3 tools/art/rd_generate.py --promote ship_bow_section --source art/rd/starship/model.bbmodel --from-group nose --scale-like body/ship
```
- `--source` is any `.bbmodel`, for example one made in RD's app.
- `--from-group` takes one RD group and everything inside it. Repeat it to combine groups, e.g. a nacelle plus its flame: `--from-group nacelle_l --from-group fx_thrust_l`.
- `--scale-like body/ship` sizes the *whole* source model like our Ship rig and uses that one scale for every part, so the sections keep the ship's proportions. `--scale N` sets the factor directly.
- The section's skeleton is kept as with any promote. The output says which groups exist and which old bones are now empty.
- Example mapping for the RD "Create style starship": `nose` → `ship_bow_section`, `cockpit` → `ship_bridge_section`, `rear` → `ship_keel_section`, `nacelle_l` + `fx_thrust_l` → `ship_engine_nacelle_section`, `wing_l` → `ship_wing_section`, `fin` → `ship_dorsal_spine_section`, `antenna` → `ship_sensor_mast_section`, `crate` → `ship_cargo_bay_section`, `plate_l` → `ship_hull_segment_section`. Its landing legs and hatch have no section yet.

**Standalone models with animations** (entities and blocks such as a drop pod, a helper drone, a docked starship):
```
python3 tools/art/rd_generate.py --promote-as entity/starship --source art/rd/starship/model.bbmodel
```
- **Written:** `geo/entity/starship.geo.json`, `textures/entity/starship.png` and `animations/entity/starship.animation.json`. Also `entity/` or `block/`.
- **Bones** keep RD's group names (`nose`, `wing_l`, ...) under a root bone named after the model.
- **Lost parents are repaired.** RD's Re-rig can leave effect groups (`fx_flame_*`, `fx_tool_*`, `fx_watch_*`) at the model's top level, where no bone moves them (a flame floats where the foot was). The promote puts them back under their effect bone (`fx_boot_l`, `fx_jet_l`, `fx_tool`, `fx_watch`; `bbgeo.PARENT_HINTS`) and prints each move.
- **Size:** RD's size is kept unless you pass `--fit`, `--scale` or `--scale-like` (e.g. `--scale-like body/ship` makes it as long as the Ship rig).
- **Animations** are converted the way Blockbench's own Bedrock export does it: rotation X/Y and position X negated, positions scaled with the model. Step keys are held until the next key, because GeckoLib reads only a keyframe's `post` value. Loop modes map to GeckoLib's (`loop`, `hold_on_last_frame`, play once).
- **Checks:** `GeoModelLoadTest` loads every animation through GeckoLib and checks that each moved bone exists in its model. For body animations, that means the rig or any section or module.
- The entity or block that shows the model is built in its own phase. The exception is `entity/starship`, which the Test Starship entity already draws: re-promote over it (`--promote-as entity/starship --scale-like body/ship --source NEW.bbmodel`) and keep the animation names `ignite_prep` and `gear_cycle`.

**RD's Minecraft (Java) export isn't used:** it tilts boxes freely, which needs Minecraft 1.21.11+. Always promote from the `.bbmodel`.
- The game draws these models from Phase 5b (body rendering) on.

## Fixing existing art (`--fix`)
Cheap repairs of an image we already have, with RD's canvas edit tools and Pixel Fixer. The input is the asset's
current file in the mod (`--fix ASSET`) or a staged variant (`--fix ASSET=N`). The result is saved as the asset's next
variant and shows on the contact sheet; promote it like any other (`--promote ASSET=N`).

| `--tool` | RD tool | Price | What it does |
|---|---|---|---|
| `palette` (default) | `palette_converter` | $0.001 | maps every pixel onto the asset's style-guide palette (its category's) |
| `reduce` | `color_reducer` | $0.001 | fewer colours (`--colors N`, default 16) |
| `downscale` | `k_centroid_downscale` | $0.001 | down to the asset's target size |
| `unbg` | `background_remover` | $0.01 | transparent background |
| `correct` | `pixel_correction` | $0.001 | cleans stray pixels |
| `pixel` | Pixel Fixer, standard | $0.002 | rebuilds soft or AI-rendered pixel art on its native grid |
| `pixel-neural` | Pixel Fixer, neural | $0.005 | neural rebuild at the asset's target size |

Dithering is off (our style guide has no dithering). Spending is as for generation: a free estimate (edit tools) or
RD's fixed price (Pixel Fixer), the balance check, and `Spend about $X? [y/N]`; `--dry-run` prints the plan for free.
Edit tools run as RD tasks with a `custom_id` built from the asset, the tool and the input image, so repeating a fix
returns the first result instead of paying again; Pixel Fixer results are remembered in `art/rd/state.json`. `--again`
makes a genuinely new run. The Pixel Fixer needs at least a 16x16 image.

Example: `python3 tools/art/rd_generate.py --fix asteroid_silicate --tool palette` (the asteroid that came out orange).

### Repainting part of an image
The same `--fix` command with RD's generative edit tools ($0.18 a run, run as RD tasks like the others):

| `--tool` | RD tool | Needs | What it does |
|---|---|---|---|
| `edit` | `image_edit` | `--prompt` | repaints the whole image from an instruction, keeping its layout ("make the flakes rough, no blade") |
| `inpaint` | `inpainting` | `--prompt`, `--mask mask.png` | repaints only the masked part |
| `outpaint` | `outpainting` | `--expand L,R,T,B` (pixels), optional `--prompt` | grows the canvas and paints the new border |
| `seam` | `seam_tiling` | `--axes x`, `y` or `xy` (default) | makes a block texture tile without visible seams |

`--seed N` fixes the seed. The mask is an RGBA PNG the size of the image: transparent where it must stay as it is,
opaque where to repaint (only alpha counts). The tool refuses a mask of the wrong size, or one that is opaque everywhere
(that would repaint the whole image). For consistent variants of one asset, derive them from one base with `edit` and a
prompt that pins the layout, rather than generating each from scratch (RD's advice).

