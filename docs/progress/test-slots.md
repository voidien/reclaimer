## Tools: GameTest servers side by side on the Omen
- `tools/test-slots.sh` says how many servers a machine runs at once: four on the Omen, one anywhere else, as the owner asked. `tools/gametest.sh` waits for a slot, and each world has its own lock, so `tools/restart-test.sh` no longer needs the shared one.
- `tools/verify.sh` runs the two suites at the same time where there is more than one slot.
