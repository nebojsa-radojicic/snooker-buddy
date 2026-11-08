package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto;
import java.time.Instant;
import java.util.List;

public record TrainingPlanDto(
        Long id,
        String name,
        Long coachId,
        String coachName,
        Long playerId,
        String playerName,
        AssignmentFormat format,
        SessionKind kind,
        Instant createdAt,
        List<ExerciseDto> exercises) {}
