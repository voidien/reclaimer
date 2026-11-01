## Spire, part 6: the Planetary Grid: done
- `earth/PlanetaryGridItem`, component `planet_sectors`, `client/hud/PlanetaryGridLayer` and `PlanetaryGridLayout` (pure). The item keeps its own copy of the planet's map, so it reads the same from deep space.
- Replicator recipe `planetary_grid`: Tools tab, Earthfall, 10/15/5/10 dust (placeholder).
- No CC peripheral: it is an item with no block, and the Spire's `getSectors` already reads the planet from any dimension.
- Tests: `PlanetaryGridLayoutTest`, `PlanetaryGridGameTests` (2), and `RecipeDescriptionTest` for the line in the Replicator.

### Placeholder assets
- `textures/item/planetary_grid.png`: a small drawn grid. The map itself is plain `GuiGraphics.fill`; the `gui/planetary_grid.png` row in the asset list stays future.

### Not seen in a client yet
The map's place on screen, beside the rest of the HUD at small GUI scales.

## Decisions pending (for the owner)
- **Planetary Grid:** a map at the right edge while held rather than a screen or a vanilla map in hand; the item stores the sectors, so a Grid in a chest stays stale until someone carries it; the recipe sits behind Earthfall, so the first one is made after landing ("Spire, part 6").
