## Debug tool: a body's handling in numbers
- `/vrphoto handling [all]` prints a body's mass, top speed, lengths a second, acceleration, braking, time to top speed and stopping distance, for yours or every built-in body (`debug/HandlingReadout`). From the flight research. It is a client command, not `/void_reclaimer debug handling` as first named, because a body's length is only known to the model. Test: `HandlingReadoutTest`.
- Fixed the Shuttle's comment in `BuiltinBodies`, which said scale 3.
