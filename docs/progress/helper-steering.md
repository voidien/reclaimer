## Helper drones, part 4: steering round blocks: done
- Drones no longer fly through blocks. Each tick a drone looks only at the cells its next step enters; when one is solid, a bounded search gives it a way round, straightened into a few waypoints. A long way that does not fit one search is walked in stages, each ending closer to the target. With no way, the drone holds still, its status says "Blocked" and it looks again every 100 ticks; opening a way sets it going, with its cargo and phase intact.
- A drone hovers on a block's first free side (top, four sides, under), so a chest under a roof or the underside of a rock is reachable. A drone only counts as there with a clear line to that spot. Miners choose only rock with an open face. A drone with a block set on it leaves straight towards its target.
- Photo mode's overlay draws the detour and says "blocked".
- Nothing new is saved: the route is worked out again after a load, so a drone from an older save loads as it was.
- **Tests:** `HelperSteeringTest` (unit, 20); `HelperSteeringGameTests` (6: round a wall, no way then a way opens, a block set on a drone, a chest under a roof, solid cells and unloaded chunks, a buried rock skipped).
- **Known limits:** drones do not see Sable ships, whose blocks live in their plots; a drone is a point to the steering, so its model can graze a corner (not seen in a client); a wall much wider than a search's reach with the target right behind it counts as no way (raise `helper.searchNodes`).

### Not seen in a client yet
Drones flying a detour, and the overlay's detour lines.

## Decisions pending (for the owner)
- **Helper steering:** a search looks at up to 2048 cells (`helper.searchNodes`) and a drone with no way looks again every 100 ticks (`helper.blockedRetry`); a block with every face closed counts as out of reach; a drone does not work through a wall ("Helper drones, part 4").
