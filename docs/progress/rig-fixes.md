## Fix: the Mech pelvis and Titan drive hull come off the model
- Both stayed on the model after their section was removed, as the rig audit found. Their `root` bones are now listed under `socket_parts`. Seen on a Mech with no pelvis.
- `socket_parts` promised to hide a bone without its children, but GeckoLib's `setHidden` hides them too, so `root` would have taken the whole body with it. `BodyLook.hideAlone` makes it true. This also fixes a Titan with no neck, which lost its saucer. Test: `aBoneHiddenAloneKeepsItsChildren`.
