## Test: the picker's request from a seat on a ship
- A review suspected `PickShellPayload` would refuse a seat on a ship, because its block position is in the ship's plot. `aSeatAboardAShipHoldsItAndWakesYouByIt` now links through that payload, and it passes, so the server side is fine. The picker screen's own distance check was not tried on a ship in a client.
