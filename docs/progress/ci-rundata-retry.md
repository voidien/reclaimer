## CI: `runData` runs once
- The retry loop round `runData` is gone: `RenderClassesFirstMixin` ended the deadlock it was there for, with 20 of 118 `main` runs failed or hung before it and 0 of 30 after. CI keeps the 5-minute `timeout` and the thread dump, so a hang now fails the build with its classes in the log, and a re-run no longer hides it (rule 8).

## Ideas for the owner
- **The GameTest retry loops could go the same way:** the two loops in `build.yml` retry a run over the timeout, for the same start-up hang. From `main`'s logs, 12 of 306 GameTest jobs hit one before the fix and 0 of 130 after.
