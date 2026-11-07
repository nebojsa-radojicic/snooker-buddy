package com.javacoderarchitect.snookerbuddy.training;

public enum SessionKind {
    INDEPENDENT("Independent"),
    JOINT("Joint");
    private final String label;

    SessionKind(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
