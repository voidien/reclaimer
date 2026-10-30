# Void Reclaimer playtest checklist

From the first grain of dust to the first Dyson launch. Written from the code and the recipe data on `main`, not from a play-through, so a step that doesn't behave as written is exactly what to report.

**How to use it.** Work top to bottom. Each stage has *Do*, *Expect*, and a **Skip** line: commands that jump past the waiting, so you can test a later stage without playing the earlier ones. Ticking a box means it matched. Anything that didn't: note the stage, what you did and what happened (screenshot if it is visual).

## 0. Setup
- [ ] Launch with `./gradlew runClient` (Sodium, ModernFix, FerriteCore and ImmediatelyFast load with it) or a build with the same mods.
- [ ] Make a **new** world. The `voidReclaimerStarterNebula` game rule (off by default) puts four small dust pockets 450 blocks from spawn, but it is locked in when the world is first entered, so decide at creation. With it off the first cloud may be far away; use `dust locate`.
- [ ] Allow cheats (the commands below need op). Stay in survival to play; use creative only to look around.
- [ ] Mods > Void Reclaimer > Config opens a settings screen. Change `body.minLight` (client > Body) and check it saves.

Handy commands (op):
| Goal | Command |
|---|---|
| Nearest dust cloud | `/void_reclaimer dust locate` |
| Nearest asteroid | `/void_reclaimer asteroid locate` (or `... locate large`, `giant`) |
| Dust (clamped to your storage) | `/void_reclaimer dust set @s void_reclaimer:metallic 200` (also `silicate`, `ice`, `carbon`) |
| Energy | `/void_reclaimer energy set @s 100` |
| Unlock a milestone | `/void_reclaimer milestone grant @s void_reclaimer:expansion` (`revoke` undoes it) |
| Ready-made body | `/void_reclaimer body preset @s drone|mech|ship|shuttle|titan [full]`, `/void_reclaimer body reset @s` |
| Dyson counters | `/void_reclaimer dyson set satellites 60`, `... trusses 50`, `/void_reclaimer dyson get` |
| Items | `/give @s void_reclaimer:<item> <n>` |
| Fly to a place | `/tp @s x y z` |

Keys: **R** Replicator, **Z** dampener, **B** sensor pulse, **N** cloud scanner, **G** hold to recall to a Tether Anchor, **Left Alt** module wheel, **V** creative thrusters.

## 1. Spawn and flight
- [ ] A new world puts you in deep space: black sky, stars, a small distant sun, no gravity, no hunger bar (an energy bar sits in its place).
- [ ] WASD thrust along where you look, Space and Shift go up and down. Top speed is about 10 m/s for the starter drone. With the dampener on (Z) you stop when you let go; off, you drift.
- [ ] Drifting through space shows specks streaking past; speed and dampener state show right of the hotbar.
- [ ] You look like a small probe-like drone in third person (F5), brighter than pure black.
- [ ] Energy slowly charges in sunlight and drops with thrust. At 0 you are in low power: thrusters offline, no damage.
- **Skip:** `/void_reclaimer energy set @s 100`.

## 2. First dust
- [ ] Find a cloud (`dust locate`, or fly toward the starter nebula). Inside you feel drag and the dust meter fills; flying through gathers far more than sitting still.
- [ ] Dust is per kind (Metallic, Silicate, Ice, Carbon) and stops at the storage cap (200 per kind at the start).
- **Skip:** `/void_reclaimer dust set @s void_reclaimer:metallic 200`, same for the others.

## 3. Replicator and the first body parts
- [ ] **R** opens the Replicator; the game keeps running. Tabs: Tools, Modules, Parts, Blocks, Raw, Logs, Body. Locked recipes show `???` and a hint.
- [ ] Awakening is granted at once (Scaffold, Dust Sensor, Thruster Mk I, Rotor Arm). **First Dust** appears with the first dust and unlocks the Drone Kit, Core Mk I, tools and the four Raw recipes.
- [ ] Queue a **Drone Kit** (40 metallic, 20 silicate, 80 s). Dust is taken when queued; cancelling refunds it. A finished job lands in the inventory (or the buffer; "Collect output").
- [ ] Body tab: install parts from the inventory; wrong ones are dimmed with a reason. Check locked inventory rows grey out and storage pods unlock more.
- [ ] Craft a **Dust Sensor** and install it in a sensor slot: readout inside a cloud, **B** pulse (5 energy, 10 s cooldown), **N** scanner mesh.
- [ ] Jobs pause in low power.
- **Skip:** `/void_reclaimer milestone grant @s void_reclaimer:first_dust`, fill dust, then wait out the 80 s or use a smaller part.

