## Rig audit
- `BodyRigBonesTest` checks every whole-model rig both ways: a bone with cubes is hidden by some socket or is one of the core's, and a section that can come off has something to hide. A bone in neither fails, and so does a known gap that has been fixed but not taken off the test's list.

## Ideas for the owner
- **What the audit found, not fixed:** the Drone's six removable sections (dome, four rotors, claw) change nothing on its model, because it has one model and no socket bones. The Mech's pelvis stays when taken off: `"pelvis": ["root"]` under `socket_parts` would hide it, since `root` carries the chest. The Titan's drive hull (`root`) stays when the neck comes off, which also matters for saucer separation.
