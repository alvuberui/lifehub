package com.finanzapp.movements.controller.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoricalDashboardDto {

    Integer year;

    BigDecimal idealAmount;

    BigDecimal realAmount;
}
