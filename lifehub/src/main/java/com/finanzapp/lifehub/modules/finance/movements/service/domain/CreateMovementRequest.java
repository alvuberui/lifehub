package com.finanzapp.movements.service.domain;


import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMovementRequest {

    private String userEmail;

    private MovementType movementType;

    private BigDecimal amount;

    private LocalDate date;

    private Long tagId;

    private String description;
}
