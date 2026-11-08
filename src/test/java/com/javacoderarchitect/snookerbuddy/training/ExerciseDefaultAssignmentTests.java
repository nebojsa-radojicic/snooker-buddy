package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

import com.javacoderarchitect.snookerbuddy.exercises.*;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExerciseDefaultAssignmentTests {
    @Mock
    private TrainingPlanRepository repository;

    @Mock
    private PlayerService players;

    @Mock
    private ExercisesAPI exercises;

    @Mock
    private UsersAPI users;

    private TrainingService service;
    private final List<ExerciseDto> library = List.of(
            new ExerciseDto(7L, "Pot", "Setup", "Goal", Difficulty.EASY, ExerciseType.TECHNIQUE, 20, 15),
            new ExerciseDto(8L, "Safety", "Setup", "Goal", Difficulty.HARD, ExerciseType.SAFETY_PLAY, 12, 9));

    @BeforeEach
    void setUp() {
        service = new TrainingService(repository, players, exercises, users);
    }

    @ParameterizedTest
    @EnumSource(AssignmentFormat.class)
    void eachExerciseKeepsItsOwnDefaultsInEitherAssignmentFormat(AssignmentFormat format) {
        when(players.isPlayer(1L, 2L)).thenReturn(true);
        when(exercises.selected(1L, List.of(7L, 8L))).thenReturn(library);
        var stored = mock(TrainingPlanEntity.class);
        when(stored.getId()).thenReturn(3L);
        when(repository.save(any())).thenReturn(stored);
        service.assign(
                1L,
                new CreateAssignmentCmd(
                        2L, "Practice", format, SessionKind.INDEPENDENT, List.of(7L, 8L), false, 999, 999));
        var saved = ArgumentCaptor.forClass(TrainingPlanEntity.class);
        verify(repository, times(format == AssignmentFormat.TRAINING_SESSION ? 1 : 2))
                .save(saved.capture());
        var assigned = saved.getAllValues().stream()
                .flatMap(plan -> plan.getExercises().stream())
                .toList();
        assertThat(assigned).extracting(TrainingPlanExercise::getExerciseId).containsExactly(7L, 8L);
        assertThat(assigned).extracting(TrainingPlanExercise::getAttempts).containsExactly(20, 12);
        assertThat(assigned).extracting(TrainingPlanExercise::getTarget).containsExactly(15, 9);
    }

    @Test
    void existingPersonalizationOverridesCopiedTargetsWithoutChangingLibraryDefaults() {
        when(players.isPlayer(1L, 2L)).thenReturn(true);
        when(exercises.selected(1L, List.of(7L, 8L))).thenReturn(library);
        var stored = mock(TrainingPlanEntity.class);
        when(stored.getId()).thenReturn(3L);
        when(repository.save(any())).thenReturn(stored);
        service.assign(
                1L,
                new CreateAssignmentCmd(
                        2L,
                        "Practice",
                        AssignmentFormat.TRAINING_SESSION,
                        SessionKind.JOINT,
                        List.of(7L, 8L),
                        true,
                        30,
                        22));
        var saved = ArgumentCaptor.forClass(TrainingPlanEntity.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getExercises()).allSatisfy(item -> {
            assertThat(item.getAttempts()).isEqualTo(30);
            assertThat(item.getTarget()).isEqualTo(22);
        });
        assertThat(library).extracting(ExerciseDto::attempts).containsExactly(20, 12);
        assertThat(library).extracting(ExerciseDto::target).containsExactly(15, 9);
    }

    @Test
    void viewingAnAssignedExerciseUsesItsStoredTargetsRatherThanCurrentLibraryDefaults() {
        var plan = new TrainingPlanEntity(
                1L,
                2L,
                "Practice",
                AssignmentFormat.TRAINING_SESSION,
                SessionKind.INDEPENDENT,
                List.of(new TrainingPlanExercise(7L, 20, 15)));
        when(repository.findById(3L)).thenReturn(Optional.of(plan));
        when(users.hasRole(2L, com.javacoderarchitect.snookerbuddy.users.Role.ROLE_PLAYER))
                .thenReturn(true);
        when(exercises.selected(1L, List.of(7L)))
                .thenReturn(List.of(library.getFirst().withTargets(100, 80)));
        var assigned = service.exercise(2L, 3L, 7L);
        assertThat(assigned.attempts()).isEqualTo(20);
        assertThat(assigned.target()).isEqualTo(15);
    }
}
