package io.github.twmeai.openbedwars.config;

import io.github.twmeai.openbedwars.game.TeamColor;

import java.util.Objects;

public record TeamDefinition(
        TeamColor color,
        Position spawn,
        Position bedHead,
        Position bedFoot,
        Position itemShop,
        Position upgradeShop,
        Position forge
) {
    public TeamDefinition {
        Objects.requireNonNull(color, "color");
        Objects.requireNonNull(spawn, "spawn");
        Objects.requireNonNull(bedHead, "bedHead");
        Objects.requireNonNull(bedFoot, "bedFoot");
        Objects.requireNonNull(itemShop, "itemShop");
        Objects.requireNonNull(upgradeShop, "upgradeShop");
        Objects.requireNonNull(forge, "forge");
    }
}
