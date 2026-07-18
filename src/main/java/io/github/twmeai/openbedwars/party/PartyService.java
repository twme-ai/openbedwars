package io.github.twmeai.openbedwars.party;

import io.github.twmeai.openbedwars.OpenBedWarsPlugin;
import io.github.twmeai.openbedwars.game.Arena;
import io.github.twmeai.openbedwars.game.ArenaManager;
import io.github.twmeai.openbedwars.message.MessageService;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class PartyService {
    private static final List<String> SUBCOMMANDS = List.of(
            "invite", "accept", "decline", "leave", "kick", "promote", "disband", "list", "chat", "help"
    );

    private final OpenBedWarsPlugin plugin;
    private final ArenaManager arenas;
    private final Map<UUID, Party> partyByMember = new HashMap<>();
    private final Map<UUID, Invitation> invitations = new HashMap<>();

    public PartyService(OpenBedWarsPlugin plugin, ArenaManager arenas) {
        this.plugin = plugin;
        this.arenas = arenas;
    }

    public void execute(Player sender, String[] args, int offset) {
        if (!sender.hasPermission("openbedwars.play")) {
            plugin.messages().send(sender, "error.no-permission");
            return;
        }
        if (args.length <= offset || args[offset].equalsIgnoreCase("help")) {
            plugin.messages().send(sender, "party.help");
            return;
        }
        String subcommand = args[offset].toLowerCase(Locale.ROOT);
        switch (subcommand) {
            case "invite" -> invite(sender, argument(args, offset + 1));
            case "accept" -> accept(sender);
            case "decline" -> decline(sender);
            case "leave" -> leave(sender);
            case "kick" -> kick(sender, argument(args, offset + 1));
            case "promote" -> promote(sender, argument(args, offset + 1));
            case "disband" -> disband(sender);
            case "list" -> list(sender);
            case "chat" -> chat(sender, joinArguments(args, offset + 1));
            default -> plugin.messages().send(sender, "party.help");
        }
    }

    public List<String> tabComplete(Player sender, String[] args, int offset) {
        if (args.length == offset + 1) return matches(SUBCOMMANDS, args[offset]);
        if (args.length == offset + 2 && args[offset].equalsIgnoreCase("invite")) {
            return matches(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList(), args[offset + 1]);
        }
        if (args.length == offset + 2 && (args[offset].equalsIgnoreCase("kick")
                || args[offset].equalsIgnoreCase("promote"))) {
            Party party = partyByMember.get(sender.getUniqueId());
            return party == null ? List.of() : matches(party.members().values(), args[offset + 1]);
        }
        return List.of();
    }

    public void joinArena(Player requester, Arena arena) {
        Party party = partyByMember.get(requester.getUniqueId());
        if (party == null) {
            sendJoinResult(requester, arena, arenas.join(requester, arena));
            return;
        }
        if (!party.leader().equals(requester.getUniqueId())) {
            plugin.messages().send(requester, "party.leader-must-join");
            return;
        }
        List<Player> online = onlineMembers(party);
        if (online.stream().anyMatch(player -> arenas.arenaOf(player).isPresent())) {
            plugin.messages().send(requester, "party.member-busy");
            return;
        }
        Arena.JoinResult result = arenas.joinGroup(online, arena);
        if (result == Arena.JoinResult.SUCCESS) {
            for (Player player : online) {
                plugin.messages().send(player, "arena.joined",
                        MessageService.text("arena", arena.displayName()),
                        MessageService.number("current", arena.playerCount()),
                        MessageService.number("maximum", arena.maxPlayers()));
            }
            sendParty(party, "party.joined-arena", MessageService.text("arena", arena.displayName()));
        } else {
            sendJoinResult(requester, arena, result);
        }
    }

    public void joinRandomArena(Player requester) {
        Party party = partyByMember.get(requester.getUniqueId());
        if (party != null && !party.leader().equals(requester.getUniqueId())) {
            plugin.messages().send(requester, "party.leader-must-join");
            return;
        }
        List<Player> online = party == null ? List.of(requester) : onlineMembers(party);
        if (party != null && online.stream().anyMatch(player -> arenas.arenaOf(player).isPresent())) {
            plugin.messages().send(requester, "party.member-busy");
            return;
        }
        Arena arena = arenas.bestAvailableArena(online.size()).orElse(null);
        if (arena == null) {
            plugin.messages().send(requester, "error.no-arena-available");
            return;
        }
        joinArena(requester, arena);
    }

    public void shutdown() {
        for (Invitation invitation : invitations.values()) invitation.task().cancel();
        invitations.clear();
        partyByMember.clear();
    }

    private void invite(Player sender, String targetName) {
        if (targetName == null) {
            plugin.messages().send(sender, "party.help");
            return;
        }
        Player target = Bukkit.getPlayerExact(targetName);
        if (target == null) {
            plugin.messages().send(sender, "party.player-not-found", MessageService.text("player", targetName));
            return;
        }
        if (target.equals(sender)) {
            plugin.messages().send(sender, "party.cannot-invite-self");
            return;
        }
        if (partyByMember.containsKey(target.getUniqueId())) {
            plugin.messages().send(sender, "party.already-in-party", MessageService.text("player", target.getName()));
            return;
        }
        if (invitations.containsKey(target.getUniqueId())) {
            plugin.messages().send(sender, "party.already-invited", MessageService.text("player", target.getName()));
            return;
        }
        Party party = partyByMember.get(sender.getUniqueId());
        if (party != null && !party.leader().equals(sender.getUniqueId())) {
            plugin.messages().send(sender, "party.not-leader");
            return;
        }
        if (party == null) {
            party = new Party(sender.getUniqueId(), sender.getName());
            partyByMember.put(sender.getUniqueId(), party);
        }
        Party activeParty = party;
        int pending = (int) invitations.values().stream()
                .filter(invitation -> invitation.party() == activeParty)
                .count();
        int maxSize = plugin.getConfig().getInt("party.max-size", 4);
        if (party.size() + pending >= maxSize) {
            plugin.messages().send(sender, "party.full", MessageService.number("maximum", maxSize));
            return;
        }
        int expiry = Math.max(1, plugin.getConfig().getInt("party.invite-expiration-seconds", 60));
        Party invitedParty = activeParty;
        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin,
                () -> expireInvite(target.getUniqueId()), expiry * 20L);
        invitations.put(target.getUniqueId(), new Invitation(invitedParty, sender.getUniqueId(), target.getName(), task));
        plugin.messages().send(sender, "party.invite-sent", MessageService.text("player", target.getName()));
        plugin.messages().send(target, "party.invite-received",
                MessageService.text("player", sender.getName()), MessageService.number("seconds", expiry));
    }

    private void accept(Player player) {
        Invitation invitation = invitations.remove(player.getUniqueId());
        if (invitation == null) {
            plugin.messages().send(player, "party.not-invited");
            return;
        }
        invitation.task().cancel();
        Party party = invitation.party();
        int maxSize = plugin.getConfig().getInt("party.max-size", 4);
        if (party.size() >= maxSize || !partyByMember.containsValue(party)) {
            plugin.messages().send(player, "party.invite-expired");
            return;
        }
        party.add(player.getUniqueId(), player.getName());
        partyByMember.put(player.getUniqueId(), party);
        sendParty(party, "party.joined", MessageService.text("player", player.getName()));
    }

    private void decline(Player player) {
        Invitation invitation = invitations.remove(player.getUniqueId());
        if (invitation == null) {
            plugin.messages().send(player, "party.not-invited");
            return;
        }
        invitation.task().cancel();
        plugin.messages().send(player, "party.declined");
        Player inviter = Bukkit.getPlayer(invitation.inviter());
        if (inviter != null) {
            plugin.messages().send(inviter, "party.invite-declined",
                    MessageService.text("player", player.getName()));
        }
        removeEmptyParty(invitation.party());
    }

    private void leave(Player player) {
        Party party = partyByMember.get(player.getUniqueId());
        if (party == null) {
            plugin.messages().send(player, "party.not-in-party");
            return;
        }
        if (party.size() == 1) {
            disbandParty(party);
            return;
        }
        party.remove(player.getUniqueId());
        partyByMember.remove(player.getUniqueId());
        sendParty(party, "party.left", MessageService.text("player", player.getName()));
        plugin.messages().send(player, "party.you-left");
        if (party.leader().equals(player.getUniqueId())) {
            party.leader(party.firstMember());
            sendParty(party, "party.promoted", MessageService.text("player", party.name(party.leader())));
        }
    }

    private void kick(Player sender, String targetName) {
        Party party = requireLeader(sender);
        if (party == null || targetName == null) return;
        UUID targetId = memberByName(party, targetName);
        if (targetId == null || targetId.equals(sender.getUniqueId())) {
            plugin.messages().send(sender, "party.member-not-found", MessageService.text("player", targetName));
            return;
        }
        String name = party.name(targetId);
        party.remove(targetId);
        partyByMember.remove(targetId);
        sendParty(party, "party.kicked", MessageService.text("player", name));
        Player target = Bukkit.getPlayer(targetId);
        if (target != null) plugin.messages().send(target, "party.you-were-kicked");
    }

    private void promote(Player sender, String targetName) {
        Party party = requireLeader(sender);
        if (party == null || targetName == null) return;
        UUID targetId = memberByName(party, targetName);
        if (targetId == null) {
            plugin.messages().send(sender, "party.member-not-found", MessageService.text("player", targetName));
            return;
        }
        party.leader(targetId);
        sendParty(party, "party.promoted", MessageService.text("player", party.name(targetId)));
    }

    private void disband(Player sender) {
        Party party = requireLeader(sender);
        if (party != null) disbandParty(party);
    }

    private void list(Player sender) {
        Party party = partyByMember.get(sender.getUniqueId());
        if (party == null) {
            plugin.messages().send(sender, "party.not-in-party");
            return;
        }
        String members = String.join(", ", party.members().values());
        plugin.messages().send(sender, "party.list",
                MessageService.text("leader", party.name(party.leader())),
                MessageService.text("members", members),
                MessageService.number("current", party.size()),
                MessageService.number("maximum", plugin.getConfig().getInt("party.max-size", 4)));
    }

    private void chat(Player sender, String content) {
        Party party = partyByMember.get(sender.getUniqueId());
        if (party == null) {
            plugin.messages().send(sender, "party.not-in-party");
            return;
        }
        if (content == null || content.isBlank()) {
            plugin.messages().send(sender, "party.help");
            return;
        }
        sendParty(party, "party.chat",
                MessageService.text("player", sender.getName()), MessageService.text("message", content));
    }

    private Party requireLeader(Player sender) {
        Party party = partyByMember.get(sender.getUniqueId());
        if (party == null) {
            plugin.messages().send(sender, "party.not-in-party");
            return null;
        }
        if (!party.leader().equals(sender.getUniqueId())) {
            plugin.messages().send(sender, "party.not-leader");
            return null;
        }
        return party;
    }

    private List<Player> onlineMembers(Party party) {
        return party.members().keySet().stream()
                .map(Bukkit::getPlayer)
                .filter(java.util.Objects::nonNull)
                .filter(Player::isOnline)
                .toList();
    }

    private void disbandParty(Party party) {
        sendParty(party, "party.disbanded");
        for (UUID member : party.members().keySet()) partyByMember.remove(member);
        invitations.entrySet().removeIf(entry -> {
            if (entry.getValue().party() != party) return false;
            entry.getValue().task().cancel();
            return true;
        });
    }

    private void expireInvite(UUID targetId) {
        Invitation invitation = invitations.remove(targetId);
        if (invitation == null) return;
        Player target = Bukkit.getPlayer(targetId);
        if (target != null) plugin.messages().send(target, "party.invite-expired");
        Player inviter = Bukkit.getPlayer(invitation.inviter());
        if (inviter != null) plugin.messages().send(inviter, "party.invite-expired-sender",
                MessageService.text("player", invitation.targetName()));
        removeEmptyParty(invitation.party());
    }

    private void removeEmptyParty(Party party) {
        boolean hasInvites = invitations.values().stream().anyMatch(invitation -> invitation.party() == party);
        if (party.size() == 1 && !hasInvites) {
            partyByMember.remove(party.leader());
        }
    }

    private void sendJoinResult(Player player, Arena arena, Arena.JoinResult result) {
        switch (result) {
            case SUCCESS -> plugin.messages().send(player, "arena.joined",
                    MessageService.text("arena", arena.displayName()),
                    MessageService.number("current", arena.playerCount()),
                    MessageService.number("maximum", arena.maxPlayers()));
            case ALREADY_JOINED -> plugin.messages().send(player, "error.already-in-arena");
            case FULL -> plugin.messages().send(player, "error.arena-full");
            case RUNNING -> plugin.messages().send(player, "error.arena-running");
        }
    }

    private void sendParty(Party party, String key, TagResolver... resolvers) {
        for (UUID memberId : party.members().keySet()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null) plugin.messages().send(member, key, resolvers);
        }
    }

    private UUID memberByName(Party party, String name) {
        return party.members().entrySet().stream()
                .filter(entry -> entry.getValue().equalsIgnoreCase(name))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);
    }

    private String argument(String[] args, int index) {
        return args.length > index ? args[index] : null;
    }

    private String joinArguments(String[] args, int index) {
        if (args.length <= index) return null;
        return String.join(" ", java.util.Arrays.copyOfRange(args, index, args.length));
    }

    private List<String> matches(Collection<String> values, String prefix) {
        return values.stream()
                .filter(value -> value.regionMatches(true, 0, prefix, 0, prefix.length()))
                .sorted()
                .toList();
    }

    private record Invitation(Party party, UUID inviter, String targetName, BukkitTask task) {
    }
}
