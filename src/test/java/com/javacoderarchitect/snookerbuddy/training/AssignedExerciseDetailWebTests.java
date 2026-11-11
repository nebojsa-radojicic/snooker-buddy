package com.javacoderarchitect.snookerbuddy.training;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.config.WebSecurityConfig;
import com.javacoderarchitect.snookerbuddy.exercises.*;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(TrainingController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class AssignedExerciseDetailWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private TrainingService service;

    @MockitoBean
    private PlayerService players;

    @MockitoBean
    private ExercisesAPI exercises;

    @Autowired
    AssignedExerciseDetailWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser account(long id, Role role) {
        return new SecurityUser(id, "Account", "account@example.com", "unused", role);
    }

    @ParameterizedTest
    @CsvSource({
        "1,ROLE_COACH,INDEPENDENT,false",
        "2,ROLE_PLAYER,INDEPENDENT,true",
        "1,ROLE_COACH,JOINT,true",
        "2,ROLE_PLAYER,JOINT,false"
    })
    void bothParticipantsCanReviewTheExerciseButOnlyTheRecorderGetsTheLink(
            long viewer, Role role, SessionKind kind, boolean canRecord) throws Exception {
        var exercise = new ExerciseDto(
                9L,
                "The stop shot",
                "Reset the position after each shot.",
                "Stop the white near the red.",
                Difficulty.HARD,
                ExerciseType.CUE_BALL_CONTROL,
                12,
                9);
        var plan = new TrainingPlanDto(
                5L,
                "Control practice",
                1L,
                "Coach",
                2L,
                "Player",
                AssignmentFormat.TRAINING_SESSION,
                kind,
                Instant.now(),
                List.of(exercise));
        when(service.exercise(viewer, 5L, 9L)).thenReturn(exercise);
        when(service.plan(viewer, 5L)).thenReturn(plan);
        var drawing = new ExerciseDrawingDto(new byte[] {4, 5, 6}, "image/jpeg");
        when(service.drawing(viewer, 5L, 9L)).thenReturn(drawing);
        var principal = account(viewer, role);
        var recordLink = containsString("href=\"/training/5/exercises/9/scores\"");
        mvc.perform(get("/training/5/exercises/9").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The stop shot")))
                .andExpect(content().string(containsString("Reset the position after each shot.")))
                .andExpect(content().string(containsString("Stop the white near the red.")))
                .andExpect(content().string(containsString("src=\"/training/5/exercises/9/drawing\"")))
                .andExpect(content().string(containsString("Exercise type: <span>Cue-ball control</span>")))
                .andExpect(content().string(containsString("Exercise difficulty: <span>Hard</span>")))
                .andExpect(content().string(canRecord ? recordLink : not(recordLink)))
                .andExpect(content().string(not(containsString("Player level"))));
        mvc.perform(get("/training/5/exercises/9/drawing").with(user(principal)))
                .andExpect(status().isOk())
                .andExpect(content().bytes(drawing.bytes()))
                .andExpect(header().string("Cache-Control", "no-store"));
        verify(service).exercise(viewer, 5L, 9L);
        verify(service).plan(viewer, 5L);
        verify(service).drawing(viewer, 5L, 9L);
        verifyNoMoreInteractions(service);
        verifyNoInteractions(players, exercises);
    }

    @Test
    void inaccessibleOrUnassignedExerciseDoesNotRenderPrivateInstructions() throws Exception {
        when(service.exercise(3L, 5L, 9L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Training plan not found"));
        when(service.drawing(3L, 5L, 9L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Training plan not found"));
        var principal = account(3, Role.ROLE_PLAYER);
        mvc.perform(get("/training/5/exercises/9").with(user(principal)))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("Reset the position after each shot."))));
        mvc.perform(get("/training/5/exercises/9/drawing").with(user(principal)))
                .andExpect(status().isNotFound());
        verify(service, never()).plan(anyLong(), anyLong());
    }
}
