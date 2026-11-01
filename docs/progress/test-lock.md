## Tool: one test server at a time
- `tools/gametest.sh` takes a machine-wide lock, so checkouts wait for each other instead of running 6 GB JVMs side by side. `tools/verify.sh` refuses a second run in one checkout, which is how two overlapping runs garbled a log and let a wrong unit test reach CI, and says when a failure was a JVM crash.

## Ideas for the owner
- **Run memtest.** Five JVM crashes in five minutes on 2026-11-01, in the garbage collector and in compiled code, with a saved key read as `Namg` for `Name`, one flipped bit. It may also explain Claude Code exiting, but that is a guess.
