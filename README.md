# Create: War Horn

**A Create addon: a kinetic block that breathes compressed air, then screams across the map.**

Connect a rotating shaft to the back to pressurize air inside the block. Once pressure builds up,
a redstone pulse unleashes a massive sonic wave — audible for hundreds of blocks around — and
scrambles anything nearby that hears it.

## Features

- **Kinetic charging** — driven by a shaft from directly behind the block, like a Millstone or
  Saw. RPM only controls *how fast* the tank fills; it never changes the pitch of the final blast.
- **Air-intake particles** — while charging, puffs drift inward across the whole front face, as if
  the horn were inhaling. After it fires, it vents residual air for as long as the blast rings out.
- **Range dial** — a brass dial on top of the block, tuned by holding right-click on it and
  scrolling: exactly Create's own `ScrollOptionBehaviour` widget, the same one a Belt uses for its
  distribution method. Four tiers (Very Short / Short / Medium / Long) trade recharge time for
  audible range:

  | Range | Recharge (min., at 256 RPM) | Audible distance |
  |---|---|---|
  | Very Short | 15 s | 96 blocks |
  | Short | 30 s | 192 blocks |
  | Medium | 45 s | 288 blocks |
  | Long | 60 s | 384 blocks |

  The recharge time is a hard floor, not just a suggestion: it's clamped so charging can never
  finish faster than that, however high the driving RPM goes, which guarantees two blasts can
  never overlap.
- **Redstone-triggered blast** — a rising-edge redstone pulse while armed instantly empties the
  tank and plays the blast sound at a volume matching the chosen range.
- **Scrambles nearby mobs** — anything within the blast's audible range panics and scrambles for
  14 seconds (bosses - Wither, Ender Dragon - are unfazed). A Warden, being especially
  sound-sensitive, also takes 20 damage and panics for twice as long.
- **Draws Stress** — a real consumer on Create's kinetic network (comparable to a Turntable), so
  you need spare Stress capacity to run it.
- **Its own goggle readout** — Engineer's Goggles show the usual Create kinetic stats plus an air
  charge bar, the current range and its audible distance, and a live "ready in Xs" countdown
  computed from the current RPM.
- **Create-style tooltips** — the summary and its highlighted key phrases use Create's own
  `ItemDescription` system and `STANDARD_CREATE` color palette (the same two-tone highlighting as
  e.g. the Wrench), and the Stress Impact bar is Create's own `KineticStats` display.
- **A dedicated advancement tab** — three hidden achievements that reveal themselves as you
  progress: crafting the horn, firing it on Long range (credited to whoever's nearest when it
  fires), and hurting a Warden with the blast.
- **Crafted on a Mechanical Crafter** — a dedicated 3×4 pattern (see below).
- **Lives in Create's own creative tab** — no extra tab clutter.
- Localized in **English, French, Spanish, Mandarin and Hindi**.

## Crafting

Made in a Create **Mechanical Crafter**, using a 3-wide × 4-tall pattern:

| | | |
|---|---|---|
| Red Dye | Goat Horn | Red Dye |
| Brass Sheet | Fluid Tank | Brass Sheet |
| Brass Casing | Precision Mechanism | Brass Casing |
| Fluid Pipe | Shaft | Fluid Pipe |

The recipe is mirror-accepting, so either horizontal orientation of the pattern works.

## Placement

- **Normal placement**: the mouth faces you, like a Furnace.
- **Sneak placement**: the mouth instead faces the same way you're looking.

## Requirements

- Minecraft **1.21.1**
- **NeoForge**
- **Create** (6.0.x or newer for 1.21.1)

JEI is fully supported for browsing the recipe (Create's own JEI plugin picks it up automatically)
but isn't a hard dependency of this mod.

## Compatibility

The War Horn is a plain `HorizontalKineticBlock`, so it assembles onto Create contraptions and
trains - and Create: Aeronautics ship sub-levels - without crashing, the same as any other
undecorated kinetic block. Like most Create blocks without a custom `MovementBehaviour`, it simply
won't tick (charge/fire) *while* it's part of a moving contraption; once it's placed back down it
resumes at its real world position, so every sound, particle and mob-panic effect it computes is
always based on genuine world coordinates rather than stale contraption-local ones.

## Building from source

```bash
./gradlew build
```

The compiled jar will be in `build/libs/`. The only external dependency is Create itself, resolved
straight from Modrinth's public maven - no CurseForge API token needed. See `gradle.properties`
for the pinned version and `build.gradle` for the dependency block.

## License

All Rights Reserved (change this in `gradle.properties` / `mod_license` before publishing if you
want a different license).
