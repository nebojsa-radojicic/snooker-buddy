package com.javacoderarchitect.snookerbuddy.training;

import static org.assertj.core.api.Assertions.assertThat;

import com.javacoderarchitect.snookerbuddy.BaseIT;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AssignmentTargetsMigrationTests extends BaseIT {
    private final DataSource source;

    @Autowired
    AssignmentTargetsMigrationTests(DataSource source) {
        this.source = source;
    }

    @Test
    void migratingExistingPlansCopiesTheirTargetsAndPreservesScores() throws Exception {
        String schema = "target_migration_" + UUID.randomUUID().toString().replace("-", "");
        try (var connection = source.getConnection()) {
            connection.setAutoCommit(true);
            try (var statement = connection.createStatement()) {
                try {
                    Flyway.configure()
                            .dataSource(source)
                            .schemas(schema)
                            .defaultSchema(schema)
                            .locations("classpath:db/migration")
                            .target("6")
                            .load()
                            .migrate();
                    statement.executeUpdate(
                            "insert into " + schema + ".coach_players(coach_id, player_id) values (1, 2)");
                    statement.executeUpdate(
                            "insert into " + schema
                                    + ".exercises(id, owner_id, name, description, goal, difficulty, type, drawing, drawing_content_type) "
                                    + "values (-1, 1, 'Pot', 'Setup', 'Goal', 'EASY', 'TECHNIQUE', decode('00', 'hex'), 'image/jpeg')");
                    statement.executeUpdate("insert into " + schema
                            + ".training_plans(id, coach_id, player_id, name, format, kind, attempts, target) "
                            + "values (-1, 1, 2, 'Legacy plan', 'TRAINING_SESSION', 'JOINT', 20, 13)");
                    statement.executeUpdate("insert into " + schema
                            + ".training_plan_exercises(plan_id, position, exercise_id) values (-1, 0, -1)");
                    statement.executeUpdate("insert into " + schema
                            + ".scores(id, plan_id, exercise_id, total, successful) values (-1, -1, -1, 20, 11)");
                    Flyway.configure()
                            .dataSource(source)
                            .schemas(schema)
                            .defaultSchema(schema)
                            .locations("classpath:db/migration")
                            .load()
                            .migrate();
                    try (var row = statement.executeQuery(
                            "select attempts, target from " + schema + ".training_plan_exercises where plan_id = -1")) {
                        assertThat(row.next()).isTrue();
                        assertThat(row.getInt("attempts")).isEqualTo(20);
                        assertThat(row.getInt("target")).isEqualTo(13);
                    }
                    try (var row = statement.executeQuery(
                            "select default_attempts, default_target from " + schema + ".exercises where id = -1")) {
                        assertThat(row.next()).isTrue();
                        assertThat(row.getInt("default_attempts")).isEqualTo(10);
                        assertThat(row.getInt("default_target")).isEqualTo(8);
                    }
                    try (var row = statement.executeQuery(
                            "select total, successful from " + schema + ".scores where id = -1")) {
                        assertThat(row.next()).isTrue();
                        assertThat(row.getInt("total")).isEqualTo(20);
                        assertThat(row.getInt("successful")).isEqualTo(11);
                    }
                } finally {
                    statement.execute("drop schema if exists " + schema + " cascade");
                }
            }
        }
    }
}
