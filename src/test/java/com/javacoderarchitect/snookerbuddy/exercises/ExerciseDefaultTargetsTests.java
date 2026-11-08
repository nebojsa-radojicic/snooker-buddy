package com.javacoderarchitect.snookerbuddy.exercises;

import static org.assertj.core.api.Assertions.*;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import jakarta.validation.ConstraintViolationException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;

@Sql("/test-data.sql")
class ExerciseDefaultTargetsTests extends BaseIT {
    private final ExerciseService service;

    @Autowired
    ExerciseDefaultTargetsTests(ExerciseService service) {
        this.service = service;
    }

    private CreateExerciseCmd exercise(int attempts, int target) throws Exception {
        var bytes = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(30, 20, BufferedImage.TYPE_INT_RGB), "png", bytes);
        return new CreateExerciseCmd(
                "Pot",
                "Reset the red.",
                "Pot cleanly.",
                Difficulty.EASY,
                ExerciseType.TECHNIQUE,
                new MockMultipartFile("drawing", "drawing.png", "image/png", bytes.toByteArray()),
                attempts,
                target);
    }

    @Test
    void savedTargetsSurviveReopeningAndLibraryRetrieval() throws Exception {
        var id = service.create(1L, exercise(20, 15));
        var reopened = service.find(1L, id);
        assertThat(reopened.attempts()).isEqualTo(20);
        assertThat(reopened.target()).isEqualTo(15);
        assertThat(reopened.goal()).isEqualTo("Pot cleanly.");
        assertThat(service.findLibrary(1L, "pot", null, null)).containsExactly(reopened);
    }

    @Test
    void directServiceCallsCannotSaveInvalidTargets() throws Exception {
        var invalid = exercise(10, 11);
        assertThatThrownBy(() -> service.create(1L, invalid)).isInstanceOf(ConstraintViolationException.class);
        assertThat(service.findLibrary(1L, "", null, null)).isEmpty();
    }
}
