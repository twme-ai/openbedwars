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
2. Copy the example in `plugins/OpenBedWars/arenas.yml`.
3. Set the arena world and every team, bed, shop, forge, and generator location.
4. Set `enabled: true` and run `/bw reload`.
5. Join with `/bw join <arena>`.

All messages use MiniMessage and live in `plugins/OpenBedWars/lang/`. English (`en_US`) and Traditional Chinese (`zh_TW`) are included. Players select a language with `/bw language <locale>`.

## Status

Implemented in the current development build:

- Isolated arenas with waiting, countdown, running, sudden-death, ending, and reset phases
- Automatic team balancing, beds, respawning, final kills, victory detection, and combat attribution
- Iron, gold, diamond, and emerald generators with timed tiers and forge upgrades
- Hypixel-style item shop categories, permanent armor and shears, tiered tools, potions, and utilities
- Team upgrades, an ordered three-trap queue, heal pool, and Dragon Buff
- Per-player MiniMessage locales, English and Traditional Chinese translations, and localized scoreboards
- Game block, liquid, container, entity, and inventory cleanup without replacing the arena world

Still planned before the first stable release: complete special-item behavior, in-game arena setup tools, persistent statistics and progression, reconnect handling, parties, broader automated event tests, and multi-client soak testing. Exact behavior sources and known research limitations are tracked in [docs/RESEARCH.md](docs/RESEARCH.md).

## License

Licensed under the [Apache License 2.0](LICENSE).
