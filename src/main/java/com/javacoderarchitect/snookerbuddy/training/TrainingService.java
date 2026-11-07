package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.exercises.ExerciseDrawingDto;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto;
import com.javacoderarchitect.snookerbuddy.exercises.ExercisesAPI;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.server.ResponseStatusException;

@Service
@Validated
class TrainingService {
    private final TrainingPlanRepository repository;
    private final PlayerService players;
    private final ExercisesAPI exercises;
    private final UsersAPI users;

    TrainingService(TrainingPlanRepository repository, PlayerService players, ExercisesAPI exercises, UsersAPI users) {
        this.repository = repository;
        this.players = players;
        this.exercises = exercises;
        this.users = users;
    }

    @Transactional
    public List<Long> assign(Long coachId, @Valid CreateAssignmentCmd cmd) {
        if (!players.isPlayer(coachId, cmd.playerId()))
            throw new IllegalArgumentException("Choose a player from your list.");
        if (cmd.exerciseIds().stream().distinct().count() != cmd.exerciseIds().size())
            throw new IllegalArgumentException("Choose each exercise only once.");
        List<ExerciseDto> selected;
        try {
            selected = exercises.selected(coachId, cmd.exerciseIds());
        } catch (ResponseStatusException ex) {
            throw new IllegalArgumentException("Choose exercises from your own library.", ex);
        }
        int attempts = cmd.customTargets() ? cmd.attempts() : 10;
        int target = cmd.customTargets() ? cmd.target() : 8;
        if (cmd.format() == AssignmentFormat.INDIVIDUAL_EXERCISES) {
            return selected.stream()
                    .map(exercise -> repository
                            .save(new TrainingPlanEntity(
                                    coachId,
                                    cmd.playerId(),
                                    exercise.name(),
                                    cmd.format(),
                                    cmd.kind(),
                                    attempts,
                                    target,
                                    List.of(exercise.id())))
                            .getId())
                    .toList();
        }
        String name = cmd.name() == null || cmd.name().isBlank()
                ? "Practice session"
                : cmd.name().strip();
        return List.of(repository
                .save(new TrainingPlanEntity(
                        coachId, cmd.playerId(), name, cmd.format(), cmd.kind(), attempts, target, cmd.exerciseIds()))
                .getId());
    }

    @Transactional(readOnly = true)
    public List<TrainingPlanDto> plans(Long userId) {
        return repository.findByCoachIdOrPlayerIdOrderByCreatedAtDescIdDesc(userId, userId).stream()
                .map(this::dto)
                .toList();
    }

    @Transactional(readOnly = true)
    public TrainingPlanDto plan(Long userId, Long id) {
        return dto(accessible(userId, id));
    }

    @Transactional(readOnly = true)
    public ExerciseDto exercise(Long userId, Long planId, Long exerciseId) {
        var plan = containing(userId, planId, exerciseId);
        return exercises.selected(plan.getCoachId(), List.of(exerciseId)).getFirst();
    }

    @Transactional(readOnly = true)
    public ExerciseDrawingDto drawing(Long userId, Long planId, Long exerciseId) {
        return exercises.drawing(containing(userId, planId, exerciseId).getCoachId(), exerciseId);
    }

    private TrainingPlanEntity containing(Long userId, Long id, Long exerciseId) {
        var plan = accessible(userId, id);
        if (!plan.getExerciseIds().contains(exerciseId)) throw missing();
        return plan;
    }

    private TrainingPlanEntity accessible(Long userId, Long id) {
        return repository
                .findById(id)
                .filter(plan ->
                        plan.getCoachId().equals(userId) || plan.getPlayerId().equals(userId))
                .orElseThrow(this::missing);
    }

    private ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Training plan not found");
    }

    private TrainingPlanDto dto(TrainingPlanEntity plan) {
        return new TrainingPlanDto(
                plan.getId(),
                plan.getName(),
                plan.getCoachId(),
                users.findById(plan.getCoachId()).orElseThrow().name(),
                plan.getPlayerId(),
                users.findById(plan.getPlayerId()).orElseThrow().name(),
                plan.getFormat(),
                plan.getKind(),
                plan.getAttempts(),
                plan.getTarget(),
                plan.getCreatedAt(),
                exercises.selected(plan.getCoachId(), plan.getExerciseIds()));
    }
}
