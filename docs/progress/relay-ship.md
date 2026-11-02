## Relay block, part 3: it keeps its ship loaded
- A lit Relay aboard a ship holds that ship loaded, so a relay ship stays on the network with nobody near it. It lets go when it goes dark, when it is broken and when it moves to another ship. Test: `aLitRelayHoldsItsShip`.
- **Default chosen (safest, reversible):** a dark relay does not hold its ship, so a ship whose power has run out unloads like any other, and its relay stays listed as dark. It cannot wake itself: something has to come back to it.
