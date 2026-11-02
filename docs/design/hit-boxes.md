# Hit boxes for big bodies

The owner's decision (2026-11-01): every big body, meaning the Ship, Shuttle and Titan, gets several small boxes along its model that turn with it. They hit blocks, ships and other bodies. A turn into something pushes the body away, and stops if it is still blocked. The view turns at once, and big bodies turn with weight. This replaces the rule against custom hit boxes for big bodies once built.

This is the plan from the old PC's reading of the code and the Sable 2.0.6 jar, checked by the spike below. Nothing is built, and the costs are estimates unless the spike measured them.

The owner's further answers: a portal takes a Shuttle, while Ships and Titans stay in orbit. Thrust follows where the body points. Helper drones steer around big bodies. Wanted with this: a test that boxes match the model, a collision event with speed and contact, bodies pushing ships they hit, and debris rocks on the same collision code.

**Built so far:** the data and the pure maths (`api.body.HitBox`, `Socket.boxes`, `body/BodyBoxes`) with the Ship, Shuttle and Titan lists. Nothing reads them yet, so a body flies as it did.

## How it works
- **Hook:** `ThrusterFlight.travel`, between `step` and `move`, calls `BodyBoxes.resolve(player, velocity)`. No mixin is needed, and it can't clash with Sable's `@Redirect` on `collide`. Collision is client side today: `LivingEntityTravelMixin` runs only for the local player, and the server's `handleMovePlayer` re-runs `move` with the vanilla box only. Creative vanilla flight stays boxless.
- **Own box test:** vanilla's `collideBoundingBox` sweeps axis-aligned boxes, so it can't handle boxes that turn. We write a pure box-against-box test of about 60 lines, unit-tested in `fastTest`. A world block is the same test with no pose, so blocks and ships share one path.
- **Ships:** `SubLevelEntityCollision.collide` reads the vanilla box and changes entity state (kicking, tracking sub-level, ground flags, step-up), so it can't be called per box. Leave it for the vanilla box. Use the public pieces: `SableCompanion.getAllIntersecting(level, bounds)`, `SubLevelAccess.logicalPose()`, `lastPose()`, `boundingBox()` and `Pose3d.transformPositionInverse`. Turn each box into the ship's plot frame, read blocks there, and turn the push back.
- **Other bodies:** `level.getEntities(player, union, Player or BodyHost)`, then box pairs after a quick union check. Each client moves itself out. A small body counts as one box, its vanilla box. `BodyHost` needs its pose read from its own yaw and pitch.
- **Heading:** nothing separates body from view today: `turnRate` only reaches UI, and `BodyLook.turn` uses the view. A new `BodyHeading` (yaw, pitch) is stepped at the top of `travel` toward the view, limited by the turn rate.
- **Turning:** build the boxes at the next heading and check overlaps. Sum the push vectors, capped at 0.25 blocks a tick to stay inside `checkSpeed`'s 1.5 tolerance, and apply them through `move`. Boxes on opposite sides cancel, which means wedged. If it is still overlapping, or wedged, keep the old heading and zero the turn. The view keeps going.
- **Never trap:** a box already overlapping at the start of a tick (a bigger chassis fitted in a tunnel, a spawn, a shell unfolding) only blocks motion that goes deeper, as vanilla does.
- **Server:** a heading payload while turning, clamped to the rate × 1.5 per tick and synced to trackers for rendering. Every 10 ticks it runs the same overlap test, and deep overlap held for 2 seconds teleports back, like `checkSpeed`.
- **Cost, estimated for a Titan:** 19 boxes. Skipping empty chunk sections makes open space about 100 cheap checks. Against a rock wall, perhaps 8 touching boxes × 200 solid blocks = 1,600 tests, about 0.3 to 0.5 ms a tick on the client. Two sub-steps at Shuttle speed double that.

## Boxes as data
On `Socket`, so a removed section loses its boxes, and the chassis is already synced and common:

```json
"boxes": [{"at": [0, 13.5, 14], "size": [14, 8, 14]}]
```

`at` is right, up, forward from the feet, and `size` is width, height, length. Store them at scale 1, multiplied by the size class scale, like `cockpit`. The tables below are at body scale, so divide by the scale for JSON. Optional, default none, with a row in `extension-points.md`. The model turns about the feet and then rises by `originHeight`, so `at` must include that: the Shuttle's up is 0.5 lower, as noted.

