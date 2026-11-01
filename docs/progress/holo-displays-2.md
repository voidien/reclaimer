## Holo-displays, part 2: the readouts: done
Twelve new readout kinds on the registry from part 1: the helm, thrusters, the siphon, electrolyzer and spinner, the launch complex, the Dyson Sphere, the Spire, the Backup Core, the nearest body, the dust cloud and any tank or energy store beside the projector. `ReadoutsTest` checks the lines and that the biggest numbers still fit 48 characters; `HoloGameTests` checks finding the nearest source, moving on when it is removed, and the kinds that need no block.

## Decisions pending (for the owner)
- **Holo readouts:** the 8-block source range (`holo.sourceRange`); the nearest block wins, with no way to pin one; `device` checks only the six neighbours; `launch_complex` replaces the plan's `mass_driver`, because the old driver is inert; the lines are English from the server, not translated.
