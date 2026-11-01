## Remote control: link events
- `ShellLinkEvent.Pre` (cancellable, before anything changes), `.Started` and `.Ended` with a `Reason` (left, signal lost, body lost). An end cannot be stopped, and a listener that throws cannot stop or undo a move. Tests: `LinkEventGameTests` (4), and the reason in `aShellOutOfRangeLosesItsSignal`, mutation-checked.
