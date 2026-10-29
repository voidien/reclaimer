# Art asset list

Everything the mod draws, as of `main` on 2026-10-29 (after Phase 8 and art drop 1), plus what the plan still needs. The list is for generating art with Retro Diffusion (RD); `assets.csv` next to this file has every row with its full prompt, ready for batch work.

## At a glance
| What | Count | How |
|---|---|---|
| Placeholder textures (programmer art, made by us) | 51 | RD image: 43 item icons + 8 block textures |
| Art drop 1 icons, block faces, HUD sprites (redo for one consistent style) | 49 | RD image |
| Art drop 1 3D models: 3 body rigs, 3 first-person rigs, 30 sections, 4 modules | 40 | RD 3D (blocky), then fitted to our bone names |
| Art drop 1 body animations | 16 | RD 3D animate, then renamed and retargeted |
| Art drop 1 body skins (80: a base texture and an accent mask per model) | 80 | Base comes with the RD 3D model; accent masks painted in Blockbench |
| Module models missing for the body (5b) | 9 | RD 3D (blocky) |
| New optional HUD/GUI/sky sprites (need a small code change to show) | 7 | RD image |
| Future-phase assets (Phases 7b, 9, 11–14, later tracks) | 20 | Mixed, per row |

## Retro Diffusion's modes used here
- **RD image:** pixel images. Item icons, tileable block textures (its tiling option is best at 16x16 or 32x32), block faces, HUD/GUI and sky sprites.
- **RD image, VFX/animation:** sprite-sheet animations. Used for the Nanite Swarm icon, the portal surface and the power beam. Minecraft wants the frames stacked in one vertical strip.
- **RD 3D, Blocky or Blocky Lite** (`rd_lowpoly__blocky`, `rd_lowpoly__blocky_lite`): boxes only, with pixel-art textures. Lite is $0.10 a model with simpler builds, good for drafts; Blocky costs $0.25–3.00 by size. The Detailed styles add cylinders and spheres, which GeckoLib and Minecraft can't use. It exports to Blockbench (`.bbmodel`, with animations) and as a Minecraft Java resource pack.
  - Static blocks (Space Thruster, Helm, Electrolyzer, Spinner, Monolith): use the **Java** export.
  - Anything rigged or animated (body parts, Mass Driver, Spire, drop pod, helper drone): use **`.bbmodel`**, then convert to GeckoLib.
- **RD 3D animate:** animations from a prompt. The first one rigs the model automatically. They come along in the `.bbmodel` export.

Costs from RD's API page: about $0.25–3.00 per model, $0.20–1.50 per revision and $0.15–1.00 per animation. Its estimate endpoint is free.

## 3D workflow (GeckoLib parts and rigs)
The body is built in code from separate part models. Each part attaches to its parent by bone name, so a model only works if it keeps the **socket contract**. The art drop already meets it; a regenerated part must too.

**Automatic:** `python3 tools/art/rd_generate.py --promote <asset>` does steps 2–4 and 6 for a part. It keeps our bones and positions, puts RD's boxes into the part's frame, scales the model to the old one's size, bakes rotated face UVs, and writes the model and texture with an empty accent mask. Details are in `tools/art/README.md`. It can also cut sections out of one whole model (`--from-group`, `--scale-like body/ship`), and `--promote-as entity/NAME` turns a whole model and its animations into a standalone GeckoLib model. The manual steps below are for body animations, body rigs and anything the tool can't map.

**By hand in Blockbench:**
1. **Generate** in RD 3D, blocky style, with the row's prompt. Up to 4 reference images help: the current item icon, or the old model's turntable.
2. **Export `.bbmodel`** and open it in Blockbench. Convert it to a GeckoLib model with the GeckoLib plugin.
3. **Rename bones** to the row's "Bones to keep" list. Line up their pivots with the old model's. Easiest: open the old `.geo.json` beside the new one and copy the pivots.
   - The pattern is `<part>` (root), `<part>_casing`, `<part>_frame`, `cog_internal`, `cap_<socket>` and `mount_<kind>_<n>`.
   - Whole rigs name their sockets after the chassis JSON (`hull`, `rotor_front_left`, ...).
