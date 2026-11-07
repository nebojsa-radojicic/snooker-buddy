package com.javacoderarchitect.snookerbuddy.exercises;

public record ExerciseDto(
        Long id, String name, String description, String goal, Difficulty difficulty, ExerciseType type) {
    static ExerciseDto from(ExerciseEntity exercise) {
        return new ExerciseDto(
                exercise.getId(),
                exercise.getName(),
                exercise.getDescription(),
                exercise.getGoal(),
                exercise.getDifficulty(),
                exercise.getType());
    }
}
