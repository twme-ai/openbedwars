# OpenBedWars

OpenBedWars is an independent, open-source, Hypixel-inspired Bed Wars minigame for Paper 1.21.11. It targets modern servers, uses Adventure MiniMessage for every player-facing message, and supports per-player language selection.

> [!IMPORTANT]
> This project is under active development. The current `0.1.0-SNAPSHOT` series is not ready for a production network. Hypixel is a trademark of Hypixel Inc.; this project is not affiliated with or endorsed by Hypixel Inc.

## Requirements

- Paper 1.21.11
- Java 21 or newer
- A separate world for each arena

## Build

```bash
./gradlew build
```

The plugin JAR is generated in `build/libs/`.

## Configuration

1. Start Paper once with OpenBedWars installed.
2. Load a dedicated arena world.
3. Run `/bw setup create <arena> [playersPerTeam]` and record every required location.
4. Run `/bw setup enable <arena>` to validate and load it.
5. Join with `/bw join <arena>`.

See [docs/SETUP.md](docs/SETUP.md) for the complete command sequence. `arenas.yml` remains available for manual or automated configuration.

All messages use MiniMessage and live in `plugins/OpenBedWars/lang/`. English (`en_US`) and Traditional Chinese (`zh_TW`) are included. Players select a language with `/bw language <locale>`.

## Status

Implemented in the current development build:

- Isolated arenas with waiting, countdown, running, sudden-death, ending, reset, and automatic cross-world release
- Automatic team balancing, beds, respawning with attack-cancelled protection, final kills, one-time team eliminations, victory detection, and combat attribution
- Per-match Ender Chest isolation, final-death forge drops, and killer resource rewards
- Per-arena instant void-death, exclusive build-height limits, and configurable spawn, shop, forge, and generator protection zones
- Capped iron, gold, diamond, and emerald generators with timed tiers, forge upgrades, rotating displays, and same-team resource splitting
- Hypixel-style Quick Buy and item shop categories, container-safe swords and permanent equipment, tiered tools, potions with enemy-hidden armor, and utilities
- Team upgrades, an ordered three-trap queue, heal pool, and team-owned Sudden Death dragons with Dragon Buff
- Cooldown-limited Fireballs plus team-owned Bed Bugs and Dream Defenders, Bridge Eggs, Magic Milk, Sponges, and Pop-up Towers
- Per-player MiniMessage locales, complete English and Traditional Chinese dynamic names, and live-localized scoreboards
- Asynchronous SQLite statistics for games, wins/losses, kills, final kills/deaths, beds, XP, and Bed Wars levels
- Automatic same-game reconnect with a configurable grace period, preserved respawn countdowns, and delayed snapshot restoration
- Final-elimination spectator flight with an active-player teleporter, enemy-hidden observers, and a return-to-lobby item
- Hypixel-style parties with expiring invites, leadership, moderation, chat, and atomic arena joining
- Asynchronous top-10 leaderboards for wins, kills, final kills, beds, and level
- Game block, liquid, container, inventory, and chunk-safe match-spawned entity cleanup, including uncollected block drops

The core match loop has also been exercised with two real clients on Paper 1.21.11, including countdown, team placement, bed destruction, final elimination, victory, player restoration, persistent statistics, and arena rollback. Still planned before the first stable release: broader automated event tests and long-running multi-client soak testing. Exact behavior sources, runtime verification, and known research limitations are tracked in [docs/RESEARCH.md](docs/RESEARCH.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
