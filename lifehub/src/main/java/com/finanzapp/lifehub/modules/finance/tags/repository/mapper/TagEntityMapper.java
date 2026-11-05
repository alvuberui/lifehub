package com.finanzapp.tags.repository.mapper;


import com.finanzapp.tags.repository.entities.TagEntity;
import com.finanzapp.tags.service.domain.CreateTagRequest;
import com.finanzapp.tags.service.domain.Tag;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TagEntityMapper {

    TagEntity toEntity(CreateTagRequest createTagRequest, String userEmail);

    Tag toDomain(TagEntity tagEntity);
}
