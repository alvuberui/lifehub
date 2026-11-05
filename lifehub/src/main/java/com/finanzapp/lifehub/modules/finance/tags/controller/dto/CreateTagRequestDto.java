package com.finanzapp.tags.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateTagRequestDto {

    @NotNull
    @NotBlank
    @Size(max = 40)
    private String name;

    @NotNull
    @NotBlank
    @Size(min = 4, max = 7)
    @Pattern(regexp = "^#(?:[0-9A-Fa-f]{3}|[0-9A-Fa-f]{6})$", message = "El color debe ser hexadecimal, por ejemplo #22C55E")
    private String color;
}
