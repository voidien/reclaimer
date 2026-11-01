# The CogScript patch: `holo` for Advanced Electricity's computers

A CogScript program can write to Holo Projectors through a small mixin. This file says why that is a patch, what it touches and how to find it again when the addon changes.

## Why a patch
CogScript has no extension API: every module (`create`, `redstone`, `gui`, ...) is hard-coded in one private method. The owner wants programs on these computers to reach the projector as well as CC: Tweaked programs do, so we add one module to that method. Nothing else of the addon changes. For ships CC: Tweaked with Create: Avionics stays the route: this module only talks to projectors.

Both addons are All Rights Reserved. We compile against the jar and inject at run time; we never copy, bundle or redistribute any of it.

## Who owns what
| Jar | Mod id | What is there |
|---|---|---|
| `create-advanced-electricity-addon-*.jar` | `light_bulb_addon_create_crafts_and__addions` | **The interpreter**: `computer.ComputerOS`, `computer.script.*`, the computer block |
| `create-advanced-computer-technic-*.jar` | `advanced_computer_technic` | Only device links (radio, printer, camera hooks into `ComputerLinks.devices`) |

So the patch targets Advanced Electricity, despite this file's name, which the plan chose.

## Classes (package `de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer`)
| Class | Use |
|---|---|
| `ComputerOS` | One running computer, `implements script.Interpreter$Host`. `private void installApi(Interpreter, List<String>)` defines every module; the list is the program's arguments (it becomes the `args` value). `<T> T onMainCall(Supplier<T>)` runs on the server thread and waits. Field `computer`: the block entity |
| `script.Interpreter` | `defineValue(String, Object)`, `define(String, ScriptCallable.Body)`, `run(String)`, `globals()` |
| `script.ScriptMap` | A map value; `fn(name, body)` and `with(name, value)` build modules |
| `script.ScriptList` | A list value |
| `script.ScriptCallable$Body` | `Object call(Interpreter, Args)` |
| `script.ScriptCallable$Args` | `string(i)`, `number(i)`, `integer(i)`, `list(i)`, `get(i)`, `has(i)`, `error(message)` |

## The patch
`mixin/CogScriptHoloMixin` injects at the `TAIL` of `installApi` and calls `compat/act/HoloScript.install`. `TAIL` because `runProgram` calls `installApi` once for each new interpreter, before `run`, so every program gets the module, and because the last define cannot be overwritten by the addon's own. The inject has `require = 0`: if an addon update renames the method, the game still starts and programs get "holo is not defined", and `theCogScriptMixinInstallsTheModule` fails in CI on the update.

Advanced Electricity is `compileOnly` for this; at run time it was already a required mod.

## The `holo` module
A projector's address is `"x,y,z"`. Only projectors within 16 blocks of the computer can be reached (`HoloScript.RANGE`); one out of reach raises a script error, as a bad argument does. Lines count from 1.

| Function | Does |
|---|---|
| `holo.list()` | every address in reach, nearest first |
| `holo.find()` | the nearest address, or nil |
| `holo.set(addr, line, text[, color])` | writes one line; false past the last line |
| `holo.lines(addr, list)` | replaces all the text; long entries wrap |
| `holo.clear(addr)` | removes all the text |
| `holo.color(addr, color)` | colour of lines that have none: `0xRRGGBB` as a number, `"#RRGGBB"` or a dye name |
| `holo.scale(addr, x)`, `holo.height(addr, x)` | as the CC functions, held to their ranges |
| `holo.source(addr, id)` | shows a readout (a bare id means ours) |
| `holo.model(addr, id)` | shows a GeckoLib model (a bare id means ours) |
| `holo.body(addr)` | shows each viewer their own body |
| `holo.globe(addr, name)` | shows `"dyson"` or `"earth"`; false for another name |
| `holo.ship(addr)` | shows a miniature of a ship |
| `holo.map(addr)` | shows a top-down map of the blocks around it |
| `holo.get(addr)` | a map: `lines`, `mode`, `source`, `scale`, `height` |

## The thread rule
Scripts run on a daemon thread. World access must be on the server thread, so every function of the module goes through `onMainCall`, except when already on it: `onMain` queues work for the computer's own tick and waits, which would hang a caller that *is* that thread (a GameTest). `CogScriptHoloMixin` makes that choice with `level.getServer().isSameThread()`. The argument checks run inside the hop, because `Args` only reads its own list.

## Finding it again after an addon update
```
JAR=$(find ~/.gradle/caches -name 'create-advanced-electricity-addon-*.jar' | head -1)
P=de.nightstudio.light_bulb_addon_create_crafts_and__addions.computer
javap -p -cp "$JAR" $P.ComputerOS | grep -E 'installApi|onMainCall|computer;'
javap -p -cp "$JAR" $P.script.Interpreter $P.script.ScriptMap '$P.script.ScriptCallable$Args'
javap -p -c -cp "$JAR" $P.ComputerOS | grep -A70 'boolean runProgram' | grep -E 'Interpreter|installApi'
```
The last line shows `new Interpreter`, then `installApi`, then `run`: that order is what the patch relies on. If the method has moved, change `method` in the mixin and the reflective call in `HoloGameTests`.

## Testing
`tools/gametest.sh main CogScript`: `cogScriptWritesToTheProjector` runs a program in an `Interpreter` on the server thread, `theCogScriptMixinInstallsTheModule` calls the real `installApi` reflectively, and `aProgramOnItsOwnThreadReachesTheProjector` runs one on its own thread through the hop, with the test calling `ComputerOS.tick`.

## Known limits
- Only the 16-block range: no network addressing, and a computer on a ship reaches what is aboard, in plot coordinates.
- The module has no CogScript `help` entry or terminal completion, which live in the addon.
- `holo` shadows any script variable of that name (none in the addon's own programs).
