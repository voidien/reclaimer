## Hull Plating
- Hull Plating Mk I to III are utility modules: +2, +6 and +20 hearts at 0.5, 2 and 6 mass, at tiers 1, 3 and 4, so a Drone can wear a Mk I. A new `plating` spec on `ModuleStats` carries the health, so a datapack module can do the same, and it fades with the wear of the section holding it.
- **Decided alone:** three tiers, like the Dust Compressor, because one number can't suit a Drone and a Titan. The stats and prices are placeholders to tune: 40/20/10/0 for 60 s at Diversified, 120/60/30/20 for 150 s at Starship, 300/160/80/60 for 300 s at Titan unlock.
- **Known issue:** plating does not soften a crash, because `BodyCrash` takes a share of max health, so more health means a bigger hit by the same share. It only helps against flat damage.
- **Not done:** bulking the body up on its model. Item icons are placeholders.
