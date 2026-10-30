# CC: Tweaked peripherals

Void Reclaimer works with **CC: Tweaked** (optional). Put a computer next to one of our blocks, or connect it with a wired
modem, and `peripheral.wrap(side)` gives you the functions below. Without CC: Tweaked installed none of this is loaded.

Functions run on the server thread (`mainThread = true`), so they always see the block's current state. Control
functions follow the same rules as a player: anything a computer can do, a player could do by hand or with redstone.

## Factory machines (`MachinePeripheral`)

| Block | Function | Returns |
|---|---|---|
| Cloud Siphon | `getDensity()` | cloud density at the siphon as of its last sample (0 outside a cloud, sampled once a second) |
| | `getPower()` | shaft (and addon) power, 1 at 64 RPM |
| | `getSpeed()` | shaft speed in RPM |
| | `getEnergy()`, `getEnergyCapacity()` | Forge Energy in the buffer, and its size |
| | `getItems()` | output slots: `{ [slot] = { name = "...", count = n } }` (empty slots left out) |
| Electrolyzer | `getProgress()` | progress toward the next batch, 0 to 1 |
| | `getSpeed()` | shaft speed in RPM |
| | `getTanks()` | `{ water, oxygen, hydrogen }`, each `{ name, amount, capacity }` in mB |
| Spinner | `getProgress()` | progress toward the next thread, 0 to 1 |
| | `getSpeed()` | shaft speed in RPM |
| | `getItems()` | input (slot 1) and output (slot 2) |

Example:

```lua
local siphon = peripheral.find("void_reclaimer:cloud_siphon")
print(("density %.2f, %d FE"):format(siphon.getDensity(), siphon.getEnergy()))
```

## Terraforming Spire (`SpirePeripheral`)

| Function | Returns |
|---|---|
| `isRunning()` | whether a Genesis Pulse is sweeping out from this Spire |
| `getProgress()` | how far the running pulse has swept, 0 to 1 (0 when idle) |
| `getPulses()` | pulses finished at this Spire |
| `getRadius()` | blocks from the Spire to the edge of its square area |
| `pulse()` | fires the Genesis Pulse like a right-click or redstone pulse: `true`, or `false` and a reason: `"busy"`, `"wrong_world"` (only on ruined Earth), `"no_profile"` |

```lua
local spire = peripheral.find("void_reclaimer:terraforming_spire")
local ok, why = spire.pulse()
if not ok then print("no pulse: " .. why) end
while spire.isRunning() do print(("%.0f%%"):format(spire.getProgress() * 100)) sleep(1) end
```

## Coming next
The Dyson launch chain (Kinetic Mass Driver, Quantum Receiver), the Terraforming Spire, and the Backup Core, Tether Anchor
and Atmospheric Portal frame. Item upgrades (turtle and pocket computer) come after.