## 4. Tools and structures
- [ ] **Scaffold** (8 per job). Place blocks by hand; after 10 blocks **First Structure** unlocks the Tether Anchor and Brass Trim.
- [ ] **Nanite Swarm** (tool module, empty hand): right-click builds the offhand block or Scaffold in empty space ahead; sneak + scroll changes range (outline shown). Costs energy.
- [ ] **Salvage Tool** breaks Scaffold, Tether Anchor and Backup Core with no drop and refunds dust.
- [ ] **Tether Anchor**: right-click to clip; you drift up to 16 blocks then get pulled back. Hold **G** to recall (long hold teleports for 25 energy, 60 s cooldown).
- [ ] Hold three dust kinds to reach **Diversified**: Backup Core (up to 3 per player, charges cost 10 metallic + 10 silicate). Die with `/kill @s`: the death screen offers charged cores; "DROPS" marker points at your items.
- **Skip:** `milestone grant @s void_reclaimer:first_structure` and `diversified`.

## 5. Asteroids and the Mech
- [ ] `asteroid locate`, fly there. Rock can't be broken by hand or any other tool.
- [ ] **Mechanical Drill** (tool module) breaks it, drops nothing and refunds dust of the rock's kind; **Matter Harvester** drops the block. Both cost energy and stop in low power.
- [ ] Fill any one dust kind to its cap: **Expansion** unlocks the Mech parts, Core Mk II, Storage Mk II, Thruster Mk II.
- [ ] Mech Core Kit (torso, pelvis, head); install the core torso and see the layout change; limbs are single parts.
- **Skip:** `milestone grant @s void_reclaimer:expansion`, then `body preset @s mech`.

## 6. Nebula materials (Create)
You need Create machines for this stage: Basin, Mechanical Press, Millstone, Crushing Wheels, Encased Fan, Blaze Burner, Deployer, Spout, Depot, a rotation source (a creative motor is fine). The Replicator's **Raw** tab makes the four raw items: 8 dust of a kind gives 4 items.
- [ ] **Ice:** Raw Ice Shards > Crushing Wheels > Crushed Cryo-Ice > heated basin > Water > superheated basin > Purified Water. Water Bucket in an Encased Fan blowing through Powder Snow > Packed Ice + empty Bucket (freezing). Packed Ice > Press > Coolant Frame Plate. Purified Water + Crushed Cryo-Ice > basin > Coolant Fluid.
- [ ] **Carbon:** Carbon Flakes > Millstone > Fine Carbon Powder; + Water > basin > Carbon Paste > Press > Carbon Composite Plate; Powder > Press > High-Purity Graphite; Silicon-Carbide Slurry (powder + Liquid Silicon) > Press > Thermal Shielding Tile; Slurry + Composite Plate > compacting > 2 Silicon-Carbide Coating.
- [ ] **Silicon:** Silicon Grit > Encased Fan washing > Pure Silicon Dust > heated basin > Liquid Silicon. A Spout over an **empty** Depot casts 250 mB Liquid Silicon into a Quartz Wafer. Liquid Silicon + Metallic Grains > Doped Silicon Ingot; Wafer + Carbon Thread > Deployer > High-Efficiency Solar Cell; Doped Ingot + Create Electron Tube > Advanced Sensor Unit.
- [ ] **Metallic:** Metallic Grains > Encased Fan blasting > Refined Metal Nodule > Press > Micro-Alloy Sheet.
- [ ] **Electrolyzer** (shaft through two opposite faces, 16 RPM minimum, water in by pipe): 250 mB water makes 125 mB Oxygen + 250 mB Hydrogen. Oxygen + Hydrogen > superheated basin > Nebulite Propellant. **Spinner**: 1 graphite > 2 Carbon Thread.
- [ ] Making one item from each chain (Coolant Frame Plate, Carbon Composite Plate, High-Efficiency Solar Cell, Micro-Alloy Sheet) reaches **Starship** (see stage 8).
- **Skip:** `/give @s void_reclaimer:micro_alloy_sheet 16` (and the other item ids, e.g. `high_purity_graphite`, `carbon_composite_plate`, `high_efficiency_solar_cell`, `thermal_shielding_tile`, `quartz_wafer`, `carbon_thread`), and `/give @s create:precision_mechanism 4`.

## 7. Frame and payloads
Sequenced Assembly on a belt (Deployer, Spout, Press in order; the transitional item shows progress).
- [ ] **Zero-G Precision Frame**: Micro-Alloy Sheet base; Deploy Graphite, Deploy Micro-Alloy Sheet, Press; 2 loops.
- [ ] **Dyson Swarm Satellite Payload**: Frame base; Deploy Composite Plate, Deploy Solar Cell, Spout Liquid Silicon, Press; 2 loops.
- [ ] **Dyson Shell Truss Module**: Frame base; Deploy Thermal Shielding Tile, Deploy Micro-Alloy Sheet, Spout Coolant Fluid, Press; 3 loops.
- [ ] RCS Thruster, Reaction Wheel Gyroscope and Atmospheric Catalyst Core also assemble (they are plain items for now; nothing uses them yet).
- [ ] **Sub-Light Quantum Receiver**: Micro-Alloy Sheet base; Deploy Solar Cell, Deploy Composite Plate, Deploy Precision Mechanism, Spout Coolant Fluid; no press.
- **Skip:** `/give @s void_reclaimer:dyson_swarm_satellite_payload 4`, `.../dyson_shell_truss_module 4`, `.../quantum_receiver`.

