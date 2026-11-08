package com.javacoderarchitect.snookerbuddy.exercises;

import jakarta.validation.constraints.*;
import org.springframework.web.multipart.MultipartFile;

public record CreateExerciseCmd(
        @NotBlank(message = "Exercise name is required") @Size(max = 60)
        String name,

        @NotBlank(message = "Setup and instructions are required") @Size(max = 5000)
        String description,

        @NotBlank(message = "Goal is required") @Size(max = 2000)
        String goal,

        @NotNull(message = "Choose a difficulty") Difficulty difficulty,
        @NotNull(message = "Choose an exercise type") ExerciseType type,
        MultipartFile drawing,

        @Min(value = 1, message = "Attempts must be between 1 and 1000")
        @Max(value = 1000, message = "Attempts must be between 1 and 1000")
        Integer attempts,

        @Min(value = 1, message = "Success target must be at least 1")
        Integer target) {
    public static final int DEFAULT_ATTEMPTS = 10;
    public static final int DEFAULT_TARGET = 8;

    public CreateExerciseCmd {
        attempts = attempts == null ? DEFAULT_ATTEMPTS : attempts;
        target = target == null ? DEFAULT_TARGET : target;
    }

    public CreateExerciseCmd(
            String name,
            String description,
            String goal,
            Difficulty difficulty,
            ExerciseType type,
            MultipartFile drawing) {
        this(name, description, goal, difficulty, type, drawing, DEFAULT_ATTEMPTS, DEFAULT_TARGET);
    }

    @AssertTrue(message = "Successful attempts target cannot exceed total attempts")
    public boolean isValidTargets() {
        return target <= attempts;
    }
}
