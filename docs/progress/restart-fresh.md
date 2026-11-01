## Fresh worlds for restart tests
- `tools/restart-test.sh` deletes the suite's world before it starts, so what a failed run leaves behind cannot decide the next one. Two of the four failed runs of `aNewerShellWinsAClash` came from that.
- Workflow: a stack of branches gets one full `tools/verify.sh all` on its top branch, and CI checks each branch as it lands (`HANDOFF.md`).
