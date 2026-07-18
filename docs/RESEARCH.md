# Research Notes

This document records the behavior sources consulted before implementation. OpenBedWars is an independent implementation and does not copy Hypixel assets or proprietary code.

## Sources consulted on 2026-07-18

- [Paper project setup](https://docs.papermc.io/paper/dev/project-setup/) and [Paper Maven metadata](https://repo.papermc.io/repository/maven-public/io/papermc/paper/paper-api/maven-metadata.xml): Paper exposes `paper-api:1.21.11-R0.1-SNAPSHOT`; Paper 1.20 through 1.21.11 requires Java 21.
- [ScreamingSandals/BedWars](https://github.com/ScreamingSandals/BedWars): established open-source Bed Wars plugin supporting modern Paper. It is used as an architectural and compatibility reference, not as copied source.
- [SBA](https://github.com/pronze/SBA): an open-source extension whose stated purpose is to reproduce Hypixel-like shop, player, event, scoreboard, trap, permanent armor, wooden sword, tool downgrade, and generator behavior.
- [SBA item shop configuration](https://github.com/pronze/SBA/blob/master/plugin/src/main/resources/shops/shop.yml): cross-reference for classic item quantities and prices.
- [SBA team upgrade configuration](https://github.com/pronze/SBA/blob/master/plugin/src/main/resources/shops/upgradeShop.yml): cross-reference for sharpness, protection, haste/efficiency, forge, heal pool, dragon buff, and traps.
- [Screaming BedWars popular-server item shop](https://github.com/ScreamingSandals/BedWars/blob/master/plugin/common/src/main/resources/shop/certain-popular-server/shop.yml) and [upgrade shop](https://github.com/ScreamingSandals/BedWars/blob/master/plugin/common/src/main/resources/shop/certain-popular-server/upgrade-shop.yml): current open-source cross-check for the classic seven-category layout and costs.
- [AzuraBedWars shop interaction](https://github.com/AzuraMC-Network/AzuraBedWars/blob/master/azurabedwars-plugin/core/src/main/java/cc/azuramc/bedwars/listener/player/PlayerInteractShopListener.java): modern open-source reference for tagged item-shop and team-upgrade NPC interaction.
- [Screaming BedWars Bridge Egg](https://github.com/ScreamingSandals/BedWars/blob/master/plugin/common/src/main/java/org/screamingsandals/bedwars/special/BridgeEggImpl.java) and [Pop-up Tower](https://github.com/ScreamingSandals/BedWars/blob/master/plugin/common/src/main/java/org/screamingsandals/bedwars/special/PopUpTowerImpl.java): behavior reference for distance-limited team-colored bridge trails and orientation-aware temporary tower blocks.
- [Paper database guidance](https://docs.papermc.io/paper/dev/using-databases/) and [scheduler guidance](https://docs.papermc.io/paper/dev/scheduler/): SQLite is appropriate for a small file-backed plugin database; prepared statements prevent injection, and database/file access belongs off the main server thread.
- [BedWars1058 default statistics](https://github.com/andrei1058/BedWars1058/blob/master/bedwars-api/src/main/java/com/andrei1058/bedwars/api/arena/stats/DefaultStatistics.java): cross-reference for kills, final kills, deaths, final deaths, and beds destroyed as distinct gameplay statistics.
- [BedWars1058 setup commands](https://github.com/andrei1058/BedWars1058/tree/master/bedwars-plugin/src/main/java/com/andrei1058/bedwars/commands/bedwars/subcmds/sensitive/setup): cross-reference for waiting/spectator positions, team creation, spawns, beds, item/upgrade shops, and generators as the minimum arena setup workflow.
- [BedWars1058 arena feature documentation](https://github.com/andrei1058/BedWars1058/wiki/Arena-Setup): cross-reference for configurable rejoin support that returns disconnected players to an active game.
- [SBA party implementation](https://github.com/pronze/SBA/tree/master/plugin/src/main/java/pronze/hypixelify/commands/party): reference for the Hypixel-style invite, accept, decline, leader, kick, promote, leave, disband, chat, size-limit, and expiring-invitation workflow.

## Initial behavior target

The default configuration follows the familiar standard-mode event sequence: Diamond II at 6:00, Emerald II at 12:00, Diamond III at 18:00, Emerald III at 24:00, bed destruction at 30:00, sudden death at 40:00, and game end at 50:00. Timings remain configurable because Hypixel can rebalance modes independently.

Player behavior includes a wooden sword fallback, permanent armor and shears, persistent pickaxe/axe tiers that downgrade after death, respawning while the team bed exists, final elimination after bed loss, team upgrades, traps, and game-only block rollback.

## Access limitation

The official Hypixel Wiki returned a Cloudflare security challenge to both direct and text-mirror requests during this research pass. Claims that could not be checked there are kept configurable and are cross-checked against the open-source references above. A later pass should re-check official pages when they are accessible.

## Runtime verification

Paper 1.21.11 build 132 was downloaded from the [Paper Downloads API](https://fill.papermc.io/v3/projects/paper/versions/1.21.11/builds). On 2026-07-18 the plugin loaded on that server, generated its resources, executed `bw list` and `bw reload`, and disabled cleanly without a plugin exception.
