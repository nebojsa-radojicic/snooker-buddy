package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.javacoderarchitect.snookerbuddy.exercises.Difficulty;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseType;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.UserDto;
import com.javacoderarchitect.snookerbuddy.users.UsersAPI;
import jakarta.validation.Validation;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ScoreServiceTests {
    @Mock
    private ScoreRepository repository;

    @Mock
    private TrainingService training;

    @Mock
    private PlayerService players;

    @Mock
    private UsersAPI users;

    private ScoreService service;

    @BeforeEach
    void setUp() {
        service = new ScoreService(repository, training, players, users);
    }

    private TrainingPlanEntity plan(SessionKind kind) {
        return new TrainingPlanEntity(
                1L,
                2L,
                "Practice",
                AssignmentFormat.TRAINING_SESSION,
                kind,
                List.of(new TrainingPlanExercise(7L, 10, 8)));
    }

    private TrainingPlanDto planDto() {
        return new TrainingPlanDto(
                3L,
                "Practice",
                1L,
                "Coach",
                2L,
                "Player",
                AssignmentFormat.TRAINING_SESSION,
                SessionKind.INDEPENDENT,
                Instant.now(),
                List.of(new ExerciseDto(7L, "Pot", "Setup", "Goal", Difficulty.EASY, ExerciseType.TECHNIQUE)));
    }

    @ParameterizedTest
    @CsvSource({"1,JOINT", "2,INDEPENDENT"})
    void storesScoreForTheAuthorizedRecorder(long userId, SessionKind kind) {
        when(training.containing(userId, 3L, 7L)).thenReturn(plan(kind));
        when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
        when(training.plan(userId, 3L)).thenReturn(planDto());
        var score = service.save(userId, 3L, 7L, new CreateScoreCmd(10, 0));
        assertThat(score.playerId()).isEqualTo(2L);
        assertThat(score.planId()).isEqualTo(3L);
        assertThat(score.exerciseId()).isEqualTo(7L);
        assertThat(score.total()).isEqualTo(10);
        assertThat(score.successful()).isZero();
    }

    @ParameterizedTest
    @CsvSource({"1,INDEPENDENT", "2,JOINT"})
    void refusesTheWrongAssignmentParticipant(long userId, SessionKind kind) {
        when(training.containing(userId, 3L, 7L)).thenReturn(plan(kind));
        assertThatThrownBy(() -> service.recording(userId, 3L, 7L)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.save(userId, 3L, 7L, new CreateScoreCmd(10, 8)))
                .isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsScoreInAnInaccessiblePlanBeforeWritingAnything() {
        when(training.containing(4L, 3L, 7L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.save(4L, 3L, 7L, new CreateScoreCmd(10, 8)))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(repository);
    }

    @Test
    void checksPlanAccessOnDirectScoreReads() {
        when(repository.findById(9L)).thenReturn(Optional.of(new ScoreEntity(3L, 7L, 10, 8)));
        when(training.plan(4L, 3L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        assertThatThrownBy(() -> service.score(4L, 9L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void returnsNotFoundForMissingScore() {
        when(repository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.score(2L, 9L))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void listsOnlyScoresAllowedByTheAccountsRoles() {
        when(users.hasRole(2L, Role.ROLE_COACH)).thenReturn(true);
        when(users.hasRole(2L, Role.ROLE_PLAYER)).thenReturn(true);
        when(repository.findVisible(2L, true, true, null)).thenReturn(List.of(new ScoreEntity(3L, 7L, 10, 8)));
        when(training.plan(2L, 3L)).thenReturn(planDto());
        assertThat(service.scores(2L))
                .singleElement()
                .satisfies(score -> assertThat(score.successful()).isEqualTo(8));
    }

    @Test
    void showsEmptyHistoryForAnAssociatedPlayer() {
        when(players.isPlayer(1L, 2L)).thenReturn(true);
        when(users.findById(2L))
                .thenReturn(Optional.of(
                        new UserDto(2L, "Player", "player@example.com", Set.of(Role.ROLE_PLAYER), Instant.now())));
        var history = service.playerHistory(1L, 2L);
        assertThat(history.playerName()).isEqualTo("Player");
        assertThat(history.scores()).isEmpty();
        verify(users).requireRole(1L, Role.ROLE_COACH);
        verify(repository).findVisible(1L, true, false, 2L);
        verifyNoMoreInteractions(repository);
    }

    @Test
    void refusesPlayerHistoryOutsideTheCoachsRosterBeforeLookingUpResults() {
        assertThatThrownBy(() -> service.playerHistory(1L, 4L))
                .isInstanceOfSatisfying(
                        ResponseStatusException.class,
                        ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
        verifyNoInteractions(repository, training);
    }

    @ParameterizedTest
    @CsvSource({"0,0,false", "10,-1,false", "10,11,false", "10,0,true", "10,10,true", ",,false"})
    void validatesBasicScoreCounts(Integer total, Integer successful, boolean valid) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator()
                            .validate(new CreateScoreCmd(total, successful))
                            .isEmpty())
                    .isEqualTo(valid);
        }
    }
}
