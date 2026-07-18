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

Diamond and emerald generators show a rotating resource block, tier, and next-drop countdown while a match is running. The display pauses at `--:--` when the nearby resource cap is full. It can be disabled or repositioned globally in `config.yml`:

```yaml
generator-displays:
  enabled: true
  item-height: 1.35
  text-height: 2.35
```

Iron and gold generated during a match are split once to every active teammate near the collector. The default three-block pickup box and eligible resource types are configurable:

```yaml
generator-splitting:
  enabled: true
  radius: 3.0
  resources:
    - iron
    - gold
```

Valid resource names are `iron`, `gold`, `diamond`, and `emerald`. Only items created by a generator carry split provenance. The marker is removed on the first pickup, so player drops, death loot, Ender Chest drops, and re-dropped generator resources never split again. Inventory overflow is dropped normally at the receiving teammate.

## Protected areas

New arenas protect blocks around team spawns, shops, forges, and map generators. The defaults follow the commonly used Bed Wars protection radii and can be changed per arena in `arenas.yml`:

```yaml
protection:
  spawn-radius: 5
  item-shop-radius: 1
  upgrade-shop-radius: 1
  generator-radius: 1
```

Spawn protection extends the configured radius in every direction. Shop protection extends one block below and four blocks above the recorded location; forge and map-generator protection extends two blocks below and five blocks above it. A radius of `0` disables that protection type. These checks apply to player placement, buckets and flowing liquids, and plugin-generated structures.

## Respawn protection

Match respawn timing and the damage-protection window are configured globally in `config.yml`:

```yaml
respawn-seconds: 5
respawn-protection-seconds: 3
```

After the respawn countdown, a player ignores non-void damage for the configured protection time. Hitting an enemy immediately removes the attacker's protection, while void damage always remains lethal. Set `respawn-protection-seconds` to `0` to disable the window. Protection is removed on death, arena leave, reconnect expiry, and match reset.

## Fireballs

Fireball pacing is configured globally in `config.yml`:

```yaml
fireballs:
  cooldown-ticks: 10
  slowness-amplifier: 3
```

The defaults reproduce the familiar half-second cooldown and Slowness IV after a successful throw. The server enforces the deadline independently of client packets and also sends the native fire-charge cooldown overlay. Attempts during the cooldown do not consume another fireball. Set `cooldown-ticks` to `0` to disable both the cooldown and throw slowness.

Fireballs, arrows, Ender Pearls, Bed Bugs, Bridge Eggs, and other player-fired projectiles are registered to their arena and removed during reset. Placed TNT is primed immediately, attributed to its placer, and included in the same cleanup so it cannot explode after a forced stop.

## Traps

An enemy crossing into the seven-block radius around a team spawn consumes the first trap in that team's ordered queue. Remaining inside the radius does not consume another trap: the enemy must leave and enter again. Queued traps become inactive when the team's bed is destroyed.

Magic Milk suppresses enemy-base entries for 30 seconds. A protected player can cross or move within a base without consuming a trap; if the effect expires while the player is still inside, their next movement can trigger the first queued trap.

## Sudden Death

The `events.sudden-death` time releases one dragon for every team that has not been eliminated. A team that purchased Dragon Buff releases two. Each dragon belongs to that team, continually selects the nearest active enemy, and cannot damage its own players directly or through a dragon fireball.

Sudden Death dragons and their projectiles are non-persistent. Reset removes all loaded tagged dragons in addition to the normal tracked-entity cleanup, so a forced stop or chunk unload cannot leave a Sudden Death entity in the arena world.

## Invisibility

The item-shop invisibility potion keeps the player's real armor equipped while hiding all four armor slots from active enemies. The player, teammates, and spectators continue to receive the real equipment, so armor protection and permanent upgrades remain unchanged. Held items and potion or movement particles are still visible.

Invisibility and its armor override end when the player receives uncancelled damage, lands a hit on an enemy, triggers an Alarm Trap, or the potion expires. Armor is also resynchronized after reconnect, respawn, permanent-armor purchases, and team protection upgrades.

## Spectating

After final elimination, a player enters a flying Adventure observer mode at the arena's spectator position. Active players cannot see or collide with observers, while observers retain visibility of every active player. Observer damage, block and entity interaction, item pickup and drop, and inventory movement are cancelled.

The compass in hotbar slot 1 opens a localized player-head menu containing only online, living players who are neither eliminated nor respawning. Each entry shows current health and food; selecting it teleports the observer to that player and revalidates the target before moving. The red bed in hotbar slot 9 leaves the arena and restores the player's pre-game location, inventory, game mode, flight, collision, effects, and scoreboard. Both items are also restored when an eliminated player reconnects during the game.

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

## Languages

Players can switch between the bundled English and Traditional Chinese catalogs at any time:

```text
/bw language en_US
/bw language zh_TW
```

The selected locale is stored on the player. Arena phases, team colors, timed events, setup fields, shop content, statistics, chat messages, and scoreboard text are resolved separately for every recipient. A language change during a match updates the scoreboard title and lines on the next arena tick.

Custom locale files may override bundled values in `plugins/OpenBedWars/lang/`. Keep their string-key set aligned with `en_US.yml`; missing custom values fall back to the configured fallback locale.
