## Test: every rig faces where it looks
- `BodyRigNoseTest` reads the front and back of all five rigs from the models themselves and checks the nose against `BodyLook.turn`, so a rig built backwards fails here rather than in flight. From the flight research; the owner chose to add it. Caught flipping the Titan's yaw.