4. **Texture size:** keep 64x64 (128x128 for the Ship and ship parts), or tell me the new size.
   - RD's `size` setting (16–256 texels) controls how fine the texture is. Aim for Minecraft's density of 16 texels per block.
   - Then paint the `_accent` mask on top (white = tinted with the player's accent color).
5. **Animations** keep their exact names: `animation.void_reclaimer.<chassis>.<idle|thrust|brake|low_power|module_work|rotor_spin>`.
   - RD rigs its own bones, so an RD animation has to be retargeted to our bones in Blockbench.
   - For the existing rigs it is often quicker to keyframe there directly. For new models (Mass Driver, drop pod, helper drone) RD's rig can simply become the rig.
6. **Export** `.geo.json` and `.animation.json` to the paths in the row. Same names = no code change.

**Java block models** (the RD Java export) have Minecraft's limits:
- each box rotates on only one axis, in 22.5° steps;
- the model must fit within -16 to 32 pixels (3 blocks).

RD's blocky style "tilts where needed", so check tilted boxes in Blockbench; a block that needs free angles should be a GeckoLib block entity instead. Kinetic blocks (Space Thruster, Electrolyzer, Spinner) leave a hole for the shaft, because Create draws the turning shaft itself.

## How to use it
**Automatically (recommended):** `tools/art/rd_generate.py` sends the rows to Retro Diffusion's API, so no prompt is typed by hand. Setup and options are in `tools/art/README.md`. In short:
1. `export RD_API_KEY=rdpk-...` (your key; it is never written to a file).
2. `python3 tools/art/rd_generate.py --status placeholder --dry-run` shows the jobs and an offline price. Without `--dry-run` it prices them with RD's free estimates, checks your balance and asks before spending.
3. Results land in `art/rd/` (not committed): 4 variants per image, 1 model per 3D asset, and a contact sheet `art/rd/index.html`.
4. `python3 tools/art/rd_generate.py --promote quartz_wafer=2` copies a picked image into the mod (size-checked). `--promote core_mk1_model` converts a 3D model into the part's GeckoLib model (see the 3D workflow).
5. Tell me when a batch is promoted. I'll check it in game and update `PROGRESS.md`.

**By hand:** take the `prompt` column into RD's web app, in the mode the `method` column names, and save the result at exactly the `file` (or `model`) path and size.

Rows marked "code change needed" (GUI skins, the sun, dust meter icons, toast icon, logo) have no slot in the game yet. They need a small change from me before they show up. The same goes for new models from future phases: their blocks and entities don't exist yet.

## Style guide
The full guide is `docs/art/STYLE_GUIDE.md`: mood, rules, colour ramps, a locked palette and a prompt template per category, and custom RD styles. `rd_generate.py` applies it to every job. This section is the short version for prompting by hand.

- **Resolution (decided):** 16x16 for item icons and blocks, like vanilla and Create. HUD, GUI and sky sprites are larger (sizes per row). The energy bar must stay 90x9.
- **Look:** Create mod's industrial style: andesite grey, brass, copper, cogs, rivets. Cyan for the AI's glow (lights, cores, lenses).
- **Icons:** transparent background, light from the top-left, dark 1-pixel outline, at most about 16 colors.
- **Blocks:** tile seamlessly. The asteroid rocks cover huge surfaces, so they need extra care to avoid visible repeats.
- **Dust kind colors** (used for asteroids, raw materials and HUD; from the datapack):

| Kind | Color |
|---|---|
| Metallic | `#B8C2CC` |
| Silicate (Silicon) | `#D9B38C` |
| Ice | `#9FE2FF` |
| Carbon | `#6E6A80` |

- **Fluid colors** (the fluids tint vanilla water; the buckets should match): Purified Water `#A8DCFF`, Oxygen `#B4C8FF`, Hydrogen `#FFC8C8`, Nebulite Propellant `#FF9638`, Coolant `#78DCE6`, Liquid Silicon `#D0D8EE`.
- **Prompt prefixes** (the CSV's prompts already include them):
  - Icons: `Minecraft item icon, 16x16 pixel art, transparent background, light from top-left, dark 1px outline, Create mod industrial style;`
  - Tiling blocks: `seamless tileable 16x16 Minecraft block texture, pixel art, Create mod style;`
  - Single block faces: `16x16 Minecraft block face texture, pixel art, Create mod style, not tiled;`
- **Unfinished items:** the `incomplete_*` icons are the finished item with parts missing (Create's convention). Make them from the finished icon (image-to-image) so the pair matches.
- **Animated textures:** a vertical strip of 16x16 frames plus a `.png.mcmeta`. The Nanite Swarm already works this way (4 frames). RD's animation output is a sprite sheet: restack its frames into one column.

## About the art drop
Art drop 1 came from your private art repo (`cosmicvoiddweller/void-reclaimer-art`, branch `art/tier-c-pilot`). If you redo any of those files, change them there too, or the next drop will overwrite the new ones. The art repo's README defines the conventions the 3D workflow above relies on (bones, base and accent layers, animation names, axes). Its `socket_contract.json` is the reference for bone pivots. The `_accent` file is a mask that the game tints with the player's accent color.

## The list
