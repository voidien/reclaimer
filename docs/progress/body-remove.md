## Debug tool: body remove takes modules too
- `/void_reclaimer body remove <player> <socket>` has existed since Phase 5b but was on no list. It now tab-completes sockets and can take one module off (`<kind> <index>`), and it is in the debug tools list. Test: `theRemoveCommandTakesOffASectionOrOneModule`.
- `docs/design/plans.md` holds the owner's answers from the build plans page.
- `docs/debug-commands.md` lists every debug command, photo key, test scene and script, read from the real command tree. Before this, `dust locate`, `asteroid locate`, `core charge`, `link wake`, `link force`, `sky sectors` and all of `holo` were on no list a session reads. `DebugCommandsListedTest` fails if a new one is missing.
