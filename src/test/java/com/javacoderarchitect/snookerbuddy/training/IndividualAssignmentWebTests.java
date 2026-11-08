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
        mvc.perform(assignment(SessionKind.INDEPENDENT).with(user(player))).andExpect(status().isForbidden());
        mvc.perform(post("/training").with(user(coach()))).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
