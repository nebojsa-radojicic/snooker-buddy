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

    // Apply to an assignment the caller has already been authorized to access.
    boolean allowsRecordingBy(Long userId, Long coachId, Long playerId) {
        return userId.equals(this == JOINT ? coachId : playerId);
    }
}
