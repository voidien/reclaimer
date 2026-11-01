# Holo-displays: design plan

Status: **being built** (stage 3 of `docs/design/plans.md` "Next four stages"). This file keeps the interview answers and the
facts checked against the jars; `docs/design/plans.md` "Holo-displays" says what exists. Corrections made while
building are marked **Checked**.

## Owner's answers (2026-10-31)

| Question | Answer |
|---|---|
| What is it | A **hologram projector** block that programs write to |
| Who writes to it | (1) a CC: Tweaked peripheral, (2) a Create Display Link target, (3) a patched CogScript computer, with extensive, repeatable notes on the patch |
| Content | text from programs, built-in readouts, 3D models (the body rig, the Dyson and Earth globes, any GeckoLib model by id, a Sable ship miniature), maps |
| Cameras | none in this feature |
| Look | placeholder now, owner art later |
| Slicing | two big PRs: **PR 1** block + text + readouts + CC + Create link + CogScript patch; **PR 2** models + maps |

## Facts verified against the jars (javap, 2026-10-31)

- The **CogScript interpreter is not in Advanced Computer Technic.** It lives in **Advanced Electricity**
  (`create-advanced-electricity-addon-*.jar`, package `de.nightstudio.light_bulb_addon_create_crafts_and__addions`,
  classes `computer.script.*`, `computer.ComputerOS implements Interpreter$Host`). Advanced Computer Technic only
  installs a device link (radio/printer/camera hooks) into `ComputerLinks.devices`. So the patch targets Advanced
  Electricity's `ComputerOS`, and CLAUDE.md's "we do not patch it" line must be amended when this is built.
- Neither addon registers a Create `DisplaySource` or `DisplayTarget`.
- `ComputerOS.installApi(Interpreter, List<String>)` (private) defines every script module (`create`, `redstone`,
  `gui`, ...); `runProgram` does `new Interpreter(host)` → `installApi` → `run(source)`. Scripts run on a daemon
  thread; world access goes through the package-private `onMainCall(Supplier)`. There is no same-thread shortcut, so
  our module must run directly when `level.getServer().isSameThread()` (the GameTest case) and marshal otherwise.
- Interpreter API: `Interpreter.defineValue(String, Object)`, `ScriptCallable$Body.call(Interpreter, Args)`,
  `ScriptMap.fn/with`. Unverified: which thread `installApi` runs on and what its `List<String>` argument means.
- Both addon jars are version `1.21.1_1.0.0` (CLAUDE.md's toolchain table says 1.0.2 for Advanced Computer Technic:
  fix the row).
- Create 6.0.10 display target: extend `com.simibubi.create.api.behaviour.display.DisplayTarget`, implement
  `acceptText(int line, List<MutableComponent>, DisplayLinkContext)` and
  `provideStats(ctx) -> new DisplayTargetStats(8, 48, this)`; register through a `DeferredRegister<DisplayTarget>` on
  `CreateRegistries.DISPLAY_TARGET` (id `holo_projector`) **and**
  `DisplayTarget.BY_BLOCK_ENTITY.register(ModBlockEntities.HOLO_PROJECTOR.get(), target)` in common setup.
  `DisplayLinkBlockEntity` is driveable without a player: `target(BlockPos)`, public `activeSource`/`targetLine`,
  `updateGatheredData()`. A Nixie Tube (`displayCustomText(String, int)`) is a player-free text source for tests.

## PR 1: block, text, readouts, three writers

**Data model** `display/HoloDisplay` (pure record + codec): up to 8 lines of 48 chars, colour `0xRRGGBB`
(default `0x7FDFFF`), mode `TEXT | READOUT` (PR 2 adds `MODEL | MAP`), source `HELM | MACHINE | PLAYER | DYSON`,
scale 0.25..4, height 0..4, billboard flag.

**Block** `display/HoloProjectorBlock` (`BaseEntityBlock`, `HORIZONTAL_FACING`; sneak-use cycles the source) and
`HoloProjectorBlockEntity` (owner UUID; mutators `setLine/clearLines/setSource/setScale/setHeight/setBillboard`, each
syncing with the Backup Core's update-packet pattern only when something changed; server ticker recomposes READOUT
lines every `Config.HOLO_REFRESH_TICKS` (default 10) through pure `display/Readouts`; inflated
`getRenderBoundingBox`). Registry and datagen like the Helm, placeholder `cubeBottomTop` textures. A program write
switches the projector to TEXT: last writer wins.

**Checked:** the renderer owns `getRenderBoundingBox` in NeoForge 21.1 (`IBlockEntityRendererExtension`), not the block
entity. The source is a registry id, not the `HELM | MACHINE | PLAYER | DYSON` enum above, so addons can add kinds
(`docs/design/extension-points.md`, rule 2). The CC peripheral comes with the block, not after the readouts, by the project's rule.

**Readouts**: a code registry `ResourceLocation -> Readout(sampler(world) -> snapshot record, pure formatter)`.
Kinds: helm, thruster, siphon, electrolyzer, spinner, mass_driver, dyson (needs no source block), spire,
backup_core, player (nearest within 16), cloud, clock, plus a generic capability fallback (energy/fluid). Resync only
when the lines change.

**CC peripheral** `compat/cc/HoloPeripheral`: `getLines/getLine/setLine(n, text[, color])/setLines/clear/setColor/
getScale/setScale/getHeight/setHeight/setBillboard/getMode/getSource/setSource/getSources/getSize`.

**Create Display Link** `compat/create/HoloDisplayTarget` as above.

