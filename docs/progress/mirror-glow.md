## Fix: mirrored parts lost their accent glow
- A mirrored part is drawn with a negative scale, which turns its faces inside out, and the accent pass culls: so one wing, and every even hull and hardpoint, had no glow. `BodyRenderer` now reverses the corners of a mirrored part's faces. The wings keep their accent edge, on both sides, by the owner's choice.
