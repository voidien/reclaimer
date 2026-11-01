## Remote control: the mirror
- While someone is linked from a seat it shows their main body: a `BodyHost` with the role MIRROR, built from the link's copy, never saved, unhurt by anything short of a command, and rebuilt within a second if it goes. A wake or a broken seat removes it. Tests: `aSeatShowsTheMainBodyWhileLinked`, `aBrokenSeatTakesItsMirrorAndLeavesTheLink`, mutation-checked.

## Decisions pending (for the owner)
- **Where the mirror stands:** on top of the seat, whatever its size, so a big main body overhangs it. A proper seat model can set this later.
