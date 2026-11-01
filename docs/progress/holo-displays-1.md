## Holo-displays, part 1: the projector and its text: done
The block, its saved content, the readout refresh, the CC peripheral and the drawing of text; plan in `docs/HOLO_DISPLAYS_PLAN.md`, facts in `docs/design/plans.md` "Holo-displays". `HoloTextTest`, `HoloContentTest`, `ReadoutsTest`, `HoloRenderMathTest`; `HoloGameTests`; `theHoloProjectorIsAPeripheral`.

**Placeholders:** `textures/block/holo_projector_top/side/bottom.png`, `textures/holo/panel.png` (white scanlines), `textures/holo/glow.png`, the cyan and flicker constants in `HoloStyle`, the price 30/40/10/20 dust.

## Decisions pending (for the owner)
- **Holo-displays:** the refresh every 10 ticks, the caps of 8 lines of 48 characters, last writer wins; lines blanked when text replaces a readout; the price 30/40/10/20 and the Diversified milestone ("Holo-displays, part 1").
