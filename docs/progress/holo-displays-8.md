## Holo-displays, part 8: the map: done
All faults caught: `holo.map`, `setMap`, re-sending an unchanged picture and the picture going into the save, in GameTests; 7 more in unit tests.

`/void_reclaimer holo <pos> map`, CC `setMap`, CogScript `holo.map`. With this every part of the plan is built. Tests: `HoloMapImageTest`, `HoloMapsTest`, `aMapShowsTheBlocksAroundItAndOnlyResyncsOnAChange`, `theMapGoesWithTheBlocksUpdateNotItsSave`, and the mode in `theHoloProjectorIsAPeripheral` and `cogScriptWritesToTheProjector`.

**Seen in a client:** a map of the arena scene: north up, east right, a red bar in the right corner, and one white dot and two cyan ones for the player and the two Remote Shells there. **Not seen:** the dust tint in a real cloud, a ship's amber dot, a map on a ship, more than 16 maps at once, a map on ruined Earth.

**Placeholders:** the dot colours and size, the plain map colours with no cyan wash.

## Decisions pending (for the owner)
- **Map:** the radius of 64 and a new picture every 5 seconds; it shows each column's highest block, so a roof hides what is under it and a far rock above a station hides the station; only what is already loaded is drawn; a map on a ship is its deck plan with no dust or dots; a 17th map in view waits for a free texture; which entities get a dot.
