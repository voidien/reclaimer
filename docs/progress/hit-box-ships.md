## Big bodies stop at ships
- Step 4 of `docs/design/hit-boxes.md`. A big body now hits ships as well as blocks, level or turned: each body box is carried into the ship's own coordinates to find its blocks, and each comes back as a box turned with the ship (`BoxCollision.ships`). The ship doesn't move yet when hit. Test: `aBigBodyStopsAtAShip`.
