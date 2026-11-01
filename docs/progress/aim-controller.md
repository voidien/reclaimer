## Mouse steering's maths
- `ship/AimController`, pure and not wired to the Helm yet: `Direct` (Space Engineers' rate steering, the owner's default) and `Aimed` (the nose swings to the look, with inertia) behind one interface, so both can be compared. Both give a torque for `ThrustAllocator`, limited by what the ship can stop at 20 ticks a second. `Authority.of` reads the thrusters and the inverse inertia.
- Tests: `AimControllerTest`, including a ship in free space where a request acts a tick late, so the aim is checked for overshoot across strengths from 0.2 to 400 radians per second squared.

## Ideas for the owner
- **Steering feel on screen:** a debug readout of the wanted turn, the real spin and the thrusters' authority per axis would make tuning `maxRate`, `stopSeconds` and `settle` quick once the Helm uses this.
