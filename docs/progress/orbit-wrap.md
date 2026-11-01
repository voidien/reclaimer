## Fix: the orbit camera spun near 180 degrees
- While the middle button was held, a yaw that wrapped from +179 to -179 left the previous yaw on the far side, so the blend between ticks spun nearly a whole circle. The held branch now keeps both on one side, as the release branch did. Test: `holdingTheCameraPastHalfWayRoundDoesNotSpinTheBlend`, which failed before the fix.
