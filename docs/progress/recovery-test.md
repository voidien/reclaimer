## Test: a recovery with no seat is checked at once
- `recoveryAsksForASeatInReachAndNoOutbox` waited 30 ticks for a note that any level save may refresh, so another test's save could fail it. A request is queued in the tick it is made, so it now asks `ShellRecovery.waiting` straight away.
