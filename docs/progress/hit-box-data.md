## Hit box data
- `Socket.boxes` is optional data: a list of `at` and `size` at scale 1, so a section that comes off takes its boxes with it. The Ship has 29, the Titan 19 and the Shuttle 14, and the Drone and Mech stay their vanilla box.
- `body/BodyBoxes` is pure: where a body's boxes are for its feet, yaw, pitch and scale, a common copy of the model's turn (a test keeps the two the same), and a box-against-box test that gives the shortest way out. Nothing reads any of it yet, so nothing changes in play.
