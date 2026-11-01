## Holo-displays, part 4: CogScript: done
CogScript programs get a `holo` module that writes to projectors within 16 blocks, through a small mixin into Advanced Electricity's `ComputerOS`; the patch and how to find it again are in `docs/ADVANCED_COMPUTER_TECHNIC_PATCH.md`. Tests: `cogScriptWritesToTheProjector`, `theCogScriptMixinInstallsTheModule`, `aProgramOnItsOwnThreadReachesTheProjector` (a program on its own thread, through the `onMainCall` hop). **Not seen in a client:** no program was typed into a computer's screen.

## Decisions pending (for the owner)
- **CogScript:** the 16-block range, addresses as `"x,y,z"`, and that the inject uses `require = 0` so an addon update costs programs the module, not the game its start.
