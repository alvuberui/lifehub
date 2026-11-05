package com.finanzapp.movements.controller.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.finanzapp.movements.service.domain.MovementType;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Digits;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMovementDto {

    private String userEmail;

    private MovementType movementType;

    @Positive(message = "El importe debe ser mayor que 0")
    @Digits(integer = 15, fraction = 2, message = "El importe debe tener como máximo 15 dígitos y 2 decimales")
    private BigDecimal amount;

    @PastOrPresent(message = "La fecha no puede ser futura")
    private LocalDate date;

    @Positive(message = "El tagId debe ser un valor positivo")
    private Long tagId;

    @Size(max = 255, message = "La descripción no puede superar los 255 caracteres")
    private String description;

    @JsonIgnore
    private boolean tagIdSpecified;

    @JsonIgnore
    private boolean descriptionSpecified;

    public void setTagId(Long tagId) {
        this.tagId = tagId;
        this.tagIdSpecified = true;
    }

    public void setDescription(String description) {
        this.description = description;
        this.descriptionSpecified = true;
    }
}
