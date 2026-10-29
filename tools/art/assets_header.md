# Art asset list

Everything the mod draws, as of `main` on 2026-10-29 (after Phase 8 and art drop 1), plus what the plan still needs. The list is for generating art with Retro Diffusion (RD); `assets.csv` next to this file has every row with its full prompt, ready for batch work.

## At a glance
| What | Count | Retro Diffusion? |
|---|---|---|
| Placeholder textures (programmer art, made by us) | 51 | Yes: 43 item icons + 8 block textures |
| Art drop 1 textures (redo if you want one consistent style) | 129 | Icons, block faces and HUD: yes. The 80 body skins: Blockbench |
| Module models missing for the body (5b) | 9 | No: Blockbench (RD for concept art) |
| New optional HUD/GUI/sky sprites (need a small code change to show) | 7 | Yes |
| Future-phase assets (Phases 7b, 9, 11–14, later tracks) | 20 | Textures yes, models Blockbench |

**What RD can and can't do here:** RD makes flat pixel images. Item icons, tileable block textures, block faces, HUD/GUI sprites and sky images are a good fit. 3D shapes are not: the GeckoLib rigs (`geo/*.geo.json`), block models and their UV-mapped skins are made in Blockbench. RD can still give concept art and material swatches for them.

## How to use it
1. Pick rows in `assets.csv` (filter by `status` and `method`).
2. Generate with the `prompt` column. For icons use RD's item style, for block textures its tiling option (best at 16x16 or 32x32), at the `size` shown.
3. Save as PNG at exactly the `file` path, at exactly the `size`. Same file name = no code change: the game picks it up.
4. Tell me when a batch is in: I'll check sizes and transparency in game, and drop the files from the placeholder list in `PROGRESS.md`.

Rows marked "code change needed" (GUI skins, the sun, dust meter icons, toast icon, logo) have no slot in the game yet. They need a small change from me before they show up.

## Style guide
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
- **Animated textures:** a vertical strip of 16x16 frames plus a `.png.mcmeta`. The Nanite Swarm already works this way (4 frames).

## About the art drop
Art drop 1 came from your private art repo (`cosmicvoiddweller/void-reclaimer-art`, branch `art/tier-c-pilot`). If you redo any of those files, change them there too, or the next drop will overwrite the new ones. The 80 body skins are painted to their rigs' UV layouts: open the `.geo.json` in Blockbench and paint over the template. The art repo's README defines the base and accent layers: the `_accent` file is a mask that the game tints with the player's accent color.

## The list
