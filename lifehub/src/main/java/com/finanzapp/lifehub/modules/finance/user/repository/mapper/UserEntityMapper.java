package com.finanzapp.user.repository.mapper;

import com.finanzapp.user.repository.entities.UserEntity;
import com.finanzapp.user.service.domain.CreateUserRequest;
import com.finanzapp.user.service.domain.IdealPercentageResponse;
import com.finanzapp.user.service.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {

    User toDomain(UserEntity userEntity);

    UserEntity toUserEntity(CreateUserRequest request);

    IdealPercentageResponse toIdealPercentageResponse(UserEntity userEntity);


}
