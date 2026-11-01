## Spire, part 5: Acid Drizzle: done
- `earth/EarthAtmosphere` (pure) and `earth/AcidDrizzle`: once a second, a body under the open sky on ruined Earth outside a restored sector wears every section through `Wear.add`, scaled by what is left of the planet. Config `earth.acidDrizzle` (on) and `earth.acidWearPerMinute` (0.003). Smog came with part 3: it is the sky's fog.
- Client: `client/render/AcidDrizzleClient` and `AcidDropParticle`, a yellow-green falling drop (`registry/ModParticles`, `particles/acid_drop.json` over vanilla's splash sprites), thinner with strength and with the game's particle setting; sound event `acid_drizzle`, vanilla rain as a stand-in.
- Tests: `EarthAtmosphereTest`, `AcidDrizzleGameTests` (5: every section wears and none breaks, a roof, restored ground, the config, deep space). Mutation-checked: dropping the sky, config or dimension check, drizzle over restored ground and wearing only one section each fail a test.

### Placeholder assets
- The `acid_drop` particle uses vanilla's splash sprites, tinted. The `acid_drizzle` sound is vanilla rain.

### Not seen in a client yet
The drops, their density, the sound.

## Decisions pending (for the owner)
- **Acid Drizzle:** it falls all the time rather than in spells; 0.003 condition a minute at full strength, a quarter of thrust wear, so about 5.5 hours outdoors takes a new section to 0; it stops at once over restored sectors and thins with every sector elsewhere; only players' bodies wear, not remote shells or helper drones; a ship's hull over a pilot does not count as a roof when Sable has it in its plot, only world blocks do ("Spire, part 5").
