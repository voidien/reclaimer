## Holo-displays, part 8: the map: built, not landed
**Before this lands:** rebase nothing, merge `main` once part 7 is in, run `tools/verify.sh`. Not yet run: the GameTest mutations of `holo.map`, `setMap`, the repeat send of an unchanged picture and the picture left out of the save; the whole suite on this branch. Caught already: 3 of the map and ship mutations in GameTests, 7 in unit tests.

`/void_reclaimer holo <pos> map`, CC `setMap`, CogScript `holo.map`. With this every part of the plan is built. Tests: `HoloMapImageTest`, `HoloMapsTest`, `aMapShowsTheBlocksAroundItAndOnlyResyncsOnAChange`, `theMapGoesWithTheBlocksUpdateNotItsSave`, and the mode in `theHoloProjectorIsAPeripheral` and `cogScriptWritesToTheProjector`.

**Seen in a client:** a map of the arena scene: north up, east right, a red bar in the right corner, and one white dot and two cyan ones for the player and the two Remote Shells there. **Not seen:** the dust tint in a real cloud, a ship's amber dot, a map on a ship, more than 16 maps at once, a map on ruined Earth.

**Placeholders:** the dot colours and size, the plain map colours with no cyan wash.

## Decisions pending (for the owner)
- **Map:** the radius of 64 and a new picture every 5 seconds; it shows each column's highest block, so a roof hides what is under it and a far rock above a station hides the station; only what is already loaded is drawn; a map on a ship is its deck plan with no dust or dots; a 17th map in view waits for a free texture; which entities get a dot.
