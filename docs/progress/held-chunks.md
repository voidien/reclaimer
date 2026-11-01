## Tool: GameTest times
- `GameTestTimeMixin` logs each test's milliseconds and ticks, and `tools/gametest.sh` prints the five slowest. A batch lasts as long as its slowest test.
- `aDroneMinesTheRocksNearItsMinePointIntoItsChest` no longer sits out a fixed 600 ticks: it goes on when the rocks are in, then waits long enough for a drone that wrongly wanted the stone to take it. Mutation-checked. Its batch went from 35 s to 22 s.

## Tool: GameTests hold their chunks
- Stack samples showed the server thread saving chunks: about 10,700 deep space saves in 74 s of tests, each packing 254 sections. GameTest runs now keep chunks in memory and end after the summary, with the same exit code a stop gives.
- Main suite 110 s to 55 s, Aeronautics 52 s to 38 s, three runs each, all passing. The suite needs about 3 GB of heap; 2 GB runs out of memory.
- **Where the checks moved:** real unloads, chunk reloads and a real stop now run on every push to `main` (`-PgametestUnloads`) and in the restart tests, not on PRs or local runs. The two runs were compared: all 246 main tests pass both ways. A failing test still fails the run.
- The small-heap check for memory growth needs `-PgametestUnloads` now.

## Ideas for the owner
- **Chunk saves in deep space:** one save costs about 5 ms on the server thread, because vanilla packs all 254 sections, nearly all empty. A player flying fast through deep space pays for that. It may be worth measuring in a real world and, if it shows up, skipping empty sections with a small mixin.
