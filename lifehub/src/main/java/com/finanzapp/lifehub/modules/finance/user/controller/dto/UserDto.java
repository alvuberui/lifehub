package com.finanzapp.user.controller.dto;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {

    private Boolean hasCompletedOnboarding;

    private String email;

    private String name;

    private String lastname;

    private String birthdate;

    private BigDecimal initialWealth;

    private BigDecimal monthlySavingPercentage;

    private BigDecimal monthlyNecessaryExpensesPercentage;

    private BigDecimal monthlyDiscretionaryExpensesPercentage;

    private BigDecimal monthlyInvestmentPercentage;
}

