package com.finanzapp.lifehub.modules.finance.movements;

import com.finanzapp.movements.controller.dto.CreateMovementDto;
import com.finanzapp.movements.controller.dto.UpdateMovementDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MovementValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void createRequiresDescription() {
        CreateMovementDto dto = CreateMovementDto.builder()
                .amount(new BigDecimal("10.00"))
                .date(java.time.LocalDate.now())
                .build();

        assertThat(validator.validate(dto))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("description");
    }

    @Test
    void amountsAllowAtMostTwoDecimals() {
        UpdateMovementDto dto = UpdateMovementDto.builder()
                .amount(new BigDecimal("10.001"))
                .build();

        assertThat(validator.validate(dto))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("amount");
    }
}

