## Remote control: stale shells and the outbox reader
- After a crash, a copy of a shell left on disk from before the link removes itself, and a shell put back in the world that never reached the disk is rebuilt from the player file. See "Remote control" in `docs/design/plans.md`.
- Tests: `LinkOutboxGameTests` (5) and two restart tests that really crash, `aWokenShellOutlivesACrash` and `anOldShellGoesAfterACrash`, in CI's restart job. Each behaviour was mutation-checked.

## Decisions pending (for the owner)
- **Outbox:** an outbox ends after two saves of the shell's level, about 10 minutes at the default autosave. A stale copy waits until its owner is online to remove itself.
