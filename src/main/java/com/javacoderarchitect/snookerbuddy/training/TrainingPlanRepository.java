package com.javacoderarchitect.snookerbuddy.training;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

interface TrainingPlanRepository extends JpaRepository<TrainingPlanEntity, Long> {
    List<TrainingPlanEntity> findByCoachIdOrPlayerIdOrderByCreatedAtDescIdDesc(Long coachId, Long playerId);
}
