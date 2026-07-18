package io.github.twmeai.openbedwars.game;

import io.github.twmeai.openbedwars.message.MessageService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Criteria;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public final class ScoreboardService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("MM/dd/yy", Locale.ROOT);

    private final MessageService messages;
    private final Map<UUID, Board> boards = new HashMap<>();

    public ScoreboardService(MessageService messages) {
        this.messages = messages;
    }

    public void update(Arena arena) {
        EventSchedule.UpcomingEvent upcoming = arena.settings().eventSchedule()
                .nextAfter(arena.elapsedSeconds())
                .orElse(new EventSchedule.UpcomingEvent(GameEventType.GAME_END, 0));
        for (PlayerState state : arena.players().values()) {
            Player player = Bukkit.getPlayer(state.playerId());
            if (player == null) continue;
            Board board = boards.computeIfAbsent(state.playerId(), ignored -> create(player));
            board.update(lines(player, arena, state, upcoming));
            if (player.getScoreboard() != board.scoreboard()) {
                player.setScoreboard(board.scoreboard());
            }
        }
    }

    public void remove(Player player) {
        boards.remove(player.getUniqueId());
    }

    public void clear() {
        boards.clear();
    }

    private List<Component> lines(Player player, Arena arena, PlayerState playerState, EventSchedule.UpcomingEvent upcoming) {
        List<Component> lines = new ArrayList<>();
        lines.add(messages.render(player, "scoreboard.date", MessageService.text("date", DATE.format(LocalDate.now()))));
        lines.add(Component.empty());
        lines.add(messages.render(player, "scoreboard.event",
                MessageService.text("event", upcoming.type().displayName()),
                MessageService.text("time", formatTime(upcoming.secondsRemaining()))));
        lines.add(Component.empty());
        for (TeamState team : arena.teams().values()) {
            if (team.members().isEmpty()) continue;
            Component status;
            if (team.bedAlive()) {
                status = Component.text("B", NamedTextColor.GREEN);
            } else {
                long alive = team.members().stream()
                        .map(arena.players()::get)
                        .filter(java.util.Objects::nonNull)
                        .filter(state -> !state.eliminated())
                        .count();
                status = alive == 0
                        ? Component.text("X", NamedTextColor.RED)
                        : Component.text(Long.toString(alive), NamedTextColor.YELLOW);
            }
            lines.add(messages.render(player, "scoreboard.team-alive",
                    MessageService.text("symbol", team.color() == playerState.team() ? ">" : " "),
                    MessageService.text("team", team.color().displayName()),
                    MessageService.component("status", status),
                    MessageService.teamColor("team_color", team.color())));
        }
        lines.add(Component.empty());
        lines.add(messages.render(player, "scoreboard.stats",
                MessageService.number("kills", playerState.kills()),
                MessageService.number("final_kills", playerState.finalKills())));
        lines.add(messages.render(player, "scoreboard.beds", MessageService.number("beds", playerState.bedsBroken())));
        return lines.size() <= 15 ? lines : List.copyOf(lines.subList(0, 15));
    }

    private Board create(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective objective = scoreboard.registerNewObjective(
                "openbedwars", Criteria.DUMMY, messages.render(player, "scoreboard.title"));
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);
        List<Team> lineTeams = new ArrayList<>();
        for (int index = 0; index < 15; index++) {
            String entry = "\u00A7" + Integer.toHexString(index);
            Team team = scoreboard.registerNewTeam("line_" + index);
            team.addEntry(entry);
            objective.getScore(entry).setScore(15 - index);
            lineTeams.add(team);
        }
        return new Board(scoreboard, lineTeams);
    }

    private String formatTime(int seconds) {
        return "%d:%02d".formatted(Math.max(0, seconds) / 60, Math.max(0, seconds) % 60);
    }

    private record Board(Scoreboard scoreboard, List<Team> teams) {
        void update(List<Component> lines) {
            for (int index = 0; index < teams.size(); index++) {
                teams.get(index).prefix(index < lines.size() ? lines.get(index) : Component.empty());
            }
        }
    }
}
