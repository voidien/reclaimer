# Art style guide

How Void Reclaimer's art looks, for people and for Retro Diffusion. Generated from `docs/art/style.json` by `tools/art/style_guide.py`: edit the JSON, then rerun the script. `tools/art/rd_generate.py` applies the same file to every job (see "How the tool applies it").

## Mood
Industrial sci-fi: Create's andesite, brass and copper machinery, made colder and spacier. Dark metals, rivets, cogs and shafts, with the AI's cyan glow as the one bright accent. Clean, readable pixel art at Minecraft's 16x16 scale.

## Rules for everything
- Light comes from the top-left. Shade with 3 to 5 steps of one ramp; no gradients, no soft airbrush, no dithering on items.
- Readable at 16x16: one clear silhouette, at most two materials per object, details at least 1 pixel wide.
- Metal is andesite grey (structure) or dark space iron (heavy plating); brass and copper are trims, bands, rivets and moving parts, not whole objects.
- Cyan is the AI's colour: lights, cores, lenses, screens and energy. It glows: a bright core pixel with one darker ring, never more.
- Dust kinds keep their colours everywhere: Metallic silver-grey, Silicon sand-tan, Ice pale cyan, Carbon violet-grey.
- No text, letters, numbers, logos, faces or creatures on any texture.
- No pure black outlines inside a texture; outlines use the darkest step of the object's own ramp.
- Space is dark: textures are a little darker and cooler than vanilla Minecraft, so the cyan glow and brass catch the eye.

## Colour ramps
Each ramp runs dark to light. Shade an object within one ramp.

| Ramp | Colours |
|---|---|
| andesite | `#1E2124` `#2E3236` `#43484D` `#5B6166` `#767C80` `#959A9C` `#B6BAB9` |
| space_iron | `#0E0F13` `#16181E` `#20232B` `#2C3039` `#3A3F4A` |
| brass | `#4A3214` `#6E4B1C` `#96682A` `#BF8B3A` `#E0B054` `#F5D88A` |
| copper | `#3B1E14` `#5E2E1D` `#8A4428` `#B65E36` `#D98250` `#F0A97A` |
| ai_cyan | `#0A2E3D` `#10536B` `#1A86A6` `#2FC0E0` `#8BE8F7` `#E6FBFF` |
| dust_metallic | `#3C444C` `#5F6A74` `#8A949D` `#B8C2CC` `#DCE3EA` |
| dust_silicon | `#5A4128` `#8A6740` `#B58C61` `#D9B38C` `#F0D8BC` |
| dust_ice | `#2A5D70` `#4C93AD` `#74C0DA` `#9FE2FF` `#D6F4FF` |
| dust_carbon | `#24222D` `#3A3746` `#534F62` `#6E6A80` `#948FA6` |
| fluids | `#A8DCFF` `#B4C8FF` `#FFC8C8` `#FF9638` `#78DCE6` `#D0D8EE` |
| status | `#C8363B` `#F2A33A` `#5BD16A` |
| light | `#F4F6F7` |
| sky | `#020306` `#05070D` `#0B0F1A` `#141B2C` |
| sun | `#FFF6D8` `#FFE29A` `#F7B955` |
| smog | `#2B1E16` `#4A3324` `#6E4A2E` `#97663C` `#C08A4E` `#7A7F4A` `#A8AE5C` |

## Categories
Every asset belongs to one category. Each category has its own locked palette: Retro Diffusion snaps every pixel to it.

### Item icons (`items`)
Everything in the inventory: tools and modules, body section icons, raw materials and intermediates, unfinished Sequenced Assembly items, buckets.

- 16x16, transparent background, the object centered with a 1-pixel margin.
- Raw materials are small heaps or shards in their dust colour; processed materials are clean plates, ingots, wafers and spools.
- Unfinished items are the finished icon with parts missing and the frame showing.
- Buckets keep vanilla's iron bucket; only the contents change.

- **Palette:** 60 colours from andesite, space_iron, brass, copper, ai_cyan, dust_metallic, dust_silicon, dust_ice, dust_carbon, fluids, status, light: `docs/art/palettes/items.png` (swatches: `items_preview.png`).
- **Prompt template:** `{prompt}, dark industrial sci-fi, andesite grey and dark iron with brass trim, cyan glow accents`
- **Reference images (for an RD custom style):** up to 4 in `docs/art/style/items/`

### Block textures (`blocks`)
Asteroid rock, machine faces, scaffold, and the textures of block models (Space Thruster, Helm, Mass Driver, Spire, Monolith).

