package com.finanzapp.movements.service.impl;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;

@Component
public class TagsClientImpl {

    private final RestClient tagsRestClient;

    public TagsClientImpl(@Qualifier("tagsRestClient") RestClient tagsRestClient) {
        this.tagsRestClient = tagsRestClient;
    }

    public Boolean existsByIdAndUserEmail(Long tagId, String bearerToken) {
        return tagsRestClient.get()
                .uri(uriBuilder -> uriBuilder.path("/tag/tags/exists/{tagId}").build(tagId))
                .header("Authorization", "Bearer " + bearerToken)
                .retrieve()
                .body(Boolean.class);
    }

    public List<TagDto> getTagsBatch(List<Long> tagIds, String bearerToken) {

        return tagsRestClient.post()
                .uri("/tag/tags/batch")
                .header("Authorization", "Bearer " + bearerToken)
                .body(tagIds)
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<TagDto>>() {});
    }

    @Getter @Setter
    public static class TagDto {
        private Long id;
        private String name;
        private String color;
    }
}
