# Arena Setup

OpenBedWars uses one loaded Minecraft world per arena. Use a clean map copy and keep it separate from survival or lobby worlds. The plugin restores changes it observes during a game; it does not replace the entire world from a template.

## Before setup

1. Put the arena world in the server root.
2. Load that world with your preferred world manager.
3. Stand in the arena world and run `/bw setup create <arena> [playersPerTeam]`.

Arena keys may contain letters, numbers, `_`, and `-`. New arenas remain disabled until validation succeeds.

## Required locations

Set the shared locations while standing at each point:

```text
/bw setup lobby <arena>
/bw setup spectator <arena>
```

Set the instant void-death line while standing at its Y level, then set the first Y level where block placement must be denied:

```text
/bw setup voidheight <arena>
/bw setup buildheight <arena>
```

The defaults are Y 0 for void death and Y 180 for the exclusive build limit. These values are stored per arena as `void-kill-y` and `max-build-y`; the build limit must be above the void line.

Add at least two teams. Supported colors are `red`, `blue`, `green`, `yellow`, `aqua`, `white`, `pink`, and `gray`.

```text
/bw setup addteam <arena> red
/bw setup teamspawn <arena> red
/bw setup itemshop <arena> red
/bw setup upgradeshop <arena> red
/bw setup forge <arena> red
```

For the bed, stand within six blocks, look directly at either half, and run:

```text
/bw setup bed <arena> red
```

Repeat all team commands for every team. `max-players` is updated from the number of teams and the configured players per team.

## Map generators

Stand at the item spawn point and add every diamond and emerald generator:

```text
/bw setup generator <arena> diamond
/bw setup generator <arena> emerald
```

Iron and gold use each team's `forge` location and do not need separate generator commands.

## Validate and enable

```text
/bw setup enable <arena>
```

Enable runs the production arena loader. If any required location is missing or invalid, the arena remains disabled and the console identifies the failing field. No games or waiting players may be active while loaded arenas are enabled, disabled, deleted, or reloaded.

Use `/bw list` to verify the arena, then join with `/bw join <arena>`. Disable or remove an arena with:

```text
/bw setup disable <arena>
/bw setup delete <arena> confirm
```

Deletion only removes the `arenas.yml` section. It does not delete the world.
