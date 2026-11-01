## CI: GameTests run once
- The owner chose this on the calls page. 12 of 306 GameTest jobs hung before #180 and 0 of 130 since, so a hang now fails the run, after a thread dump (`tools/timed.sh`), rather than running again.
