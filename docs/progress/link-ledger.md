## Remote control: the shell list
- The player file now keeps one entry per shell for good (`LinkState.KnownShell`: newest link number, last place, wreck, gone). It closes two gaps the review found: a stale copy that turns up after its outbox has ended now removes itself, and a shell or an emptied wreck destroyed for good cannot come back. `/void_reclaimer link shells <player>` prints the list. Tests: `LinkLedgerGameTests` (10) and four in `LinkStateTest`, all mutation-checked.
- Saves from before this have no entries; an outbox still running hands over to the list when it ends.

## Known issues
- **A shell destroyed with its owner offline** can still come back from a running outbox: the list is in the player file, so it cannot hear of it. Only a dedicated server can reach this.
