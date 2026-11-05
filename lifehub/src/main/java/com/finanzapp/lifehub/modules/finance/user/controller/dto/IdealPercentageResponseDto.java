package com.finanzapp.user.controller.dto;

import jakarta.persistence.Column;
import lombok.*;

import java.math.BigDecimal;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class IdealPercentageResponseDto {

    private BigDecimal monthlySavingPercentage;

    private BigDecimal monthlyNecessaryExpensesPercentage;

    private BigDecimal monthlyDiscretionaryExpensesPercentage;

    private BigDecimal monthlyInvestmentPercentage;
}
