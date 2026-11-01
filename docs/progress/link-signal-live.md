## Remote control: signal in play
- A seat refuses a shell its signal does not reach, and a link from a seat ends when it goes out of range: the player wakes and the shell stays, ready to link again. The strength reaches the client for the HUD (`LINK_SIGNAL`), and CC gets `getRange`. Tests: `LinkSignalGameTests` (6), mutation-checked.
- There are no relays or bridges yet, so a seat's reach is its own. A debug link has no signal rules.
