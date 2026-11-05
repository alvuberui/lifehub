package com.finanzapp.movements.controller.dto;

import com.finanzapp.movements.service.domain.MovementType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateMovementDto {

    @NotNull(message = "El tipo de movimiento es obligatorio")
    private MovementType movementType;

    @NotNull(message = "El importe es obligatorio")
    @Positive(message = "El importe debe ser mayor que 0")
    @Digits(integer = 15, fraction = 2, message = "El importe debe tener como máximo 15 dígitos y 2 decimales")
    private BigDecimal amount;

    @NotNull(message = "La fecha es obligatoria")
    @PastOrPresent(message = "La fecha no puede ser futura")
    private LocalDate date;

    @Positive(message = "El tagId debe ser un valor positivo")
    private Long tagId;

    @NotNull(message = "La descripción es obligatoria")
    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String description;
}
