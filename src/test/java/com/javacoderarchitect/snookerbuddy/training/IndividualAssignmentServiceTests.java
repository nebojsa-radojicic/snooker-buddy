package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.javacoderarchitect.snookerbuddy.exercises.*;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class IndividualAssignmentServiceTests {
    @Mock
    private TrainingPlanRepository repository;

    @Mock
    private PlayerService players;

    @Mock
    private ExercisesAPI exercises;

    @Mock
    private UsersAPI users;

    private TrainingService service;

    @BeforeEach
    void setUp() {
        service = new TrainingService(repository, players, exercises, users);
    }

    private CreateAssignmentCmd command(SessionKind kind) {
        return new CreateAssignmentCmd(
                2L, "", AssignmentFormat.INDIVIDUAL_EXERCISES, kind, List.of(7L), false, 999, 999);
    }

    @ParameterizedTest
    @EnumSource(SessionKind.class)
    void storesThePlayerPracticeKindAndLibraryDefaultTargets(SessionKind kind) {
        when(players.isPlayer(1L, 2L)).thenReturn(true);
        when(exercises.selected(1L, List.of(7L)))
                .thenReturn(List.of(
                        new ExerciseDto(7L, "Pot", "Setup", "Goal", Difficulty.EASY, ExerciseType.TECHNIQUE, 20, 15)));
        var stored = mock(TrainingPlanEntity.class);
        when(stored.getId()).thenReturn(3L);
        when(repository.save(any())).thenReturn(stored);
        assertThat(service.assign(1L, command(kind))).containsExactly(3L);
        var plan = ArgumentCaptor.forClass(TrainingPlanEntity.class);
        verify(repository).save(plan.capture());
        assertThat(plan.getValue().getCoachId()).isEqualTo(1L);
        assertThat(plan.getValue().getPlayerId()).isEqualTo(2L);
        assertThat(plan.getValue().getKind()).isEqualTo(kind);
        assertThat(plan.getValue().getFormat()).isEqualTo(AssignmentFormat.INDIVIDUAL_EXERCISES);
        assertThat(plan.getValue().getName()).isEqualTo("Pot");
        assertThat(plan.getValue().getExercises()).singleElement().satisfies(item -> {
            assertThat(item.getExerciseId()).isEqualTo(7L);
            assertThat(item.getAttempts()).isEqualTo(20);
            assertThat(item.getTarget()).isEqualTo(15);
        });
    }

    @Test
    void rejectsAPlayerOutsideTheCoachsRosterBeforeLookingUpExercises() {
        assertThatThrownBy(() -> service.assign(1L, command(SessionKind.INDEPENDENT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Choose a player from your list");
        verifyNoInteractions(exercises, repository);
    }

    @Test
    void rejectsForeignExercisesWithoutSavingAnAssignment() {
        when(players.isPlayer(1L, 2L)).thenReturn(true);
        when(exercises.selected(1L, List.of(7L))).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.assign(1L, command(SessionKind.INDEPENDENT)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own library");
        verifyNoInteractions(repository);
    }

    @Test
    void directServiceCallsAlsoRequireTheCoachRole() {
        doThrow(new AccessDeniedException("Access denied")).when(users).requireRole(1L, Role.ROLE_COACH);
        assertThatThrownBy(() -> service.assign(1L, command(SessionKind.INDEPENDENT)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(players, exercises, repository);
    }
}
