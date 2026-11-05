package com.finanzapp.movements.controller.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthDashboardDto {

    Integer month;

    String monthName;

    BigDecimal idealAmount;

    BigDecimal realAmount;
}
