# Contributing

Thank you for contributing to OpenBedWars.

## Development setup

1. Install JDK 21 or newer.
2. Run `./gradlew build`.
3. Test gameplay changes on Paper 1.21.11 with at least two real clients.

Keep implementation, code comments, commit messages, and documentation in English. Player-facing text must use MiniMessage and be added to every bundled locale file. Add a focused test for pure game rules and include manual verification notes for Bukkit event behavior.

## Behavior changes

Behavior intended to match Hypixel Bed Wars needs a public source. Add the source and the date consulted to `docs/RESEARCH.md`. Prefer official documentation or direct observation; use open-source implementations to cross-check behavior and architecture.

Do not submit Hypixel maps, artwork, sounds, textures, decompiled code, or other proprietary assets.
