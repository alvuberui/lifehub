package com.finanzapp.movements.repository;

import com.finanzapp.movements.repository.entities.ForecastEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ForecastEventRepository extends JpaRepository<ForecastEventEntity, Long> {

    List<ForecastEventEntity> findByUserEmailOrderByEventMonthAscIdAsc(String userEmail);

    Optional<ForecastEventEntity> findByIdAndUserEmail(Long id, String userEmail);

    void deleteByIdAndUserEmail(Long id, String userEmail);
}
