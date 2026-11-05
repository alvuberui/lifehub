package com.finanzapp.lifehub.modules.identity.user.controller.mapper;

import com.finanzapp.lifehub.modules.identity.user.controller.dto.BooleanRespondeDto;
import com.finanzapp.lifehub.modules.identity.user.controller.dto.CreateUserRequestDto;
import com.finanzapp.lifehub.modules.identity.user.service.domain.BooleanResponse;
import com.finanzapp.lifehub.modules.identity.user.service.domain.CreateUserRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface CommonObjectMapper {
    BooleanRespondeDto toBooleanRespondeDto(BooleanResponse booleanResponse);

    CreateUserRequest toCreateUserRequest(CreateUserRequestDto createUserRequestDto);
}
