package com.javacoderarchitect.snookerbuddy.training;

import java.time.Instant;

public record ScoreDto(
        Long id,
        Long planId,
        Long exerciseId,
        Long playerId,
        String playerName,
        String exerciseName,
        int total,
        int successful,
        Instant createdAt) {}
