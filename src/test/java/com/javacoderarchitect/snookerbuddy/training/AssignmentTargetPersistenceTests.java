package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SqlGroup({
    @Sql({"/test-data.sql", "/exercise-search-data.sql", "/training-data.sql"}),
    @Sql(
            statements = {
                "update exercises set default_attempts = 20, default_target = 15 where id = -1",
                "update exercises set default_attempts = 12, default_target = 9 where id = -2"
            })
})
class AssignmentTargetPersistenceTests extends BaseIT {
    private final TrainingService service;
    private final MockMvc mockMvc;
    private final ScoreService scores;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    @Autowired
    AssignmentTargetPersistenceTests(
            TrainingService service,
            JdbcTemplate jdbc,
            PlatformTransactionManager manager,
            MockMvc mockMvc,
            ScoreService scores) {
        this.mockMvc = mockMvc;
        this.scores = scores;
        this.service = service;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
    }

    private CreateAssignmentCmd assignment(AssignmentFormat format, boolean customize) {
        return new CreateAssignmentCmd(
                2L, "Practice", format, SessionKind.INDEPENDENT, List.of(-1L, -2L), customize, 30, 22);
    }

    @Test
    void groupedSessionPersistsDifferentDefaultsAndLaterLibraryChangesDoNotChangeThem() {
        var id = service.assign(1L, assignment(AssignmentFormat.TRAINING_SESSION, false))
                .getFirst();
        transaction.executeWithoutResult(status ->
                jdbc.update("update exercises set default_attempts = 100, default_target = 80 where id in (-1, -2)"));
        var plan = service.plan(2L, id);
        assertThat(plan.exercises()).extracting(ExerciseDto::attempts).containsExactly(20, 12);
        assertThat(plan.exercises()).extracting(ExerciseDto::target).containsExactly(15, 9);
        assertThat(service.exercise(2L, id, -1L).attempts()).isEqualTo(20);
        assertThat(service.exercise(2L, id, -2L).target()).isEqualTo(9);
    }

    @Test
    void individualAssignmentsKeepTheirOwnDefaults() {
        var ids = service.assign(1L, assignment(AssignmentFormat.INDIVIDUAL_EXERCISES, false));
        assertThat(ids).hasSize(2);
        assertThat(service.plan(2L, ids.get(0)).exercises().getFirst().attempts())
                .isEqualTo(20);
        assertThat(service.plan(2L, ids.get(1)).exercises().getFirst().target()).isEqualTo(9);
    }

    @Test
    void personalizedAssignmentsDoNotModifyLibraryDefaults() {
        var id = service.assign(1L, assignment(AssignmentFormat.TRAINING_SESSION, true))
                .getFirst();
        assertThat(service.plan(2L, id).exercises()).allSatisfy(exercise -> {
            assertThat(exercise.attempts()).isEqualTo(30);
            assertThat(exercise.target()).isEqualTo(22);
        });
        assertThat(jdbc.queryForList(
                        "select default_attempts from exercises where id in (-1, -2) order by id desc", Integer.class))
                .containsExactly(20, 12);
        assertThat(jdbc.queryForList(
                        "select default_target from exercises where id in (-1, -2) order by id desc", Integer.class))
                .containsExactly(15, 9);
    }

