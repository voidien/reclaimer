## Replicator recipes can take items
- A recipe's optional `inputs` are taken from the inventory when the job is queued, with the dust, by item or tag. A job saves the exact items it took (`ReplicatorJob.items`), so a cancel gives back the same ones, wear and names included, and what doesn't fit waits in the output. Nothing is taken unless the whole recipe can be paid (`RecipeInputs`). Jobs saved before this load with no items.
- The screen and JEI don't show inputs yet, and no built-in recipe uses them. Tests: `RecipeInputsTest`, `aRecipeTakesItemsAndACancelGivesTheSameOnesBack`.
