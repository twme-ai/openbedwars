package io.github.twmeai.openbedwars.message;

import io.github.twmeai.openbedwars.game.GameEventType;
import io.github.twmeai.openbedwars.game.GamePhase;
import io.github.twmeai.openbedwars.game.ResourceType;
import io.github.twmeai.openbedwars.game.TeamColor;
import io.github.twmeai.openbedwars.shop.ShopCategory;
import io.github.twmeai.openbedwars.shop.ShopItem;
import io.github.twmeai.openbedwars.shop.UpgradeType;
import io.github.twmeai.openbedwars.statistics.LeaderboardMetric;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocaleCatalogTest {
    private static final Set<String> SETUP_FIELDS = Set.of(
            "lobby", "spectator", "void_kill_y", "max_build_y",
            "spawn", "item_shop", "upgrade_shop", "forge", "bed", "team"
    );

    @Test
    void bundledLocalesExposeTheSameStringKeys() {
        assertEquals(stringKeys(locale("en_US")), stringKeys(locale("zh_TW")));
    }

    @Test
    void everyDynamicGameNameHasABundledTranslation() {
        for (String locale : Set.of("en_US", "zh_TW")) {
            YamlConfiguration catalog = locale(locale);
            for (GameEventType type : GameEventType.values()) {
                assertTranslation(catalog, type.translationKey());
            }
            for (GamePhase phase : GamePhase.values()) {
                assertTranslation(catalog, phase.translationKey());
            }
            for (TeamColor color : TeamColor.values()) {
                assertTranslation(catalog, color.translationKey());
            }
            for (ResourceType type : ResourceType.values()) {
                assertTranslation(catalog, type.translationKey());
            }
            for (ShopCategory category : ShopCategory.values()) {
                assertTranslation(catalog, category.translationKey());
            }
            for (ShopItem item : ShopItem.values()) {
                assertTranslation(catalog, item.translationKey());
            }
            for (UpgradeType type : UpgradeType.values()) {
                assertTranslation(catalog, type.translationKey());
            }
            for (int tier = 1; tier <= 4; tier++) {
                assertTranslation(catalog, "upgrade.name.forge_" + tier);
                assertTranslation(catalog, "upgrade.effect.forge_" + tier);
            }
            for (LeaderboardMetric metric : LeaderboardMetric.values()) {
                assertTranslation(catalog, metric.translationKey());
            }
            for (String field : SETUP_FIELDS) {
                assertTranslation(catalog, "setup.field." + field);
            }
        }
    }

    @Test
    void finalAndTeamEliminationMessagesAreBundled() {
        for (String locale : Set.of("en_US", "zh_TW")) {
            YamlConfiguration catalog = locale(locale);
            for (String key : Set.of("death.normal-final", "death.killed-final", "team.eliminated")) {
                assertTranslation(catalog, key);
            }
        }
    }

    private void assertTranslation(YamlConfiguration catalog, String key) {
        assertTrue(catalog.isString(key), () -> "Missing translation key: " + key);
        assertTrue(!catalog.getString(key, "").isBlank(), () -> "Blank translation key: " + key);
    }

    private Set<String> stringKeys(YamlConfiguration catalog) {
        Set<String> keys = new TreeSet<>();
        for (String key : catalog.getKeys(true)) {
            if (catalog.isString(key)) keys.add(key);
        }
        return keys;
    }

    private YamlConfiguration locale(String name) {
        InputStream stream = getClass().getResourceAsStream("/lang/" + name + ".yml");
        assertNotNull(stream, "Missing bundled locale " + name);
        return YamlConfiguration.loadConfiguration(new InputStreamReader(stream, StandardCharsets.UTF_8));
    }
}
