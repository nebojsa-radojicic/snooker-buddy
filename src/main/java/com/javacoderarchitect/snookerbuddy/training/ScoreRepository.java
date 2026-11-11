package com.javacoderarchitect.snookerbuddy.training;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ScoreRepository extends JpaRepository<ScoreEntity, Long> {
    @Query("""
            select s from ScoreEntity s, TrainingPlanEntity p
            where s.planId = p.id
            and ((:coach = true and p.coachId = :userId) or (:player = true and p.playerId = :userId))
            and p.playerId = coalesce(:selectedPlayerId, p.playerId)
            order by s.createdAt desc, s.id desc
            """)
    List<ScoreEntity> findVisible(Long userId, boolean coach, boolean player, Long selectedPlayerId);
}
