## Remote control: stale shells, the outbox reader, and a link that survives singleplayer
- After a crash, a copy of a shell left from before the link removes itself, and a shell that never reached the disk is rebuilt from the player file. See "Remote control" in `docs/design/plans.md`.
- **Found in review, and worse than anything here:** singleplayer loads the player from `level.dat`, which `saveAll` does not write, so a link was only as safe as the last autosave and a crash could lose a whole shell. Every move now writes `level.dat` too. Checked in a real client with `kill -9`: the link survives with the fix, and the world comes back "In the main body" without it. No player could reach this yet.
- The review also led to: one look per session (no duplicate of a moved shell, no destroyed shell coming back, no chunks reloading every few seconds), a killed shell clearing its outbox, an old copy giving way only to a body that can be read, and a rebuild that throws no longer taking the server down.
- Tests: `LinkOutboxGameTests` (8), two restart tests that really crash, each behaviour mutation-checked.

## Decisions pending (for the owner)
- **Outbox:** it ends after two saves of the shell's level. On pause those can be seconds apart, so "two saves" is not a fixed time.
- **Known gaps, rare, left for the shell list in PR 10:** a stale copy in a chunk that stays unloaded until after its outbox has ended is not recognised, and a shell killed while its owner is offline comes back. A small permanent record of each shell's latest epoch would close both.

## Ideas for the owner
- **Decide what can destroy a shell before PR 10:** today a shell takes melee, drowning and suffocation, while the vision says there are no hazards but time. "Only deliberate actions" or "invulnerable while parked" would be simpler.
- **A repeatable singleplayer crash test:** a `tools/client-test` script doing what was done by hand here (link, `kill -9`, reload), because no GameTest can reach `level.dat`.
- **`/void_reclaimer link outboxes`:** each outbox's shell, epoch, saved flag and chunk state, so this logic can be seen working.
