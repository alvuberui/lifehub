package com.finanzapp.movements.controller.mapper;

import com.finanzapp.movements.controller.dto.*;
import com.finanzapp.movements.repository.entities.MovementEntity;
import com.finanzapp.movements.service.domain.*;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(componentModel = "spring")
public interface MovementMapper {

    MovementResponseDto toMovementDto(Movement movementEntity);

    List<MovementResponseDto> toMovementDtoList(List<Movement> movementEntities);

    @Mapping(target = "movementType", source = "metric")
    AnualMovementResponseDto toAnualMovementResponseDto(AnualMovementResponse anualMovementResponse);

    HistoricalMovementResponseDto toHistoricalMovementResponseDto(HistoricalMovementResponse historicalMovementResponse);

    CreateMovementRequest toCreateMovementRequest(CreateMovementDto createMovementDto);

    UpdateMovementRequest toUpdateMovementRequest(UpdateMovementDto updateMovementDto);


}
