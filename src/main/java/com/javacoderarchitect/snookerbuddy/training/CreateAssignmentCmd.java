package com.javacoderarchitect.snookerbuddy.training;

import jakarta.validation.constraints.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Integer target,
        Map<Long, @Size(max = 2000, message = "Success criterion must be at most 2000 characters") String>
                successCriteria) {
    public CreateAssignmentCmd {
        customTargets = Boolean.TRUE.equals(customTargets);
        var overrides = new LinkedHashMap<Long, String>();
        if (successCriteria != null) {
            successCriteria.forEach((id, criterion) -> {
                if (criterion != null && !criterion.isBlank()) {
                    overrides.put(id, criterion.strip());
                }
            });
        }
        successCriteria = Collections.unmodifiableMap(overrides);
    }

    public CreateAssignmentCmd(
            Long playerId,
            String name,
            AssignmentFormat format,
            SessionKind kind,
            List<Long> exerciseIds,
            Boolean customTargets,
            Integer attempts,
            Integer target) {
        this(playerId, name, format, kind, exerciseIds, customTargets, attempts, target, Map.of());
    }

    @AssertTrue(message = "Set a success criterion only for exercises included in the assignment")
    public boolean isValidSuccessCriteria() {
        return successCriteria.isEmpty() || (exerciseIds != null && exerciseIds.containsAll(successCriteria.keySet()));
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
