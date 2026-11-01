## Restart tests side by side on CI
- The five restart tests in `build.yml` each run on their own machine, so one no longer waits for the others. They took 4m24 in a row, the longest job of a pull request. Coverage is the same. A failure in one does not cancel the rest.
