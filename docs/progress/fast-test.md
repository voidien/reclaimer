## Tool: unit tests without booting the game
- `./gradlew fastTest` runs 336 of the 369 unit tests, in 62 of 67 classes, without the FML launcher. `test` is unchanged, in `build` and on CI; five classes that need the game are excluded from `fastTest` only.
