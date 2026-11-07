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
        MultipartFile drawing) {}