**CogScript patch**: Advanced Electricity as `compileOnly`; `@Mixin(targets = "...computer.ComputerOS", remap = false)`
with `@Inject(method = "installApi", at = @At("TAIL"))` calling `interp.defineValue("holo", HoloScript.module(...))`;
`compat/act/HoloScript` builds a `ScriptMap` with `list/find/set/lines/clear/color/scale/height/source/get`, addressing
projectors by `"x,y,z"` within 16 blocks of the computer. Document it all in
`docs/ADVANCED_COMPUTER_TECHNIC_PATCH.md`: why, the two jars and who owns what, the class map, the mixin and why
`installApi` TAIL, the `holo` module contract and the thread rule, the re-derive procedure with the exact javap
commands, how to test, known limits (the addons are All Rights Reserved: patch only, never redistribute).

**Rendering** (`client/render/holo/*`): `HoloProjectorRenderer` (`BlockEntityRenderer`, view distance 48,
`shouldRenderOffScreen` for size > 2); `HoloTransform` (pure: lift, scale, spin, billboard by inverting the pose
rotation so it works on moving ships); `HoloStyle` (cyan `0x66E6FF` placeholder, flicker, scanline UV scroll);
`HoloText` (pure layout + `Font.drawInBatch(...)` NORMAL pass plus a faint SEE_THROUGH pass at
`LightTexture.FULL_BRIGHT`); panel and glow quads through `RenderType.entityTranslucentEmissive(texture)` (no depth
write, no cull, always bright; `lightning()` was rejected); `HoloCaches` (WeakHashMap keyed by BE revision). Never
`endBatch` or `Tesselator` in the renderer (ImmediatelyFast and Sodium rules).

**Tests**: unit `HoloTextTest` (wrap, caps, colours incl. dye names), `HoloContentTest` (NBT round trip, line beyond
cap refused), `ReadoutsTest` (helm lines exact, off-ship, every kind fits the cap, unknown kind empty, number
formatting). GameTests `HoloGameTests` (X 98 000+): `projectorPlacesAndSavesItsLines`,
`theHoloProjectorIsAPeripheral` (in `CCPeripheralGameTests`), `aReadoutSourceComposesAndFollowsItsSource`,
`aReadoutOnlyResyncsWhenItsLinesChange` (sync counter), `playerReadoutReadsTheNearestBody`,
`theDysonReadoutNeedsNoSource`, `aCreateDisplayLinkDeliversText` (Nixie tube + link + `updateGatheredData`),
`aDirectAcceptTextKeepsOtherLines`, `cogScriptWritesToTheProjector` (new `ComputerOS` + `Interpreter` + our installer
on the server thread), `theCogScriptMixinInstallsTheModule` (reflective `installApi`; fails "holo is not defined" if
the mixin is gone). Each with a named mutation. Headless client script `tools/client-test/holo_shots.sh` with a debug
command `/void_reclaimer holo <pos> line|scale|source|clear`, one screenshot per writer.

**Docs**: CLAUDE.md settled block "Holo-displays", amend the "we do not patch it" line, fix the toolchain row,
extension-table rows (`holo_readout` registry, `create:display_target` entry, CogScript `holo` module, the mixin),
folder `holo/`; PROGRESS done section + placeholders (`textures/block/holo_projector*.png`, item icon, flat quad
look) + decisions pending (refresh cadence, caps, range, last writer wins); HANDOFF client-check notes;
`docs/CC_PERIPHERALS.md` section; `docs/ADVANCED_COMPUTER_TECHNIC_PATCH.md`.

**Order of work**: block/BE/codec/sync/text/readouts (text + clock)/recipe/debug command + unit tests → readout kinds
→ CC peripheral → Create target → Advanced Electricity `compileOnly` + `HoloScript` + mixin + doc → headless client
shots + docs.

## PR 2: models and maps

**Checked:** `GeoObjectRenderer.render` always moves a model by (0.5, 0.51, 0.5), so `HoloModels` moves it back. `HoloBody` is `BodyRenderer.drawHologram`. A tinted texture alone stays dark on our art, so every model has a second pass in plain cyan.

- `HoloModels` through GeckoLib `GeoObjectRenderer<T>(GeoModel)` with a controller-less `GeoAnimatable` and a
  `HoloGeoModel` (`DefaultedBlockGeoModel` / `DefaultedEntityGeoModel` ids; `GeckoLibCache.getBakedModels()` to check a
  model exists).
- `HoloBody` draws the viewer's own body through `BodyRenderer` with a hologram flag (a true owner's-body hologram
  needs `BodyData` in the BE packet).
- `HoloGlobe` + pure `SphereMesh`: Dyson from `DysonSphereData.of(level)` (tier, shell progress); Earth: only `SkyEra`
  is synced, so carry era + tier bytes in the BE packet.
- `HoloShip`: snapshot a ship's block states from
  `ClientSubLevelContainer.getSubLevel(uuid).getPlot().getLoadedChunks()` (cap 4096 blocks), mesh off-thread with
  `BlockRenderDispatcher.renderBatched` into our own `HoloMesh` recorder, replay per frame into
  `entityTranslucentEmissive(TextureAtlas.LOCATION_BLOCKS)`.
- `HoloMap`: the server composes a packed-MapColor byte image (64x64 default, 128 max; mind chunk-packet size), the
  client overlays ships, entities and dust density (`DustField.of(level).sample`) into a `DynamicTexture` drawn with
  `RenderType.text(id)`.
- Unit-testable: `HoloTextLayout`, `SphereMesh`, `HoloMesh`, `HoloMapImage`, `HoloTransform`.
- Placeholders: `textures/holo/panel.png`, `glow.png`, `globe_*.png`, the cyan constants.

## Open decisions for the owner

- Refresh cadence, line/char caps, the 16-block CogScript range, last-writer-wins: all reversible defaults above.
- Whether the Dyson/Earth globe should reflect live progress or a fixed look.
- Owner art for the projector, the panel and the globes.
