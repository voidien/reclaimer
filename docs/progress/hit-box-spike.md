## Spike: the client can read a turned ship
- The riskiest part of the hit box plan works: the client finds a ship through Sable's public API and reads its blocks through its pose, level or tilted, at about 0.2 µs a point. The results are in `docs/design/hit-boxes.md`, and the code was thrown away as planned.
- **Known issue:** `/vrphoto handling` reads a Ship as 15.4 long when it is about 21, because `BodyBounds` measures a composed rig to each section's pivot.
