## Helper drones, part 5: the battery: done
- Drones carry a small battery. Flying, digging and gathering spend it; sunlight and a Station Core within 32 blocks fill it. Below 10% a drone's speed and work fade smoothly, down to a quarter at empty. It never stops: a flat drone in the dark crawls on, because time is the only hazard.
- The numbers keep the open sky close to the old behaviour. Flying costs 1 a tick and deep space's sun gives 0.8, so a drone flying non-stop in the open takes about 10 minutes to run low and then settles at about 80% speed. In the dark, a full battery lasts about 2 minutes of flight.
- Right-click and photo mode's overlay show the battery; so does the item's tooltip. Taking a drone back keeps its charge. A full drone's item has no battery component, so full drones stack.
- A drone saved before this loads full and works; its whole-number dig count still reads.
- `HelperChargeEvent` lets an addon add a way to charge. The Station Core's peripheral gains `getDronesCharging()`.
- **Tests:** `HelperBatteryTest` (unit, 7); `HelperBatteryGameTests` (8: flat and full speed, flight spends and sun fills, dark then a Core, a flat miner, an old save, the item, the event, the status line); `aFlatCollectorGathersSlowly`; the peripheral's method list.
- **Known limits:** a drone does not go to a Core by itself, it only charges in range; the range is measured to the Core, not the station's hull; a Core is checked on the station list, which refreshes once a second.

### Not seen in a client yet
The battery in the status line, tooltip and overlay; a flat drone's crawl.

## Decisions pending (for the owner)
- **Helper battery:** capacity 2400, flight 1 and work 0.5 a tick, sun 1.2 at full light (0.8 in deep space), Core 4 a tick within 32 blocks (its own `helper.coreRange`, not the 8-block `station.range`), fading below 10% to 25% power at empty; a flat drone slows and never stops; a Core's charge is free and works for anyone's drone; drones charge passively and do not seek a Core ("Helper drones, part 5").

## Ideas for the owner
- **Drones that go home to charge:** a low drone with a Station Core in reach could fly there, top up and carry on. It would need a new phase in each job, so it is left out for now.
- **Charging pad:** a small block that charges drones in range, far cheaper than a Station Core, for dark bases. `HelperChargeEvent` already allows it.
