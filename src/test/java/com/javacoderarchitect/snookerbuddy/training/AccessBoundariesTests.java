package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Sql({"/test-data.sql", "/exercise-search-data.sql", "/training-data.sql"})
class AccessBoundariesTests extends BaseIT {
    private final MockMvc mvc;
    private final TrainingService training;
    private final PlayerService players;
    private final ScoreService scores;
    private final ScoreRepository repository;
    private final JdbcTemplate jdbc;

    @Autowired
    AccessBoundariesTests(
            MockMvc mvc,
            TrainingService training,
            PlayerService players,
            ScoreService scores,
            ScoreRepository repository,
            JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.training = training;
        this.players = players;
        this.scores = scores;
        this.repository = repository;
        this.jdbc = jdbc;
    }

    private SecurityUser account(long id, Role... roles) {
        return new SecurityUser(id, "Account", "account@example.com", "unused", roles);
    }

    private Long plan(Long playerId, SessionKind kind) {
        return training.assign(
                        1L,
                        new CreateAssignmentCmd(
                                playerId,
                                "Practice",
                                AssignmentFormat.TRAINING_SESSION,
                                kind,
                                List.of(-1L),
                                false,
                                null,
                                null))
                .getFirst();
    }

    @Test
    void allowsSeveralPlayersButPreventsASecondCoachIncludingAtDatabaseLevel() {
        players.add(1L, "newplayer@example.com");
        assertThat(players.players(1L)).extracting(p -> p.id()).containsExactlyInAnyOrder(2L, 5L);
        assertThatThrownBy(() -> players.add(3L, "demouser@gmail.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already has a coach");
        assertThatThrownBy(() -> jdbc.update("insert into coach_players(coach_id, player_id) values (3, 2)"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(players.isPlayer(1L, 2L)).isTrue();
        assertThat(players.isPlayer(3L, 2L)).isFalse();
    }

    @Test
    void dualRoleAccountCanTrainAndCoachWithoutInheritingOtherPeoplesAccess() throws Exception {
        players.add(2L, "newplayer@example.com");
        Long ownPlan = plan(2L, SessionKind.INDEPENDENT);
        Long coachedPlan = training.assign(
                        2L,
                        new CreateAssignmentCmd(
                                5L,
                                "My player",
                                AssignmentFormat.TRAINING_SESSION,
                                SessionKind.JOINT,
                                List.of(-7L),
                                false,
                                null,
                                null))
                .getFirst();
        var ownScore = scores.save(2L, ownPlan, -1L, new CreateScoreCmd(10, 8));
        var coachedScore = scores.save(2L, coachedPlan, -7L, new CreateScoreCmd(10, 6));
        assertThat(scores.scores(2L))
                .extracting(ScoreDto::id)
                .containsExactlyInAnyOrder(ownScore.id(), coachedScore.id());
        assertThat(scores.scores(1L)).extracting(ScoreDto::id).containsExactly(ownScore.id());
        mvc.perform(get("/training").with(user(account(2, Role.ROLE_COACH, Role.ROLE_PLAYER))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("My player")));
        mvc.perform(get("/scores/" + coachedScore.id()).with(user(account(1, Role.ROLE_COACH))))
                .andExpect(status().isNotFound());
    }

    @Test
    void playerOnlyAccountCannotUseCoachRoutesOrCoachServices() throws Exception {
        var player = account(4, Role.ROLE_PLAYER);
        for (String path : List.of(
                "/exercises",
                "/exercises/new",
                "/exercises/-1",
                "/exercises/-1/drawing",
                "/players",
                "/training/new")) {
            mvc.perform(get(path).with(user(player))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/players").with(user(player)).with(csrf()).param("email", "newplayer@example.com"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/training").with(user(player)).with(csrf())).andExpect(status().isForbidden());
        assertThatThrownBy(() -> players.add(4L, "newplayer@example.com")).isInstanceOf(AccessDeniedException.class);
        mvc.perform(get("/training").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("href=\"/training/new\""))))
                .andExpect(content().string(not(containsString("href=\"/players\""))));
    }

    @Test
    void doesNotAllowLinkingAnAccountWithoutThePlayerRole() {
        assertThatThrownBy(() -> players.add(1L, "othercoach@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("player role");
    }

    @Test
    void ownersSeeOnlyTheirLibraryAndAssignedPlayersSeeOnlyIncludedExercises() throws Exception {
        Long id = plan(2L, SessionKind.INDEPENDENT);
        mvc.perform(get("/exercises").with(user(account(1, Role.ROLE_COACH))))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("Straight private"))));
        for (String path : List.of("/exercises/-1", "/exercises/-1/drawing")) {
            mvc.perform(get(path).with(user(account(3, Role.ROLE_COACH)))).andExpect(status().isNotFound());
        }
        mvc.perform(get("/training/" + id + "/exercises/-1").with(user(account(2, Role.ROLE_PLAYER))))
                .andExpect(status().isOk());
        mvc.perform(get("/training/" + id + "/exercises/-1/drawing").with(user(account(2, Role.ROLE_PLAYER))))
                .andExpect(status().isOk());
        for (String path : List.of(
                "/training/" + id, "/training/" + id + "/exercises/-1", "/training/" + id + "/exercises/-1/drawing")) {
            mvc.perform(get(path).with(user(account(4, Role.ROLE_PLAYER)))).andExpect(status().isNotFound());
        }
        for (String suffix : List.of("", "/drawing")) {
            mvc.perform(get("/training/" + id + "/exercises/-2" + suffix).with(user(account(2, Role.ROLE_PLAYER))))
                    .andExpect(status().isNotFound());
        }
    }

