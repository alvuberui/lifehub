package com.finanzapp.movements.service.impl;

import com.finanzapp.movements.controller.dto.MovementResponseDto;

import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class MovementTagsEnricher {

    private final TagsClientImpl tagsClient;

    public MovementTagsEnricher(TagsClientImpl tagsClient) {
        this.tagsClient = tagsClient;
    }

    public List<MovementResponseDto> enrich(List<MovementResponseDto> dtos, String bearerToken) {

        List<Long> tagIds = dtos.stream()
                .map(MovementResponseDto::getTagId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (tagIds.isEmpty()) return dtos;

        List<TagsClientImpl.TagDto> tags = tagsClient.getTagsBatch(tagIds, bearerToken);

        Map<Long, TagsClientImpl.TagDto> tagMap = tags.stream()
                .collect(Collectors.toMap(TagsClientImpl.TagDto::getId, t -> t));

        dtos.forEach(m -> {
            if (m.getTagId() != null) {
                TagsClientImpl.TagDto tag = tagMap.get(m.getTagId());
                if (tag != null) {
                    m.setTagName(tag.getName());
                    m.setTagColor(tag.getColor());
                }
            }
        });

        return dtos;
    }
}
