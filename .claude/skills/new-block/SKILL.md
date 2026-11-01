---
name: new-block
description: Add a block or block item to Void Reclaimer with all its registration, datagen, recipe, lang, CC peripheral, docs and tests. Use whenever a new block is added.
---

Every step below is a file a real block needed (see the Docking Clamp, commit 46791da). Follow the block nearest yours and match it. Skip a step only when it truly does not apply, and say so in the PR.

**Code**
1. The block and block entity classes in their feature package (`station/`, `machine/`, `ship/`). Pure logic goes in a config-free class with a unit test.
2. `registry/ModBlocks.java`, `ModItems.java` (a `BlockItem`), `ModBlockEntities.java`, `ModCreativeTabs.java`.

**Datagen**, in `datagen/`
3. `ModBlockStateProvider`: the blockstate, model and item model.
4. `ModBlockLootProvider`: `dropSelf` or its own table.
5. `ModBlockTagsProvider`: `mineable/pickaxe` at least. Open sets are tags, not code (`CLAUDE.md` "Extensibility").
6. `ModRecipeProvider`: a Replicator recipe, with a milestone and a placeholder price noted in the progress file.
7. `ModLangProvider`: the name, every message, and a line in `addReplicatorDescriptions` (`RecipeDescriptionTest` fails without it).
8. Run `./gradlew runData` and commit `src/generated`.

**Art:** placeholder textures are allowed: add them to `docs/art/ASSETS.md` with `python3 tools/art/asset_list.py`.

**CC: Tweaked** (standing rule): a block with state or actions gets a peripheral in the same PR: `compat/cc/<Name>Peripheral.java`, registered in `CCCompat.Sources.all()`, rows in `docs/CC_PERIPHERALS.md`, a test in `CCPeripheralGameTests`.

**Tests:** a GameTest for the behaviour, on a chunk the test holds a ticket on, mutation-checked: break the feature, see the test fail, restore.

**Docs:** `docs/progress/<slug>.md` for this PR; `CLAUDE.md` only for a new settled decision or extension point; a config number goes in `Config.java` with a comment.

**Before the push:** `tools/verify.sh`.
