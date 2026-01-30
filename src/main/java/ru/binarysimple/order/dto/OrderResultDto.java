package ru.binarysimple.order.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Value;
import ru.binarysimple.order.model.Order;
import ru.binarysimple.order.model.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for {@link Order}
 */
@Data
@NoArgsConstructor
public class OrderResultDto {
    private Long id;
    private String username;
    private List<OrderPositionDto> orderPositions;
    private BigDecimal totalCost;
    private LocalDateTime createdAt;
    private Long shopId;
    private OrderStatus status;
    private Long deliveryId;
}