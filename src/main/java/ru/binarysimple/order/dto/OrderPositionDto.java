package ru.binarysimple.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.binarysimple.order.model.OrderPosition;

import java.math.BigDecimal;

/**
 * DTO for {@link OrderPosition}
 */
@Setter
@Getter
@AllArgsConstructor
public class OrderPositionDto {
    @NotNull
    Long productId;
    @NotNull
    BigDecimal price;
    @NotNull
    Integer quantity;
//    @NotNull
//    Long orderId;
}