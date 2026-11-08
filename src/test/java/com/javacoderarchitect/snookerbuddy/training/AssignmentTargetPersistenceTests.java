package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.assertThat;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlGroup;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SqlGroup({
    @Sql({"/test-data.sql", "/exercise-search-data.sql", "/training-data.sql"}),
    @Sql(
            statements = {
                "update exercises set default_attempts = 20, default_target = 15 where id = -1",
                "update exercises set default_attempts = 12, default_target = 9 where id = -2"
            })
})
class AssignmentTargetPersistenceTests extends BaseIT {
    private final TrainingService service;
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    @Autowired
    AssignmentTargetPersistenceTests(TrainingService service, JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.service = service;
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
    }

    private CreateAssignmentCmd assignment(AssignmentFormat format, boolean customize) {
        return new CreateAssignmentCmd(
                2L, "Practice", format, SessionKind.INDEPENDENT, List.of(-1L, -2L), customize, 30, 22);
    }

    @Test
    void groupedSessionPersistsDifferentDefaultsAndLaterLibraryChangesDoNotChangeThem() {
        var id = service.assign(1L, assignment(AssignmentFormat.TRAINING_SESSION, false))
                .getFirst();
        transaction.executeWithoutResult(status ->
                jdbc.update("update exercises set default_attempts = 100, default_target = 80 where id in (-1, -2)"));
        var plan = service.plan(2L, id);
        assertThat(plan.exercises()).extracting(ExerciseDto::attempts).containsExactly(20, 12);
        assertThat(plan.exercises()).extracting(ExerciseDto::target).containsExactly(15, 9);
        assertThat(service.exercise(2L, id, -1L).attempts()).isEqualTo(20);
        assertThat(service.exercise(2L, id, -2L).target()).isEqualTo(9);
    }

    @Test
    void individualAssignmentsKeepTheirOwnDefaults() {
        var ids = service.assign(1L, assignment(AssignmentFormat.INDIVIDUAL_EXERCISES, false));
        assertThat(ids).hasSize(2);
        assertThat(service.plan(2L, ids.get(0)).exercises().getFirst().attempts())
                .isEqualTo(20);
        assertThat(service.plan(2L, ids.get(1)).exercises().getFirst().target()).isEqualTo(9);
    }

    @Test
    void personalizedAssignmentsDoNotModifyLibraryDefaults() {
        var id = service.assign(1L, assignment(AssignmentFormat.TRAINING_SESSION, true))
                .getFirst();
        assertThat(service.plan(2L, id).exercises()).allSatisfy(exercise -> {
            assertThat(exercise.attempts()).isEqualTo(30);
            assertThat(exercise.target()).isEqualTo(22);
        });
        assertThat(jdbc.queryForList(
                        "select default_attempts from exercises where id in (-1, -2) order by id desc", Integer.class))
                .containsExactly(20, 12);
        assertThat(jdbc.queryForList(
                        "select default_target from exercises where id in (-1, -2) order by id desc", Integer.class))
                .containsExactly(15, 9);
    }
}
