## Remote control: the Uplink Seat, part 1
- The block, its CC peripheral (`getSeatId`, `isOccupied`, `getOccupant`), and a link that remembers its seat (`Link.seat`). Right-click takes the player to the newest shell on their list that stands ready. Tests: `UplinkSeatGameTests` (4), `theUplinkSeatIsAPeripheral`, two in `LinkStateTest`, mutation-checked.
- Placeholders: the seat uses the Station Core's textures. It has no recipe on purpose: remote control stays out of survival until the Remote Shell (PR 14).

## Decisions pending (for the owner)
- **Seat, before the picker:** right-click links to the newest idle shell on the list. The picker replaces this.
- **A seat is a full block for now:** there is no model yet, so nothing sits in it; the mirror comes next.
