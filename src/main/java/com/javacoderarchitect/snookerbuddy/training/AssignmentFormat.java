package com.javacoderarchitect.snookerbuddy.training;

public enum AssignmentFormat {
    TRAINING_SESSION("Training session"),
    INDIVIDUAL_EXERCISES("Individual exercises");
    private final String label;

    AssignmentFormat(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
