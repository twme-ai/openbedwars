package io.github.twmeai.openbedwars.config;

import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GameSettingsTimingTest {
    @Test
    void missingRespawnProtectionSettingUsesThreeSeconds() {
        GameSettings settings = GameSettings.from(new YamlConfiguration());

        assertEquals(3, settings.respawnProtectionSeconds());
    }

    @Test
    void negativeRespawnProtectionTimeIsRejected() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("respawn-protection-seconds", -1);

        assertThrows(IllegalArgumentException.class, () -> GameSettings.from(config));
    }
}
