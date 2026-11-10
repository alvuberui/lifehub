package com.finanzapp.movements.controller.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ForecastEventRequest(
        @NotBlank(message = "El nombre del evento es obligatorio")
        @Size(max = 255, message = "El nombre no puede superar los 255 caracteres")
        String name,

        @NotBlank(message = "El mes del evento es obligatorio")
        @Pattern(regexp = "^\\d{4}-(0[1-9]|1[0-2])$", message = "El mes debe tener formato YYYY-MM")
        String month,

        @NotNull(message = "El importe es obligatorio")
        @Positive(message = "El importe debe ser mayor que 0")
        @Digits(integer = 15, fraction = 2, message = "El importe debe tener como máximo 15 dígitos y 2 decimales")
        BigDecimal amount,

        @NotBlank(message = "El tipo del evento es obligatorio")
        @Pattern(regexp = "(?i)^(expense|income)$", message = "El tipo debe ser expense o income")
        String type
) {
}