- 16x16. Rock and other surfaces that cover large areas tile seamlessly and avoid any single feature that would repeat visibly.
- Asteroid rock is in its dust kind's ramp only, with a few darker pits; no ores (ore veins come later as separate blocks).
- Machine faces follow Create: an andesite or brass casing border 1-2 pixels wide, the working part inside, a round shaft hole where a shaft enters.
- Lower contrast than items, so large areas stay calm.

- **Palette:** 53 colours from andesite, space_iron, brass, copper, ai_cyan, dust_metallic, dust_silicon, dust_ice, dust_carbon, status: `docs/art/palettes/blocks.png` (swatches: `blocks_preview.png`).
- **Prompt template:** `{prompt}, Create mod style block, dark cool tones`
- **Reference images (for an RD custom style):** up to 4 in `docs/art/style/blocks/`

### The AI's body and other models (`body`)
GeckoLib body rigs, sections and modules made in RD 3D, plus entity models (drop pod, helper drone, Industrial Mech).

- Built from boxes only (RD Blocky or Blocky Lite).
- A part reads as one machine: a dark iron or andesite core, brass or copper joints and bands, cyan lights only where the AI senses or thinks (eyes, cores, sensors).
- Leave plain areas where the player's casing and accent colour will show; the accent mask is painted afterwards.
- Thrusters and moving joints show their mechanism: nozzles, pistons, cogs.

- **Palette:** 34 colours from andesite, space_iron, brass, copper, ai_cyan, status, light: `docs/art/palettes/body.png` (swatches: `body_preview.png`).
- **Prompt template:** `{prompt}, blocky Minecraft-style robot part, Create mod machinery, dark iron and andesite with brass joints, cyan lights`
- **Reference images (for an RD custom style):** up to 4 in `docs/art/style/body/`

### HUD and screens (`gui`)
The energy bar, Replicator panels and tabs, the module wheel, HUD icons, toasts, the mod logo.

- Dark translucent-looking space-iron panels with a thin andesite frame and brass corners.
- Cyan for active, selected and energy; amber for warnings; red only for empty or broken.
- Flat shading, crisp 1-pixel lines; panels are 9-slice friendly (plain middles, detail in the corners and edges).

- **Palette:** 48 colours from space_iron, andesite, brass, ai_cyan, status, light, dust_metallic, dust_silicon, dust_ice, dust_carbon: `docs/art/palettes/gui.png` (swatches: `gui_preview.png`).
- **Prompt template:** `{prompt}, sci-fi machine interface, dark panel, cyan highlights`
- **Reference images (for an RD custom style):** up to 4 in `docs/art/style/gui/`

### Sky and effects (`sky`)
The distant sun, Dyson swarm and shell sprites, power beams, launch streaks, the Atmospheric Portal surface, Ruined Earth's smoggy skies and acid rain.

- Deep space is near-black blue, never pure black, with a pale distant star.
- Energy (beams, portal, Dyson glints) uses the cyan ramp with brass glints; it is the brightest thing on screen.
- Ruined Earth uses the smog ramp: brown-orange haze and sickly yellow-green for acid.

- **Palette:** 27 colours from sky, sun, ai_cyan, brass, smog, light: `docs/art/palettes/sky.png` (swatches: `sky_preview.png`).
- **Prompt template:** `{prompt}, deep space, cold dark blues, glowing cyan energy`
- **Reference images (for an RD custom style):** up to 4 in `docs/art/style/sky/`

## How the tool applies it
`rd_generate.py` puts every row in a category: item icons in `items`, block textures and block models in `blocks`, body parts and entity models in `body`, HUD and screens in `gui`, sky and effects in `sky`. Then, on every job:
- **Palette:** the category's palette PNG is sent as `input_palette`, so every pixel uses its colours (2D images and 3D models alike).
- **Prompt:** the subject from `assets.csv` is wrapped in the category's prompt template.
- **Custom style:** once a category has reference images (1 to 4 in its folder above; PNG, JPEG or WebP) and you have run `python3 tools/art/rd_generate.py --setup-styles`, that category's 2D jobs use your RD custom style instead of the base style. A custom style is an RD Pro style ($0.18 an image instead of $0.06). It carries the reference images, the rules above as instructions, and the prompt template. Choosing references: generate a few assets, pick the ones that best show the look together, and copy them into the folder.
- `--no-style` turns all of this off for a run.
