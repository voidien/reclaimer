## GameTest servers end on running out of memory
- `-XX:+ExitOnOutOfMemoryError` on both GameTest runs. A server out of memory used to stay alive in full collections: one held 9 of 16 threads and 7 GB for six hours after its wrapper was stopped. Now it exits and the run fails at once. Checked with `-PgametestHeap=300m`: see the PR.
