package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.config.WebSecurityConfig;
import com.javacoderarchitect.snookerbuddy.exercises.*;
import com.javacoderarchitect.snookerbuddy.users.*;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(TrainingController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class IndividualAssignmentWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private TrainingService service;

    @MockitoBean
    private PlayerService players;

    @MockitoBean
    private ExercisesAPI exercises;

    @Autowired
    IndividualAssignmentWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser coach() {
        return new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
    }

    private MockHttpServletRequestBuilder assignment(SessionKind kind) {
        return post("/training")
                .with(user(coach()))
                .with(csrf())
                .param("playerId", "2")
                .param("exerciseIds", "7")
                .param("format", "INDIVIDUAL_EXERCISES")
                .param("kind", kind.name());
    }

    private void choices() {
        when(players.players(1L))
                .thenReturn(List.of(
                        new UserDto(2L, "Player", "player@example.com", Set.of(Role.ROLE_PLAYER), Instant.now())));
        when(exercises.library(1L))
                .thenReturn(List.of(new ExerciseDto(
                        7L, "Pot", "Setup", "Pot cleanly.", Difficulty.EASY, ExerciseType.TECHNIQUE, 20, 15)));
    }

    @Test
    void aSingleLibraryExerciseDefaultsToIndividualAssignment() throws Exception {
        choices();
        when(exercises.selected(1L, List.of(7L)))
                .thenReturn(List.of(new ExerciseDto(
                        7L, "Pot", "Setup", "Pot cleanly.", Difficulty.EASY, ExerciseType.TECHNIQUE, 20, 15)));
        var response = mvc.perform(get("/training/new").param("exercise", "7").with(user(coach())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Pot cleanly.")))
                .andExpect(content().string(containsString("15 / 20 default target")))
                .andExpect(content().string(containsString("name=\"successCriteria[7]\"")))
                .andReturn();
        var form = (CreateAssignmentCmd) response.getModelAndView().getModel().get("assignment");
        assertThat(form.exerciseIds()).containsExactly(7L);
        assertThat(form.format()).isEqualTo(AssignmentFormat.INDIVIDUAL_EXERCISES);
        assertThat(form.customTargets()).isFalse();
    }

    @ParameterizedTest
    @EnumSource(SessionKind.class)
    void submitsTheRecipientExerciseAndPracticeKindAsTheAuthenticatedCoach(SessionKind kind) throws Exception {
        when(service.assign(eq(1L), any())).thenReturn(List.of(3L));
        mvc.perform(assignment(kind).param("coachId", "99"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/training/3"));
        var cmd = ArgumentCaptor.forClass(CreateAssignmentCmd.class);
        verify(service).assign(eq(1L), cmd.capture());
        assertThat(cmd.getValue().playerId()).isEqualTo(2L);
        assertThat(cmd.getValue().exerciseIds()).containsExactly(7L);
        assertThat(cmd.getValue().format()).isEqualTo(AssignmentFormat.INDIVIDUAL_EXERCISES);
        assertThat(cmd.getValue().kind()).isEqualTo(kind);
        assertThat(cmd.getValue().customTargets()).isFalse();
    }

    @Test
    void ownershipRejectionReturnsToTheFormWithAnActionableError() throws Exception {
        choices();
        when(service.assign(eq(1L), any()))
                .thenThrow(new IllegalArgumentException("Choose exercises from your own library."));
        mvc.perform(assignment(SessionKind.INDEPENDENT))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("assignment"))
                .andExpect(content().string(containsString("Choose exercises from your own library.")));
    }

    @Test
    void missingExerciseCannotBeSubmitted() throws Exception {
        choices();
        mvc.perform(post("/training")
                        .with(user(coach()))
                        .with(csrf())
                        .param("playerId", "2")
                        .param("format", "INDIVIDUAL_EXERCISES")
                        .param("kind", "INDEPENDENT"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("assignment", "exerciseIds"));
        verifyNoInteractions(service);
    }

    @Test
    void playerCannotIssueAssignmentsAndCsrfRemainsRequired() throws Exception {
        var player = new SecurityUser(2L, "Player", "player@example.com", "unused", Role.ROLE_PLAYER);
        mvc.perform(assignment(SessionKind.INDEPENDENT)
                        .with(user(player))
                        .param("successCriteria[7]", "Change the coach's criterion."))
                .andExpect(status().isForbidden());
        mvc.perform(post("/training").with(user(coach()))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void bindsDistinctCriteriaForEachSelectedExercise() throws Exception {
        when(service.assign(eq(1L), any())).thenReturn(List.of(3L));
        mvc.perform(post("/training")
                        .with(user(coach()))
                        .with(csrf())
                        .param("playerId", "2")
                        .param("format", "TRAINING_SESSION")
                        .param("kind", "INDEPENDENT")
                        .param("exerciseIds", "-1", "-2")
                        .param("successCriteria[-1]", "  Pot with no cushion contact.  ")
                        .param("successCriteria[-2]", "Leave the cue ball on the spot.")
                        .param("successCriteria[-3]", " "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/training/3"));
        var cmd = ArgumentCaptor.forClass(CreateAssignmentCmd.class);
        verify(service).assign(eq(1L), cmd.capture());
        assertThat(cmd.getValue().successCriteria())
                .containsExactlyInAnyOrderEntriesOf(
                        java.util.Map.of(-1L, "Pot with no cushion contact.", -2L, "Leave the cue ball on the spot."));
        assertThat(cmd.getValue().customTargets()).isFalse();
    }

    @Test
    void rejectsWrittenCriteriaForUnselectedExercises() throws Exception {
        choices();
        mvc.perform(assignment(SessionKind.INDEPENDENT).param("successCriteria[8]", "Pot cleanly."))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Set a success criterion only for exercises included")));
        verifyNoInteractions(service);
    }

    @Test
    void rejectsWrittenCriteriaLongerThanTheLibraryLimit() throws Exception {
        choices();
        mvc.perform(assignment(SessionKind.INDEPENDENT).param("successCriteria[7]", "a".repeat(2001)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Success criterion must be at most 2000 characters")));
        verifyNoInteractions(service);
    }

    @Test
    void writtenOverrideDoesNotBypassNumericTargetValidation() throws Exception {
        choices();
        mvc.perform(assignment(SessionKind.INDEPENDENT)
                        .param("customTargets", "true")
                        .param("attempts", "10")
                        .param("target", "11")
                        .param("successCriteria[7]", "<Pot cleanly>"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a success target between 1 and the total attempts")))
                .andExpect(content().string(containsString("&lt;Pot cleanly&gt;")));
        verifyNoInteractions(service);
    }
}
