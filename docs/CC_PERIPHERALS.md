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

## Dyson launch chain (`DysonPeripheral`)
Read-only: the Mass Driver fires on its own once wound up, and a player cannot make it launch either.

| Block | Function | Returns |
|---|---|---|
| Kinetic Drive Base | `isAssembled()` | whether the Payload Chamber and Barrel Nozzle sit on top of it |
| | `getProgress()` | wind-up of the next launch, 0 to 1 |
| | `getLaunches()` | payloads this driver has fired |
| | `getSpeed()` | shaft speed in RPM |
| | `hasPayload()` | whether the chamber above holds a payload |
| | `getSphere()` | the Dyson Sphere (below) |
| Payload Chamber | `getItems()` | its slots: `{ [slot] = { name, count } }` |
| Sub-Light Quantum Receiver | `getSpeed()` | the speed it turns the network at (RPM), 0 before any truss is launched |
| | `getStressCapacity()` | the stress capacity it adds, in SU |
| | `getSphere()` | the Dyson Sphere (below) |

`getSphere()` returns `{ satellites, trusses, shellTotal, tier, shellProgress, sunlight }`: launches so far, the sky tier
(0 to 4), how much of the shell is built (0 to 1) and how much of the star's light still reaches deep space (1 to 0).

```lua
local driver = peripheral.find("void_reclaimer:kinetic_drive_base")
local s = driver.getSphere()
print(("tier %d, shell %.0f%%"):format(s.tier, s.shellProgress * 100))
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

## Spawn and travel (`SpawnTravelPeripherals`)
Read-only.

| Block | Function | Returns |
|---|---|---|
| Backup Core | `getName()` | its name |
| | `getOwner()` | the owner's name (or UUID), nil before anyone placed it |
| | `getCharges()`, `getMaxCharges()` | respawn charges stored, and the most it holds |
| | `getSunProgress()` | sunlight's progress toward the next charge, 0 to 1 |
| Tether Anchor | `getTethered()` | names of the players clipped to it |
| Atmospheric Portal Frame | `getPortal()` | nil if this block is not part of a closed frame, else `{ lit, width, height, axis }` (interior size; axis `"x"` or `"z"`) |

The Tether Anchor and the portal frame have no block entity, so they are attached through CC's block capability; the
functions are the same to a computer.

## Ship thrusters (`ThrusterPeripheral`)
Read-only. A computer sets the throttle the way a player does: with redstone (`redstone.setAnalogOutput(side, 0 to 15)`).

| Block | Function | Returns |
|---|---|---|
| Chemical Thruster | `getThrottle()` | the redstone throttle, 0 to 1 |
| | `getForce()` | the force it pushes with right now |
| | `isFiring()` | whether it is pushing |
| | `getSpeed()` | shaft speed in RPM |
| | `getTanks()` | its fuel tank: `{ { name, amount, capacity } }` in mB |
| Ion Thruster, RCS Thruster | `getThrottle()`, `getForce()`, `isFiring()` | as above |
| | `getEnergy()`, `getEnergyCapacity()` | Forge Energy in the buffer, and its size |

## Helm (`HelmPeripheral`)
Read-only: a computer cannot take the stick.

| Function | Returns |
|---|---|
| `isOnShip()` | whether the Helm is built into a ship |
| `getPilot()` | the pilot's name, or nil |
| `getThrusterCount()` | thrusters the Helm found on its ship |
| `getTranslation()` | how well the ship can push each way, 0 to 1: `{ forward, back, right, left, up, down }` |
| `getRotation()` | how well it can turn each way: `{ rollRight, rollLeft, noseUp, noseDown, yawLeft, yawRight }` |
| `getSupplies()` | `{ fuel, fuelCapacity, energy, energyCapacity }` across the ship's thrusters |
| `getVelocity()` | the ship's speed along the Helm's axes, blocks per second: `{ surge, sway, heave, speed }` |
| `getSpin()` | its turning rate, degrees per second, as the pilot reads it: `{ rollRight, noseUp, yawRight }` |
| `getAttitude()` | degrees: `{ pitch, roll, heading }` (nose above the horizon, roll to the right, compass bearing of the nose) |

## Coming next
Item upgrades: turtle and pocket computer upgrades for our tools (for example a Mechanical Drill turtle and a Dust Sensor
pocket computer), as their own step.
