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
    private final UsersAPI users;

    ScoreService(ScoreRepository repository, TrainingService training, UsersAPI users) {
        this.repository = repository;
        this.training = training;
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
        return repository
                .findVisible(userId, users.hasRole(userId, Role.ROLE_COACH), users.hasRole(userId, Role.ROLE_PLAYER))
                .stream()
                .map(score -> dto(userId, score))
                .toList();
    }

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
                score.getTotal(),
                score.getSuccessful(),
                score.getCreatedAt());
    }
}
