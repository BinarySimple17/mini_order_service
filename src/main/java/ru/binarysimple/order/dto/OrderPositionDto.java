package ru.binarysimple.order.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Value;
import ru.binarysimple.order.model.OrderPosition;

import java.math.BigDecimal;

/**
 * DTO for {@link OrderPosition}
 */
@Value
public class OrderPositionDto {
    @NotNull
    Long productId;
    @NotNull
    BigDecimal price;
    @NotNull
    Integer quantity;

}