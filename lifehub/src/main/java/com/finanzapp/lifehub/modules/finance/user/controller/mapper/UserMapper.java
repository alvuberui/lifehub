package com.finanzapp.user.controller.mapper;

import com.finanzapp.user.controller.dto.IdealPercentageResponseDto;
import com.finanzapp.user.controller.dto.UserDto;
import com.finanzapp.user.service.domain.IdealPercentageResponse;
import com.finanzapp.user.service.domain.UpdateUserRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {

    IdealPercentageResponseDto toIdealPercentageResponseDto(IdealPercentageResponse response);

    UpdateUserRequest toUpdateUserRequest(com.finanzapp.user.controller.dto.UpdateUserRequestDto requestDto);

    UserDto toUserDto(com.finanzapp.user.service.domain.User user);


}
