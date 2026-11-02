## Helm ships have a speed cap
- 100 blocks a second by default, in config (`ship.helmSpeedCap`). At the cap the keys stop adding speed and can still steer, and a ship over it is held down by its own thrusters, dampener on or off (`ThrustAllocator.underCap`, `overCap`). A ship on redstone has no cap, and a ship with no thrusters to slow it stays over it. Test: `aHelmHoldsItsShipToTheSpeedCap`.
