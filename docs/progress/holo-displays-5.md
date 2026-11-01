## Holo-displays, part 5: models and the body: done
A projector can show any GeckoLib model by id or the viewer's own body, turning slowly, from CC (`setModel`, `setBody`), CogScript (`holo.model`, `holo.body`) and `/void_reclaimer holo <pos> model|body|billboard`. Seen in the client with the mining probe, the Drone body and a missing id. Text, the billboard and the fixed picture (north and east) were checked by eye in the same session; a billboard on a turning ship has not been.

**Placeholders:** the tint and strength of the cyan wash (`HoloStyle.MODEL_TINT`, `MODEL_ALPHA`, `WASH_ALPHA`).

## Decisions pending (for the owner)
- **Holo models:** the body shown is the viewer's, not the owner's (each person sees their own); models turn at 1 degree per tick whatever the billboard setting; a model gets its texture by the `textures/<path>.png` rule, or the plain glow; a model plays no animation.