    @Test
    void storesScoresAndRestrictsListsAndDirectReadsToPlayerAndTheirCoach() throws Exception {
        Long id = plan(2L, SessionKind.INDEPENDENT);
        var result = mvc.perform(post("/training/" + id + "/exercises/-1/scores")
                        .with(user(account(2, Role.ROLE_PLAYER)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\":10,\"successful\":7}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.playerId").value(2))
                .andExpect(jsonPath("$.successful").value(7))
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        assertThat(repository.findAll()).singleElement().satisfies(score -> {
            assertThat(score.getTotal()).isEqualTo(10);
            assertThat(score.getSuccessful()).isEqualTo(7);
            assertThat(score.getPlanId()).isEqualTo(id);
        });
        for (var viewer : List.of(account(1, Role.ROLE_COACH), account(2, Role.ROLE_PLAYER))) {
            mvc.perform(get(location).with(user(viewer)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.total").value(10))
                    .andExpect(header().string("Cache-Control", "no-store"));
            mvc.perform(get("/scores").with(user(viewer)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("7 / 10")));
        }
        for (var outsider :
                List.of(account(3, Role.ROLE_COACH), account(4, Role.ROLE_PLAYER), account(1, Role.ROLE_ADMIN))) {
            mvc.perform(get(location).with(user(outsider)))
                    .andExpect(status().is(outsider.getRoles().contains(Role.ROLE_ADMIN) ? 403 : 404));
            if (!outsider.getRoles().contains(Role.ROLE_ADMIN)) {
                mvc.perform(get("/scores").with(user(outsider)))
                        .andExpect(status().isOk())
                        .andExpect(content().string(containsString("No scores recorded yet")));
            }
        }
    }

    @Test
    void rejectedScoreWritesDoNotCreateRecords() throws Exception {
        Long id = plan(2L, SessionKind.INDEPENDENT);
        String path = "/training/" + id + "/exercises/-1/scores";
        mvc.perform(post(path)
                        .with(user(account(3, Role.ROLE_COACH)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\":10,\"successful\":8}"))
                .andExpect(status().isNotFound());
        mvc.perform(post(path)
                        .with(user(account(1, Role.ROLE_COACH)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\":10,\"successful\":8}"))
                .andExpect(status().isForbidden());
        mvc.perform(post(path)
                        .with(user(account(2, Role.ROLE_PLAYER)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\":10,\"successful\":8}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/training/" + id + "/exercises/-2/scores")
                        .with(user(account(2, Role.ROLE_PLAYER)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\":10,\"successful\":8}"))
                .andExpect(status().isNotFound());
        assertThat(repository.count()).isZero();
    }

    @Test
    void onlyTheResponsibleParticipantCanOpenAndSubmitTheRecordingForm() throws Exception {
        var coach = account(1, Role.ROLE_COACH);
        var player = account(2, Role.ROLE_COACH, Role.ROLE_PLAYER);
        var outsider = account(4, Role.ROLE_PLAYER);
        Long joint = plan(2L, SessionKind.JOINT);
        Long independent = plan(2L, SessionKind.INDEPENDENT);

        for (Long id : List.of(joint, independent)) {
            var recorder = id.equals(joint) ? coach : player;
            var viewer = id.equals(joint) ? player : coach;
            String exercisePath = "/training/" + id + "/exercises/-1";
            String recordingPath = exercisePath + "/scores";
            mvc.perform(get(exercisePath).with(user(recorder)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("href=\"" + recordingPath + "\"")));
            mvc.perform(get(exercisePath).with(user(viewer)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(not(containsString("href=\"" + recordingPath + "\""))));
            mvc.perform(get(recordingPath).with(user(recorder))).andExpect(status().isOk());
            mvc.perform(get(recordingPath).with(user(viewer))).andExpect(status().isForbidden());
            mvc.perform(get(recordingPath).with(user(outsider))).andExpect(status().isNotFound());

            long scoresBefore = repository.count();
            mvc.perform(scoreForm(recordingPath, viewer)).andExpect(status().isForbidden());
            mvc.perform(scoreForm(recordingPath, outsider)).andExpect(status().isNotFound());
            mvc.perform(scoreForm("/training/" + id + "/exercises/-2/scores", recorder))
                    .andExpect(status().isNotFound());
            assertThat(repository.count()).isEqualTo(scoresBefore);

            mvc.perform(scoreForm(recordingPath, recorder))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrl("/scores"));
        }
        assertThat(repository.findAll())
                .extracting(ScoreEntity::getPlanId)
                .containsExactlyInAnyOrder(joint, independent);
    }

    private MockHttpServletRequestBuilder scoreForm(String path, SecurityUser recorder) {
        return post(path)
                .with(user(recorder))
                .with(csrf())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("total", "10")
                .param("successful", "8");
    }

    @Test
    void savesZeroSuccessesWithTheActualTotalAndShowsTheAssignmentContextInScores() throws Exception {
        Long id = plan(2L, SessionKind.INDEPENDENT);
        var player = account(2, Role.ROLE_PLAYER);
        String exercisePath = "/training/" + id + "/exercises/-1";
        String recordingPath = exercisePath + "/scores";
        mvc.perform(get(recordingPath).with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("8 successful / 10 attempts")));

        mvc.perform(post(recordingPath)
                        .with(user(player))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("total", "3")
                        .param("successful", "0"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/scores"));

        assertThat(repository.findAll()).singleElement().satisfies(score -> {
            assertThat(score.getPlanId()).isEqualTo(id);
            assertThat(score.getExerciseId()).isEqualTo(-1L);
            assertThat(score.getTotal()).isEqualTo(3);
            assertThat(score.getSuccessful()).isZero();
        });
        mvc.perform(get("/scores").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Demo User")))
                .andExpect(content().string(containsString("Straight pot")))
                .andExpect(content().string(containsString("href=\"" + exercisePath + "\"")))
                .andExpect(content().string(containsString("Independent")))
                .andExpect(content().string(containsString("0 / 3")));
    }

    @Test
    void historyShowsBothPracticeKindsAndFiltersByRosterWithoutChangingResults() throws Exception {
        var coach = account(1, Role.ROLE_COACH);
        var player = account(2, Role.ROLE_PLAYER);
        players.add(1L, "newplayer@example.com");
        Long joint = plan(2L, SessionKind.JOINT);
        Long independent = plan(2L, SessionKind.INDEPENDENT);
        Long other = plan(5L, SessionKind.JOINT);
        scores.save(1L, joint, -1L, new CreateScoreCmd(7, 4));
        scores.save(2L, independent, -1L, new CreateScoreCmd(3, 0));
        scores.save(1L, other, -1L, new CreateScoreCmd(5, 2));
        var before = scores.scores(1L);

        mvc.perform(get("/players").with(user(coach)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("href=\"/scores?playerId=2\"")));
        for (var request : List.of(
                get("/scores").param("playerId", "2").with(user(coach)),
                get("/scores").with(user(player)))) {
            mvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Demo User")))
                    .andExpect(content().string(containsString("Straight pot")))
                    .andExpect(content().string(containsString(">Joint<")))
                    .andExpect(content().string(containsString(">Independent<")))
                    .andExpect(content().string(containsString(">4 / 7<")))
                    .andExpect(content().string(containsString(">0 / 3<")))
                    .andExpect(content().string(not(containsString(">2 / 5<"))));
        }
        mvc.perform(get("/scores").param("playerId", "4").with(user(coach))).andExpect(status().isNotFound());
        mvc.perform(get("/scores").param("playerId", "5").with(user(account(4, Role.ROLE_PLAYER))))
                .andExpect(status().isForbidden());
        assertThat(scores.scores(1L)).containsExactlyElementsOf(before);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{\"total\":0,\"successful\":0}",
                "{\"total\":10,\"successful\":11}",
                "{\"total\":10,\"successful\":-1}",
                "{}"
            })
    void rejectsInvalidScoreCounts(String body) throws Exception {
        Long id = plan(2L, SessionKind.INDEPENDENT);
        mvc.perform(post("/training/" + id + "/exercises/-1/scores")
                        .with(user(account(2, Role.ROLE_PLAYER)))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
        assertThat(repository.count()).isZero();
    }

    @Test
    void requiresAuthenticationForScoreReads() throws Exception {
        mvc.perform(get("/scores")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/scores/1")).andExpect(status().is3xxRedirection());
    }
}