    @ParameterizedTest
    @EnumSource(SessionKind.class)
    void groupedAssignmentKeepsEachExerciseAndPerformanceSeparate(SessionKind kind) throws Exception {
        var coach = new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
        var player = new SecurityUser(2L, "Player", "player@example.com", "unused", Role.ROLE_PLAYER);
        var response = mockMvc.perform(post("/training")
                        .with(user(coach))
                        .with(csrf())
                        .param("playerId", "2")
                        .param("name", "Potting and cue control")
                        .param("format", "TRAINING_SESSION")
                        .param("kind", kind.name())
                        .param("exerciseIds", "-2", "-1"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        assertThat(service.plans(2L)).singleElement().satisfies(plan -> {
            assertThat(plan.playerId()).isEqualTo(2L);
            assertThat(plan.kind()).isEqualTo(kind);
            assertThat(plan.format()).isEqualTo(AssignmentFormat.TRAINING_SESSION);
            assertThat(plan.exercises())
                    .extracting(ExerciseDto::id, ExerciseDto::attempts, ExerciseDto::target)
                    .containsExactly(tuple(-2L, 12, 9), tuple(-1L, 20, 15));
        });
        var plan = service.plans(2L).getFirst();
        var location = response.getResponse().getRedirectedUrl();
        assertThat(location).isEqualTo("/training/" + plan.id());
        mockMvc.perform(get(location).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Potting and cue control")))
                .andExpect(content().string(containsString("Straight stop")))
                .andExpect(content().string(containsString("Straight pot")));

        for (var exercise : plan.exercises()) {
            var exerciseUrl = location + "/exercises/" + exercise.id();
            mockMvc.perform(get(exerciseUrl).with(user(player)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString(exercise.name())))
                    .andExpect(content().string(containsString(exercise.description())))
                    .andExpect(content().string(containsString(exercise.goal())))
                    .andExpect(content()
                            .string(containsString(
                                    exercise.target() + " successful / " + exercise.attempts() + " attempts")));
            mockMvc.perform(get(exerciseUrl + "/drawing").with(user(player)))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/jpeg"))
                    .andExpect(content().bytes(new byte[] {0}));
        }
        mockMvc.perform(get(location + "/exercises/-3").with(user(player))).andExpect(status().isNotFound());

        Long recorder = kind == SessionKind.JOINT ? 1L : 2L;
        scores.save(recorder, plan.id(), -1L, new CreateScoreCmd(20, 14));
        scores.save(recorder, plan.id(), -2L, new CreateScoreCmd(12, 9));
        assertThat(scores.scores(2L))
                .extracting(
                        ScoreDto::planId,
                        ScoreDto::exerciseId,
                        ScoreDto::playerId,
                        ScoreDto::total,
                        ScoreDto::successful)
                .containsExactlyInAnyOrder(tuple(plan.id(), -1L, 2L, 20, 14), tuple(plan.id(), -2L, 2L, 12, 9));
    }

    @Test
    void writtenAndNumericOverridesAreScopedToTheSelectedPlayerAndExercise() throws Exception {
        transaction.executeWithoutResult(
                status -> jdbc.update("insert into coach_players(coach_id, player_id) values (1, 5)"));
        var defaultPlanId = service.assign(
                        1L,
                        new CreateAssignmentCmd(
                                5L,
                                "Defaults",
                                AssignmentFormat.TRAINING_SESSION,
                                SessionKind.INDEPENDENT,
                                List.of(-1L, -2L),
                                false,
                                null,
                                null))
                .getFirst();
        var coach = new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
        var player = new SecurityUser(2L, "Player", "player@example.com", "unused", Role.ROLE_PLAYER);
        var created = mockMvc.perform(post("/training")
                        .with(user(coach))
                        .with(csrf())
                        .param("playerId", "2")
                        .param("format", "TRAINING_SESSION")
                        .param("kind", "INDEPENDENT")
                        .param("exerciseIds", "-1", "-2")
                        .param("customTargets", "true")
                        .param("attempts", "30")
                        .param("target", "22")
                        .param("successCriteria[-1]", "Pot without touching a cushion.")
                        .param("successCriteria[-2]", "Stop the white on the spot."))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        var location = created.getResponse().getRedirectedUrl();
        mockMvc.perform(get(location + "/exercises/-1").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pot without touching a cushion.")))
                .andExpect(content().string(containsString("22 successful / 30 attempts")));
        mockMvc.perform(get(location + "/exercises/-2").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stop the white on the spot.")));

        assertThat(service.plan(5L, defaultPlanId).exercises())
                .extracting(ExerciseDto::id, ExerciseDto::goal, ExerciseDto::attempts, ExerciseDto::target)
                .containsExactly(tuple(-1L, "Pot the red", 20, 15), tuple(-2L, "Stop the cue ball", 12, 9));
        assertThat(jdbc.queryForObject("select goal from exercises where id = -1", String.class))
                .isEqualTo("Pot the red");
        assertThat(jdbc.queryForObject("select default_target from exercises where id = -1", Integer.class))
                .isEqualTo(15);
    }

    @Test
    void assignedTrainingListsAndDetailsUseTheRightPlayerAndPlanTargets() throws Exception {
        var individualId = service.assign(
                        1L,
                        new CreateAssignmentCmd(
                                2L,
                                "",
                                AssignmentFormat.INDIVIDUAL_EXERCISES,
                                SessionKind.INDEPENDENT,
                                List.of(-1L),
                                false,
                                null,
                                null))
                .getFirst();
        var groupedId = service.assign(
                        1L,
                        new CreateAssignmentCmd(
                                2L,
                                "Joint cue control",
                                AssignmentFormat.TRAINING_SESSION,
                                SessionKind.JOINT,
                                List.of(-1L, -2L),
                                true,
                                30,
                                22,
                                java.util.Map.of(-1L, "Pot without touching a cushion.")))
                .getFirst();
        transaction.executeWithoutResult(status -> jdbc.update("""
                insert into exercises(id, owner_id, name, description, goal, difficulty, type, drawing, drawing_content_type)
                values (-8, 3, 'Other drill', 'Setup', 'Goal', 'EASY', 'TECHNIQUE', decode('00', 'hex'), 'image/jpeg')
                """));
        var otherId = service.assign(
                        3L,
                        new CreateAssignmentCmd(
                                4L,
                                "Other roster practice",
                                AssignmentFormat.TRAINING_SESSION,
                                SessionKind.INDEPENDENT,
                                List.of(-8L),
                                false,
                                null,
                                null))
                .getFirst();

        var coach = new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
        var player = new SecurityUser(2L, "Player", "player@example.com", "unused", Role.ROLE_PLAYER);
        for (var viewer : List.of(coach, player)) {
            mockMvc.perform(get("/training").with(user(viewer)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("href=\"/training/" + individualId + "\"")))
                    .andExpect(content().string(containsString("href=\"/training/" + groupedId + "\"")))
                    .andExpect(content().string(not(containsString("Other roster practice"))));
            mockMvc.perform(get("/training/" + otherId).with(user(viewer))).andExpect(status().isNotFound());
        }

        mockMvc.perform(get("/training/" + groupedId).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Demo User")))
                .andExpect(content().string(containsString("Joint practice")))
                .andExpect(content().string(containsString("Straight stop")));
        mockMvc.perform(get("/training/" + individualId).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Demo User")))
                .andExpect(content().string(containsString("Independent practice")));
        mockMvc.perform(get("/training/" + individualId + "/exercises/-1").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("15 successful / 20 attempts")))
                .andExpect(content().string(containsString("Pot the red")));
        mockMvc.perform(get("/training/" + groupedId + "/exercises/-1").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("22 successful / 30 attempts")))
                .andExpect(content().string(containsString("Pot without touching a cushion.")));
    }
}
