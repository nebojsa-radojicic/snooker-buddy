package com.javacoderarchitect.snookerbuddy.exercises;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ExerciseRepository extends JpaRepository<ExerciseEntity, Long> {
    @Query("""
        select new com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto(e.id, e.name, e.description, e.goal, e.difficulty, e.type, e.attempts, e.target)
        from ExerciseEntity e
        where e.ownerId = :ownerId
          and locate(:search, lower(e.name)) > 0
          and (:difficulty is null or e.difficulty = :difficulty)
          and (:type is null or e.type = :type)
        order by e.createdAt desc, e.id desc
        """)
    List<ExerciseDto> findLibrary(Long ownerId, String search, Difficulty difficulty, ExerciseType type);

    @Query("""
        select new com.javacoderarchitect.snookerbuddy.exercises.ExerciseDto(e.id, e.name, e.description, e.goal, e.difficulty, e.type, e.attempts, e.target)
        from ExerciseEntity e where e.ownerId = :ownerId and e.id in :ids
        """)
    List<ExerciseDto> findOwned(Long ownerId, List<Long> ids);

    Optional<ExerciseEntity> findByIdAndOwnerId(Long id, Long ownerId);
}
