package com.javacoderarchitect.snookerbuddy.exercises;

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
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;

@Sql("/test-data.sql")
class ExerciseTests extends BaseIT {
    private final MockMvc mockMvc;
    private final ExerciseService service;

    @Autowired
    ExerciseTests(MockMvc mockMvc, ExerciseService service) {
        this.mockMvc = mockMvc;
        this.service = service;
    }

    private SecurityUser coach(long id) {
        return new SecurityUser(id, "Coach", "coach@example.com", "unused", Role.ROLE_COACH);
    }

    private MockMultipartFile photo() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(1800, 900, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("drawing", "practice.png", "image/png", output.toByteArray());
    }

    @Test
    void savesAllFieldsAndServesTheDrawingOnlyToItsOwner() throws Exception {
        var result = mockMvc.perform(multipart("/exercises")
                        .file(photo())
                        .with(user(coach(1)))
                        .with(csrf())
                        .param("name", "The straight pot")
                        .param("description", "Place the red on its spot.")
                        .param("goal", "Pot the red.")
                        .param("difficulty", "MEDIUM")
                        .param("type", "TECHNIQUE"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        var url = result.getResponse().getRedirectedUrl();
        mockMvc.perform(get(url).with(user(coach(1))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The straight pot")))
                .andExpect(content().string(containsString("Place the red on its spot.")))
                .andExpect(content().string(containsString("Pot the red.")))
                .andExpect(content().string(containsString("Medium")))
                .andExpect(content().string(containsString("Technique")));
        var image = mockMvc.perform(get(url + "/drawing").with(user(coach(1))))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn()
                .getResponse()
                .getContentAsByteArray();
        var decoded = ImageIO.read(new java.io.ByteArrayInputStream(image));
        assertThat(decoded.getWidth()).isEqualTo(1600);
        mockMvc.perform(get(url).with(user(coach(2)))).andExpect(status().isNotFound());
        mockMvc.perform(get(url + "/drawing").with(user(coach(2)))).andExpect(status().isNotFound());
        mockMvc.perform(get("/exercises").with(user(coach(2))))
                .andExpect(content().string(not(containsString("The straight pot"))));
    }

    @Test
    void combinesNameDifficultyAndTypeFilters() throws Exception {
        service.create(
                1L,
                new CreateExerciseCmd(
                        "Straight pot", "Setup", "Pot", Difficulty.EASY, ExerciseType.TECHNIQUE, photo()));
        service.create(
                1L,
                new CreateExerciseCmd(
                        "Safety drill", "Setup", "Safe", Difficulty.HARD, ExerciseType.SAFETY_PLAY, photo()));
        mockMvc.perform(get("/exercises")
                        .with(user(coach(1)))
                        .param("q", "STRAIGHT")
                        .param("difficulty", "EASY")
                        .param("type", "TECHNIQUE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Straight pot")))
                .andExpect(content().string(not(containsString("Safety drill"))));
        mockMvc.perform(get("/exercises")
                        .with(user(coach(1)))
                        .param("q", "Straight")
                        .param("difficulty", "HARD"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No exercises found")));
    }

    @Test
    void rejectsMissingFieldsAndInvalidImagesWithoutSaving() throws Exception {
        mockMvc.perform(multipart("/exercises")
                        .with(user(coach(1)))
                        .with(csrf())
                        .param("name", " "))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Exercise name is required")));
        mockMvc.perform(multipart("/exercises")
                        .file(new MockMultipartFile("drawing", "fake.png", "image/png", "not an image".getBytes()))
                        .with(user(coach(1)))
                        .with(csrf())
                        .param("name", "Fake")
                        .param("description", "Setup")
                        .param("goal", "Pot")
                        .param("difficulty", "EASY")
                        .param("type", "TECHNIQUE"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Choose a readable JPG or PNG drawing.")));
        assertThat(service.findLibrary(1L, "", null, null)).isEmpty();
    }

    @Test
    void requiresAuthenticationAndCsrfAndRendersNewExerciseForm() throws Exception {
        mockMvc.perform(get("/exercises")).andExpect(status().is3xxRedirection());
        mockMvc.perform(multipart("/exercises").file(photo()).with(user(coach(1))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/exercises/new").with(user(coach(1))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("name=\"_csrf\"")))
                .andExpect(content().string(containsString("multipart/form-data")));
    }
}
