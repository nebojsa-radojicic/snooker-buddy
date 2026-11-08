package com.javacoderarchitect.snookerbuddy.exercises;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.config.WebSecurityConfig;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExerciseController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class ExerciseDifficultyFilterWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private ExerciseService service;

    @Autowired
    ExerciseDifficultyFilterWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser coach() {
        return new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
    }

    @ParameterizedTest
    @EnumSource(Difficulty.class)
    void appliesTheSelectedDifficultyToTheSignedInCoachsCollection(Difficulty difficulty) throws Exception {
        var exercise = new ExerciseDto(9L, "Practice drill", "Setup", "Goal", difficulty, ExerciseType.TECHNIQUE);
        when(service.findLibrary(1L, "", difficulty, null)).thenReturn(List.of(exercise));
        var response = mvc.perform(get("/exercises")
                        .with(user(coach()))
                        .param("difficulty", difficulty.name())
                        .param("ownerId", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("difficulty", difficulty))
                .andExpect(content().string(containsString("Practice drill")))
                .andExpect(content().string(containsString("Filter by difficulty")))
                .andExpect(content().string(containsString("1 exercise")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var selectedOption = java.util.regex.Pattern.compile("<option[^>]*value=\"" + difficulty.name()
                + "\"[^>]*selected(?:=\"selected\")?[^>]*>" + difficulty.getLabel() + "</option>");
        assertThat(selectedOption.matcher(response).find()).isTrue();
        verify(service).findLibrary(1L, "", difficulty, null);
        verifyNoMoreInteractions(service);
    }

    @Test
    void selectingAllDifficultiesClearsOnlyTheDifficultyRestriction() throws Exception {
        var exercise = new ExerciseDto(9L, "Straight pot", "Setup", "Goal", Difficulty.EASY, ExerciseType.TECHNIQUE);
        when(service.findLibrary(1L, "straight", null, ExerciseType.TECHNIQUE)).thenReturn(List.of(exercise));
        mvc.perform(get("/exercises")
                        .with(user(coach()))
                        .param("q", "straight")
                        .param("difficulty", "")
                        .param("type", "TECHNIQUE"))
                .andExpect(status().isOk())
                .andExpect(model().attributeDoesNotExist("difficulty"))
                .andExpect(content().string(containsString("All difficulties")))
                .andExpect(content().string(containsString("Straight pot")))
                .andExpect(content().string(containsString("value=\"straight\"")))
                .andExpect(content().string(containsString("name=\"type\" value=\"TECHNIQUE\"")));
        verify(service).findLibrary(1L, "straight", null, ExerciseType.TECHNIQUE);
        verifyNoMoreInteractions(service);
    }

    @Test
    void noMatchingExercisesProducesAnEmptyStateForNormalAndLiveFiltering() throws Exception {
        when(service.findLibrary(1L, "straight", Difficulty.HARD, ExerciseType.TECHNIQUE))
                .thenReturn(List.of());
        for (boolean live : List.of(false, true)) {
            var request = get("/exercises")
                    .with(user(coach()))
                    .param("q", "straight")
                    .param("difficulty", "HARD")
                    .param("type", "TECHNIQUE");
            if (live) request.header("HX-Request", "true");
            mvc.perform(request)
                    .andExpect(status().isOk())
                    .andExpect(content().string(containsString("No exercises found")))
                    .andExpect(content().string(containsString("0 exercises")))
                    .andExpect(content().string(not(containsString("class=\"exercise-card\""))))
                    .andExpect(content().string(containsString("change from:select")));
        }
    }

    @Test
    void privateDifficultyFilteringRequiresACoachAccount() throws Exception {
        mvc.perform(get("/exercises").param("difficulty", "EASY")).andExpect(status().is3xxRedirection());
        var player = new SecurityUser(2L, "Player", "player@example.com", "unused", Role.ROLE_PLAYER);
        mvc.perform(get("/exercises").with(user(player)).param("difficulty", "EASY"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
