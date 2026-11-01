## Spire, part 1: Bio-Nutrient Slurry: done
- `ModFluids.BIO_NUTRIENT_SLURRY` (`bio_nutrient_slurry`, tint `#6FA83A`, thick, no glow), with its bucket. Create basin recipe `create/terraform/bio_nutrient_slurry`, unheated: 250 mB Purified Water, 1 Fine Carbon Powder and 1 Ashen Slag give 250 mB of slurry, under the new toggle `bioSlurry`.
- Tests: `theBasinMixesBioNutrientSlurry` (the loaded recipe's inputs, heat and result), `theSlurryRecipeIsGatedByItsToggle` (the file's condition names the toggle), plus the recipe in `everyChainRecipeLoadsAsItsCreateType` and the walk of `CreateRecipeGameTests`. Mutation-checked: a recipe with dirt in place of slag fails the first, a recipe under another toggle fails the second.

### Placeholder assets
- `textures/item/bio_nutrient_slurry_bucket.png`: the Purified Water bucket recoloured green. The fluid uses tinted water like the others.

## Decisions pending (for the owner)
- **Bio-Nutrient Slurry:** the recipe's ingredients (Purified Water, Fine Carbon Powder, Ashen Slag: only nebula and Earth materials exist before the first pulse, so nothing grown), unheated, and 250 mB in and out ("Spire, part 1").
