# Progress

## Setup (workflow step 1) — done
- `CLAUDE.md` with plan, rules, toolchain versions, and package layout.
- ModDevGradle build (`build.gradle`, `settings.gradle`, `gradle.properties`, Gradle 9.2.1 wrapper).
- `neoforge.mods.toml` template: requires NeoForge 21.1.265+, Minecraft 1.21.1, GeckoLib 4.9.3+; JEI optional.
- Minimal `@Mod` entry class `com.voidreclaimer.VoidReclaimer`.
- Sodium, ModernFix, FerriteCore, and ImmediatelyFast added as dev-run-only (`localRuntime`) dependencies; compatibility rules in `CLAUDE.md`.
- `./gradlew build` passes.

## Phases
- [ ] 1. Deep Space & Spawn
- [ ] 2. Zero-G Flight
- [ ] 3. Cosmic Dust
- [ ] 4. Replicator
- [ ] 5. Modular Body
- [ ] 6. Asteroids
- [ ] 7. Machines
- [ ] 8. Stations & Helpers
- [ ] 9. Ruined Earth
- [ ] 10. Terraforming

## Known issues
- None yet. The game has not been launched (`runClient`), so far only compile and packaging are verified.
