package com.finanzapp.user.controller.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequestDto {

    @NotBlank(message = "Obligatorio")
    @Email(message = "Email inválido")
    private String email;

    @NotBlank(message = "Obligatorio")
    @Size(min = 2, message = "El nombre es muy corto")
    private String name;

    @NotBlank(message = "Obligatorio")
    @Size(min = 2, message = "El apellido es muy corto")
    private String lastname;

    @NotNull(message = "Obligatorio")
    @Past(message = "La fecha debe ser anterior a hoy")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate birthdate;

    @NotNull(message = "Obligatorio")
    @DecimalMin(value = "0.0", message = "El patrimonio inicial no puede ser negativo")
    @Digits(integer = 15, fraction = 2, message = "El patrimonio inicial debe tener como máximo 15 dígitos y 2 decimales")
    private BigDecimal initialWealth;

    @NotNull(message = "Obligatorio")
    @DecimalMin(value = "0.0", message = "Debe estar entre 0 y 100")
    @DecimalMax(value = "100.0", message = "El porcentaje debe estar entre 0 y 100")
    @Digits(integer = 3, fraction = 2, message = "El porcentaje debe tener como máximo 2 decimales")
    private BigDecimal monthlySavingPercentage;

    @NotNull(message = "Obligatorio")
    @DecimalMin(value = "0.0", message = "Debe estar entre 0 y 100")
    @DecimalMax(value = "100.0", message = "El porcentaje debe estar entre 0 y 100")
    @Digits(integer = 3, fraction = 2, message = "El porcentaje debe tener como máximo 2 decimales")
    private BigDecimal monthlyNecessaryExpensesPercentage;

    @NotNull(message = "Obligatorio")
    @DecimalMin(value = "0.0", message = "Debe estar entre 0 y 100")
    @DecimalMax(value = "100.0", message = "El porcentaje debe estar entre 0 y 100")
    @Digits(integer = 3, fraction = 2, message = "El porcentaje debe tener como máximo 2 decimales")
    private BigDecimal monthlyDiscretionaryExpensesPercentage;

    @NotNull(message = "Obligatorio")
    @DecimalMin(value = "0.0", message = "Debe estar entre 0 y 100")
    @DecimalMax(value = "100.0", message = "El porcentaje debe estar entre 0 y 100")
    @Digits(integer = 3, fraction = 2, message = "El porcentaje debe tener como máximo 2 decimales")
    private BigDecimal monthlyInvestmentPercentage;

    @AssertTrue(message = "La suma de porcentajes debe ser 100%")
    public boolean isPercentagesSum100() {
        if (monthlySavingPercentage == null ||
                monthlyNecessaryExpensesPercentage == null ||
                monthlyDiscretionaryExpensesPercentage == null ||
                monthlyInvestmentPercentage == null) {
            return false;
        }

        BigDecimal sum = monthlySavingPercentage
                .add(monthlyNecessaryExpensesPercentage)
                .add(monthlyDiscretionaryExpensesPercentage)
                .add(monthlyInvestmentPercentage);

        // equivalente a Math.round(sum*100)/100 en JS
        BigDecimal rounded = sum.setScale(2, RoundingMode.HALF_UP);

        return rounded.compareTo(new BigDecimal("100.00")) == 0;
    }
}
