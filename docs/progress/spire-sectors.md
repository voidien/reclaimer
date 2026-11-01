## Spire, part 3: sectors and the sky: done
- `earth/Sectors`, `earth/SectorMap` (saved, synced attachment `earth_sectors` on the Earth level), `earth/EarthRestoration` (the one writer; moves the sky era on and posts the new `SectorRestoredEvent`).
- A tower's pulse sweeps out from the tower, stays inside the sector holding the tower's centre, and marks it when it ends, so the pulse covers the whole sector exactly wherever in it the tower stands. `Sectors.reach` sets the radius; the config `spireRadius` is gone. The biome rewrite takes a square now. `tryStart` refuses with `OFF_PLANET` and `SECTOR_RESTORED`. A pulse from `start(radius, duration)`, as an old save's, names no sector: not clipped, restores nothing.
- `SkyLook.in(level)` is what the client draws: `blend(fraction)`, a straight line through ruined (0), healing (0.5), restored (1), so every sector moves the sky and thins the fog. Clouds appear halfway to healing. An era set by the debug command differs from `SkyEra.forSectors(count)`, so it shows as it is until the next restored sector.
- `/void_reclaimer sky sectors [count]`; the debug readout shows the count.
- CC: `getSector`, `getSectors`, `getRestoredCount`; `getRadius` is now the reach to the far edge; `pulse()` can answer `"off_planet"` and `"sector_restored"`.
- Tests: `SectorsTest`, `SectorMapTest`, `SkyLookBlendTest`, `SectorGameTests` (5). Mutation-checked: no clip of the wave, no clip of the biome, no check for a restored sector, no check for the planet, no restoring at the end, an era that does not follow, a sky that ignores the blend each fail a test.

### Not seen in a client yet
The in-between skies and fog.

## Decisions pending (for the owner)
- **Sectors:** a pulse rewrites exactly the sector the tower stands in, wherever in it, rather than a 128-block square around the tower; so the wave is lopsided from an off-centre tower, a pulse lasts the same whatever the reach, and `spireRadius` is gone. Sky and fog blend across the whole planet with the fraction restored, not per place. Ruined at 0, healing from 1 to 15, restored only at 16 ("Spire, part 3").
- **Sector numbers:** row and column from the north-west; the landing area at the origin is the corner of four sectors.
