## Remote control: death in a shell
- A player whose shell dies around them wakes in the main body with nothing lost: no items fall, no death marker, no Backup Core charge, and the shell is left as a wreck. Test: `dyingInAShellWakesYouAtHomeAndLeavesAWreck`, mutation-checked.

## Decisions pending (for the owner)
- **Lost shell:** a chat line, placeholder text, tells the player. If the crash window after a loss rebuilds the shell from the outbox it comes back as a shell, not a wreck: nothing is lost, only the destruction.
