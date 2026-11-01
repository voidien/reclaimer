## Holo-displays, part 6: the Dyson and Earth globes: done
`/void_reclaimer holo <pos> globe dyson|earth`, CC `setGlobe`, CogScript `holo.globe`. The globes show live progress, through `HoloGlobes.Progress` in the block's packet. Tests: `GlobeTest`, `HoloGlobesTest`, `aGlobeFollowsTheSphereAndOnlyResyncsOnAChange`.

**The test that "never finished":** it was not the test. `aGlobeFollowsTheSphereAndOnlyResyncsOnAChange` takes 484 ms and 5 ticks alone on a fresh world and passes in the full suite. The stuck JVM was a full run out of heap on a fresh world, the known ring of half-made chunks (a live histogram: 14.5 million `PalettedContainer`s, about 28,500 deep space chunks, 5.3 of the 6 GB), and a checkout with no globe code ran out the same way after 74 tests. So the suite stopped before reaching the test.

**Seen in a client:** both globes on the arena scene, with 150 satellites, 900 trusses and 7 of 16 sectors: the star, the shell and lattice, the green and ruined sectors and the captions, which moved up after the first look showed the Earth's air over them.

**Placeholders:** the globe colours in `GlobeLook`, with no `globe_*.png` textures: the sphere is plain coloured scanlines.

## Decisions pending (for the owner)
- **Globes:** one dot per satellite up to 200, the lattice shown from tier 3, a caption always on.
