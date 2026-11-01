## Spire, part 4: what a pulse costs: done
- Right-click, redstone or CC `pulse()` checks the cost and starts a charge; `Start.STARTED` now means charging. The charge counts ticks at 64 RPM (`MachineMath.progressPerTick`), waits while the shaft is below `minRpm`, and ends a fallen tower's charge. When it ends the cost is taken, all or nothing, and the wave leaves.
- Cost (config group `terraform`): `catalysts` 1 from `#void_reclaimer:spire/catalysts`, `water` 8000 mB from the fluid tag `spire/water`, `slurry` 4000 mB from `spire/slurry`; two tanks of `tank` 16000 mB; `chargeTicks` 400 (20 s at 64 RPM); `minRpm` 32; `stress` 32 SU per RPM.
- Automation only puts things in; a right-click with a catalyst or a fluid container loads it by hand. Catalysts drop when the Spire breaks; the tanks are lost. The new `ModFluidTagsProvider` writes the fluid tags.
- CC: `isCharging`, `getCharge`, `getCost`, `getStock`; `pulse()` can answer `"no_catalyst"`, `"no_water"`, `"no_slurry"`, `"no_rotation"`.
- Tests: `SpireCostGameTests` (8: every part of the cost, the charge and when it pays, a fallen tower, automation, the tags through the GameTest pack, hand loading, a reload and the stress value, drops), the CC test. Mutation-checked: a charge that ignores the shaft, a charge that outlives its tower, automation that can take out, no drop, an unpaid tank, tanks that take anything and no rotation check each fail a test.

### Not seen in a client yet
The charge's sparks at the top of the tower, loading the Spire by hand.

## Decisions pending (for the owner)
- **Pulse cost:** 8 buckets of water and 4 of slurry, 20 s of charge at 64 RPM from 32 RPM up, 32 SU per RPM, all placeholders; the cost is paid when the charge ends, so a charge that stops loses nothing but time; tanks are lost when the Spire breaks, catalysts drop ("Spire, part 4").
