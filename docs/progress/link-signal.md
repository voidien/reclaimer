## Remote control: signal range
- `link/SignalMath` and `link/RelayGraph`, pure, not used yet: how far a seat or relay reaches at a Dyson tier, what a seat at a station gains (the owner's 1.5 times range and later static), the strength at a distance, and the best chain through relays and portal bridges. Tests: `SignalMathTest`, `RelayGraphTest`, `theSignalRulesReadTheConfig`, mutation-checked below.

## Decisions pending (for the owner)
- **Signal numbers, placeholders:** seat 1024, relay 2048; Dyson tiers +2048, +16,384, +262,144 and +20,000,000 on every node; static in the outer 0.25 of a range, 0.1 at a station; the 1.5 times at a station counts the Dyson bonus too.
