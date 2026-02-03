package ru.binarysimple.order.dto;

import lombok.Value;
import ru.binarysimple.order.model.OperationType;

import java.math.BigDecimal;
import java.time.LocalDateTime;


@Value
public class OperationDto {
    Long id;
    LocalDateTime createdAt;
    OperationType type;
    BigDecimal amount;
    Long orderId;
}