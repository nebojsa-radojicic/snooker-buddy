package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@SqlGroup({
    @Sql({"/test-data.sql", "/exercise-search-data.sql", "/training-data.sql"}),
    @Sql(statements = "update exercises set default_attempts = 20, default_target = 15 where id = -1")
})
class IndividualAssignmentTests extends BaseIT {
    private final MockMvc mvc;
    private final JdbcTemplate jdbc;

    @Autowired
    IndividualAssignmentTests(MockMvc mvc, JdbcTemplate jdbc) {
        this.mvc = mvc;
        this.jdbc = jdbc;
    }

    private SecurityUser account(long id, Role role) {
        return new SecurityUser(id, "Account", "account@example.com", "unused", role);
    }

    private MockHttpServletRequestBuilder assignment(SessionKind kind, String player, String exercise) {
        return post("/training")
                .with(user(account(1, Role.ROLE_COACH)))
                .with(csrf())
                .param("playerId", player)
                .param("exerciseIds", exercise)
                .param("format", "INDIVIDUAL_EXERCISES")
                .param("kind", kind.name());
    }

    @ParameterizedTest
    @EnumSource(SessionKind.class)
    void persistsAnIndividualAssignmentAndLetsOnlyItsRecipientAndCoachReadIt(SessionKind kind) throws Exception {
        var created = mvc.perform(
                        assignment(kind, "2", "-1").param("attempts", "999").param("target", "999"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String location = created.getResponse().getRedirectedUrl();
        var recipient = account(2, Role.ROLE_PLAYER);
        var opened = mvc.perform(get(location).with(user(recipient)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Straight pot")))
                .andExpect(content().string(containsString("15 / 20 target")))
                .andReturn();
        var plan = (TrainingPlanDto) opened.getModelAndView().getModel().get("plan");
        assertThat(plan.playerId()).isEqualTo(2L);
        assertThat(plan.coachId()).isEqualTo(1L);
        assertThat(plan.format()).isEqualTo(AssignmentFormat.INDIVIDUAL_EXERCISES);
        assertThat(plan.kind()).isEqualTo(kind);
        assertThat(plan.exercises()).singleElement().satisfies(exercise -> {
            assertThat(exercise.id()).isEqualTo(-1L);
            assertThat(exercise.attempts()).isEqualTo(20);
            assertThat(exercise.target()).isEqualTo(15);
        });
        mvc.perform(get(location + "/exercises/-1").with(user(recipient)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Setup")))
                .andExpect(content().string(containsString("Pot the red")))
                .andExpect(content().string(containsString("15 successful / 20 attempts")));
        mvc.perform(get(location + "/exercises/-1/drawing").with(user(recipient)))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[] {0}));
        mvc.perform(get(location).with(user(account(1, Role.ROLE_COACH)))).andExpect(status().isOk());
        mvc.perform(get(location).with(user(account(4, Role.ROLE_PLAYER)))).andExpect(status().isNotFound());
        mvc.perform(get(location + "/exercises/-2").with(user(recipient))).andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject("select count(*) from training_plans", Long.class))
                .isEqualTo(1);
    }

    @Test
    void rejectedOwnershipAndPlayerRequestsLeaveNoAssignments() throws Exception {
        mvc.perform(assignment(SessionKind.INDEPENDENT, "4", "-1"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose a player from your list.")));
        mvc.perform(assignment(SessionKind.INDEPENDENT, "2", "-7"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose exercises from your own library.")));
        mvc.perform(assignment(SessionKind.INDEPENDENT, "2", "-1").with(user(account(4, Role.ROLE_PLAYER))))
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("select count(*) from training_plans", Long.class))
                .isZero();
    }
}
