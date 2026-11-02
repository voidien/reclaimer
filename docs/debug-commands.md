# Debug commands

Every debug command, key and script in the project. **Look here before you click through a screen, write a throwaway test or suggest a new tool.** What you want may already exist.

A new command, photo key, test scene or script goes here in the same PR. `DebugCommandsListedTest` fails otherwise.

`<player>` takes a selector such as `@s`. Server commands need op level 2. The longer tools are explained in `CLAUDE.md`, "Debug tools".

## Server commands

All start with `/void_reclaimer`.

### Set up a body
| Command | What it does |
|---|---|
| `body preset <player> drone\|mech\|ship\|shuttle\|titan` | A complete basic body: every socket filled, Mk I core, thrusters and storage |
| `body preset <player> <chassis> full` | The same with the rest of the modules on it, to see them all |
| `body remove <player> <socket>` | Takes a section off into the inventory, with its modules and child sections |
| `body remove <player> <socket> <kind> <index>` | Takes one module off, such as `rotor_front_left thruster 0` |
| `body reset <player>` | Back to the starter Drone |
| `body get <player>` | Size, mass, inventory slots and top speed |
| `wear <player> <condition>` | Sets every section's condition, 1 new and 0 spent |

Socket names and module kinds tab-complete.

### Fill reserves
| Command | What it does |
|---|---|
| `energy get <player>`, `energy set <player> <amount>` | Reads or sets energy |
| `dust get <player>`, `dust set <player> <kind> <amount>` | Reads or sets dust of one kind |
| `core charge <player>` | Fills every Backup Core the player owns |

### Move progress
| Command | What it does |
|---|---|
| `milestone grant <player> <milestone>`, `milestone revoke <player> <milestone>` | A revoked milestone whose condition still holds comes back at the next check |
| `dyson get`, `dyson set satellites <count>`, `dyson set trusses <count>` | Dyson Sphere counters and tier |
| `sky era get`, `sky era set <era>` | Ruined Earth's sky: ruined, healing or restored |
| `sky sectors`, `sky sectors <count>` | Reads restored sectors, or marks the first `<count>` restored; the ground does not change |

### Find things
| Command | What it does |
|---|---|
| `dust locate`, `dust locate <range>` | The nearest cloud from where you are |
| `asteroid locate`, `asteroid locate <size>` | The nearest asteroid in deep space: small, large or giant |

### Ready-made setups
| Command | What it does |
|---|---|
| `scene <name>` | Builds a setup in deep space through the real code and moves you to it (below) |
| `starship spawn` | A placeholder Test Starship 12 blocks ahead |

Scenes: `arena` (a pale lit floor and walls for judging dark bodies and sizes), `launch` (a complete launch complex with payloads), `drones` (helper drones at work), `station` (a station), `speedway` (nine timed gates, with splits in chat).

### Remote control
| Command | What it does |
|---|---|
| `link spawn <body>`, `link spawn <body> full` | A shell ahead of you, without crafting one |
| `link status <player>` | How the player's link stands |
| `link shells <player>` | Every shell the player has owned, where it was last seen and how it stands |
| `link force <player> <shell>` | Links to a shell by selector, with no seat or signal |
| `link reach <player> <shell>` | Links by shell id, wherever it is, loading its chunks first |
| `link wake <player>` | Ends the link and wakes the player |
| `link census <player>` | Counts everything carried, in the Replicator's queue and output, and lying nearby, and what changed since the last count |

### Holo Projector
Writes to a projector with no computer. All start `holo <pos>`:

| Command | What it does |
|---|---|
| `line <line> <text>` | Sets one line of text |
| `scale <scale>` | Sets the size |
| `source <source>` | Shows a data source |
| `billboard <on>` | Turns to face the viewer, or not |
| `model <id>`, `body`, `ship`, `map`, `globe <globe>` | What it shows |
| `clear` | Clears it |

### Readouts
| Command | What it does |
|---|---|
| `debug helm`, `debug helm <player>` | The last two seconds of a Helm's steering, and the lag from asking to turning |
| `debug chunks`, `debug chunks <where>` | One chunk's ticket and status, and the state of the queue |

## Client commands

All start with `/vrphoto`, and they run on the client.

