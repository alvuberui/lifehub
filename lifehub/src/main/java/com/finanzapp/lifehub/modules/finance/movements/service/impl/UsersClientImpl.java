package com.finanzapp.movements.service.impl;

import com.finanzapp.movements.service.domain.IdealPercentageResponse;
import com.finanzapp.movements.service.domain.User;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class UsersClientImpl {

    private final RestClient usersRestClient;

    public UsersClientImpl(@Qualifier("usersRestClient") RestClient usersRestClient) {
        this.usersRestClient = usersRestClient;
    }

    public IdealPercentageResponse getIdealPercent(String bearerToken) {
        return usersRestClient.get()
                .uri("/user/users/idealPercentage")
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(IdealPercentageResponse.class);
    }

    public User getUser(String bearerToken) {
        return usersRestClient.get()
                .uri("/user/users/")
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(User.class);
    }
}