## 8. Mass Driver, the first launch
- [ ] Replicator Blocks tab (Diversified): Kinetic Drive Base, Payload Chamber, Magnetic Barrel Nozzle.
- [ ] Stack them vertically: Drive Base at the bottom (shaft through two opposite faces), Payload Chamber above, Nozzle on top. Drive the base with a Create motor.
- [ ] Put a payload in the chamber (right-click or a belt, funnel or hopper). **Nothing happens below 64 RPM**; at 64 RPM the wind-up is 20 s, at 256 RPM about 3 s.
- [ ] On firing: launch sound, cloud particles at the nozzle, and a blue streak across the sky towards the sun. `dyson get` rises by one.
- [ ] Satellites: 1 to 50 is sky tier 1, 51 or more tier 2. Trusses: 50 is tier 3 (swarm lattice), 2000 is tier 4 (shell complete). Each tier adds an AI log entry (placeholder text).
- [ ] The Quantum Receiver turns once any truss is launched; its output grows with the trusses launched; a beam to the sun is drawn.
- **Skip:** `/void_reclaimer dyson set satellites 60`, `... trusses 50`, `... trusses 2000`.

## 9. Starship and bigger bodies
- [ ] **Starship** milestone: hold the four chain items. Unlocks Core Mk III, 14 Ship sections, the Ship Core Kit, and the Shuttle sections.
- [ ] Ship and Shuttle need a Core Mk III. The Titan (Dyson tier 3) needs Core Mk IV. Note: very expensive parts exceed the base dust cap, so build storage pods first (`dust set` is clamped to your capacity).
- [ ] Look at each body in third person (use a lit room; the `full` presets show every module): Drone (mining probe), Mech, Ship, Shuttle, Titan. Camera distance grows with the body.
- **Skip:** `milestone grant @s void_reclaimer:starship`, then `body preset @s ship|shuttle|titan full`.

## 9b. Round 2: screens, cameras and the start of the game
New since the last round. Each box is something to try; tell me what looks wrong or feels off (sizes, speeds and offsets are guesses).
- [ ] **Start:** in a brand-new world the AI log (Replicator Logs tab, and the toast) tells you to press R and build a Dust Sensor. You begin with 25 metallic, 30 silicate and 5 carbon dust, enough for the Dust Sensor, Thruster Mk I, Scaffold and Rotor Arm. **First Dust** and **Diversified** must *not* be unlocked yet; flying through a cloud unlocks them.
- [ ] **Hum:** before you have a Dust Sensor, face a cloud within 512 blocks: you hear the tone (no bar). With the sensor the bar and markers appear.
- [ ] **Dust meter** (top left): icon, bar and number per kind (placeholder icons: diamond, block, cross, disc). It dims after about 10 s without a change; with a screen open it shows the names.
- [ ] **Health bar:** one bar with `health / max` above it, longer and taller for a bigger body; try `body preset @s titan`.
- [ ] **Replicator (R):** each unlocked recipe shows a description when you hover it and in the box under the list; prices are icon plus amount (hover an icon for held / needed); drag a waiting job in the queue to reorder it (the first one never moves); with JEI, hover a result and press R or U.
- [ ] **Inventory (E):** opens our screen: the rows your body has unlocked (the others darker), no armor or crafting slots, a 3D preview of your body and the dust reserves. Shift-click moves between the hotbar and the rows. The Replicator, Body and Logs tabs switch screens; R opens the Replicator.
- [ ] **Death:** `/kill` with a charged Backup Core: the death screen shows a card per choice (name, charge pips, distance) and a placeholder AI line under the score.
- [ ] **Orbit camera:** third person (F5), hold the middle mouse button and move the mouse: the camera swings round the body without turning it; scroll zooms; letting go eases back.
- [ ] **Photo mode (F8):** the HUD hides and the camera is free: drag with the left mouse button to look, WASD and Space/Shift to move (Ctrl fast), scroll for FOV, Q/E to roll, R resets roll, Enter takes a screenshot, Escape or F8 leaves.
- [ ] **Big ships first person:** `body preset @s ship` (also `shuttle`, `titan`), first person (F5 until it cycles): no hand, and the camera is forward of your eyes. Tell me where the camera should sit.
- [ ] **Mech:** `body preset @s mech full`: it leans into its motion (forward, back, sideways), keeps the flying animation while cruising, its flames follow its feet, and swinging a tool moves only the right arm.

## 10. Report
For anything wrong, send: stage number, what you did, what you expected, what happened, the world's mods (Sodium and the others loaded?), a screenshot if visual, and `logs/latest.log` if the game crashed or errors showed in chat.

Known gaps, so you don't report them again: the Mass Driver, machines and the Titan have static models (no spinning), Quantum Receiver tooltip shows the per-RPM figure, first person still shows the vanilla hand for the Drone and the Mech, sounds are vanilla stand-ins, and some item icons are placeholders (`docs/art/ASSETS.md`).
