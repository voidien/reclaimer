## Per-axis dampening
- With the dampener on, a held thrust key now also brakes whatever drift is off the thrust's direction, at the dampener's rate, and leaves the axis being thrust along to the thrust. Velocity follows the nose through a turn, as in Space Engineers. With no key held it brakes everything, as before; with the dampener off or no power nothing changes.
- A pair of keys counts as one direction, the diagonal between them, so there is no view frame in the rule: `FlightPhysics.trimDrift` works from the thrust alone. On ruined Earth a fall counts as drift while a key is held.
- Tests: six in `FlightPhysicsTest`, `aHeldKeyBrakesTheDriftOffTheNose` in `FlightGameTests`.

## Decisions pending (for the owner)
- **Per-axis dampening costs no extra energy:** the trim comes with a held key, which already pays 2 energy a second against braking's 1, so `ThrusterFlight.isBraking` and the dampener sound stay as they were: nothing is charged or heard for it.
