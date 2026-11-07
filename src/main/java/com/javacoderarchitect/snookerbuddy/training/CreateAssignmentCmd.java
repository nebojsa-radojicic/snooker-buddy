package com.javacoderarchitect.snookerbuddy.training;

import jakarta.validation.constraints.*;
import java.util.List;

public record CreateAssignmentCmd(
        @NotNull(message = "Choose a player") Long playerId,

        @Size(max = 120, message = "Training name must be at most 120 characters")
        String name,

        @NotNull(message = "Choose an assignment format") AssignmentFormat format,
        @NotNull(message = "Choose a session type") SessionKind kind,

        @NotEmpty(message = "Choose at least one exercise") @Size(max = 100, message = "Choose up to 100 exercises")
        List<@NotNull Long> exerciseIds,

        Boolean customTargets,
        Integer attempts,
        Integer target) {
    public CreateAssignmentCmd {
        customTargets = Boolean.TRUE.equals(customTargets);
    }

    @AssertTrue(message = "Enter a success target between 1 and the total attempts (up to 1000)")
    public boolean isValidTargets() {
        return !customTargets
                || (attempts != null
                        && target != null
                        && attempts >= 1
                        && attempts <= 1000
                        && target >= 1
                        && target <= attempts);
    }
}
