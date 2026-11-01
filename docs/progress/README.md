# Progress entries
Each PR adds one file here, `docs/progress/<slug>.md`, instead of editing `PROGRESS.md`: new files never conflict.

A file is one or more `## ` sections. `## Ideas for the owner` and `## Decisions pending (for the owner)` add bullets to those lists at the top of `PROGRESS.md`; any other heading is a new entry, in the style of the existing ones. `python3 tools/progress_fold.py` folds them all into `PROGRESS.md` and deletes them, in a docs-only PR at the end of a session.
