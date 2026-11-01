# The original phases

The first plan. `plans.md` has the revised one.

Build order after Phase 4: 5a → 6 → 7 → 5b → 8 → 9 → 10.

1. **Deep Space & Spawn:** `deep_space` dimension (datapack JSON), forced spawn/respawn, no gravity, hunger, or fall damage.
2. **Zero-G Flight:** WASD/Space/Shift thrust, momentum, dampening, mass-scaled handling, thrust payload with server validation.
3. **Cosmic Dust:** noise sampler, invisible clouds, drag, dust accumulation, basic dust meter, Dust Sensor HUD module.
4. **Replicator:** keybind GUI, custom recipe type with datagen, JEI category.
5. **Modular Body**, split in two:
   - **5a. Body data:** part/frame data model, size classes, `generic.scale`, body stats driving flight (`FlightStats`), storage capacity, and sensor/tool module slots.
   - **5b. Body rendering & assembly:** GeckoLib rendering replacing the vanilla player model (incl. first person), and the assembly UI.
6. **Asteroids:** floating rock/ice worldgen, drill-gated mining.
7. **Machines:** single-block processing machines and item transport between them.
8. **Stations & Helpers:** multiblock structures and helper drone entities that ferry items.
9. **Ruined Earth:** climate-disaster dimension and the Atmospheric Portal.
10. **Terraforming:** Terraformer blocks/arrays and biome restoration.

---

