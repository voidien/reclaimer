# Aeronautics spike, retest (2026-10-29)

Spike branch only; nothing here is merged. Sable 2.0.6 + Create Aeronautics 1.3.2 with Create 6.0.10,
Sodium, ModernFix, FerriteCore and ImmediatelyFast.

## Result
Sable works in `deep_space`. The first spike's "flung away / removed" platforms were simply falling:
the zero-gravity `dimension_physics` file was not in effect, so Sable's default gravity pulled them down
at up to ~110 blocks/s until they passed Y -10,000 and Sable removed them ("extreme Y coordinate range").
Platforms from that session were still falling in the test world (one at Y -14,037, -110 blocks/s).

With `data/void_reclaimer/dimension_physics/*.json` shipped in the mod (gravity 0, pressure 0, drag 0):

| Test | Where | Result |
|---|---|---|
| Platform at rest, 10 s | Overworld, deep_space, vanilla-height copy | Stays at exactly its spawn point in all three |
| Sable default gravity, 10 s | Same three | Falls smoothly and alike in all three (~430-510 blocks), no removal |
| Survival player standing on / inside the platform | deep_space, vanilla-height copy | Platform stays put |
| Our zero-G systems off (`-PspikeNoZeroG=true`) | All of the above | Same results |
| Real client: spawn at the player, Sodium drawing | deep_space | Spawns, stays put, draws correctly |
| Real client: thrust down onto the platform | deep_space | Player stops on its top surface (Y -415.99 on a -416.0 top) |
| GameTests with Sable + Aeronautics loaded | | All 52 required tests pass |

## The other AI's suggestions
- **Dimension height:** not the cause. Sable removes a sub-level only outside its config range
  (`sub_level_remove_min` -10,000 / `sub_level_remove_max` 100,000); deep_space is Y -2032..2031. A
  vanilla-height copy behaved identically.
- **sable-rapier-64bit:** not needed for this. It fixes f32 precision far from the origin. It is CurseForge
  only (not reachable from this container) and predates Sable 2.0's change to native loading, so it may not
  fit 2.0.6. Worth a look if very distant stations ever jitter.

## Not tested yet
- A moving ship (propellers / thrusters) and riding it with our flight.
- CI datagen with Sable loaded (it hung in the first spike).

## How to rerun
`./gradlew runGameTestServer` (tests `SableSpikeGameTests`, logs `[SableSpike ...]`), optionally with
`-PspikeNoZeroG=true`.
