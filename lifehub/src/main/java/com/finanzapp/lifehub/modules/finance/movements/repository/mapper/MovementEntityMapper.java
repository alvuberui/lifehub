package com.finanzapp.movements.repository.mapper;

import com.finanzapp.movements.repository.entities.MovementEntity;
import com.finanzapp.movements.service.domain.*;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface MovementEntityMapper {

    Movement toDomain(MovementEntity movementEntity);

    List<Movement> toDomainList(List<MovementEntity> movementEntities);

    @Mapping(target = "userEmail", source = "userEmail")
    MovementEntity toEntity(CreateMovementRequest movement, String userEmail);

    MovementEntity toEntity(Movement movement, String userEmail);

    /**
     * Construye un AnualMovementResponse agregando por mes.
     * - movementType: el tipo del primer movimiento (se asume que la lista ya viene filtrada por tipo)
     * - year: el año del primer movimiento (se asume que la lista ya viene filtrada por año)
     * - series: 12 meses con realAmount agregado; idealAmount por defecto 0.0
     */
    default AnualMovementResponse toAnualMovementResponse(
            List<Movement> results,
            Locale locale,
            BigDecimal idealPercent,
            List<Movement> benefitResults
    ) {
        if (results == null || results.isEmpty()) {
            return AnualMovementResponse.builder()
                    .metric(null)
                    .year(null)
                    .series(Collections.emptyList())
                    .build();
        }

        // Metadata (se asume results filtrado por año y tipo)
        Movement first = results.getFirst();
        String movementType = first.getMovementType().toString();
        int year = first.getDate().getYear();

        BigDecimal percent = (idealPercent == null ? BigDecimal.ZERO : idealPercent)
                .divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP);

        // Real por mes (sum amount)
        Map<Integer, BigDecimal> realByMonth = results.stream()
                .collect(Collectors.groupingBy(
                        m -> m.getDate().getMonthValue(),
                        Collectors.reducing(BigDecimal.ZERO, m -> Optional.ofNullable(m.getAmount()).orElse(BigDecimal.ZERO), BigDecimal::add)
                ));

        // Beneficios por mes (sum amount) — solo del mismo año por seguridad
        Map<Integer, BigDecimal> benefitByMonth = (benefitResults == null ? List.<Movement>of() : benefitResults).stream()
                .filter(m -> m.getDate() != null && m.getDate().getYear() == year)
                .collect(Collectors.groupingBy(
                        m -> m.getDate().getMonthValue(),
                        Collectors.reducing(BigDecimal.ZERO, m -> Optional.ofNullable(m.getAmount()).orElse(BigDecimal.ZERO), BigDecimal::add)
                ));

        // Serie mensual (12 meses)
        List<MonthDashboard> series = new ArrayList<>(12);
        for (int month = 1; month <= 12; month++) {
            Month mo = Month.of(month);
            String monthName = mo.getDisplayName(TextStyle.FULL, locale);

            BigDecimal benefitSum = benefitByMonth.getOrDefault(month, BigDecimal.ZERO);
            BigDecimal idealAmount = benefitSum.multiply(percent);

            series.add(MonthDashboard.builder()
                    .month(month)
                    .monthName(monthName)
                    .idealAmount(idealAmount)
                    .realAmount(realByMonth.getOrDefault(month, BigDecimal.ZERO))
                    .build());
        }

        return AnualMovementResponse.builder()
                .metric(movementType)
                .year(year)
                .series(series)
                .build();
    }

    default HistoricalMovementResponse toHistoricalMovementResponse(
            List<Movement> results,
            Locale locale,
            BigDecimal idealPercent,
            List<Movement> benefitResults
    ) {
        if (results == null || results.isEmpty()) {
            return HistoricalMovementResponse.builder()
                    .metric(null)
                    .series(Collections.emptyList())
                    .build();
        }

        // La "métrica" la sacamos del tipo del primer movimiento (se asume lista filtrada por métrica/tipo)
        String metric = results.getFirst().getMovementType().toString();

        BigDecimal percent = (idealPercent == null ? BigDecimal.ZERO : idealPercent)
                .divide(BigDecimal.valueOf(100), 10, java.math.RoundingMode.HALF_UP);

        // Real por año
        Map<Integer, BigDecimal> realByYear = results.stream()
                .filter(m -> m.getDate() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getDate().getYear(),
                        Collectors.reducing(BigDecimal.ZERO, m -> Optional.ofNullable(m.getAmount()).orElse(BigDecimal.ZERO), BigDecimal::add)
                ));

        // Beneficios por año (para calcular ideal por año)
        Map<Integer, BigDecimal> benefitByYear = (benefitResults == null ? List.<Movement>of() : benefitResults).stream()
                .filter(m -> m.getDate() != null)
                .collect(Collectors.groupingBy(
                        m -> m.getDate().getYear(),
                        Collectors.reducing(BigDecimal.ZERO, m -> Optional.ofNullable(m.getAmount()).orElse(BigDecimal.ZERO), BigDecimal::add)
                ));

        // Unión de años (para cubrir años donde haya benefits pero no movements del tipo, o viceversa)
        Set<Integer> years = new HashSet<>();
        years.addAll(realByYear.keySet());
        years.addAll(benefitByYear.keySet());

        List<Integer> sortedYears = years.stream().sorted().toList();

        List<HistoricalDashboard> series = new ArrayList<>(sortedYears.size());
        for (Integer year : sortedYears) {
            BigDecimal benefitSum = benefitByYear.getOrDefault(year, BigDecimal.ZERO);
            BigDecimal idealAmount = benefitSum.multiply(percent);
            BigDecimal realAmount = realByYear.getOrDefault(year, BigDecimal.ZERO);

            series.add(HistoricalDashboard.builder()
                    .year(year)
                    .idealAmount(idealAmount)
                    .realAmount(realAmount)
                    .build());
        }

        return HistoricalMovementResponse.builder()
                .metric(metric)
                .series(series)
                .build();
    }


    default HistoricalMovementResponse toHistoricalMovementResponse(List<Movement> results, BigDecimal idealPercent,
                                                                    List<Movement> benefitResults) {
        return toHistoricalMovementResponse(results, new Locale("es", "ES"), idealPercent, benefitResults);
    }


    default AnualMovementResponse toAnualMovementResponse(List<Movement> results, BigDecimal idealPercent,
                                                          List<Movement> benefitResults) {
        return toAnualMovementResponse(results, new Locale("es", "ES"), idealPercent, benefitResults);
    }
}
