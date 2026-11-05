package com.finanzapp.tags.controller.mapper;

import com.finanzapp.tags.controller.dto.TagDto;
import com.finanzapp.tags.service.domain.CreateTagRequest;
import org.mapstruct.Mapper;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagDto toTagDto(com.finanzapp.tags.service.domain.Tag tag);

    CreateTagRequest toCreateTagRequest(com.finanzapp.tags.controller.dto.CreateTagRequestDto createTagRequestDto);




}
