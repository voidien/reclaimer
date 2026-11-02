## A body's heading and the turn test, as pure logic
- `BodyHeading` steps toward the view at `turnRate / mass × 2400` degrees a second, held between 6 and 120, and eases in over the last few degrees. A basic Ship turns at 64, a Shuttle at 62 and a Titan at 24. The Drone and Mech follow the view exactly. Flight and the renderer don't use it yet.
- `BodyBoxes.turnPush` and `BodyHeading.turn` decide whether a turn fits: no push, a push of up to 0.25 blocks a tick, part of the turn, or none. A body already in something can turn out of it, and can't turn deeper. `BodyHeadingTest` covers these, a wedged body, a corner, the seam at 180 degrees and random turns.
- The numbers are a starting guess for the owner to feel. Known limit: a turn starts at full rate, with no spin-up.
