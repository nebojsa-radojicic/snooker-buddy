package com.javacoderarchitect.snookerbuddy.training;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

public record CreateScoreCmd(
        @NotNull @Positive Integer total,
        @NotNull @PositiveOrZero Integer successful) {
    @AssertTrue(message = "Successful attempts cannot exceed total attempts")
    public boolean isValidScore() {
        return total == null || successful == null || successful <= total;
    }
}
