package com.finanzapp.lifehub.modules.finance.tags;

import com.finanzapp.tags.controller.dto.CreateTagRequestDto;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TagValidationTests {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void colorsMustBeHexadecimal() {
        CreateTagRequestDto dto = CreateTagRequestDto.builder()
                .name("Casa")
                .color("javascript:alert(1)")
                .build();

        assertThat(validator.validate(dto))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("color");
    }

    @Test
    void validHexColorsAreAccepted() {
        CreateTagRequestDto dto = CreateTagRequestDto.builder()
                .name("Casa")
                .color("#22C55E")
                .build();

        assertThat(validator.validate(dto)).isEmpty();
    }
}

