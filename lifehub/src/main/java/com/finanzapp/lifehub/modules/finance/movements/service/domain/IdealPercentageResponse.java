package com.finanzapp.movements.service.domain;

import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IdealPercentageResponse {

    private BigDecimal monthlySavingPercentage;

    private BigDecimal monthlyNecessaryExpensesPercentage;

    private BigDecimal monthlyDiscretionaryExpensesPercentage;

    private BigDecimal monthlyInvestmentPercentage;
}
