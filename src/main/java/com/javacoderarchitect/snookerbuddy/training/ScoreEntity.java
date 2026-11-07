package com.javacoderarchitect.snookerbuddy.training;

import com.javacoderarchitect.snookerbuddy.shared.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "scores")
class ScoreEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "score_id_generator")
    @SequenceGenerator(name = "score_id_generator", sequenceName = "score_id_seq")
    private Long id;

    @Column(nullable = false)
    private Long planId;

    @Column(nullable = false)
    private Long exerciseId;

    @Column(nullable = false)
    private int total;

    @Column(nullable = false)
    private int successful;

    protected ScoreEntity() {}

    ScoreEntity(Long planId, Long exerciseId, int total, int successful) {
        this.planId = planId;
        this.exerciseId = exerciseId;
        this.total = total;
        this.successful = successful;
    }

    public Long getId() {
        return id;
    }

    public Long getPlanId() {
        return planId;
    }

    public Long getExerciseId() {
        return exerciseId;
    }

    public int getTotal() {
        return total;
    }

    public int getSuccessful() {
        return successful;
    }
}
