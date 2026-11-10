package com.finanzapp.movements.controller.dto;

import java.math.BigDecimal;

public record ForecastEventResponse(
        Long id,
        String name,
        String month,
        BigDecimal amount,
        String type
) {
}
