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
The Launch Complex fires on its own once wound up; a program can assemble or set it down but cannot make it launch, as a player cannot either.

| Block | Function | Returns |
|---|---|---|
| Launch Controller | `isAssembled()` | whether the ring is complete and the pad filled |
| | `assemble()` | assembles the complex: `assembled`, `already_assembled`, `ring_incomplete`, `blocked` or `failed` |
| | `disassemble()` | sets it down: `disassembled`, `not_assembled` or `failed` |
| | `getProgress()` | wind-up of the next launch, 0 to 1 |
| | `getLaunches()` | payloads this complex has fired |
| | `getSpeed()` | shaft speed in RPM |
| | `hasPayload()` | whether a payload is loaded |
| | `getItems()` | its two payload slots: `{ [slot] = { name, count } }` |
| | `getSphere()` | the Dyson Sphere (below) |
| Kinetic Drive Base (old, inert) | `isAssembled()` | whether the Payload Chamber and Barrel Nozzle sit on top of it |
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
| `isAssembled()` | whether the frame is complete and the column filled |
| `assemble()` | assembles the tower, like a sneak-use: `assembled`, `already_assembled`, `frame_incomplete`, `blocked` or `failed` |
| `disassemble()` | sets it down: `disassembled`, `not_assembled` or `failed` |
| `getSpeed()` | shaft speed in RPM |
| `isCharging()` | whether the tower is charging for a pulse |
| `getCharge()` | how far the charge has come, 0 to 1 (0 when none is under way) |
| `getCost()` | what one pulse burns: `catalysts`, and `water` and `slurry` in mB |
| `getStock()` | what is loaded: `catalysts`, and `water` and `slurry` in mB |
| `isRunning()` | whether a Genesis Pulse is sweeping out from this Spire's tower |
| `getProgress()` | how far the running pulse has swept, 0 to 1 (0 when idle) |
| `getPulses()` | pulses finished at this Spire |
| `getRadius()` | blocks from the tower's centre to the far edge of its sector (0 off the planet) |
| `getSector()` | this tower's sector: `column` and `row` (1 to 4 from the north-west) and `restored`; nil off the planet or in another dimension |
| `getSectors()` | 16 booleans, row by row from the north-west: which sectors are restored, read from any dimension |
| `getRestoredCount()` | how many sectors are restored, 0 to 16 |
| `pulse()` | charges and fires the Genesis Pulse like a right-click or redstone pulse: `true` when the charge starts, or `false` and a reason: `"busy"`, `"wrong_world"` (only on ruined Earth), `"not_assembled"`, `"off_planet"`, `"sector_restored"`, `"no_profile"`, `"no_catalyst"`, `"no_water"`, `"no_slurry"`, `"no_rotation"` |

```lua
local spire = peripheral.find("void_reclaimer:terraforming_spire")
if not spire.isAssembled() then print(spire.assemble()) end
local ok, why = spire.pulse()
if not ok then print("no pulse: " .. why) end
while spire.isCharging() do sleep(1) end
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

## Station Core (`StationPeripheral`)
What a player can do at the core by hand.

| Function | Returns |
|---|---|
| `getName()` | the station's name |
| `setName(name)` | renames the station (empty restores the default; 32 characters at most) |
| `isAssembled()` | whether the core is built into an assembled station (a Sable ship) |
| `assemble()` | assembles the connected blocks into a station: `assembled`, `already_assembled`, `nothing`, `too_big` or `failed` |
| `disassemble()` | sets the station down as blocks: `disassembled`, `not_assembled`, `not_level`, `blocked`, `nothing` or `failed` |
| `getPosition()` | `{ x, y, z }` where the core is in the world right now |
| `getFilledSlots()` | how many of the station chest's 27 slots hold something |

## Docking Clamp (`DockingClampPeripheral`)
What sneak-use and redstone do.

| Function | Returns |
|---|---|
| `isDocked()` | whether a ship is docked |
| `getDockedName()` | the docked ship's name, or `""` |
| `getStatus()` | what the clamp last did or found: `idle`, `not_on_ship`, `no_visitor`, `misaligned`, `blocked`, `docked`, `released`, `empty`, `failed` |
| `dock()` | docks the ship touching the clamp's face; returns the status |
| `release()` | lets the docked ship go; returns the status |

## Holo Projector (`HoloPeripheral`)
Lines count from 1. Writing text switches the projector to text and `setSource` to a readout: the last writer wins.

| Function | Returns |
|---|---|
| `getLines()` | every line, as a list of strings |
| `getLine(n)` | line `n`, or `""` past the last one |
| `setLine(n, text[, color])` | writes one line (cut at 48 characters); `true`, or `false, "no_such_line"` |
| `setLines({ ... })` | replaces all the text; long entries wrap on to the next lines |
| `clear()` | removes all the text |
| `setColor(color)` | colour of lines that have none of their own: `0xRRGGBB`, `"#RRGGBB"` or a dye name such as `"cyan"` |
| `getScale()` / `setScale(x)` | size of the picture, 0.25 to 4 (held to that range) |
| `getHeight()` / `setHeight(x)` | blocks above the projector, 0 to 4 (held to that range) |
| `setBillboard(on)` | whether the picture turns to face the viewer |
| `setModel(id)` | shows a GeckoLib model, turning slowly: `"entity/mining_probe"` means `geo/entity/mining_probe.geo.json` (a bare id means ours); `false` for a malformed id. A client without that model shows "NO MODEL" |
| `setBody()` | shows each viewer their own body |
| `getModel()` | the model shown, or nil |
| `getMode()` | `"text"`, `"readout"`, `"model"` or `"body"` |
| `getSource()` | the chosen readout kind's id |
| `setSource(id)` | shows a readout from now on (a bare id means ours); `true`, or `false, "unknown_source"` |
| `getSources()` | every readout kind's id, sorted |
| `getSize()` | `8, 48`: how many lines and characters fit |

## Coming next
Item upgrades: turtle and pocket computer upgrades for our tools (for example a Mechanical Drill turtle and a Dust Sensor
pocket computer), as their own step.
