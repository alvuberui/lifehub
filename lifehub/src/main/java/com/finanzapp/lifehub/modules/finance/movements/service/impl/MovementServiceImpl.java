package com.finanzapp.movements.service.impl;

import com.finanzapp.movements.repository.MovementRepository;
import com.finanzapp.movements.repository.entities.MovementEntity;
import com.finanzapp.movements.repository.entities.MovementType;
import com.finanzapp.movements.repository.mapper.MovementEntityMapper;
import com.finanzapp.movements.service.MovementService;
import com.finanzapp.movements.service.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {

    private final MovementRepository movementRepository;
    private final MovementEntityMapper movementMapper;
    private final UsersClientImpl usersClient;
    private final TagsClientImpl tagsClient;

    @Override
    public List<Integer> getYearsFromMovements(String userEmail) {
        return movementRepository.findDistinctYearsByUserEmail(userEmail);
    }

    @Override
    public List<Movement> getMovementsByUserEmailAndDateBetweenOrderByDate(String userEmail, LocalDate from, LocalDate to) {
        return movementMapper.toDomainList(
                movementRepository.findByUserEmailAndDateBetweenOrderByDate(userEmail, from, to)
        );
    }

    @Override
    public List<Movement> getMovementsByUserEmailAndDateBetweenOrderByDateFilterByTagId(String userEmail, LocalDate from, LocalDate to, Long tagId) {
        return movementMapper.toDomainList(
                movementRepository.findByUserEmailAndTagIdAndDateBetweenOrderByDate(userEmail, tagId, from, to)
        );
    }

    @Override
    public AnualMovementResponse getAnualMovementDashboard(
            String userEmail,
            String metric,
            LocalDate from,
            LocalDate to,
            String tokenValue
    ) {
        validateSameYear(from, to);

        Metric metricEnum = parseMetric(metric);

        // Siempre cargamos BENEFIT porque:
        // - lo necesitas como base para calcular IDEAL (ideal = benefits * %)
        // - lo necesitas para SAVINGS (benefit - expenses - investment)
        List<Movement> benefit = loadMovements(userEmail, MovementType.BENEFIT, from, to, false, null);

        IdealPercentageResponse idealPercentResponse = usersClient.getIdealPercent(tokenValue);
        BigDecimal idealPercent = getIdealPercent(idealPercentResponse, metricEnum);

        return switch (metricEnum) {
            case BENEFIT -> movementMapper.toAnualMovementResponse(benefit, idealPercent, benefit);

            case INVESTMENTS -> {
                List<Movement> investment = loadMovements(userEmail, MovementType.INVESTMENT, from, to, false, null);
                yield movementMapper.toAnualMovementResponse(investment, idealPercent, benefit);
            }

            case DISCRETIONARY_EXPENSES -> {
                List<Movement> notEssential = loadMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, from, to, false, null);
                yield movementMapper.toAnualMovementResponse(notEssential, idealPercent, benefit);
            }

            case NECESSARY_EXPENSES -> {
                List<Movement> essential = loadMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, from, to, false, null);
                yield movementMapper.toAnualMovementResponse(essential, idealPercent, benefit);
            }

            case TOTAL_EXPENSES -> {
                List<Movement> essential = loadMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, from, to, false, null);
                List<Movement> notEssential = loadMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, from, to, false, null);

                Map<Integer, BigDecimal> totalExpensesByMonth = addMaps(sumByMonth(essential), sumByMonth(notEssential));

                List<Movement> synthetic = buildMonthlySyntheticMovements(
                        userEmail,
                        com.finanzapp.movements.service.domain.MovementType.ESSENTIAL_EXPENSE, // placeholder
                        from.getYear(),
                        totalExpensesByMonth,
                        "SYNTHETIC_TOTAL_EXPENSES"
                );

                yield movementMapper.toAnualMovementResponse(synthetic, idealPercent, benefit);
            }

            case SAVINGS -> {
                List<Movement> essential = loadMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, from, to, false, null);
                List<Movement> notEssential = loadMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, from, to, false, null);
                List<Movement> investment = loadMovements(userEmail, MovementType.INVESTMENT, from, to, false, null);

                Map<Integer, BigDecimal> expensesByMonth = addMaps(sumByMonth(essential), sumByMonth(notEssential));
                Map<Integer, BigDecimal> savingsByMonth = subtractMaps(
                        subtractMaps(sumByMonth(benefit), expensesByMonth),
                        sumByMonth(investment)
                );

                List<Movement> synthetic = buildMonthlySyntheticMovements(
                        userEmail,
                        com.finanzapp.movements.service.domain.MovementType.BENEFIT, // placeholder
                        from.getYear(),
                        savingsByMonth,
                        "SYNTHETIC_SAVINGS"
                );

                yield movementMapper.toAnualMovementResponse(synthetic, idealPercent, benefit);
            }
        };
    }

    @Override
    public AnualMovementResponse getAnualMovementDashboardFilterByTagId(String userEmail, String metric, LocalDate from, LocalDate to, String tokenValue, Long tagId) {
        validateSameYear(from, to);

        Metric metricEnum = parseMetric(metric);

        // Siempre cargamos BENEFIT porque:
        // - lo necesitas como base para calcular IDEAL (ideal = benefits * %)
        // - lo necesitas para SAVINGS (benefit - expenses - investment)
        List<Movement> benefit = loadMovements(userEmail, MovementType.BENEFIT, from, to, true, tagId);

        IdealPercentageResponse idealPercentResponse = usersClient.getIdealPercent(tokenValue);
        BigDecimal idealPercent = getIdealPercent(idealPercentResponse, metricEnum);

        return switch (metricEnum) {
            case BENEFIT -> movementMapper.toAnualMovementResponse(benefit, idealPercent, benefit);

            case INVESTMENTS -> {
                List<Movement> investment = loadMovements(userEmail, MovementType.INVESTMENT, from, to, true, tagId);
                yield movementMapper.toAnualMovementResponse(investment, idealPercent, benefit);
            }

            case DISCRETIONARY_EXPENSES -> {
                List<Movement> notEssential = loadMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, from, to, true, tagId);
                yield movementMapper.toAnualMovementResponse(notEssential, idealPercent, benefit);
            }

            case NECESSARY_EXPENSES -> {
                List<Movement> essential = loadMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, from, to, true, tagId);
                yield movementMapper.toAnualMovementResponse(essential, idealPercent, benefit);
            }

            case TOTAL_EXPENSES -> {
                List<Movement> essential = loadMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, from, to, true, tagId);
                List<Movement> notEssential = loadMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, from, to, true, tagId);

                Map<Integer, BigDecimal> totalExpensesByMonth = addMaps(sumByMonth(essential), sumByMonth(notEssential));

                List<Movement> synthetic = buildMonthlySyntheticMovements(
                        userEmail,
                        com.finanzapp.movements.service.domain.MovementType.ESSENTIAL_EXPENSE, // placeholder
                        from.getYear(),
                        totalExpensesByMonth,
                        "SYNTHETIC_TOTAL_EXPENSES"
                );

                yield movementMapper.toAnualMovementResponse(synthetic, idealPercent, benefit);
            }

            case SAVINGS -> {
                List<Movement> essential = loadMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, from, to, true, tagId);
                List<Movement> notEssential = loadMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, from, to, true, tagId);
                List<Movement> investment = loadMovements(userEmail, MovementType.INVESTMENT, from, to, true, tagId);

                Map<Integer, BigDecimal> expensesByMonth = addMaps(sumByMonth(essential), sumByMonth(notEssential));
                Map<Integer, BigDecimal> savingsByMonth = subtractMaps(
                        subtractMaps(sumByMonth(benefit), expensesByMonth),
                        sumByMonth(investment)
                );

                List<Movement> synthetic = buildMonthlySyntheticMovements(
                        userEmail,
                        com.finanzapp.movements.service.domain.MovementType.BENEFIT, // placeholder
                        from.getYear(),
                        savingsByMonth,
                        "SYNTHETIC_SAVINGS"
                );

                yield movementMapper.toAnualMovementResponse(synthetic, idealPercent, benefit);
            }
        };
    }

    @Override
    public HistoricalMovementResponse getHistoricalMovements(String userEmail, String metric, String tokenValue) {
        Metric metricEnum = parseMetric(metric);

        // Siempre cargamos BENEFIT porque:
        // - IDEAL se calcula como (beneficios del año * porcentaje)
        // - SAVINGS se calcula con benefits - expenses - investments
        List<Movement> benefit = loadHistoricalMovements(userEmail, MovementType.BENEFIT, false, null);

        IdealPercentageResponse idealPercentResponse = usersClient.getIdealPercent(tokenValue);
        BigDecimal idealPercent = getIdealPercent(idealPercentResponse, metricEnum);

        return switch (metricEnum) {
            case BENEFIT -> movementMapper.toHistoricalMovementResponse(benefit, idealPercent, benefit);

            case INVESTMENTS -> {
                List<Movement> investment = loadHistoricalMovements(userEmail, MovementType.INVESTMENT, false, null);
                yield movementMapper.toHistoricalMovementResponse(investment, idealPercent, benefit);
            }

            case DISCRETIONARY_EXPENSES -> {
                List<Movement> notEssential = loadHistoricalMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, false, null);
                yield movementMapper.toHistoricalMovementResponse(notEssential, idealPercent, benefit);
            }

            case NECESSARY_EXPENSES -> {
                List<Movement> essential = loadHistoricalMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, false, null);
                yield movementMapper.toHistoricalMovementResponse(essential, idealPercent, benefit);
            }

            case TOTAL_EXPENSES -> {
                List<Movement> essential = loadHistoricalMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, false, null);
                List<Movement> notEssential = loadHistoricalMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, false, null);

                Map<Integer, BigDecimal> totalExpensesByYear = addYearMaps(sumByYear(essential), sumByYear(notEssential));

                List<Movement> synthetic = buildYearlySyntheticMovements(
                        userEmail,
                        com.finanzapp.movements.service.domain.MovementType.ESSENTIAL_EXPENSE, // placeholder
                        totalExpensesByYear,
                        "SYNTHETIC_TOTAL_EXPENSES"
                );

                yield movementMapper.toHistoricalMovementResponse(synthetic, idealPercent, benefit);
            }

            case SAVINGS -> {
                List<Movement> essential = loadHistoricalMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, false, null);
                List<Movement> notEssential = loadHistoricalMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, false, null);
                List<Movement> investment = loadHistoricalMovements(userEmail, MovementType.INVESTMENT, false, null);

                Map<Integer, BigDecimal> expensesByYear = addYearMaps(sumByYear(essential), sumByYear(notEssential));

                Map<Integer, BigDecimal> savingsByYear = subtractYearMaps(
                        subtractYearMaps(sumByYear(benefit), expensesByYear),
                        sumByYear(investment)
                );

                List<Movement> synthetic = buildYearlySyntheticMovements(
                        userEmail,
                        com.finanzapp.movements.service.domain.MovementType.BENEFIT, // placeholder
                        savingsByYear,
                        "SYNTHETIC_SAVINGS"
                );

                yield movementMapper.toHistoricalMovementResponse(synthetic, idealPercent, benefit);
            }
        };
    }

    @Override
    public HistoricalMovementResponse getHistoricalMovementsFilterByTagId(String userEmail, String metric, String tokenValue, Long tagId) {
        Metric metricEnum = parseMetric(metric);

        // Siempre cargamos BENEFIT porque:
        // - IDEAL se calcula como (beneficios del año * porcentaje)
        // - SAVINGS se calcula con benefits - expenses - investments
        List<Movement> benefit = loadHistoricalMovements(userEmail, MovementType.BENEFIT, true, tagId);

        IdealPercentageResponse idealPercentResponse = usersClient.getIdealPercent(tokenValue);
        BigDecimal idealPercent = getIdealPercent(idealPercentResponse, metricEnum);

        return switch (metricEnum) {
            case BENEFIT -> movementMapper.toHistoricalMovementResponse(benefit, idealPercent, benefit);

            case INVESTMENTS -> {
                List<Movement> investment = loadHistoricalMovements(userEmail, MovementType.INVESTMENT, true, tagId);
                yield movementMapper.toHistoricalMovementResponse(investment, idealPercent, benefit);
            }

            case DISCRETIONARY_EXPENSES -> {
                List<Movement> notEssential = loadHistoricalMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, true, tagId);
                yield movementMapper.toHistoricalMovementResponse(notEssential, idealPercent, benefit);
            }

            case NECESSARY_EXPENSES -> {
                List<Movement> essential = loadHistoricalMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, true, tagId);
                yield movementMapper.toHistoricalMovementResponse(essential, idealPercent, benefit);
            }

            case TOTAL_EXPENSES -> {
                List<Movement> essential = loadHistoricalMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, true, tagId);
                List<Movement> notEssential = loadHistoricalMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, true, tagId);

                Map<Integer, BigDecimal> totalExpensesByYear = addYearMaps(sumByYear(essential), sumByYear(notEssential));

                List<Movement> synthetic = buildYearlySyntheticMovements(
                        userEmail,
                        com.finanzapp.movements.service.domain.MovementType.ESSENTIAL_EXPENSE, // placeholder
                        totalExpensesByYear,
                        "SYNTHETIC_TOTAL_EXPENSES"
                );

                yield movementMapper.toHistoricalMovementResponse(synthetic, idealPercent, benefit);
            }

            case SAVINGS -> {
                List<Movement> essential = loadHistoricalMovements(userEmail, MovementType.ESSENTIAL_EXPENSE, true, tagId);
                List<Movement> notEssential = loadHistoricalMovements(userEmail, MovementType.DISCRETIONARY_EXPENSE, true, tagId);
                List<Movement> investment = loadHistoricalMovements(userEmail, MovementType.INVESTMENT, true, tagId);

                Map<Integer, BigDecimal> expensesByYear = addYearMaps(sumByYear(essential), sumByYear(notEssential));

                Map<Integer, BigDecimal> savingsByYear = subtractYearMaps(
                        subtractYearMaps(sumByYear(benefit), expensesByYear),
                        sumByYear(investment)
                );

                List<Movement> synthetic = buildYearlySyntheticMovements(
                        userEmail,
                        com.finanzapp.movements.service.domain.MovementType.BENEFIT, // placeholder
                        savingsByYear,
                        "SYNTHETIC_SAVINGS"
                );

                yield movementMapper.toHistoricalMovementResponse(synthetic, idealPercent, benefit);
            }
        };
    }

    @Override
    public Movement createMovement(CreateMovementRequest movement, String userEmail, String bearerToken) {
        if (movement.getAmount() != null && movement.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("La cantidad debe de ser mayor que 0");
        }
        if(movement.getTagId() != null) {
            if (!Boolean.TRUE.equals(tagsClient.existsByIdAndUserEmail(movement.getTagId(), bearerToken))) {
                throw new IllegalArgumentException("La etiqueta indicada no existe");
            }
        }
        return this.movementMapper.toDomain( this.movementRepository.save(this.movementMapper.toEntity(movement, userEmail)));
    }

    @Override
    public Movement updateMovement(Long movementId, UpdateMovementRequest movement, String userEmail, String bearerToken) {
        MovementEntity existingMovement = this.movementRepository.findByIdAndUserEmail(movementId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado"));

        if (movement.getAmount() != null && movement.getAmount().signum() <= 0) {
            throw new IllegalArgumentException("La cantidad debe de ser mayor que 0");
        }

        if(movement.getTagId() != null) {
            if (!Boolean.TRUE.equals(tagsClient.existsByIdAndUserEmail(movement.getTagId(), bearerToken))) {
                throw new IllegalArgumentException("La etiqueta indicada no existe");
            }
        }

        if (movement.getAmount() != null) {
            existingMovement.setAmount(movement.getAmount());
        }
        if (movement.getDate() != null) {
            existingMovement.setDate(movement.getDate());
        }
        if (movement.isTagIdSpecified()) {
            existingMovement.setTagId(movement.getTagId());
        }
        if (movement.isDescriptionSpecified()) {
            existingMovement.setDescription(movement.getDescription());
        }
        if (movement.getMovementType() != null) {
            existingMovement.setMovementType( MovementType.valueOf( movement.getMovementType().name() ) );
        }

        MovementEntity updatedMovement = this.movementRepository.save(existingMovement);
        return this.movementMapper.toDomain(updatedMovement);
    }

    @Override
    public Movement getMovementById(Long movementId, String userEmail) {
        MovementEntity movementEntity = this.movementRepository.findByIdAndUserEmail(movementId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado"));
        return this.movementMapper.toDomain(movementEntity);
    }

    @Override
    public void deleteMovementById(Long movementId, String userEmail) {
        MovementEntity movementEntity = this.movementRepository.findByIdAndUserEmail(movementId, userEmail)
                .orElseThrow(() -> new IllegalArgumentException("Movimiento no encontrado"));
        this.movementRepository.delete(movementEntity);
    }

    @Override
    @Transactional(transactionManager = "movementsTransactionManager")
    public void updateMovementsWhenTagIsDeleted(String userEmail, Long tagId) {
        List<Movement> movements = movementMapper.toDomainList(
                movementRepository.findByUserEmailAndTagId(userEmail, tagId)
        );
        for (Movement movement : movements) {
            movement.setTagId(null);
            MovementEntity entityToUpdate = movementMapper.toEntity(movement, userEmail);
            movementRepository.save(entityToUpdate);
        }
    }

    @Override
    public BigDecimal getActualTotalSavings(String userEmail, String tokenValue) {
        User user = usersClient.getUser(tokenValue);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }
        List<Movement> allMovements = movementMapper.toDomainList(
                movementRepository.findByUserEmail(userEmail)
        );
        BigDecimal totalBenefits = allMovements.stream()
                .filter(m -> m.getMovementType() == com.finanzapp.movements.service.domain.MovementType.BENEFIT)
                .map(m -> m.getAmount() == null ? BigDecimal.ZERO : m.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalExpenses = allMovements.stream()
                .filter(m -> m.getMovementType() == com.finanzapp.movements.service.domain.MovementType.ESSENTIAL_EXPENSE
                        || m.getMovementType() == com.finanzapp.movements.service.domain.MovementType.DISCRETIONARY_EXPENSE)
                .map(m -> m.getAmount() == null ? BigDecimal.ZERO : m.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalBenefits.subtract(totalExpenses);
    }

    @Override
    public BigDecimal getNetWorth(String userEmail, String tokenValue) {
        User user = usersClient.getUser(tokenValue);
        if (user == null) {
            throw new IllegalArgumentException("Usuario no encontrado");
        }

        BigDecimal initialWealth = user.getInitialWealth() == null
                ? BigDecimal.ZERO
                : user.getInitialWealth();
        List<Movement> allMovements = movementMapper.toDomainList(
                movementRepository.findByUserEmail(userEmail)
        );
        BigDecimal totalBenefits = sumByType(allMovements, com.finanzapp.movements.service.domain.MovementType.BENEFIT);
        BigDecimal totalExpenses = allMovements.stream()
                .filter(m -> m.getMovementType() == com.finanzapp.movements.service.domain.MovementType.ESSENTIAL_EXPENSE
                        || m.getMovementType() == com.finanzapp.movements.service.domain.MovementType.DISCRETIONARY_EXPENSE)
                .map(m -> m.getAmount() == null ? BigDecimal.ZERO : m.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return initialWealth
                .add(totalBenefits)
                .subtract(totalExpenses);
    }

    private BigDecimal sumByType(List<Movement> movements, com.finanzapp.movements.service.domain.MovementType type) {
        return movements.stream()
                .filter(m -> m.getMovementType() == type)
                .map(m -> m.getAmount() == null ? BigDecimal.ZERO : m.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    // -----------------------
    // Helpers
    // -----------------------

    private Map<Integer, BigDecimal> sumByYear(List<Movement> movements) {
        if (movements == null) return Map.of();

        return movements.stream()
                .filter(m -> m.getDate() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> m.getDate().getYear(),
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO,
                                m -> m.getAmount() == null ? BigDecimal.ZERO : m.getAmount(), BigDecimal::add)
                ));
    }

    private Map<Integer, BigDecimal> addYearMaps(Map<Integer, BigDecimal> a, Map<Integer, BigDecimal> b) {
        Map<Integer, BigDecimal> out = new HashMap<>();
        java.util.Set<Integer> years = new java.util.HashSet<>();
        years.addAll(a.keySet());
        years.addAll(b.keySet());

        for (Integer y : years) {
            out.put(y, a.getOrDefault(y, BigDecimal.ZERO).add(b.getOrDefault(y, BigDecimal.ZERO)));
        }
        return out;
    }

    private Map<Integer, BigDecimal> subtractYearMaps(Map<Integer, BigDecimal> base, Map<Integer, BigDecimal> minus) {
        Map<Integer, BigDecimal> out = new HashMap<>();
        java.util.Set<Integer> years = new java.util.HashSet<>();
        years.addAll(base.keySet());
        years.addAll(minus.keySet());

        for (Integer y : years) {
            out.put(y, base.getOrDefault(y, BigDecimal.ZERO).subtract(minus.getOrDefault(y, BigDecimal.ZERO)));
        }
        return out;
    }

    private List<Movement> buildYearlySyntheticMovements(
            String userEmail,
            com.finanzapp.movements.service.domain.MovementType type,
            Map<Integer, BigDecimal> amountByYear,
            String description
    ) {
        List<Movement> out = new java.util.ArrayList<>(amountByYear.size());

        // Ordenados por año ascendente
        List<Integer> years = amountByYear.keySet().stream().sorted().toList();

        for (Integer year : years) {
            BigDecimal amount = amountByYear.getOrDefault(year, BigDecimal.ZERO);

            out.add(Movement.builder()
                    .id(null)
                    .userEmail(userEmail)
                    .movementType(type)
                    .amount(amount)
                    .date(LocalDate.of(year, 1, 1)) // marcador del año
                    .tagId(null)
                    .description(description)
                    .build());
        }

        return out;
    }

    private void validateSameYear(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("from/to no pueden ser null");
        }
        if (from.getYear() != to.getYear()) {
            throw new IllegalArgumentException("from/to deben estar en el mismo año");
        }
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from no puede ser posterior a to");
        }
    }

    private Metric parseMetric(String metric) {
        if (metric == null || metric.isBlank()) {
            throw new IllegalArgumentException("metric es obligatorio");
        }
        try {
            return Metric.valueOf(metric.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("metric no válido");
        }
    }

    private List<Movement> loadMovements(String userEmail, MovementType type, LocalDate from, LocalDate to, Boolean filerByTag, Long tagId) {
        if (filerByTag) {
            return movementMapper.toDomainList(
                    movementRepository.findByUserEmailAndMovementTypeAndTagIdAndDateBetweenOrderByDateAsc(userEmail, type, tagId, from, to)
            );
        }
        return movementMapper.toDomainList(
                movementRepository.findByUserEmailAndMovementTypeAndDateBetweenOrderByDateAsc(userEmail, type, from, to)
        );
    }

    private List<Movement> loadHistoricalMovements(String userEmail, MovementType type, Boolean filerByTag, Long tagId) {
        if (filerByTag) {
            return movementMapper.toDomainList(
                    movementRepository.findByUserEmailAndMovementTypeAndTagIdOrderByDateAsc(userEmail, type, tagId)
            );
        }
        return movementMapper.toDomainList(
                movementRepository.findByUserEmailAndMovementTypeOrderByDateAsc(userEmail, type)
        );
    }

    private Map<Integer, BigDecimal> sumByMonth(List<Movement> movements) {
        if (movements == null) return Map.of();

        return movements.stream()
                .filter(m -> m.getDate() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        m -> m.getDate().getMonthValue(),
                        java.util.stream.Collectors.reducing(BigDecimal.ZERO,
                                m -> m.getAmount() == null ? BigDecimal.ZERO : m.getAmount(), BigDecimal::add)
                ));
    }

    private Map<Integer, BigDecimal> addMaps(Map<Integer, BigDecimal> a, Map<Integer, BigDecimal> b) {
        Map<Integer, BigDecimal> out = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            out.put(m, a.getOrDefault(m, BigDecimal.ZERO).add(b.getOrDefault(m, BigDecimal.ZERO)));
        }
        return out;
    }

    private Map<Integer, BigDecimal> subtractMaps(Map<Integer, BigDecimal> base, Map<Integer, BigDecimal> minus) {
        Map<Integer, BigDecimal> out = new HashMap<>();
        for (int m = 1; m <= 12; m++) {
            out.put(m, base.getOrDefault(m, BigDecimal.ZERO).subtract(minus.getOrDefault(m, BigDecimal.ZERO)));
        }
        return out;
    }

    private List<Movement> buildMonthlySyntheticMovements(
            String userEmail,
            com.finanzapp.movements.service.domain.MovementType type,
            int year,
            Map<Integer, BigDecimal> amountByMonth,
            String description
    ) {
        List<Movement> out = new java.util.ArrayList<>(12);

        for (int month = 1; month <= 12; month++) {
            BigDecimal amount = amountByMonth.getOrDefault(month, BigDecimal.ZERO);

            out.add(Movement.builder()
                    .id(null)
                    .userEmail(userEmail)
                    .movementType(type)
                    .amount(amount)
                    .date(LocalDate.of(year, month, 1))
                    .tagId(null)
                    .description(description)
                    .build());
        }
        return out;
    }

    private BigDecimal getIdealPercent(IdealPercentageResponse idealPercentageResponse, Metric metric) {
        if (idealPercentageResponse == null) {
            // fallback defensivo
            return metric == Metric.BENEFIT ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        }

        return switch (metric) {
            case BENEFIT -> BigDecimal.valueOf(100);
            case SAVINGS -> idealPercentageResponse.getMonthlySavingPercentage();
            case TOTAL_EXPENSES ->
                    idealPercentageResponse.getMonthlyNecessaryExpensesPercentage()
                            .add(idealPercentageResponse.getMonthlyDiscretionaryExpensesPercentage());
            case DISCRETIONARY_EXPENSES -> idealPercentageResponse.getMonthlyDiscretionaryExpensesPercentage();
            case NECESSARY_EXPENSES -> idealPercentageResponse.getMonthlyNecessaryExpensesPercentage();
            case INVESTMENTS -> idealPercentageResponse.getMonthlyInvestmentPercentage();
        };
    }
}
