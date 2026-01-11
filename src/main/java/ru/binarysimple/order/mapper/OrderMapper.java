package ru.binarysimple.order.mapper;

import org.mapstruct.*;
import ru.binarysimple.order.dto.OrderDto;
import ru.binarysimple.order.model.Order;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING,
        uses = OrderPositionMapper.class)
public interface OrderMapper {

    Order toEntity(OrderDto orderDto);

    OrderDto toOrderDto(Order order);

    Order updateWithNull(OrderDto orderDto, @MappingTarget Order order);
}