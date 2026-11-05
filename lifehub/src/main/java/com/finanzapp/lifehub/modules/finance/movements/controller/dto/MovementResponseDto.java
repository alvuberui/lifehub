package com.finanzapp.movements.controller.dto;

import com.finanzapp.movements.repository.entities.MovementType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MovementResponseDto {

    private Long id;
    private String userEmail;
    private MovementType movementType;
    private BigDecimal amount;
    private LocalDate date;

    private Long tagId;
    private String tagName;
    private String tagColor;

    private String description;
}


