# Hit boxes for big bodies

The owner's decision (2026-11-01): every big body, meaning the Ship, Shuttle and Titan, gets several small boxes along its model that turn with it. They hit blocks, ships and other bodies. A turn into something pushes the body away, and stops if it is still blocked. The view turns at once, and big bodies turn with weight. This replaces the rule against custom hit boxes for big bodies once built.

This is the plan from the old PC's reading of the code and the Sable 2.0.6 jar, checked by the spike below. Nothing is built, and the costs are estimates unless the spike measured them.

The owner's further answers: a portal takes a Shuttle, while Ships and Titans stay in orbit. Thrust follows where the body points. Helper drones steer around big bodies. Wanted with this: a test that boxes match the model, a collision event with speed and contact, bodies pushing ships they hit, and debris rocks on the same collision code.

**Built so far:** the data and the pure maths (`api.body.HitBox`, `Socket.boxes`, `body/BodyBoxes`) with the Ship, Shuttle and Titan lists, and `BodyBoxes.resolve`, the move among blocks. Flight does not read any of it yet, so a body flies as it did.

## How it works
- **Hook:** `ThrusterFlight.travel`, between `step` and `move`, calls `BodyBoxes.resolve(body, blocks, velocity)`, with every block within the length of the velocity of the boxes. No mixin is needed, and it can't clash with Sable's `@Redirect` on `collide`. Collision is client side today: `LivingEntityTravelMixin` runs only for the local player, and the server's `handleMovePlayer` re-runs `move` with the vanilla box only. Creative vanilla flight stays boxless.
- **Own box test:** vanilla's `collideBoundingBox` sweeps axis-aligned boxes, so it can't handle boxes that turn. We write a pure box-against-box test of about 60 lines, unit-tested in `fastTest`. A world block is the same test with no pose, so blocks and ships share one path. `resolve` sweeps each pair: it works out when the gap on each axis closes, so the contact is exact, the axis that closed last is the one the move loses, and a thin block can't be stepped over. Each step is swept, so the 0.5 block limit on a step isn't what stops a fast body. Against a wall it costs under 10%, and no test can tell it is there.
- **Ships:** `SubLevelEntityCollision.collide` reads the vanilla box and changes entity state (kicking, tracking sub-level, ground flags, step-up), so it can't be called per box. Leave it for the vanilla box. Use the public pieces: `SableCompanion.getAllIntersecting(level, bounds)`, `SubLevelAccess.logicalPose()`, `lastPose()`, `boundingBox()` and `Pose3d.transformPositionInverse`. Turn each box into the ship's plot frame, read blocks there, and turn the push back.
- **Other bodies:** `level.getEntities(player, union, Player or BodyHost)`, then box pairs after a quick union check. Each client moves itself out. A small body counts as one box, its vanilla box. `BodyHost` needs its pose read from its own yaw and pitch.
- **Heading:** nothing separates body from view yet: `BodyLook.turn` and thrust use the view. `body/BodyHeading` (yaw, pitch) is built and steps toward the view at the top of `travel`, the short way round, with yaw and pitch arriving together. `degreesPerTick` is `turnRate / mass × 2400` degrees a second, held between 6 and 120. `turnRate` is already on the parts, and mass is the owner's weight: a basic Ship turns at 64, a basic Shuttle at 62 and a basic Titan at 24, so half a turn takes 2.8, 2.9 and 7.4 seconds. A heavier fit turns slower, a worn wing or RCS cluster turns slower, and a body with no turn parts still turns at 6, or it could only ever face one way. Each tick goes at most 15% of what is left, the Helm aim's 3 a second, so a turn eases in over its last few degrees. A turn starts at full rate: there is no spin-up, because that needs a third number to sync. Size classes below the Ship's rank, the Drone and Mech, have no limit: their heading is exactly the view.
- **Turning:** `BodyHeading.turn` builds the boxes at the next heading and asks `BodyBoxes.turnPush`. The pushes are taken one at a time, deepest first, each from where the last left the body, up to four of them and 0.25 blocks a tick in all, to stay inside `checkSpeed`'s 1.5 tolerance. They aren't added up at once, because that counts one wall once for every block of it. A push more than 120 degrees from the pushes so far means wedged. If the whole step can't clear, half of it is tried, then a quarter and an eighth, because a long body's nose swings much further than it turns: a basic Titan's moves about 0.7 blocks a tick at full rate, so without this it would stop dead at a wall and never push off it. If no share fits, the heading stays and the view keeps going. The push is a move, not a speed, so it must not reach `deltaMovement`.
- **Never trap:** a box that starts a tick inside something (a bigger chassis fitted in a tunnel, a spawn, a shell unfolding) can go no deeper into anything than it started. The depth is per box, not per block: a per-block rule traps a body too big for a tunnel, because the next wall block ahead wasn't overlapping at the start. A hair of overlap from rounding gives only a hair of room, so it opens no way through a wall. Going that deep into something new is allowed, which is how a body leaves the tunnel at all. A box buried in solid rock can't cross the block grid along its thinnest axis, because its depth rises and falls with the grid there, but it can leave along the others. A body with buried boxes thin on all three axes can stick, and step 8's teleport covers that. Until step 6, the view turns the body at once, so turning into a wall gives a box as much room as it turned in.
- **Server:** a heading payload while turning, clamped to the rate × 1.5 per tick and synced to trackers for rendering. Every 10 ticks it runs the same overlap test, and deep overlap held for 2 seconds teleports back, like `checkSpeed`.
- **Cost, estimated for a Titan:** 19 boxes. Skipping empty chunk sections makes open space about 100 cheap checks. Against a rock wall, perhaps 8 touching boxes × 200 solid blocks = 1,600 tests, about 0.3 to 0.5 ms a tick on the client. **Measured for `resolve`:** a Titan against a wall of 9,963 blocks takes 0.8 to 1.6 ms a tick, turned or not, at speeds up to the Helm cap, and 0.03 ms with 300 blocks out of reach. That was on the old PC, in a unit test, and doesn't include reading the blocks. A turn takes 1.4 ms with the nose on the same wall, and 3.7 ms wedged between two walls of 6,700 blocks, where every share is tried. It runs only while the heading is off the view.

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
   - The Ship is about 21 long and 18 wide on the 16-block floor grid, which matches the rig pivots. `BodyBounds` read it short then; it now measures each section's model, and reads 21.4.
   - Not answered: whether a body resting on a deck jitters (`trackingSubLevel`). That needs real collision, so it falls to PR 4.
