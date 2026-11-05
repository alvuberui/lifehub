package com.finanzapp.lifehub.modules.finance.user;

import com.finanzapp.user.controller.dto.CreateUserRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class UserValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void percentagesMustHaveAtMostTwoDecimals() {
        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("user@example.com")
                .name("Alba")
                .lastname("Usuario")
                .birthdate(LocalDate.of(1990, 1, 1))
                .initialWealth(new BigDecimal("100.00"))
                .monthlySavingPercentage(new BigDecimal("20.001"))
                .monthlyNecessaryExpensesPercentage(new BigDecimal("50.00"))
                .monthlyDiscretionaryExpensesPercentage(new BigDecimal("20.00"))
                .monthlyInvestmentPercentage(new BigDecimal("9.999"))
                .build();

        assertThat(validator.validate(dto))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("monthlySavingPercentage", "monthlyInvestmentPercentage");
    }

    @Test
    void percentagesMustSumToOneHundred() {
        CreateUserRequestDto dto = CreateUserRequestDto.builder()
                .email("user@example.com")
                .name("Alba")
                .lastname("Usuario")
                .birthdate(LocalDate.of(1990, 1, 1))
                .initialWealth(BigDecimal.ZERO)
                .monthlySavingPercentage(new BigDecimal("20.00"))
                .monthlyNecessaryExpensesPercentage(new BigDecimal("50.00"))
                .monthlyDiscretionaryExpensesPercentage(new BigDecimal("20.00"))
                .monthlyInvestmentPercentage(new BigDecimal("5.00"))
                .build();

        assertThat(validator.validate(dto))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("percentagesSum100");
    }
}

