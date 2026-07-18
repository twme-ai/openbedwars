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

- Isolated arenas with waiting, countdown, running, sudden-death, ending, and reset phases
- Automatic team balancing, beds, respawning, final kills, victory detection, and combat attribution
- Iron, gold, diamond, and emerald generators with timed tiers and forge upgrades
- Hypixel-style item shop categories, permanent armor and shears, tiered tools, potions, and utilities
- Team upgrades, an ordered three-trap queue, heal pool, and Dragon Buff
- Working Fireballs, Bridge Eggs, Bed Bugs, Dream Defenders, Magic Milk, Sponges, and Pop-up Towers
- Per-player MiniMessage locales, English and Traditional Chinese translations, and localized scoreboards
- Asynchronous SQLite statistics for games, wins/losses, kills, final kills/deaths, beds, XP, and Bed Wars levels
- Automatic same-game reconnect with a configurable grace period and delayed snapshot restoration
- Hypixel-style parties with expiring invites, leadership, moderation, chat, and atomic arena joining
- Asynchronous top-10 leaderboards for wins, kills, final kills, beds, and level
- Game block, liquid, container, entity, and inventory cleanup without replacing the arena world

Still planned before the first stable release: broader automated event tests and multi-client soak testing. Exact behavior sources and known research limitations are tracked in [docs/RESEARCH.md](docs/RESEARCH.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
