package com.finanzapp.movements.service;

import com.finanzapp.movements.service.domain.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface MovementService {

    /**
     * Get distinct years from movements for a given user email.
     * @param userEmail the user's email
     * @return list of distinct years
     */
    List<Integer> getYearsFromMovements(String userEmail);

    /**
     * Get movements by user email and date range, ordered by date.
     * @param userEmail the user's email
     * @param from the start date
     * @param to the end date
     * @return list of movements
     */
    List<Movement> getMovementsByUserEmailAndDateBetweenOrderByDate(String userEmail, LocalDate from, LocalDate to);

    /**
     * Get movements by user email, date range, and tag ID, ordered by date.
     * @param userEmail the user's email
     * @param from the start date
     * @param to the end date
     * @param tagId the tag ID to filter by
     * @return list of movements
     */
    List<Movement> getMovementsByUserEmailAndDateBetweenOrderByDateFilterByTagId(String userEmail, LocalDate from, LocalDate to, Long tagId);

    /**
     * Get annual movement dashboard data for a given user email, movement type, and date range.
     * @param userEmail the user's email
     * @param metric metric to calculate
     * @param from the start date
     * @param to the end date
     * @return annual movement response
     */
    AnualMovementResponse getAnualMovementDashboard(
            String userEmail,
            String metric,
            LocalDate from,
            LocalDate to,
            String tokenValue
    );

    /**
     * Get annual movement dashboard data filtered by tag ID for a given user email, movement type, and date range.
     * @param userEmail the user's email
     * @param metric metric to calculate
     * @param from the start date
     * @param to the end date
     * @param tagId the tag ID to filter by
     * @return annual movement response
     */
    AnualMovementResponse getAnualMovementDashboardFilterByTagId(
            String userEmail,
            String metric,
            LocalDate from,
            LocalDate to,
            String tokenValue,
            Long tagId
    );

    /**
     * Get historical movements for a given user email and metric.
     * @param userEmail the user's email
     * @param metric   metric to calculate
     * @param tokenValue   the token value
     * @return historical movement response
     */
    HistoricalMovementResponse getHistoricalMovements(
            String userEmail,
            String metric,
            String tokenValue
    );

    /**
     * Get historical movements for a given user email and metric filtered by tag ID.
     * @param userEmail the user's email
     * @param metric   metric to calculate
     * @param tokenValue   the token value
     * @param tagId the tag ID to filter by
     * @return historical movement response
     */
    HistoricalMovementResponse getHistoricalMovementsFilterByTagId(
            String userEmail,
            String metric,
            String tokenValue,
            Long tagId
    );

    /**
     * Create a new movement.
     * @param movement the movement to create
     * @param userEmail the user email
     */
    Movement createMovement(CreateMovementRequest movement, String userEmail, String bearerToken);


    /**
     * Update an existing movement.
     * @param movementId the ID of the movement to update
     * @param movement the updated movement data
     * @param userEmail the user email
     * @return the updated movement
     */
    Movement updateMovement(Long movementId, UpdateMovementRequest movement, String userEmail, String bearerToken);

    /**
     * Delete a movement by ID.
     * @param movementId the ID of the movement to delete
     * @param userEmail the user emaiL
     * @return the deleted movement
     */
    Movement getMovementById(Long movementId, String userEmail);

    /**
     * Delete a movement by ID.
     * @param movementId the ID of the movement to delete
     * @param userEmail the user email
     */
    void deleteMovementById(Long movementId, String userEmail);

    /**
     * Update movements when a tag is deleted.
     * @param userEmail the user email
     * @param tagId the ID of the deleted tag
     */
    void updateMovementsWhenTagIsDeleted(String userEmail, Long tagId);

    /**
     * Get the actual total savings for a user and token.
     * @param userEmail the user email
     * @param tokenValue the token value
     * @return the actual total savings
     */
    BigDecimal getActualTotalSavings(String userEmail, String tokenValue);

    /**
     * Get the net worth for a user and token.
     * Formula: initial wealth + benefits - expenses.
     */
    BigDecimal getNetWorth(String userEmail, String tokenValue);
}
