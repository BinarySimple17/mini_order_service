package ru.binarysimple.order.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Value;
import ru.binarysimple.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link ru.binarysimple.order.model.Order}
 */
@Value
public class OrderDto {
    @NotNull
    @NotEmpty
    String username;
    @NotNull
    BigDecimal totalCost;
    LocalDateTime createdAt;
    @NotNull
    Long shopId;
    @NotNull
    OrderStatus orderStatus;
    @NotNull
    Long deliveryId;
    @NotNull
    List<OrderPositionDto> orderPositions;
}