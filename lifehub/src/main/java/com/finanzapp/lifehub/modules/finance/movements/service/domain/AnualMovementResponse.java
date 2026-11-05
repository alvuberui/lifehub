package com.finanzapp.movements.service.domain;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AnualMovementResponse {

    String metric;

    Integer year;

    List<MonthDashboard> series;
}
