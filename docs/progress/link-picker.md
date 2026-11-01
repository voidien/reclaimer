## Remote control: the picker
- Right-clicking a seat opens a list of the player's shells, with how far off each is and how strong the signal is there. One out of range or wrecked is shown and cannot be picked. This replaces the stand-in that took the newest idle shell. Tests: `thePickerListsWhatIsLeftAndSaysWhatCanBeReached`, `aPickRunsTheSeatsOwnChecks`, mutation-checked. The screen is client code and has not been seen in a real client.

## Decisions pending (for the owner)
- **Shells have numbers, not names:** "Shell 2, 120 blocks off: 100%". The number is its place on the player's list and does not change.
