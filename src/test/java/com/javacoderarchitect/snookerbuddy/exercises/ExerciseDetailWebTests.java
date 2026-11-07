package com.javacoderarchitect.snookerbuddy.exercises;

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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

@WebMvcTest(ExerciseController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class ExerciseDetailWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private ExerciseService service;

    @Autowired
    ExerciseDetailWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser account(Role role) {
        return new SecurityUser(1L, "Coach", "coach@example.com", "unused", role);
    }

    @Test
    void displaysTheSavedDrawingAndAllExerciseMetadataUsingOnlyReadOperations() throws Exception {
        when(service.find(1L, 9L))
                .thenReturn(new ExerciseDto(
                        9L,
                        "The straight pot",
                        "Place the red on its spot.",
                        "Pot the red cleanly.",
                        Difficulty.MEDIUM,
                        ExerciseType.TECHNIQUE));
        var drawing = new DrawingService.Drawing(new byte[] {1, 2, 3}, "image/jpeg");
        when(service.drawing(1L, 9L)).thenReturn(drawing);
        mvc.perform(get("/exercises/9").with(user(account(Role.ROLE_COACH))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The straight pot")))
                .andExpect(content().string(containsString("Place the red on its spot.")))
                .andExpect(content().string(containsString("Pot the red cleanly.")))
                .andExpect(content().string(containsString("src=\"/exercises/9/drawing\"")))
                .andExpect(content().string(containsString("Exercise type: <span>Technique</span>")))
                .andExpect(content().string(containsString("Exercise difficulty: <span>Medium</span>")))
                .andExpect(content().string(not(containsString("Player level"))));
        mvc.perform(get("/exercises/9/drawing").with(user(account(Role.ROLE_COACH))))
                .andExpect(status().isOk())
                .andExpect(content().bytes(drawing.bytes()))
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", "no-store"));
        verify(service).find(1L, 9L);
        verify(service).drawing(1L, 9L);
        verifyNoMoreInteractions(service);
    }

    @Test
    void missingOrInaccessibleExerciseReturnsNotFoundWithoutItsDrawing() throws Exception {
        when(service.find(1L, 9L)).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
        when(service.drawing(1L, 9L))
                .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Exercise not found"));
        mvc.perform(get("/exercises/9").with(user(account(Role.ROLE_COACH)))).andExpect(status().isNotFound());
        mvc.perform(get("/exercises/9/drawing").with(user(account(Role.ROLE_COACH))))
                .andExpect(status().isNotFound());
    }

    @Test
    void playerCannotUseThePrivateLibraryDetailRoute() throws Exception {
        mvc.perform(get("/exercises/9").with(user(account(Role.ROLE_PLAYER)))).andExpect(status().isForbidden());
        mvc.perform(get("/exercises/9/drawing").with(user(account(Role.ROLE_PLAYER))))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @Test
    void anonymousViewerMustSignInToReadDetailsAndDrawings() throws Exception {
        mvc.perform(get("/exercises/9")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/exercises/9/drawing")).andExpect(status().is3xxRedirection());
        verifyNoInteractions(service);
    }
}
