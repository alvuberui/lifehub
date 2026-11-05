package com.finanzapp.movements.service.domain;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthDashboard {

    Integer month;

    String monthName;

    BigDecimal idealAmount;

    BigDecimal realAmount;
}
