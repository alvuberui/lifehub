package com.finanzapp.lifehub.modules.identity.user.controller.mapper;

import com.finanzapp.lifehub.modules.identity.user.controller.dto.IdealPercentageResponseDto;
import com.finanzapp.lifehub.modules.identity.user.controller.dto.UserDto;
import com.finanzapp.lifehub.modules.identity.user.service.domain.IdealPercentageResponse;
import com.finanzapp.lifehub.modules.identity.user.service.domain.UpdateUserRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    IdealPercentageResponseDto toIdealPercentageResponseDto(IdealPercentageResponse response);

    UpdateUserRequest toUpdateUserRequest(com.finanzapp.lifehub.modules.identity.user.controller.dto.UpdateUserRequestDto requestDto);

    UserDto toUserDto(com.finanzapp.lifehub.modules.identity.user.service.domain.User user);


}
