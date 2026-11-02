# The Replicator and the small tools

Settled decisions, moved here from `CLAUDE.md`. Keep them current in the same PR as the change.

**Phase 4a**
- The Replicator is a timed queue (key R). It holds 5 jobs. Dust, and any items a recipe takes, are paid when a job is queued and refunded on cancel. Items come from the main inventory, hotbar first, and not from the offhand or armor; what doesn't fit on a cancel waits in the output. A blueprint never takes items. A job that took items remembers which body paid, and only that body can cancel it, so items can't travel for free; if that body has gone or is a wreck, any body can. It uses 1.5 energy/s while working and pauses in low power. Output goes to the inventory, with a 9-slot overflow buffer. The screen never pauses the game.
- Recipes unlock through `Milestone`s. Each milestone adds an AI log entry (toast + Logs tab). New milestones are enum entries with an `isReached` check plus lang keys for their title, hint, and log.
- The recipe type `void_reclaimer:replicator` is datagen-only (`ModRecipeProvider`, ids `replicator/<name>`) and `isSpecial()`, so it stays out of the vanilla recipe book. JEI shows it.
- The Salvage Tool refunds 50% (config) of the cheapest per-item recipe cost for blocks in `#void_reclaimer:salvageable`, with no drop.
**Phase 4b**
- **Nanite Swarm** builds the offhand block (Scaffold from the inventory if the offhand is empty) in empty space 2–6 blocks ahead (sneak + scroll, `nanite_range` component). It costs 1 energy per block, uses vanilla `BlockItem#place`, and places against a looked-at block if there is one. The outline is a plain white box.
- **Tether Anchor**:
  - Leash: clip on with right-click; you drift up to 16 blocks, then get pulled back gently. Client physics in `ThrusterFlight`.
  - Recall (hold G): within 256 blocks a 1 s hold starts a pull at 2× top speed for 3 energy/s, stopped by releasing G. Farther away, a 3 s hold teleports you for 25 energy with a 60 s cooldown. Same dimension only.
  - HUD: text only, "TETHER x/16m".
- **Backup Core**:
  - Up to 3 per player, each with 0–3 charges. A charge costs Metallic 10 + Silicate 10, or sunlight refills it slowly.
  - The core UI renames it. On death the death screen lists charged cores; picking one spends a charge on respawn, with the deep space spawn as the free default.
  - Items drop and float at the death spot; the HUD death marker points there.
- Logic for nanites and the tether lives in plain classes (`Nanites`, `Tether`) so Phase 5a can run them from body modules.
- **Milestones:** First Structure (10 blocks placed) unlocks the Tether Anchor; Diversified (3 dust kinds) unlocks the Backup Core.
- **Starter nebula:** an opt-in `voidReclaimerStarterNebula` game rule (off by default), locked when the world is created; its center reaches the client through `DustFieldSeed`.
- **Saved data:** attachment codecs always give defaults (`optionalFieldOf`), and use `lenientOptionalFieldOf` for lists and complex values, so old or partly broken saves load.

**Replicator screen (2026-10-30, owner's choices)**
- **Descriptions:** every built-in Replicator recipe has a one- or two-line gameplay hint, translation key `replicator.<namespace>.description.<name>` (recipe id `<namespace>:replicator/<name>`); a recipe may carry its own `description` text, which overrides it (datapack field, synced). The screen shows it in a box under the list for the selected recipe and as a tooltip on every unlocked row. `RecipeDescriptionTest` fails when a built-in recipe has none, so new recipes need a line in `ModLangProvider.addReplicatorDescriptions`.
- The panel is 236 tall (was 200; at most 240 so it fits a 720p window at GUI scale 3) with 5 recipe rows and a 5-line description box.
- **JEI on the screen:** `VoidReclaimerJeiPlugin.registerGuiHandlers` registers `ReplicatorScreen` (it is not a container screen) with its bounds, so JEI's lists sit beside the panel, and `ReplicatorScreen.itemAt` tells JEI which item (recipe result, queued job, output item) is under the mouse, so JEI's recipe and uses keys work on them. Dust icons are not JEI ingredients and have no lookup.
- **Reorder:** drag a waiting job in the queue to a new place (`ReplicatorActionPayload` action `MOVE`, `ReplicatorLogic.reorder`, `Replicator.move`). The job being worked on never moves and nothing goes in front of it, so progress is never lost; the server validates every move.

**Respawn screen (2026-10-30, owner's choices)**
- The death screen's core list is a "Restore at" panel of cards (`DeathScreenCores`): the free deep space spawn and every charged Backup Core, each with its name, charge pips (`core/RespawnCards`) and the distance from the death spot; the chosen card is sent to the server as before (`RespawnChoicePayload`). A short AI line sits under the score; its text is a placeholder marked `[Placeholder, lore text pending.]` in the lang file.
**Body inventory (2026-10-30, owner's choices)**
- **E opens `BodyInventoryMenu`** (`ModMenus.BODY_INVENTORY`, screen `BodyInventoryScreen`) instead of the vanilla inventory, for survival and adventure players: only the three main rows and the hotbar, no armor, offhand or crafting slots. The client cancels the vanilla `InventoryScreen` (`ClientInventoryController`) and sends `OpenInventoryPayload`; the server opens the menu. Rows the body has not unlocked are darkened (`InventoryLock` and `SlotLockMixin` still enforce it). Creative and spectator keep their own screens.
- The screen has a tab strip: Inventory, Replicator, Body, Logs (`ReplicatorScreen.Start`); R opens the Replicator; the Replicator screen has an Inventory button. The panel's right side shows a 3D preview of the body (the vanilla entity-in-GUI render, which goes through our `RenderPlayerEvent` body renderer; sized by the larger of hitbox height and, for big classes, hitbox width times four) and the dust reserves (`DustMeterLayer.drawRows`, shared with the HUD meter, which hides while this screen is open).

