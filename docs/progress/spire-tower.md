## Spire, part 2: the tower: done
- **The old block grew up, it was not replaced.** `terraforming_spire` keeps its id, item, recipe, block entity id and CC type, and is now the tower's controller: `SpireBlock` is a `HorizontalKineticBlock` that faces its tower, with a shaft on its back. A world with an old Spire loses nothing: the block loads facing north with no tower, keeps its pulse count, and a pulse caught mid-sweep runs out from the block itself (`pulseCentre` is absent in the old save, so it stays the block). It only stops firing until a tower stands around it.
- `SpireTower` (pure): a 5 x 5 base in front of the Spire, which sits in the middle of the near edge; the frame is the base's outer row without the Spire plus four corner pillars 9 tall, 47 blocks; the column is the 3 x 3 inside, 9 tall. Cells are (across, forward, up).
- `SpireAssembly`, like `LaunchComplex`: sneak-use (or CC `assemble()`) needs every frame cell in the new tag `#void_reclaimer:spire_frame` (by default everything in `#void_reclaimer:launch_frame`) and a free column, fills the column with invisible solid `spire_column` blocks (no item, no drop); setting down removes them. Every 20 ticks a broken frame sets the tower down and a missing column block grows back. A pulse already running finishes whatever happens to the tower.
- The pulse now sweeps out from the tower's centre; `tryStart` refuses with `NOT_ASSEMBLED`.
- `client/render/SpireTowerRenderer`: a global GeckoLib block entity renderer, drawn while assembled, seen up to 256 blocks away. A wrench does nothing to an assembled Spire: the frame is found from its facing.
- CC: `isAssembled`, `assemble`, `disassemble`, `getSpeed`; `pulse()` gained `"not_assembled"`.
- Tests: `SpireTowerTest` (3), `SpireTowerGameTests` (11: assembling, gaps and obstacles, the tag from the GameTest pack, a broken frame, breaking the Spire, sneak-use, the wrench, the client packet and save, refusing without a tower, an old save with a pulse mid-sweep), `aTowersPulseSweepsOutFromItsCentre`, `theSpireIsAPeripheralAndPulsesUnderThePlayersRules`. Mutation-checked: a frame check that takes any block, a column that does not grow back and a wrench that ignores the tower each fail a test.

### Placeholder assets
- `geo/entity/spire_tower.geo.json`, `textures/entity/spire_tower.png`, `animations/entity/spire_tower.animation.json` (empty): stacked boxes about 24 blocks tall, flat colours. Replace with `--promote-as entity/spire_tower`.
- The Spire block still wears `textures/block/terraforming_spire.png` on all faces, so its shaft face does not show.

### Not seen in a client yet
The tower model's place and height over the frame.

## Decisions pending (for the owner)
- **Spire tower:** the old block becomes the controller rather than an inert leftover (old Spires face north, so a player may need to rebuild around or re-place one); the 5 x 5 x 9 shape with four pillars and no top ring; the default frame set (the launch frame's); the 256-block view distance ("Spire, part 2").
