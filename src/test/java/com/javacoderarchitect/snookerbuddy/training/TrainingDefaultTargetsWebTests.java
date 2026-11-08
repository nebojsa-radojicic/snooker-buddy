package com.javacoderarchitect.snookerbuddy.training;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.config.WebSecurityConfig;
import com.javacoderarchitect.snookerbuddy.exercises.*;
import com.javacoderarchitect.snookerbuddy.users.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrainingController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class TrainingDefaultTargetsWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private TrainingService service;

    @MockitoBean
    private PlayerService players;

    @MockitoBean
    private ExercisesAPI exercises;

    private final List<ExerciseDto> library = List.of(
            new ExerciseDto(7L, "Pot", "Setup", "Pot cleanly.", Difficulty.EASY, ExerciseType.TECHNIQUE, 20, 15),
            new ExerciseDto(
                    8L, "Safety", "Setup", "Leave the white safe.", Difficulty.HARD, ExerciseType.SAFETY_PLAY, 12, 9));

    @Autowired
    TrainingDefaultTargetsWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void assignmentPreparationShowsEachDefaultAndItsSuccessCriterion() throws Exception {
        when(exercises.selected(1L, List.of(7L))).thenReturn(List.of(library.getFirst()));
        when(exercises.library(1L)).thenReturn(library);
        when(players.players(1L))
                .thenReturn(List.of(
                        new UserDto(2L, "Player", "player@example.com", Set.of(Role.ROLE_PLAYER), Instant.now())));
        var coach = new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
        mvc.perform(get("/training/new").param("exercise", "7").with(user(coach)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("15 / 20 default target")))
                .andExpect(content().string(containsString("9 / 12 default target")))
                .andExpect(content().string(containsString("Pot cleanly.")))
                .andExpect(content().string(containsString("Leave the white safe.")));
    }

    @Test
    void allPlanViewsDisplayTargetsForTheRelevantExercise() throws Exception {
        var plan = new TrainingPlanDto(
                3L,
                "Practice",
                1L,
                "Coach",
                2L,
                "Player",
                AssignmentFormat.TRAINING_SESSION,
                SessionKind.INDEPENDENT,
                Instant.now(),
                library);
        when(service.plans(2L)).thenReturn(List.of(plan));
        when(service.plan(2L, 3L)).thenReturn(plan);
        when(service.exercise(2L, 3L, 7L)).thenReturn(library.getFirst());
        var player = new SecurityUser(2L, "Player", "player@example.com", "unused", Role.ROLE_PLAYER);
        for (String path : List.of("/training", "/training/3")) {
            mvc.perform(get(path).with(user(player)))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("15 / 20 target")))
                    .andExpect(content().string(containsString("9 / 12 target")));
        }
        mvc.perform(get("/training/3/exercises/7").with(user(player)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("15 successful / 20 attempts")));
    }
}