| Command | What it does |
|---|---|
| `at <x> <y> <z> <yaw> <pitch> [fov]` | Puts the camera there |
| `focus crosshair`, `focus body`, `focus <x> <y> <z>` | What the camera orbits |
| `orbit <yaw> <pitch> <distance>` | Moves the camera round the focus |
| `debug true\|false` | The debug layer: hit boxes, chunk borders and a panel of facts |
| `labels true\|false` | Socket and mount names over the model, with positions in a shot's text file |
| `overlay true\|false` | Ship, thruster, helper drone and rotation overlays, and big bodies' hit boxes: green free, red in a block, with a line for where the body points |
| `paint all\|base\|accent` | Draws parts with one of their two passes |
| `time freeze\|step\|slow\|normal` | Drives the server's tick manager |
| `handling`, `handling all` | Mass, speeds, lengths a second, time to top speed and stopping distance, for your body or every built-in one |
| `shot <name>` | A screenshot and its facts |
| `sheet <name> [distance]` | Four views |
| `reload` | Reloads resources |
| `exit` | Leaves photo mode |

## Keys

**Photo mode** (its own key binding):

| Key | What it does |
|---|---|
| Drag, `W` `A` `S` `D`, Space, Shift, `LEFT_CONTROL` | Look, move, up, down, and move fast |
| Scroll | Field of view |
| `Q`, `E`, `R` | Roll, and reset it |
| `G` | Debug layer |
| `L` | Labels |
| `K` | Paint: all, base only, accent only |
| `O` | Overlays |
| `P`, `PERIOD`, `COMMA` | Freeze, step one tick, quarter speed |
| `T` | Chat, returning to the free camera when it closes |
| `ENTER`, `F2` | Take a shot |
| `ESCAPE` | Leave |

**Anywhere:** F6 on any screen outlines its widgets and click regions, red where two overlap, and writes `debug/gui.txt`. Left Alt looks around at the Helm.

## Scripts

### In the running game: `tools/client-test/quick.sh`
| Command | What it does |
|---|---|
| `start [world]` | Starts the headless client and returns when the world has loaded |
| `window [world]` | Your own game in a real window |
| `swap` | Compiles and replaces changed classes in the running game, in about 3 s |
| `reload` | Copies changed assets and lang in, with no screen open |
| `shot NAME` | A screenshot to `build/client-shots/NAME.png` |
| `photo NAME [distance]` | Four views joined into one picture, with the facts printed |
| `compare A B` | How two shots differ |
| `cmd "/command"` | Runs a chat command, with no screen open |
| `gui` | With a screen open, prints its buttons, click regions and overlaps |
| `test NAME` | Runs a GameTest in the live client, to watch it |
| `gclick X Y`, `gsclick X Y`, `ghover X Y`, `gdrag X Y DX DY` | Mouse at GUI coordinates, the second with Shift held |
| `key KEYSYM`, `scroll N` | Presses a key, or scrolls the wheel |
| `stop` | Closes the game |

Run these from bash. In zsh, a variable holding a command does not split into words.

Behind it: `xt.py` (raw X events), `client.sh` (helpers to source), `bootstrap_world.sh` (makes the test world once), `jei_shots.py` (screenshots JEI's recipe views).

### Tests and merging
| Script | What it does |
|---|---|
| `tools/gametest.sh main\|aero [filter]` | Runs GameTests, prints failures and kills the server |
| `tools/restart-test.sh main\|aero <name>` | Runs a test that spans a restart, on a fresh world |
| `tools/verify.sh [all]` | The checks a branch needs before it merges |
| `tools/verified.sh` | Records and reads which commits passed |
| `tools/test_verified.sh` | Tests that record |
| `tools/pr.sh "title" "body"` | Opens a PR against `main` |
| `tools/land.sh N` | Merges a verified PR |
| `tools/timed.sh SECONDS PATTERN command...` | Runs a command with a time limit and a thread dump on a hang |

### Looking things up
| Script | What it does |
|---|---|
| `tools/mcsrc.sh 'text'`, `tools/mcsrc.sh -f Name` | Searches the decompiled Minecraft and NeoForge sources |

`tools/progress_fold.py` folds `docs/progress/` into `PROGRESS.md`. It takes no flags, and any argument runs the real fold, so run it only on purpose. The art tools are in `tools/art/README.md`.
