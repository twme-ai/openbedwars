package io.github.twmeai.openbedwars.message;

import io.github.twmeai.openbedwars.game.TeamColor;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.NamespacedKey;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public final class MessageService {
    private static final List<String> BUNDLED_LOCALES = List.of("en_US", "zh_TW");

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private final NamespacedKey localeKey;
    private final Map<String, Map<String, String>> translations = new HashMap<>();
    private String defaultLocale;
    private String fallbackLocale;

    public MessageService(JavaPlugin plugin) {
        this.plugin = plugin;
        this.localeKey = new NamespacedKey(plugin, "locale");
        reload();
    }

    public void reload() {
        defaultLocale = plugin.getConfig().getString("default-locale", "en_US");
        fallbackLocale = plugin.getConfig().getString("fallback-locale", "en_US");
        File directory = new File(plugin.getDataFolder(), "lang");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create the language directory");
        }
        for (String locale : BUNDLED_LOCALES) {
            File target = new File(directory, locale + ".yml");
            if (!target.exists()) {
                plugin.saveResource("lang/" + locale + ".yml", false);
            }
        }

        translations.clear();
        File[] files = directory.listFiles((ignored, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                loadLocale(file.getName().substring(0, file.getName().length() - 4), file);
            }
        }
        if (!translations.containsKey(fallbackLocale)) {
            throw new IllegalStateException("Fallback locale " + fallbackLocale + " is not installed");
        }
    }

    public Set<String> availableLocales() {
        return Set.copyOf(new TreeSet<>(translations.keySet()));
    }

    public boolean setLocale(Player player, String requestedLocale) {
        String locale = findLocale(requestedLocale);
        if (locale == null) {
            return false;
        }
        player.getPersistentDataContainer().set(localeKey, PersistentDataType.STRING, locale);
        return true;
    }

    public String localeOf(CommandSender sender) {
        if (sender instanceof Player player) {
            String stored = player.getPersistentDataContainer().get(localeKey, PersistentDataType.STRING);
            if (stored != null && translations.containsKey(stored)) {
                return stored;
            }
            String clientLocale = findLocale(player.locale().toString());
            if (clientLocale != null) {
                return clientLocale;
            }
        }
        return translations.containsKey(defaultLocale) ? defaultLocale : fallbackLocale;
    }

    public Component render(CommandSender sender, String key, TagResolver... resolvers) {
        String template = translation(localeOf(sender), key);
        return miniMessage.deserialize(template, TagResolver.resolver(resolvers));
    }

    public Component render(String locale, String key, TagResolver... resolvers) {
        return miniMessage.deserialize(translation(locale, key), TagResolver.resolver(resolvers));
    }

    public void send(CommandSender sender, String key, TagResolver... resolvers) {
        Component prefix = render(sender, "prefix");
        sender.sendMessage(prefix.append(render(sender, key, resolvers)));
    }

    public static TagResolver text(String name, String value) {
        return Placeholder.unparsed(name, value);
    }

    public static TagResolver number(String name, int value) {
        return Placeholder.unparsed(name, Integer.toString(value));
    }

    public static TagResolver component(String name, Component value) {
        return Placeholder.component(name, value);
    }

    public static TagResolver color(String name, TextColor color) {
        return TagResolver.resolver(name, Tag.styling(color));
    }

    public static TagResolver teamColor(String name, TeamColor color) {
        return color(name, color.textColor());
    }

    private String translation(String locale, String key) {
        Map<String, String> selected = translations.getOrDefault(locale, translations.get(fallbackLocale));
        String value = selected.get(key);
        if (value == null) {
            value = translations.get(fallbackLocale).get(key);
        }
        if (value == null) {
            plugin.getLogger().warning("Missing translation key '" + key + "'");
            return "<red>Missing translation: " + key + "</red>";
        }
        return value;
    }

    private void loadLocale(String locale, File file) {
        Map<String, String> values = new HashMap<>();
        try (InputStream resource = plugin.getResource("lang/" + locale + ".yml")) {
            if (resource != null) {
                YamlConfiguration bundled = YamlConfiguration.loadConfiguration(
                        new InputStreamReader(resource, StandardCharsets.UTF_8));
                collectStrings(bundled, values);
            }
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("Could not load bundled locale " + locale, exception);
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        collectStrings(yaml, values);
        translations.put(locale, Map.copyOf(values));
    }

    private void collectStrings(YamlConfiguration yaml, Map<String, String> values) {
        for (String key : yaml.getKeys(true)) {
            if (yaml.isString(key)) {
                values.put(key, yaml.getString(key, ""));
            }
        }
    }

    private String findLocale(String requested) {
        if (requested == null) {
            return null;
        }
        String normalized = requested.replace('-', '_');
        for (String locale : translations.keySet()) {
            if (locale.equalsIgnoreCase(normalized)) {
                return locale;
            }
        }
        String language = normalized.split("_", 2)[0].toLowerCase(Locale.ROOT);
        return translations.keySet().stream()
                .filter(locale -> locale.toLowerCase(Locale.ROOT).startsWith(language + "_"))
                .sorted()
                .findFirst()
                .orElse(null);
    }
}
