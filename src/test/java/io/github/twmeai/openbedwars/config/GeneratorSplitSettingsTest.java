package io.github.twmeai.openbedwars.config;

import io.github.twmeai.openbedwars.game.ResourceType;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratorSplitSettingsTest {
    @Test
    void limitsSplittingToEnabledResources() {
        GameSettings.GeneratorSplitSettings settings = new GameSettings.GeneratorSplitSettings(
                true, 3.0, EnumSet.of(ResourceType.IRON, ResourceType.GOLD));

        assertTrue(settings.splits(ResourceType.IRON));
        assertTrue(settings.splits(ResourceType.GOLD));
        assertFalse(settings.splits(ResourceType.DIAMOND));
    }

    @Test
    void disabledSettingNeverSplitsAndInvalidRadiiAreRejected() {
        GameSettings.GeneratorSplitSettings disabled = new GameSettings.GeneratorSplitSettings(
                false, 3.0, EnumSet.allOf(ResourceType.class));

        assertFalse(disabled.splits(ResourceType.IRON));
        assertThrows(IllegalArgumentException.class, () -> new GameSettings.GeneratorSplitSettings(
                true, 0, EnumSet.of(ResourceType.IRON)));
        assertThrows(IllegalArgumentException.class, () -> new GameSettings.GeneratorSplitSettings(
                true, Double.NaN, EnumSet.of(ResourceType.IRON)));
    }

    @Test
    void missingConfigurationUsesClassicDefaultsAndInvalidResourcesFailClearly() {
        GameSettings defaults = GameSettings.from(new YamlConfiguration());

        assertTrue(defaults.generatorSplitting().splits(ResourceType.IRON));
        assertTrue(defaults.generatorSplitting().splits(ResourceType.GOLD));
        assertFalse(defaults.generatorSplitting().splits(ResourceType.DIAMOND));

        YamlConfiguration invalid = new YamlConfiguration();
        invalid.set("generator-splitting.resources", List.of("coal"));
        assertThrows(IllegalArgumentException.class, () -> GameSettings.from(invalid));
    }
}
