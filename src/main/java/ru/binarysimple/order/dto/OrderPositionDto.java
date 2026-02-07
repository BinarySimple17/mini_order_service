package ru.binarysimple.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.binarysimple.order.model.OrderPosition;

import java.math.BigDecimal;

/**
 * DTO for {@link OrderPosition}
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class OrderPositionDto {
    @NotNull
    Long productId;
    @NotNull
    BigDecimal price;
    @NotNull
    Integer quantity;

}