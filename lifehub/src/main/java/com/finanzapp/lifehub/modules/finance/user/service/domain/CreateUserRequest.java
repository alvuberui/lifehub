package com.finanzapp.user.service.domain;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateUserRequest {

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
