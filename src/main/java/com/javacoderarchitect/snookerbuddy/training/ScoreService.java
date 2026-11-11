package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
class ScoreService {
    private final ScoreRepository repository;
    private final TrainingService training;
    private final PlayerService players;
    private final UsersAPI users;

    ScoreService(ScoreRepository repository, TrainingService training, PlayerService players, UsersAPI users) {
        this.repository = repository;
        this.training = training;
        this.players = players;
        this.users = users;
    }

    @Transactional
    public ScoreDto save(Long userId, Long planId, Long exerciseId, @Valid CreateScoreCmd cmd) {
        requireRecorder(userId, planId, exerciseId);
        var score = repository.saveAndFlush(new ScoreEntity(planId, exerciseId, cmd.total(), cmd.successful()));
        return dto(userId, score);
    }

    @Transactional(readOnly = true)
    public TrainingPlanDto recording(Long userId, Long planId, Long exerciseId) {
        requireRecorder(userId, planId, exerciseId);
        return training.plan(userId, planId);
    }

    private void requireRecorder(Long userId, Long planId, Long exerciseId) {
        var plan = training.containing(userId, planId, exerciseId);
        if (!plan.getKind().allowsRecordingBy(userId, plan.getCoachId(), plan.getPlayerId())) {
            throw new AccessDeniedException(
                    "Coaches record joint practice; players record their own independent practice.");
        }
    }

    @Transactional(readOnly = true)
    public List<ScoreDto> scores(Long userId) {
        return visibleScores(
                userId, users.hasRole(userId, Role.ROLE_COACH), users.hasRole(userId, Role.ROLE_PLAYER), null);
    }

    @Transactional(readOnly = true)
    public PlayerHistory playerHistory(Long coachId, Long playerId) {
        users.requireRole(coachId, Role.ROLE_COACH);
        if (!players.isPlayer(coachId, playerId)) throw playerNotFound();
        var player = users.findById(playerId).orElseThrow(this::playerNotFound);
        return new PlayerHistory(player.name(), visibleScores(coachId, true, false, playerId));
    }

    private List<ScoreDto> visibleScores(Long userId, boolean coach, boolean player, Long selectedPlayerId) {
        return repository.findVisible(userId, coach, player, selectedPlayerId).stream()
                .map(score -> dto(userId, score))
                .toList();
    }

    private ResponseStatusException playerNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Player not found");
    }

    record PlayerHistory(String playerName, List<ScoreDto> scores) {}

    @Transactional(readOnly = true)
    public ScoreDto score(Long userId, Long id) {
        return dto(
                userId,
                repository
                        .findById(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Score not found")));
    }

    private ScoreDto dto(Long userId, ScoreEntity score) {
        var plan = training.plan(userId, score.getPlanId());
        var exercise = plan.exercises().stream()
                .filter(e -> e.id().equals(score.getExerciseId()))
                .findFirst()
                .orElseThrow();
        return new ScoreDto(
                score.getId(),
                score.getPlanId(),
                score.getExerciseId(),
                plan.playerId(),
                plan.playerName(),
                exercise.name(),
                plan.kind(),
                score.getTotal(),
                score.getSuccessful(),
                score.getCreatedAt());
    }
}
