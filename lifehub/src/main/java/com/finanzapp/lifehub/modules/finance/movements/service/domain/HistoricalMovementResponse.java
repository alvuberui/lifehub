package com.finanzapp.movements.service.domain;

import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class HistoricalMovementResponse {

    String metric;

    List<HistoricalDashboard> series;
}
