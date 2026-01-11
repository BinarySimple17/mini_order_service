package ru.binarysimple.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;
import ru.binarysimple.order.model.OrderPosition;
import ru.binarysimple.order.dto.OrderPositionDto;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface OrderPositionMapper {

    OrderPosition toEntity(OrderPositionDto orderPositionDto);

    OrderPositionDto toOrderPositionDto(OrderPosition orderPosition);
}