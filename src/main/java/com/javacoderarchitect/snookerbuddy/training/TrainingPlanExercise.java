package com.javacoderarchitect.snookerbuddy.training;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
class TrainingPlanExercise {
    @Column(name = "exercise_id", nullable = false)
    private Long exerciseId;

    @Column(nullable = false)
    private int attempts;

    @Column(nullable = false)
    private int target;

    protected TrainingPlanExercise() {}

    TrainingPlanExercise(Long exerciseId, int attempts, int target) {
        this.exerciseId = exerciseId;
        this.attempts = attempts;
        this.target = target;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getTarget() {
        return target;
    }
}
