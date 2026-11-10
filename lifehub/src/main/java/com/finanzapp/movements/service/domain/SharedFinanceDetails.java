package com.finanzapp.movements.service.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SharedFinanceDetails(
        List<String> members,
        List<String> editableMembers,
        List<String> adminMembers,
        List<SharedFinanceMovement> movements) {

    public record SharedFinanceMovement(
            Long id,
            String userEmail,
            String movementType,
            BigDecimal amount,
            LocalDate date,
            Long tagId,
            String tagName,
            String tagColor,
            String description,
            Boolean includeInForecast) {
    }
}
