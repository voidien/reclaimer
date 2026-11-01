## Tool: GameTest times
- `GameTestTimeMixin` logs each test's milliseconds and ticks, and `tools/gametest.sh` prints the five slowest. A batch lasts as long as its slowest test.
- `aDroneMinesTheRocksNearItsMinePointIntoItsChest` no longer sits out a fixed 600 ticks: it goes on when the rocks are in, then waits long enough for a drone that wrongly wanted the stone to take it. Mutation-checked. Its batch went from 35 s to 22 s.

## Tool: GameTests write no chunks
- Stack samples showed the server thread packing chunks for saving: about 10,700 deep space saves in 74 s of tests, each of 254 sections. GameTest runs now write no chunk (`GameTestChunkSaveMixin`) and end after the summary (`GameTestQuickExitMixin`), with the same exit code a stop gives.
- **First try, dropped:** keeping every chunk in memory passed locally and ran out of memory on CI's fresh world. Local runs read an old world; a fresh one generates a ring of half-made chunks round each test area, about 27,000 chunks by the third batch. Run on a fresh world before trusting a memory change: `rm -rf run/world`.
- **What it needs:** a fresh world passes 246 and 37 tests three times each at 6 GB; 4 GB stalls. CI's fast runs get 10 GB of its 16.
- **Where the checks moved:** real chunk saves, reloads and a real stop now run on every push to `main` (`-PgametestChunkSaves`) and in the restart tests, not on PRs or local runs.
- `darknessDrainsAndThrustCostsEnergy` built its box in a chunk with no ticket, and only survived an unload through the save. It forces the chunk now. Any test must hold a ticket on a chunk it needs.

## Ideas for the owner
- **Chunk saves in deep space:** one save costs about 5 ms on the server thread, because vanilla packs all 254 sections, nearly all empty. A player flying fast through deep space pays for that. It may be worth measuring in a real world and, if it shows up, skipping empty sections with a small mixin.
- **A lighter deep space:** each section of an empty chunk costs about 600 bytes, and with 254 of them a chunk costs about 150 KB before anything is in it. Tests pay this, and so does a player: a smaller height for deep space would cut both memory and saves, though it changes the dimension, so it is your call.
