package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.assertThat;
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
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@Sql({"/test-data.sql", "/exercise-search-data.sql", "/training-data.sql"})
class TrainingAssignmentsTests extends BaseIT {
    private final MockMvc mvc;
    private final TrainingService service;

    @Autowired
    TrainingAssignmentsTests(MockMvc mvc, TrainingService service) {
        this.mvc = mvc;
        this.service = service;
    }

    private SecurityUser account(long id) {
        return new SecurityUser(id, "Account", "account@example.com", "unused", Role.ROLE_USER);
    }

    private MockHttpServletRequestBuilder assignment(String player, String format, String... ids) {
        var request = post("/training")
                .with(user(account(1)))
                .with(csrf())
                .param("playerId", player)
                .param("name", "Cue-ball session")
                .param("format", format)
                .param("kind", "JOINT");
        if (ids.length > 0) request.param("exerciseIds", ids);
        return request;
    }

    private Long group() {
        return service.assign(
                        1L,
                        new CreateAssignmentCmd(
                                2L,
                                "Practice",
                                AssignmentFormat.TRAINING_SESSION,
                                SessionKind.INDEPENDENT,
                                List.of(-1L),
                                false,
                                null,
                                null))
                .getFirst();
    }

    @Test
    void groupsExercisesWithTargetsAndRetainsTheirOrder() throws Exception {
        var result = mvc.perform(assignment("2", "TRAINING_SESSION", "-2", "-1")
                        .param("customTargets", "true")
                        .param("attempts", "12")
                        .param("target", "9"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        var plans = service.plans(1L);
        assertThat(plans).hasSize(1);
        var plan = plans.getFirst();
        assertThat(plan.name()).isEqualTo("Cue-ball session");
        assertThat(plan.exercises()).extracting(e -> e.id()).containsExactly(-2L, -1L);
        assertThat(plan.kind()).isEqualTo(SessionKind.JOINT);
        assertThat(plan.attempts()).isEqualTo(12);
        assertThat(plan.target()).isEqualTo(9);
        assertThat(result.getResponse().getRedirectedUrl()).isEqualTo("/training/" + plan.id());
        mvc.perform(get("/training").with(user(account(1))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Cue-ball session")))
                .andExpect(content().string(containsString("Demo User")));
        mvc.perform(get("/training/" + plan.id()).with(user(account(2))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("9 / 12 target")));
    }

    @Test
    void individualModeCreatesSeparatePlansNamedAfterEachExercise() throws Exception {
        mvc.perform(assignment("2", "INDIVIDUAL_EXERCISES", "-1", "-2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/training"));
        var plans = service.plans(2L);
        assertThat(plans).hasSize(2);
        assertThat(plans).extracting(TrainingPlanDto::name).containsExactlyInAnyOrder("Straight pot", "Straight stop");
        assertThat(plans).allSatisfy(plan -> {
            assertThat(plan.exercises()).hasSize(1);
            assertThat(plan.format()).isEqualTo(AssignmentFormat.INDIVIDUAL_EXERCISES);
            assertThat(plan.attempts()).isEqualTo(10);
            assertThat(plan.target()).isEqualTo(8);
        });
    }

    @Test
    void blankSessionNameUsesDefaultAndSingleExerciseRemainsAValidPlan() {
        var id = service.assign(
                        1L,
                        new CreateAssignmentCmd(
                                2L,
                                " ",
                                AssignmentFormat.TRAINING_SESSION,
                                SessionKind.INDEPENDENT,
                                List.of(-1L),
                                false,
                                999,
                                999))
                .getFirst();
        var plan = service.plan(2L, id);
        assertThat(plan.name()).isEqualTo("Practice session");
        assertThat(plan.target()).isEqualTo(8);
        assertThat(plan.attempts()).isEqualTo(10);
    }

    @Test
    void assignedPlayerCanReadOnlyExercisesAndDrawingsIncludedInThePlan() throws Exception {
        Long id = group();
        String exercise = "/training/" + id + "/exercises/-1";
        for (long userId : List.of(1L, 2L)) {
            mvc.perform(get(exercise).with(user(account(userId))))
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("Straight pot")))
                    .andExpect(content().string(containsString("8 successful / 10 attempts")));
            mvc.perform(get(exercise + "/drawing").with(user(account(userId))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType("image/jpeg"))
                    .andExpect(header().string("Cache-Control", "no-store"));
        }
        for (String path : List.of("/training/" + id, exercise, exercise + "/drawing")) {
            mvc.perform(get(path).with(user(account(3)))).andExpect(status().isNotFound());
        }
        for (String path : List.of("/training/" + id + "/exercises/-2", "/training/" + id + "/exercises/-2/drawing")) {
            mvc.perform(get(path).with(user(account(2)))).andExpect(status().isNotFound());
        }
        mvc.perform(get("/exercises/-1").with(user(account(2)))).andExpect(status().isNotFound());
        mvc.perform(get("/exercises/-1/drawing").with(user(account(2)))).andExpect(status().isNotFound());
        assertThat(service.plans(3L)).isEmpty();
    }

    @Test
    void rejectsOtherCoachesPlayersForeignExercisesAndDuplicatesWithoutPartialAssignments() throws Exception {
        mvc.perform(assignment("4", "TRAINING_SESSION", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose a player from your list.")));
        mvc.perform(assignment("2", "INDIVIDUAL_EXERCISES", "-1", "-7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose exercises from your own library.")));
        mvc.perform(assignment("2", "TRAINING_SESSION", "-1", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose each exercise only once.")));
        mvc.perform(assignment("2", "TRAINING_SESSION"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose at least one exercise")));
        assertThat(service.plans(1L)).isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"0,1", "10,11", "1001,8", "10,0", "-1,-1"})
    void rejectsInvalidPersonalTargets(String attempts, String target) throws Exception {
        mvc.perform(assignment("2", "TRAINING_SESSION", "-1")
                        .param("customTargets", "true")
                        .param("attempts", attempts)
                        .param("target", target))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Enter a success target")));
        assertThat(service.plans(1L)).isEmpty();
    }

    @Test
    void preselectsLibraryExerciseAndDisplaysOnlyTheCoachsPlayersAndExercises() throws Exception {
        var result = mvc.perform(get("/training/new").with(user(account(1))).param("exercise", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Demo User")))
                .andExpect(content().string(not(containsString("Other Player"))))
                .andExpect(content().string(not(containsString("Straight private"))))
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andReturn();
        var form = (CreateAssignmentCmd) result.getModelAndView().getModel().get("assignment");
        assertThat(form.exerciseIds()).containsExactly(-1L);
        mvc.perform(get("/training/new").with(user(account(1))).param("exercise", "-7"))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(get("/training")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/players")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/training").with(user(account(1)))).andExpect(status().isForbidden());
        mvc.perform(post("/players").with(user(account(1)))).andExpect(status().isForbidden());
    }

    @Test
    void linksAnExistingAccountAndProtectsTheSingleCoachRelationship() throws Exception {
        mvc.perform(post("/players").with(user(account(1))).with(csrf()).param("email", " NEWPLAYER@example.com "))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/training/new"));
        mvc.perform(get("/players").with(user(account(1))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("New Player")))
                .andExpect(content().string(not(containsString("Other Player"))));
        for (var email :
                List.of("otherplayer@example.com", "demouser@gmail.com", "admin@gmail.com", "unknown@example.com")) {
            mvc.perform(post("/players").with(user(account(1))).with(csrf()).param("email", email))
                    .andExpect(status().isOk())
                    .andExpect(model().attributeHasFieldErrors("player", "email"));
        }
    }
}
