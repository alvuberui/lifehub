package com.finanzapp.movements.controller.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoricalMovementResponseDto {

    String metric;

    List<HistoricalDashboardDto> series;
}
