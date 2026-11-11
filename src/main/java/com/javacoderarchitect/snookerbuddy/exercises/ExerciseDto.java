package com.javacoderarchitect.snookerbuddy.exercises;

public record ExerciseDto(
        Long id,
        String name,
        String description,
        String goal,
        Difficulty difficulty,
        ExerciseType type,
        int attempts,
        int target) {
    public ExerciseDto(
            Long id, String name, String description, String goal, Difficulty difficulty, ExerciseType type) {
        this(
                id,
                name,
                description,
                goal,
                difficulty,
                type,
                CreateExerciseCmd.DEFAULT_ATTEMPTS,
                CreateExerciseCmd.DEFAULT_TARGET);
    }

    static ExerciseDto from(ExerciseEntity exercise) {
        return new ExerciseDto(
                exercise.getId(),
                exercise.getName(),
                exercise.getDescription(),
                exercise.getGoal(),
                exercise.getDifficulty(),
                exercise.getType(),
                exercise.getAttempts(),
                exercise.getTarget());
    }

    public ExerciseDto withTargets(int plannedAttempts, int successTarget) {
        return withTargets(plannedAttempts, successTarget, goal);
    }

    public ExerciseDto withTargets(int plannedAttempts, int successTarget, String successCriterion) {
        return new ExerciseDto(
                id, name, description, successCriterion, difficulty, type, plannedAttempts, successTarget);
    }
}
