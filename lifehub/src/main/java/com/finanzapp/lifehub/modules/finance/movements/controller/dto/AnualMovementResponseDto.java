package com.finanzapp.movements.controller.dto;

import com.finanzapp.movements.service.domain.MonthDashboard;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnualMovementResponseDto {

    String movementType;

    Integer year;

    List<MonthDashboardDto> series;
}
