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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class ExerciseSavingWebTests {
    private final MockMvc mvc;

    @MockitoBean
    private ExerciseService service;

    @Autowired
    ExerciseSavingWebTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    private SecurityUser account(Role role) {
        return new SecurityUser(1L, "Account", "account@example.com", "unused", role);
    }

    private MockMultipartHttpServletRequestBuilder upload() {
        return upload(null);
    }

    private MockMultipartHttpServletRequestBuilder upload(String omittedField) {
        var request = multipart("/exercises")
                .file(new MockMultipartFile("drawing", "photo.png", "image/png", new byte[] {1, 2, 3}));
        request.with(user(account(Role.ROLE_COACH))).with(csrf());
        Map.of(
                        "name",
                        "Straight pot",
                        "description",
                        "Place the red on its spot.",
                        "goal",
                        "Pot the red.",
                        "difficulty",
                        "MEDIUM",
                        "type",
                        "TECHNIQUE")
                .forEach((name, value) -> {
                    if (!name.equals(omittedField)) request.param(name, value);
                });
        return request;
    }

    @Test
    void savesUsingAuthenticatedCoachIdentityWithoutRequiringAPlayerOrTableEditor() throws Exception {
        when(service.create(eq(1L), any())).thenReturn(9L);
        mvc.perform(upload().param("ownerId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/exercises/9"));
        var command = ArgumentCaptor.forClass(CreateExerciseCmd.class);
        verify(service).create(eq(1L), command.capture());
        assertThat(command.getValue().name()).isEqualTo("Straight pot");
        assertThat(command.getValue().description()).isEqualTo("Place the red on its spot.");
        assertThat(command.getValue().goal()).isEqualTo("Pot the red.");
        assertThat(command.getValue().difficulty()).isEqualTo(Difficulty.MEDIUM);
        assertThat(command.getValue().type()).isEqualTo(ExerciseType.TECHNIQUE);
        assertThat(command.getValue().drawing().getBytes()).containsExactly(1, 2, 3);
    }

    @Test
    void playerCannotSubmitAnExerciseWithAValidFormAndCsrfToken() throws Exception {
        mvc.perform(upload().with(user(account(Role.ROLE_PLAYER))).param("ownerId", "2"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(service);
    }

    @ParameterizedTest
    @ValueSource(strings = {"name", "description", "goal", "difficulty", "type"})
    void rejectsMissingMetadataWithoutSaving(String field) throws Exception {
        mvc.perform(upload(field))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("exercise", field));
        verifyNoInteractions(service);
    }

    @Test
    void uploadFailureRetainsMetadataAndExplainsHowToRetry() throws Exception {
        when(service.create(eq(1L), any())).thenThrow(new IllegalArgumentException("Upload your exercise drawing."));
        mvc.perform(upload())
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("exercise", "drawing"))
                .andExpect(content().string(containsString("Upload your exercise drawing.")))
                .andExpect(content().string(containsString("Straight pot")))
                .andExpect(content().string(containsString("Place the red on its spot.")));
    }
}
