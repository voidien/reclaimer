## Test: every core can be bought in a smaller body
- Dust is paid in full when a job starts, so a body can only buy what it can hold. `everyCoreCanBeBoughtInASmallerBody` checks each size class's core against the biggest smaller body: the Titan's core section costs 1,200 and a smaller body holds 1,400.
- **Known issue:** the Titan Core Kit costs 1,650, so it can't be bought until you own a Titan, while its three parts bought one at a time cost 1,840. The test covers single parts; it can cover kits once the price is decided.
