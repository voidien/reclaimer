## Holo-displays, part 6: the Dyson and Earth globes: done
`/void_reclaimer holo <pos> globe dyson|earth`, CC `setGlobe`, CogScript `holo.globe`. The globes show live progress, through `HoloGlobes.Progress` in the block's packet. Tests: `GlobeTest`, `HoloGlobesTest`, `aGlobeFollowsTheSphereAndOnlyResyncsOnAChange`.

**Placeholders:** the globe colours in `GlobeLook`, with no `globe_*.png` textures: the sphere is plain coloured scanlines.

## Decisions pending (for the owner)
- **Globes:** one dot per satellite up to 200, the lattice shown from tier 3, a caption always on.
