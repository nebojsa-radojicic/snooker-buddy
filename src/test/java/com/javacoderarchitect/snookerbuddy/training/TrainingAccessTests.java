package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.javacoderarchitect.snookerbuddy.exercises.Difficulty;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseType;
import com.javacoderarchitect.snookerbuddy.exercises.ExercisesAPI;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TrainingAccessTests {
    @Mock
    private TrainingPlanRepository repository;

    @Mock
    private PlayerService players;

    @Mock
    private ExercisesAPI exercises;

    @Mock
    private UsersAPI users;

    private TrainingService service;
    private TrainingPlanEntity plan;

    @BeforeEach
    void setUp() {
        service = new TrainingService(repository, players, exercises, users);
        plan = new TrainingPlanEntity(
                1L,
                2L,
                "Practice",
                AssignmentFormat.TRAINING_SESSION,
                SessionKind.INDEPENDENT,
                List.of(new TrainingPlanExercise(7L, 10, 8)));
    }

    @Test
    void anUnrelatedCoachCannotReadAPlanOrItsExerciseDrawing() {
        when(repository.findById(3L)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.plan(4L, 3L))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.drawing(4L, 3L, 7L)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(exercises);
    }

    @Test
    void thePlayerRoleDoesNotAuthorizeAccessToTheCoachsOtherExercises() {
        when(repository.findById(3L)).thenReturn(Optional.of(plan));
        when(users.hasRole(2L, Role.ROLE_PLAYER)).thenReturn(true);
        assertThatThrownBy(() -> service.drawing(2L, 3L, 8L)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(exercises);
    }

    @Test
    void owningTheCoachIdWithoutTheCoachRoleDoesNotAuthorizePlanAccess() {
        when(repository.findById(3L)).thenReturn(Optional.of(plan));
        assertThatThrownBy(() -> service.plan(1L, 3L)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(exercises);
    }

    @Test
    void aCoachMustStillHaveThePlayerRelationship() {
        when(repository.findById(3L)).thenReturn(Optional.of(plan));
        when(users.hasRole(1L, Role.ROLE_COACH)).thenReturn(true);
        assertThatThrownBy(() -> service.plan(1L, 3L)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(exercises);
    }

    @Test
    void theAssignedPlayerCanReadTheIncludedDrawingThroughThePlan() {
        when(repository.findById(3L)).thenReturn(Optional.of(plan));
        when(users.hasRole(2L, Role.ROLE_PLAYER)).thenReturn(true);
        var drawing =
                new com.javacoderarchitect.snookerbuddy.exercises.ExerciseDrawingDto(new byte[] {1}, "image/jpeg");
        when(exercises.drawing(1L, 7L)).thenReturn(drawing);
        assertThat(service.drawing(2L, 3L, 7L)).isEqualTo(drawing);
    }

    @Test
    void aRelatedCoachCanReadTheIncludedDrawingThroughThePlan() {
        when(repository.findById(3L)).thenReturn(Optional.of(plan));
        when(users.hasRole(1L, Role.ROLE_COACH)).thenReturn(true);
        when(players.isPlayer(1L, 2L)).thenReturn(true);
        var drawing =
                new com.javacoderarchitect.snookerbuddy.exercises.ExerciseDrawingDto(new byte[] {1}, "image/jpeg");
        when(exercises.drawing(1L, 7L)).thenReturn(drawing);
        assertThat(service.drawing(1L, 3L, 7L)).isEqualTo(drawing);
    }

    @Test
    void accountsWithoutBusinessRolesHaveNoVisiblePlans() {
        when(repository.findByCoachIdOrPlayerIdOrderByCreatedAtDescIdDesc(1L, 1L))
                .thenReturn(List.of(plan));
        assertThat(service.plans(1L)).isEmpty();
    }

    @Test
    void theSameExerciseUsesTheTargetsFromThePlanBeingReviewed() {
        var personalized = new TrainingPlanEntity(
                1L,
                2L,
                "Joint practice",
                AssignmentFormat.INDIVIDUAL_EXERCISES,
                SessionKind.JOINT,
                List.of(new TrainingPlanExercise(7L, 20, 15, "Pot without touching a cushion.")));
        when(repository.findById(3L)).thenReturn(Optional.of(plan));
        when(repository.findById(4L)).thenReturn(Optional.of(personalized));
        when(users.hasRole(2L, Role.ROLE_PLAYER)).thenReturn(true);
        when(exercises.selected(1L, List.of(7L)))
                .thenReturn(List.of(
                        new ExerciseDto(7L, "Pot", "Setup", "Pot the red", Difficulty.EASY, ExerciseType.TECHNIQUE)));

        var first = service.exercise(2L, 3L, 7L);
        assertThat(first.attempts()).isEqualTo(10);
        assertThat(first.target()).isEqualTo(8);
        assertThat(first.goal()).isEqualTo("Pot the red");

        var second = service.exercise(2L, 4L, 7L);
        assertThat(second.attempts()).isEqualTo(20);
        assertThat(second.target()).isEqualTo(15);
        assertThat(second.goal()).isEqualTo("Pot without touching a cushion.");
    }
}
