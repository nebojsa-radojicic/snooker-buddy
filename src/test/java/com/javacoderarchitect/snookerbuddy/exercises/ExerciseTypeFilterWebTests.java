package com.javacoderarchitect.snookerbuddy.exercises;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.config.WebSecurityConfig;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExerciseController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class ExerciseTypeFilterWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private ExerciseService service;

    @Autowired
    ExerciseTypeFilterWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser coach(long id) {
        return new SecurityUser(id, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
    }

    @ParameterizedTest
    @EnumSource(ExerciseType.class)
    void filtersAsTheSignedInCoachAndMarksTheChosenType(ExerciseType type) throws Exception {
        var exercise = new ExerciseDto(9L, "Practice drill", "Setup", "Goal", Difficulty.EASY, type);
        when(service.findLibrary(1L, "", null, type)).thenReturn(List.of(exercise));
        var response = mvc.perform(get("/exercises")
                        .with(user(coach(1)))
                        .param("type", type.name())
                        .param("ownerId", "2"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("type", type))
                .andExpect(content().string(containsString("Practice drill")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var tab = Pattern.compile("<a\\b([^>]*)>" + Pattern.quote(type.getLabel()) + "</a>")
                .matcher(response);
        assertThat(tab.find()).isTrue();
        assertThat(tab.group(1)).contains("type=" + type.name(), "aria-current=\"true\"");
        verify(service).findLibrary(1L, "", null, type);
        verifyNoMoreInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 2})
    void creationAndRetrievalOfferTheSamePredefinedTypesForEveryCoach(long coachId) throws Exception {
        when(service.findLibrary(coachId, "", null, null)).thenReturn(List.of());
        var library = mvc.perform(get("/exercises").with(user(coach(coachId))))
                .andExpect(status().isOk())
                .andReturn();
        var form = mvc.perform(get("/exercises/new").with(user(coach(coachId))))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(library.getModelAndView().getModel().get("types")).isEqualTo(ExerciseType.values());
        assertThat(form.getModelAndView().getModel().get("types")).isEqualTo(ExerciseType.values());
        var html = form.getResponse().getContentAsString();
        for (var type : ExerciseType.values()) {
            assertThat(Pattern.compile("<option[^>]*value=\"" + type.name() + "\"[^>]*>"
                                    + Pattern.quote(type.getLabel()) + "</option>")
                            .matcher(html)
                            .find())
                    .isTrue();
        }
    }

    @Test
    void clearingTheTypeFromTheRenderedLinkRetainsNameAndDifficulty() throws Exception {
        when(service.findLibrary(1L, "straight", Difficulty.HARD, ExerciseType.TECHNIQUE))
                .thenReturn(List.of());
        var response = mvc.perform(get("/exercises")
                        .with(user(coach(1)))
                        .param("q", "straight")
                        .param("difficulty", "HARD")
                        .param("type", "TECHNIQUE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No exercises found")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var clear = Pattern.compile("<a[^>]+href=\"([^\"]+)\"[^>]*>All exercises</a>")
                .matcher(response);
        assertThat(clear.find()).isTrue();
        var href = clear.group(1).replace("&amp;", "&");
        assertThat(href).contains("q=straight", "difficulty=HARD").doesNotContain("type=");
        var exercise =
                new ExerciseDto(9L, "Straight stop", "Setup", "Goal", Difficulty.HARD, ExerciseType.CUE_BALL_CONTROL);
        when(service.findLibrary(1L, "straight", Difficulty.HARD, null)).thenReturn(List.of(exercise));
        mvc.perform(get(href).with(user(coach(1))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Straight stop")))
                .andExpect(model().attributeDoesNotExist("type"));
        verify(service).findLibrary(1L, "straight", Difficulty.HARD, null);
    }

    @Test
    void liveTypeFilteringRendersAnEmptyStateWhenNothingMatches() throws Exception {
        when(service.findLibrary(1L, "missing", null, ExerciseType.SAFETY_PLAY)).thenReturn(List.of());
        mvc.perform(get("/exercises")
                        .with(user(coach(1)))
                        .param("q", "missing")
                        .param("type", "SAFETY_PLAY")
                        .header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No exercises found")))
                .andExpect(content().string(containsString("0 exercises")))
                .andExpect(content().string(not(containsString("class=\"exercise-card\""))));
    }

    @Test
    void rejectsAnUnrecognizedTypeInsteadOfCreatingACustomType() throws Exception {
        mvc.perform(multipart("/exercises")
                        .with(user(coach(1)))
                        .with(csrf())
                        .file(new MockMultipartFile("drawing", "photo.png", "image/png", new byte[] {1}))
                        .param("name", "Custom drill")
                        .param("description", "Setup")
                        .param("goal", "Goal")
                        .param("difficulty", "EASY")
                        .param("type", "COACH_CUSTOM_TYPE"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("exercise", "type"));
        verifyNoInteractions(service);
    }
}
