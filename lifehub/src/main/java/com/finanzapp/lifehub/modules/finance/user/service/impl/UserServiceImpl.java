package com.finanzapp.user.service.impl;

import com.finanzapp.user.repository.UserRepository;
import com.finanzapp.user.repository.mapper.UserEntityMapper;
import com.finanzapp.user.service.UserService;
import com.finanzapp.user.service.domain.*;
import lombok.Builder;
import org.springframework.stereotype.Service;

@Builder
@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    private final UserEntityMapper userEntityMapper;

    @Override
    public BooleanResponse hasOnBoarded(String email) {
        boolean isOnBoarded = userRepository.findByEmail(email).isPresent();
        return BooleanResponse.builder().result(isOnBoarded).build();
    }

    @Override
    public void createUser(CreateUserRequest request) {
        if (hasOnBoarded(request.getEmail()).getResult()) {
            throw new IllegalArgumentException("El usuario con email " + request.getEmail() + " ya existe.");
        }

        userRepository.save(userEntityMapper.toUserEntity(request));
        return;
    }

    @Override
    public IdealPercentageResponse getIdealPercentage(String email) {
        return this.userEntityMapper.toIdealPercentageResponse(
                userRepository.findByEmail(email)
                        .orElseThrow(() -> new IllegalArgumentException("El usuario con email " + email + " no existe."))
        );
    }

    @Override
    public User updateUser(UpdateUserRequest user, String email) {
        var userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("El usuario con email " + email + " no existe."));

        if(user.getBirthdate() != null) {
            userEntity.setBirthdate(user.getBirthdate());
        }
        if(user.getName() != null) {
            userEntity.setName(user.getName());
        }
        if(user.getLastname() != null) {
            userEntity.setLastname(user.getLastname());
        }
        if(user.getInitialWealth() != null) {
            userEntity.setInitialWealth(user.getInitialWealth());
        }
        if(user.getMonthlyInvestmentPercentage() != null) {
            userEntity.setMonthlyInvestmentPercentage(user.getMonthlyInvestmentPercentage());
        }
        if(user.getMonthlyNecessaryExpensesPercentage() != null) {
            userEntity.setMonthlyNecessaryExpensesPercentage(user.getMonthlyNecessaryExpensesPercentage());
        }
        if(user.getMonthlyDiscretionaryExpensesPercentage() != null) {
            userEntity.setMonthlyDiscretionaryExpensesPercentage(user.getMonthlyDiscretionaryExpensesPercentage());
        }
        if(user.getMonthlySavingPercentage() != null) {
            userEntity.setMonthlySavingPercentage(user.getMonthlySavingPercentage());
        }

        var updatedUserEntity = userRepository.save(userEntity);

        return userEntityMapper.toDomain(updatedUserEntity);
    }

    @Override
    public User getUserByEmail(String email) {
        var userEntity = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("El usuario con email " + email + " no existe."));

        return userEntityMapper.toDomain(userEntity);
    }

}
