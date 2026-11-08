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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ExerciseController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class ExerciseNameSearchWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private ExerciseService service;

    @Autowired
    ExerciseNameSearchWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser coach() {
        return new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
    }

    @Test
    void searchesAsTheSignedInCoachAndKeepsTheQueryVisible() throws Exception {
        var exercise = new ExerciseDto(9L, "Straight pot", "Setup", "Pot", Difficulty.EASY, ExerciseType.TECHNIQUE);
        when(service.findLibrary(1L, "straight", null, null)).thenReturn(List.of(exercise));
        mvc.perform(get("/exercises").with(user(coach())).param("q", "straight").param("ownerId", "2"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Straight pot")))
                .andExpect(content().string(containsString("value=\"straight\"")))
                .andExpect(content().string(containsString("Clear search")));
        verify(service).findLibrary(1L, "straight", null, null);
        verifyNoMoreInteractions(service);
    }

    @Test
    void showsNoResultsAndProvidesAClearSearchLinkThatPreservesOtherFilters() throws Exception {
        when(service.findLibrary(1L, "missing", Difficulty.EASY, ExerciseType.TECHNIQUE))
                .thenReturn(List.of());
        var response = mvc.perform(get("/exercises")
                        .with(user(coach()))
                        .param("q", "missing")
                        .param("difficulty", "EASY")
                        .param("type", "TECHNIQUE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No exercises found")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        // Follow the rendered link rather than re-creating its URL in the assertion.
        var link = java.util.regex.Pattern.compile("<a[^>]+href=\"([^\"]+)\"[^>]*>Clear search</a>")
                .matcher(response);
        assertThat(link.find()).isTrue();
        String href = link.group(1).replace("&amp;", "&");
        assertThat(href).contains("difficulty=EASY", "type=TECHNIQUE").doesNotContain("q=");
        when(service.findLibrary(1L, "", Difficulty.EASY, ExerciseType.TECHNIQUE))
                .thenReturn(List.of(
                        new ExerciseDto(9L, "Straight pot", "Setup", "Pot", Difficulty.EASY, ExerciseType.TECHNIQUE)));
        mvc.perform(get(href).with(user(coach())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Straight pot")))
                .andExpect(content().string(not(containsString(">Clear search</a>"))));
        verify(service).findLibrary(1L, "", Difficulty.EASY, ExerciseType.TECHNIQUE);
    }

    @Test
    void searchControlsWorkWithAnOrdinaryGetFormAndTheLiveSearchPath() throws Exception {
        when(service.findLibrary(1L, "straight", null, null)).thenReturn(List.of());
        mvc.perform(get("/exercises").with(user(coach())).param("q", "straight").header("HX-Request", "true"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("method=\"get\"")))
                .andExpect(content().string(containsString("name=\"q\"")))
                .andExpect(content().string(containsString("input changed delay:300ms")))
                .andExpect(content().string(containsString("hx-get=\"/exercises\"")));
    }

    @Test
    void anonymousAndPlayerOnlyAccountsCannotSearchThePrivateLibrary() throws Exception {
        mvc.perform(get("/exercises").param("q", "straight")).andExpect(status().is3xxRedirection());
        var player = new SecurityUser(2L, "Player", "player@example.com", "unused", Role.ROLE_PLAYER);
        mvc.perform(get("/exercises").with(user(player)).param("q", "straight")).andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }
}
