package com.javacoderarchitect.snookerbuddy.training;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface CoachPlayerRepository extends JpaRepository<CoachPlayerEntity, Long> {
    List<CoachPlayerEntity> findByCoachIdOrderByCreatedAtAsc(Long coachId);

    boolean existsByCoachIdAndPlayerId(Long coachId, Long playerId);
}
