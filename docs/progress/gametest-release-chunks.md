## GameTests let go of chunks between batches
- Every forced test area kept about 625 half-made chunks in memory for the whole run, and the remote control tests took a fresh world past 10 GB (66,000 chunks, about 17 million sections). `GameTestBatchChunkMixin` releases all forced chunks as each batch starts, when every test before it has finished. This was tried once and made things worse because each unloaded chunk was saved; normal runs write no chunks now. Restart tests keep their chunks.
- It also ends forced chunks carrying over between local runs, an entry under "Ideas for the owner".
