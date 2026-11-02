## Big bodies stop at walls
- Step 3 of `docs/design/hit-boxes.md`: flight now uses a big body's hit boxes against blocks (`body/BoxCollision`, `BodyBoxes.resolve`), so a Ship, Shuttle or Titan stops where its model meets a wall and slides along it. Small bodies keep their vanilla box. Ships and other bodies don't collide yet. The overlay shows how many blocks the last check read and how long it took. Test: `aBigBodyStopsAtAWallItsVanillaBoxWouldMiss`.
- **Known issue until steps 6 and 7:** the view turns a big body at once, so turning can still swing its nose into rock.
