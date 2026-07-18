package io.github.twmeai.openbedwars.config;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.game.ResourceType;
import io.github.twmeai.openbedwars.game.TeamColor;
import io.github.twmeai.openbedwars.message.MessageService;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Bed;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ArenaSetupService {
    private static final List<String> OPERATIONS = List.of(
            "create", "lobby", "spectator", "addteam", "teamspawn", "bed",
            "itemshop", "upgradeshop", "forge", "generator", "enable", "disable", "delete"
    );

    private final OpenBedWarsPlugin plugin;
    private final ArenaManager arenas;
    private final File file;

    public ArenaSetupService(OpenBedWarsPlugin plugin, ArenaManager arenas) {
        this.plugin = plugin;
        this.arenas = arenas;
        this.file = new File(plugin.getDataFolder(), "arenas.yml");
    }

    public void execute(Player player, String[] args) {
        if (!player.hasPermission("openbedwars.admin")) {
            plugin.messages().send(player, "error.no-permission");
            return;
        }
        if (args.length < 2) {
            plugin.messages().send(player, "setup.usage");
            return;
        }
        String operation = args[1].toLowerCase(Locale.ROOT);
        try {
            switch (operation) {
                case "create" -> create(player, args);
                case "lobby" -> location(player, args, "lobby");
                case "spectator" -> location(player, args, "spectator");
                case "addteam" -> addTeam(player, args);
                case "teamspawn" -> teamLocation(player, args, "spawn");
                case "bed" -> bed(player, args);
                case "itemshop" -> teamLocation(player, args, "item-shop");
                case "upgradeshop" -> teamLocation(player, args, "upgrade-shop");
                case "forge" -> teamLocation(player, args, "forge");
                case "generator" -> generator(player, args);
                case "enable" -> enable(player, args);
                case "disable" -> disable(player, args);
                case "delete" -> delete(player, args);
                default -> plugin.messages().send(player, "setup.usage");
            }
        } catch (IOException | InvalidConfigurationException exception) {
            plugin.getLogger().log(java.util.logging.Level.SEVERE, "Could not update arenas.yml", exception);
            plugin.messages().send(player, "setup.save-failed");
        }
    }

    public List<String> tabComplete(String[] args) {
        if (args.length == 2) return matches(OPERATIONS, args[1]);
        if (args.length == 3 && !args[1].equalsIgnoreCase("create")) {
            return matches(arenaKeys(), args[2]);
        }
        if (args.length == 4 && List.of(
                "addteam", "teamspawn", "bed", "itemshop", "upgradeshop", "forge"
        ).contains(args[1].toLowerCase(Locale.ROOT))) {
            return matches(java.util.Arrays.stream(TeamColor.values()).map(TeamColor::key).toList(), args[3]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("generator")) {
            return matches(List.of("diamond", "emerald"), args[3]);
        }
        if (args.length == 4 && args[1].equalsIgnoreCase("delete")) {
            return matches(List.of("confirm"), args[3]);
        }
        return List.of();
    }

    private void create(Player player, String[] args) throws IOException, InvalidConfigurationException {
        if (args.length < 3 || !args[2].matches("[a-zA-Z0-9_-]+")) {
            plugin.messages().send(player, "setup.usage");
            return;
        }
        String key = args[2].toLowerCase(Locale.ROOT);
        int playersPerTeam = 1;
        if (args.length >= 4) {
            try {
                playersPerTeam = Integer.parseInt(args[3]);
            } catch (NumberFormatException ignored) {
                playersPerTeam = 0;
            }
        }
        if (playersPerTeam < 1 || playersPerTeam > 16) {
            plugin.messages().send(player, "setup.invalid-team-size");
            return;
        }
        YamlConfiguration yaml = load();
        String root = "arenas." + key;
        if (yaml.isConfigurationSection(root)) {
            plugin.messages().send(player, "setup.exists", MessageService.text("arena", key));
            return;
        }
        yaml.set(root + ".enabled", false);
        yaml.set(root + ".display-name", args[2]);
        yaml.set(root + ".world", player.getWorld().getName());
        yaml.set(root + ".min-players", 2);
        yaml.set(root + ".max-players", playersPerTeam * 2);
        yaml.set(root + ".players-per-team", playersPerTeam);
        save(yaml);
        plugin.messages().send(player, "setup.created", MessageService.text("arena", key));
    }

    private void location(Player player, String[] args, String field) throws IOException, InvalidConfigurationException {
        SetupContext context = context(player, args, 3);
        if (context == null) return;
        context.yaml().set(context.path() + "." + field, serialize(player.getLocation(), true));
        save(context.yaml());
        saved(player, field, context.key());
    }

    private void addTeam(Player player, String[] args) throws IOException, InvalidConfigurationException {
        SetupContext context = context(player, args, 4);
        TeamColor color = teamColor(player, args, 3);
        if (context == null || color == null) return;
        String path = context.path() + ".teams." + color.key();
        if (context.yaml().isConfigurationSection(path)) {
            plugin.messages().send(player, "setup.team-exists", MessageService.text("team", color.displayName()));
            return;
        }
        context.yaml().createSection(path);
        int teams = context.yaml().getConfigurationSection(context.path() + ".teams").getKeys(false).size();
        int playersPerTeam = context.yaml().getInt(context.path() + ".players-per-team", 1);
        context.yaml().set(context.path() + ".max-players", teams * playersPerTeam);
        save(context.yaml());
        plugin.messages().send(player, "setup.team-added",
                MessageService.text("team", color.displayName()), MessageService.text("arena", context.key()));
    }

    private void teamLocation(Player player, String[] args, String field) throws IOException, InvalidConfigurationException {
        SetupContext context = context(player, args, 4);
        TeamColor color = teamColor(player, args, 3);
        if (context == null || color == null || !requireTeam(player, context, color)) return;
        context.yaml().set(context.path() + ".teams." + color.key() + "." + field,
                serialize(player.getLocation(), true));
        save(context.yaml());
        saved(player, color.displayName() + " " + field, context.key());
    }

    private void bed(Player player, String[] args) throws IOException, InvalidConfigurationException {
        SetupContext context = context(player, args, 4);
        TeamColor color = teamColor(player, args, 3);
        if (context == null || color == null || !requireTeam(player, context, color)) return;
        Block selected = player.getTargetBlockExact(6);
        if (selected == null || !(selected.getBlockData() instanceof Bed data)) {
            plugin.messages().send(player, "setup.look-at-bed");
            return;
        }
        Block head;
        Block foot;
        if (data.getPart() == Bed.Part.HEAD) {
            head = selected;
            foot = selected.getRelative(data.getFacing().getOppositeFace());
        } else {
            foot = selected;
            head = selected.getRelative(data.getFacing());
        }
        String teamPath = context.path() + ".teams." + color.key();
        context.yaml().set(teamPath + ".bed-head", serialize(head.getLocation(), false));
        context.yaml().set(teamPath + ".bed-foot", serialize(foot.getLocation(), false));
        save(context.yaml());
        saved(player, color.displayName() + " bed", context.key());
    }

    private void generator(Player player, String[] args) throws IOException, InvalidConfigurationException {
        SetupContext context = context(player, args, 4);
        if (context == null) return;
        ResourceType type;
        try {
            type = ResourceType.valueOf(args[3].toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            plugin.messages().send(player, "setup.invalid-generator");
            return;
        }
        if (type != ResourceType.DIAMOND && type != ResourceType.EMERALD) {
            plugin.messages().send(player, "setup.invalid-generator");
            return;
        }
        String path = context.path() + ".generators." + type.name().toLowerCase(Locale.ROOT);
        List<Map<?, ?>> locations = new ArrayList<>(context.yaml().getMapList(path));
        locations.add(serialize(player.getLocation(), false));
        context.yaml().set(path, locations);
        save(context.yaml());
        plugin.messages().send(player, "setup.generator-added",
                MessageService.text("type", type.name().toLowerCase(Locale.ROOT)),
                MessageService.text("arena", context.key()));
    }

    private void enable(Player player, String[] args) throws IOException, InvalidConfigurationException {
        SetupContext context = context(player, args, 3, false);
        if (context == null || !requireIdle(player)) return;
        context.yaml().set(context.path() + ".enabled", true);
        save(context.yaml());
        if (!new ArenaConfigRepository(plugin).load().containsKey(context.key())) {
            YamlConfiguration reverted = load();
            reverted.set(context.path() + ".enabled", false);
            save(reverted);
            plugin.messages().send(player, "setup.incomplete", MessageService.text("arena", context.key()));
            return;
        }
        arenas.reload();
        plugin.messages().send(player, "setup.enabled", MessageService.text("arena", context.key()));
    }

    private void disable(Player player, String[] args) throws IOException, InvalidConfigurationException {
        SetupContext context = context(player, args, 3, false);
        if (context == null || !requireIdle(player)) return;
        context.yaml().set(context.path() + ".enabled", false);
        save(context.yaml());
        arenas.reload();
        plugin.messages().send(player, "setup.disabled", MessageService.text("arena", context.key()));
    }

    private void delete(Player player, String[] args) throws IOException, InvalidConfigurationException {
        SetupContext context = context(player, args, 3, false);
        if (context == null || !requireIdle(player)) return;
        if (args.length < 4 || !args[3].equalsIgnoreCase("confirm")) {
            plugin.messages().send(player, "setup.delete-confirm", MessageService.text("arena", context.key()));
            return;
        }
        context.yaml().set(context.path(), null);
        save(context.yaml());
        arenas.reload();
        plugin.messages().send(player, "setup.deleted", MessageService.text("arena", context.key()));
    }

    private SetupContext context(Player player, String[] args, int requiredLength)
            throws IOException, InvalidConfigurationException {
        return context(player, args, requiredLength, true);
    }

    private SetupContext context(Player player, String[] args, int requiredLength, boolean requireWorld)
            throws IOException, InvalidConfigurationException {
        if (args.length < requiredLength) {
            plugin.messages().send(player, "setup.usage");
            return null;
        }
        String key = args[2].toLowerCase(Locale.ROOT);
        YamlConfiguration yaml = load();
        String path = "arenas." + key;
        if (!yaml.isConfigurationSection(path)) {
            plugin.messages().send(player, "error.arena-not-found", MessageService.text("arena", key));
            return null;
        }
        String world = yaml.getString(path + ".world");
        if (requireWorld && !player.getWorld().getName().equals(world)) {
            plugin.messages().send(player, "setup.wrong-world", MessageService.text("world", world == null ? "" : world));
            return null;
        }
        return new SetupContext(key, path, yaml);
    }

    private TeamColor teamColor(Player player, String[] args, int index) {
        if (args.length <= index) {
            plugin.messages().send(player, "setup.usage");
            return null;
        }
        TeamColor color = TeamColor.fromKey(args[index]).orElse(null);
        if (color == null) plugin.messages().send(player, "error.invalid-team");
        return color;
    }

    private boolean requireTeam(Player player, SetupContext context, TeamColor color) {
        if (!context.yaml().isConfigurationSection(context.path() + ".teams." + color.key())) {
            plugin.messages().send(player, "setup.team-missing", MessageService.text("team", color.displayName()));
            return false;
        }
        return true;
    }

    private boolean requireIdle(Player player) {
        if (arenas.hasActiveGames()) {
            plugin.messages().send(player, "setup.active-games");
            return false;
        }
        return true;
    }

    private YamlConfiguration load() throws IOException, InvalidConfigurationException {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.options().parseComments(true);
        yaml.load(file);
        return yaml;
    }

    private void save(YamlConfiguration yaml) throws IOException {
        yaml.save(file);
    }

    private Map<String, Object> serialize(Location location, boolean rotation) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("x", rotation ? location.getX() : location.getBlockX());
        values.put("y", rotation ? location.getY() : location.getBlockY());
        values.put("z", rotation ? location.getZ() : location.getBlockZ());
        if (rotation) {
            values.put("yaw", location.getYaw());
            values.put("pitch", location.getPitch());
        }
        return values;
    }

    private Collection<String> arenaKeys() {
        try {
            ConfigurationSection section = load().getConfigurationSection("arenas");
            return section == null ? List.of() : section.getKeys(false);
        } catch (IOException | InvalidConfigurationException exception) {
            return List.of();
        }
    }

    private void saved(Player player, String field, String arena) {
        plugin.messages().send(player, "setup.saved",
                MessageService.text("field", field), MessageService.text("arena", arena));
    }

    private List<String> matches(Collection<String> values, String prefix) {
        return values.stream()
                .filter(value -> value.regionMatches(true, 0, prefix, 0, prefix.length()))
                .sorted()
                .toList();
    }

    private record SetupContext(String key, String path, YamlConfiguration yaml) {
    }
}
