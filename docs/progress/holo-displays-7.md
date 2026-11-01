## Holo-displays, part 7: the ship miniature: done
`/void_reclaimer holo <pos> ship`, CC `setShip`, CogScript `holo.ship`. Tests: `HoloMeshTest`, `HoloShipsTest`, `aShipMiniatureIsSavedAndTheLastWriterWins`, and the mode in `theHoloProjectorIsAPeripheral` and `cogScriptWritesToTheProjector`.

**Seen in a client:** a projector next to the `station` test scene showed the ship turning in cyan, 47 blocks as 488 vertices, with the core standing on its deck, and looked at every 2 seconds without meshing again. That look is what found the only bug here: a first check that overflowed a `long` and never ran, which no test on the server could catch. **Not seen:** a ship that changes while it is shown, a ship in flight, a ship over 4096 blocks, a projector standing on the ship it shows.

## Decisions pending (for the owner)
- **Ship miniature:** the projector shows its own ship or the nearest within 64 blocks, with no way to pick another; a ship over 4096 blocks shows only the blocks copied first, not a scaled-down whole; Create's moving parts are left out, so a ship made mostly of them looks thin.
