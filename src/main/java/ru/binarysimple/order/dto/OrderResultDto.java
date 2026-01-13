package ru.binarysimple.order.dto;

import lombok.Value;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link Order}
 */
@Value
public class OrderResultDto {
    Long id;
    String username;
    List<OrderPositionDto> orderPositions;
    BigDecimal totalCost;
    LocalDateTime createdAt;
    Long shopId;
    OrderStatus status;
    Long deliveryId;
}