1. **Data:** `Socket.boxes`, the built-in lists, a pure `BodyBoxes` pose, a common copy of the turn maths, and unit tests. No behaviour change.
2. **Debug view (done, `debug/BoxOverlay`):** in the `overlay` group (`O`, `/vrphoto overlay`). `DebugShapes.box` takes eight corners, so turned boxes draw correctly. Green free, red touching, plus a heading line. Update `docs/debug-commands.md`. It comes early because it proves the data before anything can block a player.
3. **Blocks (done, `body/BoxCollision`):** `BodyBoxes.resolve` as pure logic, with sub-steps of at most 0.5 block and the never-trap rule (done). Next, reading the blocks and calling it in `travel`, with the client checks.
4. **Ships.**
5. **Bodies.**
6. **Heading:** `BodyHeading` (pure, done). Next, the renderer and sync. The Drone stays 1:1.
7. **Turn test, push and stop:** `BodyBoxes.turnPush` and `BodyHeading.turn` (pure, done). Next, calling them in `travel`, with thrust following the heading.
8. **Server checks.**

## For later
- **If collision costs too much:** merge the Ship's tiny boxes first. 10 of its 29 are hardpoints and cargo bays, at the cost of a removed part keeping its box. Measure with real timings before doing this.
- **Titan separation:** boxes follow sockets, so they split cleanly, except the hull's three, which live on `neck` because the rig's `root` does. The drive half needs those to go with it.

## Not confirmed
What `OrientedBoundingBox3d.sat` returns, jitter on a deck, `MergedMassTracker`, how `ShipOverlay` is wired in, and timings for the full collision pass.
