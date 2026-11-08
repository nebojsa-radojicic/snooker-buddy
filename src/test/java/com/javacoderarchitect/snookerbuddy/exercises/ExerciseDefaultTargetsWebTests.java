package com.javacoderarchitect.snookerbuddy.exercises;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.config.WebSecurityConfig;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

@WebMvcTest(ExerciseController.class)
@Import(WebSecurityConfig.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class ExerciseDefaultTargetsWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private ExerciseService service;

    @Autowired
    ExerciseDefaultTargetsWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser coach() {
        return new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
    }

    private MockMultipartHttpServletRequestBuilder form() {
        var request = multipart("/exercises")
                .file(new MockMultipartFile("drawing", "drawing.png", "image/png", new byte[] {1}));
        request.with(user(coach()))
                .with(csrf())
                .param("name", "Pot")
                .param("description", "Reset the red.")
                .param("goal", "Pot cleanly.")
                .param("difficulty", "EASY")
                .param("type", "TECHNIQUE");
        return request;
    }

    @Test
    void newExerciseFormOffersDefaultAttemptAndSuccessTargets() throws Exception {
        mvc.perform(get("/exercises/new").with(user(coach())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Default targets")))
                .andExpect(content().string(containsString("name=\"attempts\" value=\"10\"")))
                .andExpect(content().string(containsString("name=\"target\" value=\"8\"")));
    }

    @Test
    void acceptsTargetsAndTheExistingTextSuccessCriterion() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(9L);
        mvc.perform(form().param("attempts", "20").param("target", "15"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/exercises/9"));
        var cmd = ArgumentCaptor.forClass(CreateExerciseCmd.class);
        verify(service).create(eq(1L), cmd.capture());
        assertThat(cmd.getValue().attempts()).isEqualTo(20);
        assertThat(cmd.getValue().target()).isEqualTo(15);
        assertThat(cmd.getValue().goal()).isEqualTo("Pot cleanly.");
    }

    @Test
    void omittedDefaultsPreserveTheExistingTenAttemptsAndEightSuccesses() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(9L);
        mvc.perform(form()).andExpect(status().is3xxRedirection());
        var cmd = ArgumentCaptor.forClass(CreateExerciseCmd.class);
        verify(service).create(eq(1L), cmd.capture());
        assertThat(cmd.getValue().attempts()).isEqualTo(10);
        assertThat(cmd.getValue().target()).isEqualTo(8);
    }

    @ParameterizedTest
    @CsvSource({"1,1", "1000,1000", "10,10"})
    void acceptsBoundaryTargetsAndTargetsEqualToAttempts(String attempts, String target) throws Exception {
        when(service.create(eq(1L), any())).thenReturn(9L);
        mvc.perform(form().param("attempts", attempts).param("target", target))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/exercises/9"));
    }

    @ParameterizedTest
    @CsvSource({"0,1", "1001,8", "10,11", "10,0", "-1,-1"})
    void invalidTargetsDoNotReachStorage(String attempts, String target) throws Exception {
        mvc.perform(form().param("attempts", attempts).param("target", target))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasErrors("exercise"));
        verifyNoInteractions(service);
    }

    @Test
    void savedDefaultTargetsAreVisibleInExerciseDetails() throws Exception {
        when(service.find(1L, 9L))
                .thenReturn(new ExerciseDto(
                        9L, "Pot", "Reset the red.", "Pot cleanly.", Difficulty.EASY, ExerciseType.TECHNIQUE, 20, 15));
        mvc.perform(get("/exercises/9").with(user(coach())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Library default target:")))
                .andExpect(content().string(containsString("15 successful / 20 attempts")));
    }
}
