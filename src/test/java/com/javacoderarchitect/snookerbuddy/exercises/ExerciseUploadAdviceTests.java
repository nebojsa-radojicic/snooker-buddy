package com.javacoderarchitect.snookerbuddy.exercises;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.javacoderarchitect.snookerbuddy.ApplicationProperties;
import com.javacoderarchitect.snookerbuddy.users.Role;
import com.javacoderarchitect.snookerbuddy.users.SecurityUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@WebMvcTest(ExerciseController.class)
@EnableConfigurationProperties(ApplicationProperties.class)
class ExerciseUploadAdviceTests {
    private final MockMvc mvc;

    @MockitoBean
    private ExerciseService service;

    @Autowired
    ExerciseUploadAdviceTests(MockMvc mvc) {
        this.mvc = mvc;
    }

    @Test
    void oversizedUploadRendersAnActionableFormWithAllChoices() throws Exception {
        when(service.create(any(), any())).thenThrow(new MaxUploadSizeExceededException(DrawingService.MAX_BYTES));
        var principal = new SecurityUser(1L, "Coach", "coach@example.com", "unused", Role.ROLE_USER);
        mvc.perform(multipart("/exercises")
                        .file(new MockMultipartFile("drawing", "drawing.png", "image/png", new byte[] {1}))
                        .with(user(principal))
                        .with(csrf())
                        .param("name", "Pot")
                        .param("description", "Setup")
                        .param("goal", "Success")
                        .param("difficulty", "EASY")
                        .param("type", "TECHNIQUE"))
                .andExpect(status().is(413))
                .andExpect(content().string(containsString("Choose a photo up to 20 MB")))
                .andExpect(content().string(containsString("Mental preparation")))
                .andExpect(content().string(containsString("Medium")));
    }
}
