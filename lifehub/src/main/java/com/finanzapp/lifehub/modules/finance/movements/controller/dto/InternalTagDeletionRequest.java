package com.finanzapp.movements.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InternalTagDeletionRequest(
        @NotBlank String userEmail,
        @NotNull Long tagId
) {
}
