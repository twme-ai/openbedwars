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

    @Test
    void missingFireballSettingsUseHypixelTiming() {
        GameSettings settings = GameSettings.from(new YamlConfiguration());

        assertEquals(10, settings.fireballs().cooldownTicks());
        assertEquals(3, settings.fireballs().slownessAmplifier());
    }

    @Test
    void customFireballSettingsAreLoaded() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("fireballs.cooldown-ticks", 20);
        config.set("fireballs.slowness-amplifier", 5);

        GameSettings settings = GameSettings.from(config);

        assertEquals(20, settings.fireballs().cooldownTicks());
        assertEquals(5, settings.fireballs().slownessAmplifier());
    }

    @Test
    void invalidFireballSettingsAreRejected() {
        YamlConfiguration negativeCooldown = new YamlConfiguration();
        negativeCooldown.set("fireballs.cooldown-ticks", -1);
        YamlConfiguration invalidAmplifier = new YamlConfiguration();
        invalidAmplifier.set("fireballs.slowness-amplifier", 256);

        assertThrows(IllegalArgumentException.class, () -> GameSettings.from(negativeCooldown));
        assertThrows(IllegalArgumentException.class, () -> GameSettings.from(invalidAmplifier));
    }
}
