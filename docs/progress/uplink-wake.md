## Remote control: the wake chain
- A wake goes to where the main body was left, else to a listed Backup Core with no charge spent, else to the deep space spawn. A seat aboard a ship holds the ship loaded while it is in use and keeps the link's record of the main body's place current, so a wake lands by the ship. Tests: `LinkWakeGameTests` (3) and `aSeatAboardAShipHoldsItAndWakesYouByIt`, mutation-checked.
- `aRefusedWakeChangesNothingHoweverOftenItIsAsked` now refuses by an unreadable main body: a locked dimension no longer refuses a wake, it starts the chain. What it checks is the same.
