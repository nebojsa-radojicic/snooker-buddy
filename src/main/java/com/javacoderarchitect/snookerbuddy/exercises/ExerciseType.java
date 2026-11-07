package com.javacoderarchitect.snookerbuddy.exercises;

public enum ExerciseType {
    TECHNIQUE("Technique"),
    CUE_BALL_CONTROL("Cue-ball control"),
    POSITIONAL_PLAY("Positional play"),
    TACTICS("Tactics"),
    SAFETY_PLAY("Safety play"),
    MENTAL_PREPARATION("Mental preparation");
    private final String label;

    ExerciseType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
