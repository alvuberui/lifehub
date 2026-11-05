package com.finanzapp.tags.service.impl;

import org.springframework.http.HttpStatusCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import com.finanzapp.tags.service.domain.InternalTagDeletionRequest;

@Component
public class MovementsClientImpl {

    private final RestClient movementsRestClient;
    private final String internalServiceToken;

    public MovementsClientImpl(
            @Qualifier("movementsRestClient") RestClient movementsRestClient,
            @Value("${services.internal-service-token}") String internalServiceToken
    ) {
        this.movementsRestClient = movementsRestClient;
        this.internalServiceToken = internalServiceToken;
    }

    public void detachMovements(String userEmail, Long tagId) {
        movementsRestClient
                .post()
                .uri("/movement/movements/internal/tag-deletions")
                .header("X-Internal-Service-Token", internalServiceToken)
                .body(new InternalTagDeletionRequest(userEmail, tagId))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                    throw new IllegalStateException(
                            "Error llamando a updateMovementTags. Status: " + res.getStatusCode()
                    );
                })
                .toBodilessEntity();
    }
}