**Titan (19 boxes), scale 8, measured** (right is the pilot's: `_l` sockets are negative. Measured as GeckoLib draws the cubes, turned ones included.)

| Socket | at (right, up, forward) | size |
|---|---|---|
| saucer | 0, 13.5, 14 | 16, 8, 32 |
| saucer | ±12, 13.5, 14 (2) | 8, 8, 22 |
| bridge | 0, 18, 14 | 4, 1.2, 4 |
| neck | 0, 11, 0.8 | 3, 3, 10 |
| neck (hull `root`) | 0, 5.25, -17 / -7 / 1 (3) | 10, 10.5, 10 |
| bay_door | 0, 5.5, -23.5 | 6, 5, 3 |
| pylon_l, pylon_r | ±4.75, 10.9, -16 and ±9.25, 16.6, -16 (4) | 4.5, 5.7, 4 |
| nacelle_l, nacelle_r | ±11, 19, -26.5 / -17 / -7.5 (6) | 5, 5, 10 |

**Shuttle (14 boxes), scale 2, measured, then 0.5 lower for the pivot**

| Socket | at | size |
|---|---|---|
| body | 0, 2, 2.5 and 0, 2, -2.5 | 4.2, 3.9, 5 |
| nose | 0, 1.8, 6.4 | 3.6, 2, 2.8 |
| dome | 0, 4.25, 2.4 | 2, 0.7, 2 |
| aft | 0, 2.5, -5 | 5, 3.4, 2 |
| nacelle_l, nacelle_r | ±5, 1.25, -4.7 and -0.2 (4) | 1.6, 1.5, 4.6 |
| strut_l, strut_r | ±3.25, 1.75, -1.6 | 2.5, 0.5, 1.5 |
| strut_l2, strut_r2 | ±3.25, 1.75, -3.6 | 2.5, 0.5, 1.5 |
| ridge | 0, 4.12, -1.8 | 1.5, 0.5, 5 |

**Ship (29 boxes), scale 3, measured.** 21.4 long, 17.6 wide, 7.8 tall, from the rig's pivots and each section's model as they are drawn. `BodyBounds` and `/vrphoto handling` read 15.4, because a composed rig's box only reaches each section's pivot, so don't size anything from them. `up` is from the feet, with the rig's origin 2.7 above them. `wing_port` and every odd numbered pair member are on the pilot's left, and the keel is the rig's own frame, not `ship_keel`. The RCS clusters have no box: they sit inside the keel and bow.

| Socket | at | size |
|---|---|---|
| keel | 0, 2.815, -2.44 | 3, 1.35, 10.88 |
| bow | 0, 2.915, 7.97 | 3.56, 1.75, 9.94 |
| bridge | 0, 5.42, 1.66 | 1.68, 3.94, 2.68 |
| hull_1 to 6 | ±1.265, 2.69, 2.25 / 0.75 / -0.75 | 1.41, 1.1, 1.5 |
| wing_port, wing_starboard | ±5.365, 2.69, -2.865 | 6.89, 0.54, 7.39 |
| nacelle_1 to 4 | ±1.5, 3.265 and 2.135, -5.065 | 1.34, 1.05, 6.75 |
| hardpoint_1 to 6 | ±2.3, 2.325, 2.25 / 0.75 / -0.75 | 1.22, 0.37, 0.38 |
| cargo_1 to 4 | ±0.75, 1.555, 0.77 and -0.35 | 1.12, 0.89, 1.08 |
| mast_port, mast_starboard | ±1.125, 3.97, 0 | 0.57, 1.78, 0.56 |
| dorsal_spine | 0, 4.53, -2.435 | 0.56, 2.9, 5.63 |
| ventral_fin | 0, 0.97, -3.095 | 0.56, 2.72, 3.57 |

## PRs, each working alone
0. **Spike, done 2026-11-02, code not kept.** A client command probed a grid of points round the player through `SableCompanion.INSTANCE.getAllIntersecting` and `logicalPose().transformPositionInverse`, then read `level.getBlockState` at the plot position, against a real 5 x 5 ship made with `/sable assemble connected`:
   - No ship before assembly, one after. All 25 blocks read on a level ship, and again after a restart. On a ship tilted with `/sable teleport @e x y z 45 30`, 24 of 25 were hit by the world-aligned sample grid, which is the grid landing unevenly on a tilted plate.
   - About 0.2 µs a point at 15,625 points (2.5 to 3.5 ms) and 0.07 µs at 274,625 (18 ms). That covers looking up the block, so the estimate of 1,600 tests a tick stands.
   - The Ship is about 21 long and 18 wide on the 16-block floor grid, which matches the rig pivots. `BodyBounds` and `/vrphoto handling` read 15.4 for it, because a composed rig's box only reaches each section's pivot plus a block.
   - Not answered: whether a body resting on a deck jitters (`trackingSubLevel`). That needs real collision, so it falls to PR 4.
1. **Data:** `Socket.boxes`, the built-in lists, a pure `BodyBoxes` pose, a common copy of the turn maths, and unit tests. No behaviour change.
2. **Debug view (done, `debug/BoxOverlay`):** in the `overlay` group (`O`, `/vrphoto overlay`). `DebugShapes.box` takes eight corners, so turned boxes draw correctly. Green free, red touching, plus a heading line. Update `docs/debug-commands.md`. It comes early because it proves the data before anything can block a player.
3. **Blocks:** `resolve` in `travel`, with sub-steps of at most 0.5 block and the never-trap rule.
4. **Ships.**
5. **Bodies.**
6. **Heading:** turning with weight, the renderer and sync. The Drone stays 1:1.
7. **Turn test, push and stop.**
8. **Server checks.**

## Not confirmed
What `OrientedBoundingBox3d.sat` returns, jitter on a deck, `MergedMassTracker`, how `ShipOverlay` is wired in, and timings for the full collision pass.
