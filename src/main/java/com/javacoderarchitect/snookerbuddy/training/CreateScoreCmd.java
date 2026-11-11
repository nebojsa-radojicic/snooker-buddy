package com.javacoderarchitect.snookerbuddy.training;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.annotation.JsonDeserialize;

public record CreateScoreCmd(
        @JsonDeserialize(using = WholeNumberDeserializer.class)
        @NotNull(message = "Enter the total attempts performed.")
        @Positive(message = "Total attempts must be greater than zero.")
        Integer total,

        @JsonDeserialize(using = WholeNumberDeserializer.class)
        @NotNull(message = "Enter the number of successful attempts.")
        @PositiveOrZero(message = "Successful attempts must be zero or greater.")
        Integer successful) {
    @AssertTrue(message = "Successful attempts cannot exceed total attempts")
    public boolean isValidScore() {
        return total == null || successful == null || successful <= total;
    }

    static final class WholeNumberDeserializer extends ValueDeserializer<Integer> {
        @Override
        public Integer deserialize(JsonParser parser, DeserializationContext context) {
            if (parser.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
                return context.reportInputMismatch(Integer.class, "Attempt counts must be whole numbers.");
            }
            return context.readValue(parser, Integer.class);
        }
    }
}
