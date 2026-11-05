package com.finanzapp.movements.service.domain;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoricalDashboard {

    Integer year;

    BigDecimal idealAmount;

    BigDecimal realAmount;
}
