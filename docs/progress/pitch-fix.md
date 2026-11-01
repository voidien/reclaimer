## Fix: Drone, Shuttle and Titan pitched backwards
- A rig built facing +Z was turned by its 180 degree yaw after the pitch, not before, so looking down pointed its nose up. The composed Ship faces the other way and was right. `BodyLook.turn` applies the rig's yaw first. Test: `BodyLookTurnTest`, which fails on the old order.
