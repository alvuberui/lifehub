package com.finanzapp.lifehub.modules.identity.user.repository.mapper;

import com.finanzapp.lifehub.modules.identity.user.repository.entities.UserEntity;
import com.finanzapp.lifehub.modules.identity.user.service.domain.CreateUserRequest;
import com.finanzapp.lifehub.modules.identity.user.service.domain.IdealPercentageResponse;
import com.finanzapp.lifehub.modules.identity.user.service.domain.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserEntityMapper {

    User toDomain(UserEntity userEntity);

    UserEntity toUserEntity(CreateUserRequest request);

    IdealPercentageResponse toIdealPercentageResponse(UserEntity userEntity);


}
