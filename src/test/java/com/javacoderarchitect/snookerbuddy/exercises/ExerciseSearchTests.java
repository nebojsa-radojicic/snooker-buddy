package com.javacoderarchitect.snookerbuddy.exercises;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.list;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql({"/test-data.sql", "/exercise-search-data.sql"})
class ExerciseSearchTests extends BaseIT {
    private final MockMvc mvc;

    @Autowired
    ExerciseSearchTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser coach() {
        return new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
    }

    static Stream<Arguments> filters() {
        return Stream.of(
                Arguments.of("  STRAIGHT  ", "", "", new String[] {"Straight stop", "Straight pot"}),
                Arguments.of("", "EASY", "", new String[] {"Safety 100% drill", "Straight pot"}),
                Arguments.of("", "MEDIUM", "", new String[] {"Focus_under pressure", "Position drill"}),
                Arguments.of("", "HARD", "", new String[] {"Tactical choice", "Straight stop"}),
                Arguments.of("", "", "TECHNIQUE", new String[] {"Straight pot"}),
                Arguments.of("", "", "CUE_BALL_CONTROL", new String[] {"Straight stop"}),
                Arguments.of("", "", "POSITIONAL_PLAY", new String[] {"Position drill"}),
                Arguments.of("", "", "TACTICS", new String[] {"Tactical choice"}),
                Arguments.of("", "", "SAFETY_PLAY", new String[] {"Safety 100% drill"}),
                Arguments.of("", "", "MENTAL_PREPARATION", new String[] {"Focus_under pressure"}),
                Arguments.of("straight", "EASY", "TECHNIQUE", new String[] {"Straight pot"}),
                Arguments.of("straight", "HARD", "TECHNIQUE", new String[] {}),
                Arguments.of("missing", "", "", new String[] {}),
                Arguments.of("%", "", "", new String[] {"Safety 100% drill"}),
                Arguments.of("_", "", "", new String[] {"Focus_under pressure"}),
                Arguments.of("' or 1=1 --", "", "", new String[] {}),
                Arguments.of("  ", "", "", new String[] {
                    "Focus_under pressure",
                    "Safety 100% drill",
                    "Tactical choice",
                    "Position drill",
                    "Straight stop",
                    "Straight pot"
                }));
    }

    @ParameterizedTest
    @MethodSource("filters")
    void filtersByNameDifficultyAndSharedTypes(String q, String difficulty, String type, String[] expected)
            throws Exception {
        var result = mvc.perform(get("/exercises")
                        .with(user(coach()))
                        .param("q", q)
                        .param("difficulty", difficulty)
                        .param("type", type))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(result.getModelAndView().getModel().get("exercises"))
                .asInstanceOf(list(ExerciseDto.class))
                .extracting(ExerciseDto::name)
                .containsExactly(expected);
    }

    @Test
    void rendersLiveControlsAndRetainsSelectedFilters() throws Exception {
        mvc.perform(get("/exercises")
                        .with(user(coach()))
                        .param("q", "straight")
                        .param("difficulty", "EASY")
                        .param("type", "TECHNIQUE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("id=\"exercise-search\"")))
                .andExpect(content().string(containsString("input changed delay:300ms")))
                .andExpect(content().string(containsString("hx-sync=\"#exercise-library:replace\"")))
                .andExpect(content().string(containsString("1 exercise")))
                .andExpect(content().string(containsString("name=\"type\" value=\"TECHNIQUE\"")))
                .andExpect(content().string(containsString("value=\"straight\"")))
                .andExpect(content().string(containsString("aria-current=\"true\"")));
    }

    @Test
    void usesTheSamePredefinedTypesForSavingAndSearching() throws Exception {
        var library = mvc.perform(get("/exercises").with(user(coach())))
                .andExpect(status().isOk())
                .andReturn();
        var form = mvc.perform(get("/exercises/new").with(user(coach())))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(library.getModelAndView().getModel().get("types")).isEqualTo(ExerciseType.values());
        assertThat(form.getModelAndView().getModel().get("types")).isEqualTo(ExerciseType.values());
    }
}
