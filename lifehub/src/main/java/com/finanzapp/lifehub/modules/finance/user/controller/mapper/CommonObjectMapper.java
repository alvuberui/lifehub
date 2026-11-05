package com.finanzapp.user.controller.mapper;

import com.finanzapp.user.controller.dto.BooleanRespondeDto;
import com.finanzapp.user.controller.dto.CreateUserRequestDto;
import com.finanzapp.user.service.domain.BooleanResponse;
import com.finanzapp.user.service.domain.CreateUserRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommonObjectMapper {
    BooleanRespondeDto toBooleanRespondeDto(BooleanResponse booleanResponse);

    CreateUserRequest toCreateUserRequest(CreateUserRequestDto createUserRequestDto);
}
