package com.finanzapp.movements.repository;

import com.finanzapp.movements.repository.entities.MovementEntity;
import com.finanzapp.movements.repository.entities.MovementType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MovementRepository extends JpaRepository<MovementEntity, Long> {

    @Query("""
        SELECT DISTINCT CAST(EXTRACT(YEAR FROM m.date) AS integer)
        FROM MovementEntity m
        WHERE m.userEmail = :email
        ORDER BY CAST(EXTRACT(YEAR FROM m.date) AS integer)
    """)
    List<Integer> findDistinctYearsByUserEmail(@Param("email") String email);

    List<MovementEntity> findByUserEmailAndDateBetweenOrderByDate(
            String userEmail,
            LocalDate from,
            LocalDate to
    );

    List<MovementEntity> findByUserEmailAndTagIdAndDateBetweenOrderByDate(
            String userEmail,
            Long tagId,
            LocalDate from,
            LocalDate to
    );


    List<MovementEntity> findByUserEmailAndMovementTypeAndDateBetweenOrderByDateAsc(
            String userEmail,
            MovementType movementType,
            LocalDate from,
            LocalDate to
    );

    List<MovementEntity> findByUserEmailAndMovementTypeAndTagIdAndDateBetweenOrderByDateAsc(
            String userEmail,
            MovementType movementType,
            Long tagId,
            LocalDate from,
            LocalDate to
    );



    List<MovementEntity> findByUserEmailAndMovementTypeOrderByDateAsc(
            String userEmail,
            MovementType movementType
    );

    List<MovementEntity> findByUserEmailAndMovementTypeAndTagIdOrderByDateAsc(
            String userEmail,
            MovementType movementType,
            Long tagId
    );


    Optional<MovementEntity> findByIdAndUserEmail(Long id, String userEmail);

    List<MovementEntity> findByUserEmailAndTagId(String userEmail, Long tagId);

    List<MovementEntity> findByUserEmail(String userEmail);



}
