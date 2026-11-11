package com.javacoderarchitect.snookerbuddy.training;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.config.WebSecurityConfig;
import com.javacoderarchitect.snookerbuddy.exercises.Difficulty;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseType;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ScoreController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class ScoreControllerTests {
    private final MockMvc mvc;

    @MockitoBean
    private ScoreService service;

    @Autowired
    ScoreControllerTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser player() {
        return new SecurityUser(2L, "Player", "player@example.com", "unused", Role.ROLE_PLAYER);
    }

    private ScoreDto score() {
        return new ScoreDto(
                9L, 3L, 7L, 2L, "Player", "Pot", SessionKind.INDEPENDENT, 10, 8, Instant.parse("2026-11-07T12:00:00Z"));
    }

    private void givenRecordingContext() {
        var exercise = new ExerciseDto(7L, "Pot", "Setup", "Goal", Difficulty.EASY, ExerciseType.TECHNIQUE)
                .withTargets(12, 9, "Use a smooth cue action");
        when(service.recording(2L, 3L, 7L))
                .thenReturn(new TrainingPlanDto(
                        3L,
                        "Practice",
                        1L,
                        "Coach",
                        2L,
                        "Player",
                        AssignmentFormat.TRAINING_SESSION,
                        SessionKind.INDEPENDENT,
                        Instant.now(),
                        List.of(exercise)));
    }

    @Test
    void rendersRecordingContextAndSavesUsingTheAssignmentPath() throws Exception {
        givenRecordingContext();
        String path = "/training/3/exercises/7/scores";
        mvc.perform(get(path).with(user(player())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Record result for Pot")))
                .andExpect(content().string(containsString("Player · Practice · Independent practice")))
                .andExpect(content().string(containsString("9 successful / 12 attempts")))
                .andExpect(content().string(containsString("Use a smooth cue action")))
                .andExpect(content().string(containsString("action=\"" + path + "\"")));
        mvc.perform(post(path)
                        .with(user(player()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("total", "10")
                        .param("successful", "0")
                        .param("playerId", "4")
                        .param("planId", "5")
                        .param("exerciseId", "6"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/scores"));
        verify(service).save(2L, 3L, 7L, new CreateScoreCmd(10, 0));
    }

    @ParameterizedTest
    @CsvSource({
        "'',0,Enter the total attempts performed.",
        "0,0,Total attempts must be greater than zero.",
        "1.5,0,Total attempts must be a whole number.",
        "5,'',Enter the number of successful attempts.",
        "5,-1,Successful attempts must be zero or greater.",
        "5,1.5,Successful attempts must be a whole number.",
        "5,6,Successful attempts cannot exceed total attempts"
    })
    void invalidSummaryShowsTheProblemAndDoesNotSave(String total, String successful, String message) throws Exception {
        givenRecordingContext();
        mvc.perform(post("/training/3/exercises/7/scores")
                        .with(user(player()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("total", total)
                        .param("successful", successful))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(message)))
                .andExpect(content().string(containsString("9 successful / 12 attempts")));
        verify(service, never()).save(anyLong(), anyLong(), anyLong(), any());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"{\"total\":10,\"successful\":8,\"playerId\":4}", "{\"total\":\"10\",\"successful\":\"8\"}"})
    void storesCountsForTheAuthenticatedIdentityAndReturnsLocation(String body) throws Exception {
        when(service.save(2L, 3L, 7L, new CreateScoreCmd(10, 8))).thenReturn(score());
        mvc.perform(post("/training/3/exercises/7/scores")
                        .with(user(player()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/scores/9"))
                .andExpect(jsonPath("$.playerId").value(2))
                .andExpect(jsonPath("$.successful").value(8));
    }

    @Test
    void rendersAuthorizedScoresAndSupportsDirectReads() throws Exception {
        when(service.scores(2L)).thenReturn(List.of(score()));
        when(service.score(2L, 9L)).thenReturn(score());
        mvc.perform(get("/scores").with(user(player())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Actual successful / total attempts")))
                .andExpect(content().string(containsString("Independent")))
                .andExpect(content().string(containsString("8 / 10")));
        mvc.perform(get("/scores/9").with(user(player())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exerciseId").value(7))
                .andExpect(jsonPath("$.kind").value("INDEPENDENT"))
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void inaccessibleAndMissingScoresBothReturnNotFound() throws Exception {
        when(service.score(2L, 9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND));
        mvc.perform(get("/scores/9").with(user(player()))).andExpect(status().isNotFound());
    }

    @Test
    void refusesForbiddenWritesWithTheCorrectHttpStatus() throws Exception {
        when(service.save(2L, 3L, 7L, new CreateScoreCmd(10, 8))).thenThrow(new AccessDeniedException("Forbidden"));
        mvc.perform(post("/training/3/exercises/7/scores")
                        .with(user(player()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\":10,\"successful\":8}"))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"total\":0,\"successful\":0}",
                "{\"total\":10,\"successful\":11}",
                "{\"total\":1.5,\"successful\":0}",
                "{\"total\":10,\"successful\":1.5}",
                "{\"total\":2147483648,\"successful\":0}",
                "not json"
            })
    void invalidInputReturnsBadRequestWithoutCallingStorage(String body) throws Exception {
        mvc.perform(post("/training/3/exercises/7/scores")
                        .with(user(player()))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").isNotEmpty());
        verifyNoInteractions(service);
    }

    @Test
    void requiresSignInBusinessRolesAndCsrf() throws Exception {
        mvc.perform(get("/scores")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/scores/9")
                        .with(user(new SecurityUser(1L, "Admin", "admin@example.com", "unused", Role.ROLE_ADMIN))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/training/3/exercises/7/scores")
                        .with(user(player()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"total\":10,\"successful\":8}"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void playerRoleCannotReachCoachEndpointsEvenWithCsrf() throws Exception {
        for (String path : List.of("/exercises", "/players", "/training/new")) {
            mvc.perform(get(path).with(user(player()))).andExpect(status().isForbidden());
        }
        mvc.perform(post("/training").with(user(player())).with(csrf())).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
